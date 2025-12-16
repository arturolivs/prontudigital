import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus } from '@fortawesome/free-solid-svg-icons'

interface AddAppointmentButtonProps {
  onClick: () => void
  disabled?: boolean
  variant?: 'primary' | 'secondary' | 'outline'
  size?: 'sm' | 'md' | 'lg'
}

const AddAppointmentButton: React.FC<AddAppointmentButtonProps> = ({
  onClick,
  disabled = false,
  variant = 'primary',
  size = 'md',
}) => {
  const getButtonClasses = () => {
    const baseClasses = 'add-appointment-btn'
    const variantClasses = {
      primary: 'btn-primary',
      secondary: 'btn-secondary',
      outline: 'btn-outline',
    }
    const sizeClasses = {
      sm: 'btn-sm',
      md: 'btn-md',
      lg: 'btn-lg',
    }

    return `${baseClasses} ${variantClasses[variant]} ${sizeClasses[size]}`
  }

  return (
    <button
      className={getButtonClasses()}
      onClick={onClick}
      disabled={disabled}
      aria-label="Adicionar novo agendamento"
    >
      <FontAwesomeIcon icon={faPlus} className="btn-icon" />
      <span>Novo Agendamento</span>
    </button>
  )
}

export default AddAppointmentButton
