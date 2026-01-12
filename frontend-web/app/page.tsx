'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '../contexts/AuthContext'
import Loading from './loading'

export default function Home() {
  const { user, isLoading } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (!isLoading) {
      if (user) {
        if (user.roles.includes('ADMIN')) {
          router.push('/dashboard')
        } else {
          router.push('/schedule')
        }
      } else {
        router.push('/login')
      }
    }
  }, [user, isLoading, router])

  return <Loading />
}
