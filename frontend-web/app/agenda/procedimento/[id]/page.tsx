'use client'

import { useState, useEffect, use } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/contexts/AuthContext'
import { useNotificacao } from '@/contexts/ToastContext'
import { agendamentoAPI } from '@/lib/agendamento.service'
import { anamneseAPI } from '@/lib/anamnese.service'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import {
  Agendamento,
  EvolucaoCurativoRequisicao,
  TecidoLeito,
  InfeccaoInflamacaoCurativo,
  ExsudatoTime,
  BordasFerida,
  TipoDesbridamento,
  AvaliacaoEvolucao,
} from '@/tipos/agendamento'
import { nomeProcedimento } from '@/tipos/TipoProcedimento'
import { ROTULO_LOCAL_ATENDIMENTO } from '@/tipos/LocalAtendimento'
import { RotaProtegida } from '@/components/RotaProtegida'
import Layout from '@/components/Layout/Layout'
import Modal from '@/components/Modal'
import {
  ArrowLeft,
  Calendar,
  Clock,
  User,
  Tag,
  Circle,
  CheckCircle,
  FileText,
  History,
  Activity,
  Pill,
  Play,
  CalendarPlus,
  Eye,
  Scissors,
  MessageSquare,
  BookOpen,
} from 'lucide-react'
import {
  BloqueadoTag,
  Campo,
  CampoSelect,
  CheckItem,
  LinhaObservacao,
} from './campos'
import FichaAnamnese, {
  AnamneseForm,
  ANAMNESE_INICIAL,
  anamneseParaApi,
  anamneseParaFormulario,
} from './FichaAnamnese'
import FichaEnfermagem, {
  EnfermagemForm,
  ENFERMAGEM_INICIAL,
  enfermagemParaApi,
  enfermagemParaFormulario,
} from './FichaEnfermagem'
import './procedimento.css'

// ── Ficha de Evolução Diária – Curativos ─────────────────────────
// Modelo: ajustar_campos/modelos/FICHA DE EVOLUÇÃO DIÁRIA - CURATIVOS.pdf
// Preenchida apenas em agendamentos do tipo TRATAMENTO.

type EvolucaoForm = {
  // 1. Avaliação diária da ferida
  comprimento: string
  largura: string
  profundidade: string
  tecido: string
  infeccaoInflamacao: string
  exsudato: string
  bordas: string
  odorPresente: boolean
  dorEscala: string
  pelePerilesional: string
  // 2. Intervenções realizadas
  limpezaIrrigacao: string
  desbridamento: string
  desbridamentoObs: string
  coberturaPrimaria: string
  orientacoesPaciente: string
  // 3. Avaliação da evolução
  evolucao: string
  observacoes: string
  // 4. Plano / ações futuras
  planoManterConduta: string
  planoAlterarCobertura: string
  planoSolicitarExames: string
  planoEncaminhamento: string
  retornoPrevisto: string
}

const EVOLUCAO_INICIAL: EvolucaoForm = {
  comprimento: '',
  largura: '',
  profundidade: '',
  tecido: '',
  infeccaoInflamacao: '',
  exsudato: '',
  bordas: '',
  odorPresente: false,
  dorEscala: '',
  pelePerilesional: '',
  limpezaIrrigacao: '',
  desbridamento: '',
  desbridamentoObs: '',
  coberturaPrimaria: '',
  orientacoesPaciente: '',
  evolucao: '',
  observacoes: '',
  planoManterConduta: '',
  planoAlterarCobertura: '',
  planoSolicitarExames: '',
  planoEncaminhamento: '',
  retornoPrevisto: '',
}

// ── Opções (legenda TIME do modelo) ──────────────────────────────

const OPCOES_TECIDO: { value: TecidoLeito; label: string }[] = [
  { value: 'GRANULACAO', label: 'G — Granulação' },
  { value: 'ESFACELO', label: 'E — Esfacelo' },
  { value: 'NECROSE', label: 'N — Necrose' },
  { value: 'EPITELIZACAO', label: 'EP — Epitelização' },
]

const OPCOES_INFECCAO: {
  value: InfeccaoInflamacaoCurativo
  label: string
}[] = [
  { value: 'AUSENTE', label: '0 — Ausente' },
  { value: 'LOCAL', label: '1 — Local' },
  { value: 'SISTEMICA', label: '2 — Sistêmica' },
]

const OPCOES_EXSUDATO: { value: ExsudatoTime; label: string }[] = [
  { value: 'AUSENTE', label: '0 — Ausente' },
  { value: 'PEQUENO', label: '1 — Pequeno' },
  { value: 'MODERADO', label: '2 — Moderado' },
  { value: 'INTENSO', label: '3 — Intenso' },
]

const OPCOES_BORDAS: { value: BordasFerida; label: string }[] = [
  { value: 'INTEGRAS', label: 'I — Íntegras' },
  { value: 'MACERADAS', label: 'M — Maceradas' },
  { value: 'DESCOLADAS', label: 'D — Descoladas' },
  { value: 'EPITELIZANDO', label: 'EP — Epitelizando' },
]

