import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import { Agendamento } from '../tipos/CareSession'
import {
  AgendamentoRequisicao,
  AgendamentoResposta,
  ReagendarRequisicao,
} from '../tipos/appointment'

const API_BASE_URL =
  process.env.NEXT_PUBLIC_AGENDAMENTOS_API_URL ||
  'http://localhost:9090/api/agendamentos'

const api = axios.create()

setupRefreshInterceptor(api)

const tipoVisualizacaoMap: Record<string, string> = {
  day: 'DIA',
  week: 'SEMANA',
  month: 'MES',
}

export const agendamentoAPI = {
  getCareSessions: async (
    viewType: string,
    date: Date | string,
  ): Promise<Agendamento[]> => {
    const formattedDate =
      typeof date === 'string' ? date : date.toISOString().split('T')[0]
    const tipo = tipoVisualizacaoMap[viewType] || 'DIA'
    const response = await api.get<Agendamento[]>(`${API_BASE_URL}/agenda`, {
      params: { data: formattedDate, tipo },
    })
    return response.data
  },

  criarAgendamento: async (
    dados: AgendamentoRequisicao,
  ): Promise<AgendamentoResposta> => {
    const response = await api.post<AgendamentoResposta>(API_BASE_URL, dados)
    return response.data
  },

  cancelarAgendamento: async (id: number): Promise<void> => {
    await api.patch(`${API_BASE_URL}/${id}/cancelar`)
  },

  concluirAgendamento: async (id: number): Promise<void> => {
    await api.patch(`${API_BASE_URL}/${id}/concluir`)
  },

  reagendarAgendamento: async (
    id: number,
    dados: ReagendarRequisicao,
  ): Promise<AgendamentoResposta> => {
    const response = await api.patch<AgendamentoResposta>(
      `${API_BASE_URL}/${id}/reagendar`,
      dados,
    )
    return response.data
  },

  getTratamentosPorAvaliacao: async (
    avaliacaoId: number,
  ): Promise<Agendamento[]> => {
    const response = await api.get<Agendamento[]>(
      `${API_BASE_URL}/avaliacoes/${avaliacaoId}/tratamentos`,
    )
    return response.data
  },

  getMeusAgendamentos: async (): Promise<Agendamento[]> => {
    const response = await api.get<Agendamento[]>(`${API_BASE_URL}/meus`)
    return response.data
  },
}

// Alias mantido por compatibilidade com a página de detalhes
export const careSessionAPI = agendamentoAPI
