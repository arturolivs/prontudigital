import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import { HistoricoItem } from '../tipos/historico'

const API_BASE_URL =
  process.env.NEXT_PUBLIC_PRONTUARIO_API_URL ||
  'http://localhost:9090/api/prontuario'

const api = axios.create({ baseURL: API_BASE_URL })

setupRefreshInterceptor(api)

export const historicoAPI = {
  listar: async (pacienteUuid: string): Promise<HistoricoItem[]> => {
    const response = await api.get<HistoricoItem[]>(
      `/pacientes/${pacienteUuid}/historico`,
    )
    return response.data
  },
}
