export interface Appointment {
  id: number
  startDateTime: string
  endDateTime: string
  professionalUuid: string
  patientUuid: string
  patientName: string
  professionalName: string
  bedridden: boolean
  type: 'AVALIACAO' | 'TRATAMENTO'
  tratamentType: 'PODEATRIA' | 'TRATAMENTO_FERIDAS'
}

export interface NewAppointmentData {
  patientName: string
  age: number
  bedridden: boolean
  dateTime: string
  type: 'AVALIACAO' | 'TRATAMENTO'
  tratamentType: 'PODEATRIA' | 'TRATAMENTO_FERIDAS'
  duration: number
}

export type GroupedAppointments = Record<string, Appointment[]>
