'use client'

import { ReactNode } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import './LayoutPaciente.css'

interface PropsLayoutPaciente {
  children: ReactNode
}

const LayoutPaciente = ({ children }: PropsLayoutPaciente) => {
  const { usuario, logout } = useAuth()

  return (
    <div className="lp-root">
      <header className="lp-header">
        <div className="lp-header-inner">
          <div className="lp-logo">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
            </svg>
            <span>ProntuDigital</span>
          </div>

          <div className="lp-header-right">
            {usuario?.nomeCompleto && (
              <span className="lp-saudacao">
                Olá, <strong>{usuario.nomeCompleto.split(' ')[0]}</strong>
              </span>
            )}
            <button className="lp-logout-btn" onClick={() => logout()} title="Sair">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
              Sair
            </button>
          </div>
        </div>
      </header>

      <main className="lp-main">
        <div className="lp-content">
          {children}
        </div>
      </main>
    </div>
  )
}

export default LayoutPaciente
