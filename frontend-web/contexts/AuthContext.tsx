'use client'

import {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
} from 'react'
import { useRouter } from 'next/navigation'
import { autenticacaoAPI, tokenService } from '../lib/auth.service'
import { usuariosAPI } from '../lib/usuario.service'
import {
  UsuarioAutenticado,
  LoginRequisicao,
  JwtResposta,
  PERFIS,
} from '../tipos/autenticacao'

interface AuthContextType {
  usuario: UsuarioAutenticado | null
  login: (credenciais: LoginRequisicao) => Promise<void>
  logout: () => Promise<void>
  isLoading: boolean
  temPerfil: (perfil: string) => boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

interface AuthProviderProps {
  children: ReactNode
}

const AuthProviderContent = ({ children }: AuthProviderProps) => {
  const [usuario, setUsuario] = useState<UsuarioAutenticado | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const router = useRouter()

  useEffect(() => {
    verificarAutenticacao()
  }, [])

  const verificarAutenticacao = async () => {
    try {
      let token = tokenService.getToken()

      if (!token) {
        try {
          token = await tokenService.refresh()
        } catch {
          setUsuario(null)
          return
        }
      }

      const dadosArmazenados = tokenService.getDadosUsuario()
      if (dadosArmazenados) {
        setUsuario(dadosArmazenados)
        return
      }

      try {
        const dadosUsuario = decodificarJWT(token)
        tokenService.setDadosUsuario(dadosUsuario)
        setUsuario(dadosUsuario)
      } catch (error) {
        console.error('❌ Erro ao decodificar JWT:', error)
        tokenService.clearTokens()
        setUsuario(null)
      }
    } catch (error) {
      console.error('❌ Erro em verificarAutenticacao:', error)
      tokenService.clearTokens()
      setUsuario(null)
    } finally {
      setIsLoading(false)
    }
  }

  const decodificarJWT = (token: string): UsuarioAutenticado => {
    const payloadBase64Url = token.split('.')[1]
    if (!payloadBase64Url) throw new Error('JWT inválido')

    const base64 = payloadBase64Url.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    const claims = JSON.parse(atob(padded))
    const perfis: string[] = claims.perfis ?? claims.roles ?? []

    return { username: claims.sub, perfis, token }
  }

  const temPerfil = (perfil: string): boolean => {
    return usuario?.perfis.includes(perfil) ?? false
  }

  const login = async (credenciais: LoginRequisicao) => {
    try {
      const resposta: JwtResposta = await autenticacaoAPI.login(credenciais)

      tokenService.setTokens(resposta.accessToken)

      const dadosUsuario: UsuarioAutenticado = {
        username: resposta.username,
        perfis: resposta.perfis,
        token: resposta.accessToken,
      }

      try {
        const usuarioCompleto = await usuariosAPI.buscarUsuarioAtual()
        dadosUsuario.uuid = usuarioCompleto.uuid
        dadosUsuario.nomeCompleto = usuarioCompleto.nomeCompleto
      } catch {}

      tokenService.setDadosUsuario(dadosUsuario)
      setUsuario(dadosUsuario)

      const perfilPrincipal = resposta.perfis[0]
      const mapaRedirecionamento: { [key: string]: string } = {
        [PERFIS.ADMIN]: '/dashboard',
        [PERFIS.PROFISSIONAL]: '/agenda',
        [PERFIS.PACIENTE]: '/minha-agenda',
        [PERFIS.USUARIO]: '/agenda',
      }

      router.push(mapaRedirecionamento[perfilPrincipal] || '/agenda')
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Erro ao fazer login')
    }
  }

  const logout = async () => {
    setUsuario(null)
    tokenService.clearTokens()
    autenticacaoAPI.logout().catch(() => {})
    router.push('/login')
  }

  const value: AuthContextType = {
    usuario,
    login,
    logout,
    isLoading,
    temPerfil,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  return <AuthProviderContent>{children}</AuthProviderContent>
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider')
  }
  return context
}
