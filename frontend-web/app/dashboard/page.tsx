'use client'

import { ProtectedRoute } from '../../components/ProtectedRoute'
import { useAuth } from '../../contexts/AuthContext'

export default function Dashboard() {
  const { user, logout } = useAuth()

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-gray-100">
        <nav className="bg-white shadow-sm">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between h-16">
              <div className="flex items-center">
                <h1 className="text-xl font-semibold">Dashboard</h1>
              </div>
              <div className="flex items-center space-x-4">
                <span className="text-gray-700">Olá, {user?.name}</span>
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
            <div className="border-4 border-dashed border-gray-200 rounded-lg h-96 p-8">
              <h2 className="text-2xl font-bold text-gray-900 mb-4">
                Bem-vindo ao Dashboard!
              </h2>
              <p className="text-gray-600">
                Esta é uma área protegida. Apenas usuários autenticados podem
                acessar esta página.
              </p>
              <div className="mt-6 p-4 bg-white rounded-lg shadow">
                <h3 className="text-lg font-medium mb-2">
                  Informações do usuário:
                </h3>
                <pre className="text-sm bg-gray-50 p-4 rounded">
                  {JSON.stringify(user, null, 2)}
                </pre>
              </div>
            </div>
          </div>
        </main>
      </div>
    </ProtectedRoute>
  )
}
