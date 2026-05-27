export type TipoBloqueio = 'INDISPONIVEL' | 'URGENCIA' | 'FOLGA' | 'MANUTENCAO'

export interface BloqueioHorario {
  id: number
  uuid: string
  profissionalUuid: string
  inicioEm: string
  fimEm: string
  motivo?: string
  tipo: TipoBloqueio
}

export interface BloqueioHorarioRequisicao {
  profissionalUuid: string
  inicioEm: string
  fimEm: string
  motivo?: string
  tipo: TipoBloqueio
}

export const TIPO_BLOQUEIO_LABELS: Record<TipoBloqueio, string> = {
  INDISPONIVEL: 'Indisponível',
  URGENCIA: 'Urgência',
  FOLGA: 'Folga',
  MANUTENCAO: 'Manutenção',
}
