'use client'

import { useRouter, usePathname } from 'next/navigation'
import './Layout.css'

interface PropsBarraLateral {
  aberta: boolean
  alternar: () => void
  perfil: 'ROLE_ADMIN' | 'ROLE_PROFISSIONAL'
}

interface ItemMenu {
  rotulo: string
  caminho: string
  icone: string
  perfis: ('ROLE_ADMIN' | 'ROLE_PROFISSIONAL')[]
}

const BarraLateral = ({ aberta, alternar, perfil }: PropsBarraLateral) => {
  const router = useRouter()
  const pathname = usePathname()

  const itensMenu: ItemMenu[] = [
    {
      rotulo: 'Dashboard',
      caminho: '/dashboard',
      icone: '📊',
      perfis: ['ROLE_ADMIN'],
    },
    {
      rotulo: 'Agenda',
      caminho: '/agenda',
      icone: '📅',
      perfis: ['ROLE_ADMIN', 'ROLE_PROFISSIONAL'],
    },
    {
      rotulo: 'Indisponibilidades',
      caminho: '/bloqueios',
      icone: '🚫',
      perfis: ['ROLE_PROFISSIONAL'],
    },
    {
      rotulo: 'Pacientes',
      caminho: '/patients',
      icone: '👥',
      perfis: ['ROLE_ADMIN', 'ROLE_PROFISSIONAL'],
    },
    {
      rotulo: 'Prontuários',
      caminho: '/records',
      icone: '📋',
      perfis: ['ROLE_ADMIN', 'ROLE_PROFISSIONAL'],
    },
    {
      rotulo: 'Relatórios',
      caminho: '/reports',
      icone: '📈',
      perfis: ['ROLE_ADMIN'],
    },
    {
      rotulo: 'Configurações',
      caminho: '/settings',
      icone: '⚙️',
      perfis: ['ROLE_ADMIN'],
    },
  ]

  const itensFiltrados = itensMenu.filter(item => item.perfis.includes(perfil))

  const navegarPara = (caminho: string) => {
    router.push(caminho)
  }

  const sair = () => {
    router.push('/login')
  }

  return (
    <div className={`sidebar ${aberta ? 'open' : 'closed'}`}>
      <div className="sidebar-header">
        {aberta && (
          <div className="logo">
            <h2>ProntoDigital</h2>
          </div>
        )}
        <button className="toggle-btn" onClick={alternar}>
          {aberta ? '◀' : '▶'}
        </button>
      </div>

      <nav className="sidebar-nav">
        <ul>
          {itensFiltrados.map(item => (
            <li key={item.caminho}>
              <button
                className={`nav-item ${pathname === item.caminho ? 'active' : ''}`}
                onClick={() => navegarPara(item.caminho)}
                title={item.rotulo}
              >
                <span className="nav-icon">{item.icone}</span>
                {aberta && <span className="nav-label">{item.rotulo}</span>}
              </button>
            </li>
          ))}
        </ul>
      </nav>

      <div className="sidebar-footer">
        <button className="logout-btn" onClick={sair} title="Sair">
          <span className="nav-icon">🚪</span>
          {aberta && <span className="nav-label">Sair</span>}
        </button>
      </div>
    </div>
  )
}

export default BarraLateral
