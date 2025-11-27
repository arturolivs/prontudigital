import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  reactStrictMode: true,
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL,
  },
  async redirects() {
    return [
      // Redirecionamento da raiz para /dashboard ou /login
      // (o AuthContext vai lidar com a lógica de redirecionamento)
      {
        source: '/',
        destination: '/dashboard',
        permanent: false,
      },
    ]
  },
  // Configurações adicionais que você pode precisar
  images: {
    domains: [], // Adicione domínios para otimização de imagens se necessário
  },
  // Para evitar problemas com páginas estáticas
  experimental: {
    // Adicione configurações experimentais se necessário
  },
}

export default nextConfig
