'use client'

import { useState } from 'react'
import { AgendamentoRequisicao } from '@/tipos/appointment'
import './styles.css'

interface AppointmentFormModalProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (dados: AgendamentoRequisicao) => void
  profissionalUuid?: string
}

const initialForm = (profissionalUuid?: string): Partial<AgendamentoRequisicao> => ({
  pacienteUuid: '',
  profissionalUuid: profissionalUuid ?? '',
  inicioEm: '',
  fimEm: '',
  tipo: 'AVALIACAO',
  observacoes: '',
  avaliacaoId: undefined,
})

export default function AppointmentFormModal({
  isOpen,
  onClose,
  onSubmit,
  profissionalUuid,
}: AppointmentFormModalProps) {
  const [formData, setFormData] = useState<Partial<AgendamentoRequisicao>>(
    initialForm(profissionalUuid),
  )
  const [errors, setErrors] = useState<Record<string, string>>({})

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: name === 'avaliacaoId' ? (value ? Number(value) : undefined) : value,
    }))
    if (errors[name]) setErrors(prev => ({ ...prev, [name]: '' }))
  }

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!formData.pacienteUuid?.trim())
      newErrors.pacienteUuid = 'UUID do paciente é obrigatório'
    if (!formData.profissionalUuid?.trim())
      newErrors.profissionalUuid = 'UUID do profissional é obrigatório'
    if (!formData.inicioEm)
      newErrors.inicioEm = 'Data/hora de início é obrigatória'
    if (!formData.fimEm)
      newErrors.fimEm = 'Data/hora de término é obrigatória'
    if (formData.tipo === 'TRATAMENTO' && !formData.avaliacaoId)
      newErrors.avaliacaoId = 'ID da avaliação é obrigatório para tratamentos'

    if (formData.inicioEm && formData.fimEm) {
      if (new Date(formData.fimEm) <= new Date(formData.inicioEm))
        newErrors.fimEm = 'Término deve ser após o início'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!validateForm()) return

    onSubmit(formData as AgendamentoRequisicao)
    setFormData(initialForm(profissionalUuid))
    setErrors({})
  }

  const handleCancel = () => {
    setFormData(initialForm(profissionalUuid))
    setErrors({})
    onClose()
  }

  if (!isOpen) return null

  return (
    <div className="modal-overlay" onClick={handleCancel}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="text-xl font-bold text-[#2b6cb0]">Novo Agendamento</h2>
          <button onClick={handleCancel} className="text-gray-400 hover:text-gray-600">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          <div className="form-grid">

            <div className="form-group">
              <label htmlFor="pacienteUuid" className="form-label">
                UUID do Paciente *
              </label>
              <input
                type="text"
                id="pacienteUuid"
                name="pacienteUuid"
                value={formData.pacienteUuid || ''}
                onChange={handleChange}
                className={`form-input ${errors.pacienteUuid ? 'border-red-500' : ''}`}
                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              />
              {errors.pacienteUuid && (
                <span className="error-message">{errors.pacienteUuid}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="profissionalUuid" className="form-label">
                UUID do Profissional *
              </label>
              <input
                type="text"
                id="profissionalUuid"
                name="profissionalUuid"
                value={formData.profissionalUuid || ''}
                onChange={handleChange}
                readOnly={!!profissionalUuid}
                className={`form-input ${profissionalUuid ? 'bg-gray-100 cursor-not-allowed' : ''} ${errors.profissionalUuid ? 'border-red-500' : ''}`}
                placeholder="Preenchido automaticamente"
              />
              {errors.profissionalUuid && (
                <span className="error-message">{errors.profissionalUuid}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="inicioEm" className="form-label">
                Data/Hora Início *
              </label>
              <input
                type="datetime-local"
                id="inicioEm"
                name="inicioEm"
                value={formData.inicioEm || ''}
                onChange={handleChange}
                className={`form-input ${errors.inicioEm ? 'border-red-500' : ''}`}
              />
              {errors.inicioEm && (
                <span className="error-message">{errors.inicioEm}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="fimEm" className="form-label">
                Data/Hora Término *
              </label>
              <input
                type="datetime-local"
                id="fimEm"
                name="fimEm"
                value={formData.fimEm || ''}
                onChange={handleChange}
                className={`form-input ${errors.fimEm ? 'border-red-500' : ''}`}
              />
              {errors.fimEm && (
                <span className="error-message">{errors.fimEm}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="tipo" className="form-label">
                Tipo *
              </label>
              <select
                id="tipo"
                name="tipo"
                value={formData.tipo || 'AVALIACAO'}
                onChange={handleChange}
                className="form-input"
              >
                <option value="AVALIACAO">Avaliação</option>
                <option value="TRATAMENTO">Tratamento</option>
              </select>
            </div>

            {formData.tipo === 'TRATAMENTO' && (
              <div className="form-group">
                <label htmlFor="avaliacaoId" className="form-label">
                  ID da Avaliação de Origem *
                </label>
                <input
                  type="number"
                  id="avaliacaoId"
                  name="avaliacaoId"
                  value={formData.avaliacaoId ?? ''}
                  onChange={handleChange}
                  className={`form-input ${errors.avaliacaoId ? 'border-red-500' : ''}`}
                  placeholder="ID do agendamento de avaliação"
                  min={1}
                />
                {errors.avaliacaoId && (
                  <span className="error-message">{errors.avaliacaoId}</span>
                )}
              </div>
            )}

            <div className="form-group col-span-2">
              <label htmlFor="observacoes" className="form-label">
                Observações
              </label>
              <textarea
                id="observacoes"
                name="observacoes"
                value={formData.observacoes || ''}
                onChange={handleChange}
                className="form-input"
                placeholder="Observações opcionais sobre o agendamento"
                rows={3}
              />
            </div>

          </div>

          <div className="modal-footer">
            <button type="button" onClick={handleCancel} className="btn-secondary">
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
