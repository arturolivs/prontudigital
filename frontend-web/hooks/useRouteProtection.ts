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
      publicasComRedirect: ['/login'],

      publicasLivres: ['/agendar'],

      protegidas: {
        '/dashboard': [PERFIS.ADMIN],
        '/agenda': [PERFIS.PROFISSIONAL, PERFIS.ADMIN],
      } as Record<string, string[]>,

      redirecionamentosPadrao: {
        [PERFIS.ADMIN]: '/dashboard',
        [PERFIS.PROFISSIONAL]: '/agenda',
      } as Record<string, string>,
    }

    const match = (lista: string[]) =>
      lista.some(rota => pathname === rota || pathname.startsWith(rota + '/'))

    const rotaPublicaLivre = match(configRotas.publicasLivres)
    const rotaPublicaComRedirect = match(configRotas.publicasComRedirect)
    const rotaProtegida =
      !rotaPublicaLivre &&
      !rotaPublicaComRedirect &&
      Object.keys(configRotas.protegidas).some(
        rota => pathname === rota || pathname.startsWith(rota + '/'),
      )

    if (rotaPublicaLivre) return

    if (rotaProtegida && !usuario) {
      console.log('🚫 Usuário não autenticado, redirecionando para login')
      router.push('/login')
      return
    }

    if (rotaPublicaComRedirect && usuario) {
      console.log('✅ Usuário autenticado em rota pública, redirecionando')
      const rotaPadrao =
        configRotas.redirecionamentosPadrao[usuario.perfis[0]] || '/agenda'
      router.push(rotaPadrao)
      return
    }

    if (rotaProtegida && usuario) {
      const chaveRota = Object.keys(configRotas.protegidas).find(
        rota => pathname === rota || pathname.startsWith(rota + '/'),
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
          router.push('/acesso-negado')
          return
        }
      }
    }
  }, [pathname, usuario, isLoading, router])
}
