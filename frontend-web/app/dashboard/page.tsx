'use client'

import { useAuth } from '../../contexts/AuthContext'

export default function DashboardPage() {
  const { user: usuraio, temPerfil } = useAuth()

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">
        Dashboard Administrativo
      </h1>

      <div className="bg-white p-6 rounded-lg shadow mb-6">
        <h3 className="text-lg font-medium mb-4">Informações do Usuário</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <p className="text-sm text-gray-600">Username</p>
            <p className="font-medium">{usuraio?.username}</p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Roles</p>
            <div className="flex space-x-1 mt-1">
              {usuraio?.roles.map(role => (
                <span
                  key={role}
                  className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800"
                >
                  {role}
                </span>
              ))}
            </div>
          </div>
          {usuraio?.professionalUuid && (
            <div className="md:col-span-2">
              <p className="text-sm text-gray-600">Professional UUID</p>
              <p className="font-medium text-sm break-all">
                {usuraio.professionalUuid}
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
          <h3 className="text-lg font-medium mb-2">Gerenciar Usuários</h3>
          <p className="text-gray-600">Administre contas de usuários</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow">
          <h3 className="text-lg font-medium mb-2">Configurações</h3>
          <p className="text-gray-600">Configure o sistema</p>
        </div>
      </div>
    </div>
  )
}
