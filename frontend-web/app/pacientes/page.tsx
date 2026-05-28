'use client'

import { useState, useEffect, useCallback, useMemo } from 'react'
import { RotaProtegida } from '../../components/RotaProtegida'
import { useAuth } from '../../contexts/AuthContext'
import { agendamentoAPI } from '../../lib/agendamento.service'
import Layout from '@/components/Layout/Layout'
import './patients.css'
import { Agendamento } from '@/tipos/agendamento'

const formatarDataISO = (date: Date): string => date.toISOString().split('T')[0]

const formatarHora = (dataString: string): string =>
  new Date(dataString).toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
  })

const formatarDataCurta = (dataString: string): string =>
  new Date(dataString).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })

const obterIniciais = (nome: string): string => {
  const partes = nome.trim().split(' ').filter(Boolean)
  if (partes.length === 1) return partes[0].substring(0, 2).toUpperCase()
  return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase()
}

const ROTULO_STATUS: Record<string, string> = {
  AGENDADO: 'Agendado',
  CONFIRMADO: 'Confirmado',
  CANCELADO: 'Cancelado',
  REMARCADO: 'Remarcado',
  REALIZADO: 'Realizado',
  NAO_COMPARECEU: 'Não compareceu',
}

const CLASSE_STATUS: Record<string, string> = {
  AGENDADO: 'status-agendado',
  CONFIRMADO: 'status-confirmado',
  CANCELADO: 'status-cancelado',
  REMARCADO: 'status-remarcado',
  REALIZADO: 'status-realizado',
  NAO_COMPARECEU: 'status-nao-compareceu',
}

const CORES_AVATAR = [
  '#2b6cb0',
  '#7991bc',
  '#10b981',
  '#f59e0b',
  '#8b5cf6',
  '#ef4444',
  '#3b82f6',
  '#ec4899',
]

const obterCorAvatar = (uuid: string): string => {
  let hash = 0
  for (let i = 0; i < uuid.length; i++) {
    hash = uuid.charCodeAt(i) + ((hash << 5) - hash)
  }
  return CORES_AVATAR[Math.abs(hash) % CORES_AVATAR.length]
}

interface PacienteAgrupado {
  pacienteUuid: string
  nomePaciente: string
  agendamentos: Agendamento[]
}

const CardAgendamento = ({ agendamento }: { agendamento: Agendamento }) => (
  <div
    className={`pac-appt-card ${agendamento.tipo === 'AVALIACAO' ? 'pac-appt-avaliacao' : 'pac-appt-tratamento'}`}
  >
    <div className="pac-appt-time">
      <span className="pac-appt-hora">
        {formatarHora(agendamento.inicioEm)}
      </span>
      <span className="pac-appt-data">
        {formatarDataCurta(agendamento.inicioEm)}
      </span>
    </div>
    <div className="pac-appt-info">
      <span className="pac-appt-tipo">
        {agendamento.tipo === 'AVALIACAO' ? 'Avaliação' : 'Tratamento'}
      </span>
    </div>
    <span
      className={`pac-appt-status ${CLASSE_STATUS[agendamento.status] || ''}`}
    >
      {ROTULO_STATUS[agendamento.status] ?? agendamento.status}
    </span>
  </div>
)