const OPCOES_DESBRIDAMENTO: { value: TipoDesbridamento; label: string }[] = [
  { value: 'NAO', label: 'Não' },
  { value: 'AUTOLITICO', label: 'Autolítico' },
  { value: 'INSTRUMENTAL', label: 'Instrumental' },
  { value: 'ENZIMATICO', label: 'Enzimático' },
]

const OPCOES_EVOLUCAO: { value: AvaliacaoEvolucao; label: string }[] = [
  { value: 'MELHORA', label: 'Melhora' },
  { value: 'ESTAVEL', label: 'Estável' },
  { value: 'PIORA', label: 'Piora' },
]

// ── Limites da ficha ─────────────────────────────────────────────
//
// Espelham as restrições de EvolucaoCurativoRequestDTO no backend. Se
// divergirem, o usuário preenche a ficha inteira e só descobre o problema
// no 422 devolvido ao finalizar a consulta.

/** `@Size(max = 5000)` — campos descritivos longos. */
const LIMITE_TEXTO_LONGO = 5000
/** `@Size(max = 1000)` — cobertura, plano de ações e retorno. */
const LIMITE_TEXTO_CURTO = 1000
/** `@Min(0) @Max(10)` — escala de dor. */
const DOR_MIN = 0
const DOR_MAX = 10

/**
 * Teto das medidas da ferida, em cm.
 *
 * O DTO não valida as dimensões: elas seguem direto para colunas
 * `NUMERIC(6,2)` e estouram no Postgres como erro 500. O limite é 999,99 e
 * não 9999,99 (o teto da coluna) porque `areaAproximada` = C × L vai para uma
 * `NUMERIC(8,2)`: com 999,99 a área máxima é 999.980, que ainda cabe.
 */
const DIMENSAO_MAX = 999.99

/**
 * Valida o que o navegador não consegue impedir sozinho.
 *
 * Os `maxLength` dos campos já barram o `@Size` na digitação, mas `min`/`max`
 * de um input `type="number"` limitam apenas as setas — não impedem digitar
 * ou colar 15. O backend também aceitaria 4.5 arredondando para 4
 * (ACCEPT_FLOAT_AS_INT do Jackson), o que falsearia a escala em silêncio.
 *
 * Retorna a mensagem do primeiro problema encontrado, ou `null` se estiver ok.
 */
function validarEvolucao(form: EvolucaoForm): string | null {
  const dimensoes = [form.comprimento, form.largura, form.profundidade]
  for (const bruto of dimensoes) {
    const texto = bruto.trim()
    if (!texto) continue
    const medida = Number(texto)
    if (!Number.isFinite(medida) || medida < 0 || medida > DIMENSAO_MAX) {
      return MENSAGENS.validacao.medidaForaDaFaixa
    }
  }

  const dor = form.dorEscala.trim()
  if (!dor) return null

  const valor = Number(dor)
  if (!Number.isInteger(valor) || valor < DOR_MIN || valor > DOR_MAX) {
    return MENSAGENS.validacao.dorForaDaEscala
  }
  return null
}

// ── Conversões formulário ↔ API ──────────────────────────────────

function numParaTexto(n?: number): string {
  return n == null ? '' : n.toString()
}

function txt(valor: string): string | undefined {
  return valor.trim() ? valor : undefined
}

function num(valor: string): number | undefined {
  return valor.trim() ? Number(valor) : undefined
}

/** Área aproximada da ferida (C × L), arredondada a 2 casas. */
function calcularArea(
  comprimento?: number,
  largura?: number,
): number | undefined {
  if (comprimento == null || largura == null) return undefined
  return Math.round(comprimento * largura * 100) / 100
}

function evolucaoFromAgendamento(ag: Agendamento): EvolucaoForm {
  const ec = ag.evolucaoCurativo
  return {
    comprimento: numParaTexto(ec?.comprimento),
    largura: numParaTexto(ec?.largura),
    profundidade: numParaTexto(ec?.profundidade),
    tecido: ec?.tecido ?? '',
    infeccaoInflamacao: ec?.infeccaoInflamacao ?? '',
    exsudato: ec?.exsudato ?? '',
    bordas: ec?.bordas ?? '',
    odorPresente: ec?.odorPresente ?? false,
    dorEscala: numParaTexto(ec?.dorEscala),
    pelePerilesional: ec?.pelePerilesional ?? '',
    limpezaIrrigacao: ec?.limpezaIrrigacao ?? '',
    desbridamento: ec?.desbridamento ?? '',
    desbridamentoObs: ec?.desbridamentoObs ?? '',
    coberturaPrimaria: ec?.coberturaPrimaria ?? '',
    orientacoesPaciente: ec?.orientacoesPaciente ?? '',
    evolucao: ec?.evolucao ?? '',
    observacoes: ec?.observacoes ?? '',
    planoManterConduta: ec?.planoManterConduta ?? '',
    planoAlterarCobertura: ec?.planoAlterarCobertura ?? '',
    planoSolicitarExames: ec?.planoSolicitarExames ?? '',
    planoEncaminhamento: ec?.planoEncaminhamento ?? '',
    retornoPrevisto: ec?.retornoPrevisto ?? '',
  }
}

