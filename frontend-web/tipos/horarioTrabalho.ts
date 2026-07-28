/** Janela semanal de atendimento do profissional (RF05). */
export interface HorarioTrabalho {
  id: number
  uuid: string
  profissionalUuid: string
  /** ISO-8601: 1=Segunda … 7=Domingo */
  diaSemana: number
  /** HH:mm */
  horaInicio: string
  horaFim: string
  ativo: boolean
}

export interface HorarioTrabalhoRequisicao {
  profissionalUuid: string
  diaSemana: number
  horaInicio: string
  horaFim: string
}

export const DIAS_SEMANA_TRABALHO: {
  valor: number
  label: string
  abrev: string
}[] = [
  { valor: 1, label: 'Segunda-feira', abrev: 'Seg' },
  { valor: 2, label: 'Terça-feira', abrev: 'Ter' },
  { valor: 3, label: 'Quarta-feira', abrev: 'Qua' },
  { valor: 4, label: 'Quinta-feira', abrev: 'Qui' },
  { valor: 5, label: 'Sexta-feira', abrev: 'Sex' },
  { valor: 6, label: 'Sábado', abrev: 'Sáb' },
  { valor: 7, label: 'Domingo', abrev: 'Dom' },
]

export function nomeDiaSemana(dia: number): string {
  return DIAS_SEMANA_TRABALHO.find(d => d.valor === dia)?.label ?? String(dia)
}
