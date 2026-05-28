'use client'

import { RotaProtegida } from '../../components/RotaProtegida'
import Layout from '@/components/Layout/Layout'

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <RotaProtegida perfisNecessarios={['ROLE_ADMIN']}>
      <Layout perfil="ROLE_ADMIN">{children}</Layout>
    </RotaProtegida>
  )
}
