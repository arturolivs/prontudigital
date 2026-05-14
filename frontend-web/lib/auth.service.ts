import axios from 'axios'
import { AuthResponse, LoginCredentials, User } from '../tipos/autenticacao'

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9090/api/auth/v1'

const mockAuthResponse: AuthResponse = {
  accessToken:
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c',
  refreshToken: 'dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4tZXhhbXBsZQ==',
  tokenType: 'Bearer',
  expiresIn: 3600,
  username: 'enfermeira.silva',
  roles: ['NURSE'],
  professionalUuid: '123e4567-e89b-12d3-a456-426614174000',
}

export const authAPI = {
  login: async (credentials: LoginCredentials): Promise<AuthResponse> => {
    //const result = await axios.post(`${API_URL}/signin`, credentials)
    return mockAuthResponse
  },
  logout: async (): Promise<void> => {
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) {
      await axios.post(`${API_URL}/logout`, { refreshToken })
    }
  },
}

export const tokenService = {
  getToken: (): string | null => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('authToken')
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
    localStorage.removeItem('userData')
  },

  getProfessionalUuid: (): string | null => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('professionalUuid')
    }
    return null
  },

  setUserData: (userData: User): void => {
    localStorage.setItem('userData', JSON.stringify(userData))
  },

  getUserData: (): User | null => {
    if (typeof window !== 'undefined') {
      const userData = localStorage.getItem('userData')
      return userData ? JSON.parse(userData) : null
    }
    return null
  },
}
