'use client'

import { useState, useEffect, useCallback } from 'react'
import { RotaProtegida } from '../../components/RotaProtegida'
import { useAuth } from '../../contexts/AuthContext'
import { useNotificacao } from '../../contexts/ToastContext'
import { usuariosAPI } from '../../lib/usuario.service'
import { bloqueioAPI } from '../../lib/bloqueio.service'
import Layout from '@/components/Layout/Layout'
import {
  BloqueioHorario,
  BloqueioHorarioRequisicao,
  TipoBloqueio,
  TIPO_BLOQUEIO_LABELS,
} from '@/tipos/bloqueio'
import './bloqueios.css'

/* ── helpers de data ── */

const hoje = () => new Date().toISOString().split('T')[0]

const inicioDaSemana = () => {
  const d = new Date()
  d.setDate(d.getDate() - d.getDay() + 1)
  return d.toISOString().split('T')[0]
}

const fimDaSemana = () => {
  const d = new Date()
  d.setDate(d.getDate() - d.getDay() + 7)
  return d.toISOString().split('T')[0]
}

const inicioDoMes = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}

const fimDoMes = () => {
  const d = new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0)
  return d.toISOString().split('T')[0]
}

const proximos30 = () => {
  const d = new Date()
  d.setDate(d.getDate() + 30)
  return d.toISOString().split('T')[0]
}

const formatarDataHora = (iso: string) => {
  const d = new Date(iso)
  return {
    data: d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }),
    hora: d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
  }
}

const calcularDuracao = (inicio: string, fim: string) => {
  const ms = new Date(fim).getTime() - new Date(inicio).getTime()
  const horas = Math.floor(ms / 3600000)
  const minutos = Math.floor((ms % 3600000) / 60000)
  if (horas === 0) return `${minutos}min`
  if (minutos === 0) return `${horas}h`
  return `${horas}h ${minutos}min`
}

/* ── tipos para o formulário ── */

interface FormState {
  inicioEm: string
  fimEm: string
  tipo: TipoBloqueio
  motivo: string
}

const TIPOS: { valor: TipoBloqueio; emoji: string }[] = [
  { valor: 'INDISPONIVEL', emoji: '🚫' },
  { valor: 'URGENCIA', emoji: '🚨' },
  { valor: 'FOLGA', emoji: '🌴' },
  { valor: 'MANUTENCAO', emoji: '🔧' },
]

type FiltroRapido = 'SEMANA' | 'MES' | '30DIAS' | 'PERSONALIZADO'

/* ── componente principal ── */

