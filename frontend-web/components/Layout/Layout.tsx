'use client'

import { ReactNode, useState } from 'react'
import Sidebar from './Sidebar'
import './Layout.css'

interface LayoutProps {
  children: ReactNode
  userRole: 'ADMIN' | 'NURSE'
}

const Layout = ({ children, userRole }: LayoutProps) => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true)

  const toggleSidebar = () => {
    setIsSidebarOpen(!isSidebarOpen)
  }

  return (
    <div className="layout">
      <Sidebar
        isOpen={isSidebarOpen}
        toggleSidebar={toggleSidebar}
        userRole={userRole}
      />
      <main
        className={`main-content ${isSidebarOpen ? 'sidebar-open' : 'sidebar-closed'}`}
      >
        {children}
      </main>
    </div>
  )
}

export default Layout
