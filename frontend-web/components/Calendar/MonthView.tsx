'use client'

import { CalendarDay, CalendarViewProps } from './types'

const MonthView = ({ currentDate, events }: CalendarViewProps) => {
  const year = currentDate.getFullYear()
  const month = currentDate.getMonth()
  const today = new Date()

  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const firstDayOfWeek = firstDay.getDay()
  const daysInMonth = lastDay.getDate()
  const prevMonthLastDay = new Date(year, month, 0).getDate()

  const days: CalendarDay[] = []

  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    days.push({
      date: new Date(year, month - 1, prevMonthLastDay - i),
      isCurrentMonth: false,
      isToday: false,
    })
  }

  // Adicionar dias do mês atual
  for (let i = 1; i <= daysInMonth; i++) {
    const date = new Date(year, month, i)
    days.push({
      date,
      isCurrentMonth: true,
      isToday:
        date.getDate() === today.getDate() &&
        date.getMonth() === today.getMonth() &&
        date.getFullYear() === today.getFullYear(),
    })
  }

  // Adicionar dias do próximo mês
  const totalCells = 42 // 6 semanas * 7 dias
  const remainingCells = totalCells - days.length

  for (let i = 1; i <= remainingCells; i++) {
    days.push({
      date: new Date(year, month + 1, i),
      isCurrentMonth: false,
      isToday: false,
    })
  }

  const getEventsForDay = (date: Date) => {
    return events.filter(
      event =>
        event.date.getDate() === date.getDate() &&
        event.date.getMonth() === date.getMonth() &&
        event.date.getFullYear() === date.getFullYear(),
    )
  }

  return (
    <div className="month-view">
      <div className="month-header">
        <div>Dom</div>
        <div>Seg</div>
        <div>Ter</div>
        <div>Qua</div>
        <div>Qui</div>
        <div>Sex</div>
        <div>Sáb</div>
      </div>
      <div className="month-days">
        {days.map((day, index) => (
          <div
            key={index}
            className={`day ${!day.isCurrentMonth ? 'other-month' : ''} ${day.isToday ? 'current-day' : ''}`}
          >
            <div className="day-number">{day.date.getDate()}</div>
            {getEventsForDay(day.date).map(event => (
              <div
                key={event.id}
                className="event"
                style={{ backgroundColor: event.color }}
              >
                {event.title}
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}

export default MonthView
