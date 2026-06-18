import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import {
  AgendamentoRequisicao,
  Agendamento,
  EvolucaoTratamentoRequisicao,
  PacienteAgendamentosDTO,
  ReagendarRequisicao,
} from '../tipos/agendamento'
import { PageResponse } from '../tipos/paginacao'

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
  buscarPorId: async (id: number): Promise<Agendamento> => {
    const response = await api.get<Agendamento>(`${API_BASE_URL}/${id}`)
    return response.data
  },

  getAgendamentos: async (
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
  ): Promise<Agendamento> => {
    const response = await api.post<Agendamento>(API_BASE_URL, dados)
    return response.data
  },

  confirmarAgendamento: async (id: number): Promise<void> => {
    await api.patch(`${API_BASE_URL}/${id}/confirmar`)
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
  ): Promise<Agendamento> => {
    const response = await api.patch<Agendamento>(
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

  registrarEvolucao: async (
    id: number,
    dados: EvolucaoTratamentoRequisicao,
  ): Promise<Agendamento> => {
    const response = await api.patch<Agendamento>(
      `${API_BASE_URL}/${id}/evolucao`,
      dados,
    )
    return response.data
  },

  listarPacientesComAgendamentos: async (
    busca: string,
    status: string,
    page = 0,
    size = 10,
  ): Promise<PageResponse<PacienteAgendamentosDTO>> => {
    const response = await api.get<PageResponse<PacienteAgendamentosDTO>>(
      `${API_BASE_URL}/pacientes`,
      { params: { busca: busca || undefined, status, page, size } },
    )
    return response.data
  },
}
