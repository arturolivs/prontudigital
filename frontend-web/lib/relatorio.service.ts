import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import {
  FiltrosRelatorio,
  RelatorioAtendimentos,
  RelatorioOcupacao,
} from '../tipos/relatorio'

const BASE_URL =
  process.env.NEXT_PUBLIC_RELATORIOS_API_URL ||
  'http://localhost:9090/api/relatorios'

const api = axios.create({ baseURL: BASE_URL })

setupRefreshInterceptor(api)

export const relatorioAPI = {
  /** RF19 — atendimentos do período, com filtros opcionais. */
  atendimentos: async (
    filtros: FiltrosRelatorio,
  ): Promise<RelatorioAtendimentos> => {
    const response = await api.get<RelatorioAtendimentos>('/atendimentos', {
      params: {
        inicio: filtros.inicio,
        fim: filtros.fim,
        profissionalUuid: filtros.profissionalUuid || undefined,
        status: filtros.status || undefined,
        tipo: filtros.tipo || undefined,
      },
    })
    return response.data
  },

  /** RF20 — comparecimento, cancelamentos e ocupação da agenda. */
  ocupacao: async (
    inicio: string,
    fim: string,
    profissionalUuid?: string,
  ): Promise<RelatorioOcupacao> => {
    const response = await api.get<RelatorioOcupacao>('/ocupacao', {
      params: { inicio, fim, profissionalUuid: profissionalUuid || undefined },
    })
    return response.data
  },
}
