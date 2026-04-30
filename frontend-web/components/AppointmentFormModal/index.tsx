'use client'

import { useState } from 'react'
import { Appointment } from '@/types/appointment'
import './styles.css'

interface AppointmentFormModalProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (appointmentData: Partial<Appointment>) => void
}

export default function AppointmentFormModal({
  isOpen,
  onClose,
  onSubmit,
}: AppointmentFormModalProps) {
  const [formData, setFormData] = useState<Partial<Appointment>>({
    startDateTime: '',
    endDateTime: '',
    patientPhone: '',
    patientName: '',
    professionalName: '',
    bedridden: false,
    type: 'AVALIACAO',
    tratamentType: 'PODEATRIA',
  })

  const [errors, setErrors] = useState<Record<string, string>>({})

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value, type } = e.target
    if (type === 'checkbox') {
      const checked = (e.target as HTMLInputElement).checked
      setFormData(prev => ({ ...prev, [name]: checked }))
    } else {
      setFormData(prev => ({ ...prev, [name]: value }))
    }
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }))
    }
  }

  const validateForm = () => {
    const newErrors: Record<string, string> = {}
    if (!formData.startDateTime)
      newErrors.startDateTime = 'Data/hora de início é obrigatória'
    if (!formData.endDateTime)
      newErrors.endDateTime = 'Data/hora de término é obrigatória'
    if (!formData.patientName)
      newErrors.patientName = 'Nome do paciente é obrigatório'
    if (!formData.patientPhone)
      newErrors.patientPhone = 'Telefone do paciente é obrigatório'

    if (formData.startDateTime && formData.endDateTime) {
      const start = new Date(formData.startDateTime)
      const end = new Date(formData.endDateTime)
      if (end <= start) {
        newErrors.endDateTime =
          'Data/hora de término deve ser após a data/hora de início'
      }
    }
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (validateForm()) {
      onSubmit(formData)
      setFormData({
        startDateTime: '',
        endDateTime: '',
        patientPhone: '',
        patientName: '',
        professionalName: '',
        bedridden: false,
        type: 'AVALIACAO',
        tratamentType: 'PODEATRIA',
      })
    }
  }

  const handleCancel = () => {
    setFormData({
      startDateTime: '',
      endDateTime: '',
      patientPhone: '',
      patientName: '',
      professionalName: '',
      bedridden: false,
      type: 'AVALIACAO',
      tratamentType: 'PODEATRIA',
    })
    setErrors({})
    onClose()
  }

  if (!isOpen) return null

  return (
    <div className="modal-overlay" onClick={handleCancel}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="text-xl font-bold text-[#2b6cb0]">Novo Agendamento</h2>
          <button
            onClick={handleCancel}
            className="text-gray-400 hover:text-gray-600"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-6 w-6"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="patientName" className="form-label">
                Nome do Paciente *
              </label>
              <input
                type="text"
                id="patientName"
                name="patientName"
                value={formData.patientName || ''}
                onChange={handleChange}
                className={`form-input ${errors.patientName ? 'border-red-500' : ''}`}
                placeholder="Digite o nome do paciente"
              />
              {errors.patientName && (
                <span className="error-message">{errors.patientName}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="patientPhone" className="form-label">
                Telefone *
              </label>
              <input
                type="tel"
                id="patientPhone"
                name="patientPhone"
                value={formData.patientPhone || ''}
                onChange={handleChange}
                className={`form-input ${errors.patientPhone ? 'border-red-500' : ''}`}
                placeholder="(11) 99999-9999"
              />
              {errors.patientPhone && (
                <span className="error-message">{errors.patientPhone}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="startDateTime" className="form-label">
                Data/Hora Início *
              </label>
              <input
                type="datetime-local"
                id="startDateTime"
                name="startDateTime"
                value={formData.startDateTime || ''}
                onChange={handleChange}
                className={`form-input ${errors.startDateTime ? 'border-red-500' : ''}`}
              />
              {errors.startDateTime && (
                <span className="error-message">{errors.startDateTime}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="endDateTime" className="form-label">
                Data/Hora Término *
              </label>
              <input
                type="datetime-local"
                id="endDateTime"
                name="endDateTime"
                value={formData.endDateTime || ''}
                onChange={handleChange}
                className={`form-input ${errors.endDateTime ? 'border-red-500' : ''}`}
              />
              {errors.endDateTime && (
                <span className="error-message">{errors.endDateTime}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="type" className="form-label">
                Tipo de Agendamento *
              </label>
              <select
                id="type"
                name="type"
                value={formData.type || 'AVALIACAO'}
                onChange={handleChange}
                className="form-input"
              >
                <option value="AVALIACAO">Avaliação</option>
                <option value="TRATAMENTO">Tratamento</option>
              </select>
            </div>

            {formData.type === 'TRATAMENTO' && (
              <div className="form-group">
                <label htmlFor="tratamentType" className="form-label">
                  Tipo de Tratamento *
                </label>
                <select
                  id="tratamentType"
                  name="tratamentType"
                  value={formData.tratamentType || 'PODEATRIA'}
                  onChange={handleChange}
                  className="form-input"
                >
                  <option value="PODEATRIA">Podiatria</option>
                  <option value="TRATAMENTO_FERIDAS">
                    Tratamento de Feridas
                  </option>
                </select>
              </div>
            )}

            <div className="form-group">
              <label htmlFor="professionalName" className="form-label">
                Nome do Profissional
              </label>
              <input
                type="text"
                id="professionalName"
                name="professionalName"
                value={formData.professionalName || ''}
                onChange={handleChange}
                className="form-input"
                placeholder="Digite o nome do profissional"
              />
            </div>

            <div className="form-group flex items-center">
              <input
                type="checkbox"
                id="bedridden"
                name="bedridden"
                checked={formData.bedridden || false}
                onChange={handleChange}
                className="h-4 w-4 rounded border-gray-300 text-[#2b6cb0] focus:ring-[#7991bc]"
              />
              <label htmlFor="bedridden" className="ml-2 text-gray-700">
                Paciente acamado
              </label>
            </div>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              onClick={handleCancel}
              className="btn-secondary"
            >
              Cancelar
            </button>
            <button type="submit" className="btn-primary">
              Criar Agendamento
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
