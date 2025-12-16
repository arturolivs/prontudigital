import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCalendarTimes } from '@fortawesome/free-solid-svg-icons'

interface DateHeaderProps {
  dateKey: string
  count: number
}

const DateHeader: React.FC<DateHeaderProps> = ({ dateKey, count }) => {
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
        <FontAwesomeIcon icon={faCalendarTimes} className="calendar-icon" />
        <span>{formatDate(dateKey)}</span>
      </div>
      <div className="appointment-count">
        <span className="count-badge">{count}</span>
        <span className="count-label">agendamento{count !== 1 ? 's' : ''}</span>
      </div>
    </div>
  )
}

export default DateHeader
