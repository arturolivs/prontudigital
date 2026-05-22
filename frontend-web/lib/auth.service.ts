import axios from 'axios'
import {
  JwtResposta,
  LoginRequisicao,
  UsuarioAutenticado,
} from '../tipos/autenticacao'

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9090/api/auth'

export const autenticacaoAPI = {
  login: async (credenciais: LoginRequisicao): Promise<JwtResposta> => {
    const { data } = await axios.post<JwtResposta>(
      `${API_URL}/login`,
      credenciais,
    )
    return data
  },

  logout: async (): Promise<void> => {
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) {
      await axios.post(`${API_URL}/logout`, { refreshToken })
    }
  },
}

// TODO segurança: localStorage é vulnerável a XSS. O refreshToken deveria estar
// em cookie httpOnly + Secure + SameSite. Mantido aqui apenas pela paridade
// com o código original. Não é seguro pra produção como está.
export const tokenService = {
  getToken: (): string | null => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('authToken')
    }
    return null
  },

  getRefreshToken: (): string | null => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('refreshToken')
    }
    return null
  },

  setTokens: (token: string, refreshToken: string): void => {
    localStorage.setItem('authToken', token)
    localStorage.setItem('refreshToken', refreshToken)
  },

  clearTokens: (): void => {
    localStorage.removeItem('authToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('dadosUsuario')
  },

  setDadosUsuario: (usuario: UsuarioAutenticado): void => {
    localStorage.setItem('dadosUsuario', JSON.stringify(usuario))
  },

  getDadosUsuario: (): UsuarioAutenticado | null => {
    if (typeof window !== 'undefined') {
      const dados = localStorage.getItem('dadosUsuario')
      return dados ? JSON.parse(dados) : null
    }
    return null
  },
}