function evolucaoParaApi(form: EvolucaoForm): EvolucaoCurativoRequisicao {
  const comprimento = num(form.comprimento)
  const largura = num(form.largura)

  return {
    comprimento,
    largura,
    profundidade: num(form.profundidade),
    areaAproximada: calcularArea(comprimento, largura),
    tecido: (form.tecido as TecidoLeito) || undefined,
    infeccaoInflamacao:
      (form.infeccaoInflamacao as InfeccaoInflamacaoCurativo) || undefined,
    exsudato: (form.exsudato as ExsudatoTime) || undefined,
    bordas: (form.bordas as BordasFerida) || undefined,
    odorPresente: form.odorPresente,
    dorEscala: num(form.dorEscala),
    pelePerilesional: txt(form.pelePerilesional),
    limpezaIrrigacao: txt(form.limpezaIrrigacao),
    desbridamento: (form.desbridamento as TipoDesbridamento) || undefined,
    desbridamentoObs: txt(form.desbridamentoObs),
    coberturaPrimaria: txt(form.coberturaPrimaria),
    orientacoesPaciente: txt(form.orientacoesPaciente),
    evolucao: (form.evolucao as AvaliacaoEvolucao) || undefined,
    observacoes: txt(form.observacoes),
    planoManterConduta: txt(form.planoManterConduta),
    planoAlterarCobertura: txt(form.planoAlterarCobertura),
    planoSolicitarExames: txt(form.planoSolicitarExames),
    planoEncaminhamento: txt(form.planoEncaminhamento),
    retornoPrevisto: txt(form.retornoPrevisto),
  }
}

// ── Helpers ──────────────────────────────────────────────────────

const fmt = {
  hora: (iso: string) =>
    new Date(iso).toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
    }),

  data: (iso: string) =>
    new Date(iso).toLocaleDateString('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    }),

  dataCurta: (iso: string) =>
    new Date(iso).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: 'short',
    }),

  duracao: (inicio: string, fim: string): string => {
    const min = Math.floor(
      (new Date(fim).getTime() - new Date(inicio).getTime()) / 60000,
    )
    if (min < 60) return `${min} min`
    const h = Math.floor(min / 60)
    const m = min % 60
    return m > 0 ? `${h}h${m}min` : `${h}h`
  },
}

const ROTULO_STATUS: Record<string, string> = {
  AGENDADO: 'Agendado',
  CONFIRMADO: 'Confirmado',
  REALIZADO: 'Realizado',
  CANCELADO: 'Cancelado',
  REMARCADO: 'Remarcado',
  NAO_COMPARECEU: 'Não compareceu',
}

const ROTULO_TIPO: Record<string, string> = {
  AVALIACAO: 'Avaliação',
  TRATAMENTO: 'Tratamento',
}

// ── Sub-componentes ───────────────────────────────────────────────

function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`proc-status proc-status--${status}`}>
      {ROTULO_STATUS[status] ?? status}
    </span>
  )
}

function TipoBadge({ tipo }: { tipo: string }) {
  const classe =
    tipo === 'AVALIACAO' ? 'proc-tipo-avaliacao' : 'proc-tipo-tratamento'
  const Icon = tipo === 'AVALIACAO' ? Activity : Pill
  return (
    <span className={`proc-tipo-badge ${classe}`}>
      <Icon size={12} strokeWidth={2.5} />
      {ROTULO_TIPO[tipo] ?? tipo}
    </span>
  )
}

function HistoricoItem({
  item,
  isAtual,
  index,
}: {
  item: Agendamento
  isAtual: boolean
  index: number
}) {
  const isRealizado = item.status === 'REALIZADO'
  const dotClasse = isAtual
    ? 'proc-hist-dot--atual'
    : isRealizado
      ? 'proc-hist-dot--realizado'
      : item.tipo === 'AVALIACAO'
        ? 'proc-hist-dot--avaliacao'
        : 'proc-hist-dot--tratamento'

  return (
    <div className={`proc-hist-item${isAtual ? ' proc-hist-item--atual' : ''}`}>
      <div className={`proc-hist-dot ${dotClasse}`}>
        {isRealizado && !isAtual ? <CheckCircle size={14} /> : index + 1}
      </div>
      <div className="proc-hist-info">
        <div className="proc-hist-esq">
          <span className="proc-hist-data">{fmt.dataCurta(item.inicioEm)}</span>
          <span className="proc-hist-tipo">
            {ROTULO_TIPO[item.tipo] ?? item.tipo}
            {item.tipo === 'TRATAMENTO' && !isAtual && ` #${index}`}
          </span>
        </div>
        {isAtual ? (
          <span className="proc-hist-atual-tag">Consulta atual</span>
        ) : (
          <StatusBadge status={item.status} />
        )}
      </div>
    </div>
  )
}

// ── Página principal ─────────────────────────────────────────────

