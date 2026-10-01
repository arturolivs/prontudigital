import { StatusAgendamento } from './StatusAgendamento'
import { TipoAgendamento } from './TipoAgendamento'
import { TipoProcedimento } from './TipoProcedimento'
import { LocalAtendimento } from './LocalAtendimento'

// ── Enums das fichas clínicas (TIME) ─────────────────────────────
// Espelham os enums do backend em agendamento/enums. Compartilhados
// entre a Ficha de Evolução de Enfermagem e a de Curativos.

/** T (Tecido): G = Granulação | E = Esfacelo | N = Necrose | EP = Epitelização */
export type TecidoLeito = 'GRANULACAO' | 'ESFACELO' | 'NECROSE' | 'EPITELIZACAO'

/** I (Infecção/Inflamação) na ficha de curativos: 0 = Ausente | 1 = Local | 2 = Sistêmica */
export type InfeccaoInflamacaoCurativo = 'AUSENTE' | 'LOCAL' | 'SISTEMICA'

/** I (Infecção/Inflamação) na ficha de enfermagem — sinal predominante. */
export type InfeccaoInflamacao =
  | 'AUSENTE'
  | 'EXSUDATO_PURULENTO'
  | 'ODOR'
  | 'DOR'

/** M (Exsudato): 0 = Ausente | 1 = Pequeno | 2 = Moderado | 3 = Intenso */
export type ExsudatoTime = 'AUSENTE' | 'PEQUENO' | 'MODERADO' | 'INTENSO'

/** E (Bordas): I = Íntegras | M = Maceradas | D = Descoladas | EP = Epitelizando */
export type BordasFerida =
  | 'INTEGRAS'
  | 'MACERADAS'
  | 'DESCOLADAS'
  | 'EPITELIZANDO'

export type TipoDesbridamento =
  | 'NAO'
  | 'AUTOLITICO'
  | 'INSTRUMENTAL'
  | 'ENZIMATICO'

export type AvaliacaoEvolucao = 'MELHORA' | 'ESTAVEL' | 'PIORA'

/** Etiologia da ferida na Ficha de Evolução de Enfermagem. */
export type TipoFerida =
  | 'CIRURGICA'
  | 'TRAUMATICA'
  | 'ULCERA_VENOSA'
  | 'LESAO_PRESSAO'
  | 'PE_DIABETICO'
  | 'OUTRA'

/** Solução utilizada na limpeza da ferida. */
export type TipoLimpeza = 'SF_09' | 'OUTRO'

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

/**
 * Ficha de Evolução de Enfermagem.
 * Preenchida em agendamentos do tipo AVALIACAO (o backend recusa os demais).
 * Espelha EvolucaoEnfermagemRequestDTO.
 */
export interface EvolucaoEnfermagemRequisicao {
  // 1. Dados da avaliação
  /** Data no formato YYYY-MM-DD — pré-preenchida do agendamento. */
  dataAvaliacao?: string
  /** Hora no formato HH:mm — pré-preenchida do agendamento. */
  horaAvaliacao?: string
  diagnosticoMedico?: string
  comorbDiabetes?: boolean
  comorbHipertensao?: boolean
  comorbDoencaVascular?: boolean
  comorbNeuropatia?: boolean
  comorbOutras?: boolean
  comorbOutrasDetalhe?: string
  medicamentosRelevantes?: string
  // 2. Avaliação da ferida (modelo TIME)
  localizacaoAnatomica?: string
  tipoFerida?: TipoFerida
  tipoFeridaOutra?: string
  dimensoes?: string
  comprimento?: number
  largura?: number
  profundidade?: number
  tunelizacao?: boolean
  descolamento?: boolean
  tecidoLeito?: TecidoLeito
  infeccaoInflamacao?: InfeccaoInflamacao
  exsudato?: ExsudatoTime
  exsudatoTipo?: string
  bordas?: BordasFerida
  pelePerilesional?: string
  /** Escala de 0 a 10 */
  dorEscala?: number
  sinaisVitais?: string
  pa?: string
  fc?: string
  fr?: string
  temp?: string
  // 3. Diagnósticos de enfermagem
  diagIntegridadePele?: boolean
  diagIntegridadeTissular?: boolean
  diagRiscoInfeccao?: boolean
  diagPerfusaoIneficaz?: boolean
  diagDorAguda?: boolean
  diagOutros?: string
  // 4. Conduta realizada
  limpeza?: TipoLimpeza
  limpezaOutro?: string
  desbridamento?: TipoDesbridamento
  coberturaPrimaria?: string
  coberturaSecundaria?: string
  fixacao?: string
  orientacoesPaciente?: string
  // 5. Avaliação da evolução
  avaliacaoEvolucao?: AvaliacaoEvolucao
  reducaoArea?: boolean
  observacoes?: string
  // 6. Plano
  planoManterConduta?: boolean
  planoAjustarCobertura?: boolean
  planoAvaliacaoMedica?: boolean
  planoSolicitarExames?: boolean
  planoEncaminhamento?: boolean
  retornoDias?: number
}

/** Resposta do backend (EvolucaoEnfermagemDTO) — requisição + auditoria. */
export interface EvolucaoEnfermagem extends EvolucaoEnfermagemRequisicao {
  criadoEm?: string
  atualizadoEm?: string
}

/**
 * Ficha de Evolução Diária – Curativos.
 * Preenchida em agendamentos do tipo TRATAMENTO (o backend recusa os demais).
 * Espelha EvolucaoCurativoRequestDTO.
 */
export interface EvolucaoCurativoRequisicao {
  // 1. Avaliação diária da ferida
  comprimento?: number
  largura?: number
  profundidade?: number
  /** C × L — calculada no frontend */
  areaAproximada?: number
  tecido?: TecidoLeito
  infeccaoInflamacao?: InfeccaoInflamacaoCurativo
  exsudato?: ExsudatoTime
  bordas?: BordasFerida
  odorPresente?: boolean
  /** Escala de 0 a 10 */
  dorEscala?: number
  pelePerilesional?: string
  // 2. Intervenções realizadas
  limpezaIrrigacao?: string
  desbridamento?: TipoDesbridamento
  desbridamentoObs?: string
  coberturaPrimaria?: string
  orientacoesPaciente?: string
  // 3. Avaliação da evolução
  evolucao?: AvaliacaoEvolucao
  observacoes?: string
  // 4. Plano / ações futuras — observação preenchida equivale a ação marcada
  planoManterConduta?: string
  planoAlterarCobertura?: string
  planoSolicitarExames?: string
  planoEncaminhamento?: string
  retornoPrevisto?: string
}

/** Resposta do backend (EvolucaoCurativoDTO) — requisição + auditoria. */
export interface EvolucaoCurativo extends EvolucaoCurativoRequisicao {
  criadoEm?: string
  atualizadoEm?: string
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
  evolucaoEnfermagem?: EvolucaoEnfermagem
  evolucaoCurativo?: EvolucaoCurativo
}

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
