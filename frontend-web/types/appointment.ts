export interface Appointment {
  id: number
  startDateTime: string
  endDateTime: string
  professionalUuid: string
  patientUuid: string
  patientName: string
  patientPhone: string
  professionalName: string
  bedridden: boolean
  type: 'AVALIACAO' | 'TRATAMENTO'
  tratamentType: 'PODEATRIA' | 'TRATAMENTO_FERIDAS'
}
