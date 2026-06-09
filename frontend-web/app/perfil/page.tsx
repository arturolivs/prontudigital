'use client'

import { useState, useEffect } from 'react'
import { User, Mail, Phone, AtSign, Save, RefreshCw, Lock, Eye, EyeOff } from 'lucide-react'
import Layout from '@/components/Layout/Layout'
import { RotaProtegida } from '@/components/RotaProtegida'
import { useAuth } from '@/contexts/AuthContext'
import { useNotificacao } from '@/contexts/ToastContext'
import { usuariosAPI } from '@/lib/usuario.service'
import { tokenService } from '@/lib/auth.service'
import { mascaraTelefone } from '@/lib/mascaras'
import { Usuario } from '@/tipos/autenticacao'
import './perfil.css'

const PERFIS_ROTULO: Record<string, string> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_PROFISSIONAL: 'Profissional',
  ROLE_PACIENTE: 'Paciente',
  USUARIO: 'Usuário',
}

export default function PerfilPage() {
  const { temPerfil } = useAuth()
  const { exibirNotificacao } = useNotificacao()

  const isAdmin = temPerfil('ROLE_ADMIN')
  const perfilLayout = isAdmin ? 'ROLE_ADMIN' : 'ROLE_PROFISSIONAL'

  const [dadosUsuario, setDadosUsuario] = useState<Usuario | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [salvando, setSalvando] = useState(false)
  const [salvandoSenha, setSalvandoSenha] = useState(false)

  // Campos do perfil
  const [nomeCompleto, setNomeCompleto] = useState('')
  const [email, setEmail] = useState('')
  const [telefone, setTelefone] = useState('')
  const [erros, setErros] = useState<Record<string, string>>({})

  // Campos de senha
  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmarSenha, setConfirmarSenha] = useState('')
  const [errosSenha, setErrosSenha] = useState<Record<string, string>>({})
  const [mostrarSenhas, setMostrarSenhas] = useState({
    atual: false,
    nova: false,
    confirmar: false,
  })

  useEffect(() => {
    carregarDados()
  }, [])

  const carregarDados = async () => {
    try {
      setCarregando(true)
      const dados = await usuariosAPI.buscarUsuarioAtual()
      setDadosUsuario(dados)
      setNomeCompleto(dados.nomeCompleto || '')
      setEmail(dados.email || '')
      setTelefone(mascaraTelefone(dados.telefone || ''))
    } catch {
      exibirNotificacao('Erro ao carregar dados do perfil', 'error')
    } finally {
      setCarregando(false)
    }
  }

  const validarPerfil = (): boolean => {
    const novosErros: Record<string, string> = {}
    if (!nomeCompleto.trim()) novosErros.nomeCompleto = 'Nome é obrigatório'
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      novosErros.email = 'Email inválido'
    setErros(novosErros)
    return Object.keys(novosErros).length === 0
  }

  const validarSenha = (): boolean => {
    const novosErros: Record<string, string> = {}
    if (!senhaAtual) novosErros.senhaAtual = 'Informe a senha atual'
    if (!novaSenha) novosErros.novaSenha = 'Informe a nova senha'
    else if (novaSenha.length < 6)
      novosErros.novaSenha = 'A senha deve ter pelo menos 6 caracteres'
    if (!confirmarSenha) novosErros.confirmarSenha = 'Confirme a nova senha'
    else if (novaSenha !== confirmarSenha)
      novosErros.confirmarSenha = 'As senhas não coincidem'
    setErrosSenha(novosErros)
    return Object.keys(novosErros).length === 0
  }

  const handleSalvarPerfil = async () => {
    if (!validarPerfil() || !dadosUsuario) return
    try {
      setSalvando(true)
      const atualizado = await usuariosAPI.atualizarPerfil(dadosUsuario.id, {
        nomeCompleto: nomeCompleto.trim(),
        email: email.trim() || undefined,
        telefone: telefone.trim() || undefined,
      })
      setDadosUsuario(atualizado)

      const dadosAtuais = tokenService.getDadosUsuario()
      if (dadosAtuais) {
        tokenService.setDadosUsuario({
          ...dadosAtuais,
          nomeCompleto: atualizado.nomeCompleto,
        })
      }

      exibirNotificacao('Perfil atualizado com sucesso!', 'success')
    } catch (err: any) {
      exibirNotificacao(
        err.response?.data?.message || 'Erro ao salvar perfil',
        'error',
      )
    } finally {
      setSalvando(false)
    }
  }

  const handleAlterarSenha = async () => {
    if (!validarSenha() || !dadosUsuario) return
    try {
      setSalvandoSenha(true)
      await usuariosAPI.alterarSenha(dadosUsuario.id, senhaAtual, novaSenha)
      setSenhaAtual('')
      setNovaSenha('')
      setConfirmarSenha('')
      setErrosSenha({})
      exibirNotificacao('Senha alterada com sucesso!', 'success')
    } catch (err: any) {
      const mensagem = err.response?.data?.message || 'Erro ao alterar senha'
      if (err.response?.status === 422) {
        setErrosSenha(prev => ({ ...prev, senhaAtual: mensagem }))
      } else {
        exibirNotificacao(mensagem, 'error')
      }
    } finally {
      setSalvandoSenha(false)
    }
  }

  const toggleVisibilidade = (campo: 'atual' | 'nova' | 'confirmar') => {
    setMostrarSenhas(prev => ({ ...prev, [campo]: !prev[campo] }))
  }

  const iniciais = nomeCompleto
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map(n => n[0].toUpperCase())
    .join('')

  const temAlteracaoPerfil =
    dadosUsuario &&
    (nomeCompleto.trim() !== (dadosUsuario.nomeCompleto || '') ||
      email.trim() !== (dadosUsuario.email || '') ||
      telefone.trim() !== (dadosUsuario.telefone || ''))

  const temCampoSenha = senhaAtual || novaSenha || confirmarSenha

  if (carregando) {
    return (
      <Layout perfil={perfilLayout}>
        <RotaProtegida perfisNecessarios={['ROLE_PROFISSIONAL', 'ROLE_ADMIN']}>
          <div className="perfil-loading">
            <div className="spinner" />
            Carregando perfil...
          </div>
        </RotaProtegida>
      </Layout>
    )
  }

  return (
    <Layout perfil={perfilLayout}>
      <RotaProtegida perfisNecessarios={['ROLE_PROFISSIONAL', 'ROLE_ADMIN']}>
        <div className="perfil-page">
          <div className="perfil-header">
            <div className="perfil-header-texto">
              <h1>Meu Perfil</h1>
              <p>Atualize suas informações pessoais e de contato</p>
            </div>
          </div>

          <div className="perfil-conteudo">
            {/* Avatar + identidade */}
            <div className="perfil-identidade">
              <div className="perfil-avatar">{iniciais || '?'}</div>
              <div className="perfil-identidade-info">
                <span className="perfil-nome-exibido">
                  {nomeCompleto || 'Sem nome'}
                </span>
                <span className="perfil-username">@{dadosUsuario?.username}</span>
                <div className="perfil-badges">
                  {dadosUsuario?.perfis.map(p => (
                    <span key={p} className="perfil-badge">
                      {PERFIS_ROTULO[p] || p}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            {/* Informações pessoais */}
            <div className="perfil-card">
              <h2 className="perfil-secao-titulo">Informações Pessoais</h2>
              <div className="perfil-campos">
                <div className="campo">
                  <label htmlFor="nomeCompleto">
                    Nome Completo <span className="campo-obrigatorio">*</span>
                  </label>
                  <div className="campo-icone-wrapper">
                    <User size={16} className="campo-icone" />
                    <input
                      id="nomeCompleto"
                      type="text"
                      className={`campo-input com-icone${erros.nomeCompleto ? ' campo-input--erro' : ''}`}
                      value={nomeCompleto}
                      onChange={e => {
                        setNomeCompleto(e.target.value)
                        setErros(prev => ({ ...prev, nomeCompleto: '' }))
                      }}
                      placeholder="Seu nome completo"
                    />
                  </div>
                  {erros.nomeCompleto && (
                    <span className="campo-erro">{erros.nomeCompleto}</span>
                  )}
                </div>

                <div className="campo">
                  <label htmlFor="email">Email</label>
                  <div className="campo-icone-wrapper">
                    <Mail size={16} className="campo-icone" />
                    <input
                      id="email"
                      type="email"
                      className={`campo-input com-icone${erros.email ? ' campo-input--erro' : ''}`}
                      value={email}
                      onChange={e => {
                        setEmail(e.target.value)
                        setErros(prev => ({ ...prev, email: '' }))
                      }}
                      placeholder="seu@email.com"
                    />
                  </div>
                  {erros.email && (
                    <span className="campo-erro">{erros.email}</span>
                  )}
                </div>

                <div className="campo">
                  <label htmlFor="telefone">Telefone</label>
                  <div className="campo-icone-wrapper">
                    <Phone size={16} className="campo-icone" />
                    <input
                      id="telefone"
                      type="tel"
                      className="campo-input com-icone"
                      value={telefone}
                      onChange={e => setTelefone(mascaraTelefone(e.target.value))}
                      placeholder="(99) 9 9999-9999"
                    />
                  </div>
                </div>
              </div>

              <div className="perfil-acoes">
                {temAlteracaoPerfil && (
                  <button
                    className="btn-descartar"
                    onClick={() => {
                      if (!dadosUsuario) return
                      setNomeCompleto(dadosUsuario.nomeCompleto || '')
                      setEmail(dadosUsuario.email || '')
                      setTelefone(dadosUsuario.telefone || '')
                      setErros({})
                    }}
                    disabled={salvando}
                  >
                    <RefreshCw size={15} />
                    Descartar
                  </button>
                )}
                <button
                  className="btn-salvar-perfil"
                  onClick={handleSalvarPerfil}
                  disabled={salvando || !temAlteracaoPerfil}
                >
                  <Save size={15} />
                  {salvando ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </div>

            {/* Acesso */}
            <div className="perfil-card">
              <h2 className="perfil-secao-titulo">Acesso</h2>
              <div className="perfil-campos">
                <div className="campo">
                  <label htmlFor="username">Nome de usuário</label>
                  <div className="campo-icone-wrapper">
                    <AtSign size={16} className="campo-icone" />
                    <input
                      id="username"
                      type="text"
                      className="campo-input com-icone campo-input--readonly"
                      value={dadosUsuario?.username || ''}
                      readOnly
                    />
                  </div>
                  <span className="campo-hint">
                    O nome de usuário é usado para login e não pode ser alterado aqui
                  </span>
                </div>
              </div>
            </div>

            {/* Alterar senha */}
            <div className="perfil-card">
              <h2 className="perfil-secao-titulo">Alterar Senha</h2>
              <div className="perfil-campos">
                <div className="campo">
                  <label htmlFor="senhaAtual">Senha atual</label>
                  <div className="campo-icone-wrapper">
                    <Lock size={16} className="campo-icone" />
                    <input
                      id="senhaAtual"
                      type={mostrarSenhas.atual ? 'text' : 'password'}
                      className={`campo-input com-icone com-toggle${errosSenha.senhaAtual ? ' campo-input--erro' : ''}`}
                      value={senhaAtual}
                      onChange={e => {
                        setSenhaAtual(e.target.value)
                        setErrosSenha(prev => ({ ...prev, senhaAtual: '' }))
                      }}
                      placeholder="Digite sua senha atual"
                      autoComplete="current-password"
                    />
                    <button
                      type="button"
                      className="campo-toggle-visibilidade"
                      onClick={() => toggleVisibilidade('atual')}
                      tabIndex={-1}
                    >
                      {mostrarSenhas.atual ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                  {errosSenha.senhaAtual && (
                    <span className="campo-erro">{errosSenha.senhaAtual}</span>
                  )}
                </div>

                <div className="campo">
                  <label htmlFor="novaSenha">Nova senha</label>
                  <div className="campo-icone-wrapper">
                    <Lock size={16} className="campo-icone" />
                    <input
                      id="novaSenha"
                      type={mostrarSenhas.nova ? 'text' : 'password'}
                      className={`campo-input com-icone com-toggle${errosSenha.novaSenha ? ' campo-input--erro' : ''}`}
                      value={novaSenha}
                      onChange={e => {
                        setNovaSenha(e.target.value)
                        setErrosSenha(prev => ({ ...prev, novaSenha: '' }))
                      }}
                      placeholder="Mínimo de 6 caracteres"
                      autoComplete="new-password"
                    />
                    <button
                      type="button"
                      className="campo-toggle-visibilidade"
                      onClick={() => toggleVisibilidade('nova')}
                      tabIndex={-1}
                    >
                      {mostrarSenhas.nova ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                  {errosSenha.novaSenha && (
                    <span className="campo-erro">{errosSenha.novaSenha}</span>
                  )}
                  {novaSenha && !errosSenha.novaSenha && (
                    <div className="senha-forca">
                      <div className={`senha-forca-barra${novaSenha.length >= 10 ? ' forte' : novaSenha.length >= 6 ? ' media' : ' fraca'}`} />
                      <span className="senha-forca-rotulo">
                        {novaSenha.length >= 10 ? 'Forte' : novaSenha.length >= 6 ? 'Média' : 'Fraca'}
                      </span>
                    </div>
                  )}
                </div>

                <div className="campo">
                  <label htmlFor="confirmarSenha">Confirmar nova senha</label>
                  <div className="campo-icone-wrapper">
                    <Lock size={16} className="campo-icone" />
                    <input
                      id="confirmarSenha"
                      type={mostrarSenhas.confirmar ? 'text' : 'password'}
                      className={`campo-input com-icone com-toggle${errosSenha.confirmarSenha ? ' campo-input--erro' : ''}`}
                      value={confirmarSenha}
                      onChange={e => {
                        setConfirmarSenha(e.target.value)
                        setErrosSenha(prev => ({ ...prev, confirmarSenha: '' }))
                      }}
                      placeholder="Repita a nova senha"
                      autoComplete="new-password"
                    />
                    <button
                      type="button"
                      className="campo-toggle-visibilidade"
                      onClick={() => toggleVisibilidade('confirmar')}
                      tabIndex={-1}
                    >
                      {mostrarSenhas.confirmar ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                  {errosSenha.confirmarSenha && (
                    <span className="campo-erro">{errosSenha.confirmarSenha}</span>
                  )}
                </div>
              </div>

              <div className="perfil-acoes">
                {temCampoSenha && (
                  <button
                    className="btn-descartar"
                    onClick={() => {
                      setSenhaAtual('')
                      setNovaSenha('')
                      setConfirmarSenha('')
                      setErrosSenha({})
                    }}
                    disabled={salvandoSenha}
                  >
                    <RefreshCw size={15} />
                    Limpar
                  </button>
                )}
                <button
                  className="btn-salvar-perfil"
                  onClick={handleAlterarSenha}
                  disabled={salvandoSenha || !temCampoSenha}
                >
                  <Lock size={15} />
                  {salvandoSenha ? 'Alterando...' : 'Alterar senha'}
                </button>
              </div>
            </div>
          </div>
        </div>
      </RotaProtegida>
    </Layout>
  )
}
