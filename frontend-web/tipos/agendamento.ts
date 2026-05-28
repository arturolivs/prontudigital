import { StatusAgendamento } from './StatusAgendamento'
import { TipoAgendamento } from './TipoAgendamento'

export interface AgendamentoRequisicao {
  pacienteUuid: string
  profissionalUuid: string
  inicioEm: string
  fimEm: string
  tipo: 'AVALIACAO' | 'TRATAMENTO'
  observacoes?: string
  avaliacaoId?: number
}

export interface Agendamento {
  id: number
  inicioEm: string
  fimEm: string
  profissionalUuid: string
  pacienteUuid: string
  tipo: TipoAgendamento
  status: StatusAgendamento
  nomePaciente?: string
  nomeProfissional?: string
  observacoes?: string
  criadoEm: string
  avaliacaoId?: number
  concluidoEm?: string
}

export interface ReagendarRequisicao {
  novoInicioEm: string
  novoFimEm: string
  motivo?: string
}
