'use client'

import { ReactNode, useState } from 'react'
import BarraLateral from './Sidebar'
import './Layout.css'

interface PropsLayout {
  children: ReactNode
  perfil: 'ROLE_ADMIN' | 'ROLE_PROFISSIONAL'
}

const Layout = ({ children, perfil }: PropsLayout) => {
  const [barraLateralAberta, setBarraLateralAberta] = useState(true)

  const alternarBarraLateral = () => {
    setBarraLateralAberta(!barraLateralAberta)
  }

  return (
    <div className="layout">
      <BarraLateral
        aberta={barraLateralAberta}
        alternar={alternarBarraLateral}
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
