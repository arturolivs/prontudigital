import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  faCheckCircle,
  faClock,
  faTimesCircle,
  faCalendarCheck,
  faUserSlash,
  faCircle,
} from '@fortawesome/free-solid-svg-icons'

interface StatusBadgeProps {
  status: string
  showIcon?: boolean
  size?: 'sm' | 'md' | 'lg'
}

const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  showIcon = true,
  size = 'md',
}) => {
  const getStatusConfig = (status: string) => {
    const config: Record<string, { icon: any; class: string }> = {
      Avaliação: { icon: faCheckCircle, class: 'avaliacao' },
      Tratamento: { icon: faCalendarCheck, class: 'tratamento' },
      Confirmado: { icon: faCheckCircle, class: 'confirmed' },
      Pendente: { icon: faClock, class: 'pendente' },
      Cancelado: { icon: faTimesCircle, class: 'cancelado' },
      Agendado: { icon: faCalendarCheck, class: 'agendado' },
      Concluído: { icon: faCheckCircle, class: 'completed' },
      Ausente: { icon: faUserSlash, class: 'absent' },
    }

    return config[status] || { icon: faCircle, class: 'default' }
  }

  const { icon, class: statusClass } = getStatusConfig(status)

  return (
    <div className={`status-badge status-${statusClass} size-${size}`}>
      {showIcon && <FontAwesomeIcon icon={icon} className="status-icon" />}
      <span>{status}</span>
    </div>
  )
}

export default StatusBadge
