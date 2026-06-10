'use client'

import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { useAuth } from '../../../contexts/AuthContext'
import { usuariosAPI } from '@/lib/usuario.service'
import { Usuario, RegistrarRequisicao } from '@/tipos/autenticacao'
import './usuariosPage.css'
import { useNotificacao } from '@/contexts/ToastContext'
import FormularioUsuarioModal from '@/components/FormularioUsuarioModal'
import ModalConfirmacao from '@/components/ModalConfirmacao'

const ITENS_POR_PAGINA = 10

function gerarPaginas(total: number, atual: number): (number | string)[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const paginas: (number | string)[] = [1]
  if (atual > 3) paginas.push('...')
  for (let i = Math.max(2, atual - 1); i <= Math.min(total - 1, atual + 1); i++) paginas.push(i)
  if (atual < total - 2) paginas.push('...')
  paginas.push(total)
  return paginas
}

export default function UsuariosPage() {
  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [totalPaginas, setTotalPaginas] = useState(1)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [usuarioEditando, setUsuarioEditando] = useState<Usuario | null>(null)
  const [confirmacao, setConfirmacao] = useState<{ mensagem: string; acao: () => void } | null>(null)
  const [paginaAtual, setPaginaAtual] = useState(1)
  const { usuario } = useAuth()
  const { exibirNotificacao } = useNotificacao()

  const fetchUsuarios = async (pagina: number) => {
    try {
      setLoading(true)
      const data = await usuariosAPI.listarUsuarios(pagina - 1, ITENS_POR_PAGINA)
      setUsuarios(data.content)
      setTotalPaginas(Math.max(1, data.totalPages))
    } catch (error) {
      exibirNotificacao('Erro ao carregar lista de usuários', 'error', 6000)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchUsuarios(paginaAtual)
  }, [paginaAtual])

  const handleDelete = (id: number) => {
    setConfirmacao({
      mensagem: 'Tem certeza que deseja excluir este usuário?',
      acao: async () => {
        setConfirmacao(null)
        try {
          await usuariosAPI.excluirUsuario(id)
          const novaPagina = usuarios.length === 1 && paginaAtual > 1 ? paginaAtual - 1 : paginaAtual
          if (novaPagina === paginaAtual) {
            fetchUsuarios(paginaAtual)
          } else {
            setPaginaAtual(novaPagina)
          }
          exibirNotificacao('Usuário excluído com sucesso!', 'success', 6000)
        } catch (error: any) {
          console.error('Erro ao excluir usuário', error)
          exibirNotificacao(
            error.message || 'Erro ao excluir usuário',
            'error',
            6000,
          )
        }
      },
    })
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

        await usuariosAPI.criarUsuario(novoUsuario)
        await fetchUsuarios(paginaAtual)
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

  const paginaValida = Math.min(paginaAtual, totalPaginas)

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
      </div>
    )
  }

  return (
    <div className="users-page">
      <div className="users-header">
        <h1 className="users-title">Usuários</h1>
        <button onClick={openCreateModal} className="btn-primary">
          <Plus size={16} strokeWidth={2} />
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
              <th className="action-buttons-header">Ações</th>
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
                <td className="roles-cell">
                  <div className="perfis-badges">
                    {u.perfis.map(perfil => (
                      <span
                        key={perfil}
                        className={`perfil-badge perfil-${perfil.toLowerCase()}`}
                      >
                        {perfil === 'ROLE_ADMIN'
                          ? 'Admin'
                          : perfil === 'ROLE_PROFISSIONAL'
                            ? 'Profissional'
                            : perfil === 'ROLE_PACIENTE'
                              ? 'Paciente'
                              : perfil}
                      </span>
                    ))}
                  </div>
                </td>
                <td className="action-buttons">
                  <button
                    onClick={() => openEditModal(u)}
                    className="btn-edit"
                    title="Editar"
                  >
                    <Pencil size={14} />
                    <span>Editar</span>
                  </button>
                  <button
                    onClick={() => handleDelete(u.id)}
                    className="btn-delete"
                    title="Excluir"
                  >
                    <Trash2 size={14} />
                    <span>Excluir</span>
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {totalPaginas > 1 && (
        <div className="paginacao">
          <button
            className="paginacao-btn"
            onClick={() => setPaginaAtual(prev => Math.max(1, prev - 1))}
            disabled={paginaValida === 1}
          >
            ← Anterior
          </button>
          {gerarPaginas(totalPaginas, paginaValida).map((p, i) =>
            typeof p === 'string' ? (
              <span key={`ellipsis-${i}`} className="paginacao-ellipsis">…</span>
            ) : (
              <button
                key={`page-${p}`}
                className={`paginacao-btn paginacao-num${paginaValida === p ? ' paginacao-ativa' : ''}`}
                onClick={() => setPaginaAtual(p)}
              >
                {p}
              </button>
            )
          )}
          <button
            className="paginacao-btn"
            onClick={() => setPaginaAtual(prev => Math.min(totalPaginas, prev + 1))}
            disabled={paginaValida === totalPaginas}
          >
            Próximo →
          </button>
        </div>
      )}

      {modalOpen && (
        <FormularioUsuarioModal
          user={usuarioEditando}
          onClose={closeModal}
          onSave={handleSave}
        />
      )}

      {confirmacao && (
        <ModalConfirmacao
          mensagem={confirmacao.mensagem}
          titulo="Excluir usuário"
          textoBotaoConfirmar="Excluir"
          onConfirmar={confirmacao.acao}
          onCancelar={() => setConfirmacao(null)}
        />
      )}
    </div>
  )
}
