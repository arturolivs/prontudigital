import { CareType } from './CareType'
import { SessionType } from './SessionType'

export interface CareSession {
  id: number
  sessionUuid: string
  sessionNumber: number

  // Informações temporais
  scheduledStart: string // Agendado para
  scheduledEnd: string // Agendado até
  actualStart?: string | undefined // Início real (se diferente)
  actualEnd?: string | undefined // Término real

  // Identificação
  patientUuid: string
  patientName: string
  patientPhone: string
  professionalUuid: string
  professionalName: string

  // Classificação
  sessionType: SessionType
  careType: CareType

  // Status
  status?: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
  bedriddenPatient: boolean
  location?: 'CLINIC' | 'HOME'

  // Documentação
  clinicalNotes?: string
  nextSessionSuggested?: string

  // Metadados
  createdAt: string
  updatedAt: string
}
