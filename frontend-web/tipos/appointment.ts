export interface AgendamentoRequisicao {
  pacienteUuid: string
  profissionalUuid: string
  inicioEm: string
  fimEm: string
  tipo: 'AVALIACAO' | 'TRATAMENTO'
  observacoes?: string
  avaliacaoId?: number
}

export interface AgendamentoResposta {
  id: number
  inicioEm: string
  fimEm: string
  profissionalUuid: string
  pacienteUuid: string
  tipo: 'AVALIACAO' | 'TRATAMENTO'
  status: string
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
