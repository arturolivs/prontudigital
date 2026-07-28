export interface Anexo {
  uuid: string
  pacienteUuid: string
  pacienteNome: string
  agendamentoUuid: string | null
  nomeOriginal: string
  tipoConteudo: string
  tamanhoBytes: number
  registradoPor: string | null
  criadoEm: string
}

/** Deve espelhar app.armazenamento.tipos-permitidos no backend. */
export const TIPOS_ACEITOS = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'application/pdf',
] as const

/** Deve espelhar app.armazenamento.tamanho-maximo-bytes (10 MB). */
export const TAMANHO_MAXIMO_BYTES = 10 * 1024 * 1024

export const ehImagem = (tipoConteudo: string): boolean =>
  tipoConteudo.startsWith('image/')

export const formatarTamanho = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
