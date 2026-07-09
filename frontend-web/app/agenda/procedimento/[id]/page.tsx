'use client'

import { useState, useEffect, use } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/contexts/AuthContext'
import { useNotificacao } from '@/contexts/ToastContext'
import { agendamentoAPI } from '@/lib/agendamento.service'
import { MENSAGENS } from '@/lib/mensagens'
import {
  Agendamento,
  EvolucaoTratamentoRequisicao,
  CaracteristicaBorda,
  CaracteristicaPerilesional,
  SinalInfeccao,
  SinalEvolucao,
} from '@/tipos/agendamento'
import { ROTULO_TIPO_PROCEDIMENTO } from '@/tipos/TipoProcedimento'
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
import './procedimento.css'

// ── Tipos do formulário de evolução ──────────────────────────────

type EvolucaoForm = {
  // Dados da ferida
  localizacaoAnatomica: string
  etiologia: string
  tempoEvolucao: string
  // Mensuração
  medidaComprimento: string
  medidaLargura: string
  medidaProfundidade: string
  tunelizacao: boolean
  descolamentoBordas: boolean
  // Leito da ferida
  epitelizacaoPercentual: string
  granulacaoPercentual: string
  esfaceloPercentual: string
  necrosePercentual: string
  tendaoExposto: boolean
  musculoExposto: boolean
  ossoExposto: boolean
  // Exsudato
  exsudatoVolume: string
  exsudatoCaracteristica: string
  odorIntensidade: string
  // Bordas
  caracteristicasBordas: CaracteristicaBorda[]
  // Pele perilesional
  caracteristicasPerilesional: CaracteristicaPerilesional[]
  // Sinais de infecção
  sinaisInfeccao: SinalInfeccao[]
  // Dor
  classificacaoDor: string
  // Avaliação vascular
  grauEdema: string
  avaliacaoPulsos: string
  // Evolução da ferida
  evolucaoFerida: string
  sinaisEvolucao: SinalEvolucao[]
  // Conduta
  limpezaLesao: boolean
  desbridamento: boolean
  coberturaAplicada: boolean
  coberturaDescricao: string
  terapiaAdjuvante: boolean
  terapiaAdjuvanteDescricao: string
  orientacoesFornecidas: boolean
  // Observações
  observacoesFerida: string
}

const EVOLUCAO_INICIAL: EvolucaoForm = {
  localizacaoAnatomica: '',
  etiologia: '',
  tempoEvolucao: '',
  medidaComprimento: '',
  medidaLargura: '',
  medidaProfundidade: '',
  tunelizacao: false,
  descolamentoBordas: false,
  epitelizacaoPercentual: '',
  granulacaoPercentual: '',
  esfaceloPercentual: '',
  necrosePercentual: '',
  tendaoExposto: false,
  musculoExposto: false,
  ossoExposto: false,
  exsudatoVolume: '',
  exsudatoCaracteristica: '',
  odorIntensidade: '',
  caracteristicasBordas: [],
  caracteristicasPerilesional: [],
  sinaisInfeccao: [],
  classificacaoDor: '',
  grauEdema: '',
  avaliacaoPulsos: '',
  evolucaoFerida: '',
  sinaisEvolucao: [],
  limpezaLesao: false,
  desbridamento: false,
  coberturaAplicada: false,
  coberturaDescricao: '',
  terapiaAdjuvante: false,
  terapiaAdjuvanteDescricao: '',
  orientacoesFornecidas: false,
  observacoesFerida: '',
}

// ── Rótulos das opções (checklist) ───────────────────────────────

const OPCOES_BORDA: { value: CaracteristicaBorda; label: string }[] = [
  { value: 'INTEGRAS', label: 'Íntegras' },
  { value: 'MACERADAS', label: 'Maceradas' },
  { value: 'ADERIDAS', label: 'Aderidas' },
  { value: 'DESCOLADAS', label: 'Descoladas' },
  { value: 'EPIBOLIA', label: 'Epibolia' },
  { value: 'HIPERQUERATOSE', label: 'Hiperqueratose' },
]

