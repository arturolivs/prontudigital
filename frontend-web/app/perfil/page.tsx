'use client'

import { useState, useEffect, useRef, ChangeEvent } from 'react'
import {
  User,
  Mail,
  Phone,
  AtSign,
  Save,
  RefreshCw,
  Lock,
  Eye,
  EyeOff,
  Camera,
} from 'lucide-react'
import Layout from '@/components/Layout/Layout'
import { RotaProtegida } from '@/components/RotaProtegida'
import { useAuth } from '@/contexts/AuthContext'
import { useNotificacao } from '@/contexts/ToastContext'
import { usuariosAPI } from '@/lib/usuario.service'
import { tokenService } from '@/lib/auth.service'
import {
  mascaraCEP,
  mascaraCPF,
  mascaraTelefone,
  somenteDigitos,
} from '@/lib/mascaras'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import { Usuario } from '@/tipos/autenticacao'
import './perfil.css'

const PERFIS_ROTULO: Record<string, string> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_PROFISSIONAL: 'Profissional',
  ROLE_PACIENTE: 'Paciente',
  USUARIO: 'Usuário',
}

// Espelham TIPOS_AVATAR e TAMANHO_MAXIMO_AVATAR do UsuarioServiceImpl. A
// checagem no cliente é conveniência — evita subir 5 MB para receber 422 —,
// e não substitui a do backend.
const TIPOS_AVATAR = ['image/jpeg', 'image/png', 'image/webp']
const TAMANHO_MAXIMO_AVATAR = 2 * 1024 * 1024

// Ordem em que os campos aparecem na tela — é ela que define qual erro recebe
// o foco. Não dá para usar `Object.keys(erros)`: essa ordem é a de inserção,
// ou seja, a da função de validação, que não precisa coincidir com a visual.
// Os valores são os `id` dos inputs.
const CAMPOS_PERFIL = ['nomeCompleto', 'email']
const CAMPOS_SENHA = ['senhaAtual', 'novaSenha', 'confirmarSenha']

/**
 * Leva o usuário ao primeiro campo com erro, depois de um submit recusado.
 *
 * Sem isto, num formulário longo como o do perfil, a mensagem podia ficar
 * inteiramente fora da área visível: o clique em "Salvar" não parecia fazer
 * nada.
 */
