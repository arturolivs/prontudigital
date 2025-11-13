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
import { User, LoginCredentials } from '../types/auth'

interface AuthContextType {
  user: User | null
  login: (credentials: LoginCredentials) => Promise<void>
  logout: () => Promise<void>
  isLoading: boolean
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
    // Proteção de rotas no cliente
    if (!isLoading) {
      const publicRoutes = ['/login']
      const isPublicRoute = publicRoutes.includes(pathname)
      const isProtectedRoute = pathname.startsWith('/dashboard')

      if (isProtectedRoute && !user) {
        router.push('/login')
      }

      if (isPublicRoute && user) {
        router.push('/dashboard')
      }
    }
  }, [pathname, user, isLoading, router])

  const checkAuth = async () => {
    const token = tokenService.getToken()
    if (token) {
      try {
        const userData = decodeJWT(token)
        setUser(userData)
      } catch {
        tokenService.clearTokens()
      }
    }
    setIsLoading(false)
  }

  const decodeJWT = (token: string): User => {
    try {
      const payload = token.split('.')[1]
      const decoded = atob(payload)
      return JSON.parse(decoded)
    } catch {
      throw new Error('Token inválido')
    }
  }

  const login = async (credentials: LoginCredentials) => {
    try {
      const response = await authAPI.login(credentials)
      tokenService.setTokens(response.token, response.refreshToken)
      setUser(response.user)
      router.push('/dashboard')
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Erro ao fazer login')
    }
  }

  const logout = async () => {
    await authAPI.logout()
    tokenService.clearTokens()
    setUser(null)
    router.push('/login')
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
