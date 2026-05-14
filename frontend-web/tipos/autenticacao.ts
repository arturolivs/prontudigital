export interface UsuarioDTO {
  id: number
  uuid: string
  nomeCompleto: string
  username: string
  email: string
  ativo: boolean
  perfis: string[] // ex: ["ADMIN", "PROFISSIONAL"]
  createdAt: string
  updatedAt: string
}

export interface LoginRequisicaoDTO {
  username: string
  password: string
}

export interface RegistrarRequisicaoDTO {
  nomeCompleto: string
  email: string
  username: string
  password: string
  perfis?: string[]
}

export interface JwtRespostaDTO {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  username: string
  perfis: string[]
}

export interface RefreshTokenRequisicaoDTO {
  refreshToken: string
}

export interface RefreshTokenRespostaDTO {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}