const OPCOES_PERILESIONAL: {
  value: CaracteristicaPerilesional
  label: string
}[] = [
  { value: 'INTEGRA', label: 'Íntegra' },
  { value: 'HIPEREMIADA', label: 'Hiperemiada' },
  { value: 'MACERADA', label: 'Macerada' },
  { value: 'RESSECADA', label: 'Ressecada' },
  { value: 'EDEMACIADA', label: 'Edemaciada' },
  { value: 'DERMATITE', label: 'Dermatite' },
]

const OPCOES_INFECCAO: { value: SinalInfeccao; label: string }[] = [
  { value: 'AUSENTES', label: 'Ausentes' },
  { value: 'ERITEMA', label: 'Eritema' },
  { value: 'CALOR_LOCAL', label: 'Calor local' },
  { value: 'EDEMA', label: 'Edema' },
  { value: 'DOR_AUMENTADA', label: 'Dor aumentada' },
  { value: 'EXSUDATO_PURULENTO', label: 'Exsudato purulento' },
  { value: 'MAU_ODOR', label: 'Mau odor' },
]

const OPCOES_SINAL_EVOLUCAO: { value: SinalEvolucao; label: string }[] = [
  { value: 'REDUCAO_DIMENSOES', label: 'Redução das dimensões' },
  { value: 'AUMENTO_GRANULACAO', label: 'Aumento do tecido de granulação' },
  { value: 'REDUCAO_EXSUDATO', label: 'Redução do exsudato' },
  { value: 'EPITELIZACAO_PROGRESSIVA', label: 'Epitelização progressiva' },
  {
    value: 'NECESSITA_REAVALIACAO',
    label: 'Necessita reavaliação terapêutica',
  },
]

function alternar<T>(lista: T[], valor: T): T[] {
  return lista.includes(valor)
    ? lista.filter(v => v !== valor)
    : [...lista, valor]
}

function numParaTexto(n?: number): string {
  return n == null ? '' : n.toString()
}

function evolucaoFromAgendamento(ag: Agendamento): EvolucaoForm {
  const ec = ag.evolucaoClinica
  return {
    localizacaoAnatomica: ec?.localizacaoAnatomica ?? '',
    etiologia: ec?.etiologia ?? '',
    tempoEvolucao: ec?.tempoEvolucao ?? '',
    medidaComprimento: numParaTexto(ec?.medidaComprimento),
    medidaLargura: numParaTexto(ec?.medidaLargura),
    medidaProfundidade: numParaTexto(ec?.medidaProfundidade),
    tunelizacao: ec?.tunelizacao ?? false,
    descolamentoBordas: ec?.descolamentoBordas ?? false,
    epitelizacaoPercentual: numParaTexto(ec?.epitelizacaoPercentual),
    granulacaoPercentual: numParaTexto(ec?.granulacaoPercentual),
    esfaceloPercentual: numParaTexto(ec?.esfaceloPercentual),
    necrosePercentual: numParaTexto(ec?.necrosePercentual),
    tendaoExposto: ec?.tendaoExposto ?? false,
    musculoExposto: ec?.musculoExposto ?? false,
    ossoExposto: ec?.ossoExposto ?? false,
    exsudatoVolume: ec?.exsudatoVolume ?? '',
    exsudatoCaracteristica: ec?.exsudatoCaracteristica ?? '',
    odorIntensidade: ec?.odorIntensidade ?? '',
    caracteristicasBordas: ec?.caracteristicasBordas ?? [],
    caracteristicasPerilesional: ec?.caracteristicasPerilesional ?? [],
    sinaisInfeccao: ec?.sinaisInfeccao ?? [],
    classificacaoDor: ec?.classificacaoDor ?? '',
    grauEdema: ec?.grauEdema ?? '',
    avaliacaoPulsos: ec?.avaliacaoPulsos ?? '',
    evolucaoFerida: ec?.evolucaoFerida ?? '',
    sinaisEvolucao: ec?.sinaisEvolucao ?? [],
    limpezaLesao: ec?.limpezaLesao ?? false,
    desbridamento: ec?.desbridamento ?? false,
    coberturaAplicada: ec?.coberturaAplicada ?? false,
    coberturaDescricao: ec?.coberturaDescricao ?? '',
    terapiaAdjuvante: ec?.terapiaAdjuvante ?? false,
    terapiaAdjuvanteDescricao: ec?.terapiaAdjuvanteDescricao ?? '',
    orientacoesFornecidas: ec?.orientacoesFornecidas ?? false,
    observacoesFerida: ec?.observacoes ?? '',
  }
}

