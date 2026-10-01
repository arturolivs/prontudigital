import axios from 'axios'
import { setupRefreshInterceptor } from './auth.service'
import {
  FiltrosRelatorio,
  FormatoExportacao,
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

  /**
   * RF21 — baixa o relatório em PDF/XLSX. Vai por axios (não por link direto)
   * para que o interceptor mande o token; um `<a href>` não autenticaria.
   */
  exportarAtendimentos: async (
    filtros: FiltrosRelatorio,
    formato: FormatoExportacao,
  ): Promise<void> => {
    const response = await api.get('/atendimentos/exportar', {
      params: {
        inicio: filtros.inicio,
        fim: filtros.fim,
        profissionalUuid: filtros.profissionalUuid || undefined,
        status: filtros.status || undefined,
        tipo: filtros.tipo || undefined,
        formato,
      },
      responseType: 'blob',
    })
    salvarArquivo(response, `atendimentos_${filtros.inicio}_a_${filtros.fim}`)
  },

  exportarOcupacao: async (
    inicio: string,
    fim: string,
    formato: FormatoExportacao,
    profissionalUuid?: string,
  ): Promise<void> => {
    const response = await api.get('/ocupacao/exportar', {
      params: {
        inicio,
        fim,
        profissionalUuid: profissionalUuid || undefined,
        formato,
      },
      responseType: 'blob',
    })
    salvarArquivo(response, `ocupacao_${inicio}_a_${fim}`)
  },
}

/**
 * Dispara o download do blob. Prefere o nome vindo do Content-Disposition e
 * recorre a `nomePadrao` quando o cabeçalho não chega (proxy pode removê-lo).
 */
function salvarArquivo(
  response: { data: Blob; headers: Record<string, unknown> },
  nomePadrao: string,
) {
  const blob = response.data
  const disposicao = String(response.headers['content-disposition'] ?? '')
  const encontrado = disposicao.match(/filename="?([^";]+)"?/)
  const extensao = blob.type.includes('spreadsheet') ? 'xlsx' : 'pdf'
  const nome = encontrado?.[1] ?? `${nomePadrao}.${extensao}`

  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = nome
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
