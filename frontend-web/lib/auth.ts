import axios from 'axios'
import { AuthResponse, LoginCredentials, User } from '../types/auth'

const mock_login = {
  accessToken:
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2FvLnNpbHZhQGNsaW5pY2EuY29tIiwicm9sZXMiOlsiRE9DVE9SIl0sInByb2Zlc3Npb25hbFV1aWQiOiJkM2Y1YTdiMi1jOGU0LTRhOWQtYjFmMi04ZTZjNWEzYjlkN2YiLCJpYXQiOjE3MDM1MDAwMDAsImV4cCI6MTcwMzUwMzYwMH0.mock_doctor_token_123456',
  refreshToken:
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2FvLnNpbHZhQGNsaW5pY2EuY29tIiwiaWF0IjoxNzAzNTAwMDAwLCJleHAiOjE3MDQxMDQ4MDB9.mock_doctor_refresh_789012',
  tokenType: 'Bearer',
  expiresIn: 3600,
  username: 'joao.silva@clinica.com',
  roles: ['DOCTOR'],
  professionalUuid: 'd3f5a7b2-c8e4-4a9d-b1f2-8e6c5a3b9d7f',
}

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/auth/v1'

export const authAPI = {
  login: async (credentials: LoginCredentials): Promise<AuthResponse> =>
    await axios.post(`${API_URL}/signin`, credentials),

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
