// components/CareSessionList.tsx
'use client'

import { CareSession } from '@/types/CareSession'
import './styles.css'

interface CareSessionListProps {
  sessions: CareSession[]
  loading: boolean
  error: string | null
}

export default function CareSessionList({
  sessions,
  loading,
  error,
}: CareSessionListProps) {
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

  if (sessions.length === 0) {
    return (
      <div className="sessions-empty">
        <div className="empty-icon">📅</div>
        <p>Nenhuma sessão agendada para hoje</p>
      </div>
    )
  }

  // Função para formatar data e hora
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

  // Função para traduzir tipos
  const translateType = (type: string) => {
    const translations: Record<string, string> = {
      AVALIACAO: 'Avaliação',
      TRATAMENTO: 'Tratamento',
      PODEATRIA: 'Podiatria',
      TRATAMENTO_FERIDAS: 'Tratamento de Feridas',
    }
    return translations[type] || type
  }

  // Função para obter a cor do status
  const getStatusColor = (status: CareSession['status']) => {
    const colors: Record<string, string> = {
      SCHEDULED: 'bg-blue-100 text-blue-800',
      IN_PROGRESS: 'bg-yellow-100 text-yellow-800',
      COMPLETED: 'bg-green-100 text-green-800',
      CANCELLED: 'bg-red-100 text-red-800',
    }
    return colors['SCHEDULED'] || 'bg-gray-100 text-gray-800'
  }

  // Ordenar sessões por horário
  const sortedSessions = [...sessions].sort(
    (a, b) =>
      new Date(a.scheduledStart).getTime() -
      new Date(b.scheduledStart).getTime(),
  )

  return (
    <div className="sessions-container">
      <div className="sessions-header">
        <h2>Sessões de Cuidado</h2>
        <div className="sessions-count">
          <span className="count-badge">{sessions.length}</span>
          sessões hoje
        </div>
      </div>

      <div className="sessions-grid">
        {sortedSessions.map(session => {
          const startTime = formatDateTime(session.scheduledStart)
          const endTime = formatDateTime(session.scheduledEnd)

          return (
            <div key={session.id} className="session-card">
              <div className="session-card-header">
                <div className="session-number">
                  Sessão #{session.sessionNumber}
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
                <div className="session-info-row">
                  <div className="info-label">Paciente:</div>
                  <div className="info-value">{session.patientName}</div>
                </div>

                <div className="session-info-row">
                  <div className="info-label">Telefone:</div>
                  <div className="info-value">{session.patientPhone}</div>
                </div>

                <div className="session-info-row">
                  <div className="info-label">Profissional:</div>
                  <div className="info-value">{session.professionalName}</div>
                </div>

                <div className="session-time">
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
                    <span className="detail-label">Tipo:</span>
                    <span className="detail-value">
                      {translateType(session.sessionType)}
                    </span>
                  </div>

                  <div className="detail-item">
                    <span className="detail-label">Cuidado:</span>
                    <span className="detail-value">
                      {translateType(session.careType)}
                    </span>
                  </div>

                  <div className="detail-item">
                    <span className="detail-label">Local:</span>
                    <span className="detail-value">
                      {session.location === 'CLINIC' ? 'Clínica' : 'Domicílio'}
                    </span>
                  </div>

                  {session.bedriddenPatient && (
                    <div className="detail-item warning">
                      <span className="detail-label">Paciente Acamado</span>
                    </div>
                  )}
                </div>

                {session.clinicalNotes && (
                  <div className="session-notes">
                    <div className="notes-label">Observações:</div>
                    <div className="notes-content">{session.clinicalNotes}</div>
                  </div>
                )}
              </div>

              <div className="session-card-footer">
                <div className="session-actions">
                  <button className="action-button view">Ver detalhes</button>
                  <button className="action-button edit">Editar</button>
                  {session.status === 'SCHEDULED' && (
                    <button className="action-button start">Iniciar</button>
                  )}
                  {session.status === 'IN_PROGRESS' && (
                    <button className="action-button finish">Finalizar</button>
                  )}
                </div>

                <div className="session-date">{startTime.date}</div>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
