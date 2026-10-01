'use client'

import { ReactNode } from 'react'
import { Activity, LogOut } from 'lucide-react'
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
            <Activity size={24} strokeWidth={2} />
            <span>ProntuDigital</span>
          </div>

          <div className="lp-header-right">
            {usuario?.nomeCompleto && (
              <span className="lp-saudacao">
                Olá, <strong>{usuario.nomeCompleto.split(' ')[0]}</strong>
              </span>
            )}
            <button
              className="lp-logout-btn"
              onClick={() => logout()}
              title="Sair"
            >
              <LogOut size={15} strokeWidth={2} />
              Sair
            </button>
          </div>
        </div>
      </header>

      <main className="lp-main">
        <div className="lp-content">{children}</div>
      </main>
    </div>
  )
}

export default LayoutPaciente