function focarPrimeiroErro(erros: Record<string, string>, ordem: string[]) {
  const campo = ordem.find(id => erros[id])
  if (!campo) return

  const elemento = document.getElementById(campo)
  if (!elemento) return

  // `focus()` sozinho já rola a página, mas de forma abrupta e deixando o campo
  // colado na borda. `preventScroll` desliga esse salto para o scrollIntoView
  // logo abaixo centralizar o campo com animação.
  elemento.focus({ preventScroll: true })
  elemento.scrollIntoView({ behavior: 'smooth', block: 'center' })
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
  // RF04 — dados pessoais
  const [cpf, setCpf] = useState('')
  const [dataNascimento, setDataNascimento] = useState('')
  const [endereco, setEndereco] = useState({
    cep: '',
    logradouro: '',
    numero: '',
    complemento: '',
    bairro: '',
    cidade: '',
    uf: '',
  })
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

  // Avatar. `avatarUrl` é um object URL criado a partir do blob baixado — um
  // `<img src>` apontando direto ao endpoint não passaria o token.
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null)
  const [enviandoAvatar, setEnviandoAvatar] = useState(false)
  const inputAvatarRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    carregarDados()
  }, [])

  // Revoga o object URL anterior sempre que ele é trocado, e o último ao
  // desmontar. Sem isto, cada troca de foto vazaria o blob da anterior.
  useEffect(() => {
    if (!avatarUrl) return
    return () => URL.revokeObjectURL(avatarUrl)
  }, [avatarUrl])

  const carregarDados = async () => {
    try {
      setCarregando(true)
      const dados = await usuariosAPI.buscarUsuarioAtual()
      setDadosUsuario(dados)
      setNomeCompleto(dados.nomeCompleto || '')
      setEmail(dados.email || '')
      setTelefone(mascaraTelefone(dados.telefone || ''))
      setCpf(mascaraCPF(dados.cpf || ''))
      setDataNascimento(dados.dataNascimento || '')
      setEndereco({
        cep: mascaraCEP(dados.endereco?.cep || ''),
        logradouro: dados.endereco?.logradouro || '',
        numero: dados.endereco?.numero || '',
        complemento: dados.endereco?.complemento || '',
        bairro: dados.endereco?.bairro || '',
        cidade: dados.endereco?.cidade || '',
        uf: dados.endereco?.uf || '',
      })
      if (dados.temAvatar) await carregarAvatar()
    } catch {
      exibirNotificacao(MENSAGENS.erro.carregarPerfil, 'error')
    } finally {
      setCarregando(false)
    }
  }

  /**
   * Falha ao baixar a imagem não vira notificação de erro: o perfil continua
   * utilizável sem a foto, e o fallback de iniciais já comunica o estado.
   */
  const carregarAvatar = async () => {
    try {
      const blob = await usuariosAPI.baixarAvatar()
      setAvatarUrl(URL.createObjectURL(blob))
    } catch {
      setAvatarUrl(null)
    }
  }

  const aoEscolherAvatar = async (e: ChangeEvent<HTMLInputElement>) => {
    const arquivo = e.target.files?.[0]
    e.target.value = '' // permite reenviar o mesmo arquivo depois
    if (!arquivo) return

    if (!TIPOS_AVATAR.includes(arquivo.type)) {
      exibirNotificacao(MENSAGENS.erro.avatarTipoInvalido, 'error', 6000)
      return
    }
    if (arquivo.size > TAMANHO_MAXIMO_AVATAR) {
      exibirNotificacao(MENSAGENS.erro.avatarTamanho, 'error', 6000)
      return
    }

    try {
      setEnviandoAvatar(true)
      const atualizado = await usuariosAPI.enviarAvatar(arquivo)
      setDadosUsuario(atualizado)
      // Mostra o arquivo escolhido em vez de rebaixar o que acabou de subir.
      setAvatarUrl(URL.createObjectURL(arquivo))
      exibirNotificacao(MENSAGENS.sucesso.avatarAtualizado, 'success')
    } catch (err: any) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.enviarAvatar),
        'error',
        6000,
      )
    } finally {
      setEnviandoAvatar(false)
    }
  }

  const removerAvatar = async () => {
    try {
      setEnviandoAvatar(true)
      setDadosUsuario(await usuariosAPI.removerAvatar())
      setAvatarUrl(null)
      exibirNotificacao(MENSAGENS.sucesso.avatarRemovido, 'success')
    } catch (err: any) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.removerAvatar),
        'error',
        6000,
      )
    } finally {
      setEnviandoAvatar(false)
    }
  }

  // Devolvem os erros em vez de um booleano: `setErros` é assíncrono, então
  // quem chama não conseguiria ler o estado logo depois para saber em qual
  // campo dar foco.
  const validarPerfil = (): Record<string, string> => {
    const novosErros: Record<string, string> = {}
    if (!nomeCompleto.trim())
      novosErros.nomeCompleto = MENSAGENS.validacao.nomeObrigatorio
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      novosErros.email = MENSAGENS.validacao.emailInvalido
    setErros(novosErros)
    return novosErros
  }

  const validarSenha = (): Record<string, string> => {
    const novosErros: Record<string, string> = {}
    if (!senhaAtual)
      novosErros.senhaAtual = MENSAGENS.validacao.senhaAtualObrigatoria
    if (!novaSenha)
      novosErros.novaSenha = MENSAGENS.validacao.novaSenhaObrigatoria
    else if (novaSenha.length < 6)
      novosErros.novaSenha = MENSAGENS.validacao.senhaMinima
    if (!confirmarSenha)
      novosErros.confirmarSenha = MENSAGENS.validacao.confirmarSenhaObrigatoria
    else if (novaSenha !== confirmarSenha)
      novosErros.confirmarSenha = MENSAGENS.validacao.senhasNaoCoincidem
    setErrosSenha(novosErros)
    return novosErros
  }

  const handleSalvarPerfil = async () => {
    const errosValidacao = validarPerfil()
    if (Object.keys(errosValidacao).length > 0) {
      focarPrimeiroErro(errosValidacao, CAMPOS_PERFIL)
      return
    }
    if (!dadosUsuario) return
    try {
      setSalvando(true)
      const enderecoLimpo = {
        cep: somenteDigitos(endereco.cep),
        logradouro: endereco.logradouro.trim() || undefined,
        numero: endereco.numero.trim() || undefined,
        complemento: endereco.complemento.trim() || undefined,
        bairro: endereco.bairro.trim() || undefined,
        cidade: endereco.cidade.trim() || undefined,
        uf: endereco.uf.trim().toUpperCase() || undefined,
      }

      const atualizado = await usuariosAPI.atualizarPerfil(dadosUsuario.id, {
        nomeCompleto: nomeCompleto.trim(),
        email: email.trim() || undefined,
        telefone: telefone.trim() || undefined,
        // O backend guarda o CPF só com dígitos.
        cpf: somenteDigitos(cpf),
        dataNascimento: dataNascimento || undefined,
        endereco: Object.values(enderecoLimpo).some(Boolean)
          ? enderecoLimpo
          : undefined,
      })
      setDadosUsuario(atualizado)

      const dadosAtuais = tokenService.getDadosUsuario()
      if (dadosAtuais) {
        tokenService.setDadosUsuario({
          ...dadosAtuais,
          nomeCompleto: atualizado.nomeCompleto,
        })
      }

      exibirNotificacao(MENSAGENS.sucesso.perfilAtualizado, 'success')
    } catch (err: any) {
      exibirNotificacao(mensagemErro(err, MENSAGENS.erro.salvarPerfil), 'error')
    } finally {
      setSalvando(false)
    }
  }

  const handleAlterarSenha = async () => {
    const errosValidacao = validarSenha()
    if (Object.keys(errosValidacao).length > 0) {
      focarPrimeiroErro(errosValidacao, CAMPOS_SENHA)
      return
    }
    if (!dadosUsuario) return
    try {
      setSalvandoSenha(true)
      await usuariosAPI.alterarSenha(dadosUsuario.id, senhaAtual, novaSenha)
      setSenhaAtual('')
      setNovaSenha('')
      setConfirmarSenha('')
      setErrosSenha({})
      exibirNotificacao(MENSAGENS.sucesso.senhaAlterada, 'success')
    } catch (err: any) {
      const mensagem = mensagemErro(err, MENSAGENS.erro.alterarSenha)
      if (err.response?.status === 422) {
        // Senha atual incorreta: o backend só descobre no submit, mas o erro
        // pertence a um campo — leva o usuário até ele como na validação local.
        setErrosSenha(prev => ({ ...prev, senhaAtual: mensagem }))
        focarPrimeiroErro({ senhaAtual: mensagem }, CAMPOS_SENHA)
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

  const enderecoSalvo = dadosUsuario?.endereco
  const temAlteracaoPerfil =
    dadosUsuario &&
    (nomeCompleto.trim() !== (dadosUsuario.nomeCompleto || '') ||
      email.trim() !== (dadosUsuario.email || '') ||
      telefone.trim() !== (dadosUsuario.telefone || '') ||
      (somenteDigitos(cpf) ?? '') !== (dadosUsuario.cpf || '') ||
      dataNascimento !== (dadosUsuario.dataNascimento || '') ||
      (somenteDigitos(endereco.cep) ?? '') !== (enderecoSalvo?.cep || '') ||
      endereco.logradouro.trim() !== (enderecoSalvo?.logradouro || '') ||
      endereco.numero.trim() !== (enderecoSalvo?.numero || '') ||
      endereco.complemento.trim() !== (enderecoSalvo?.complemento || '') ||
      endereco.bairro.trim() !== (enderecoSalvo?.bairro || '') ||
      endereco.cidade.trim() !== (enderecoSalvo?.cidade || '') ||
      endereco.uf.trim() !== (enderecoSalvo?.uf || ''))

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
              <div className="perfil-avatar-bloco">
                <button
                  type="button"
                  className="perfil-avatar"
                  onClick={() => inputAvatarRef.current?.click()}
                  disabled={enviandoAvatar}
                  title="Alterar foto de perfil"
                  aria-label="Alterar foto de perfil"
                >
                  {avatarUrl ? (
                    <img src={avatarUrl} alt="" className="perfil-avatar-img" />
                  ) : (
                    <span className="perfil-avatar-iniciais">
                      {iniciais || '?'}
                    </span>
                  )}
                  <span className="perfil-avatar-overlay">
                    {enviandoAvatar ? (
                      <RefreshCw size={18} className="perfil-avatar-girando" />
                    ) : (
                      <Camera size={18} />
                    )}
                  </span>
                </button>

                <input
                  ref={inputAvatarRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={aoEscolherAvatar}
                  hidden
                />

                {avatarUrl && !enviandoAvatar && (
                  <button
                    type="button"
                    className="perfil-avatar-remover"
                    onClick={removerAvatar}
                  >
                    Remover foto
                  </button>
                )}
              </div>
              <div className="perfil-identidade-info">
                <span className="perfil-nome-exibido">
                  {nomeCompleto || 'Sem nome'}
                </span>
                <span className="perfil-username">
                  @{dadosUsuario?.username}
                </span>
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
                      onChange={e =>
                        setTelefone(mascaraTelefone(e.target.value))
                      }
                      placeholder="(99) 9 9999-9999"
                    />
                  </div>
                </div>

                {/* RF04 — identificação */}
                <div className="campo">
                  <label htmlFor="cpf">CPF</label>
                  <input
                    id="cpf"
                    type="text"
                    className="campo-input"
                    value={cpf}
                    onChange={e => setCpf(mascaraCPF(e.target.value))}
                    placeholder="000.000.000-00"
                    inputMode="numeric"
                  />
                </div>

                <div className="campo">
                  <label htmlFor="dataNascimento">Data de nascimento</label>
                  <input
                    id="dataNascimento"
                    type="date"
                    className="campo-input"
                    value={dataNascimento}
                    onChange={e => setDataNascimento(e.target.value)}
                    max={new Date().toISOString().split('T')[0]}
                  />
                </div>

                {/* RF04 — endereço */}
                <div className="campo">
                  <label htmlFor="cep">CEP</label>
                  <input
                    id="cep"
                    type="text"
                    className="campo-input"
                    value={endereco.cep}
                    onChange={e =>
                      setEndereco(prev => ({
                        ...prev,
                        cep: mascaraCEP(e.target.value),
                      }))
                    }
                    placeholder="00000-000"
                    inputMode="numeric"
                  />
                </div>

                <div className="campo">
                  <label htmlFor="logradouro">Logradouro</label>
                  <input
                    id="logradouro"
                    type="text"
                    className="campo-input"
                    value={endereco.logradouro}
                    onChange={e =>
                      setEndereco(prev => ({
                        ...prev,
                        logradouro: e.target.value,
                      }))
                    }
                    placeholder="Rua, avenida..."
                  />
                </div>

                <div className="campo">
                  <label htmlFor="numero">Número</label>
                  <input
                    id="numero"
                    type="text"
                    className="campo-input"
                    value={endereco.numero}
                    onChange={e =>
                      setEndereco(prev => ({ ...prev, numero: e.target.value }))
                    }
                  />
                </div>

                <div className="campo">
                  <label htmlFor="complemento">Complemento</label>
                  <input
                    id="complemento"
                    type="text"
                    className="campo-input"
                    value={endereco.complemento}
                    onChange={e =>
                      setEndereco(prev => ({
                        ...prev,
                        complemento: e.target.value,
                      }))
                    }
                    placeholder="Apto, bloco..."
                  />
                </div>

                <div className="campo">
                  <label htmlFor="bairro">Bairro</label>
                  <input
                    id="bairro"
                    type="text"
                    className="campo-input"
                    value={endereco.bairro}
                    onChange={e =>
                      setEndereco(prev => ({ ...prev, bairro: e.target.value }))
                    }
                  />
                </div>

                <div className="campo">
                  <label htmlFor="cidade">Cidade</label>
                  <input
                    id="cidade"
                    type="text"
                    className="campo-input"
                    value={endereco.cidade}
                    onChange={e =>
                      setEndereco(prev => ({ ...prev, cidade: e.target.value }))
                    }
                  />
                </div>

                <div className="campo">
                  <label htmlFor="uf">UF</label>
                  <input
                    id="uf"
                    type="text"
                    className="campo-input"
                    value={endereco.uf}
                    onChange={e =>
                      setEndereco(prev => ({
                        ...prev,
                        uf: e.target.value
                          .replace(/[^A-Za-z]/g, '')
                          .slice(0, 2),
                      }))
                    }
                    placeholder="SP"
                    maxLength={2}
                  />
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
                      setCpf(mascaraCPF(dadosUsuario.cpf || ''))
                      setDataNascimento(dadosUsuario.dataNascimento || '')
                      setEndereco({
                        cep: mascaraCEP(dadosUsuario.endereco?.cep || ''),
                        logradouro: dadosUsuario.endereco?.logradouro || '',
                        numero: dadosUsuario.endereco?.numero || '',
                        complemento: dadosUsuario.endereco?.complemento || '',
                        bairro: dadosUsuario.endereco?.bairro || '',
                        cidade: dadosUsuario.endereco?.cidade || '',
                        uf: dadosUsuario.endereco?.uf || '',
                      })
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
                    O nome de usuário é usado para login e não pode ser alterado
                    aqui
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
                      {mostrarSenhas.atual ? (
                        <EyeOff size={16} />
                      ) : (
                        <Eye size={16} />
                      )}
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
                      {mostrarSenhas.nova ? (
                        <EyeOff size={16} />
                      ) : (
                        <Eye size={16} />
                      )}
                    </button>
                  </div>
                  {errosSenha.novaSenha && (
                    <span className="campo-erro">{errosSenha.novaSenha}</span>
                  )}
                  {novaSenha && !errosSenha.novaSenha && (
                    <div className="senha-forca">
                      <div
                        className={`senha-forca-barra${novaSenha.length >= 10 ? ' forte' : novaSenha.length >= 6 ? ' media' : ' fraca'}`}
                      />
                      <span className="senha-forca-rotulo">
                        {novaSenha.length >= 10
                          ? 'Forte'
                          : novaSenha.length >= 6
                            ? 'Média'
                            : 'Fraca'}
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
                      {mostrarSenhas.confirmar ? (
                        <EyeOff size={16} />
                      ) : (
                        <Eye size={16} />
                      )}
                    </button>
                  </div>
                  {errosSenha.confirmarSenha && (
                    <span className="campo-erro">
                      {errosSenha.confirmarSenha}
                    </span>
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
