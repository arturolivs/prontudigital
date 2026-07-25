/** Tipos de atestado emitidos pela clínica (RF17). */
export type TipoAtestado = 'COMPARECIMENTO' | 'AFASTAMENTO'

export const ROTULO_TIPO_ATESTADO: Record<TipoAtestado, string> = {
  COMPARECIMENTO: 'Comparecimento',
  AFASTAMENTO: 'Afastamento',
}

export interface Atestado {
  uuid: string
  pacienteUuid: string
  nomePaciente: string
  agendamentoUuid?: string
  tipo: TipoAtestado
  /** Só presente em AFASTAMENTO. */
  diasAfastamento?: number
  cid?: string
  observacoes?: string
  emitidoPor?: string
  nomeProfissional?: string
  criadoEm: string
}

export interface AtestadoRequisicao {
  tipo: TipoAtestado
  agendamentoUuid?: string
  /** Obrigatório em AFASTAMENTO; o backend recusa em COMPARECIMENTO. */
  diasAfastamento?: number
  cid?: string
  observacoes?: string
}
