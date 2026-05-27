export interface Usuario {
  id: number
  uuid: string
  nomeCompleto: string
  username: string
  email: string
  ativo: boolean
  perfis: string[]
  criadoEm: string
  atualizadoEm: string
}

export interface LoginRequisicao {
  username: string
  password: string
}

export interface RegistrarRequisicao {
  nomeCompleto: string
  email: string
  username: string
  password: string
  perfis?: string[]
}

export interface JwtResposta {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  username: string
  perfis: string[]
}

export interface RefreshTokenRequisicao {
  refreshToken: string
}

export interface RefreshTokenResposta {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface UsuarioAutenticado {
  username: string
  perfis: string[]
  token: string
  uuid?: string
  nomeCompleto?: string
}

export const PERFIS = {
  ADMIN: 'ROLE_ADMIN',
  PROFISSIONAL: 'ROLE_PROFISSIONAL',
  USUARIO: 'USUARIO',
} as const

export type Perfil = (typeof PERFIS)[keyof typeof PERFIS]
