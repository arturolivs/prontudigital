'use client'

import { useState, useEffect, use } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/contexts/AuthContext'
import { useNotificacao } from '@/contexts/ToastContext'
import { agendamentoAPI } from '@/lib/agendamento.service'
import { Agendamento, EvolucaoTratamentoRequisicao } from '@/tipos/agendamento'
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
  localizacaoAnatomica: string
  tipoLesao: string
  medidaComprimento: string
  medidaLargura: string
  medidaProfundidade: string
  aspectoLeitoFerida: string
  exsudatoVolume: string
  exsudatoCaracteristica: string
  condicaoBordas: string
  aspectoPerilesional: string
  sinaisFlogisticos: string
  presencaOdor: string
  limpezaRealizada: string
  coberturasAplicadas: string
  produtosUtilizados: string
  aceitacaoProcedimento: string
  escalaDor: string
  intercorrencias: string
  cuidadosCurativo: string
  sinaisAlerta: string
  orientacaoRetorno: string
}

const EVOLUCAO_INICIAL: EvolucaoForm = {
  localizacaoAnatomica: '',
  tipoLesao: '',
  medidaComprimento: '',
  medidaLargura: '',
  medidaProfundidade: '',
  aspectoLeitoFerida: '',
  exsudatoVolume: '',
  exsudatoCaracteristica: '',
  condicaoBordas: '',
  aspectoPerilesional: '',
  sinaisFlogisticos: '',
  presencaOdor: '',
  limpezaRealizada: '',
  coberturasAplicadas: '',
  produtosUtilizados: '',
  aceitacaoProcedimento: '',
  escalaDor: '',
  intercorrencias: '',
  cuidadosCurativo: '',
  sinaisAlerta: '',
  orientacaoRetorno: '',
}

function evolucaoFromAgendamento(ag: Agendamento): EvolucaoForm {
  const ec = ag.evolucaoClinica
  return {
    localizacaoAnatomica: ec?.localizacaoAnatomica ?? '',
    tipoLesao: ec?.tipoLesao ?? '',
    medidaComprimento: ec?.medidaComprimento?.toString() ?? '',
    medidaLargura: ec?.medidaLargura?.toString() ?? '',
    medidaProfundidade: ec?.medidaProfundidade?.toString() ?? '',
    aspectoLeitoFerida: ec?.aspectoLeitoFerida ?? '',
    exsudatoVolume: ec?.exsudatoVolume ?? '',
    exsudatoCaracteristica: ec?.exsudatoCaracteristica ?? '',
    condicaoBordas: ec?.condicaoBordas ?? '',
    aspectoPerilesional: ec?.aspectoPerilesional ?? '',
    sinaisFlogisticos:
      ec?.sinaisFlogisticos == null ? '' : String(ec.sinaisFlogisticos),
    presencaOdor: ec?.presencaOdor == null ? '' : String(ec.presencaOdor),
    limpezaRealizada: ec?.limpezaRealizada ?? '',
    coberturasAplicadas: ec?.coberturasAplicadas ?? '',
    produtosUtilizados: ec?.produtosUtilizados ?? '',
    aceitacaoProcedimento: ec?.aceitacaoProcedimento ?? '',
    escalaDor: ec?.escalaDor?.toString() ?? '',
    intercorrencias: ec?.intercorrencias ?? '',
    cuidadosCurativo: ec?.cuidadosCurativo ?? '',
    sinaisAlerta: ec?.sinaisAlerta ?? '',
    orientacaoRetorno: ec?.orientacaoRetorno ?? '',
  }
}

