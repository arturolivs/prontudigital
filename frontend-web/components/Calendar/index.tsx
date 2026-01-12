'use client'

import { useState, useEffect, JSX } from 'react'

import MonthView from './MonthView'
import DayView from './DayView'
import WeekView from './WeekView'

import './Calendar.css'
import { CalendarEvent, CalendarView } from './types'

const Calendar = () => {
  const [currentDate, setCurrentDate] = useState<Date>(new Date())
  const [currentView, setCurrentView] = useState<CalendarView>('month')
  const [events, setEvents] = useState<CalendarEvent[]>([])

  useEffect(() => {
    const sampleEvents: CalendarEvent[] = [
      {
        id: 1,
        title: 'Paciente 1',
        date: new Date(
          currentDate.getFullYear(),
          currentDate.getMonth(),
          10,
          10,
          0,
        ),
        duration: 60,
        color: '#4a6fa5',
      },
      {
        id: 2,
        title: 'Paciente 2',
        date: new Date(
          currentDate.getFullYear(),
          currentDate.getMonth(),
          10,
          12,
          30,
        ),
        duration: 90,
        color: '#6a4fa5',
      },
      {
        id: 3,
        title: 'Paciente 3',
        date: new Date(
          currentDate.getFullYear(),
          currentDate.getMonth(),
          2,
          14,
          0,
        ),
        duration: 120,
        color: '#4fa56a',
      },
      {
        id: 4,
        title: 'Paciente 4',
        date: new Date(
          currentDate.getFullYear(),
          currentDate.getMonth(),
          2,
          9,
          0,
        ),
        duration: 30,
        color: '#a56a4f',
      },
      {
        id: 5,
        title: 'paciente 5',
        date: new Date(
          currentDate.getFullYear(),
          currentDate.getMonth(),
          22,
          0,
          0,
        ),
        duration: 1440,
        color: '#a54f6a',
      },
    ]
    setEvents(sampleEvents)
  }, [currentDate])

  const navigateCalendar = (direction: number): void => {
    const newDate = new Date(currentDate)

    if (currentView === 'month') {
      newDate.setMonth(newDate.getMonth() + direction)
    } else if (currentView === 'week') {
      newDate.setDate(newDate.getDate() + 7 * direction)
    } else if (currentView === 'day') {
      newDate.setDate(newDate.getDate() + direction)
    }

    setCurrentDate(newDate)
  }

  const goToToday = (): void => {
    setCurrentDate(new Date())
  }

  const getTitle = (): string => {
    if (currentView === 'month') {
      return currentDate.toLocaleDateString('pt-BR', {
        month: 'long',
        year: 'numeric',
      })
    } else if (currentView === 'week') {
      const weekStart = new Date(currentDate)
      weekStart.setDate(currentDate.getDate() - currentDate.getDay())

      const weekEnd = new Date(weekStart)
      weekEnd.setDate(weekStart.getDate() + 6)

      return (
        `${weekStart.getDate()} de ${weekStart.toLocaleDateString('pt-BR', { month: 'long' })} - ` +
        `${weekEnd.getDate()} de ${weekEnd.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}`
      )
    } else if (currentView === 'day') {
      return currentDate.toLocaleDateString('pt-BR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      })
    }
    return ''
  }

  const handleViewChange = (view: CalendarView): void => {
    setCurrentView(view)
  }

  const renderView = (): JSX.Element => {
    switch (currentView) {
      case 'month':
        return <MonthView currentDate={currentDate} events={events} />
      case 'week':
        return <WeekView currentDate={currentDate} events={events} />
      case 'day':
        return <DayView currentDate={currentDate} events={events} />
      default:
        return <MonthView currentDate={currentDate} events={events} />
    }
  }

  return (
    <div className="calendar-container">
      <div className="calendar-header">
        <div className="calendar-title">{getTitle()}</div>
        <div className="calendar-controls">
          <button onClick={() => navigateCalendar(-1)}>&lt; Anterior</button>
          <button onClick={goToToday}>Hoje</button>
          <button onClick={() => navigateCalendar(1)}>Próximo &gt;</button>
        </div>
      </div>

      <div className="view-selector">
        <button
          className={currentView === 'month' ? 'active' : ''}
          onClick={() => handleViewChange('month')}
        >
          Mês
        </button>
        <button
          className={currentView === 'week' ? 'active' : ''}
          onClick={() => handleViewChange('week')}
        >
          Semana
        </button>
        <button
          className={currentView === 'day' ? 'active' : ''}
          onClick={() => handleViewChange('day')}
        >
          Dia
        </button>
      </div>

      <div className="calendar-body">{renderView()}</div>
    </div>
  )
}

export default Calendar
