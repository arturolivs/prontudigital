'use client'

import { useRouter, usePathname } from 'next/navigation'
import { useAuth } from '@/contexts/AuthContext'
import {
  LayoutDashboard,
  Calendar,
  Ban,
  Users,
  FileText,
  BarChart3,
  Settings,
  LogOut,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react'
import './Layout.css'

interface PropsBarraLateral {
  aberta: boolean
  alternar: () => void
  perfil: 'ROLE_ADMIN' | 'ROLE_PROFISSIONAL'
}

interface ItemMenu {
  rotulo: string
  caminho: string
  icone: React.ElementType
  perfis: ('ROLE_ADMIN' | 'ROLE_PROFISSIONAL')[]
}

const BarraLateral = ({ aberta, alternar, perfil }: PropsBarraLateral) => {
  const router = useRouter()
  const pathname = usePathname()
  const { logout } = useAuth()

  const itensMenu: ItemMenu[] = [
    {
      rotulo: 'Dashboard',
      caminho: '/dashboard',
      icone: LayoutDashboard,
      perfis: ['ROLE_ADMIN'],
    },
    {
      rotulo: 'Agenda',
      caminho: '/agenda',
      icone: Calendar,
      perfis: ['ROLE_PROFISSIONAL'],
    },
    {
      rotulo: 'Indisponibilidades',
      caminho: '/bloqueios',
      icone: Ban,
      perfis: ['ROLE_PROFISSIONAL'],
    },
    {
      rotulo: 'Pacientes',
      caminho: '/pacientes',
      icone: Users,
      perfis: ['ROLE_PROFISSIONAL'],
    },
    {
      rotulo: 'Prontuários',
      caminho: '/records',
      icone: FileText,
      perfis: ['ROLE_PROFISSIONAL'],
    },
    {
      rotulo: 'Usuários',
      caminho: '/dashboard/usuarios',
      icone: Users,
      perfis: ['ROLE_ADMIN'],
    },
    {
      rotulo: 'Relatórios',
      caminho: '/reports',
      icone: BarChart3,
      perfis: ['ROLE_ADMIN'],
    },
    {
      rotulo: 'Configurações',
      caminho: '/settings',
      icone: Settings,
      perfis: ['ROLE_ADMIN'],
    },
  ]

  const itensFiltrados = itensMenu.filter(item => item.perfis.includes(perfil))

  const navegarPara = (caminho: string) => {
    router.push(caminho)
    if (window.innerWidth <= 768) {
      fechar()
    }
  }

  const sair = () => {
    logout()
  }

  return (
    <>
      {!aberta && (
        <button
          className="sidebar-mobile-toggle"
          onClick={alternar}
          aria-label="Expandir menu"
        >
          <Menu size={22} />
        </button>
      )}
      <div className={`sidebar ${aberta ? 'open' : 'closed'}`}>
        <div className="sidebar-header">
          {aberta && (
            <div className="logo">
              <h2>ProntoDigital</h2>
            </div>
          )}
          <button
            className="toggle-btn"
            onClick={alternar}
            aria-label={aberta ? 'Recolher menu' : 'Expandir menu'}
          >
            {aberta ? <ChevronLeft size={20} /> : <ChevronRight size={20} />}
          </button>
        </div>

        <nav className="sidebar-nav">
          <ul>
            {itensFiltrados.map(item => {
              const Icon = item.icone
              const isActive = pathname === item.caminho
              return (
                <li key={item.caminho}>
                  <button
                    className={`nav-item ${isActive ? 'active' : ''}`}
                    onClick={() => navegarPara(item.caminho)}
                    title={!aberta ? item.rotulo : undefined}
                  >
                    <Icon size={20} className="nav-icon" />
                    {aberta && <span className="nav-label">{item.rotulo}</span>}
                  </button>
                </li>
              )
            })}
          </ul>
        </nav>

        <div className="sidebar-footer">
          <button
            className="logout-btn"
            onClick={sair}
            title={!aberta ? 'Sair' : undefined}
          >
            <LogOut size={20} className="nav-icon" />
            {aberta && <span className="nav-label">Sair</span>}
          </button>
        </div>
      </div>
    </>
  )
}

export default BarraLateral
