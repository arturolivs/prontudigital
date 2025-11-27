'use client'

import { CalendarViewProps } from './types'

const DayView = ({ currentDate, events }: CalendarViewProps) => {
  const timeSlots: number[] = []
  for (let hour = 0; hour < 24; hour++) {
    timeSlots.push(hour)
  }

  const getEventsForHour = (hour: number) => {
    return events.filter(event => {
      const eventDate = event.date
      return (
        eventDate.getDate() === currentDate.getDate() &&
        eventDate.getMonth() === currentDate.getMonth() &&
        eventDate.getFullYear() === currentDate.getFullYear() &&
        eventDate.getHours() === hour
      )
    })
  }

  return (
    <div className="day-view">
      <div className="day-header">
        <div>Horário</div>
        <div>
          {currentDate.toLocaleDateString('pt-BR', {
            weekday: 'long',
            day: 'numeric',
            month: 'long',
          })}
        </div>
      </div>
      <div className="day-grid">
        {timeSlots.map(hour => {
          const hourEvents = getEventsForHour(hour)

          return (
            <div key={`time-${hour}`}>
              <div key={`time-${hour}`} className="day-time-slot">
                {hour.toString().padStart(2, '0')}:00
              </div>
              <div key={`schedule-${hour}`} className="day-schedule">
                {hourEvents.map(event => (
                  <div
                    key={event.id}
                    className="day-event"
                    style={{
                      backgroundColor: event.color,
                      top: '5px',
                      left: '5px',
                      right: '5px',
                      height: '50px',
                    }}
                  >
                    {`${event.date.getHours().toString().padStart(2, '0')}:${event.date.getMinutes().toString().padStart(2, '0')} - ${event.title}`}
                  </div>
                ))}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default DayView
