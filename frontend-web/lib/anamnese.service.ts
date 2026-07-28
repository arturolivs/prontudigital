import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import { Anamnese, AnamneseRequisicao } from '../tipos/anamnese'

const API_BASE_URL =
  process.env.NEXT_PUBLIC_PRONTUARIO_API_URL ||
  'http://localhost:9090/api/prontuario'

const api = axios.create({ baseURL: API_BASE_URL })

setupRefreshInterceptor(api)

export const anamneseAPI = {
  /** Retorna `null` quando o paciente ainda não possui anamnese (404). */
  buscar: async (pacienteUuid: string): Promise<Anamnese | null> => {
    try {
      const response = await api.get<Anamnese>(
        `/pacientes/${pacienteUuid}/anamnese`,
      )
      return response.data
    } catch (erro) {
      if (axios.isAxiosError(erro) && erro.response?.status === 404) return null
      throw erro
    }
  },

  registrar: async (
    pacienteUuid: string,
    dados: AnamneseRequisicao,
  ): Promise<Anamnese> => {
    const response = await api.post<Anamnese>(
      `/pacientes/${pacienteUuid}/anamnese`,
      dados,
    )
    return response.data
  },

  atualizar: async (
    pacienteUuid: string,
    dados: AnamneseRequisicao,
  ): Promise<Anamnese> => {
    const response = await api.put<Anamnese>(
      `/pacientes/${pacienteUuid}/anamnese`,
      dados,
    )
    return response.data
  },
}
