import { StatusAgendamento } from './StatusAgendamento'
import { TipoAgendamento } from './TipoAgendamento'
import { TipoProcedimento } from './TipoProcedimento'
import { LocalAtendimento } from './LocalAtendimento'

export type ExsudatoVolume = 'AUSENTE' | 'PEQUENO' | 'MODERADO' | 'GRANDE'
export type ExsudatoCaracteristica =
  | 'SEROSO'
  | 'SEROSSANGUINOLENTO'
  | 'SANGUINOLENTO'
  | 'PURULENTO'
export type OdorIntensidade = 'AUSENTE' | 'LEVE' | 'MODERADO' | 'INTENSO'
export type CaracteristicaBorda =
  | 'INTEGRAS'
  | 'MACERADAS'
  | 'ADERIDAS'
  | 'DESCOLADAS'
  | 'EPIBOLIA'
  | 'HIPERQUERATOSE'
export type CaracteristicaPerilesional =
  | 'INTEGRA'
  | 'HIPEREMIADA'
  | 'MACERADA'
  | 'RESSECADA'
  | 'EDEMACIADA'
  | 'DERMATITE'
export type SinalInfeccao =
  | 'AUSENTES'
  | 'ERITEMA'
  | 'CALOR_LOCAL'
  | 'EDEMA'
  | 'DOR_AUMENTADA'
  | 'EXSUDATO_PURULENTO'
  | 'MAU_ODOR'
export type ClassificacaoDor = 'AUSENTE' | 'LEVE' | 'MODERADA' | 'INTENSA'
export type GrauEdema = 'SEM_EDEMA' | 'MAIS_1' | 'MAIS_2' | 'MAIS_3' | 'MAIS_4'
export type AvaliacaoPulsos = 'PALPAVEIS' | 'DIMINUIDOS' | 'AUSENTES'
export type EvolucaoFerida = 'MELHORANDO' | 'ESTAVEL' | 'PIORANDO'
export type SinalEvolucao =
  | 'REDUCAO_DIMENSOES'
  | 'AUMENTO_GRANULACAO'
  | 'REDUCAO_EXSUDATO'
  | 'EPITELIZACAO_PROGRESSIVA'
  | 'NECESSITA_REAVALIACAO'

export interface AgendamentoRequisicao {
  pacienteUuid: string
  profissionalUuid: string
  inicioEm: string
  fimEm: string
  tipo: 'AVALIACAO' | 'TRATAMENTO'
  /** Legado (RF06): prefira procedimentoId */
  tipoProcedimento?: TipoProcedimento
  /** ID do procedimento na tabela de procedimentos (RF06) */
  procedimentoId?: number
  localAtendimento: LocalAtendimento
  pacienteAcamado: boolean
  avaliacaoId?: number
}

export interface EvolucaoClinica {
  // Dados da ferida
  localizacaoAnatomica?: string
  etiologia?: string
  tempoEvolucao?: string
  // Mensuração
  medidaComprimento?: number
  medidaLargura?: number
  medidaProfundidade?: number
  tunelizacao?: boolean
  descolamentoBordas?: boolean
  // Leito da ferida
  epitelizacaoPercentual?: number
  granulacaoPercentual?: number
  esfaceloPercentual?: number
  necrosePercentual?: number
  tendaoExposto?: boolean
  musculoExposto?: boolean
  ossoExposto?: boolean
  // Exsudato
  exsudatoVolume?: ExsudatoVolume
  exsudatoCaracteristica?: ExsudatoCaracteristica
  odorIntensidade?: OdorIntensidade
  // Bordas
  caracteristicasBordas?: CaracteristicaBorda[]
  // Pele perilesional
  caracteristicasPerilesional?: CaracteristicaPerilesional[]
  // Sinais de infecção
  sinaisInfeccao?: SinalInfeccao[]
  // Dor
  classificacaoDor?: ClassificacaoDor
  // Avaliação vascular
  grauEdema?: GrauEdema
  avaliacaoPulsos?: AvaliacaoPulsos
  // Evolução da ferida
  evolucaoFerida?: EvolucaoFerida
  sinaisEvolucao?: SinalEvolucao[]
  // Conduta
  limpezaLesao?: boolean
  desbridamento?: boolean
  coberturaAplicada?: boolean
  coberturaDescricao?: string
  terapiaAdjuvante?: boolean
  terapiaAdjuvanteDescricao?: string
  orientacoesFornecidas?: boolean
  // Observações
  observacoes?: string
}

export interface Agendamento {
  id: number
  inicioEm: string
  fimEm: string
  profissionalUuid: string
  pacienteUuid: string
  tipo: TipoAgendamento
  tipoProcedimento?: TipoProcedimento
  procedimentoId?: number
  procedimentoNome?: string
  localAtendimento?: LocalAtendimento
  pacienteAcamado?: boolean
  status: StatusAgendamento
  nomePaciente?: string
  nomeProfissional?: string
  criadoEm: string
  avaliacaoId?: number
  concluidoEm?: string
  evolucaoClinica?: EvolucaoClinica
}

export type EvolucaoTratamentoRequisicao = EvolucaoClinica

export interface ReagendarRequisicao {
  novoInicioEm: string
  novoFimEm: string
  motivo?: string
}

export interface AgendamentoView {
  id: number
  inicioEm: string
  fimEm: string
  profissionalUuid: string
  pacienteUuid: string
  tipo: TipoAgendamento
  tipoProcedimento?: TipoProcedimento
  procedimentoId?: number
  procedimentoNome?: string
  localAtendimento?: LocalAtendimento
  pacienteAcamado?: boolean
  status: StatusAgendamento
  nomePaciente?: string
  nomeProfissional?: string
  avaliacaoId?: number
}

export interface PacienteAgendamentosDTO {
  pacienteUuid: string
  nomePaciente: string
  agendamentos: AgendamentoView[]
}