const CartaoPaciente = ({ paciente }: { paciente: PacienteAgrupado }) => {
  const [expandido, setExpandido] = useState(true)

  const stats = useMemo(() => {
    const avaliacoes = paciente.agendamentos.filter(
      a => a.tipo === 'AVALIACAO',
    ).length
    const tratamentos = paciente.agendamentos.filter(
      a => a.tipo === 'TRATAMENTO',
    ).length
    const realizados = paciente.agendamentos.filter(
      a => a.status === 'REALIZADO',
    ).length
    const agendados = paciente.agendamentos.filter(
      a => a.status === 'AGENDADO' || a.status === 'CONFIRMADO',
    ).length
    return { avaliacoes, tratamentos, realizados, agendados }
  }, [paciente.agendamentos])

  const ultimoAgendamento = paciente.agendamentos[0]
  const cor = obterCorAvatar(paciente.pacienteUuid)

  return (
    <div className="pac-card">
      <button
        className="pac-card-header"
        onClick={() => setExpandido(v => !v)}
        aria-expanded={expandido}
      >
        <div className="pac-avatar" style={{ background: cor }}>
          {obterIniciais(paciente.nomePaciente)}
        </div>

        <div className="pac-header-info">
          <span className="pac-nome">{paciente.nomePaciente}</span>
          <div className="pac-header-meta">
            {stats.avaliacoes > 0 && (
              <span className="pac-meta-chip pac-chip-avaliacao">
                {stats.avaliacoes} aval.
              </span>
            )}
            {stats.tratamentos > 0 && (
              <span className="pac-meta-chip pac-chip-tratamento">
                {stats.tratamentos} trat.
              </span>
            )}
            {stats.realizados > 0 && (
              <span className="pac-meta-chip pac-chip-realizado">
                {stats.realizados} realizado{stats.realizados !== 1 ? 's' : ''}
              </span>
            )}
            {stats.agendados > 0 && (
              <span className="pac-meta-chip pac-chip-pendente">
                {stats.agendados} pendente{stats.agendados !== 1 ? 's' : ''}
              </span>
            )}
          </div>
        </div>

        <div className="pac-header-right">
          {ultimoAgendamento && (
            <span className="pac-ultimo">
              Últ.: {formatarDataCurta(ultimoAgendamento.inicioEm)}
            </span>
          )}
          <span className={`pac-total-badge`}>
            {paciente.agendamentos.length}
          </span>
          <svg
            className={`pac-chevron${expandido ? ' pac-chevron-aberto' : ''}`}
            width="16"
            height="16"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
              clipRule="evenodd"
            />
          </svg>
        </div>
      </button>

      {expandido && (
        <div className="pac-card-body">
          {paciente.agendamentos.map(a => (
            <CardAgendamento key={a.id} agendamento={a} />
          ))}
        </div>
      )}
    </div>
  )
}

