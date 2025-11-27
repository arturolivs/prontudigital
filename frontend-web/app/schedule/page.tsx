'use client'

import Calendar from '../../components/Calendar'

const SchedulePage = () => {
  return (
    <div
      style={{
        padding: '20px',
        backgroundColor: '#f5f7fa',
        minHeight: '100vh',
      }}
    >
      <h1 style={{ textAlign: 'center', marginBottom: '20px', color: '#333' }}>
        Agenda e Calendário
      </h1>
      <Calendar />
    </div>
  )
}

export default SchedulePage
