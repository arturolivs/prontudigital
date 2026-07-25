/** Endereço do usuário (RF04). */
export interface Endereco {
  cep?: string
  logradouro?: string
  numero?: string
  complemento?: string
  bairro?: string
  cidade?: string
  uf?: string
}

export interface Usuario {
  id: number
  uuid: string
  nomeCompleto: string
  username: string
  email?: string
  telefone?: string
  /** Somente dígitos — a máscara é aplicada na exibição (RF04). */
  cpf?: string
  /** ISO date (yyyy-MM-dd) (RF04). */
  dataNascimento?: string
  endereco?: Endereco
  /** Registro no COREN do profissional (RF05). */
  coren?: string
  /** Especialidade do profissional (RF05). */
  especialidade?: string
  ativo: boolean
  acessoAtivado: boolean
  perfis: string[]
  criadoEm: string
  atualizadoEm: string
}

/** Dados pessoais que o próprio usuário edita (PATCH /{id}/perfil). */
export interface AtualizarPerfilRequisicao {
  nomeCompleto: string
  email?: string
  telefone?: string
  cpf?: string
  dataNascimento?: string
  endereco?: Endereco
}

export interface LoginRequisicao {
  username: string
  senha: string
}

export interface RegistrarRequisicao {
  nomeCompleto: string
  email: string
  username: string
  senha: string
  telefone?: string
  perfis?: string[]
}

export interface CadastrarPacienteRequisicao {
  nomeCompleto: string
  telefone: string
}

export interface AtivarAcessoRequisicao {
  telefone: string
  email: string
  username: string
  senha: string
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
  PACIENTE: 'ROLE_PACIENTE',
  USUARIO: 'USUARIO',
} as const

export type Perfil = (typeof PERFIS)[keyof typeof PERFIS]