export default function PacientesPage() {
  const { usuario, temPerfil } = useAuth()
  const [agendamentos, setAgendamentos] = useState<Agendamento[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busca, setBusca] = useState('')
  const [filtroStatus, setFiltroStatus] = useState<string>('TODOS')

  const isEnfermeiro = temPerfil('ROLE_PROFISSIONAL')
  const isAdmin = temPerfil('ROLE_ADMIN')
  const hasRequiredRole = isEnfermeiro || isAdmin

  const fetchAgendamentos = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const hoje = new Date()
      const [r1, r2, r3] = await Promise.all([
        agendamentoAPI.getAgendamentos('month', formatarDataISO(hoje)),
        agendamentoAPI.getAgendamentos(
          'month',
          formatarDataISO(new Date(hoje.getFullYear(), hoje.getMonth() - 1, 1)),
        ),
        agendamentoAPI.getAgendamentos(
          'month',
          formatarDataISO(new Date(hoje.getFullYear(), hoje.getMonth() - 2, 1)),
        ),
      ])
      const todos = [...r1, ...r2, ...r3]
      const deduplicados = Array.from(
        new Map(todos.map(a => [a.id, a])).values(),
      )
      setAgendamentos(deduplicados)
    } catch (err: any) {
      setError(err.message || 'Erro ao carregar agendamentos')
      console.error('Erro:', err)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (usuario && hasRequiredRole) {
      fetchAgendamentos()
    }
  }, [usuario, hasRequiredRole, fetchAgendamentos])

  const pacientesAgrupados = useMemo<PacienteAgrupado[]>(() => {
    const mapa: Record<string, PacienteAgrupado> = {}

    const filtrados = agendamentos.filter(a => {
      const matchBusca =
        a.nomePaciente || ''.toLowerCase().includes(busca.toLowerCase())
      const matchStatus = filtroStatus === 'TODOS' || a.status === filtroStatus
      return matchBusca && matchStatus
    })

    filtrados.forEach(a => {
      if (!mapa[a.pacienteUuid]) {
        mapa[a.pacienteUuid] = {
          pacienteUuid: a.pacienteUuid,
          nomePaciente: a.nomePaciente,
          agendamentos: [],
        }
      }
      mapa[a.pacienteUuid].agendamentos.push(a)
    })

    return Object.values(mapa)
      .map(p => ({
        ...p,
        agendamentos: p.agendamentos.sort(
          (a, b) =>
            new Date(b.inicioEm).getTime() - new Date(a.inicioEm).getTime(),
        ),
      }))
      .sort((a, b) => {
        const ta = new Date(a.agendamentos[0]?.inicioEm ?? 0).getTime()
        const tb = new Date(b.agendamentos[0]?.inicioEm ?? 0).getTime()
        return tb - ta
      })
  }, [agendamentos, busca, filtroStatus])

  if (!usuario || !hasRequiredRole) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
      </div>
    )
  }

  return (
    <Layout perfil={isAdmin ? 'ROLE_ADMIN' : 'ROLE_PROFISSIONAL'}>
      <RotaProtegida perfisNecessarios={['ROLE_PROFISSIONAL', 'ROLE_ADMIN']}>
        <div className="pac-page">
          <div className="pac-page-header">
            <div className="pac-page-titulo">
              <h1>Pacientes</h1>
              {!loading && (
                <span className="pac-total-geral">
                  {pacientesAgrupados.length} paciente
                  {pacientesAgrupados.length !== 1 ? 's' : ''} ·{' '}
                  {agendamentos.length} agendamento
                  {agendamentos.length !== 1 ? 's' : ''}
                </span>
              )}
            </div>
            <span className="pac-periodo-label">Últimos 3 meses</span>
          </div>

          <div className="pac-filtros">
            <div className="pac-busca-wrapper">
              <svg
                className="pac-busca-icon"
                width="16"
                height="16"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
                  clipRule="evenodd"
                />
              </svg>
              <input
                type="text"
                className="pac-busca"
                placeholder="Buscar paciente..."
                value={busca}
                onChange={e => setBusca(e.target.value)}
              />
              {busca && (
                <button
                  className="pac-busca-limpar"
                  onClick={() => setBusca('')}
                  aria-label="Limpar busca"
                >
                  ✕
                </button>
              )}
            </div>

            <div className="pac-filtro-status">
              {[
                'TODOS',
                'AGENDADO',
                'CONFIRMADO',
                'REALIZADO',
                'CANCELADO',
                'NAO_COMPARECEU',
              ].map(s => (
                <button
                  key={s}
                  className={`pac-filtro-btn${filtroStatus === s ? ' pac-filtro-ativo' : ''}`}
                  onClick={() => setFiltroStatus(s)}
                >
                  {s === 'TODOS'
                    ? 'Todos'
                    : s === 'NAO_COMPARECEU'
                      ? 'Não compareceu'
                      : ROTULO_STATUS[s]}
                </button>
              ))}
            </div>
          </div>

          {loading && (
            <div className="pac-estado">
              <div className="pac-spinner" />
              <p>Carregando pacientes...</p>
            </div>
          )}

          {error && !loading && (
            <div className="pac-estado pac-estado-erro">
              <p>{error}</p>
              <button className="pac-retry-btn" onClick={fetchAgendamentos}>
                Tentar novamente
              </button>
            </div>
          )}

          {!loading && !error && pacientesAgrupados.length === 0 && (
            <div className="pac-estado">
              <svg
                width="48"
                height="48"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#d1d5db"
                strokeWidth="1.5"
              >
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" />
              </svg>
              <p>
                {busca || filtroStatus !== 'TODOS'
                  ? 'Nenhum paciente encontrado para os filtros selecionados.'
                  : 'Nenhum agendamento encontrado nos últimos 3 meses.'}
              </p>
            </div>
          )}

          {!loading && !error && pacientesAgrupados.length > 0 && (
            <div className="pac-lista">
              {pacientesAgrupados.map(p => (
                <CartaoPaciente key={p.pacienteUuid} paciente={p} />
              ))}
            </div>
          )}
        </div>
      </RotaProtegida>
    </Layout>
  )
}
