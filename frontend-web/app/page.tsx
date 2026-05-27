'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '../contexts/AuthContext'
import Loading from './loading'

export default function Home() {
  const { usuario, isLoading } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (!isLoading) {
      if (usuario) {
        if (usuario.perfis.includes('ROLE_ADMIN')) {
          router.push('/dashboard')
        } else if (usuario.perfis.includes('ENFERMEIRO')) {
          router.push('/agenda')
        }
      } else {
        router.push('/login')
      }
    }
  }, [usuario, isLoading, router])

  return <Loading />
}
