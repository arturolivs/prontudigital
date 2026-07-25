import { StatusAgendamento } from './StatusAgendamento'
import { TipoAgendamento } from './TipoAgendamento'
import { LocalAtendimento } from './LocalAtendimento'

/** Linha do relatório de atendimentos (RF19). */
export interface RelatorioAtendimentoItem {
  agendamentoId: number
  inicioEm: string
  fimEm: string
  duracaoMinutos: number
  pacienteUuid: string
  nomePaciente: string
  profissionalUuid: string
  nomeProfissional: string
  tipo?: TipoAgendamento
  procedimentoNome?: string
  localAtendimento?: LocalAtendimento
  status: StatusAgendamento
  concluidoEm?: string
}

export interface RelatorioAtendimentos {
  inicio: string
  fim: string
  /** Nulo quando o relatório cobre a clínica inteira. */
  nomeProfissional?: string
  total: number
  totalPorStatus: Record<string, number>
  totalPorTipo: Record<string, number>
  itens: RelatorioAtendimentoItem[]
}

/**
 * Relatório de ocupação (RF20). As taxas vêm nulas quando não há base de
 * comparação — período sem agendamentos, ou profissional sem expediente
 * cadastrado (RF05) no caso da ocupação.
 */
export interface RelatorioOcupacao {
  inicio: string
  fim: string
  nomeProfissional?: string
  total: number
  realizados: number
  cancelados: number
  naoCompareceram: number
  remarcados: number
  emAberto: number
  taxaComparecimento?: number
  taxaCancelamento?: number
  taxaAbsenteismo?: number
  horasAgendadas: number
  horasDisponiveis?: number
  taxaOcupacao?: number
}

/** Formatos aceitos pelos endpoints de exportação (RF21). */
export type FormatoExportacao = 'PDF' | 'XLSX'

export interface FiltrosRelatorio {
  inicio: string
  fim: string
  profissionalUuid?: string
  status?: StatusAgendamento
  tipo?: TipoAgendamento
}
