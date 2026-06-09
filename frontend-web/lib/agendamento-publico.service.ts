import axios from 'axios'
import { BloqueioHorario } from '../tipos/bloqueio'

const BASE = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:9090'

export interface ProfissionalPublico {
  uuid: string
  nomeCompleto: string
}

export const agendamentoPublicoAPI = {
  listarProfissionais: async (): Promise<ProfissionalPublico[]> => {
    const response = await axios.get<ProfissionalPublico[]>(
      `${BASE}/api/public/profissionais`,
    )
    return response.data
  },

  listarBloqueios: async (
    profissionalUuid: string,
    data: string,
  ): Promise<BloqueioHorario[]> => {
    const response = await axios.get<BloqueioHorario[]>(
      `${BASE}/api/bloqueios-horario/public`,
      { params: { profissionalUuid, inicio: data, fim: data } },
    )
    return response.data
  },
}
