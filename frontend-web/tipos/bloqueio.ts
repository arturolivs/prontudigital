export type TipoBloqueio = 'INDISPONIVEL' | 'URGENCIA' | 'FOLGA' | 'MANUTENCAO'

export interface BloqueioHorario {
  id: number
  uuid: string
  profissionalUuid: string
  inicioEm: string
  fimEm: string
  motivo?: string
  tipo: TipoBloqueio
  /** Presente quando o bloco foi expandido de uma regra recorrente */
  recorrenteUuid?: string
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

// ── Bloqueios recorrentes ─────────────────────────────────────

/** ISO-8601: 1=Segunda … 6=Sábado, 7=Domingo */
export type DiaSemana = 1 | 2 | 3 | 4 | 5 | 6 | 7

export interface BloqueioRecorrente {
  id: number
  uuid: string
  profissionalUuid: string
  diaSemana: DiaSemana
  horaInicio: string // "HH:mm:ss"
  horaFim: string
  motivo?: string
  tipo: TipoBloqueio
}

export interface BloqueioRecorrenteRequisicao {
  profissionalUuid: string
  diaSemana: DiaSemana
  horaInicio: string
  horaFim: string
  motivo?: string
  tipo: TipoBloqueio
}

export const DIAS_SEMANA: { valor: DiaSemana; label: string; abrev: string }[] = [
  { valor: 1, label: 'Segunda-feira', abrev: 'Seg' },
  { valor: 2, label: 'Terça-feira',   abrev: 'Ter' },
  { valor: 3, label: 'Quarta-feira',  abrev: 'Qua' },
  { valor: 4, label: 'Quinta-feira',  abrev: 'Qui' },
  { valor: 5, label: 'Sexta-feira',   abrev: 'Sex' },
  { valor: 6, label: 'Sábado',        abrev: 'Sáb' },
  { valor: 7, label: 'Domingo',       abrev: 'Dom' },
]
