'use client'

import { useState, useRef, useEffect } from 'react'
import { UserFull } from '@/types/auth'
import './userFormModal.style.css'

const ROLE_OPTIONS = [
  { label: 'Administrador', value: 'ADMIN' },
  { label: 'Enfermeiro', value: 'NURSE' },
  { label: 'Paciente', value: 'PATIENT' },
]

function RolesCombobox({
  value,
  onChange,
}: {
  value: string[]
  onChange: (roles: string[]) => void
}) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const toggleRole = (role: string) => {
    if (value.includes(role)) {
      onChange(value.filter(r => r !== role))
    } else {
      onChange([...value, role])
    }
  }

  const selectedLabels = value
    .map(v => ROLE_OPTIONS.find(o => o.value === v)?.label ?? v)
    .join(', ')

  return (
    <div className="combobox-container" ref={containerRef}>
      <button
        type="button"
        className={`combobox-trigger${open ? ' combobox-trigger--open' : ''}`}
        onClick={() => setOpen(prev => !prev)}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <span
          className={`combobox-value${value.length === 0 ? ' combobox-placeholder' : ''}`}
        >
          {value.length === 0 ? 'Selecione as funções...' : selectedLabels}
        </span>
        <svg
          className={`combobox-chevron${open ? ' combobox-chevron--open' : ''}`}
          viewBox="0 0 20 20"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <path
            d="M5 7.5L10 12.5L15 7.5"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      {open && (
        <ul
          className="combobox-dropdown"
          role="listbox"
          aria-multiselectable="true"
        >
          {ROLE_OPTIONS.map(option => {
            const selected = value.includes(option.value)
            return (
              <li
                key={option.value}
                role="option"
                aria-selected={selected}
                className={`combobox-option${selected ? ' combobox-option--selected' : ''}`}
                onClick={() => toggleRole(option.value)}
              >
                <span className="combobox-checkbox" aria-hidden="true">
                  {selected && (
                    <svg
                      viewBox="0 0 12 12"
                      fill="none"
                      xmlns="http://www.w3.org/2000/svg"
                    >
                      <path
                        d="M2 6L5 9L10 3"
                        stroke="currentColor"
                        strokeWidth="1.8"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  )}
                </span>
                {option.label}
                <span className="combobox-badge">{option.value}</span>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}

function UserFormModal({
  user,
  onClose,
  onSave,
}: {
  user: UserFull | null
  onClose: () => void
  onSave: (data: Partial<UserFull> & { password?: string }) => void
}) {
  const [formData, setFormData] = useState({
    fullName: user?.fullName || '',
    username: user?.username || '',
    email: user?.email || '',
    password: '',
    isActive: user?.isActive !== undefined ? user.isActive : true,
    roles: user?.roles || ['USER'],
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const dataToSave =
      user && !formData.password
        ? { ...formData, password: undefined }
        : formData
    onSave(dataToSave)
  }

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <h2 className="modal-title">
          {user ? 'Editar Usuário' : 'Novo Usuário'}
        </h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Nome Completo</label>
            <input
              type="text"
              value={formData.fullName}
              onChange={e =>
                setFormData({ ...formData, fullName: e.target.value })
              }
              className="form-input"
              required
            />
          </div>
          <div className="form-group">
            <label className="form-label">Username</label>
            <input
              type="text"
              value={formData.username}
              onChange={e =>
                setFormData({ ...formData, username: e.target.value })
              }
              className="form-input"
              required
            />
          </div>
          <div className="form-group">
            <label className="form-label">Email</label>
            <input
              type="email"
              value={formData.email}
              onChange={e =>
                setFormData({ ...formData, email: e.target.value })
              }
              className="form-input"
              required
            />
          </div>
          <div className="form-group">
            <label className="form-label">
              {user ? 'Nova Senha (deixe em branco para manter)' : 'Senha'}
            </label>
            <input
              type="password"
              value={formData.password}
              onChange={e =>
                setFormData({ ...formData, password: e.target.value })
              }
              className="form-input"
              required={!user}
              minLength={!user ? 8 : undefined}
            />
          </div>
          <div className="form-group">
            <label className="form-label">Status</label>
            <select
              value={formData.isActive ? 'true' : 'false'}
              onChange={e =>
                setFormData({
                  ...formData,
                  isActive: e.target.value === 'true',
                })
              }
              className="form-select"
            >
              <option value="true">Ativo</option>
              <option value="false">Inativo</option>
            </select>
          </div>
          <div className="form-group">
            <label className="form-label">Funções</label>
            <RolesCombobox
              value={formData.roles}
              onChange={roles => setFormData({ ...formData, roles })}
            />
          </div>
          <div className="form-actions">
            <button type="button" onClick={onClose} className="btn-secondary">
              Cancelar
            </button>
            <button type="submit" className="btn-primary">
              Salvar
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default UserFormModal
