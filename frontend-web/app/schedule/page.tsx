'use client'

import { useState, useEffect } from 'react'
import { ProtectedRoute } from '../../components/ProtectedRoute'
import { useAuth } from '../../contexts/AuthContext'
import { careSessionAPI } from '../../lib/careSession'
import { Appointment } from '../../tipos/appointment'
import Layout from '@/components/Layout/Layout'
import AppointmentFormModal from '@/components/AppointmentFormModal'
import './schedule.css'
import CareSessionList from '@/components/CareSessionList'
import { CareSession } from '@/tipos/CareSession'

export default function CareSessionsPage() {
  const { user, hasRole, getProfessionalUuid } = useAuth()
  const [careSessions, setCaresessions] = useState<CareSession[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split('T')[0],
  )
  const [isModalOpen, setIsModalOpen] = useState(false)

  const hasRequiredRole =
    hasRole('NURSE') || hasRole('DOCTOR') || hasRole('ADMIN')

  useEffect(() => {
    if (user && hasRequiredRole) {
      fetchAppointments()
    }
  }, [selectedDate, user, hasRequiredRole])

  const fetchAppointments = async () => {
    try {
      setLoading(true)
      setError(null)
      /**
 * 
 * 
 * 

      const professionalUuid = getProfessionalUuid()

      if (!professionalUuid) {
        setError('Professional UUID não encontrado')
        setLoading(false)
        return
      }
 */
      const data = await careSessionAPI.getCareSessions('day', selectedDate)

      setCaresessions(data)
    } catch (err: any) {
      setError(err.message || 'Erro ao carregar agendamentos')
      console.error('Erro:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleCreateAppointment = async (
    appointmentData: Partial<Appointment>,
  ) => {
    try {
      const professionalUuid = getProfessionalUuid()

      if (!professionalUuid) {
        setError('Professional UUID não encontrado')
        return
      }

      const newAppointment = {
        ...appointmentData,
        professionalUuid,
        createdAt: new Date().toISOString(),
      }

      await careSessionAPI.createAppointment(newAppointment)

      setIsModalOpen(false)
      fetchAppointments()
    } catch (err: any) {
      setError(err.message || 'Erro ao criar agendamento')
      console.error('Erro:', err)
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
          <div className="flex justify-between items-center mb-6">
            <button
              onClick={() => setIsModalOpen(true)}
              className="bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg flex items-center gap-2 transition-colors"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M10 3a1 1 0 00-1 1v5H4a1 1 0 100 2h5v5a1 1 0 102 0v-5h5a1 1 0 100-2h-5V4a1 1 0 00-1-1z"
                  clipRule="evenodd"
                />
              </svg>
            </button>
          </div>

          <CareSessionList
            sessions={careSessions}
            loading={loading}
            error={error}
          />
        </div>
      </ProtectedRoute>
    </Layout>
  )
}
