'use client'

import { useState, useEffect } from 'react'
import { autenticacaoAPI, tokenService } from '../../lib/auth.service'
import { usuariosAPI } from '../../lib/usuario.service'
import { agendamentoAPI } from '../../lib/agendamento.service'
import {
  agendamentoPublicoAPI,
  ProfissionalPublico,
} from '../../lib/agendamento-publico.service'
import { BloqueioHorario } from '../../tipos/bloqueio'
import { Agendamento } from '../../tipos/agendamento'
import { TipoProcedimento, ROTULO_TIPO_PROCEDIMENTO } from '../../tipos/TipoProcedimento'
import { LocalAtendimento, ROTULO_LOCAL_ATENDIMENTO } from '../../tipos/LocalAtendimento'
import { mascaraTelefone } from '../../lib/mascaras'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import './agendar.css'

/* ── helpers ── */

const amanha = (): string => {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return d.toISOString().split('T')[0]
}

const iniciais = (nome: string) =>
  nome
    .split(' ')
    .slice(0, 2)
    .map(n => n[0])
    .join('')
    .toUpperCase()

const SLOTS_HORA = [
  '08:00',
  '09:00',
  '10:00',
  '11:00',
  '12:00',
  '13:00',
  '14:00',
  '15:00',
  '16:00',
]

const slotBloqueado = (
  slot: string,
  data: string,
  bloqueios: BloqueioHorario[],
) => {
  const [h, m] = slot.split(':').map(Number)
  const inicio = new Date(`${data}T${slot}:00`)
  const fim = new Date(inicio.getTime() + 60 * 60 * 1000)
  return bloqueios.some(b => {
    const bIni = new Date(b.inicioEm)
    const bFim = new Date(b.fimEm)
    return inicio < bFim && fim > bIni
  })
}

