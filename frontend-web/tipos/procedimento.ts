export interface Procedimento {
  id: number
  /** Código legado do enum TipoProcedimento (apenas registros migrados) */
  codigo?: string
  nome: string
  descricao?: string
  duracaoPadraoMinutos?: number
  ativo: boolean
  criadoEm?: string
  atualizadoEm?: string
}

export interface ProcedimentoRequisicao {
  nome: string
  descricao?: string
  duracaoPadraoMinutos?: number
  ativo?: boolean
}
