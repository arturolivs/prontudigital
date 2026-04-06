import axios from 'axios'
import { tokenService } from './auth.service'
import { User, RegisterRequest, UserFull } from '../types/auth'

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9090/api/auth/v1'

// Criar instância do axios com configuração padrão
const api = axios.create({
  baseURL: API_URL,
})

// Interceptor para adicionar o token em todas as requisições
api.interceptors.request.use(
  config => {
    const token = tokenService.getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    console.log('@@@@@@@@@', config)

    return config
  },
  error => {
    return Promise.reject(error)
  },
)

// Interceptor para tratar erros de autenticação
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      // Token inválido ou expirado ou sem permissão
      console.error('Erro de autenticação ou permissão:', error.response.data)

      // Se for 403, é problema de permissão (não é admin)
      if (error.response?.status === 403) {
        throw new Error('Acesso negado. Permissão de administrador necessária.')
      }

      // Se for 401, redireciona para login (token expirado)
      if (typeof window !== 'undefined') {
        tokenService.clearTokens()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export const usersAPI = {
  /**
   * Lista todos os usuários (requer role ADMIN)
   * GET /users
   */
  listUsers: async (): Promise<UserFull[]> => {
    try {
      const response = await api.get('/users')
      return response.data
    } catch (error) {
      console.error('Erro ao listar usuários:', error)
      throw error
    }
  },

  /**
   * Busca um usuário por ID (requer role ADMIN)
   * GET /users/{id}
   */
  getUserById: async (id: number): Promise<User> => {
    try {
      const response = await api.get(`/users/${id}`)
      return response.data
    } catch (error) {
      console.error(`Erro ao buscar usuário ${id}:`, error)
      throw error
    }
  },

  /**
   * Busca um usuário por UUID (requer role ADMIN)
   * GET /users/uuid/{uuid}
   */
  getUserByUuid: async (uuid: string): Promise<User> => {
    try {
      const response = await api.get(`/users/uuid/${uuid}`)
      return response.data
    } catch (error) {
      console.error(`Erro ao buscar usuário com UUID ${uuid}:`, error)
      throw error
    }
  },

  /**
   * Cria um novo usuário (requer role ADMIN)
   * POST /users
   */
  createUser: async (userData: RegisterRequest): Promise<UserFull> => {
    try {
      const response = await api.post('/users', userData)
      return response.data
    } catch (error) {
      console.error('Erro ao criar usuário:', error)
      throw error
    }
  },

  /**
   * Atualiza um usuário existente (requer role ADMIN)
   * PUT /users/{id}
   */
  updateUser: async (
    id: number,
    userData: Partial<UserFull>,
  ): Promise<UserFull> => {
    try {
      const response = await api.put(`/users/${id}`, userData)
      return response.data
    } catch (error) {
      console.error(`Erro ao atualizar usuário ${id}:`, error)
      throw error
    }
  },

  /**
   * Atualiza um usuário por UUID (requer role ADMIN)
   * PUT /users/uuid/{uuid}
   */
  updateUserByUuid: async (
    uuid: string,
    userData: Partial<UserFull>,
  ): Promise<UserFull> => {
    try {
      const response = await api.put(`/users/uuid/${uuid}`, userData)
      return response.data
    } catch (error) {
      console.error(`Erro ao atualizar usuário com UUID ${uuid}:`, error)
      throw error
    }
  },

  /**
   * Exclui um usuário (requer role ADMIN)
   * DELETE /users/{id}
   */
  deleteUser: async (id: number): Promise<void> => {
    try {
      await api.delete(`/users/${id}`)
    } catch (error) {
      console.error(`Erro ao excluir usuário ${id}:`, error)
      throw error
    }
  },

  /**
   * Exclui um usuário por UUID (requer role ADMIN)
   * DELETE /users/uuid/{uuid}
   */
  deleteUserByUuid: async (uuid: string): Promise<void> => {
    try {
      await api.delete(`/users/uuid/${uuid}`)
    } catch (error) {
      console.error(`Erro ao excluir usuário com UUID ${uuid}:`, error)
      throw error
    }
  },

  /**
   * Ativa/desativa um usuário (requer role ADMIN)
   * PATCH /users/{id}/status
   */
  toggleUserStatus: async (
    id: number,
    isActive: boolean,
  ): Promise<UserFull> => {
    try {
      const response = await api.patch(`/users/${id}/status`, { isActive })
      return response.data
    } catch (error) {
      console.error(`Erro ao alterar status do usuário ${id}:`, error)
      throw error
    }
  },

  /**
   * Atualiza as roles de um usuário (requer role ADMIN)
   * PATCH /users/{id}/roles
   */
  updateUserRoles: async (id: number, roles: string[]): Promise<UserFull> => {
    try {
      const response = await api.patch(`/users/${id}/roles`, { roles })
      return response.data
    } catch (error) {
      console.error(`Erro ao atualizar roles do usuário ${id}:`, error)
      throw error
    }
  },

  /**
   * Busca usuários por role (requer role ADMIN)
   * GET /users/role/{role}
   */
  getUsersByRole: async (role: string): Promise<UserFull[]> => {
    try {
      const response = await api.get(`/users/role/${role}`)
      return response.data
    } catch (error) {
      console.error(`Erro ao buscar usuários com role ${role}:`, error)
      throw error
    }
  },

  /**
   * Busca usuários por status (ativo/inativo) (requer role ADMIN)
   * GET /users/status/{isActive}
   */
  getUsersByStatus: async (isActive: boolean): Promise<UserFull[]> => {
    try {
      const response = await api.get(`/users/status/${isActive}`)
      return response.data
    } catch (error) {
      console.error(`Erro ao buscar usuários com status ${isActive}:`, error)
      throw error
    }
  },
}

// Exportar também uma versão com autenticação manual (caso precise em algum contexto específico)
export const usersAPIRaw = {
  listUsers: async (token: string): Promise<UserFull[]> => {
    try {
      const response = await axios.get(`${API_URL}/users`, {
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
