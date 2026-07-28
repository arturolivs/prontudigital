export type TipoPrescricao = 'MEDICAMENTO' | 'CUIDADO'

export const ROTULO_TIPO_PRESCRICAO: Record<TipoPrescricao, string> = {
  MEDICAMENTO: 'Medicamento',
  CUIDADO: 'Cuidado de enfermagem',
}

export interface Prescricao {
  uuid: string
  pacienteUuid: string
  pacienteNome: string
  agendamentoUuid: string | null
  tipo: TipoPrescricao
  descricao: string
  posologia: string | null
  frequencia: string | null
  duracao: string | null
  orientacoes: string | null
  registradoPor: string | null
  criadoEm: string
  atualizadoEm: string | null
}

export interface PrescricaoRequisicao {
  tipo: TipoPrescricao
  descricao: string
  posologia?: string | null
  frequencia?: string | null
  duracao?: string | null
  orientacoes?: string | null
  agendamentoUuid?: string | null
}
