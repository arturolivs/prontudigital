import React, { useMemo, useState } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTriangleExclamation } from '@fortawesome/free-solid-svg-icons'
import {
  Appointment,
  NewAppointmentData,
  GroupedAppointments,
} from '../../types/appointment'
import AddAppointmentButton from '../AddAppointmentButton'
import AppointmentFormModal from '../AppointmentFormModal'
import DateHeader from '../DateHeader'
import AppointmentCard from '../AppointmentCard'
import './appointmentList.css'

interface AppointmentListProps {
  appointments?: Appointment[]
  loading?: boolean
  error?: string | null
  onAddAppointment?: (data: NewAppointmentData) => void
  onAppointmentClick?: (appointment: Appointment) => void
}

const AppointmentList: React.FC<AppointmentListProps> = ({
  appointments = [],
  loading = false,
  error = null,
  onAddAppointment,
  onAppointmentClick,
}) => {
  const [isModalOpen, setIsModalOpen] = useState(false)

  const groupedAppointments = useMemo(() => {
    const grouped: GroupedAppointments = {}

    appointments.forEach(appointment => {
      const dateKey = new Date(appointment.startDateTime)
        .toISOString()
        .split('T')[0]

      if (!grouped[dateKey]) {
        grouped[dateKey] = []
      }

      grouped[dateKey].push(appointment)
    })

    const sortedDates = Object.keys(grouped).sort()
    const sortedGrouped: GroupedAppointments = {}

    sortedDates.forEach(date => {
      sortedGrouped[date] = grouped[date].sort(
        (a, b) =>
          new Date(a.startDateTime).getTime() -
          new Date(b.startDateTime).getTime(),
      )
    })

    return sortedGrouped
  }, [appointments])

  const handleAddAppointment = (appointmentData: NewAppointmentData) => {
    console.log('Novo agendamento:', appointmentData)

    if (onAddAppointment) {
      onAddAppointment(appointmentData)
    }
  }

  const handleAppointmentClick = (appointment: Appointment) => {
    if (onAppointmentClick) {
      onAppointmentClick(appointment)
    }
  }

  const hasAppointments = Object.keys(groupedAppointments).length > 0
  const totalAppointments = appointments.length

  if (loading) {
    return (
      <div className="appointment-list-container">
        <div className="loading-state">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          <p>Carregando agendamentos...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="appointment-list-container">
        <div className="error-message">
          <FontAwesomeIcon icon={faTriangleExclamation} className="h-5 w-5" />
          <div className="error-content">
            <h3>Erro ao carregar agendamentos</h3>
            <p>{error}</p>
            <button
              className="retry-btn"
              onClick={() => window.location.reload()}
            >
              Tentar novamente
            </button>
          </div>
        </div>
      </div>
    )
  }

  if (!hasAppointments) {
    return (
      <div className="appointment-list-container">
        <div className="no-appointments">
          <i className="far fa-calendar-times"></i>
          <h3>Nenhum agendamento encontrado</h3>
          <p>Não há agendamentos para a data selecionada.</p>
          <AddAppointmentButton
            onClick={() => setIsModalOpen(true)}
            variant="primary"
          />
        </div>
      </div>
    )
  }

  return (
    <div className="appointment-list-container">
      <AppointmentFormModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleAddAppointment}
      />

      <div className="header">
        <div className="header-content">
          <h1>Agendas</h1>
          {hasAppointments && (
            <div className="total-appointments">
              Total: {totalAppointments} agendamento
              {totalAppointments !== 1 ? 's' : ''}
            </div>
          )}
        </div>

        <div className="header-actions">
          <AddAppointmentButton
            onClick={() => setIsModalOpen(true)}
            variant="primary"
          />
        </div>
      </div>

      <div className="dates-container">
        {Object.entries(groupedAppointments).map(
          ([dateKey, dateAppointments]) => (
            <div className="day-container" key={dateKey}>
              <DateHeader dateKey={dateKey} count={dateAppointments.length} />

              <div className="appointments-list">
                {dateAppointments.map(appointment => (
                  <AppointmentCard
                    key={appointment.id}
                    appointment={appointment}
                    onClick={handleAppointmentClick}
                  />
                ))}
              </div>
            </div>
          ),
        )}
      </div>
    </div>
  )
}

export default AppointmentList
