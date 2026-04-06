'use client'

import { useEffect, useState } from 'react'
import { useAuth } from '../../../contexts/AuthContext'
import { usersAPI } from '@/lib/users.service'
import { RegisterRequest, UserFull } from '@/types/auth'
import './usuariosPage.css' // Importando o CSS customizado

export default function UsuariosPage() {
  const [users, setUsers] = useState<UserFull[]>([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<UserFull | null>(null)
  const { user } = useAuth()

  const fetchUsers = async () => {
    try {
      setLoading(true)
      const data = await usersAPI.listUsers()
      setUsers(data)
    } catch (error) {
      console.error('Erro ao carregar usuários', error)
      alert('Erro ao carregar lista de usuários')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchUsers()
  }, [])

  const handleDelete = async (id: number) => {
    if (!confirm('Tem certeza que deseja excluir este usuário?')) return

    try {
      await usersAPI.deleteUser(id)
      setUsers(users.filter(user => user.id !== id))
      alert('Usuário excluído com sucesso!')
    } catch (error: any) {
      console.error('Erro ao excluir usuário', error)
      alert(error.message || 'Erro ao excluir usuário')
    }
  }

  const handleToggleStatus = async (id: number, currentStatus: boolean) => {
    try {
      const updatedUser = await usersAPI.toggleUserStatus(id, !currentStatus)
      setUsers(users.map(user => (user.id === id ? updatedUser : user)))
    } catch (error: any) {
      console.error('Erro ao alterar status', error)
      alert(error.message || 'Erro ao alterar status do usuário')
    }
  }

  const openEditModal = (user: UserFull) => {
    setEditingUser(user)
    setModalOpen(true)
  }

  const openCreateModal = () => {
    setEditingUser(null)
    setModalOpen(true)
  }

  const closeModal = () => {
    setModalOpen(false)
    setEditingUser(null)
  }

  const handleSave = async (
    userData: Partial<UserFull> & { password?: string },
  ) => {
    try {
      if (editingUser) {
        const updatedUser = await usersAPI.updateUser(editingUser.id, {
          fullName: userData.fullName,
          username: userData.username,
          email: userData.email,
          isActive: userData.isActive,
          roles: userData.roles,
        })

        setUsers(
          users.map(user => (user.id === editingUser.id ? updatedUser : user)),
        )
        alert('Usuário atualizado com sucesso!')
      } else {
        const newUserData: RegisterRequest = {
          fullName: userData.fullName || '',
          username: userData.username || '',
          email: userData.email || '',
          password: userData.password || '',
          roles: userData.roles,
        }

        const newUser = await usersAPI.createUser(newUserData)
        setUsers([...users, newUser])
        alert('Usuário criado com sucesso!')
      }

      closeModal()
    } catch (error: any) {
      console.error('Erro ao salvar usuário', error)
      alert(error.message || 'Erro ao salvar usuário')
    }
  }

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
      </div>
    )
  }

  return (
    <div className="users-container">
      <div className="users-header">
        <h1 className="users-title">Usuários</h1>
        <button onClick={openCreateModal} className="btn-primary">
          Novo Usuário
        </button>
      </div>

      <div className="users-table-container">
        <table className="users-table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Username</th>
              <th>Email</th>
              <th>Status</th>
              <th>Roles</th>
              <th className="action-buttons">Ações</th>
            </tr>
          </thead>
          <tbody>
            {users.map(user => (
              <tr key={user.id}>
                <td>{user.fullName}</td>
                <td>{user.username}</td>
                <td>{user.email}</td>
                <td>
                  <button
                    onClick={() => handleToggleStatus(user.id, user.isActive)}
                    className={`status-badge ${
                      user.isActive
                        ? 'status-badge-active'
                        : 'status-badge-inactive'
                    }`}
                  >
                    {user.isActive ? 'Ativo' : 'Inativo'}
                  </button>
                </td>
                <td className="roles-text">{user.roles.join(', ')}</td>
                <td className="action-buttons">
                  <button
                    onClick={() => openEditModal(user)}
                    className="btn-edit"
                  >
                    Editar
                  </button>
                  <button
                    onClick={() => handleDelete(user.id)}
                    className="btn-delete"
                  >
                    Excluir
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalOpen && (
        <UserModal
          user={editingUser}
          onClose={closeModal}
          onSave={handleSave}
        />
      )}
    </div>
  )
}

// Componente do modal
function UserModal({
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
            <label className="form-label">Roles (separadas por vírgula)</label>
            <input
              type="text"
              value={formData.roles.join(', ')}
              onChange={e =>
                setFormData({
                  ...formData,
                  roles: e.target.value.split(',').map(r => r.trim()),
                })
              }
              className="form-input"
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
