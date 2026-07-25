import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import { Atestado, AtestadoRequisicao } from '../tipos/atestado'

const API_BASE_URL =
  process.env.NEXT_PUBLIC_PRONTUARIO_API_URL ||
  'http://localhost:9090/api/prontuario'

const api = axios.create({ baseURL: API_BASE_URL })

setupRefreshInterceptor(api)

export const atestadoAPI = {
  listar: async (pacienteUuid: string): Promise<Atestado[]> => {
    const response = await api.get<Atestado[]>(
      `/pacientes/${pacienteUuid}/atestados`,
    )
    return response.data
  },

  emitir: async (
    pacienteUuid: string,
    dados: AtestadoRequisicao,
  ): Promise<Atestado> => {
    const response = await api.post<Atestado>(
      `/pacientes/${pacienteUuid}/atestados`,
      dados,
    )
    return response.data
  },

  /**
   * Baixa o PDF como blob e abre em nova aba. Vai por axios para que o
   * interceptor mande o token — um link direto não autenticaria.
   */
  baixarPdf: async (
    pacienteUuid: string,
    atestadoUuid: string,
  ): Promise<void> => {
    const response = await api.get(
      `/pacientes/${pacienteUuid}/atestados/${atestadoUuid}/pdf`,
      { responseType: 'blob' },
    )
    const url = URL.createObjectURL(response.data as Blob)
    window.open(url, '_blank', 'noopener')
    // O object URL precisa sobreviver até a aba carregar; revogar na hora
    // deixaria a nova aba em branco.
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  },

  excluir: async (
    pacienteUuid: string,
    atestadoUuid: string,
  ): Promise<void> => {
    await api.delete(`/pacientes/${pacienteUuid}/atestados/${atestadoUuid}`)
  },
}
