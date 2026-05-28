'use client'

import { useState, useRef, useEffect } from 'react'
import { Usuario } from '@/tipos/autenticacao'
import './userFormModal.style.css'

const OPCOES_PERFIL = [
  { label: 'Administrador', value: 'ROLE_ADMIN' },
  { label: 'Enfermeiro', value: 'ROLE_PROFISSIONAL' },
  { label: 'Usuário', value: 'USUARIO' },
]

function PerfisCombobox({
  value,
  onChange,
}: {
  value: string[]
  onChange: (perfis: string[]) => void
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

  const togglePerfil = (perfil: string) => {
    if (value.includes(perfil)) {
      onChange(value.filter(p => p !== perfil))
    } else {
      onChange([...value, perfil])
    }
  }

  const selectedLabels = value
    .map(v => OPCOES_PERFIL.find(o => o.value === v)?.label ?? v)
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
          {value.length === 0 ? 'Selecione os perfis...' : selectedLabels}
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
          {OPCOES_PERFIL.map(opcao => {
            const selected = value.includes(opcao.value)
            return (
              <li
                key={opcao.value}
                role="option"
                aria-selected={selected}
                className={`combobox-option${selected ? ' combobox-option--selected' : ''}`}
                onClick={() => togglePerfil(opcao.value)}
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
                {opcao.label}
                <span className="combobox-badge">{opcao.value}</span>
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
  user: Usuario | null
  onClose: () => void
  onSave: (data: Partial<Usuario> & { senha?: string }) => void
}) {
  const [formData, setFormData] = useState({
    nomeCompleto: user?.nomeCompleto || '',
    username: user?.username || '',
    email: user?.email || '',
    telefone: user?.telefone || '',
    senha: '',
    ativo: user?.ativo !== undefined ? user.ativo : true,
    perfis: user?.perfis || ['USUARIO'],
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const dataToSave =
      user && !formData.senha
        ? { ...formData, senha: undefined }
        : formData
    onSave(dataToSave)
  }

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <h2 className="modal-title" style={{ color: '#2b6cb0' }}>
          {user ? 'Editar Usuário' : 'Novo Usuário'}
        </h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Nome Completo</label>
            <input
              type="text"
              value={formData.nomeCompleto}
              onChange={e =>
                setFormData({ ...formData, nomeCompleto: e.target.value })
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
            <label className="form-label">Telefone</label>
            <input
              type="tel"
              value={formData.telefone}
              onChange={e =>
                setFormData({ ...formData, telefone: e.target.value })
              }
              className="form-input"
              placeholder="(00) 00000-0000"
            />
          </div>
          <div className="form-group">
            <label className="form-label">
              {user ? 'Nova Senha (deixe em branco para manter)' : 'Senha'}
            </label>
            <input
              type="password"
              value={formData.senha}
              onChange={e =>
                setFormData({ ...formData, senha: e.target.value })
              }
              className="form-input"
              required={!user}
              minLength={!user ? 8 : undefined}
            />
          </div>
          <div className="form-group">
            <label className="form-label">Status</label>
            <select
              value={formData.ativo ? 'true' : 'false'}
              onChange={e =>
                setFormData({
                  ...formData,
                  ativo: e.target.value === 'true',
                })
              }
              className="form-select"
            >
              <option value="true">Ativo</option>
              <option value="false">Inativo</option>
            </select>
          </div>
          <div className="form-group">
            <label className="form-label">Perfis</label>
            <PerfisCombobox
              value={formData.perfis}
              onChange={perfis => setFormData({ ...formData, perfis })}
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
