'use client'

import { usePathname, useRouter } from 'next/navigation'
import { useAuth } from '../contexts/AuthContext'
import { useEffect } from 'react'
import { PERFIS } from '../tipos/autenticacao'

export const useRouteProtection = () => {
  const { usuario, isLoading } = useAuth()
  const pathname = usePathname()
  const router = useRouter()

  useEffect(() => {
    if (isLoading) return

    console.log('🔄 Verificando proteção de rota:', {
      pathname,
      username: usuario?.username,
      perfis: usuario?.perfis,
    })

    const configRotas = {
      publicas: ['/login'],

      protegidas: {
        '/dashboard': [PERFIS.ADMIN],
        '/agenda': [PERFIS.ENFERMEIRO, PERFIS.MEDICO, PERFIS.ADMIN],
      } as Record<string, string[]>,

      redirecionamentosPadrao: {
        [PERFIS.ADMIN]: '/dashboard',
        [PERFIS.ENFERMEIRO]: '/agenda',
      } as Record<string, string>,
    }

    const rotaPublica = configRotas.publicas.includes(pathname)
    const rotaProtegida = Object.keys(configRotas.protegidas).some(rota =>
      pathname.startsWith(rota),
    )

    // Não autenticado tentando acessar rota protegida → login
    if (rotaProtegida && !usuario) {
      console.log('🚫 Usuário não autenticado, redirecionando para login')
      router.push('/login')
      return
    }

    // Autenticado em rota pública → redireciona pra home do perfil
    if (rotaPublica && usuario) {
      console.log('✅ Usuário autenticado em rota pública, redirecionando')
      const rotaPadrao =
        configRotas.redirecionamentosPadrao[usuario.perfis[0]] || '/agenda'
      router.push(rotaPadrao)
      return
    }

    // Autenticado em rota protegida → verifica permissão
    if (rotaProtegida && usuario) {
      const chaveRota = Object.keys(configRotas.protegidas).find(rota =>
        pathname.startsWith(rota),
      )

      if (chaveRota) {
        const perfisNecessarios = configRotas.protegidas[chaveRota]
        const temPermissao = perfisNecessarios.some(perfil =>
          usuario.perfis.includes(perfil),
        )

        if (!temPermissao) {
          console.log(
            `🚫 Usuário sem permissão para ${chaveRota}, redirecionando para unauthorized`,
          )
          router.push('/unauthorized')
          return
        }
      }
    }
  }, [pathname, usuario, isLoading, router])
}
