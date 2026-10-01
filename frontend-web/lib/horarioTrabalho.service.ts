import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import {
  HorarioTrabalho,
  HorarioTrabalhoRequisicao,
} from '../tipos/horarioTrabalho'

const BASE_URL =
  process.env.NEXT_PUBLIC_HORARIOS_TRABALHO_API_URL ||
  'http://localhost:9090/api/horarios-trabalho'

const api = axios.create({ baseURL: BASE_URL })

setupRefreshInterceptor(api)

export const horarioTrabalhoAPI = {
  criar: async (dados: HorarioTrabalhoRequisicao): Promise<HorarioTrabalho> => {
    const response = await api.post<HorarioTrabalho>('', dados)
    return response.data
  },

  listar: async (profissionalUuid: string): Promise<HorarioTrabalho[]> => {
    const response = await api.get<HorarioTrabalho[]>('', {
      params: { profissionalUuid },
    })
    return response.data
  },

  /** Sem autenticação — usado pela tela pública de agendamento. */
  listarPublico: async (
    profissionalUuid: string,
  ): Promise<HorarioTrabalho[]> => {
    const response = await axios.get<HorarioTrabalho[]>(`${BASE_URL}/public`, {
      params: { profissionalUuid },
    })
    return response.data
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/${id}`)
  },
}
