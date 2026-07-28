'use client'

import { useState, useEffect, useCallback, useMemo } from 'react'
import Link from 'next/link'
import { RotaProtegida } from '../../components/RotaProtegida'
import { useAuth } from '../../contexts/AuthContext'
import { agendamentoAPI } from '../../lib/agendamento.service'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import {
  formatarDataCurta,
  obterCorAvatar,
  obterIniciais,
} from '@/lib/paciente-ui'
import Layout from '@/components/Layout/Layout'
import './patients.css'
import { AgendamentoView, PacienteAgendamentosDTO } from '@/tipos/agendamento'

const formatarHora = (dataString: string): string =>
  new Date(dataString).toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
  })

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

const CardAgendamento = ({ agendamento }: { agendamento: AgendamentoView }) => (
  <div
    className={`pac-appt-card ${agendamento.tipo === 'AVALIACAO' ? 'pac-appt-avaliacao' : 'pac-appt-tratamento'}`}
  >
    <div className="pac-appt-topo">
      <div className="pac-appt-time">
        <span className="pac-appt-hora">
          {formatarHora(agendamento.inicioEm)}
        </span>
        <span className="pac-appt-data">
          {formatarDataCurta(agendamento.inicioEm)}
        </span>
      </div>
      <span
        className={`pac-appt-status ${CLASSE_STATUS[agendamento.status] || ''}`}
      >
        {ROTULO_STATUS[agendamento.status] ?? agendamento.status}
      </span>
    </div>
    <div className="pac-appt-info">
      <span className="pac-appt-tipo">
        {agendamento.tipo === 'AVALIACAO' ? 'Avaliação' : 'Tratamento'}
      </span>
    </div>
  </div>
)

const CartaoPaciente = ({
  paciente,
}: {
  paciente: PacienteAgendamentosDTO
}) => {
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
          <div className="pac-card-acoes">
            <Link
              href={`/pacientes/${paciente.pacienteUuid}/prontuario?nome=${encodeURIComponent(paciente.nomePaciente)}&de=pacientes`}
              className="pac-prontuario-link"
            >
              Ver prontuário →
            </Link>
          </div>
          {paciente.agendamentos.map(a => (
            <CardAgendamento key={a.id} agendamento={a} />
          ))}
        </div>
      )}
    </div>
  )
}

const ITENS_POR_PAGINA = 10

function gerarPaginas(total: number, atual: number): (number | string)[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const paginas: (number | string)[] = [1]
  if (atual > 3) paginas.push('...')
  for (let i = Math.max(2, atual - 1); i <= Math.min(total - 1, atual + 1); i++)
    paginas.push(i)
  if (atual < total - 2) paginas.push('...')
  paginas.push(total)
  return paginas
}

export default function PacientesPage() {
  const { usuario, temPerfil } = useAuth()
  const [pacientes, setPacientes] = useState<PacienteAgendamentosDTO[]>([])
  const [totalPaginas, setTotalPaginas] = useState(1)
  const [totalPacientes, setTotalPacientes] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busca, setBusca] = useState('')
  const [filtroStatus, setFiltroStatus] = useState<string>('TODOS')
  const [paginaAtual, setPaginaAtual] = useState(1)

  useEffect(() => {
    setPaginaAtual(1)
  }, [busca, filtroStatus])

  const isEnfermeiro = temPerfil('ROLE_PROFISSIONAL')
  const isAdmin = temPerfil('ROLE_ADMIN')
  const hasRequiredRole = isEnfermeiro || isAdmin

  const fetchPacientes = useCallback(
    async (pagina: number, buscaAtual: string, statusAtual: string) => {
      try {
        setLoading(true)
        setError(null)
        const data = await agendamentoAPI.listarPacientesComAgendamentos(
          buscaAtual,
          statusAtual,
          pagina - 1,
          ITENS_POR_PAGINA,
        )
        setPacientes(data.content)
        setTotalPaginas(Math.max(1, data.totalPages))
        setTotalPacientes(data.totalElements)
      } catch (err: any) {
        setError(mensagemErro(err, MENSAGENS.erro.carregarPacientes))
        console.error('Erro:', err)
      } finally {
        setLoading(false)
      }
    },
    [],
  )

  useEffect(() => {
    if (!usuario || !hasRequiredRole) return
    const timer = setTimeout(
      () => fetchPacientes(paginaAtual, busca, filtroStatus),
      busca ? 400 : 0,
    )
    return () => clearTimeout(timer)
  }, [
    usuario,
    hasRequiredRole,
    paginaAtual,
    busca,
    filtroStatus,
    fetchPacientes,
  ])

  const paginaValida = Math.min(paginaAtual, totalPaginas)

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
                  {totalPacientes} paciente{totalPacientes !== 1 ? 's' : ''}
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
              <button
                className="pac-retry-btn"
                onClick={() => fetchPacientes(paginaAtual, busca, filtroStatus)}
              >
                Tentar novamente
              </button>
            </div>
          )}

          {!loading && !error && pacientes.length === 0 && (
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

          {!loading && !error && pacientes.length > 0 && (
            <>
              <div className="pac-lista">
                {pacientes.map(p => (
                  <CartaoPaciente key={p.pacienteUuid} paciente={p} />
                ))}
              </div>

              {totalPaginas > 1 && (
                <div className="paginacao">
                  <button
                    className="paginacao-btn"
                    onClick={() =>
                      setPaginaAtual(prev => Math.max(1, prev - 1))
                    }
                    disabled={paginaValida === 1}
                  >
                    ← Anterior
                  </button>
                  {gerarPaginas(totalPaginas, paginaValida).map((p, i) =>
                    typeof p === 'string' ? (
                      <span
                        key={`ellipsis-${i}`}
                        className="paginacao-ellipsis"
                      >
                        …
                      </span>
                    ) : (
                      <button
                        key={`page-${p}`}
                        className={`paginacao-btn paginacao-num${paginaValida === p ? ' paginacao-ativa' : ''}`}
                        onClick={() => setPaginaAtual(p)}
                      >
                        {p}
                      </button>
                    ),
                  )}
                  <button
                    className="paginacao-btn"
                    onClick={() =>
                      setPaginaAtual(prev => Math.min(totalPaginas, prev + 1))
                    }
                    disabled={paginaValida === totalPaginas}
                  >
                    Próximo →
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </RotaProtegida>
    </Layout>
  )
}
