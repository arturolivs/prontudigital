import axios from 'axios'
import { tokenService } from './auth.service'
import { Usuario, RegistrarRequisicao } from '../tipos/autenticacao'

const USUARIOS_API_URL =
  process.env.NEXT_PUBLIC_USUARIOS_API_URL ||
  'http://localhost:9090/api/usuarios'

const AUTH_API_URL =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9090/api/auth'

const api = axios.create({
  baseURL: USUARIOS_API_URL,
})

api.interceptors.request.use(
  config => {
    const token = tokenService.getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error),
)

// TODO arquitetura: não há refresh automático. O backend expõe refresh token
// mas o frontend nunca o usa — o usuário é deslogado quando o access token
// expira (15 min). Implementar refresh com fila de requisições.
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 403) {
      console.error('Acesso negado:', error.response.data)
      return Promise.reject(
        new Error('Acesso negado. Permissão de administrador necessária.'),
      )
    }

    if (error.response?.status === 401) {
      console.error('Não autenticado:', error.response.data)
      if (typeof window !== 'undefined') {
        tokenService.clearTokens()
        // TODO: trocar window.location por router.push do Next pra evitar full reload
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  },
)

export const usuariosAPI = {
  buscarUsuarioAtual: async (): Promise<Usuario> => {
    try {
      const response = await api.get<Usuario>('/me')
      return response.data
    } catch (error) {
      console.error('Erro ao buscar usuário atual:', error)
      throw error
    }
  },

  listarUsuarios: async (): Promise<Usuario[]> => {
    try {
      const response = await api.get<Usuario[]>('')
      return response.data
    } catch (error) {
      console.error('Erro ao listar usuários:', error)
      throw error
    }
  },

  buscarUsuarioPorId: async (id: number): Promise<Usuario> => {
    try {
      const response = await api.get<Usuario>(`/${id}`)
      return response.data
    } catch (error) {
      console.error(`Erro ao buscar usuário ${id}:`, error)
      throw error
    }
  },

  buscarUsuarioPorUuid: async (uuid: string): Promise<Usuario> => {
    try {
      const response = await api.get<Usuario>(`/uuid/${uuid}`)
      return response.data
    } catch (error) {
      console.error(`Erro ao buscar usuário com UUID ${uuid}:`, error)
      throw error
    }
  },

  criarUsuario: async (dados: RegistrarRequisicao): Promise<Usuario> => {
    try {
      const response = await axios.post<Usuario>(
        `${AUTH_API_URL}/registrar`,
        dados,
      )
      return response.data
    } catch (error) {
      console.error('Erro ao criar usuário:', error)
      throw error
    }
  },

  atualizarUsuario: async (
    id: number,
    dados: Partial<Usuario>,
  ): Promise<Usuario> => {
    try {
      const response = await api.put<Usuario>(`/${id}`, dados)
      return response.data
    } catch (error) {
      console.error(`Erro ao atualizar usuário ${id}:`, error)
      throw error
    }
  },

  excluirUsuario: async (id: number): Promise<void> => {
    try {
      await api.delete(`/${id}`)
    } catch (error) {
      console.error(`Erro ao excluir usuário ${id}:`, error)
      throw error
    }
  },
}
