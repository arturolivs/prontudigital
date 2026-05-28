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
      const token = tokenService.getToken()
      console.log('🔐 verificarAutenticacao - Token encontrado:', !!token)

      if (!token) {
        console.log('❌ Nenhum token encontrado')
        setUsuario(null)
        return
      }

      const dadosArmazenados = tokenService.getDadosUsuario()
      if (dadosArmazenados) {
        console.log('✅ Usuário recuperado do localStorage:', dadosArmazenados)
        setUsuario(dadosArmazenados)
        return
      }

      try {
        const dadosUsuario = decodificarJWT(token)
        console.log('✅ Usuário do JWT:', dadosUsuario)
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
    try {
      const payloadBase64Url = token.split('.')[1]
      if (!payloadBase64Url) throw new Error('JWT sem payload')

      const base64 = payloadBase64Url.replace(/-/g, '+').replace(/_/g, '/')
      const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
      const decoded = atob(padded)
      const claims = JSON.parse(decoded)

      console.log('📄 Payload do JWT:', claims)

      const perfis: string[] = claims.perfis ?? claims.roles ?? []

      return {
        username: claims.sub,
        perfis,
        token,
      }
    } catch (error) {
      console.error('❌ Erro ao decodificar JWT:', error)
      throw new Error('Token inválido')
    }
  }

  const temPerfil = (perfil: string): boolean => {
    return usuario?.perfis.includes(perfil) ?? false
  }

  const login = async (credenciais: LoginRequisicao) => {
    try {
      console.log('🔐 Iniciando login...')
      const resposta: JwtResposta = await autenticacaoAPI.login(credenciais)
      console.log('✅ Resposta do login:', resposta)

      tokenService.setTokens(resposta.accessToken, resposta.refreshToken)

      const dadosUsuario: UsuarioAutenticado = {
        username: resposta.username,
        perfis: resposta.perfis,
        token: resposta.accessToken,
      }

      try {
        const usuarioCompleto = await usuariosAPI.buscarUsuarioAtual()
        dadosUsuario.uuid = usuarioCompleto.uuid
        dadosUsuario.nomeCompleto = usuarioCompleto.nomeCompleto
      } catch {
        console.warn(
          'Não foi possível obter dados completos do usuário após login',
        )
      }

      console.log('💾 Salvando dadosUsuario no localStorage:', dadosUsuario)
      tokenService.setDadosUsuario(dadosUsuario)
      setUsuario(dadosUsuario)

      const perfilPrincipal = resposta.perfis[0]
      const mapaRedirecionamento: { [key: string]: string } = {
        [PERFIS.ADMIN]: '/dashboard',
        [PERFIS.PROFISSIONAL]: '/agenda',
        [PERFIS.PACIENTE]: '/minha-agenda',
        [PERFIS.USUARIO]: '/agenda',
      }

      const destino = mapaRedirecionamento[perfilPrincipal] || '/agenda'
      console.log(
        `🔄 Redirecionando para ${destino} (perfil: ${perfilPrincipal})`,
      )
      router.push(destino)
    } catch (error: any) {
      console.error('❌ Erro no login:', error)
      throw new Error(error.response?.data?.message || 'Erro ao fazer login')
    }
  }

  const logout = async () => {
    try {
      console.log('🚪 Fazendo logout...')

      setUsuario(null)
      tokenService.clearTokens()

      // Fire-and-forget no backend — se falhar, o usuário já foi deslogado localmente
      autenticacaoAPI.logout().catch(error => {
        console.error('Erro no logout do backend:', error)
      })

      router.push('/login')
    } catch (error) {
      console.error('❌ Erro no logout:', error)
      tokenService.clearTokens()
      setUsuario(null)
      router.push('/login')
    }
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
