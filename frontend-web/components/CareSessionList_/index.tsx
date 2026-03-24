// components/CareSessionList.tsx
'use client'

import { CareSession } from '@/types/CareSession'
import { SessionStatus } from '@/types/SessionStatus'
import './styles.css'
import { useMemo } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCalendarTimes, faClock } from '@fortawesome/free-solid-svg-icons'
import { useRouter } from 'next/navigation'

export type GroupedSessions = Record<string, CareSession[]>

interface CareSessionListProps {
  sessions: CareSession[]
  loading: boolean
  error: string | null
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

    if (date.toDateString() === today.toDateString()) {
      return 'Hoje'
    }

    if (date.toDateString() === tomorrow.toDateString()) {
      return 'Amanhã'
    }

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
        <FontAwesomeIcon icon={faCalendarTimes} className="h-5 w-5" />
        <span>{formatDate(dateKey)}</span>
      </div>
      <div className="appointment-count">
        <span className="count-badge">{count}</span>
        <span className="count-label">sessão{count !== 1 ? 's' : ''}</span>
      </div>
    </div>
  )
}

export default function CareSessionList({
  sessions,
  loading,
  error,
}: CareSessionListProps) {
  const router = useRouter()

  const GroupedSessions = useMemo(() => {
    const grouped: GroupedSessions = {}

    sessions.forEach(session => {
      const dateKey = new Date(session.scheduledStart)
        .toISOString()
        .split('T')[0]

      if (!grouped[dateKey]) {
        grouped[dateKey] = []
      }

      grouped[dateKey].push(session)
    })

    const sortedDates = Object.keys(grouped).sort()
    const sortedGrouped: GroupedSessions = {}

    sortedDates.forEach(date => {
      sortedGrouped[date] = grouped[date].sort(
        (a, b) =>
          new Date(a.scheduledStart).getTime() -
          new Date(b.scheduledStart).getTime(),
      )
    })

    return sortedGrouped
  }, [sessions])

  const hasSessions = Object.keys(GroupedSessions).length > 0

  if (loading) {
    return (
      <div className="sessions-loading">
        <div className="spinner"></div>
        <p>Carregando sessões...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="sessions-error">
        <div className="error-icon">!</div>
        <p>{error}</p>
        <button
          className="retry-button"
          onClick={() => window.location.reload()}
        >
          Tentar novamente
        </button>
      </div>
    )
  }

  if (!hasSessions) {
    return (
      <div className="sessions-empty">
        <div className="empty-icon">📅</div>
        <p>Nenhuma sessão agendada</p>
      </div>
    )
  }

  const formatDateTime = (dateTime: string) => {
    const date = new Date(dateTime)
    return {
      date: date.toLocaleDateString('pt-BR'),
      time: date.toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit',
      }),
      fullDate: date.toLocaleString('pt-BR'),
    }
  }

  const translateType = (type: string) => {
    const translations: Record<string, string> = {
      AVALIACAO: 'Avaliação',
      TRATAMENTO: 'Tratamento',
      PODEATRIA: 'Podiatria',
      TRATAMENTO_FERIDAS: 'Tratamento de Feridas',
    }
    return translations[type] || type
  }

  const getStatusColor = (status: SessionStatus) => {
    const colors: Record<SessionStatus, string> = {
      SCHEDULED: 'bg-blue-100 text-blue-800',
      CONFIRMED: 'bg-yellow-100 text-yellow-800',
      IN_PROGRESS: 'bg-yellow-100 text-yellow-800',
      COMPLETED: 'bg-green-100 text-green-800',
      CANCELLED: 'bg-red-100 text-red-800',
    }
    return colors[status] || 'bg-gray-100 text-gray-800'
  }

  return (
    <>
      <div className="header">
        <h1>Agendamentos</h1>
      </div>

      <div className="dates-container">
        {Object.entries(GroupedSessions).map(([dateKey, dateSessions]) => (
          <div className="day-container" key={dateKey}>
            <DateHeader dateKey={dateKey} count={dateSessions.length} />

            <div className="sessions-grid">
              {dateSessions.map(session => {
                const startTime = formatDateTime(session.scheduledStart)
                const endTime = formatDateTime(session.scheduledEnd)

                return (
                  <div key={session.id} className="session-card">
                    <div className="session-card-header">
                      <div className="patient-info">
                        <div className="patient-name">
                          {session.patientName}
                        </div>
                        <div className="patient-phone">
                          {session.patientPhone}
                        </div>
                      </div>
                      <div
                        className={`session-status ${getStatusColor(session.status)}`}
                      >
                        {session.status === 'SCHEDULED' && 'Agendada'}
                        {session.status === 'IN_PROGRESS' && 'Em Andamento'}
                        {session.status === 'COMPLETED' && 'Concluída'}
                        {session.status === 'CANCELLED' && 'Cancelada'}
                      </div>
                    </div>

                    <div className="session-card-body">
                      <div className="session-time">
                        <FontAwesomeIcon icon={faClock} className="h-5 w-5" />
                        <div className="time-slot">
                          <span className="time-label">Início:</span>
                          <span className="time-value">{startTime.time}</span>
                        </div>
                        <div className="time-separator">→</div>
                        <div className="time-slot">
                          <span className="time-label">Término:</span>
                          <span className="time-value">{endTime.time}</span>
                        </div>
                      </div>

                      <div className="session-details">
                        <div className="detail-item">
                          <span className="detail-value">
                            {translateType(session.sessionType)}
                          </span>
                        </div>

                        <div className="detail-item">
                          <span className="detail-value">
                            {translateType(session.careType)}
                          </span>
                        </div>

                        <div className="detail-item">
                          <span className="detail-value">
                            {session.location === 'CLINIC'
                              ? 'Clínica'
                              : 'Domicílio'}
                          </span>
                        </div>

                        {session.bedriddenPatient && (
                          <div className="detail-item warning">
                            <span className="detail-label">
                              Paciente Acamado
                            </span>
                          </div>
                        )}
                      </div>

                      {session.clinicalNotes && false && (
                        <div className="session-notes">
                          <div className="notes-label">Observações:</div>
                          <div className="notes-content">
                            {session.clinicalNotes}
                          </div>
                        </div>
                      )}
                    </div>

                    {session.status === 'CONFIRMED' && (
                      <div className="session-card-footer">
                        <button
                          className="action-button start"
                          onClick={() =>
                            router.push(`/care-sessions/${session.sessionUuid}`)
                          }
                        >
                          Iniciar
                        </button>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        ))}
      </div>
    </>
  )
}
