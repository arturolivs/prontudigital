'use client'

import {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
} from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { authAPI, tokenService } from '../lib/auth'
import { User, LoginCredentials, AuthResponse } from '../types/auth'

interface AuthContextType {
  user: User | null
  login: (credentials: LoginCredentials) => Promise<void>
  logout: () => Promise<void>
  isLoading: boolean
  hasRole: (role: string) => boolean
  getProfessionalUuid: () => string | null
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const router = useRouter()
  const pathname = usePathname()

  useEffect(() => {
    checkAuth()
  }, [])

  useEffect(() => {
    if (!isLoading) {
      console.log('🔄 Verificando redirecionamento...', {
        pathname,
        user: user?.username,
        roles: user?.roles,
      })

      const publicRoutes = ['/login']
      const isPublicRoute = publicRoutes.includes(pathname)
      const isProtectedRoute =
        pathname.startsWith('/dashboard') ||
        pathname.startsWith('/appointments')

      if (isProtectedRoute && !user) {
        console.log(
          '🚫 Usuário não autenticado em rota protegida, redirecionando para login',
        )
        router.push('/login')
        return
      }

      if (isPublicRoute && user) {
        console.log(
          '✅ Usuário autenticado em rota pública, redirecionando baseado na role',
        )
        redirectBasedOnRole(user.roles)
        return
      }

      if (isProtectedRoute && user) {
        console.log('🔍 Verificando permissões para rota protegida')

        if (
          pathname.startsWith('/dashboard') &&
          !user.roles.includes('ADMIN')
        ) {
          console.log(
            '🚫 Usuário não é ADMIN tentando acessar dashboard, redirecionando',
          )
          if (user.roles.includes('NURSE') || user.roles.includes('DOCTOR')) {
            router.push('/appointments')
          } else {
            router.push('/unauthorized')
          }
          return
        }

        if (
          pathname.startsWith('/appointments') &&
          !user.roles.some(role => ['NURSE', 'DOCTOR', 'ADMIN'].includes(role))
        ) {
          console.log(
            '🚫 Usuário sem permissão para appointments, redirecionando',
          )
          if (user.roles.includes('ADMIN')) {
            router.push('/dashboard')
          } else {
            router.push('/unauthorized')
          }
          return
        }
      }
    }
  }, [pathname, user, isLoading, router])

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

  const redirectBasedOnRole = (roles: string[]) => {
    if (roles.includes('ADMIN')) {
      console.log('🔄 Redirecionando para dashboard (ADMIN)')
      router.push('/dashboard')
    } else if (roles.includes('NURSE') || roles.includes('DOCTOR')) {
      console.log('🔄 Redirecionando para appointments (NURSE/DOCTOR)')
      router.push('/appointments')
    } else {
      console.log('🔄 Redirecionando para appointments (role padrão)')
      router.push('/appointments')
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

      redirectBasedOnRole(response.roles)
    } catch (error: any) {
      console.error('❌ Erro no login:', error)
      throw new Error(error.response?.data?.message || 'Erro ao fazer login')
    }
  }

  const logout = async () => {
    try {
      console.log('🚪 Fazendo logout...')

      // 🔥 CORREÇÃO: Limpa os estados primeiro
      setUser(null)
      setIsLoading(false)

      // 🔥 CORREÇÃO: Limpa o storage
      tokenService.clearTokens()

      // 🔥 CORREÇÃO: Faz o logout assíncrono mas não espera
      authAPI.logout().catch(error => {
        console.error('Erro no logout do backend:', error)
      })

      // 🔥 CORREÇÃO: Redireciona imediatamente
      router.push('/login')
    } catch (error) {
      console.error('❌ Erro no logout:', error)
      // 🔥 CORREÇÃO: Mesmo com erro, limpa e redireciona
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

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
