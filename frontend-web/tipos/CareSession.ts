import { StatusAgendamento } from './SessionStatus'
import { TipoAgendamento } from './SessionType'

export interface Agendamento {
  id: number
  inicioEm: string
  fimEm: string
  profissionalUuid: string
  pacienteUuid: string
  tipo: TipoAgendamento
  status: StatusAgendamento
  nomePaciente: string
  nomeProfissional: string
  avaliacaoId?: number
}
