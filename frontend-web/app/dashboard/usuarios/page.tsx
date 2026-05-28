'use client'

import { useEffect, useState } from 'react'
import { useAuth } from '../../../contexts/AuthContext'
import { usuariosAPI } from '@/lib/usuario.service'
import { Usuario, RegistrarRequisicao } from '@/tipos/autenticacao'
import './usuariosPage.css'
import { useNotificacao } from '@/contexts/ToastContext'
import UserFormModal from '@/components/UserFormModal'

export default function UsuariosPage() {
  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [usuarioEditando, setUsuarioEditando] = useState<Usuario | null>(null)
  const { usuario } = useAuth()
  const { exibirNotificacao } = useNotificacao()

  const fetchUsuarios = async () => {
    try {
      setLoading(true)
      const data = await usuariosAPI.listarUsuarios()
      setUsuarios(data)
    } catch (error) {
      exibirNotificacao('Erro ao carregar lista de usuários', 'error', 6000)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchUsuarios()
  }, [])

  const handleDelete = async (id: number) => {
    if (!confirm('Tem certeza que deseja excluir este usuário?')) return

    try {
      await usuariosAPI.excluirUsuario(id)
      setUsuarios(usuarios.filter(u => u.id !== id))
      exibirNotificacao('Usuário excluído com sucesso!', 'success', 6000)
    } catch (error: any) {
      console.error('Erro ao excluir usuário', error)
      exibirNotificacao(
        error.message || 'Erro ao excluir usuário',
        'error',
        6000,
      )
    }
  }

  const handleToggleStatus = async (id: number, ativoAtual: boolean) => {
    const usuarioAtual = usuarios.find(u => u.id === id)
    if (!usuarioAtual) return

    try {
      const atualizado = await usuariosAPI.atualizarUsuario(id, {
        ...usuarioAtual,
        ativo: !ativoAtual,
      })
      setUsuarios(usuarios.map(u => (u.id === id ? atualizado : u)))
    } catch (error: any) {
      console.error('Erro ao alterar status', error)
      exibirNotificacao(
        error.message || 'Erro ao alterar status do usuário',
        'error',
        6000,
      )
    }
  }

  const openEditModal = (u: Usuario) => {
    setUsuarioEditando(u)
    setModalOpen(true)
  }

  const openCreateModal = () => {
    setUsuarioEditando(null)
    setModalOpen(true)
  }

  const closeModal = () => {
    setModalOpen(false)
    setUsuarioEditando(null)
  }

  const handleSave = async (dados: Partial<Usuario> & { senha?: string }) => {
    try {
      if (usuarioEditando) {
        const atualizado = await usuariosAPI.atualizarUsuario(
          usuarioEditando.id,
          {
            nomeCompleto: dados.nomeCompleto,
            username: dados.username,
            email: dados.email,
            telefone: dados.telefone,
            ativo: dados.ativo,
            perfis: dados.perfis,
          },
        )

        setUsuarios(
          usuarios.map(u => (u.id === usuarioEditando.id ? atualizado : u)),
        )
        exibirNotificacao('Usuário atualizado com sucesso!', 'success', 6000)
      } else {
        const novoUsuario: RegistrarRequisicao = {
          nomeCompleto: dados.nomeCompleto || '',
          username: dados.username || '',
          email: dados.email || '',
          senha: dados.senha || '',
          telefone: dados.telefone,
          perfis: dados.perfis,
        }

        const criado = await usuariosAPI.criarUsuario(novoUsuario)
        setUsuarios([...usuarios, criado])
        exibirNotificacao('Usuário criado com sucesso!', 'success', 6000)
      }

      closeModal()
    } catch (error: any) {
      console.error('Erro ao salvar usuário', error)
      exibirNotificacao(
        error.message || 'Erro ao salvar usuário',
        'error',
        6000,
      )
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
              <th>Telefone</th>
              <th>Status</th>
              <th>Perfis</th>
              <th className="action-buttons">Ações</th>
            </tr>
          </thead>
          <tbody>
            {usuarios.map(u => (
              <tr key={u.id}>
                <td>{u.nomeCompleto}</td>
                <td>{u.username}</td>
                <td>{u.email}</td>
                <td>{u.telefone || '—'}</td>
                <td>
                  <button
                    onClick={() => handleToggleStatus(u.id, u.ativo)}
                    className={`status-badge ${
                      u.ativo ? 'status-badge-active' : 'status-badge-inactive'
                    }`}
                  >
                    {u.ativo ? 'Ativo' : 'Inativo'}
                  </button>
                </td>
                <td className="roles-text">{u.perfis.join(', ')}</td>
                <td className="action-buttons">
                  <button onClick={() => openEditModal(u)} className="btn-edit">
                    Editar
                  </button>
                  <button
                    onClick={() => handleDelete(u.id)}
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
          user={usuarioEditando}
          onClose={closeModal}
          onSave={handleSave}
        />
      )}
    </div>
  )
}
