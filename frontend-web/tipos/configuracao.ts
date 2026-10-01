import { Endereco } from './autenticacao'

/**
 * Configuração da clínica atendida por esta instalação.
 *
 * O produto é distribuído em modelo silo — uma stack por cliente —, então há
 * uma única configuração por instalação, sem identificador de cliente.
 */
export interface ConfiguracaoClinica {
  nome: string
  cnpj?: string
  telefone?: string
  email?: string
  site?: string
  endereco?: Endereco
  rodapeDocumentos?: string
  /** O binário vem por GET /api/configuracao/logo, nunca embutido na resposta. */
  temLogo: boolean
  atualizadoEm?: string
}

export interface ConfiguracaoClinicaRequisicao {
  nome: string
  cnpj?: string
  telefone?: string
  email?: string
  site?: string
  endereco?: Endereco
  rodapeDocumentos?: string
}

/** Aceitos no upload da logo — espelha TIPOS_LOGO no backend. */
export const TIPOS_LOGO_ACEITOS = [
  'image/jpeg',
  'image/png',
  'image/webp',
] as const

/** 2 MB — espelha TAMANHO_MAXIMO_LOGO no backend. */
export const TAMANHO_MAXIMO_LOGO_BYTES = 2 * 1024 * 1024
