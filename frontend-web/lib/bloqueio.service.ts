import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import { BloqueioHorario, BloqueioHorarioRequisicao } from '../tipos/bloqueio'

const BASE_URL =
  process.env.NEXT_PUBLIC_BLOQUEIOS_API_URL ||
  'http://localhost:9090/api/bloqueios-horario'

const api = axios.create({ baseURL: BASE_URL })

setupRefreshInterceptor(api)

export const bloqueioAPI = {
  criar: async (dados: BloqueioHorarioRequisicao): Promise<BloqueioHorario> => {
    const response = await api.post<BloqueioHorario>('', dados)
    return response.data
  },

  listar: async (
    profissionalUuid: string,
    inicio: string,
    fim: string,
  ): Promise<BloqueioHorario[]> => {
    const response = await api.get<BloqueioHorario[]>('', {
      params: { profissionalUuid, inicio, fim },
    })
    return response.data
  },

  remover: async (id: number): Promise<void> => {
    await api.delete(`/${id}`)
  },
}
