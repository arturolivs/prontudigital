import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import {
  ConfiguracaoClinica,
  ConfiguracaoClinicaRequisicao,
} from '../tipos/configuracao'

const API_BASE_URL = `${(
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9090'
).replace(/\/+$/, '')}/api/configuracao`

const api = axios.create()

setupRefreshInterceptor(api)

export const configuracaoAPI = {
  buscar: async (): Promise<ConfiguracaoClinica> => {
    const response = await api.get<ConfiguracaoClinica>(API_BASE_URL)
    return response.data
  },

  atualizar: async (
    dados: ConfiguracaoClinicaRequisicao,
  ): Promise<ConfiguracaoClinica> => {
    const response = await api.put<ConfiguracaoClinica>(API_BASE_URL, dados)
    return response.data
  },

  enviarLogo: async (arquivo: File): Promise<ConfiguracaoClinica> => {
    const form = new FormData()
    form.append('arquivo', arquivo)
    const response = await api.post<ConfiguracaoClinica>(
      `${API_BASE_URL}/logo`,
      form,
    )
    return response.data
  },

  removerLogo: async (): Promise<ConfiguracaoClinica> => {
    const response = await api.delete<ConfiguracaoClinica>(
      `${API_BASE_URL}/logo`,
    )
    return response.data
  },

  /**
   * URL do binário da logo, para usar em `<img src>`.
   *
   * O parâmetro `v` força o navegador a rebaixar o cache depois de um upload —
   * a URL é fixa, então sem ele a logo antiga continuaria aparecendo.
   */
  urlLogo: (versao?: string): string =>
    `${API_BASE_URL}/logo${versao ? `?v=${encodeURIComponent(versao)}` : ''}`,
}