function evolucaoParaApi(form: EvolucaoForm): EvolucaoTratamentoRequisicao {
  return {
    localizacaoAnatomica: form.localizacaoAnatomica || undefined,
    tipoLesao: form.tipoLesao || undefined,
    medidaComprimento: form.medidaComprimento
      ? parseFloat(form.medidaComprimento)
      : undefined,
    medidaLargura: form.medidaLargura
      ? parseFloat(form.medidaLargura)
      : undefined,
    medidaProfundidade: form.medidaProfundidade
      ? parseFloat(form.medidaProfundidade)
      : undefined,
    aspectoLeitoFerida: form.aspectoLeitoFerida || undefined,
    exsudatoVolume:
      (form.exsudatoVolume as EvolucaoTratamentoRequisicao['exsudatoVolume']) ||
      undefined,
    exsudatoCaracteristica:
      (form.exsudatoCaracteristica as EvolucaoTratamentoRequisicao['exsudatoCaracteristica']) ||
      undefined,
    condicaoBordas: form.condicaoBordas || undefined,
    aspectoPerilesional: form.aspectoPerilesional || undefined,
    sinaisFlogisticos:
      form.sinaisFlogisticos === ''
        ? undefined
        : form.sinaisFlogisticos === 'true',
    presencaOdor:
      form.presencaOdor === '' ? undefined : form.presencaOdor === 'true',
    limpezaRealizada: form.limpezaRealizada || undefined,
    coberturasAplicadas: form.coberturasAplicadas || undefined,
    produtosUtilizados: form.produtosUtilizados || undefined,
    aceitacaoProcedimento: form.aceitacaoProcedimento || undefined,
    escalaDor: form.escalaDor !== '' ? parseInt(form.escalaDor) : undefined,
    intercorrencias: form.intercorrencias || undefined,
    cuidadosCurativo: form.cuidadosCurativo || undefined,
    sinaisAlerta: form.sinaisAlerta || undefined,
    orientacaoRetorno: form.orientacaoRetorno || undefined,
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

  const [observacoes, setObservacoes] = useState('')
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
        setObservacoes(data.observacoes ?? '')
        setEvolucao(evolucaoFromAgendamento(data))
      })
      .catch(() => setErro('Não foi possível carregar o agendamento.'))
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

  const finalizarConsulta = async () => {
    if (!agendamento) return
    setFinalizando(true)
    try {
      await agendamentoAPI.atualizarObservacoes(agendamento.id, observacoes)
      await agendamentoAPI.registrarEvolucao(
        agendamento.id,
        evolucaoParaApi(evolucao),
      )
      await agendamentoAPI.concluirAgendamento(agendamento.id)
      setAgendamento(prev =>
        prev ? { ...prev, status: 'REALIZADO', observacoes } : prev,
      )
      setIniciado(false)
      setModalAberto(true)
    } catch {
      exibirNotificacao('Erro ao finalizar consulta.', 'error', 5000)
    } finally {
      setFinalizando(false)
    }
  }

  const jaRealizado = agendamento?.status === 'REALIZADO'
  const ehTratamento = agendamento?.tipo === 'TRATAMENTO'
  const podeEditar = iniciado && !jaRealizado

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

                {/* Observações */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <FileText size={16} />
                    Observações
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-obs-area">
                    <textarea
                      className="proc-obs-textarea"
                      value={observacoes}
                      onChange={e => setObservacoes(e.target.value)}
                      placeholder={
                        jaRealizado
                          ? 'Observações registradas nesta consulta.'
                          : podeEditar
                            ? 'Registre observações, evoluções e anotações desta consulta…'
                            : 'Inicie a consulta para registrar observações.'
                      }
                      disabled={!podeEditar}
                      rows={6}
                    />
                  </div>
                </div>

                {/* Seção 2 – Avaliação da lesão */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <Eye size={16} />
                    Avaliação da lesão
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <div className="proc-form-grid">
                      <Campo label="Localização anatômica">
                        <input
                          className="proc-campo-input"
                          value={evolucao.localizacaoAnatomica}
                          onChange={setEv('localizacaoAnatomica')}
                          disabled={!podeEditar}
                          placeholder="Ex: membro inferior direito, região sacral…"
                        />
                      </Campo>
                      <Campo label="Tipo de lesão">
                        <input
                          className="proc-campo-input"
                          value={evolucao.tipoLesao}
                          onChange={setEv('tipoLesao')}
                          disabled={!podeEditar}
                          placeholder="Ex: LPP, úlcera venosa, pé diabético, ferida cirúrgica…"
                        />
                      </Campo>
                    </div>

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

                    <div className="proc-form-grid">
                      <Campo label="Volume do exsudato">
                        <select
                          className="proc-campo-select"
                          value={evolucao.exsudatoVolume}
                          onChange={setEv('exsudatoVolume')}
                          disabled={!podeEditar}
                        >
                          <option value="">Não informado</option>
                          <option value="AUSENTE">Ausente</option>
                          <option value="PEQUENO">Pequeno</option>
                          <option value="MODERADO">Moderado</option>
                          <option value="GRANDE">Grande</option>
                        </select>
                      </Campo>
                      <Campo label="Característica do exsudato">
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
                          <option value="PURULENTO">Purulento</option>
                        </select>
                      </Campo>
                      <Campo label="Sinais flogísticos">
                        <select
                          className="proc-campo-select"
                          value={evolucao.sinaisFlogisticos}
                          onChange={setEv('sinaisFlogisticos')}
                          disabled={!podeEditar}
                        >
                          <option value="">Não informado</option>
                          <option value="true">Sim</option>
                          <option value="false">Não</option>
                        </select>
                      </Campo>
                      <Campo label="Presença de odor">
                        <select
                          className="proc-campo-select"
                          value={evolucao.presencaOdor}
                          onChange={setEv('presencaOdor')}
                          disabled={!podeEditar}
                        >
                          <option value="">Não informado</option>
                          <option value="true">Sim</option>
                          <option value="false">Não</option>
                        </select>
                      </Campo>
                    </div>

                    <Campo label="Aspecto do leito da ferida">
                      <textarea
                        className="proc-campo-textarea"
                        value={evolucao.aspectoLeitoFerida}
                        onChange={setEv('aspectoLeitoFerida')}
                        disabled={!podeEditar}
                        placeholder="Granulação, epitelização, esfacelo, necrose…"
                        rows={2}
                      />
                    </Campo>

                    <div className="proc-form-grid">
                      <Campo label="Condição das bordas">
                        <textarea
                          className="proc-campo-textarea"
                          value={evolucao.condicaoBordas}
                          onChange={setEv('condicaoBordas')}
                          disabled={!podeEditar}
                          placeholder="Descreva a condição das bordas da lesão…"
                          rows={2}
                        />
                      </Campo>
                      <Campo label="Aspecto da pele perilesional">
                        <textarea
                          className="proc-campo-textarea"
                          value={evolucao.aspectoPerilesional}
                          onChange={setEv('aspectoPerilesional')}
                          disabled={!podeEditar}
                          placeholder="Descreva o aspecto da pele ao redor da lesão…"
                          rows={2}
                        />
                      </Campo>
                    </div>
                  </div>
                </div>

                {/* Seção 3 – Procedimento realizado */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <Scissors size={16} />
                    Procedimento realizado
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <div className="proc-form-grid">
                      <Campo label="Limpeza realizada e solução utilizada">
                        <textarea
                          className="proc-campo-textarea"
                          value={evolucao.limpezaRealizada}
                          onChange={setEv('limpezaRealizada')}
                          disabled={!podeEditar}
                          placeholder="Descreva a técnica de limpeza e solução utilizada…"
                          rows={3}
                        />
                      </Campo>
                      <Campo label="Coberturas aplicadas">
                        <textarea
                          className="proc-campo-textarea"
                          value={evolucao.coberturasAplicadas}
                          onChange={setEv('coberturasAplicadas')}
                          disabled={!podeEditar}
                          placeholder="Liste as coberturas aplicadas…"
                          rows={3}
                        />
                      </Campo>
                    </div>
                    <Campo label="Produtos utilizados">
                      <textarea
                        className="proc-campo-textarea"
                        value={evolucao.produtosUtilizados}
                        onChange={setEv('produtosUtilizados')}
                        disabled={!podeEditar}
                        placeholder="Liste os produtos e materiais utilizados no procedimento…"
                        rows={2}
                      />
                    </Campo>
                  </div>
                </div>

                {/* Seção 4 – Resposta do paciente */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <MessageSquare size={16} />
                    Resposta do paciente
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <div className="proc-form-grid">
                      <Campo label="Aceitação do procedimento">
                        <input
                          className="proc-campo-input"
                          value={evolucao.aceitacaoProcedimento}
                          onChange={setEv('aceitacaoProcedimento')}
                          disabled={!podeEditar}
                          placeholder="Boa, parcial, não aceitou…"
                        />
                      </Campo>
                      <Campo label="Escala de dor EVA (0–10)">
                        <input
                          className="proc-campo-input"
                          type="number"
                          min="0"
                          max="10"
                          value={evolucao.escalaDor}
                          onChange={setEv('escalaDor')}
                          disabled={!podeEditar}
                          placeholder="0"
                        />
                      </Campo>
                    </div>
                    <Campo label="Intercorrências durante o procedimento">
                      <textarea
                        className="proc-campo-textarea"
                        value={evolucao.intercorrencias}
                        onChange={setEv('intercorrencias')}
                        disabled={!podeEditar}
                        placeholder="Registre qualquer intercorrência ocorrida durante o procedimento…"
                        rows={2}
                      />
                    </Campo>
                  </div>
                </div>

                {/* Seção 5 – Orientações fornecidas */}
                <div className="proc-card proc-full">
                  <div className="proc-card-header">
                    <BookOpen size={16} />
                    Orientações fornecidas
                    {!podeEditar && !jaRealizado && <BloqueadoTag />}
                  </div>
                  <div className="proc-form">
                    <div className="proc-form-grid">
                      <Campo label="Cuidados com o curativo">
                        <textarea
                          className="proc-campo-textarea"
                          value={evolucao.cuidadosCurativo}
                          onChange={setEv('cuidadosCurativo')}
                          disabled={!podeEditar}
                          placeholder="Oriente os cuidados domiciliares com o curativo…"
                          rows={3}
                        />
                      </Campo>
                      <Campo label="Sinais de alerta">
                        <textarea
                          className="proc-campo-textarea"
                          value={evolucao.sinaisAlerta}
                          onChange={setEv('sinaisAlerta')}
                          disabled={!podeEditar}
                          placeholder="Sinais que devem motivar busca imediata por atendimento…"
                          rows={3}
                        />
                      </Campo>
                    </div>
                    <Campo label="Retorno e acompanhamento">
                      <textarea
                        className="proc-campo-textarea"
                        value={evolucao.orientacaoRetorno}
                        onChange={setEv('orientacaoRetorno')}
                        disabled={!podeEditar}
                        placeholder="Oriente sobre o próximo retorno e acompanhamento…"
                        rows={2}
                      />
                    </Campo>
                  </div>
                </div>
              </div>

              {/* Barra de ações */}
              <div className="proc-actions-bar">
                <div className="proc-actions-spacer" />

                {jaRealizado ? (
                  <div className="proc-btn-realizado">
                    <CheckCircle size={16} />
                    Consulta realizada
                  </div>
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
                  onClick={() => {
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
                    if (avaliacaoId)
                      params.set('avaliacaoId', String(avaliacaoId))
                    if (agendamento.tipoProcedimento)
                      params.set(
                        'tipoProcedimento',
                        agendamento.tipoProcedimento,
                      )
                    if (agendamento.localAtendimento)
                      params.set(
                        'localAtendimento',
                        agendamento.localAtendimento,
                      )
                    if (agendamento.pacienteAcamado != null)
                      params.set(
                        'pacienteAcamado',
                        String(agendamento.pacienteAcamado),
                      )
                    router.push(`/agenda?${params.toString()}`)
                  }}
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
