export type TipoHistorico = 'AGENDAMENTO' | 'EVOLUCAO' | 'PRESCRICAO' | 'ANEXO'

/** Espelha HistoricoItemDTO do backend (RF18). */
export interface HistoricoItem {
  tipo: TipoHistorico
  data: string // LocalDateTime ISO
  titulo: string
  descricao: string | null
  referenciaUuid: string | null
  agendamentoUuid: string | null
}

export const ROTULO_TIPO: Record<TipoHistorico, string> = {
  AGENDAMENTO: 'Atendimento',
  EVOLUCAO: 'Evolução',
  PRESCRICAO: 'Prescrição',
  ANEXO: 'Anexo',
}

/** Sufixo de classe CSS por tipo (hist-marker-*, hist-badge-*). */
export const CLASSE_TIPO: Record<TipoHistorico, string> = {
  AGENDAMENTO: 'agendamento',
  EVOLUCAO: 'evolucao',
  PRESCRICAO: 'prescricao',
  ANEXO: 'anexo',
}

export const formatarDataHora = (dataString: string): string =>
  new Date(dataString).toLocaleString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
