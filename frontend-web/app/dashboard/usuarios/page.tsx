'use client'

import { useEffect, useState } from 'react'
import { useAuth } from '../../../contexts/AuthContext'
import { usersAPI } from '@/lib/usuario.service'
import { RegisterRequest, UserFull } from '@/tipos/autenticacao'
import './usuariosPage.css' // Importando o CSS customizado
import { useToast } from '@/contexts/ToastContext'
import UserFormModal from '@/components/UserFormModal'

export default function UsuariosPage() {
  const [users, setUsers] = useState<UserFull[]>([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<UserFull | null>(null)
  const { user } = useAuth()
  const { showToast } = useToast()

  const fetchUsers = async () => {
    try {
      setLoading(true)
      const data = await usersAPI.listUsers()
      setUsers(data)
    } catch (error) {
      showToast('Erro ao carregar lista de usuários', 'error', 6000)
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
      showToast('Usuário excluído com sucesso!', 'success', 6000)
    } catch (error: any) {
      console.error('Erro ao excluir usuário', error)
      showToast(error.message || 'Erro ao excluir usuário', 'error', 6000)
    }
  }

  const handleToggleStatus = async (id: number, currentStatus: boolean) => {
    try {
      const updatedUser = await usersAPI.toggleUserStatus(id, !currentStatus)
      setUsers(users.map(user => (user.id === id ? updatedUser : user)))
    } catch (error: any) {
      console.error('Erro ao alterar status', error)
      showToast(
        error.message || 'Erro ao alterar status do usuário',
        'error',
        6000,
      )
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
        showToast('Usuário atualizado com sucesso!', 'success', 6000)
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
        showToast('Usuário criado com sucesso!', 'success', 6000)
      }

      closeModal()
    } catch (error: any) {
      console.error('Erro ao salvar usuário', error)
      showToast(error.message || 'Erro ao salvar usuário', 'error', 6000)
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
        <UserFormModal
          user={editingUser}
          onClose={closeModal}
          onSave={handleSave}
        />
      )}
    </div>
  )
}
