import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import { AuthProvider } from '../contexts/AuthContext'
import { ProtetorDeRota } from '../components/ProtetorDeRota'
import './globals.css'
import ConteinerNotificacoes from '@/components/Toast/ToastContainer'
import { ProvedorNotificacao } from '@/contexts/ToastContext'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'ProntuDigital',
  description: 'Sistema de login com Next.js e JWT',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="pt-BR">
      <body className={inter.className} suppressHydrationWarning>
        <AuthProvider>
          <ProvedorNotificacao>
            <ProtetorDeRota>{children}</ProtetorDeRota>
            <ConteinerNotificacoes />
          </ProvedorNotificacao>
        </AuthProvider>
      </body>
    </html>
  )
}
