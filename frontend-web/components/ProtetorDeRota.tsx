'use client'

import { useRouteProtection } from '../hooks/useRouteProtection'

interface PropsProtetorDeRota {
  children: React.ReactNode
}

export const ProtetorDeRota = ({ children }: PropsProtetorDeRota) => {
  useRouteProtection()
  return <>{children}</>
}
