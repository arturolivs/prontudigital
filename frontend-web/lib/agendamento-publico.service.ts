import axios from 'axios'
import { BloqueioHorario } from '../tipos/bloqueio'

const BASE = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:9090'

export interface ProfissionalPublico {
  uuid: string
  nomeCompleto: string
  /** Sem isto a tela pediria a foto de quem não tem e mostraria imagem quebrada. */
  temAvatar?: boolean
}

/**
 * URL pública da foto do profissional, para uso direto em `<img src>`.
 *
 * Diferente do avatar em "Meu perfil" — que é baixado como blob porque um
 * `<img>` não envia o header `Authorization` —, aqui o endpoint é aberto, já
 * que a tela de agendamento é anterior à autenticação.
 */
export const urlAvatarProfissional = (profissionalUuid: string): string =>
  `${BASE}/api/public/profissionais/${profissionalUuid}/avatar`

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
