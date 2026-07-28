import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import { Procedimento, ProcedimentoRequisicao } from '../tipos/procedimento'

const API_BASE_URL =
  process.env.NEXT_PUBLIC_PROCEDIMENTOS_API_URL ||
  'http://localhost:9090/api/procedimentos'

const api = axios.create()

setupRefreshInterceptor(api)

export const procedimentoAPI = {
  listar: async (incluirInativos = false): Promise<Procedimento[]> => {
    const response = await api.get<Procedimento[]>(API_BASE_URL, {
      params: incluirInativos ? { incluirInativos: true } : undefined,
    })
    return response.data
  },

  buscarPorId: async (id: number): Promise<Procedimento> => {
    const response = await api.get<Procedimento>(`${API_BASE_URL}/${id}`)
    return response.data
  },

  criar: async (dados: ProcedimentoRequisicao): Promise<Procedimento> => {
    const response = await api.post<Procedimento>(API_BASE_URL, dados)
    return response.data
  },

  atualizar: async (
    id: number,
    dados: ProcedimentoRequisicao,
  ): Promise<Procedimento> => {
    const response = await api.put<Procedimento>(`${API_BASE_URL}/${id}`, dados)
    return response.data
  },

  excluir: async (id: number): Promise<void> => {
    await api.delete(`${API_BASE_URL}/${id}`)
  },
}
