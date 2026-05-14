'use client'

import {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
} from 'react'
import { useRouter } from 'next/navigation'
import { authAPI, tokenService } from '../lib/auth.service'
import { User, LoginCredentials, AuthResponse } from '../tipos/autenticacao'

interface AuthContextType {
  user: User | null
  login: (credentials: LoginCredentials) => Promise<void>
  logout: () => Promise<void>
  isLoading: boolean
  hasRole: (role: string) => boolean
  getProfessionalUuid: () => string | null
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

interface AuthProviderProps {
  children: ReactNode
}

const AuthProviderContent = ({ children }: AuthProviderProps) => {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const router = useRouter()

  useEffect(() => {
    checkAuth()
  }, [])

  const checkAuth = async () => {
    try {
      const token = tokenService.getToken()
      console.log('🔐 CheckAuth - Token encontrado:', !!token)

      if (token) {
        const storedUserData = tokenService.getUserData()

        if (storedUserData) {
          console.log('✅ Usuário recuperado do localStorage:', storedUserData)
          setUser(storedUserData)
          setIsLoading(false)
          return
        }

        try {
          const userData = decodeJWT(token)
          console.log('✅ Usuário do JWT:', userData)

          tokenService.setUserData(userData)
          setUser(userData)
        } catch (error) {
          console.error('❌ Erro ao decodificar JWT:', error)
          tokenService.clearTokens()
          setUser(null)
        }
      } else {
        console.log('❌ Nenhum token encontrado')
        setUser(null)
      }
    } catch (error) {
      console.error('❌ Erro no checkAuth:', error)
      tokenService.clearTokens()
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }

  const decodeJWT = (token: string): User => {
    try {
      const payload = token.split('.')[1]
      const decoded = atob(payload)
      const tokenData = JSON.parse(decoded)

      console.log('📄 Payload do JWT:', tokenData)

      return {
        username: tokenData.sub,
        roles: tokenData.roles || [],
        token: token,
        professionalUuid: tokenData.professionalUuid,
      }
    } catch (error) {
      console.error('❌ Erro ao decodificar JWT:', error)
      throw new Error('Token inválido')
    }
  }

  const hasRole = (role: string): boolean => {
    return user?.roles.includes(role) || false
  }

  const getProfessionalUuid = (): string | null => {
    return user?.professionalUuid || null
  }

  const login = async (credentials: LoginCredentials) => {
    try {
      console.log('🔐 Iniciando login...')
      const response: AuthResponse = await authAPI.login(credentials)
      console.log('✅ Resposta do login:', response)

      tokenService.setTokens(response.accessToken, response.refreshToken)

      const userData: User = {
        username: response.username,
        roles: response.roles,
        token: response.accessToken,
        professionalUuid: response.professionalUuid,
      }

      console.log('💾 Salvando userData no localStorage:', userData)
      tokenService.setUserData(userData)

      setUser(userData)

      const primaryRole = response.roles[0]
      const redirectMap: { [key: string]: string } = {
        ADMIN: '/dashboard',
        NURSE: '/appointments',
        DOCTOR: '/appointments',
      }

      const redirectTo = redirectMap[primaryRole] || '/appointments'
      console.log(`🔄 Redirecionando para ${redirectTo} (role: ${primaryRole})`)
      router.push(redirectTo)
    } catch (error: any) {
      console.error('❌ Erro no login:', error)
      throw new Error(error.response?.data?.message || 'Erro ao fazer login')
    }
  }

  const logout = async () => {
    try {
      console.log('🚪 Fazendo logout...')

      setUser(null)
      setIsLoading(false)
      tokenService.clearTokens()
      authAPI.logout().catch(error => {
        console.error('Erro no logout do backend:', error)
      })

      router.push('/login')
    } catch (error) {
      console.error('❌ Erro no logout:', error)
      tokenService.clearTokens()
      setUser(null)
      router.push('/login')
    }
  }

  const value: AuthContextType = {
    user,
    login,
    logout,
    isLoading,
    hasRole,
    getProfessionalUuid,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  return <AuthProviderContent>{children}</AuthProviderContent>
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
