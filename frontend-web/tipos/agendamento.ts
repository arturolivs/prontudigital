import { StatusAgendamento } from './StatusAgendamento'
import { TipoAgendamento } from './TipoAgendamento'
import { TipoProcedimento } from './TipoProcedimento'
import { LocalAtendimento } from './LocalAtendimento'

export interface AgendamentoRequisicao {
  pacienteUuid: string
  profissionalUuid: string
  inicioEm: string
  fimEm: string
  tipo: 'AVALIACAO' | 'TRATAMENTO'
  tipoProcedimento: TipoProcedimento
  localAtendimento: LocalAtendimento
  pacienteAcamado: boolean
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
  tipoProcedimento?: TipoProcedimento
  localAtendimento?: LocalAtendimento
  pacienteAcamado?: boolean
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
