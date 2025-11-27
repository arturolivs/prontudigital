export interface Appointment {
  id: number
  startDateTime: string
  endDateTime: string
  professionalUuid: string
  patientUuid: string
  type: string
  status: string
  patientName: string
  professionalName: string
}
