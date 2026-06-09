import axios, { AxiosInstance } from 'axios'
import {
  LoginRequisicao,
  JwtResposta,
  UsuarioAutenticado,
} from '../tipos/autenticacao'

let _accessToken: string | null = null

let _refreshing = false
let _queue: Array<(token: string | null) => void> = []

function _drainQueue(token: string | null) {
  _queue.forEach(cb => cb(token))
  _queue = []
}

export const autenticacaoAPI = {
  login: async (credenciais: LoginRequisicao): Promise<JwtResposta> => {
    const { data } = await axios.post<JwtResposta>(
      '/api/auth/session',
      credenciais,
    )
    return data
  },

  logout: async (): Promise<void> => {
    await axios.delete('/api/auth/session')
  },
}

export const tokenService = {
  getToken(): string | null {
    return _accessToken
  },

  setTokens(accessToken: string, _refreshToken?: string): void {
    _accessToken = accessToken
  },

  clearTokens(): void {
    _accessToken = null
    if (typeof window !== 'undefined') {
      sessionStorage.removeItem('dadosUsuario')
    }
  },

  setDadosUsuario(usuario: UsuarioAutenticado): void {
    if (typeof window !== 'undefined') {
      sessionStorage.setItem('dadosUsuario', JSON.stringify(usuario))
    }
  },

  getDadosUsuario(): UsuarioAutenticado | null {
    if (typeof window === 'undefined') return null
    const raw = sessionStorage.getItem('dadosUsuario')
    return raw ? (JSON.parse(raw) as UsuarioAutenticado) : null
  },

  async refresh(): Promise<string> {
    const { data } = await axios.post<{ accessToken: string }>(
      '/api/auth/refresh',
    )
    _accessToken = data.accessToken
    return data.accessToken
  },
}

export function setupRefreshInterceptor(instance: AxiosInstance): void {
  instance.interceptors.request.use(config => {
    const token = tokenService.getToken()
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  })

  instance.interceptors.response.use(
    response => response,
    async error => {
      const original = error.config as typeof error.config & {
        _retry?: boolean
      }

      if (error.response?.status === 401 && !original._retry) {
        original._retry = true

        if (_refreshing) {
          return new Promise<string>((resolve, reject) => {
            _queue.push(token => {
              if (token) {
                original.headers.Authorization = `Bearer ${token}`
                resolve(instance(original))
              } else {
                reject(error)
              }
            })
          })
        }

        _refreshing = true
        try {
          const newToken = await tokenService.refresh()
          _drainQueue(newToken)
          original.headers.Authorization = `Bearer ${newToken}`
          return instance(original)
        } catch {
          _drainQueue(null)
          tokenService.clearTokens()
          if (typeof window !== 'undefined') {
            window.location.href = '/login'
          }
          return Promise.reject(error)
        } finally {
          _refreshing = false
        }
      }

      if (error.response?.status === 403) {
        return Promise.reject(
          new Error('Acesso negado. Permissão insuficiente.'),
        )
      }

      return Promise.reject(error)
    },
  )
}
