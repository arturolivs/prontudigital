import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import { Anexo } from '../tipos/anexo'

const API_BASE_URL =
  process.env.NEXT_PUBLIC_PRONTUARIO_API_URL ||
  'http://localhost:9090/api/prontuario'

const api = axios.create({ baseURL: API_BASE_URL })

setupRefreshInterceptor(api)

export const anexoAPI = {
  listar: async (pacienteUuid: string): Promise<Anexo[]> => {
    const response = await api.get<Anexo[]>(`/pacientes/${pacienteUuid}/anexos`)
    return response.data
  },

  enviar: async (
    pacienteUuid: string,
    arquivo: File,
    agendamentoUuid?: string,
  ): Promise<Anexo> => {
    const form = new FormData()
    form.append('arquivo', arquivo)
    if (agendamentoUuid) form.append('agendamentoUuid', agendamentoUuid)
    const response = await api.post<Anexo>(
      `/pacientes/${pacienteUuid}/anexos`,
      form,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
    return response.data
  },

  /**
   * Baixa o binário do anexo como Blob. Usa axios (com o interceptor de auth)
   * para que o token seja enviado — um `<img src>` direto não autenticaria.
   */
  baixarConteudo: async (
    pacienteUuid: string,
    anexoUuid: string,
  ): Promise<Blob> => {
    const response = await api.get(
      `/pacientes/${pacienteUuid}/anexos/${anexoUuid}/conteudo`,
      { responseType: 'blob' },
    )
    return response.data as Blob
  },

  excluir: async (pacienteUuid: string, anexoUuid: string): Promise<void> => {
    await api.delete(`/pacientes/${pacienteUuid}/anexos/${anexoUuid}`)
  },
}
