'use client'

import { CalendarViewProps } from './types'

const WeekView = ({ currentDate, events }: CalendarViewProps) => {
  const today = new Date()

  // Encontrar o primeiro dia da semana (domingo)
  const weekStart = new Date(currentDate)
  weekStart.setDate(currentDate.getDate() - currentDate.getDay())

  const weekDays: Date[] = []
  for (let i = 0; i < 7; i++) {
    const day = new Date(weekStart)
    day.setDate(weekStart.getDate() + i)
    weekDays.push(day)
  }

  const timeSlots: number[] = []
  for (let hour = 0; hour < 24; hour++) {
    timeSlots.push(hour)
  }

  const getEventsForDayAndHour = (day: Date, hour: number) => {
    return events.filter(event => {
      const eventDate = event.date
      return (
        eventDate.getDate() === day.getDate() &&
        eventDate.getMonth() === day.getMonth() &&
        eventDate.getFullYear() === day.getFullYear() &&
        eventDate.getHours() === hour
      )
    })
  }

  return (
    <div className="week-view">
      <div className="week-header">
        <div>Horário</div>
        {weekDays.map((day, index) => (
          <div key={index}>
            {day.toLocaleDateString('pt-BR', { weekday: 'short' })}{' '}
            {day.getDate()}
          </div>
        ))}
      </div>
      <div className="week-grid">
        {timeSlots.map(hour => (
          <div key={`time-${hour}`}>
            <div key={`time-${hour}`} className="time-slot">
              {hour.toString().padStart(2, '0')}:00
            </div>
            {weekDays.map((day, dayIndex) => {
              const isToday =
                day.getDate() === today.getDate() &&
                day.getMonth() === today.getMonth() &&
                day.getFullYear() === today.getFullYear()
              const dayEvents = getEventsForDayAndHour(day, hour)

              return (
                <div
                  key={`cell-${hour}-${dayIndex}`}
                  className={`day-cell ${isToday ? 'current-day' : ''}`}
                >
                  {dayEvents.map(event => (
                    <div
                      key={event.id}
                      className="week-event"
                      style={{
                        backgroundColor: event.color,
                        top: '5px',
                        left: '5px',
                        right: '5px',
                        height: '50px',
                      }}
                    >
                      {event.title}
                    </div>
                  ))}
                </div>
              )
            })}
          </div>
        ))}
      </div>
    </div>
  )
}

export default WeekView
