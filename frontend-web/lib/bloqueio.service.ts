import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import {
  BloqueioHorario,
  BloqueioHorarioRequisicao,
  BloqueioRecorrente,
  BloqueioRecorrenteRequisicao,
} from '../tipos/bloqueio'

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

  // ── Recorrentes ──────────────────────────────────────────────

  criarRecorrente: async (
    dados: BloqueioRecorrenteRequisicao,
  ): Promise<BloqueioRecorrente> => {
    const response = await api.post<BloqueioRecorrente>('/recorrentes', dados)
    return response.data
  },

  listarRecorrentes: async (
    profissionalUuid: string,
  ): Promise<BloqueioRecorrente[]> => {
    const response = await api.get<BloqueioRecorrente[]>('/recorrentes', {
      params: { profissionalUuid },
    })
    return response.data
  },

  removerRecorrente: async (id: number): Promise<void> => {
    await api.delete(`/recorrentes/${id}`)
  },
}
