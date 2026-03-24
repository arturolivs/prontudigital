'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { ProtectedRoute } from '../../components/ProtectedRoute'
import { useAuth } from '../../contexts/AuthContext'

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const pathname = usePathname()
  const { user, logout } = useAuth()

  const navigation = [
    { name: 'Dashboard', href: '/dashboard' },
    { name: 'Usuários', href: '/dashboard/usuarios' },
  ]

  return (
    <ProtectedRoute requiredRoles={['ADMIN']}>
      <div className="min-h-screen bg-gray-100 flex">
        {/* Sidebar */}
        <aside className="w-64 bg-white shadow-md">
          <div className="p-4 border-b">
            <h2 className="text-xl font-semibold text-gray-800">Admin</h2>
            <p className="text-sm text-gray-600 truncate">{user?.username}</p>
          </div>
          <nav className="p-2">
            {navigation.map(item => {
              const isActive = pathname === item.href
              return (
                <Link
                  key={item.name}
                  href={item.href}
                  className={`flex items-center space-x-2 px-4 py-2 rounded-md transition-colors ${
                    isActive
                      ? 'bg-blue-50 text-blue-700'
                      : 'text-gray-700 hover:bg-gray-100'
                  }`}
                >
                  <span>{item.name}</span>
                </Link>
              )
            })}
          </nav>
          <div className="absolute bottom-0 w-64 p-4 border-t">
            <button
              onClick={logout}
              className="flex items-center space-x-2 text-red-600 hover:text-red-800"
            >
              <span>Sair</span>
            </button>
          </div>
        </aside>

        {/* Conteúdo principal */}
        <main className="flex-1 overflow-auto">
          <div className="p-6">{children}</div>
        </main>
      </div>
    </ProtectedRoute>
  )
}
