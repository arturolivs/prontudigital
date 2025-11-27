import axios from 'axios'
import { AuthResponse, LoginCredentials, User } from '../types/auth'

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/auth/v1'

export const authAPI = {
  login: async (credentials: LoginCredentials): Promise<AuthResponse> => {
    const response = await axios.post(`${API_URL}/signin`, credentials)
    return response.data
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
