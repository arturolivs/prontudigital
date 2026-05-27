'use client'

import { useRouter, usePathname } from 'next/navigation'
import './Layout.css'

interface SidebarProps {
  isOpen: boolean
  toggleSidebar: () => void
  userRole: 'ROLE_ADMIN' | 'ROLE_PROFISSIONAL'
}

interface MenuItem {
  label: string
  path: string
  icon: string
  roles: ('ROLE_ADMIN' | 'ROLE_PROFISSIONAL')[]
}

const Sidebar = ({ isOpen, toggleSidebar, userRole }: SidebarProps) => {
  const router = useRouter()
  const pathname = usePathname()

  const menuItems: MenuItem[] = [
    {
      label: 'Dashboard',
      path: '/dashboard',
      icon: '📊',
      roles: ['ROLE_ADMIN'],
    },
    {
      label: 'Calendário',
      path: '/calendar',
      icon: '📅',
      roles: ['ROLE_ADMIN', 'ENFERMEIRO'],
    },
    {
      label: 'Pacientes',
      path: '/patients',
      icon: '👥',
      roles: ['ROLE_ADMIN', 'ENFERMEIRO'],
    },
    {
      label: 'Prontuários',
      path: '/records',
      icon: '📋',
      roles: ['ROLE_ADMIN', 'ENFERMEIRO'],
    },
    {
      label: 'Relatórios',
      path: '/reports',
      icon: '📈',
      roles: ['ROLE_ADMIN'],
    },
    {
      label: 'Configurações',
      path: '/settings',
      icon: '⚙️',
      roles: ['ROLE_ADMIN'],
    },
  ]

  const filteredMenuItems = menuItems.filter(item =>
    item.roles.includes(userRole),
  )

  const handleNavigation = (path: string) => {
    router.push(path)
  }

  const handleLogout = () => {
    // Implementar lógica de logout
    console.log('Logout realizado')
    router.push('/login')
  }

  return (
    <div className={`sidebar ${isOpen ? 'open' : 'closed'}`}>
      <div className="sidebar-header">
        {isOpen && (
          <div className="logo">
            <h2>ProntoDigital</h2>
          </div>
        )}
        <button className="toggle-btn" onClick={toggleSidebar}>
          {isOpen ? '◀' : '▶'}
        </button>
      </div>

      <nav className="sidebar-nav">
        <ul>
          {filteredMenuItems.map(item => (
            <li key={item.path}>
              <button
                className={`nav-item ${pathname === item.path ? 'active' : ''}`}
                onClick={() => handleNavigation(item.path)}
                title={item.label}
              >
                <span className="nav-icon">{item.icon}</span>
                {isOpen && <span className="nav-label">{item.label}</span>}
              </button>
            </li>
          ))}
        </ul>
      </nav>

      <div className="sidebar-footer">
        <button className="logout-btn" onClick={handleLogout} title="Sair">
          <span className="nav-icon">🚪</span>
          {isOpen && <span className="nav-label">Sair</span>}
        </button>
      </div>
    </div>
  )
}

export default Sidebar
