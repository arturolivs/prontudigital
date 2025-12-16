'use client'

import React, { useState, useEffect } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { appointmentsAPI } from '../../lib/appointments'
import { Appointment, NewAppointmentData } from '../../types/appointment'
import Layout from '@/components/Layout/Layout'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import AppointmentList from '@/components/AppointmentList'
// ... outros imports

export default function AppointmentsPage() {
  const { user, hasRole, getProfessionalUuid } = useAuth()
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
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
        selectedDate,
      )

      setAppointments(data)
    } catch (err: any) {
      setError(err.message || 'Erro ao carregar agendamentos')
      console.error('Erro:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleAddAppointment = async (appointmentData: NewAppointmentData) => {
    try {
      // Implemente a lógica de criação aqui
      console.log('Criando novo agendamento:', appointmentData)

      // Exemplo de chamada à API:
      // const newAppointment = await appointmentsAPI.createAppointment(appointmentData)

      // Atualizar a lista
      fetchAppointments()

      // Mostrar mensagem de sucesso
      // toast.success('Agendamento criado com sucesso!')
    } catch (error) {
      console.error('Erro ao criar agendamento:', error)
      // toast.error('Erro ao criar agendamento')
    }
  }

  const handleAppointmentClick = (appointment: Appointment) => {
    console.log('Agendamento clicado:', appointment)
    // Navegar para detalhes ou abrir modal de edição
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
            onAddAppointment={handleAddAppointment}
            onAppointmentClick={handleAppointmentClick}
          />
        </div>
      </ProtectedRoute>
    </Layout>
  )
}
