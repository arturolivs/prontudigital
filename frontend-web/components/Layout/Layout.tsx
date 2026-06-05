'use client'

import { ReactNode, useEffect, useState } from 'react'
import BarraLateral from './BarraLateral'
import './Layout.css'

interface PropsLayout {
  children: ReactNode
  perfil: 'ROLE_ADMIN' | 'ROLE_PROFISSIONAL'
}

const Layout = ({ children, perfil }: PropsLayout) => {
  const [barraLateralAberta, setBarraLateralAberta] = useState(true)

  useEffect(() => {
    if (window.innerWidth <= 768) {
      setBarraLateralAberta(false)
    }
  }, [])

  const alternarBarraLateral = () => setBarraLateralAberta(prev => !prev)
  const fecharBarraLateral = () => setBarraLateralAberta(false)

  return (
    <div className="layout">
      <BarraLateral
        aberta={barraLateralAberta}
        alternar={alternarBarraLateral}
        fechar={fecharBarraLateral}
        perfil={perfil}
      />
      <main
        className={`main-content ${barraLateralAberta ? 'sidebar-open' : 'sidebar-closed'}`}
      >
        {children}
      </main>
    </div>
  )
}

export default Layout
