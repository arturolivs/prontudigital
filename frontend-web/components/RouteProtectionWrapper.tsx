'use client'

import { useRouteProtection } from '../hooks/useRouteProtection'

interface RouteProtectionWrapperProps {
  children: React.ReactNode
}

export const RouteProtectionWrapper = ({
  children,
}: RouteProtectionWrapperProps) => {
  useRouteProtection()
  return <>{children}</>
}
