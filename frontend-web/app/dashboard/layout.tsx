'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { ProtectedRoute } from '../../components/ProtectedRoute'
import { useAuth } from '../../contexts/AuthContext'
import './dashboardLayout.css'

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const pathname = usePathname()
  const { usuario, logout } = useAuth()

  const navigation = [
    { name: 'Dashboard', href: '/dashboard' },
    { name: 'Usuários', href: '/dashboard/usuarios' },
  ]

  return (
    <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
      <div className="dashboard-layout">
        {/* Sidebar */}
        <aside className="dashboard-sidebar">
          <div className="sidebar-header">
            <h2 className="sidebar-logo">Admin</h2>
            <p className="sidebar-user">{usuario?.username}</p>
          </div>
          <nav className="sidebar-nav">
            {navigation.map(item => {
              const isActive = pathname === item.href
              return (
                <Link
                  key={item.name}
                  href={item.href}
                  className={`sidebar-link ${isActive ? 'active' : ''}`}
                >
                  {item.name}
                </Link>
              )
            })}
          </nav>
          <div className="sidebar-footer">
            <button onClick={logout} className="logout-button">
              Sair
            </button>
          </div>
        </aside>

        {/* Conteúdo principal */}
        <main className="dashboard-main">
          <div className="dashboard-content">{children}</div>
        </main>
      </div>
    </ProtectedRoute>
  )
}
