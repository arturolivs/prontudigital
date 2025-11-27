'use client'

import Link from 'next/link'
import { useAuth } from '../../contexts/AuthContext'

export default function Unauthorized() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-semibold">Acesso Não Autorizado</h1>
            </div>
            {user && (
              <div className="flex items-center space-x-4">
                <span className="text-gray-700">{user.username}</span>
                <button
                  onClick={logout}
                  className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-md text-sm font-medium"
                >
                  Sair
                </button>
              </div>
            )}
          </div>
        </div>
      </nav>

      <div className="flex-grow flex items-center justify-center">
        <div className="text-center">
          <div className="bg-white p-8 rounded-lg shadow-md max-w-md">
            <div className="text-6xl mb-4">🚫</div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">
              Acesso Negado
            </h2>
            <p className="text-gray-600 mb-6">
              Você não tem permissão para acessar esta página com seu nível de
              acesso atual.
            </p>
            <div className="space-y-3">
              <Link
                href="/appointments"
                className="block w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition"
              >
                Ir para Agendamentos
              </Link>
              <Link
                href="/dashboard"
                className="block w-full bg-gray-600 text-white py-2 px-4 rounded-md hover:bg-gray-700 transition"
              >
                Tentar Dashboard
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
