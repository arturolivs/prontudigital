// Tipos para eventos
export interface CalendarEvent {
  id: number | string
  title: string
  date: Date
  duration: number // em minutos
  color: string
  description?: string
}

// Props para os componentes de visualização
export interface CalendarViewProps {
  currentDate: Date
  events: CalendarEvent[]
}

// Tipo para dias no calendário mensal
export interface CalendarDay {
  date: Date
  isCurrentMonth: boolean
  isToday: boolean
}

// Tipo para navegação
export type CalendarView = 'month' | 'week' | 'day'
