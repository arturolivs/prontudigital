import React, { useState } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  faTimes,
  faUser,
  faCalendarAlt,
  faBed,
  faUserInjured,
  faClock,
  faPlus,
} from '@fortawesome/free-solid-svg-icons'
import { NewAppointmentData } from '../../types/appointment'

interface AppointmentFormModalProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (data: NewAppointmentData) => void
}

const AppointmentFormModal: React.FC<AppointmentFormModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
}) => {
  const [formData, setFormData] = useState<NewAppointmentData>({
    patientName: '',
    age: 0,
    bedridden: false,
    dateTime: '',
    type: 'AVALIACAO',
    tratamentType: 'PODEATRIA',
    duration: 30,
  })

  const [errors, setErrors] = useState<
    Partial<Record<keyof NewAppointmentData, string>>
  >({})

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    const newErrors: Partial<Record<keyof NewAppointmentData, string>> = {}

    if (!formData.patientName.trim()) {
      newErrors.patientName = 'Nome completo é obrigatório'
    }

    if (!formData.age || formData.age <= 0 || formData.age > 150) {
      newErrors.age = 'Idade deve ser entre 1 e 150 anos'
    }

    if (!formData.dateTime) {
      newErrors.dateTime = 'Data e hora são obrigatórias'
    } else if (new Date(formData.dateTime) < new Date()) {
      newErrors.dateTime = 'Data e hora não podem ser no passado'
    }

    if (formData.duration <= 0 || formData.duration > 480) {
      newErrors.duration = 'Duração deve ser entre 1 e 480 minutos'
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors)
      return
    }

    const formattedData = {
      ...formData,
      dateTime: new Date(formData.dateTime).toISOString(),
    }

    onSubmit(formattedData)
    resetForm()
    onClose()
  }

  const resetForm = () => {
    setFormData({
      patientName: '',
      age: 0,
      bedridden: false,
      dateTime: '',
      type: 'AVALIACAO',
      tratamentType: 'PODEATRIA',
      duration: 30,
    })
    setErrors({})
  }

  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value, type } = e.target
    const checked = (e.target as HTMLInputElement).checked

    setFormData(prev => ({
      ...prev,
      [name]:
        type === 'checkbox'
          ? checked
          : type === 'number'
            ? Number(value)
            : value,
    }))

    if (errors[name as keyof NewAppointmentData]) {
      setErrors(prev => ({ ...prev, [name]: undefined }))
    }
  }

  const getEndTime = () => {
    if (!formData.dateTime || !formData.duration) return ''
    const startTime = new Date(formData.dateTime)
    const endTime = new Date(startTime.getTime() + formData.duration * 60000)
    return endTime.toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  if (!isOpen) return null

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Novo Agendamento</h2>
          <button className="modal-close-btn" onClick={onClose}>
            <FontAwesomeIcon icon={faTimes} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="appointment-form">
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="patientName">
                <FontAwesomeIcon icon={faUser} className="label-icon" />
                Nome Completo *
              </label>
              <input
                type="text"
                id="patientName"
                name="patientName"
                value={formData.patientName}
                onChange={handleInputChange}
                placeholder="Digite o nome completo do paciente"
                className={errors.patientName ? 'input-error' : ''}
              />
              {errors.patientName && (
                <span className="error-message">{errors.patientName}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="age">
                <FontAwesomeIcon icon={faUserInjured} className="label-icon" />
                Idade *
              </label>
              <input
                type="number"
                id="age"
                name="age"
                value={formData.age || ''}
                onChange={handleInputChange}
                placeholder="Idade do paciente"
                min="1"
                max="150"
                className={errors.age ? 'input-error' : ''}
              />
              {errors.age && (
                <span className="error-message">{errors.age}</span>
              )}
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="type">Tipo de Consulta *</label>
              <select
                id="type"
                name="type"
                value={formData.type}
                onChange={handleInputChange}
                className="form-select"
              >
                <option value="AVALIACAO">Avaliação</option>
                <option value="TRATAMENTO">Tratamento</option>
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="tratamentType">Especialidade *</label>
              <select
                id="tratamentType"
                name="tratamentType"
                value={formData.tratamentType}
                onChange={handleInputChange}
                className="form-select"
              >
                <option value="PODEATRIA">Podeatria</option>
                <option value="TRATAMENTO_FERIDAS">
                  Tratamento de Feridas
                </option>
              </select>
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="dateTime">
                <FontAwesomeIcon icon={faCalendarAlt} className="label-icon" />
                Data e Hora de Início *
              </label>
              <input
                type="datetime-local"
                id="dateTime"
                name="dateTime"
                value={formData.dateTime}
                onChange={handleInputChange}
                className={errors.dateTime ? 'input-error' : ''}
                min={new Date().toISOString().slice(0, 16)}
              />
              {errors.dateTime && (
                <span className="error-message">{errors.dateTime}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="duration">
                <FontAwesomeIcon icon={faClock} className="label-icon" />
                Duração (minutos) *
              </label>
              <select
                id="duration"
                name="duration"
                value={formData.duration}
                onChange={handleInputChange}
                className={
                  errors.duration ? 'input-error form-select' : 'form-select'
                }
              >
                <option value="15">15 minutos</option>
                <option value="30">30 minutos</option>
                <option value="45">45 minutos</option>
                <option value="60">1 hora</option>
                <option value="90">1 hora e 30 minutos</option>
                <option value="120">2 horas</option>
              </select>
              {formData.dateTime && formData.duration && (
                <div className="time-preview">
                  <span>Termina às: {getEndTime()}</span>
                </div>
              )}
              {errors.duration && (
                <span className="error-message">{errors.duration}</span>
              )}
            </div>
          </div>

          <div className="form-group checkbox-group">
            <label htmlFor="bedridden" className="checkbox-label">
              <input
                type="checkbox"
                id="bedridden"
                name="bedridden"
                checked={formData.bedridden}
                onChange={handleInputChange}
              />
              <span className="checkbox-custom">
                <FontAwesomeIcon icon={faBed} />
              </span>
              <span className="checkbox-text">Paciente acamado</span>
            </label>
          </div>

          <div className="form-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="btn-primary">
              <FontAwesomeIcon icon={faPlus} className="btn-icon" />
              Agendar
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default AppointmentFormModal
