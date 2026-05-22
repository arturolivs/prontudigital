import axios from 'axios'
import { tokenService } from './auth.service'
import { Usuario, RegistrarRequisicao } from '../tipos/autenticacao'

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9090/api/auth/v1'

const api = axios.create({
  baseURL: API_URL,
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

// Interceptor: trata 401/403
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

  listarUsuarios: async (): Promise<Usuario[]> => {
    try {
      const response = await api.get<Usuario[]>('/users')
      return response.data
    } catch (error) {
      console.error('Erro ao listar usuários:', error)
      throw error
    }
  },

  buscarUsuarioPorId: async (id: number): Promise<Usuario> => {
    try {
      const response = await api.get<Usuario>(`/users/${id}`)
      return response.data
    } catch (error) {
      console.error(`Erro ao buscar usuário ${id}:`, error)
      throw error
    }
  },

  buscarUsuarioPorUuid: async (uuid: string): Promise<Usuario> => {
    try {
      const response = await api.get<Usuario>(`/users/uuid/${uuid}`)
      return response.data
    } catch (error) {
      console.error(`Erro ao buscar usuário com UUID ${uuid}:`, error)
      throw error
    }
  },

  criarUsuario: async (dados: RegistrarRequisicao): Promise<Usuario> => {
    try {
      const response = await api.post<Usuario>('/users', dados)
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
      const response = await api.put<Usuario>(`/users/${id}`, dados)
      return response.data
    } catch (error) {
      console.error(`Erro ao atualizar usuário ${id}:`, error)
      throw error
    }
  },

  atualizarUsuarioPorUuid: async (
    uuid: string,
    dados: Partial<Usuario>,
  ): Promise<Usuario> => {
    try {
      const response = await api.put<Usuario>(`/users/uuid/${uuid}`, dados)
      return response.data
    } catch (error) {
      console.error(`Erro ao atualizar usuário com UUID ${uuid}:`, error)
      throw error
    }
  },

  excluirUsuario: async (id: number): Promise<void> => {
    try {
      await api.delete(`/users/${id}`)
    } catch (error) {
      console.error(`Erro ao excluir usuário ${id}:`, error)
      throw error
    }
  },

  excluirUsuarioPorUuid: async (uuid: string): Promise<void> => {
    try {
      await api.delete(`/users/uuid/${uuid}`)
    } catch (error) {
      console.error(`Erro ao excluir usuário com UUID ${uuid}:`, error)
      throw error
    }
  },

  alternarStatusUsuario: async (
    id: number,
    ativo: boolean,
  ): Promise<Usuario> => {
    try {
      const response = await api.patch<Usuario>(`/users/${id}/status`, {
        ativo,
      })
      return response.data
    } catch (error) {
      console.error(`Erro ao alterar status do usuário ${id}:`, error)
      throw error
    }
  },

  atualizarPerfisUsuario: async (
    id: number,
    perfis: string[],
  ): Promise<Usuario> => {
    try {
      const response = await api.patch<Usuario>(`/users/${id}/roles`, {
        perfis,
      })
      return response.data
    } catch (error) {
      console.error(`Erro ao atualizar perfis do usuário ${id}:`, error)
      throw error
    }
  },

  buscarUsuariosPorPerfil: async (perfil: string): Promise<Usuario[]> => {
    try {
      const response = await api.get<Usuario[]>(`/users/role/${perfil}`)
      return response.data
    } catch (error) {
      console.error(`Erro ao buscar usuários com perfil ${perfil}:`, error)
      throw error
    }
  },

  buscarUsuariosPorStatus: async (ativo: boolean): Promise<Usuario[]> => {
    try {
      const response = await api.get<Usuario[]>(`/users/status/${ativo}`)
      return response.data
    } catch (error) {
      console.error(`Erro ao buscar usuários com status ${ativo}:`, error)
      throw error
    }
  },
}

// Versão raw com auth manual (caso necessário em contextos específicos como SSR)
export const usuariosAPIRaw = {
  listarUsuarios: async (token: string): Promise<Usuario[]> => {
    try {
      const response = await axios.get<Usuario[]>(`${API_URL}/users`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })
      return response.data
    } catch (error) {
      console.error('Erro ao listar usuários:', error)
      throw error
    }
  },
}
