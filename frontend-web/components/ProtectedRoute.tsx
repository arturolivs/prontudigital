'use client'

import { useAuth } from '../contexts/AuthContext'

interface PropsRotaProtegida {
  children: React.ReactNode
  perfisNecessarios?: string[]
}

export const RotaProtegida = ({
  children,
  perfisNecessarios = [],
}: PropsRotaProtegida) => {
  const { usuario, isLoading, temPerfil } = useAuth()

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-2 text-sm text-gray-600">Verificando acesso...</p>
        </div>
      </div>
    )
  }

  if (!usuario) {
    return null
  }

  const temPerfilNecessario =
    perfisNecessarios.length === 0 ||
    perfisNecessarios.some(perfil => temPerfil(perfil))

  if (!temPerfilNecessario) {
    return (
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
            Perfis necessários: {perfisNecessarios.join(', ')}
          </p>
          <p className="text-sm text-gray-500">
            Seus perfis: {usuario.perfis.join(', ')}
          </p>
        </div>
      </div>
    )
  }

  return <>{children}</>
}
