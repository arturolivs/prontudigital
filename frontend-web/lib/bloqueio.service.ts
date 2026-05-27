import axios from 'axios'
import { tokenService } from './auth.service'
import { BloqueioHorario, BloqueioHorarioRequisicao } from '../tipos/bloqueio'

const BASE_URL =
  process.env.NEXT_PUBLIC_BLOQUEIOS_API_URL ||
  'http://localhost:9090/api/bloqueios-horario'

const api = axios.create({ baseURL: BASE_URL })

api.interceptors.request.use(config => {
  const token = tokenService.getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      tokenService.clearTokens()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

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
