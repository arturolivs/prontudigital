import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import { Prescricao, PrescricaoRequisicao } from '../tipos/prescricao'

const API_BASE_URL =
  process.env.NEXT_PUBLIC_PRONTUARIO_API_URL ||
  'http://localhost:9090/api/prontuario'

const api = axios.create({ baseURL: API_BASE_URL })

setupRefreshInterceptor(api)

export const prescricaoAPI = {
  listar: async (pacienteUuid: string): Promise<Prescricao[]> => {
    const response = await api.get<Prescricao[]>(
      `/pacientes/${pacienteUuid}/prescricoes`,
    )
    return response.data
  },

  criar: async (
    pacienteUuid: string,
    dados: PrescricaoRequisicao,
  ): Promise<Prescricao> => {
    const response = await api.post<Prescricao>(
      `/pacientes/${pacienteUuid}/prescricoes`,
      dados,
    )
    return response.data
  },

  atualizar: async (
    pacienteUuid: string,
    prescricaoUuid: string,
    dados: PrescricaoRequisicao,
  ): Promise<Prescricao> => {
    const response = await api.put<Prescricao>(
      `/pacientes/${pacienteUuid}/prescricoes/${prescricaoUuid}`,
      dados,
    )
    return response.data
  },

  excluir: async (
    pacienteUuid: string,
    prescricaoUuid: string,
  ): Promise<void> => {
    await api.delete(`/pacientes/${pacienteUuid}/prescricoes/${prescricaoUuid}`)
  },
}
