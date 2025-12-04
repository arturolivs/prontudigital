'use client'

import { useState, useEffect } from 'react'
import { ProtectedRoute } from '../../components/ProtectedRoute'
import { useAuth } from '../../contexts/AuthContext'
import { appointmentsAPI } from '../../lib/appointments'
import { Appointment } from '../../types/appointment'
import Layout from '@/components/Layout/Layout'

import './styles.css'

export default function AppointmentsPage() {
  const { user, logout, getProfessionalUuid, hasRole } = useAuth()
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split('T')[0],
  )

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
      setError('')

      const professionalUuid = getProfessionalUuid()

      if (!professionalUuid) {
        setError('Professional UUID não encontrado')
        setLoading(false)
        return
      }

      const viewType = 'day'

      const data = await appointmentsAPI.getAppointments(
        professionalUuid,
        viewType,
        selectedDate,
      )

      setAppointments(data)
    } catch (err: any) {
      setError('Erro ao carregar agendamentos')
      console.error('Erro:', err)
    } finally {
      setLoading(false)
    }
  }

  const formatDateTime = (dateTime: string) => {
    return new Date(dateTime).toLocaleString('pt-BR')
  }

  const getRoleBadgeColor = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-purple-100 text-purple-800'
      case 'NURSE':
        return 'bg-blue-100 text-blue-800'
      case 'DOCTOR':
        return 'bg-green-100 text-green-800'
      default:
        return 'bg-gray-100 text-gray-800'
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
        <h1 className="title">Agenda</h1>
        <div className="content">
          <main className="">
            <div className="px-4 py-6 sm:px-0">
              <div className="bg-white shadow overflow-hidden sm:rounded-lg">
                <div className="px-4 py-5 sm:px-6">
                  <div className="flex justify-between items-center">
                    <div>
                      <h2 className="text-lg font-medium text-gray-900">
                        Lista de Agendamentos -{' '}
                        {new Date(selectedDate).toLocaleDateString('pt-BR')}
                      </h2>
                      <p className="mt-1 text-sm text-gray-600">
                        Professional UUID: {getProfessionalUuid()}
                      </p>
                    </div>
                    <button
                      onClick={fetchAppointments}
                      disabled={loading}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md text-sm font-medium disabled:opacity-50"
                    >
                      {loading ? 'Carregando...' : 'Atualizar'}
                    </button>
                  </div>
                  <p className="mt-1 text-sm text-gray-600">
                    {appointments.length} agendamento(s) encontrado(s)
                  </p>
                </div>

                {loading && (
                  <div className="flex justify-center py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                  </div>
                )}

                {error && (
                  <div className="mx-4 mb-4 rounded-md bg-red-50 p-4">
                    <div className="text-sm text-red-700">{error}</div>
                  </div>
                )}

                {!loading && !error && (
                  <div className="border-t border-gray-200">
                    <ul className="divide-y divide-gray-200">
                      {appointments.map(appointment => (
                        <li
                          key={appointment.id}
                          className="px-4 py-4 sm:px-6 hover:bg-gray-50"
                        >
                          <div className="flex items-center justify-between">
                            <div className="flex flex-col">
                              <p className="text-sm font-medium text-blue-600">
                                {appointment.patientName}
                              </p>
                              <p className="text-sm text-gray-500">
                                Profissional: {appointment.professionalName}
                              </p>
                              <p className="text-xs text-gray-400 mt-1">
                                Patient UUID: {appointment.patientUuid}
                              </p>
                            </div>
                            <div className="flex flex-col items-end">
                              <p className="text-sm text-gray-900">
                                {formatDateTime(appointment.startDateTime)}
                              </p>
                              <p className="text-sm text-gray-900">
                                até {formatDateTime(appointment.endDateTime)}
                              </p>
                              <div className="flex space-x-2 mt-1">
                                <span
                                  className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                    appointment.status === 'SCHEDULED'
                                      ? 'bg-green-100 text-green-800'
                                      : appointment.status === 'CANCELLED'
                                        ? 'bg-red-100 text-red-800'
                                        : 'bg-yellow-100 text-yellow-800'
                                  }`}
                                >
                                  {appointment.status}
                                </span>
                                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                                  {appointment.type}
                                </span>
                              </div>
                            </div>
                          </div>
                        </li>
                      ))}
                    </ul>

                    {appointments.length === 0 && (
                      <div className="text-center py-12">
                        <p className="text-gray-500">
                          Nenhum agendamento encontrado para esta data.
                        </p>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </main>
        </div>
      </ProtectedRoute>
    </Layout>
  )
}
