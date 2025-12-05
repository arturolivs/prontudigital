import React, { useMemo } from 'react'
import { Appointment } from '../../types/appointment'
import './schedule.css'

export type GroupedAppointments = Record<string, Appointment[]>

interface AppointmentListProps {
  appointments?: Appointment[]
  loading?: boolean
  error?: string | null
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

    if (diffMins < 60) {
      return `${diffMins} min`
    }

    const hours = Math.floor(diffMins / 60)
    const minutes = diffMins % 60

    return minutes > 0 ? `${hours}h${minutes}min` : `${hours}h`
  }, [start, end])

  return (
    <div className="duration-display">
      <i className="far fa-hourglass"></i>
      {duration}
    </div>
  )
}

const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const getStatusConfig = (status: string) => {
    const config: Record<string, { icon: string; class: string }> = {
      Confirmado: { icon: 'check-circle', class: 'confirmed' },
      Pendente: { icon: 'clock', class: 'pendente' },
      Cancelado: { icon: 'times-circle', class: 'cancelado' },
      Agendado: { icon: 'calendar-check', class: 'agendado' },
      Concluído: { icon: 'check-circle', class: 'completed' },
      Ausente: { icon: 'user-slash', class: 'absent' },
    }

    return config[status] || { icon: 'circle', class: 'default' }
  }

  const { icon, class: statusClass } = getStatusConfig(status)

  return <div className={`status-badge status-${statusClass}`}>{status}</div>
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
        <i className="far fa-calendar-alt"></i>
        <span>{formatDate(dateKey)}</span>
      </div>
      <div className="appointment-count">
        <span className="count-badge">{count}</span>
        <span className="count-label">agendamento{count !== 1 ? 's' : ''}</span>
      </div>
    </div>
  )
}

const getBorderColorClass = (status: string): string => {
  const colorMap: Record<string, string> = {
    Confirmado: 'border-status-confirmed',
    Pendente: 'border-status-pendente',
    Cancelado: 'border-status-cancelado',
    Agendado: 'border-status-agendado',
    Concluído: 'border-status-completed',
    Ausente: 'border-status-absent',
  }

  return colorMap[status] || 'border-status-default'
}

const AppointmentList: React.FC<AppointmentListProps> = ({
  appointments = [],
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

  const groupedAppointments = useMemo(() => {
    const grouped: GroupedAppointments = {}

    appointments.forEach(appointment => {
      const dateKey = new Date(appointment.startDateTime)
        .toISOString()
        .split('T')[0]

      if (!grouped[dateKey]) {
        grouped[dateKey] = []
      }

      grouped[dateKey].push(appointment)
    })

    const sortedDates = Object.keys(grouped).sort()
    const sortedGrouped: GroupedAppointments = {}

    sortedDates.forEach(date => {
      sortedGrouped[date] = grouped[date].sort(
        (a, b) =>
          new Date(a.startDateTime).getTime() -
          new Date(b.startDateTime).getTime(),
      )
    })

    return sortedGrouped
  }, [appointments])

  const hasAppointments = Object.keys(groupedAppointments).length > 0

  if (loading) {
    return (
      <div className="appointment-list-container">
        <div className="loading-state">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          <p>Carregando agendamentos...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="appointment-list-container">
        <div className="error-message">
          <i className="fas fa-exclamation-triangle"></i>
          <div className="error-content">
            <h3>Erro ao carregar agendamentos</h3>
            <p>{error}</p>
            <button
              className="retry-btn"
              onClick={() => window.location.reload()}
            >
              Tentar novamente
            </button>
          </div>
        </div>
      </div>
    )
  }

  if (!hasAppointments) {
    return (
      <div className="appointment-list-container">
        <div className="no-appointments">
          <i className="far fa-calendar-times"></i>
          <h3>Nenhum agendamento encontrado</h3>
          <p>Não há agendamentos para a data selecionada.</p>
        </div>
      </div>
    )
  }

  const totalAppointments = appointments.length

  return (
    <div className="appointment-list-container">
      <div className="header">
        <h1>Agendas</h1>
        {hasAppointments && (
          <div className="total-appointments">
            Total: {totalAppointments} agendamento
            {totalAppointments !== 1 ? 's' : ''}
          </div>
        )}
      </div>

      <div className="dates-container">
        {Object.entries(groupedAppointments).map(
          ([dateKey, dateAppointments]) => (
            <div key={dateKey}>
              <DateHeader dateKey={dateKey} count={dateAppointments.length} />

              <div className="appointments-list">
                {dateAppointments.map(appointment => {
                  const borderClass = getBorderColorClass(appointment.status)

                  return (
                    <div
                      key={appointment.id}
                      className={`appointment-card horizontal-card-2 ${borderClass}`}
                    >
                      <div className="card-main-line">
                        <div className="time-info-compact">
                          <div className="time-display-compact">
                            <i className="far fa-clock"></i>
                            <span className="time-text">
                              {formatTime(appointment.startDateTime)} -{' '}
                              {formatTime(appointment.endDateTime)}
                            </span>
                            <span className="duration-badge">
                              <DurationDisplay
                                start={appointment.startDateTime}
                                end={appointment.endDateTime}
                              />
                            </span>
                          </div>
                        </div>

                        <div className="card-details-line">
                          <div className="detail-with-icon">
                            <i className="fas fa-user"></i>
                            <span className="detail-text">
                              {appointment.patientName}
                            </span>
                          </div>

                          <div className="detail-with-icon">
                            <i className="fas fa-stethoscope"></i>
                            <span className="detail-text">
                              {appointment.type}
                            </span>
                          </div>

                          <div className="detail-with-icon">
                            <i className="fas fa-user-md"></i>
                            <span className="detail-text">
                              {appointment.professionalName}
                            </span>
                          </div>
                        </div>

                        <div className="card-actions-line">
                          <StatusBadge status={appointment.status} />
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          ),
        )}
      </div>
    </div>
  )
}

export default AppointmentList