export default function BloqueiosPage() {
  const { usuario } = useAuth()
  const { exibirNotificacao } = useNotificacao()

  const [profissionalUuid, setProfissionalUuid] = useState<string | null>(null)
  const [bloqueios, setBloqueios] = useState<BloqueioHorario[]>([])
  const [loading, setLoading] = useState(true)
  const [salvando, setSalvando] = useState(false)
  const [modalAberto, setModalAberto] = useState(false)
  const [filtroRapido, setFiltroRapido] = useState<FiltroRapido>('MES')
  const [filtroTipo, setFiltroTipo] = useState<TipoBloqueio | ''>('')
  const [dataInicio, setDataInicio] = useState(inicioDoMes)
  const [dataFim, setDataFim] = useState(fimDoMes)
  const [erroForm, setErroForm] = useState('')

  const [form, setForm] = useState<FormState>({
    inicioEm: '',
    fimEm: '',
    tipo: 'INDISPONIVEL',
    motivo: '',
  })

  /* ── resolve UUID do profissional ── */
  useEffect(() => {
    if (usuario?.uuid) {
      setProfissionalUuid(usuario.uuid)
    } else {
      usuariosAPI
        .buscarUsuarioAtual()
        .then(u => setProfissionalUuid(u.uuid))
        .catch(() => exibirNotificacao('Não foi possível identificar o profissional.', 'error', 5000))
    }
  }, [usuario])

  /* ── busca bloqueios ── */
  const fetchBloqueios = useCallback(async () => {
    if (!profissionalUuid) return
    setLoading(true)
    try {
      const dados = await bloqueioAPI.listar(profissionalUuid, dataInicio, dataFim)
      setBloqueios(dados)
    } catch {
      exibirNotificacao('Erro ao carregar horários indisponíveis.', 'error', 5000)
    } finally {
      setLoading(false)
    }
  }, [profissionalUuid, dataInicio, dataFim])

  useEffect(() => {
    fetchBloqueios()
  }, [fetchBloqueios])

  /* ── filtros rápidos ── */
  const aplicarFiltroRapido = (filtro: FiltroRapido) => {
    setFiltroRapido(filtro)
    if (filtro === 'SEMANA') { setDataInicio(inicioDaSemana()); setDataFim(fimDaSemana()) }
    if (filtro === 'MES')    { setDataInicio(inicioDoMes());    setDataFim(fimDoMes()) }
    if (filtro === '30DIAS') { setDataInicio(hoje());           setDataFim(proximos30()) }
    if (filtro === 'PERSONALIZADO') setFiltroRapido('PERSONALIZADO')
  }

  /* ── criar bloqueio ── */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErroForm('')

    if (!profissionalUuid) {
      setErroForm('Não foi possível identificar o profissional.')
      return
    }

    const inicio = new Date(form.inicioEm)
    const fim = new Date(form.fimEm)

    if (fim <= inicio) {
      setErroForm('O horário de término deve ser posterior ao de início.')
      return
    }

    setSalvando(true)
    try {
      const requisicao: BloqueioHorarioRequisicao = {
        profissionalUuid,
        inicioEm: form.inicioEm.length === 16 ? form.inicioEm + ':00' : form.inicioEm,
        fimEm:    form.fimEm.length  === 16 ? form.fimEm  + ':00' : form.fimEm,
        tipo:   form.tipo,
        motivo: form.motivo.trim() || undefined,
      }
      const criado = await bloqueioAPI.criar(requisicao)
      setBloqueios(prev => [...prev, criado].sort(
        (a, b) => new Date(a.inicioEm).getTime() - new Date(b.inicioEm).getTime()
      ))
      fecharModal()
      exibirNotificacao('Horário registrado com sucesso!', 'success', 4000)
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Erro ao registrar o horário.'
      setErroForm(msg)
    } finally {
      setSalvando(false)
    }
  }

  /* ── excluir bloqueio ── */
  const handleExcluir = async (id: number) => {
    if (!confirm('Tem certeza que deseja remover este bloqueio?')) return
    try {
      await bloqueioAPI.remover(id)
      setBloqueios(prev => prev.filter(b => b.id !== id))
      exibirNotificacao('Bloqueio removido com sucesso!', 'success', 4000)
    } catch {
      exibirNotificacao('Erro ao remover o bloqueio.', 'error', 5000)
    }
  }

  const abrirModal = () => {
    const agora = new Date()
    agora.setMinutes(0, 0, 0)
    const depois = new Date(agora)
    depois.setHours(depois.getHours() + 1)
    const toLocal = (d: Date) =>
      `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    setForm({ inicioEm: toLocal(agora), fimEm: toLocal(depois), tipo: 'INDISPONIVEL', motivo: '' })
    setErroForm('')
    setModalAberto(true)
  }

  const fecharModal = () => {
    setModalAberto(false)
    setErroForm('')
  }

  /* ── lista filtrada por tipo ── */
  const bloqueiosFiltrados = filtroTipo
    ? bloqueios.filter(b => b.tipo === filtroTipo)
    : bloqueios

  /* ── contagens por tipo ── */
  const contagens = (tipo: TipoBloqueio) => bloqueios.filter(b => b.tipo === tipo).length

  return (
    <Layout perfil="ROLE_PROFISSIONAL">
      <RotaProtegida perfisNecessarios={['ROLE_PROFISSIONAL']}>
        <div className="bloqueios-page">

          {/* ── Header ── */}
          <div className="bloqueios-header">
            <div className="bloqueios-header-texto">
              <h1>Horários Indisponíveis</h1>
              <p>Registre e gerencie seus períodos de indisponibilidade</p>
            </div>
            <button className="btn-novo-bloqueio" onClick={abrirModal}>
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clipRule="evenodd" />
              </svg>
              Registrar Bloqueio
            </button>
          </div>

          {/* ── Resumo por tipo ── */}
          <div className="bloqueios-resumo">
            {TIPOS.map(({ valor, emoji }) => (
              <div
                key={valor}
                className={`resumo-card resumo-card--${valor}`}
                onClick={() => setFiltroTipo(prev => prev === valor ? '' : valor)}
                style={{ cursor: 'pointer' }}
                title={`Filtrar por ${TIPO_BLOQUEIO_LABELS[valor]}`}
              >
                <div className="resumo-icone">{emoji}</div>
                <div className="resumo-info">
                  <span className="resumo-numero">{contagens(valor)}</span>
                  <span className="resumo-rotulo">{TIPO_BLOQUEIO_LABELS[valor]}</span>
                </div>
              </div>
            ))}
          </div>

          {/* ── Filtros ── */}
          <div className="bloqueios-filtros">
            <div className="filtro-grupo">
              <label>De</label>
              <input
                type="date"
                className="filtro-input"
                value={dataInicio}
                onChange={e => { setDataInicio(e.target.value); setFiltroRapido('PERSONALIZADO') }}
              />
            </div>
            <div className="filtro-grupo">
              <label>Até</label>
              <input
                type="date"
                className="filtro-input"
                value={dataFim}
                onChange={e => { setDataFim(e.target.value); setFiltroRapido('PERSONALIZADO') }}
              />
            </div>
            <div className="filtros-rapidos">
              {([['SEMANA', 'Esta semana'], ['MES', 'Este mês'], ['30DIAS', 'Próx. 30 dias']] as [FiltroRapido, string][]).map(([val, label]) => (
                <button
                  key={val}
                  className={`btn-filtro-rapido${filtroRapido === val ? ' ativo' : ''}`}
                  onClick={() => aplicarFiltroRapido(val)}
                >
                  {label}
                </button>
              ))}
            </div>
            <div className="filtro-grupo" style={{ minWidth: 160 }}>
              <label>Tipo</label>
              <select
                className="filtro-tipo-select filtro-input"
                value={filtroTipo}
                onChange={e => setFiltroTipo(e.target.value as TipoBloqueio | '')}
              >
                <option value="">Todos</option>
                {TIPOS.map(({ valor }) => (
                  <option key={valor} value={valor}>{TIPO_BLOQUEIO_LABELS[valor]}</option>
                ))}
              </select>
            </div>
          </div>

          {/* ── Lista ── */}
          {loading ? (
            <div className="bloqueios-loading">
              <div className="spinner" />
              Carregando…
            </div>
          ) : bloqueiosFiltrados.length === 0 ? (
            <div className="bloqueios-vazio">
              <div className="bloqueios-vazio-icone">📅</div>
              <h3>Nenhum bloqueio encontrado</h3>
              <p>
                {filtroTipo
                  ? `Não há bloqueios do tipo "${TIPO_BLOQUEIO_LABELS[filtroTipo]}" no período selecionado.`
                  : 'Você não tem horários indisponíveis registrados para o período selecionado.'}
              </p>
            </div>
          ) : (
            <div className="bloqueios-lista">
              {bloqueiosFiltrados.map(bloqueio => {
                const ini = formatarDataHora(bloqueio.inicioEm)
                const fim = formatarDataHora(bloqueio.fimEm)
                const duracao = calcularDuracao(bloqueio.inicioEm, bloqueio.fimEm)
                const tipoInfo = TIPOS.find(t => t.valor === bloqueio.tipo)

                return (
                  <div key={bloqueio.id} className={`bloqueio-card bloqueio-card--${bloqueio.tipo}`}>
                    <span className={`bloqueio-tipo-badge badge--${bloqueio.tipo}`}>
                      {tipoInfo?.emoji} {TIPO_BLOQUEIO_LABELS[bloqueio.tipo]}
                    </span>

                    <div className="bloqueio-info">
                      <div className="bloqueio-datas">
                        <span className="bloqueio-data-inicio">
                          {ini.data} {ini.hora}
                        </span>
                        <span className="bloqueio-seta">→</span>
                        <span className="bloqueio-data-fim">
                          {ini.data === fim.data ? fim.hora : `${fim.data} ${fim.hora}`}
                        </span>
                      </div>
                      <div className="bloqueio-motivo">
                        {bloqueio.motivo ? (
                          `"${bloqueio.motivo}"`
                        ) : (
                          <span className="bloqueio-motivo-vazio">Sem motivo informado</span>
                        )}
                      </div>
                      <div className="bloqueio-duracao">
                        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                        </svg>
                        {duracao}
                      </div>
                    </div>

                    <button
                      className="btn-excluir-bloqueio"
                      onClick={() => handleExcluir(bloqueio.id)}
                      title="Remover bloqueio"
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="3 6 5 6 21 6"/>
                        <path d="M19 6l-1 14H6L5 6"/>
                        <path d="M10 11v6M14 11v6"/>
                        <path d="M9 6V4h6v2"/>
                      </svg>
                    </button>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        {/* ── Modal ── */}
        {modalAberto && (
          <div className="modal-overlay" onClick={e => e.target === e.currentTarget && fecharModal()}>
            <div className="modal-conteudo">
              <div className="modal-cabecalho">
                <h2>🚫 Registrar Bloqueio</h2>
                <button className="modal-fechar" onClick={fecharModal} aria-label="Fechar">✕</button>
              </div>

              <form onSubmit={handleSubmit}>
                <div className="modal-corpo">

                  {/* Período */}
                  <div className="campo-linha">
                    <div className="campo">
                      <label>Início</label>
                      <input
                        type="datetime-local"
                        className="campo-input"
                        value={form.inicioEm}
                        onChange={e => setForm(f => ({ ...f, inicioEm: e.target.value }))}
                        required
                      />
                    </div>
                    <div className="campo">
                      <label>Término</label>
                      <input
                        type="datetime-local"
                        className="campo-input"
                        value={form.fimEm}
                        min={form.inicioEm}
                        onChange={e => setForm(f => ({ ...f, fimEm: e.target.value }))}
                        required
                      />
                    </div>
                  </div>

                  {/* Tipo */}
                  <div className="campo">
                    <label>Tipo de bloqueio</label>
                    <div className="tipo-opcoes">
                      {TIPOS.map(({ valor, emoji }) => (
                        <div key={valor} className={`tipo-opcao tipo-opcao--${valor}`}>
                          <input
                            type="radio"
                            id={`tipo-${valor}`}
                            name="tipo"
                            value={valor}
                            checked={form.tipo === valor}
                            onChange={() => setForm(f => ({ ...f, tipo: valor }))}
                          />
                          <label htmlFor={`tipo-${valor}`} className="tipo-opcao-label">
                            <span className={`tipo-dot tipo-dot--${valor}`} />
                            {emoji} {TIPO_BLOQUEIO_LABELS[valor]}
                          </label>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Motivo */}
                  <div className="campo">
                    <label>Motivo <span>(opcional)</span></label>
                    <textarea
                      className="campo-textarea"
                      placeholder="Descreva o motivo da indisponibilidade…"
                      value={form.motivo}
                      onChange={e => setForm(f => ({ ...f, motivo: e.target.value }))}
                      maxLength={255}
                    />
                  </div>

                  {/* Erro */}
                  {erroForm && (
                    <p className="campo-erro">⚠ {erroForm}</p>
                  )}
                </div>

                <div className="modal-rodape">
                  <button type="button" className="btn-cancelar" onClick={fecharModal}>
                    Cancelar
                  </button>
                  <button type="submit" className="btn-salvar" disabled={salvando}>
                    {salvando ? 'Salvando…' : 'Registrar Bloqueio'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </RotaProtegida>
    </Layout>
  )
}