const adicionarHora = (slot: string): string => {
  const [h, m] = slot.split(':').map(Number)
  const total = h + 1
  return `${String(total).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

const formatarDataExibicao = (data: string): string => {
  const d = new Date(data + 'T00:00:00')
  return d.toLocaleDateString('pt-BR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })
}

/* ── tipos internos ── */

type Etapa = 1 | 2 | 3 | 4 | 'sucesso'
type ModoAuth = 'login' | 'cadastro'

const ETAPAS = [
  { num: 1, rotulo: 'Profissional' },
  { num: 2, rotulo: 'Horário' },
  { num: 3, rotulo: 'Identificação' },
  { num: 4, rotulo: 'Confirmação' },
]

/* ══════════════════════════════════════════════ */
export default function AgendarPage() {
  const [etapa, setEtapa] = useState<Etapa>(1)

  /* dados do fluxo */
  const [profissional, setProfissional] = useState<ProfissionalPublico | null>(
    null,
  )
  const [data, setData] = useState(amanha)
  const [slot, setSlot] = useState<string | null>(null)
  const [tipoProcedimento, setTipoProcedimento] = useState<TipoProcedimento | null>(null)
  const [localAtendimento, setLocalAtendimento] = useState<LocalAtendimento | null>(null)
  const [pacienteAcamado, setPacienteAcamado] = useState<boolean | null>(null)
  const [pacienteUuid, setPacienteUuid] = useState<string | null>(null)
  const [agendamento, setAgendamento] = useState<Agendamento | null>(
    null,
  )

  /* listas */
  const [profissionais, setProfissionais] = useState<ProfissionalPublico[]>([])
  const [bloqueios, setBloqueios] = useState<BloqueioHorario[]>([])

  /* loading / erro */
  const [carregandoProf, setCarregandoProf] = useState(true)
  const [carregandoSlots, setCarregandoSlots] = useState(false)
  const [carregandoAuth, setCarregandoAuth] = useState(false)
  const [carregandoConfirmar, setCarregandoConfirmar] = useState(false)
  const [erro, setErro] = useState('')

  /* auth */
  const [modoAuth, setModoAuth] = useState<ModoAuth>('login')
  const [loginForm, setLoginForm] = useState({ username: '', senha: '' })
  const [cadastroForm, setCadastroForm] = useState({
    nomeCompleto: '',
    telefone: '',
  })

  /* ── carregar profissionais ── */
  useEffect(() => {
    agendamentoPublicoAPI
      .listarProfissionais()
      .then(setProfissionais)
      .catch(() => setErro(MENSAGENS.erro.carregarProfissionais))
      .finally(() => setCarregandoProf(false))
  }, [])

  /* ── carregar bloqueios ao avançar para etapa 2 ── */
  const carregarBloqueios = async (prof: ProfissionalPublico, d: string) => {
    setCarregandoSlots(true)
    setSlot(null)
    try {
      const lista = await agendamentoPublicoAPI.listarBloqueios(prof.uuid, d)
      setBloqueios(lista)
    } catch {
      setBloqueios([])
    } finally {
      setCarregandoSlots(false)
    }
  }

  const irParaEtapa2 = () => {
    if (!profissional) return
    carregarBloqueios(profissional, data)
    setEtapa(2)
    setErro('')
  }

  const handleDataChange = (novaData: string) => {
    setData(novaData)
    if (profissional) carregarBloqueios(profissional, novaData)
  }

  /* ── autenticação na etapa 3 ── */
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setErro('')
    setCarregandoAuth(true)
    try {
      const resp = await autenticacaoAPI.login({
        username: loginForm.username.trim(),
        senha: loginForm.senha,
      })
      tokenService.setTokens(resp.accessToken, resp.refreshToken)
      const usuarioAtual = await usuariosAPI.buscarUsuarioAtual()
      setPacienteUuid(usuarioAtual.uuid)
      setEtapa(4)
    } catch (err: any) {
      setErro(mensagemErro(err, MENSAGENS.erro.credenciaisInvalidas))
    } finally {
      setCarregandoAuth(false)
    }
  }

  const handleCadastro = async (e: React.FormEvent) => {
    e.preventDefault()
    setErro('')
    setCarregandoAuth(true)
    try {
      const resp = await usuariosAPI.cadastrarPaciente({
        nomeCompleto: cadastroForm.nomeCompleto.trim(),
        telefone: cadastroForm.telefone.trim(),
      })
      tokenService.setTokens(resp.accessToken, resp.refreshToken)
      const usuarioAtual = await usuariosAPI.buscarUsuarioAtual()
      setPacienteUuid(usuarioAtual.uuid)
      setEtapa(4)
    } catch (err: any) {
      setErro(mensagemErro(err, MENSAGENS.erro.criarCadastro))
    } finally {
      setCarregandoAuth(false)
    }
  }

  /* ── confirmar agendamento ── */
  const handleConfirmar = async () => {
    if (!profissional || !slot || !pacienteUuid || !tipoProcedimento || !localAtendimento || pacienteAcamado === null) return
    setErro('')
    setCarregandoConfirmar(true)
    try {
      const criado = await agendamentoAPI.criarAgendamento({
        pacienteUuid,
        profissionalUuid: profissional.uuid,
        inicioEm: `${data}T${slot}:00`,
        fimEm: `${data}T${adicionarHora(slot)}:00`,
        tipo: 'AVALIACAO',
        tipoProcedimento,
        localAtendimento: localAtendimento!,
        pacienteAcamado: pacienteAcamado!,
      })
      setAgendamento(criado)
      setEtapa('sucesso')
    } catch (err: any) {
      setErro(mensagemErro(err, MENSAGENS.erro.confirmarAgendamento))
    } finally {
      setCarregandoConfirmar(false)
    }
  }

  /* ── reiniciar ── */
  const reiniciar = () => {
    setProfissional(null)
    setData(amanha())
    setSlot(null)
    setTipoProcedimento(null)
    setLocalAtendimento(null)
    setPacienteAcamado(null)
    setPacienteUuid(null)
    setAgendamento(null)
    setBloqueios([])
    setErro('')
    setLoginForm({ username: '', senha: '' })
    setCadastroForm({
      nomeCompleto: '',
      telefone: '',
    })
    setEtapa(1)
  }

  /* ── render ── */
  return (
    <div className="pagina-agendar">
      {/* Header */}
      <header className="agendar-header">
        <div className="agendar-header-logo">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
            />
          </svg>
        </div>
        <div className="agendar-header-texto">
          <h1>ProntuDigital</h1>
          <p>Agendamento de consultas</p>
        </div>
      </header>

      <main className="agendar-main">
        {/* Indicador de etapas */}
        {etapa !== 'sucesso' && (
          <div className="indicador-etapas">
            {ETAPAS.map(e => {
              const num = e.num as number
              const etapaAtual = etapa as number
              const cls =
                num < etapaAtual
                  ? 'concluida'
                  : num === etapaAtual
                    ? 'ativa'
                    : ''
              return (
                <div key={e.num} className={`etapa-item ${cls}`}>
                  <div className="etapa-circulo">
                    {num < etapaAtual ? (
                      <svg
                        width="12"
                        height="12"
                        viewBox="0 0 12 12"
                        fill="none"
                      >
                        <path
                          d="M2 6l3 3 5-6"
                          stroke="currentColor"
                          strokeWidth="2"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </svg>
                    ) : (
                      e.num
                    )}
                  </div>
                  <span className="etapa-rotulo">{e.rotulo}</span>
                </div>
              )
            })}
          </div>
        )}

        {/* ═══ ETAPA 1: Profissional ═══ */}
        {etapa === 1 && (
          <div className="agendar-card">
            <div className="card-cabecalho">
              <h2>Escolha o profissional</h2>
              <p>Selecione o enfermeiro(a) para a sua consulta</p>
            </div>

            <div className="card-corpo">
              {erro && <div className="alerta-erro">⚠ {erro}</div>}

              {carregandoProf ? (
                <div className="agendar-loading">
                  <div className="spinner-sm" />
                  Carregando profissionais…
                </div>
              ) : profissionais.length === 0 ? (
                <div className="agendar-loading">
                  Nenhum profissional disponível no momento.
                </div>
              ) : (
                <div className="profissionais-lista">
                  {profissionais.map(p => (
                    <button
                      key={p.uuid}
                      className={`profissional-card${profissional?.uuid === p.uuid ? ' selecionado' : ''}`}
                      onClick={() => setProfissional(p)}
                    >
                      <div className="profissional-avatar">
                        {iniciais(p.nomeCompleto)}
                      </div>
                      <div className="profissional-info">
                        <div className="profissional-nome">
                          {p.nomeCompleto}
                        </div>
                        <div className="profissional-subtitulo">Enfermagem</div>
                      </div>
                      <div className="profissional-check">
                        {profissional?.uuid === p.uuid && (
                          <svg
                            width="10"
                            height="10"
                            viewBox="0 0 10 10"
                            fill="none"
                          >
                            <path
                              d="M2 5l2 2 4-4"
                              stroke="white"
                              strokeWidth="1.8"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                            />
                          </svg>
                        )}
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className="card-rodape">
              <span />
              <button
                className="btn-continuar"
                disabled={!profissional}
                onClick={irParaEtapa2}
              >
                Continuar
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                >
                  <path d="M5 12h14M12 5l7 7-7 7" />
                </svg>
              </button>
            </div>
          </div>
        )}

        {/* ═══ ETAPA 2: Horário ═══ */}
        {etapa === 2 && (
          <div className="agendar-card">
            <div className="card-cabecalho">
              <h2>Escolha o horário</h2>
              <p>com {profissional?.nomeCompleto}</p>
            </div>

            <div className="card-corpo">
              {erro && <div className="alerta-erro">⚠ {erro}</div>}

              <div className="slots-header">
                <div className="campo" style={{ flex: 1, marginBottom: 0 }}>
                  <label>Data</label>
                  <input
                    type="date"
                    className="slots-data-input"
                    value={data}
                    min={amanha()}
                    onChange={e => handleDataChange(e.target.value)}
                  />
                </div>
                <div className="slots-legenda">
                  <span className="legenda-item">
                    <span className="legenda-cor legenda-cor--disponivel" />
                    Livre
                  </span>
                  <span className="legenda-item">
                    <span className="legenda-cor legenda-cor--selecionado" />
                    Selecionado
                  </span>
                  <span className="legenda-item">
                    <span className="legenda-cor legenda-cor--bloqueado" />
                    Bloqueado
                  </span>
                </div>
              </div>

              {carregandoSlots ? (
                <div className="agendar-loading">
                  <div className="spinner-sm" />
                  Verificando disponibilidade…
                </div>
              ) : (
                <div className="slots-grid">
                  {SLOTS_HORA.map(s => {
                    const bloqueado = slotBloqueado(s, data, bloqueios)
                    return (
                      <button
                        key={s}
                        className={`slot-btn${slot === s ? ' selecionado' : ''}`}
                        disabled={bloqueado}
                        onClick={() => setSlot(s)}
                        title={
                          bloqueado ? 'Horário indisponível' : `Agendar às ${s}`
                        }
                      >
                        {s}
                      </button>
                    )
                  })}
                </div>
              )}

              <div className="tipo-procedimento-secao">
                <p className="tipo-procedimento-titulo">Tipo de procedimento</p>
                <div className="tipo-procedimento-opcoes">
                  {(['PODIATRIA', 'TRATAMENTO_FERIDAS'] as TipoProcedimento[]).map(tp => (
                    <button
                      key={tp}
                      className={`tipo-procedimento-btn${tipoProcedimento === tp ? ' selecionado' : ''}`}
                      onClick={() => setTipoProcedimento(tp)}
                    >
                      {ROTULO_TIPO_PROCEDIMENTO[tp]}
                    </button>
                  ))}
                </div>

                <p className="tipo-procedimento-titulo" style={{ marginTop: '1rem' }}>Local de atendimento</p>
                <div className="tipo-procedimento-opcoes">
                  {(['CLINICA', 'RESIDENCIAL'] as LocalAtendimento[]).map(l => (
                    <button
                      key={l}
                      className={`tipo-procedimento-btn${localAtendimento === l ? ' selecionado' : ''}`}
                      onClick={() => setLocalAtendimento(l)}
                    >
                      {ROTULO_LOCAL_ATENDIMENTO[l]}
                    </button>
                  ))}
                </div>

                <p className="tipo-procedimento-titulo" style={{ marginTop: '1rem' }}>Paciente acamado?</p>
                <div className="tipo-procedimento-opcoes">
                  {([true, false] as boolean[]).map(v => (
                    <button
                      key={String(v)}
                      className={`tipo-procedimento-btn${pacienteAcamado === v ? ' selecionado' : ''}`}
                      onClick={() => setPacienteAcamado(v)}
                    >
                      {v ? 'Sim' : 'Não'}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="card-rodape">
              <button
                className="btn-voltar"
                onClick={() => {
                  setEtapa(1)
                  setErro('')
                }}
              >
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                >
                  <path d="M19 12H5M12 5l-7 7 7 7" />
                </svg>
                Voltar
              </button>
              <button
                className="btn-continuar"
                disabled={!slot || !tipoProcedimento || !localAtendimento || pacienteAcamado === null}
                onClick={() => {
                  setEtapa(3)
                  setErro('')
                }}
              >
                Continuar
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                >
                  <path d="M5 12h14M12 5l7 7-7 7" />
                </svg>
              </button>
            </div>
          </div>
        )}

        {/* ═══ ETAPA 3: Identificação ═══ */}
        {etapa === 3 && (
          <div className="agendar-card">
            <div className="card-cabecalho">
              <h2>Identificação</h2>
              <p>Acesse ou crie sua conta para continuar</p>
            </div>

            <div className="card-corpo">
              <div className="auth-tabs">
                <button
                  className={`auth-tab${modoAuth === 'login' ? ' ativo' : ''}`}
                  onClick={() => {
                    setModoAuth('login')
                    setErro('')
                  }}
                >
                  Já tenho conta
                </button>
                <button
                  className={`auth-tab${modoAuth === 'cadastro' ? ' ativo' : ''}`}
                  onClick={() => {
                    setModoAuth('cadastro')
                    setErro('')
                  }}
                >
                  Primeiro acesso
                </button>
              </div>

              {erro && <div className="alerta-erro">⚠ {erro}</div>}

              {/* ── Login ── */}
              {modoAuth === 'login' && (
                <form id="form-auth" onSubmit={handleLogin}>
                  <div className="campo">
                    <label>Usuário</label>
                    <input
                      type="text"
                      className="campo-input"
                      placeholder="seu.usuario"
                      value={loginForm.username}
                      required
                      autoComplete="username"
                      onChange={e =>
                        setLoginForm(f => ({ ...f, username: e.target.value }))
                      }
                    />
                  </div>
                  <div className="campo">
                    <label>Senha</label>
                    <input
                      type="password"
                      className="campo-input"
                      placeholder="••••••••"
                      value={loginForm.senha}
                      required
                      autoComplete="current-password"
                      onChange={e =>
                        setLoginForm(f => ({ ...f, senha: e.target.value }))
                      }
                    />
                  </div>
                </form>
              )}

              {/* ── Cadastro ── */}
              {modoAuth === 'cadastro' && (
                <form id="form-auth" onSubmit={handleCadastro}>
                  <div className="campo">
                    <label>Nome completo</label>
                    <input
                      type="text"
                      className="campo-input"
                      placeholder="Maria da Silva"
                      value={cadastroForm.nomeCompleto}
                      required
                      onChange={e =>
                        setCadastroForm(f => ({
                          ...f,
                          nomeCompleto: e.target.value,
                        }))
                      }
                    />
                  </div>
                  <div className="campo">
                    <label>Telefone</label>
                    <input
                      type="tel"
                      className="campo-input"
                      placeholder="(99) 9 9999-9999"
                      value={cadastroForm.telefone}
                      required
                      autoComplete="tel"
                      onChange={e =>
                        setCadastroForm(f => ({
                          ...f,
                          telefone: mascaraTelefone(e.target.value),
                        }))
                      }
                    />
                  </div>
                  <p className="auth-separador">
                    Apenas nome e telefone são necessários para agendar. Para
                    acessar o sistema depois, configure suas credenciais no
                    perfil.
                  </p>
                </form>
              )}
            </div>

            <div className="card-rodape">
              <button
                className="btn-voltar"
                onClick={() => {
                  setEtapa(2)
                  setErro('')
                }}
              >
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                >
                  <path d="M19 12H5M12 5l-7 7 7 7" />
                </svg>
                Voltar
              </button>
              <button
                type="submit"
                form="form-auth"
                className="btn-continuar"
                disabled={carregandoAuth}
              >
                {carregandoAuth ? (
                  <>
                    <div
                      className="spinner-sm"
                      style={{
                        borderTopColor: 'white',
                        width: '1rem',
                        height: '1rem',
                      }}
                    />
                    Aguarde…
                  </>
                ) : modoAuth === 'login' ? (
                  'Entrar e continuar'
                ) : (
                  'Criar conta'
                )}
              </button>
            </div>
          </div>
        )}

        {/* ═══ ETAPA 4: Confirmação ═══ */}
        {etapa === 4 && profissional && slot && (
          <div className="agendar-card">
            <div className="card-cabecalho">
              <h2>Confirmar agendamento</h2>
              <p>Revise os detalhes antes de confirmar</p>
            </div>

            <div className="card-corpo">
              {erro && <div className="alerta-erro">⚠ {erro}</div>}

              <div className="resumo-confirmacao">
                <div className="resumo-cabecalho">
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <rect x="3" y="4" width="18" height="18" rx="2" />
                    <line x1="16" y1="2" x2="16" y2="6" />
                    <line x1="8" y1="2" x2="8" y2="6" />
                    <line x1="3" y1="10" x2="21" y2="10" />
                  </svg>
                  <span>Resumo da consulta</span>
                </div>
                <div className="resumo-corpo">
                  <div className="resumo-linha">
                    <div className="resumo-icone-wrapper">
                      <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                      >
                        <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                      </svg>
                    </div>
                    <div className="resumo-linha-info">
                      <div className="resumo-linha-rotulo">Profissional</div>
                      <div className="resumo-linha-valor">
                        {profissional.nomeCompleto}
                      </div>
                    </div>
                  </div>
                  <div className="resumo-linha">
                    <div className="resumo-icone-wrapper">
                      <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                      >
                        <rect x="3" y="4" width="18" height="18" rx="2" />
                        <line x1="16" y1="2" x2="16" y2="6" />
                        <line x1="8" y1="2" x2="8" y2="6" />
                        <line x1="3" y1="10" x2="21" y2="10" />
                      </svg>
                    </div>
                    <div className="resumo-linha-info">
                      <div className="resumo-linha-rotulo">Data</div>
                      <div className="resumo-linha-valor">
                        {formatarDataExibicao(data)}
                      </div>
                    </div>
                  </div>
                  <div className="resumo-linha">
                    <div className="resumo-icone-wrapper">
                      <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                      >
                        <circle cx="12" cy="12" r="10" />
                        <polyline points="12 6 12 12 16 14" />
                      </svg>
                    </div>
                    <div className="resumo-linha-info">
                      <div className="resumo-linha-rotulo">Horário</div>
                      <div className="resumo-linha-valor">
                        {slot} – {adicionarHora(slot)} (1 hora)
                      </div>
                    </div>
                  </div>
                  <div className="resumo-linha">
                    <div className="resumo-icone-wrapper">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                    </div>
                    <div className="resumo-linha-info">
                      <div className="resumo-linha-rotulo">Procedimento</div>
                      <div className="resumo-linha-valor">
                        {tipoProcedimento ? ROTULO_TIPO_PROCEDIMENTO[tipoProcedimento] : '—'}
                      </div>
                    </div>
                  </div>
                  <div className="resumo-linha">
                    <div className="resumo-icone-wrapper">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                      </svg>
                    </div>
                    <div className="resumo-linha-info">
                      <div className="resumo-linha-rotulo">Local</div>
                      <div className="resumo-linha-valor">
                        {localAtendimento ? ROTULO_LOCAL_ATENDIMENTO[localAtendimento] : '—'}
                      </div>
                    </div>
                  </div>
                  <div className="resumo-linha">
                    <div className="resumo-icone-wrapper">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                      </svg>
                    </div>
                    <div className="resumo-linha-info">
                      <div className="resumo-linha-rotulo">Paciente acamado</div>
                      <div className="resumo-linha-valor">
                        {pacienteAcamado === null ? '—' : pacienteAcamado ? 'Sim' : 'Não'}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="card-rodape">
              <button
                className="btn-voltar"
                onClick={() => {
                  setEtapa(3)
                  setErro('')
                }}
              >
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                >
                  <path d="M19 12H5M12 5l-7 7 7 7" />
                </svg>
                Voltar
              </button>
              <button
                className="btn-continuar"
                disabled={carregandoConfirmar}
                onClick={handleConfirmar}
              >
                {carregandoConfirmar ? (
                  <>
                    <div
                      className="spinner-sm"
                      style={{
                        borderTopColor: 'white',
                        width: '1rem',
                        height: '1rem',
                      }}
                    />
                    Confirmando…
                  </>
                ) : (
                  <>
                    Confirmar agendamento
                    <svg
                      width="14"
                      height="14"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2.5"
                    >
                      <path d="M5 12h14M12 5l7 7-7 7" />
                    </svg>
                  </>
                )}
              </button>
            </div>
          </div>
        )}

        {/* ═══ SUCESSO ═══ */}
        {etapa === 'sucesso' && agendamento && profissional && slot && (
          <div className="agendar-card">
            <div className="sucesso-container">
              <div className="sucesso-icone">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M5 13l4 4L19 7"
                  />
                </svg>
              </div>
              <h2>Agendamento confirmado!</h2>
              <p>
                Sua consulta foi agendada com sucesso. Compareça no horário
                combinado.
              </p>

              <div className="sucesso-detalhes">
                <div className="sucesso-detalhe-linha">
                  <span>Profissional</span>
                  <span>{profissional.nomeCompleto}</span>
                </div>
                <div className="sucesso-detalhe-linha">
                  <span>Data</span>
                  <span>
                    {new Date(data + 'T00:00:00').toLocaleDateString('pt-BR')}
                  </span>
                </div>
                <div className="sucesso-detalhe-linha">
                  <span>Horário</span>
                  <span>
                    {slot} – {adicionarHora(slot)}
                  </span>
                </div>
                <div className="sucesso-detalhe-linha">
                  <span>Procedimento</span>
                  <span>{tipoProcedimento ? ROTULO_TIPO_PROCEDIMENTO[tipoProcedimento] : '—'}</span>
                </div>
                <div className="sucesso-detalhe-linha">
                  <span>Local</span>
                  <span>{localAtendimento ? ROTULO_LOCAL_ATENDIMENTO[localAtendimento] : '—'}</span>
                </div>
                <div className="sucesso-detalhe-linha">
                  <span>Paciente acamado</span>
                  <span>{pacienteAcamado === null ? '—' : pacienteAcamado ? 'Sim' : 'Não'}</span>
                </div>
              </div>

              <button className="btn-novo-agendamento" onClick={reiniciar}>
                Fazer outro agendamento
              </button>
            </div>
          </div>
        )}
      </main>
    </div>
  )
}
