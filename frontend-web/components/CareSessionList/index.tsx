import React, { useMemo } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  faTriangleExclamation,
  faCalendarTimes,
  faClock,
  faHourglassHalf,
} from '@fortawesome/free-solid-svg-icons'
import './styles.css'
import { Agendamento } from '@/tipos/CareSession'

export type GroupedSessions = Record<string, Agendamento[]>

interface CareSessionProps {
  sessions?: Agendamento[]
  loading?: boolean
  error?: string | null
}

const tipoLabel: Record<string, string> = {
  AVALIACAO: 'Avaliação',
  TRATAMENTO: 'Tratamento',
}

const statusLabel: Record<string, string> = {
  AGENDADO: 'Agendado',
  CONFIRMADO: 'Confirmado',
  CANCELADO: 'Cancelado',
  REMARCADO: 'Remarcado',
  REALIZADO: 'Realizado',
  NAO_COMPARECEU: 'Não compareceu',
}

const DurationDisplay: React.FC<{ start: string; end: string }> = ({
  start,
  end,
}) => {
  const duration = useMemo(() => {
    const startDate = new Date(start)
    const endDate = new Date(end)
    const diffMs = endDate.getTime() - startDate.getTime()
    const diffMins = Math.floor(diffMs / 60000)

    if (diffMins < 60) return `${diffMins} min`
    const hours = Math.floor(diffMins / 60)
    const minutes = diffMins % 60
    return minutes > 0 ? `${hours}h${minutes}min` : `${hours}h`
  }, [start, end])

  return (
    <div className="duration-display">
      <FontAwesomeIcon icon={faHourglassHalf} />
      <span>{duration}</span>
    </div>
  )
}

const TipoBadge: React.FC<{ tipo: string }> = ({ tipo }) => {
  const getStatusClass = (t: string): string => {
    const map: Record<string, string> = {
      AVALIACAO: 'avaliacao',
      TRATAMENTO: 'tratamento',
    }
    return map[t] || 'default'
  }

  return (
    <div className={`status-badge status-${getStatusClass(tipo)}`}>
      {tipoLabel[tipo] ?? tipo}
    </div>
  )
}

const DateHeader: React.FC<{ dateKey: string; count: number }> = ({
  dateKey,
  count,
}) => {
  const formatDate = (dateString: string): string => {
    const date = new Date(dateString)
    const today = new Date()
    const tomorrow = new Date(today)
    tomorrow.setDate(tomorrow.getDate() + 1)

    if (date.toDateString() === today.toDateString()) return 'Hoje'
    if (date.toDateString() === tomorrow.toDateString()) return 'Amanhã'

    return date.toLocaleDateString('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    })
  }

  return (
    <div className="date-header">
      <div className="date-title">
        <FontAwesomeIcon icon={faCalendarTimes} />
        <span>{formatDate(dateKey)}</span>
      </div>
      <div className="appointment-count">
        <span className="count-badge">{count}</span>
        <span className="count-label">agendamento{count !== 1 ? 's' : ''}</span>
      </div>
    </div>
  )
}

const getBorderColorClass = (tipo: string): string => {
  const colorMap: Record<string, string> = {
    AVALIACAO: 'border-status-avaliacao',
    TRATAMENTO: 'border-status-tratamento',
  }
  return colorMap[tipo] || 'border-status-default'
}

const CareSessionList: React.FC<CareSessionProps> = ({
  sessions = [],
  loading = false,
  error = null,
}) => {
  const formatTime = (dateString: string): string => {
    const date = new Date(dateString)
    return date.toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const GroupedSessions = useMemo(() => {
    const grouped: GroupedSessions = {}
    sessions.forEach(session => {
      const dateKey = new Date(session.inicioEm).toISOString().split('T')[0]
      if (!grouped[dateKey]) grouped[dateKey] = []
      grouped[dateKey].push(session)
    })
    const sortedDates = Object.keys(grouped).sort()
    const sortedGrouped: GroupedSessions = {}
    sortedDates.forEach(date => {
      sortedGrouped[date] = grouped[date].sort(
        (a, b) =>
          new Date(a.inicioEm).getTime() - new Date(b.inicioEm).getTime(),
      )
    })
    return sortedGrouped
  }, [sessions])

  const hasSessions = Object.keys(GroupedSessions).length > 0

  if (loading) {
    return (
      <div className="appointment-list-container">
        <div className="loading-state">
          <div className="spinner" aria-label="Carregando agendamentos"></div>
          <p>Carregando agendamentos...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="appointment-list-container">
        <div className="error-message" role="alert">
          <FontAwesomeIcon icon={faTriangleExclamation} />
          <div className="error-content">
            <h3>Erro ao carregar agendamentos</h3>
            <p>{error}</p>
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

  if (!hasSessions) {
    return (
      <div className="appointment-list-container">
        <div className="no-appointments">
          <FontAwesomeIcon icon={faCalendarTimes} />
          <h3>Nenhum agendamento encontrado</h3>
          <p>Não há agendamentos para a data selecionada.</p>
        </div>
      </div>
    )
  }

  const totalSessions = sessions.length

  return (
    <div className="appointment-list-container">
      <div className="header">
        <h1>Agendas</h1>
        {hasSessions && (
          <div className="total-appointments">
            Total: {totalSessions} agendamento{totalSessions !== 1 ? 's' : ''}
          </div>
        )}
      </div>

      <div className="dates-container">
        {Object.entries(GroupedSessions).map(([dateKey, dateSessions]) => (
          <div className="day-container" key={dateKey}>
            <DateHeader dateKey={dateKey} count={dateSessions.length} />
            <div className="appointments-list">
              {dateSessions.map(session => {
                const borderClass = getBorderColorClass(session.tipo)
                return (
                  <div
                    key={session.id}
                    className={`appointment-card ${borderClass}`}
                    role="article"
                    aria-label={`Agendamento de ${session.nomePaciente}`}
                  >
                    <div className="time-info">
                      <span className="time-text">
                        {formatTime(session.inicioEm)} -{' '}
                        {formatTime(session.fimEm)}
                      </span>
                      <div className="time-display">
                        <FontAwesomeIcon icon={faClock} />
                        <DurationDisplay
                          start={session.inicioEm}
                          end={session.fimEm}
                        />
                      </div>
                    </div>
                    <div className="details-info">
                      <span className="detail-text">{session.nomePaciente}</span>
                      <span className="detail-sub-text">
                        {statusLabel[session.status] ?? session.status}
                      </span>
                    </div>
                    <TipoBadge tipo={session.tipo} />
                  </div>
                )
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default CareSessionList
