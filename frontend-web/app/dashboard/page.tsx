'use client'

import { ProtectedRoute } from '../../components/ProtectedRoute'
import { useAuth } from '../../contexts/AuthContext'

export default function Dashboard() {
  const { user, logout, hasRole, getProfessionalUuid } = useAuth()

  if (!user || !hasRole('ADMIN')) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  return (
    <ProtectedRoute requiredRoles={['ADMIN']}>
      <div className="min-h-screen bg-gray-100">
        <nav className="bg-white shadow-sm">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between h-16">
              <div className="flex items-center">
                <h1 className="text-xl font-semibold">
                  Dashboard Administrativo
                </h1>
              </div>
              <div className="flex items-center space-x-4">
                <div className="flex items-center space-x-2">
                  <span className="text-gray-700">{user?.username}</span>
                  {hasRole('ADMIN') && (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                      ADMIN
                    </span>
                  )}
                  {user?.professionalUuid && (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      Professional
                    </span>
                  )}
                </div>
                <button
                  onClick={logout}
                  className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-md text-sm font-medium"
                >
                  Sair
                </button>
              </div>
            </div>
          </div>
        </nav>

        <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
          <div className="px-4 py-6 sm:px-0">
            <div className="border-4 border-dashed border-gray-200 rounded-lg p-8">
              <h2 className="text-2xl font-bold text-gray-900 mb-4">
                Painel de Administração
              </h2>

              <div className="bg-white p-6 rounded-lg shadow mb-6">
                <h3 className="text-lg font-medium mb-4">
                  Informações do Usuário
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm text-gray-600">Username</p>
                    <p className="font-medium">{user?.username}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Roles</p>
                    <div className="flex space-x-1 mt-1">
                      {user?.roles.map(role => (
                        <span
                          key={role}
                          className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800"
                        >
                          {role}
                        </span>
                      ))}
                    </div>
                  </div>
                  {user?.professionalUuid && (
                    <div className="md:col-span-2">
                      <p className="text-sm text-gray-600">Professional UUID</p>
                      <p className="font-medium text-sm break-all">
                        {user.professionalUuid}
                      </p>
                    </div>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-white p-6 rounded-lg shadow">
                  <h3 className="text-lg font-medium mb-2">Estatísticas</h3>
                  <p className="text-gray-600">Visualize métricas do sistema</p>
                </div>

                <div className="bg-white p-6 rounded-lg shadow">
                  <h3 className="text-lg font-medium mb-2">
                    Gerenciar Usuários
                  </h3>
                  <p className="text-gray-600">Administre contas de usuários</p>
                </div>

                <div className="bg-white p-6 rounded-lg shadow">
                  <h3 className="text-lg font-medium mb-2">Configurações</h3>
                  <p className="text-gray-600">Configure o sistema</p>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </ProtectedRoute>
  )
}