export default function ProcedimentoPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const router = useRouter()
  const { temPerfil } = useAuth()
  const { exibirNotificacao } = useNotificacao()

  const [agendamento, setAgendamento] = useState<Agendamento | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)

  const [historico, setHistorico] = useState<Agendamento[]>([])
  const [carregandoHist, setCarregandoHist] = useState(false)

  const [evolucao, setEvolucao] = useState<EvolucaoForm>(EVOLUCAO_INICIAL)
  // Fichas da Avaliação: anamnese (1:1 com o paciente) + evolução de enfermagem
  const [anamnese, setAnamnese] = useState<AnamneseForm>(ANAMNESE_INICIAL)
  const [anamneseExistente, setAnamneseExistente] = useState(false)
  const [enfermagem, setEnfermagem] =
    useState<EnfermagemForm>(ENFERMAGEM_INICIAL)
  const [iniciado, setIniciado] = useState(false)
  const [finalizando, setFinalizando] = useState(false)
  const [modalAberto, setModalAberto] = useState(false)

  const isAdmin = temPerfil('ROLE_ADMIN')
  const perfilLayout = isAdmin ? 'ROLE_ADMIN' : 'ROLE_PROFISSIONAL'

  useEffect(() => {
    agendamentoAPI
      .buscarPorId(Number(id))
      .then(data => {
        setAgendamento(data)
        setEvolucao(evolucaoFromAgendamento(data))
        setEnfermagem(
          enfermagemParaFormulario(data.evolucaoEnfermagem, data.inicioEm),
        )
      })
      .catch(() => setErro(MENSAGENS.erro.carregarAgendamento))
      .finally(() => setCarregando(false))
  }, [id])

  // A anamnese acompanha o paciente: carrega a existente para edição na avaliação.
  const ehAvaliacao = agendamento?.tipo === 'AVALIACAO'
  const pacienteUuid = agendamento?.pacienteUuid

  useEffect(() => {
    if (!ehAvaliacao || !pacienteUuid) return
    anamneseAPI
      .buscar(pacienteUuid)
      .then(dados => {
        setAnamnese(anamneseParaFormulario(dados))
        setAnamneseExistente(dados !== null)
      })
      .catch(() =>
        exibirNotificacao(MENSAGENS.erro.carregarAnamnese, 'error', 5000),
      )
  }, [ehAvaliacao, pacienteUuid, exibirNotificacao])

  useEffect(() => {
    if (!agendamento?.avaliacaoId || agendamento.tipo !== 'TRATAMENTO') return
    setCarregandoHist(true)
    agendamentoAPI
      .getTratamentosPorAvaliacao(agendamento.avaliacaoId)
      .then(dados =>
        setHistorico(
          [...dados].sort(
            (a, b) =>
              new Date(a.inicioEm).getTime() - new Date(b.inicioEm).getTime(),
          ),
        ),
      )
      .catch(() => {})
      .finally(() => setCarregandoHist(false))
  }, [agendamento])

  const setEv =
    (campo: keyof EvolucaoForm) =>
    (
      e: React.ChangeEvent<
        HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
      >,
    ) =>
      setEvolucao(prev => ({ ...prev, [campo]: e.target.value }))

  const setBool = (campo: keyof EvolucaoForm) => (valor: boolean) =>
    setEvolucao(prev => ({ ...prev, [campo]: valor }))

  const alterarAnamnese = (mudanca: Partial<AnamneseForm>) =>
    setAnamnese(prev => ({ ...prev, ...mudanca }))

  const alterarEnfermagem = (mudanca: Partial<EnfermagemForm>) =>
    setEnfermagem(prev => ({ ...prev, ...mudanca }))

  const finalizarConsulta = async () => {
    if (!agendamento) return

    // Barra o que o backend recusaria antes de gravar qualquer coisa — o
    // fluxo grava a ficha e só depois conclui, então falhar na segunda
    // chamada deixaria a consulta pela metade.
    if (agendamento.tipo === 'TRATAMENTO') {
      const invalido = validarEvolucao(evolucao)
      if (invalido) {
        exibirNotificacao(invalido, 'error', 5000)
        return
      }
    }

    setFinalizando(true)
    try {
      // Cada tipo grava a ficha do seu modelo — o backend recusa a ficha
      // que não corresponde ao tipo do agendamento.
      if (agendamento.tipo === 'AVALIACAO') {
        const dadosAnamnese = anamneseParaApi(anamnese)
        if (anamneseExistente) {
          await anamneseAPI.atualizar(agendamento.pacienteUuid, dadosAnamnese)
        } else {
          await anamneseAPI.registrar(agendamento.pacienteUuid, dadosAnamnese)
          setAnamneseExistente(true)
        }
        await agendamentoAPI.registrarEvolucaoEnfermagem(
          agendamento.id,
          enfermagemParaApi(enfermagem),
        )
      } else if (agendamento.tipo === 'TRATAMENTO') {
        await agendamentoAPI.registrarEvolucaoCurativo(
          agendamento.id,
          evolucaoParaApi(evolucao),
        )
      }
      await agendamentoAPI.concluirAgendamento(agendamento.id)
      setAgendamento(prev => (prev ? { ...prev, status: 'REALIZADO' } : prev))
      setIniciado(false)
      setModalAberto(true)
    } catch (erro) {
      // O backend recusa a ficha campo a campo (422 + `detalhes`). Descartar o
      // erro aqui deixava o usuário sem saber o que corrigir.
      exibirNotificacao(
        mensagemErro(erro, MENSAGENS.erro.finalizarConsulta),
        'error',
        8000,
      )
    } finally {
      setFinalizando(false)
    }
  }

  const jaRealizado = agendamento?.status === 'REALIZADO'
  const ehTratamento = agendamento?.tipo === 'TRATAMENTO'
  const podeEditar = iniciado && !jaRealizado

  // Área aproximada (C×L) — somente leitura, recalculada a cada digitação
  const areaAproximadaTexto = numParaTexto(
    calcularArea(num(evolucao.comprimento), num(evolucao.largura)),
  )

  const agendarProximo = () => {
    if (!agendamento) return
    const avaliacaoId =
      agendamento.tipo === 'AVALIACAO'
        ? agendamento.id
        : agendamento.avaliacaoId
    const params = new URLSearchParams({
      novoTratamento: '1',
      tipo: 'TRATAMENTO',
    })
    params.set('pacienteUuid', agendamento.pacienteUuid)
    params.set('profissionalUuid', agendamento.profissionalUuid)
    if (avaliacaoId) params.set('avaliacaoId', String(avaliacaoId))
    if (agendamento.procedimentoId)
      params.set('procedimentoId', String(agendamento.procedimentoId))
    if (agendamento.tipoProcedimento)
      params.set('tipoProcedimento', agendamento.tipoProcedimento)
    if (agendamento.localAtendimento)
      params.set('localAtendimento', agendamento.localAtendimento)
    if (agendamento.pacienteAcamado != null)
      params.set('pacienteAcamado', String(agendamento.pacienteAcamado))
    router.push(`/agenda?${params.toString()}`)
  }

  return (
    <Layout perfil={perfilLayout}>
      <RotaProtegida perfisNecessarios={['ROLE_PROFISSIONAL', 'ROLE_ADMIN']}>
        <div className="proc-page">
          {/* Barra superior */}
          <div className="proc-topbar">
            <button className="proc-back-btn" onClick={() => router.back()}>
              <ArrowLeft size={18} />
              Voltar
            </button>
            {agendamento && (
              <div className="proc-topbar-info">
                <TipoBadge tipo={agendamento.tipo} />
                <span className="proc-topbar-nome">
                  {agendamento.nomePaciente}
                </span>
              </div>
            )}
            {agendamento && (
              <div className="proc-topbar-actions">
                <StatusBadge status={agendamento.status} />
              </div>
            )}
          </div>

          {carregando && (
            <div className="proc-estado">
              <div className="proc-spinner" />
              <p>Carregando consulta…</p>
            </div>
          )}

          {erro && (
            <div className="proc-estado">
              <p style={{ color: 'var(--color-error)' }}>{erro}</p>
              <button
                className="proc-btn-finalizar"
                onClick={() => router.back()}
              >
                Voltar para a agenda
              </button>
            </div>
          )}

          {agendamento && !carregando && (
            <>
              {/* Hero */}
              <div className="proc-hero">
                <div className="proc-hero-inner">
                  <div className="proc-hero-tipo">
                    {agendamento.tipo === 'AVALIACAO' ? (
                      <Activity size={14} />
                    ) : (
                      <Pill size={14} />
                    )}
                    {ROTULO_TIPO[agendamento.tipo]}
                    {nomeProcedimento(agendamento) && (
                      <span className="proc-hero-procedimento">
                        {nomeProcedimento(agendamento)}
                      </span>
                    )}
                    {agendamento.localAtendimento && (
                      <span className="proc-hero-procedimento">
                        {ROTULO_LOCAL_ATENDIMENTO[agendamento.localAtendimento]}
                      </span>
                    )}
                    {agendamento.pacienteAcamado && (
                      <span className="proc-hero-procedimento proc-hero-acamado">
                        Acamado
                      </span>
                    )}
                  </div>
                  <h1 className="proc-hero-nome">{agendamento.nomePaciente}</h1>
                  <div className="proc-hero-meta">
                    <span className="proc-hero-meta-item">
                      <Calendar size={14} />
                      <span style={{ textTransform: 'capitalize' }}>
                        {fmt.data(agendamento.inicioEm)}
                      </span>
                    </span>
                    <span className="proc-hero-meta-item">
                      <Clock size={14} />
                      {fmt.hora(agendamento.inicioEm)} –{' '}
                      {fmt.hora(agendamento.fimEm)}
                      <span className="proc-hero-duracao">
                        ({fmt.duracao(agendamento.inicioEm, agendamento.fimEm)})
                      </span>
                    </span>
                    {agendamento.nomeProfissional && (
                      <span className="proc-hero-meta-item">
                        <User size={14} />
                        {agendamento.nomeProfissional}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* Grade de conteúdo */}
              <div className="proc-content">
                {/* Informações */}
                <div className="proc-card proc-col-esq">
                  <div className="proc-card-header">
                    <FileText size={16} />
                    Informações da consulta
                  </div>
                  <div className="proc-info-lista">
                    <div className="proc-info-row">
                      <Calendar size={18} className="proc-info-icone" />
                      <div className="proc-info-texto">
                        <span className="proc-info-rotulo">Data</span>
                        <span
                          className="proc-info-valor"
                          style={{ textTransform: 'capitalize' }}
                        >
                          {fmt.data(agendamento.inicioEm)}
                        </span>
                      </div>
                    </div>
                    <div className="proc-info-row">
                      <Clock size={18} className="proc-info-icone" />
                      <div className="proc-info-texto">
                        <span className="proc-info-rotulo">Horário</span>
                        <span className="proc-info-valor">
                          {fmt.hora(agendamento.inicioEm)} –{' '}
                          {fmt.hora(agendamento.fimEm)}
                          <span className="proc-info-duracao">
                            (
                            {fmt.duracao(
                              agendamento.inicioEm,
                              agendamento.fimEm,
                            )}
                            )
                          </span>
                        </span>
                      </div>
                    </div>
                    {agendamento.nomeProfissional && (
                      <div className="proc-info-row">
                        <User size={18} className="proc-info-icone" />
                        <div className="proc-info-texto">
                          <span className="proc-info-rotulo">Profissional</span>
                          <span className="proc-info-valor">
                            {agendamento.nomeProfissional}
                          </span>
                        </div>
                      </div>
                    )}
                    <div className="proc-info-row">
                      <Tag size={18} className="proc-info-icone" />
                      <div className="proc-info-texto">
                        <span className="proc-info-rotulo">Tipo</span>
                        <TipoBadge tipo={agendamento.tipo} />
                      </div>
                    </div>
                    <div className="proc-info-row">
                      <Circle size={18} className="proc-info-icone" />
                      <div className="proc-info-texto">
                        <span className="proc-info-rotulo">Status</span>
                        <StatusBadge status={agendamento.status} />
                      </div>
                    </div>
                    {agendamento.concluidoEm && (
                      <div className="proc-info-row">
                        <CheckCircle size={18} className="proc-info-icone" />
                        <div className="proc-info-texto">
                          <span className="proc-info-rotulo">Concluído em</span>
                          <span className="proc-info-valor">
                            {fmt.data(agendamento.concluidoEm)}
                          </span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>

                {/* Histórico */}
                <div className="proc-card proc-col-dir">
                  <div className="proc-card-header">
                    <History size={16} />
                    Histórico
                  </div>
                  {!ehTratamento ? (
                    <div
                      className="proc-hist-loading"
                      style={{ color: 'var(--color-text-muted)' }}
                    >
                      Disponível apenas para consultas do tipo Tratamento.
                    </div>
                  ) : carregandoHist ? (
                    <div className="proc-hist-loading">
                      <div className="proc-hist-spinner" />
                      Carregando histórico…
                    </div>
                  ) : historico.length === 0 ? (
                    <div
                      className="proc-hist-loading"
                      style={{ color: 'var(--color-text-muted)' }}
                    >
                      Nenhum registro anterior encontrado.
                    </div>
                  ) : (
                    <div className="proc-historico-lista">
                      {historico.map((item, idx) => (
                        <HistoricoItem
                          key={item.id}
                          item={item}
                          isAtual={item.id === agendamento.id}
                          index={idx}
                        />
                      ))}
                    </div>
                  )}
                </div>

                {ehTratamento ? (
                  <>
                    {/* 1. Avaliação diária da ferida */}
                    <div className="proc-card proc-full">
                      <div className="proc-card-header">
                        <Eye size={16} />
                        1. Avaliação diária da ferida
                        {!podeEditar && !jaRealizado && <BloqueadoTag />}
                      </div>
                      <div className="proc-form">
                        <p className="proc-legenda">
                          <strong>Legenda TIME</strong>
                          {' — '}
                          <strong>T</strong> (Tecido): G = Granulação · E =
                          Esfacelo · N = Necrose · EP = Epitelização
                          {' | '}
                          <strong>I</strong> (Infecção/Inflamação): 0 = Ausente
                          · 1 = Local · 2 = Sistêmica
                          {' | '}
                          <strong>M</strong> (Exsudato): 0 = Ausente · 1 =
                          Pequeno · 2 = Moderado · 3 = Intenso
                          {' | '}
                          <strong>E</strong> (Bordas): I = Íntegras · M =
                          Maceradas · D = Descoladas · EP = Epitelizando
                        </p>

                        <div className="proc-form-grid-3">
                          <Campo label="Comprimento (cm)">
                            <input
                              className="proc-campo-input"
                              type="number"
                              min="0"
                              max={DIMENSAO_MAX}
                              step="0.1"
                              value={evolucao.comprimento}
                              onChange={setEv('comprimento')}
                              disabled={!podeEditar}
                              placeholder="0.0"
                            />
                          </Campo>
                          <Campo label="Largura (cm)">
                            <input
                              className="proc-campo-input"
                              type="number"
                              min="0"
                              max={DIMENSAO_MAX}
                              step="0.1"
                              value={evolucao.largura}
                              onChange={setEv('largura')}
                              disabled={!podeEditar}
                              placeholder="0.0"
                            />
                          </Campo>
                          <Campo label="Profundidade (cm)">
                            <input
                              className="proc-campo-input"
                              type="number"
                              min="0"
                              max={DIMENSAO_MAX}
                              step="0.1"
                              value={evolucao.profundidade}
                              onChange={setEv('profundidade')}
                              disabled={!podeEditar}
                              placeholder="0.0"
                            />
                          </Campo>
                        </div>

                        <div className="proc-form-grid">
                          <Campo label="Área aproximada (C×L) em cm²">
                            <input
                              className="proc-campo-input"
                              value={areaAproximadaTexto}
                              readOnly
                              disabled
                              placeholder="Calculada automaticamente"
                            />
                          </Campo>
                          <Campo label="Dor (0–10)">
                            <input
                              className="proc-campo-input"
                              type="number"
                              min="0"
                              max="10"
                              step="1"
                              value={evolucao.dorEscala}
                              onChange={setEv('dorEscala')}
                              disabled={!podeEditar}
                              placeholder="0"
                            />
                          </Campo>
                        </div>

                        <div className="proc-form-grid">
                          <CampoSelect<TecidoLeito>
                            label="Tecido (T — TIME)"
                            opcoes={OPCOES_TECIDO}
                            valor={evolucao.tecido}
                            onChange={setEv('tecido')}
                            disabled={!podeEditar}
                          />
                          <CampoSelect<InfeccaoInflamacaoCurativo>
                            label="Infecção/Inflamação (I — TIME)"
                            opcoes={OPCOES_INFECCAO}
                            valor={evolucao.infeccaoInflamacao}
                            onChange={setEv('infeccaoInflamacao')}
                            disabled={!podeEditar}
                          />
                          <CampoSelect<ExsudatoTime>
                            label="Exsudato (M — TIME)"
                            opcoes={OPCOES_EXSUDATO}
                            valor={evolucao.exsudato}
                            onChange={setEv('exsudato')}
                            disabled={!podeEditar}
                          />
                          <CampoSelect<BordasFerida>
                            label="Bordas (E — TIME)"
                            opcoes={OPCOES_BORDAS}
                            valor={evolucao.bordas}
                            onChange={setEv('bordas')}
                            disabled={!podeEditar}
                          />
                        </div>

                        <div className="proc-check-grid">
                          <CheckItem
                            label="Odor presente"
                            checked={evolucao.odorPresente}
                            onChange={setBool('odorPresente')}
                            disabled={!podeEditar}
                          />
                        </div>

                        <Campo label="Pele perilesional">
                          <input
                            className="proc-campo-input"
                            value={evolucao.pelePerilesional}
                            onChange={setEv('pelePerilesional')}
                            disabled={!podeEditar}
                            placeholder="Ex: íntegra, macerada, hiperemiada…"
                            maxLength={LIMITE_TEXTO_LONGO}
                          />
                        </Campo>
                      </div>
                    </div>

                    {/* 2. Intervenções realizadas */}
                    <div className="proc-card proc-full">
                      <div className="proc-card-header">
                        <Scissors size={16} />
                        2. Intervenções realizadas
                        {!podeEditar && !jaRealizado && <BloqueadoTag />}
                      </div>
                      <div className="proc-form">
                        <LinhaObservacao
                          rotulo="Limpeza/Irrigação"
                          valor={evolucao.limpezaIrrigacao}
                          onChange={setEv('limpezaIrrigacao')}
                          disabled={!podeEditar}
                          placeholder="Ex: SF 0,9% morno em jato"
                          maxLength={LIMITE_TEXTO_LONGO}
                        />
                        <div className="proc-form-grid">
                          <CampoSelect<TipoDesbridamento>
                            label="Desbridamento"
                            opcoes={OPCOES_DESBRIDAMENTO}
                            valor={evolucao.desbridamento}
                            onChange={setEv('desbridamento')}
                            disabled={!podeEditar}
                          />
                          <LinhaObservacao
                            rotulo="Observação do desbridamento"
                            valor={evolucao.desbridamentoObs}
                            onChange={setEv('desbridamentoObs')}
                            disabled={!podeEditar}
                            placeholder="Detalhe do procedimento realizado"
                            maxLength={LIMITE_TEXTO_LONGO}
                          />
                        </div>
                        <LinhaObservacao
                          rotulo="Cobertura primária"
                          valor={evolucao.coberturaPrimaria}
                          onChange={setEv('coberturaPrimaria')}
                          disabled={!podeEditar}
                          placeholder="Qual cobertura foi aplicada?"
                          maxLength={LIMITE_TEXTO_CURTO}
                        />
                        <LinhaObservacao
                          rotulo="Orientações ao paciente"
                          valor={evolucao.orientacoesPaciente}
                          onChange={setEv('orientacoesPaciente')}
                          disabled={!podeEditar}
                          placeholder="Orientações fornecidas ao paciente/cuidador"
                          maxLength={LIMITE_TEXTO_LONGO}
                        />
                      </div>
                    </div>

                    {/* 3. Avaliação da evolução */}
                    <div className="proc-card proc-full">
                      <div className="proc-card-header">
                        <BookOpen size={16} />
                        3. Avaliação da evolução
                        {!podeEditar && !jaRealizado && <BloqueadoTag />}
                      </div>
                      <div className="proc-form">
                        <CampoSelect<AvaliacaoEvolucao>
                          label="Evolução"
                          opcoes={OPCOES_EVOLUCAO}
                          valor={evolucao.evolucao}
                          onChange={setEv('evolucao')}
                          disabled={!podeEditar}
                        />
                        <Campo label="Observações">
                          <textarea
                            className="proc-campo-textarea"
                            value={evolucao.observacoes}
                            onChange={setEv('observacoes')}
                            disabled={!podeEditar}
                            placeholder="Observações sobre a evolução da ferida…"
                            rows={3}
                            maxLength={LIMITE_TEXTO_LONGO}
                          />
                        </Campo>
                      </div>
                    </div>

                    {/* 4. Plano / ações futuras */}
                    <div className="proc-card proc-full">
                      <div className="proc-card-header">
                        <MessageSquare size={16} />
                        4. Plano / ações futuras
                        {!podeEditar && !jaRealizado && <BloqueadoTag />}
                      </div>
                      <div className="proc-form">
                        <p className="proc-legenda">
                          Preencher a observação equivale a marcar a ação.
                        </p>
                        <LinhaObservacao
                          rotulo="Manter conduta"
                          valor={evolucao.planoManterConduta}
                          onChange={setEv('planoManterConduta')}
                          disabled={!podeEditar}
                          maxLength={LIMITE_TEXTO_CURTO}
                        />
                        <LinhaObservacao
                          rotulo="Alterar cobertura"
                          valor={evolucao.planoAlterarCobertura}
                          onChange={setEv('planoAlterarCobertura')}
                          disabled={!podeEditar}
                          maxLength={LIMITE_TEXTO_CURTO}
                        />
                        <LinhaObservacao
                          rotulo="Solicitar exames"
                          valor={evolucao.planoSolicitarExames}
                          onChange={setEv('planoSolicitarExames')}
                          disabled={!podeEditar}
                          maxLength={LIMITE_TEXTO_CURTO}
                        />
                        <LinhaObservacao
                          rotulo="Encaminhamento"
                          valor={evolucao.planoEncaminhamento}
                          onChange={setEv('planoEncaminhamento')}
                          disabled={!podeEditar}
                          maxLength={LIMITE_TEXTO_CURTO}
                        />
                        <LinhaObservacao
                          rotulo="Retorno previsto"
                          valor={evolucao.retornoPrevisto}
                          onChange={setEv('retornoPrevisto')}
                          disabled={!podeEditar}
                          placeholder="Ex: retorno em 7 dias"
                          maxLength={LIMITE_TEXTO_CURTO}
                        />
                      </div>
                    </div>
                  </>
                ) : (
                  /* Avaliação: Anamnese + Ficha de Evolução de Enfermagem */
                  <>
                    <FichaAnamnese
                      valor={anamnese}
                      aoAlterar={alterarAnamnese}
                      desabilitado={!podeEditar}
                      mostrarBloqueio={!podeEditar && !jaRealizado}
                    />
                    <FichaEnfermagem
                      valor={enfermagem}
                      aoAlterar={alterarEnfermagem}
                      desabilitado={!podeEditar}
                      mostrarBloqueio={!podeEditar && !jaRealizado}
                    />
                  </>
                )}
              </div>

              {/* Barra de ações */}
              <div className="proc-actions-bar">
                <div className="proc-actions-spacer" />

                {jaRealizado ? (
                  <>
                    <div className="proc-btn-realizado">
                      <CheckCircle size={16} />
                      Consulta realizada
                    </div>
                    <button
                      className="proc-btn-proximo"
                      onClick={agendarProximo}
                    >
                      <CalendarPlus size={16} />
                      Agendar próximo
                    </button>
                  </>
                ) : !iniciado ? (
                  <button
                    className="proc-btn-iniciar"
                    onClick={() => setIniciado(true)}
                  >
                    <Play size={16} />
                    Iniciar consulta
                  </button>
                ) : (
                  <button
                    className="proc-btn-finalizar"
                    onClick={finalizarConsulta}
                    disabled={finalizando}
                  >
                    <CheckCircle size={16} />
                    {finalizando ? 'Finalizando…' : 'Finalizar consulta'}
                  </button>
                )}
              </div>
            </>
          )}
        </div>

        {modalAberto && (
          <Modal
            titulo="Consulta finalizada!"
            tamanho="sm"
            onClose={() => setModalAberto(false)}
            rodape={
              <>
                <button
                  className="proc-modal-btn-secundario"
                  onClick={() => setModalAberto(false)}
                >
                  Não, obrigado
                </button>
                <button
                  className="proc-modal-btn-primario"
                  onClick={agendarProximo}
                >
                  <CalendarPlus size={15} />
                  Agendar próximo tratamento
                </button>
              </>
            }
          >
            <p className="proc-modal-descricao">
              Deseja agendar o próximo tratamento para este paciente?
            </p>
          </Modal>
        )}
      </RotaProtegida>
    </Layout>
  )
}
