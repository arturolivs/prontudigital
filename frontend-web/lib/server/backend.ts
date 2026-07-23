/**
 * Configuração de acesso ao backend a partir do SERVIDOR Next.js.
 *
 * ATENÇÃO: este módulo só pode ser importado por Route Handlers
 * (app/api/**) ou Server Components. A variável BACKEND_INTERNAL_URL
 * NÃO tem o prefixo NEXT_PUBLIC_, portanto é undefined no bundle do
 * navegador — importar daqui em código 'use client' quebra em runtime.
 *
 * Para chamadas feitas pelo navegador use NEXT_PUBLIC_API_URL.
 */

/** Base do backend na rede interna (ex.: http://gateway:9090). Sem barra final. */
const BACKEND_INTERNAL_URL = (
  process.env.BACKEND_INTERNAL_URL ?? 'http://localhost:9090'
).replace(/\/+$/, '')

/** Monta uma URL do módulo de autenticação: urlAuth('login') → <base>/api/auth/login */
export function urlAuth(caminho: string): string {
  return `${BACKEND_INTERNAL_URL}/api/auth/${caminho.replace(/^\/+/, '')}`
}
