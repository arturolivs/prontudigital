'use client'

import { useState, useEffect, use } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/contexts/AuthContext'
import { useNotificacao } from '@/contexts/ToastContext'
import { agendamentoAPI } from '@/lib/agendamento.service'
import { Agendamento } from '@/tipos/agendamento'
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
} from 'lucide-react'
import './procedimento.css'

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

// ── Sub‑componentes ───────────────────────────────────────────────

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

  const [observacoes, setObservacoes] = useState('')
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

  const finalizarConsulta = async () => {
    if (!agendamento) return
    setFinalizando(true)
    try {
      await agendamentoAPI.atualizarObservacoes(agendamento.id, observacoes)
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
                    {!podeEditar && !jaRealizado && (
                      <span className="proc-obs-bloqueado-tag">
                        Bloqueado — clique em Iniciar
                      </span>
                    )}
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
                  onClick={() => router.push('/agenda')}
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
