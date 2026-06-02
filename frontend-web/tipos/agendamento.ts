import { StatusAgendamento } from './StatusAgendamento'
import { TipoAgendamento } from './TipoAgendamento'
import { TipoProcedimento } from './TipoProcedimento'
import { LocalAtendimento } from './LocalAtendimento'

export type ExsudatoVolume = 'AUSENTE' | 'PEQUENO' | 'MODERADO' | 'GRANDE'
export type ExsudatoCaracteristica = 'SEROSO' | 'SEROSSANGUINOLENTO' | 'PURULENTO'

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

  // Evolução clínica pós-curativo
  localizacaoAnatomica?: string
  tipoLesao?: string
  medidaComprimento?: number
  medidaLargura?: number
  medidaProfundidade?: number
  aspectoLeitoFerida?: string
  exsudatoVolume?: ExsudatoVolume
  exsudatoCaracteristica?: ExsudatoCaracteristica
  condicaoBordas?: string
  aspectoPerilesional?: string
  sinaisFlogisticos?: boolean
  presencaOdor?: boolean
  limpezaRealizada?: string
  coberturasAplicadas?: string
  produtosUtilizados?: string
  aceitacaoProcedimento?: string
  escalaDor?: number
  intercorrencias?: string
  cuidadosCurativo?: string
  sinaisAlerta?: string
  orientacaoRetorno?: string
}

export interface EvolucaoTratamentoRequisicao {
  localizacaoAnatomica?: string
  tipoLesao?: string
  medidaComprimento?: number
  medidaLargura?: number
  medidaProfundidade?: number
  aspectoLeitoFerida?: string
  exsudatoVolume?: ExsudatoVolume
  exsudatoCaracteristica?: ExsudatoCaracteristica
  condicaoBordas?: string
  aspectoPerilesional?: string
  sinaisFlogisticos?: boolean
  presencaOdor?: boolean
  limpezaRealizada?: string
  coberturasAplicadas?: string
  produtosUtilizados?: string
  aceitacaoProcedimento?: string
  escalaDor?: number
  intercorrencias?: string
  cuidadosCurativo?: string
  sinaisAlerta?: string
  orientacaoRetorno?: string
}

export interface ReagendarRequisicao {
  novoInicioEm: string
  novoFimEm: string
  motivo?: string
}
