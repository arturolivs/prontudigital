import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import { AuthProvider } from '../contexts/AuthContext'
import { RouteProtectionWrapper } from '../components/RouteProtectionWrapper'
import './globals.css'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'Sistema de Autenticação',
  description: 'Sistema de login com Next.js e JWT',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="pt-BR">
      <body className={inter.className}>
        <AuthProvider>
          <RouteProtectionWrapper>{children}</RouteProtectionWrapper>
        </AuthProvider>
      </body>
    </html>
  )
}
