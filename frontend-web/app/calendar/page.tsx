'use client'

import Layout from '@/components/Layout/Layout'
import Calendar from '@/components/Calendar'

const CalendarPage = () => {
  // Em uma aplicação real, isso viria do contexto de autenticação
  const userRole: 'NURSE' = 'NURSE'

  return (
    <Layout userRole={userRole}>
      <div
        style={{
          padding: '20px',
          backgroundColor: '#f5f7fa',
          minHeight: '100vh',
        }}
      >
        <h1
          style={{ textAlign: 'center', marginBottom: '20px', color: '#333' }}
        >
          Calendário
        </h1>
        <Calendar />
      </div>
    </Layout>
  )
}

export default CalendarPage
