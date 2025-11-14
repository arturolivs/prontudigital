// components/ProtectedRoute.tsx
'use client'

import { useAuth } from '../contexts/AuthContext'

interface ProtectedRouteProps {
  children: React.ReactNode
  requiredRoles?: string[]
}

export const ProtectedRoute = ({
  children,
  requiredRoles = [],
}: ProtectedRouteProps) => {
  const { user, isLoading, hasRole } = useAuth()

  let content: React.ReactNode

  if (isLoading) {
    content = (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-2 text-sm text-gray-600">Verificando acesso...</p>
        </div>
      </div>
    )
  } else if (!user) {
    content = null
  } else if (
    requiredRoles.length > 0 &&
    !requiredRoles.some(role => hasRole(role))
  ) {
    content = (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="text-6xl mb-4">🚫</div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">
            Acesso Negado
          </h2>
          <p className="text-gray-600">
            Você não tem permissão para acessar esta página.
          </p>
          <p className="text-sm text-gray-500 mt-2">
            Suas roles: {user.roles.join(', ')}
          </p>
        </div>
      </div>
    )
  } else {
    content = children
  }

  return <>{content}</>
}
