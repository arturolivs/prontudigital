import React, { useMemo } from 'react'
import { AlertTriangle, CalendarX, Clock, Timer } from 'lucide-react'
import './listaAgendamentos.styles.css'
import { Agendamento } from '@/tipos/agendamento'

export type AgendamentosAgrupados = Record<string, Agendamento[]>

interface PropsListaAgendamentos {
  agendamentos?: Agendamento[]
  carregando?: boolean
  erro?: string | null
  onAgendamentoClick?: (agendamento: Agendamento) => void
}

const rotuloDeTipo: Record<string, string> = {
  AVALIACAO: 'Avaliação',
  TRATAMENTO: 'Tratamento',
}

const rotuloDeStatus: Record<string, string> = {
  AGENDADO: 'Agendado',
  CONFIRMADO: 'Confirmado',
  CANCELADO: 'Cancelado',
  REMARCADO: 'Remarcado',
  REALIZADO: 'Realizado',
  NAO_COMPARECEU: 'Não compareceu',
}

const ExibicaoDuracao: React.FC<{ inicio: string; fim: string }> = ({ inicio, fim }) => {
  const duracao = useMemo(() => {
    const diffMin = Math.floor(
      (new Date(fim).getTime() - new Date(inicio).getTime()) / 60000,
    )
    if (diffMin < 60) return `${diffMin} min`
    const h = Math.floor(diffMin / 60)
    const m = diffMin % 60
    return m > 0 ? `${h}h${m}min` : `${h}h`
  }, [inicio, fim])

  return (
    <div className="duration-display">
      <Timer size={12} strokeWidth={2} />
      <span>{duracao}</span>
    </div>
  )
}

const BadgeTipo: React.FC<{ tipo: string }> = ({ tipo }) => {
  const mapa: Record<string, string> = {
    AVALIACAO: 'avaliacao',
    TRATAMENTO: 'tratamento',
  }
  return (
    <div className={`status-badge status-${mapa[tipo] || 'default'}`}>
      {rotuloDeTipo[tipo] ?? tipo}
    </div>
  )
}

const CabecalhoData: React.FC<{ chaveData: string; total: number }> = ({
  chaveData,
  total,
}) => {
  const formatarDataExibicao = (dataStr: string): string => {
    const data = new Date(dataStr + 'T00:00:00')
    const hoje = new Date()
    hoje.setHours(0, 0, 0, 0)
    const amanha = new Date(hoje)
    amanha.setDate(amanha.getDate() + 1)
    if (data.toDateString() === hoje.toDateString()) return 'Hoje'
    if (data.toDateString() === amanha.toDateString()) return 'Amanhã'
    return data.toLocaleDateString('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    })
  }

  return (
    <div className="date-header">
      <div className="date-title">
        <CalendarX size={18} strokeWidth={1.75} />
        <span>{formatarDataExibicao(chaveData)}</span>
      </div>
      <div className="appointment-count">
        <span className="count-badge">{total}</span>
        <span className="count-label">agendamento{total !== 1 ? 's' : ''}</span>
      </div>
    </div>
  )
}

const obterClasseBorda = (tipo: string): string => {
  const mapa: Record<string, string> = {
    AVALIACAO: 'border-status-avaliacao',
    TRATAMENTO: 'border-status-tratamento',
  }
  return mapa[tipo] || 'border-status-default'
}

const ListaAgendamentos: React.FC<PropsListaAgendamentos> = ({
  agendamentos = [],
  carregando = false,
  erro = null,
  onAgendamentoClick,
}) => {
  const formatarHora = (dataString: string): string =>
    new Date(dataString).toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
    })

  const agendamentosAgrupados = useMemo(() => {
    const agrupados: AgendamentosAgrupados = {}
    agendamentos.forEach(a => {
      const chave = new Date(a.inicioEm).toISOString().split('T')[0]
      if (!agrupados[chave]) agrupados[chave] = []
      agrupados[chave].push(a)
    })
    const datasOrdenadas = Object.keys(agrupados).sort()
    const resultado: AgendamentosAgrupados = {}
    datasOrdenadas.forEach(data => {
      resultado[data] = agrupados[data].sort(
        (a, b) => new Date(a.inicioEm).getTime() - new Date(b.inicioEm).getTime(),
      )
    })
    return resultado
  }, [agendamentos])

  const temAgendamentos = Object.keys(agendamentosAgrupados).length > 0

  if (carregando) {
    return (
      <div className="appointment-list-container">
        <div className="loading-state">
          <div className="spinner" aria-label="Carregando agendamentos" />
          <p>Carregando agendamentos...</p>
        </div>
      </div>
    )
  }

  if (erro) {
    return (
      <div className="appointment-list-container">
        <div className="error-message" role="alert">
          <AlertTriangle size={24} strokeWidth={1.75} />
          <div className="error-content">
            <h3>Erro ao carregar agendamentos</h3>
            <p>{erro}</p>
            <button
              className="retry-btn"
              onClick={() => window.location.reload()}
              aria-label="Tentar novamente"
            >
              Tentar novamente
            </button>
          </div>
        </div>
      </div>
    )
  }

  if (!temAgendamentos) {
    return (
      <div className="appointment-list-container">
        <div className="no-appointments">
          <CalendarX size={48} strokeWidth={1} />
          <h3>Nenhum agendamento encontrado</h3>
          <p>Não há agendamentos para a data selecionada.</p>
        </div>
      </div>
    )
  }

  const totalAgendamentos = agendamentos.length

  return (
    <div className="appointment-list-container">
      <div className="header">
        <h1>Agendas</h1>
        {temAgendamentos && (
          <div className="total-appointments">
            Total: {totalAgendamentos} agendamento{totalAgendamentos !== 1 ? 's' : ''}
          </div>
        )}
      </div>

      <div className="dates-container">
        {Object.entries(agendamentosAgrupados).map(([chaveData, agendamentosDoDia]) => (
          <div className="day-container" key={chaveData}>
            <CabecalhoData chaveData={chaveData} total={agendamentosDoDia.length} />
            <div className="appointments-list">
              {agendamentosDoDia.map(agendamento => (
                <div
                  key={agendamento.id}
                  className={`appointment-card ${obterClasseBorda(agendamento.tipo)}`}
                  role="button"
                  tabIndex={0}
                  aria-label={`Abrir consulta de ${agendamento.nomePaciente}`}
                  onClick={() => onAgendamentoClick?.(agendamento)}
                  onKeyDown={e => e.key === 'Enter' && onAgendamentoClick?.(agendamento)}
                  style={onAgendamentoClick ? { cursor: 'pointer' } : undefined}
                >
                  <div className="time-info">
                    <span className="time-text">
                      {formatarHora(agendamento.inicioEm)} – {formatarHora(agendamento.fimEm)}
                    </span>
                    <div className="time-display">
                      <Clock size={12} strokeWidth={2} />
                      <ExibicaoDuracao inicio={agendamento.inicioEm} fim={agendamento.fimEm} />
                    </div>
                  </div>
                  <div className="details-info">
                    <span className="detail-text">{agendamento.nomePaciente}</span>
                    <span className="detail-sub-text">
                      {rotuloDeStatus[agendamento.status] ?? agendamento.status}
                    </span>
                  </div>
                  <BadgeTipo tipo={agendamento.tipo} />
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default ListaAgendamentos