function txt(valor: string): string | undefined {
  return valor.trim() ? valor : undefined
}

function num(valor: string): number | undefined {
  return valor.trim() ? Number(valor) : undefined
}

function lista<T>(valores: T[]): T[] | undefined {
  return valores.length ? valores : undefined
}

function evolucaoParaApi(form: EvolucaoForm): EvolucaoTratamentoRequisicao {
  return {
    localizacaoAnatomica: txt(form.localizacaoAnatomica),
    etiologia: txt(form.etiologia),
    tempoEvolucao: txt(form.tempoEvolucao),
    medidaComprimento: num(form.medidaComprimento),
    medidaLargura: num(form.medidaLargura),
    medidaProfundidade: num(form.medidaProfundidade),
    tunelizacao: form.tunelizacao,
    descolamentoBordas: form.descolamentoBordas,
    epitelizacaoPercentual: num(form.epitelizacaoPercentual),
    granulacaoPercentual: num(form.granulacaoPercentual),
    esfaceloPercentual: num(form.esfaceloPercentual),
    necrosePercentual: num(form.necrosePercentual),
    tendaoExposto: form.tendaoExposto,
    musculoExposto: form.musculoExposto,
    ossoExposto: form.ossoExposto,
    exsudatoVolume:
      (form.exsudatoVolume as EvolucaoTratamentoRequisicao['exsudatoVolume']) ||
      undefined,
    exsudatoCaracteristica:
      (form.exsudatoCaracteristica as EvolucaoTratamentoRequisicao['exsudatoCaracteristica']) ||
      undefined,
    odorIntensidade:
      (form.odorIntensidade as EvolucaoTratamentoRequisicao['odorIntensidade']) ||
      undefined,
    caracteristicasBordas: lista(form.caracteristicasBordas),
    caracteristicasPerilesional: lista(form.caracteristicasPerilesional),
    sinaisInfeccao: lista(form.sinaisInfeccao),
    classificacaoDor:
      (form.classificacaoDor as EvolucaoTratamentoRequisicao['classificacaoDor']) ||
      undefined,
    grauEdema:
      (form.grauEdema as EvolucaoTratamentoRequisicao['grauEdema']) ||
      undefined,
    avaliacaoPulsos:
      (form.avaliacaoPulsos as EvolucaoTratamentoRequisicao['avaliacaoPulsos']) ||
      undefined,
    evolucaoFerida:
      (form.evolucaoFerida as EvolucaoTratamentoRequisicao['evolucaoFerida']) ||
      undefined,
    sinaisEvolucao: lista(form.sinaisEvolucao),
    limpezaLesao: form.limpezaLesao,
    desbridamento: form.desbridamento,
    coberturaAplicada: form.coberturaAplicada,
    coberturaDescricao: txt(form.coberturaDescricao),
    terapiaAdjuvante: form.terapiaAdjuvante,
    terapiaAdjuvanteDescricao: txt(form.terapiaAdjuvanteDescricao),
    orientacoesFornecidas: form.orientacoesFornecidas,
    observacoes: txt(form.observacoesFerida),
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

function BloqueadoTag() {
  return (
    <span className="proc-obs-bloqueado-tag">
      Bloqueado — clique em Iniciar
    </span>
  )
}

function Campo({
  label,
  children,
  className = '',
}: {
  label: string
  children: React.ReactNode
  className?: string
}) {
  return (
    <div className={`proc-campo ${className}`}>
      <label className="proc-campo-label">{label}</label>
      {children}
    </div>
  )
}

function CheckItem({
  label,
  checked,
  onChange,
  disabled,
}: {
  label: string
  checked: boolean
  onChange: (valor: boolean) => void
  disabled?: boolean
}) {
  return (
    <label className="proc-check">
      <input
        type="checkbox"
        checked={checked}
        onChange={e => onChange(e.target.checked)}
        disabled={disabled}
      />
      <span>{label}</span>
    </label>
  )
}

function GrupoCheck<T extends string>({
  label,
  opcoes,
  valores,
  onToggle,
  disabled,
}: {
  label: string
  opcoes: { value: T; label: string }[]
  valores: T[]
  onToggle: (valor: T) => void
  disabled?: boolean
}) {
  return (
    <Campo label={label}>
      <div className="proc-check-grid">
        {opcoes.map(op => (
          <CheckItem
            key={op.value}
            label={op.label}
            checked={valores.includes(op.value)}
            onChange={() => onToggle(op.value)}
            disabled={disabled}
          />
        ))}
      </div>
    </Campo>
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
      })
      .catch(() => setErro(MENSAGENS.erro.carregarAgendamento))
      .finally(() => setCarregando(false))
  }, [id])

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

  const toggleMulti =
    <T,>(campo: keyof EvolucaoForm) =>
    (valor: T) =>
      setEvolucao(prev => ({
        ...prev,
        [campo]: alternar(prev[campo] as T[], valor),
      }))

  const finalizarConsulta = async () => {
    if (!agendamento) return
    setFinalizando(true)
    try {
      await agendamentoAPI.registrarEvolucao(
        agendamento.id,
        evolucaoParaApi(evolucao),
      )
      await agendamentoAPI.concluirAgendamento(agendamento.id)
      setAgendamento(prev => (prev ? { ...prev, status: 'REALIZADO' } : prev))
      setIniciado(false)
      setModalAberto(true)
    } catch {
      exibirNotificacao(MENSAGENS.erro.finalizarConsulta, 'error', 5000)
    } finally {
      setFinalizando(false)
    }
  }

  const jaRealizado = agendamento?.status === 'REALIZADO'
  const ehTratamento = agendamento?.tipo === 'TRATAMENTO'
  const podeEditar = iniciado && !jaRealizado

  const agendarProximo = () => {
    if (!agendamento) return
    const avaliacaoId =
      agendamento.tipo === 'AVALIACAO'
        ? agendamento.id
        : agendamento.avaliacaoId
    const params = new URLSearchParams({ novoTratamento: '1', tipo: 'TRATAMENTO' })
    params.set('pacienteUuid', agendamento.pacienteUuid)
    params.set('profissionalUuid', agendamento.profissionalUuid)
    if (avaliacaoId) params.set('avaliacaoId', String(avaliacaoId))
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
                    {agendamento.tipoProcedimento && (
                      <span className="proc-hero-procedimento">
                        {ROTULO_TIPO_PROCEDIMENTO[agendamento.tipoProcedimento]}
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

                {/* Dados da ferida */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <Eye size={16} />
                    Dados da ferida
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <div className="proc-form-grid-3">
                      <Campo label="Localização">
                        <input
                          className="proc-campo-input"
                          value={evolucao.localizacaoAnatomica}
                          onChange={setEv('localizacaoAnatomica')}
                          disabled={!podeEditar}
                          placeholder="Ex: membro inferior direito, região sacral…"
                        />
                      </Campo>
                      <Campo label="Etiologia">
                        <input
                          className="proc-campo-input"
                          value={evolucao.etiologia}
                          onChange={setEv('etiologia')}
                          disabled={!podeEditar}
                          placeholder="Ex: LPP, úlcera venosa, pé diabético…"
                        />
                      </Campo>
                      <Campo label="Tempo de evolução">
                        <input
                          className="proc-campo-input"
                          value={evolucao.tempoEvolucao}
                          onChange={setEv('tempoEvolucao')}
                          disabled={!podeEditar}
                          placeholder="Ex: 3 meses"
                        />
                      </Campo>
                    </div>
                  </div>
                </div>

                {/* Mensuração */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <Activity size={16} />
                    Mensuração
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <div className="proc-form-grid-3">
                      <Campo label="Comprimento (cm)">
                        <input
                          className="proc-campo-input"
                          type="number"
                          min="0"
                          step="0.1"
                          value={evolucao.medidaComprimento}
                          onChange={setEv('medidaComprimento')}
                          disabled={!podeEditar}
                          placeholder="0.0"
                        />
                      </Campo>
                      <Campo label="Largura (cm)">
                        <input
                          className="proc-campo-input"
                          type="number"
                          min="0"
                          step="0.1"
                          value={evolucao.medidaLargura}
                          onChange={setEv('medidaLargura')}
                          disabled={!podeEditar}
                          placeholder="0.0"
                        />
                      </Campo>
                      <Campo label="Profundidade (cm)">
                        <input
                          className="proc-campo-input"
                          type="number"
                          min="0"
                          step="0.1"
                          value={evolucao.medidaProfundidade}
                          onChange={setEv('medidaProfundidade')}
                          disabled={!podeEditar}
                          placeholder="0.0"
                        />
                      </Campo>
                    </div>
                    <div className="proc-check-grid">
                      <CheckItem
                        label="Tunelização"
                        checked={evolucao.tunelizacao}
                        onChange={setBool('tunelizacao')}
                        disabled={!podeEditar}
                      />
                      <CheckItem
                        label="Descolamento de bordas (undermining)"
                        checked={evolucao.descolamentoBordas}
                        onChange={setBool('descolamentoBordas')}
                        disabled={!podeEditar}
                      />
                    </div>
                  </div>
                </div>

                {/* Leito da ferida */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <Activity size={16} />
                    Leito da ferida
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <div className="proc-form-grid">
                      <Campo label="Epitelização (%)">
                        <input
                          className="proc-campo-input"
                          type="number"
                          min="0"
                          max="100"
                          value={evolucao.epitelizacaoPercentual}
                          onChange={setEv('epitelizacaoPercentual')}
                          disabled={!podeEditar}
                          placeholder="0"
                        />
                      </Campo>
                      <Campo label="Granulação (%)">
                        <input
                          className="proc-campo-input"
                          type="number"
                          min="0"
                          max="100"
                          value={evolucao.granulacaoPercentual}
                          onChange={setEv('granulacaoPercentual')}
                          disabled={!podeEditar}
                          placeholder="0"
                        />
                      </Campo>
                      <Campo label="Esfacelo/Fibrina (%)">
                        <input
                          className="proc-campo-input"
                          type="number"
                          min="0"
                          max="100"
                          value={evolucao.esfaceloPercentual}
                          onChange={setEv('esfaceloPercentual')}
                          disabled={!podeEditar}
                          placeholder="0"
                        />
                      </Campo>
                      <Campo label="Necrose (%)">
                        <input
                          className="proc-campo-input"
                          type="number"
                          min="0"
                          max="100"
                          value={evolucao.necrosePercentual}
                          onChange={setEv('necrosePercentual')}
                          disabled={!podeEditar}
                          placeholder="0"
                        />
                      </Campo>
                    </div>
                    <div className="proc-check-grid">
                      <CheckItem
                        label="Tendão exposto"
                        checked={evolucao.tendaoExposto}
                        onChange={setBool('tendaoExposto')}
                        disabled={!podeEditar}
                      />
                      <CheckItem
                        label="Músculo exposto"
                        checked={evolucao.musculoExposto}
                        onChange={setBool('musculoExposto')}
                        disabled={!podeEditar}
                      />
                      <CheckItem
                        label="Osso exposto"
                        checked={evolucao.ossoExposto}
                        onChange={setBool('ossoExposto')}
                        disabled={!podeEditar}
                      />
                    </div>
                  </div>
                </div>

                {/* Exsudato */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <Pill size={16} />
                    Exsudato
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <div className="proc-form-grid-3">
                      <Campo label="Quantidade">
                        <select
                          className="proc-campo-select"
                          value={evolucao.exsudatoVolume}
                          onChange={setEv('exsudatoVolume')}
                          disabled={!podeEditar}
                        >
                          <option value="">Não informado</option>
                          <option value="AUSENTE">Ausente</option>
                          <option value="PEQUENO">Pequena</option>
                          <option value="MODERADO">Moderada</option>
                          <option value="GRANDE">Grande</option>
                        </select>
                      </Campo>
                      <Campo label="Aspecto">
                        <select
                          className="proc-campo-select"
                          value={evolucao.exsudatoCaracteristica}
                          onChange={setEv('exsudatoCaracteristica')}
                          disabled={!podeEditar}
                        >
                          <option value="">Não informado</option>
                          <option value="SEROSO">Seroso</option>
                          <option value="SEROSSANGUINOLENTO">
                            Serossanguinolento
                          </option>
                          <option value="SANGUINOLENTO">Sanguinolento</option>
                          <option value="PURULENTO">Purulento</option>
                        </select>
                      </Campo>
                      <Campo label="Odor">
                        <select
                          className="proc-campo-select"
                          value={evolucao.odorIntensidade}
                          onChange={setEv('odorIntensidade')}
                          disabled={!podeEditar}
                        >
                          <option value="">Não informado</option>
                          <option value="AUSENTE">Ausente</option>
                          <option value="LEVE">Leve</option>
                          <option value="MODERADO">Moderado</option>
                          <option value="INTENSO">Intenso</option>
                        </select>
                      </Campo>
                    </div>
                  </div>
                </div>

                {/* Bordas e pele perilesional */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <Eye size={16} />
                    Bordas e pele perilesional
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <GrupoCheck<CaracteristicaBorda>
                      label="Bordas"
                      opcoes={OPCOES_BORDA}
                      valores={evolucao.caracteristicasBordas}
                      onToggle={toggleMulti<CaracteristicaBorda>(
                        'caracteristicasBordas',
                      )}
                      disabled={!podeEditar}
                    />
                    <GrupoCheck<CaracteristicaPerilesional>
                      label="Pele perilesional"
                      opcoes={OPCOES_PERILESIONAL}
                      valores={evolucao.caracteristicasPerilesional}
                      onToggle={toggleMulti<CaracteristicaPerilesional>(
                        'caracteristicasPerilesional',
                      )}
                      disabled={!podeEditar}
                    />
                  </div>
                </div>

                {/* Sinais de infecção, dor e avaliação vascular */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <Activity size={16} />
                    Sinais de infecção, dor e avaliação vascular
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <GrupoCheck<SinalInfeccao>
                      label="Sinais de infecção"
                      opcoes={OPCOES_INFECCAO}
                      valores={evolucao.sinaisInfeccao}
                      onToggle={toggleMulti<SinalInfeccao>('sinaisInfeccao')}
                      disabled={!podeEditar}
                    />
                    <div className="proc-form-grid-3">
                      <Campo label="Dor">
                        <select
                          className="proc-campo-select"
                          value={evolucao.classificacaoDor}
                          onChange={setEv('classificacaoDor')}
                          disabled={!podeEditar}
                        >
                          <option value="">Não informado</option>
                          <option value="AUSENTE">Ausente</option>
                          <option value="LEVE">EVA 1–3 (leve)</option>
                          <option value="MODERADA">EVA 4–6 (moderada)</option>
                          <option value="INTENSA">EVA 7–10 (intensa)</option>
                        </select>
                      </Campo>
                      <Campo label="Edema">
                        <select
                          className="proc-campo-select"
                          value={evolucao.grauEdema}
                          onChange={setEv('grauEdema')}
                          disabled={!podeEditar}
                        >
                          <option value="">Não informado</option>
                          <option value="SEM_EDEMA">Sem edema</option>
                          <option value="MAIS_1">Edema +1</option>
                          <option value="MAIS_2">Edema +2</option>
                          <option value="MAIS_3">Edema +3</option>
                          <option value="MAIS_4">Edema +4</option>
                        </select>
                      </Campo>
                      <Campo label="Pulsos">
                        <select
                          className="proc-campo-select"
                          value={evolucao.avaliacaoPulsos}
                          onChange={setEv('avaliacaoPulsos')}
                          disabled={!podeEditar}
                        >
                          <option value="">Não informado</option>
                          <option value="PALPAVEIS">Palpáveis</option>
                          <option value="DIMINUIDOS">Diminuídos</option>
                          <option value="AUSENTES">Ausentes</option>
                        </select>
                      </Campo>
                    </div>
                  </div>
                </div>

                {/* Evolução da ferida */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <BookOpen size={16} />
                    Evolução da ferida
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <Campo label="Evolução geral">
                      <select
                        className="proc-campo-select"
                        value={evolucao.evolucaoFerida}
                        onChange={setEv('evolucaoFerida')}
                        disabled={!podeEditar}
                      >
                        <option value="">Não informado</option>
                        <option value="MELHORANDO">Melhorando</option>
                        <option value="ESTAVEL">Estável</option>
                        <option value="PIORANDO">Piorando</option>
                      </select>
                    </Campo>
                    <GrupoCheck<SinalEvolucao>
                      label="Sinais de evolução"
                      opcoes={OPCOES_SINAL_EVOLUCAO}
                      valores={evolucao.sinaisEvolucao}
                      onToggle={toggleMulti<SinalEvolucao>('sinaisEvolucao')}
                      disabled={!podeEditar}
                    />
                  </div>
                </div>

                {/* Conduta */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <Scissors size={16} />
                    Conduta
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <div className="proc-check-grid">
                      <CheckItem
                        label="Limpeza da lesão"
                        checked={evolucao.limpezaLesao}
                        onChange={setBool('limpezaLesao')}
                        disabled={!podeEditar}
                      />
                      <CheckItem
                        label="Desbridamento (quando indicado)"
                        checked={evolucao.desbridamento}
                        onChange={setBool('desbridamento')}
                        disabled={!podeEditar}
                      />
                      <CheckItem
                        label="Orientações fornecidas ao paciente/cuidador"
                        checked={evolucao.orientacoesFornecidas}
                        onChange={setBool('orientacoesFornecidas')}
                        disabled={!podeEditar}
                      />
                    </div>
                    <div className="proc-form-grid">
                      <Campo label="Cobertura aplicada">
                        <CheckItem
                          label="Aplicou cobertura"
                          checked={evolucao.coberturaAplicada}
                          onChange={setBool('coberturaAplicada')}
                          disabled={!podeEditar}
                        />
                        <input
                          className="proc-campo-input"
                          value={evolucao.coberturaDescricao}
                          onChange={setEv('coberturaDescricao')}
                          disabled={!podeEditar || !evolucao.coberturaAplicada}
                          placeholder="Qual cobertura?"
                        />
                      </Campo>
                      <Campo label="Terapia adjuvante">
                        <CheckItem
                          label="Aplicou terapia adjuvante"
                          checked={evolucao.terapiaAdjuvante}
                          onChange={setBool('terapiaAdjuvante')}
                          disabled={!podeEditar}
                        />
                        <input
                          className="proc-campo-input"
                          value={evolucao.terapiaAdjuvanteDescricao}
                          onChange={setEv('terapiaAdjuvanteDescricao')}
                          disabled={!podeEditar || !evolucao.terapiaAdjuvante}
                          placeholder="Qual terapia?"
                        />
                      </Campo>
                    </div>
                  </div>
                </div>

                {/* Observações */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <MessageSquare size={16} />
                    Observações
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <Campo label="Observações sobre a ferida">
                      <textarea
                        className="proc-campo-textarea"
                        value={evolucao.observacoesFerida}
                        onChange={setEv('observacoesFerida')}
                        disabled={!podeEditar}
                        placeholder="Observações gerais sobre a avaliação da ferida…"
                        rows={3}
                      />
                    </Campo>
                  </div>
                </div>
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
                    <button className="proc-btn-proximo" onClick={agendarProximo}>
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
