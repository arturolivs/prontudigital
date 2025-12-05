'use client'

import { useState, useEffect } from 'react'
import { ProtectedRoute } from '../../components/ProtectedRoute'
import { useAuth } from '../../contexts/AuthContext'
import { appointmentsAPI } from '../../lib/appointments'
import { Appointment } from '../../types/appointment'
import Layout from '@/components/Layout/Layout'
import AppointmentList from '@/app/schedule/AppointmentList'

import './schedule.css'

export default function AppointmentsPage() {
  const { user, hasRole, getProfessionalUuid } = useAuth()
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split('T')[0]
  )

  const hasRequiredRole = hasRole('NURSE') || hasRole('DOCTOR') || hasRole('ADMIN')

  useEffect(() => {
    if (user && hasRequiredRole) {
      fetchAppointments()
    }
  }, [selectedDate, user, hasRequiredRole])

  const fetchAppointments = async () => {
    try {
      setLoading(true)
      setError(null)

      const professionalUuid = getProfessionalUuid()

      if (!professionalUuid) {
        setError('Professional UUID não encontrado')
        setLoading(false)
        return
      }

      const data = await appointmentsAPI.getAppointments(
        professionalUuid,
        'day',
        selectedDate
      )

      setAppointments(data)
    } catch (err: any) {
      setError(err.message || 'Erro ao carregar agendamentos')
      console.error('Erro:', err)
    } finally {
      setLoading(false)
    }
  }

  if (!user || !hasRequiredRole) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  return (
    <Layout userRole={'NURSE'}>
      <ProtectedRoute requiredRoles={['NURSE', 'DOCTOR', 'ADMIN']}>
        <div className="appointments-page">
          <AppointmentList 
            appointments={appointments}
            loading={loading}
            error={error}
          />
        </div>
      </ProtectedRoute>
    </Layout>
  )
}