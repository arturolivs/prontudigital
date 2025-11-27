'use client'

import { usePathname, useRouter } from 'next/navigation'
import { useAuth } from '../contexts/AuthContext'
import { useEffect } from 'react'

export const useRouteProtection = () => {
  const { user, isLoading } = useAuth()
  const pathname = usePathname()
  const router = useRouter()

  useEffect(() => {
    if (isLoading) return

    console.log('🔄 Verificando proteção de rota:', {
      pathname,
      user: user?.username,
      roles: user?.roles,
    })

    const routeConfig = {
      public: ['/login'],

      protected: {
        '/dashboard': ['ADMIN'],
        '/schedule': ['NURSE', 'DOCTOR', 'ADMIN'],
      },

      defaultRedirects: {
        ADMIN: '/dashboard',
        NURSE: '/schedule',
        DOCTOR: '/schedule',
      },
    }

    const isPublicRoute = routeConfig.public.includes(pathname)
    const isProtectedRoute = Object.keys(routeConfig.protected).some(route =>
      pathname.startsWith(route),
    )

    if (isProtectedRoute && !user) {
      console.log('🚫 Usuário não autenticado, redirecionando para login')
      router.push('/login')
      return
    }

    if (isPublicRoute && user) {
      console.log('✅ Usuário autenticado em rota pública, redirecionando')
      const defaultRoute =
        routeConfig.defaultRedirects[
          user.roles[0] as keyof typeof routeConfig.defaultRedirects
        ] || '/schedule'
      router.push(defaultRoute)
      return
    }

    if (isProtectedRoute && user) {
      const routeKey = Object.keys(routeConfig.protected).find(route =>
        pathname.startsWith(route),
      )

      if (routeKey) {
        const requiredRoles =
          routeConfig.protected[routeKey as keyof typeof routeConfig.protected]
        const hasPermission = requiredRoles.some(role =>
          user.roles.includes(role),
        )

        if (!hasPermission) {
          console.log(
            `🚫 Usuário sem permissão para ${routeKey}, redirecionando para unauthorized`,
          )
          router.push('/unauthorized')
          return
        }
      }
    }
  }, [pathname, user, isLoading, router])
}
