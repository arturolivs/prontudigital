'use client'

import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import Modal from '@/components/Modal'
import ModalConfirmacao from '@/components/ModalConfirmacao'
import { procedimentoAPI } from '@/lib/procedimento.service'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import { Procedimento, ProcedimentoRequisicao } from '@/tipos/procedimento'
import { useNotificacao } from '@/contexts/ToastContext'
import '../usuarios/usuariosPage.css'
import './procedimentosPage.css'

interface FormularioProcedimento {
  nome: string
  descricao: string
  duracaoPadraoMinutos: string
  ativo: boolean
}

const formVazio: FormularioProcedimento = {
  nome: '',
  descricao: '',
  duracaoPadraoMinutos: '',
  ativo: true,
}

export default function ProcedimentosPage() {
  const [procedimentos, setProcedimentos] = useState<Procedimento[]>([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editando, setEditando] = useState<Procedimento | null>(null)
  const [form, setForm] = useState<FormularioProcedimento>(formVazio)
  const [salvando, setSalvando] = useState(false)
  const [erroForm, setErroForm] = useState('')
  const [confirmacao, setConfirmacao] = useState<{
    mensagem: string
    acao: () => void
  } | null>(null)
  const { exibirNotificacao } = useNotificacao()

  const fetchProcedimentos = async () => {
    try {
      setLoading(true)
      const lista = await procedimentoAPI.listar(true)
      setProcedimentos(lista)
    } catch {
      exibirNotificacao(MENSAGENS.erro.carregarProcedimentos, 'error', 6000)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProcedimentos()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const abrirCriacao = () => {
    setEditando(null)
    setForm(formVazio)
    setErroForm('')
    setModalOpen(true)
  }

  const abrirEdicao = (p: Procedimento) => {
    setEditando(p)
    setForm({
      nome: p.nome,
      descricao: p.descricao ?? '',
      duracaoPadraoMinutos: p.duracaoPadraoMinutos?.toString() ?? '',
      ativo: p.ativo,
    })
    setErroForm('')
    setModalOpen(true)
  }

  const fecharModal = () => {
    setModalOpen(false)
    setEditando(null)
  }

  const handleSalvar = async () => {
    if (!form.nome.trim()) return
    setErroForm('')
    setSalvando(true)
    try {
      const dados: ProcedimentoRequisicao = {
        nome: form.nome.trim(),
        descricao: form.descricao.trim() || undefined,
        duracaoPadraoMinutos: form.duracaoPadraoMinutos
          ? Number(form.duracaoPadraoMinutos)
          : undefined,
        ativo: form.ativo,
      }
      if (editando) {
        await procedimentoAPI.atualizar(editando.id, dados)
        exibirNotificacao(MENSAGENS.sucesso.procedimentoAtualizado, 'success', 6000)
      } else {
        await procedimentoAPI.criar(dados)
        exibirNotificacao(MENSAGENS.sucesso.procedimentoCriado, 'success', 6000)
      }
      fecharModal()
      await fetchProcedimentos()
    } catch (err: any) {
      setErroForm(mensagemErro(err, MENSAGENS.erro.salvarProcedimento))
    } finally {
      setSalvando(false)
    }
  }

  const handleToggleAtivo = async (p: Procedimento) => {
    try {
      const atualizado = await procedimentoAPI.atualizar(p.id, {
        nome: p.nome,
        descricao: p.descricao,
        duracaoPadraoMinutos: p.duracaoPadraoMinutos,
        ativo: !p.ativo,
      })
      setProcedimentos(prev =>
        prev.map(item => (item.id === p.id ? atualizado : item)),
      )
    } catch (err: any) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.salvarProcedimento),
        'error',
        6000,
      )
    }
  }

  const handleExcluir = (p: Procedimento) => {
    setConfirmacao({
      mensagem: `Tem certeza que deseja excluir o procedimento "${p.nome}"?`,
      acao: async () => {
        setConfirmacao(null)
        try {
          await procedimentoAPI.excluir(p.id)
          exibirNotificacao(MENSAGENS.sucesso.procedimentoExcluido, 'success', 6000)
          await fetchProcedimentos()
        } catch (err: any) {
          exibirNotificacao(
            mensagemErro(err, MENSAGENS.erro.excluirProcedimento),
            'error',
            6000,
          )
        }
      },
    })
  }

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
        <h1 className="users-title">Procedimentos</h1>
        <button onClick={abrirCriacao} className="btn-primary">
          <Plus size={16} strokeWidth={2} />
          Novo Procedimento
        </button>
      </div>

      <div className="users-table-container">
        <table className="users-table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Descrição</th>
              <th>Duração padrão</th>
              <th>Status</th>
              <th className="action-buttons-header">Ações</th>
            </tr>
          </thead>
          <tbody>
            {procedimentos.map(p => (
              <tr key={p.id}>
                <td>{p.nome}</td>
                <td>{p.descricao || '—'}</td>
                <td>
                  {p.duracaoPadraoMinutos ? `${p.duracaoPadraoMinutos} min` : '—'}
                </td>
                <td>
                  <button
                    onClick={() => handleToggleAtivo(p)}
                    className={`status-badge ${
                      p.ativo ? 'status-badge-active' : 'status-badge-inactive'
                    }`}
                    title={p.ativo ? 'Desativar' : 'Ativar'}
                  >
                    {p.ativo ? 'Ativo' : 'Inativo'}
                  </button>
                </td>
                <td className="action-buttons">
                  <button
                    onClick={() => abrirEdicao(p)}
                    className="btn-edit"
                    title="Editar"
                  >
                    <Pencil size={14} />
                    <span>Editar</span>
                  </button>
                  <button
                    onClick={() => handleExcluir(p)}
                    className="btn-delete"
                    title="Excluir"
                  >
                    <Trash2 size={14} />
                    <span>Excluir</span>
                  </button>
                </td>
              </tr>
            ))}
            {procedimentos.length === 0 && (
              <tr>
                <td colSpan={5}>Nenhum procedimento cadastrado.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {modalOpen && (
        <Modal
          titulo={editando ? 'Editar procedimento' : 'Novo procedimento'}
          tamanho="md"
          onClose={fecharModal}
          rodape={
            <>
              <button
                className="proc-btn-cancelar"
                onClick={fecharModal}
                disabled={salvando}
              >
                Cancelar
              </button>
              <button
                className="proc-btn-salvar"
                onClick={handleSalvar}
                disabled={!form.nome.trim() || salvando}
              >
                {salvando ? 'Salvando…' : 'Salvar'}
              </button>
            </>
          }
        >
          <div className="proc-form">
            {erroForm && <div className="proc-form-erro">{erroForm}</div>}

            <div className="proc-form-grupo">
              <label className="proc-form-rotulo" htmlFor="proc-nome">
                Nome
              </label>
              <input
                id="proc-nome"
                className="proc-form-input"
                value={form.nome}
                maxLength={100}
                onChange={e => setForm({ ...form, nome: e.target.value })}
                placeholder="Ex: Podiatria"
              />
            </div>

            <div className="proc-form-grupo">
              <label className="proc-form-rotulo" htmlFor="proc-descricao">
                Descrição
              </label>
              <textarea
                id="proc-descricao"
                className="proc-form-textarea"
                value={form.descricao}
                onChange={e => setForm({ ...form, descricao: e.target.value })}
                placeholder="Descrição do procedimento (opcional)"
              />
            </div>

            <div className="proc-form-grupo">
              <label className="proc-form-rotulo" htmlFor="proc-duracao">
                Duração padrão (minutos)
              </label>
              <input
                id="proc-duracao"
                type="number"
                min={1}
                className="proc-form-input"
                value={form.duracaoPadraoMinutos}
                onChange={e =>
                  setForm({ ...form, duracaoPadraoMinutos: e.target.value })
                }
                placeholder="Ex: 60"
              />
            </div>

            <label className="proc-form-check">
              <input
                type="checkbox"
                checked={form.ativo}
                onChange={e => setForm({ ...form, ativo: e.target.checked })}
              />
              Disponível para agendamento
            </label>
          </div>
        </Modal>
      )}

      {confirmacao && (
        <ModalConfirmacao
          mensagem={confirmacao.mensagem}
          titulo="Excluir procedimento"
          textoBotaoConfirmar="Excluir"
          onConfirmar={confirmacao.acao}
          onCancelar={() => setConfirmacao(null)}
        />
      )}
    </div>
  )
}
