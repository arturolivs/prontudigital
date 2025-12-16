import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faClock } from '@fortawesome/free-solid-svg-icons'
import { Appointment } from '../../types/appointment'
import StatusBadge from '../StatusBadge'
import DurationDisplay from '../DurationDisplay'

interface AppointmentCardProps {
  appointment: Appointment
  onClick?: (appointment: Appointment) => void
}

const tratamentTypeDescription = {
  PODEATRIA: 'Podeatria',
  TRATAMENTO_FERIDAS: 'Tratamento de feridas',
}

const typeDescription = {
  AVALIACAO: 'Avaliação',
  TRATAMENTO: 'Tratamento',
}

const getBorderColorClass = (status: string): string => {
  const colorMap: Record<string, string> = {
    AVALIACAO: 'border-status-avaliacao',
    TRATAMENTO: 'border-status-tratamento',
  }

  return colorMap[status] || 'border-status-default'
}

const AppointmentCard: React.FC<AppointmentCardProps> = ({
  appointment,
  onClick,
}) => {
  const formatTime = (dateString: string): string => {
    const date = new Date(dateString)
    return date.toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const borderClass = getBorderColorClass(appointment.type)

  const handleClick = () => {
    if (onClick) {
      onClick(appointment)
    }
  }

  return (
    <div
      className={`appointment-card ${borderClass} ${onClick ? 'clickable' : ''}`}
      onClick={handleClick}
    >
      <div className="time-info">
        <span className="time-text">
          {formatTime(appointment.startDateTime)} -{' '}
          {formatTime(appointment.endDateTime)}
        </span>
        <div className="time-display">
          <div className="icon-container">
            <i className="far fa-clock"></i>
            <FontAwesomeIcon icon={faClock} />
          </div>
          <DurationDisplay
            start={appointment.startDateTime}
            end={appointment.endDateTime}
          />
        </div>
      </div>

      <div className="details-info">
        <span className="detail-text">{appointment.patientName}</span>
        <span className="detail-sub-text">
          {tratamentTypeDescription[appointment.tratamentType]}
        </span>
      </div>

      <StatusBadge status={typeDescription[appointment.type]} />
    </div>
  )
}

export default AppointmentCard
