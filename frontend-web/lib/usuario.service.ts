import axios from 'axios'
import { tokenService, setupRefreshInterceptor } from './auth.service'
import {
  Usuario,
  RegistrarRequisicao,
  CadastrarPacienteRequisicao,
  JwtResposta,
} from '../tipos/autenticacao'
import { PageResponse } from '../tipos/paginacao'

const USUARIOS_API_URL =
  process.env.NEXT_PUBLIC_USUARIOS_API_URL ||
  'http://localhost:9090/api/usuarios'

// NEXT_PUBLIC_API_URL é a base pública da API, SEM sufixo de caminho
// (ex.: https://prontudigital.com.br). O caminho é montado aqui.
const AUTH_API_URL = `${(
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9090'
).replace(/\/+$/, '')}/api/auth`

const api = axios.create({ baseURL: USUARIOS_API_URL })

setupRefreshInterceptor(api)

export const usuariosAPI = {
  buscarUsuarioAtual: async (): Promise<Usuario> => {
    const response = await api.get<Usuario>('/me')
    return response.data
  },

  listarUsuarios: async (
    page = 0,
    size = 10,
  ): Promise<PageResponse<Usuario>> => {
    const response = await api.get<PageResponse<Usuario>>('', {
      params: { page, size },
    })
    return response.data
  },

  buscarUsuarioPorId: async (id: number): Promise<Usuario> => {
    const response = await api.get<Usuario>(`/${id}`)
    return response.data
  },

  buscarUsuarioPorUuid: async (uuid: string): Promise<Usuario> => {
    const response = await api.get<Usuario>(`/uuid/${uuid}`)
    return response.data
  },

  criarUsuario: async (dados: RegistrarRequisicao): Promise<Usuario> => {
    const response = await axios.post<Usuario>(
      `${AUTH_API_URL}/registrar`,
      dados,
    )
    return response.data
  },

  cadastrarPaciente: async (
    dados: CadastrarPacienteRequisicao,
  ): Promise<JwtResposta> => {
    const response = await axios.post<JwtResposta>(
      `${AUTH_API_URL}/cadastrar-paciente`,
      dados,
    )
    return response.data
  },

  atualizarUsuario: async (
    id: number,
    dados: Partial<Usuario>,
  ): Promise<Usuario> => {
    const response = await api.put<Usuario>(`/${id}`, dados)
    return response.data
  },

  atualizarPerfil: async (
    id: number,
    dados: { nomeCompleto: string; email?: string; telefone?: string },
  ): Promise<Usuario> => {
    const response = await api.patch<Usuario>(`/${id}/perfil`, dados)
    return response.data
  },

  alterarSenha: async (
    id: number,
    senhaAtual: string,
    novaSenha: string,
  ): Promise<void> => {
    await api.patch(`/${id}/senha`, { senhaAtual, novaSenha })
  },

  excluirUsuario: async (id: number): Promise<void> => {
    await api.delete(`/${id}`)
  },
}
