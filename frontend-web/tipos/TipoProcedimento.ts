export type TipoProcedimento = 'PODIATRIA' | 'TRATAMENTO_FERIDAS'

export const ROTULO_TIPO_PROCEDIMENTO: Record<TipoProcedimento, string> = {
  PODIATRIA: 'Podiatria',
  TRATAMENTO_FERIDAS: 'Tratamento de Feridas',
}

/**
 * Nome de exibição do procedimento (RF06): usa o nome vindo da tabela de
 * procedimentos e recorre ao rótulo do enum legado para registros antigos.
 */
export function nomeProcedimento(agendamento: {
  procedimentoNome?: string
  tipoProcedimento?: TipoProcedimento
}): string | null {
  if (agendamento.procedimentoNome) return agendamento.procedimentoNome
  if (agendamento.tipoProcedimento)
    return ROTULO_TIPO_PROCEDIMENTO[agendamento.tipoProcedimento]
  return null
}
