'use client'

import { useState, useEffect, useCallback } from 'react'
import {
  Plus,
  Ban,
  AlertCircle,
  Umbrella,
  Wrench,
  Clock,
  Trash2,
  Calendar,
  AlertTriangle,
  RefreshCw,
  Repeat,
} from 'lucide-react'
import { RotaProtegida } from '../../components/RotaProtegida'
import { useAuth } from '../../contexts/AuthContext'
import { useNotificacao } from '../../contexts/ToastContext'
import { usuariosAPI } from '../../lib/usuario.service'
import { bloqueioAPI } from '../../lib/bloqueio.service'
import Layout from '@/components/Layout/Layout'
import {
  BloqueioHorario,
  BloqueioHorarioRequisicao,
  BloqueioRecorrente,
  BloqueioRecorrenteRequisicao,
  TipoBloqueio,
  DiaSemana,
  TIPO_BLOQUEIO_LABELS,
  DIAS_SEMANA,
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
    data: d.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }),
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

const formatarHora = (hora: string) => hora.slice(0, 5)

/* ── tipos para o formulário ── */

interface FormState {
  inicioEm: string
  fimEm: string
  tipo: TipoBloqueio
  motivo: string
}

interface FormRecorrenteState {
  diasSemana: DiaSemana[]
  horaInicio: string
  horaFim: string
  tipo: TipoBloqueio
  motivo: string
}

type ModoModal = 'UNICO' | 'RECORRENTE'

const TIPOS: { valor: TipoBloqueio; icone: React.ElementType }[] = [
  { valor: 'INDISPONIVEL', icone: Ban },
  { valor: 'URGENCIA', icone: AlertCircle },
  { valor: 'FOLGA', icone: Umbrella },
  { valor: 'MANUTENCAO', icone: Wrench },
]

type FiltroRapido = 'SEMANA' | 'MES' | '30DIAS' | 'PERSONALIZADO'

/* ── componente principal ── */

export default function BloqueiosPage() {
  const { usuario } = useAuth()
  const { exibirNotificacao } = useNotificacao()

  const [profissionalUuid, setProfissionalUuid] = useState<string | null>(null)
  const [bloqueios, setBloqueios] = useState<BloqueioHorario[]>([])
  const [recorrentes, setRecorrentes] = useState<BloqueioRecorrente[]>([])
  const [loading, setLoading] = useState(true)
  const [salvando, setSalvando] = useState(false)
  const [modalAberto, setModalAberto] = useState(false)
  const [modoModal, setModoModal] = useState<ModoModal>('UNICO')
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

  const [formRecorrente, setFormRecorrente] = useState<FormRecorrenteState>({
    diasSemana: [],
    horaInicio: '08:00',
    horaFim: '18:00',
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
        .catch(() =>
          exibirNotificacao(
            'Não foi possível identificar o profissional.',
            'error',
            5000,
          ),
        )
    }
  }, [usuario])

  /* ── busca bloqueios ── */
  const fetchBloqueios = useCallback(async () => {
    if (!profissionalUuid) return
    setLoading(true)
    try {
      const [dados, regrasDados] = await Promise.all([
        bloqueioAPI.listar(profissionalUuid, dataInicio, dataFim),
        bloqueioAPI.listarRecorrentes(profissionalUuid),
      ])
      setBloqueios(dados)
      setRecorrentes(regrasDados)
    } catch {
      exibirNotificacao(
        'Erro ao carregar horários indisponíveis.',
        'error',
        5000,
      )
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
    if (filtro === 'SEMANA') {
      setDataInicio(inicioDaSemana())
      setDataFim(fimDaSemana())
    }
    if (filtro === 'MES') {
      setDataInicio(inicioDoMes())
      setDataFim(fimDoMes())
    }
    if (filtro === '30DIAS') {
      setDataInicio(hoje())
      setDataFim(proximos30())
    }
    if (filtro === 'PERSONALIZADO') setFiltroRapido('PERSONALIZADO')
  }

  /* ── criar bloqueio único ── */
  const handleSubmitUnico = async (e: React.FormEvent) => {
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
        inicioEm:
          form.inicioEm.length === 16 ? form.inicioEm + ':00' : form.inicioEm,
        fimEm: form.fimEm.length === 16 ? form.fimEm + ':00' : form.fimEm,
        tipo: form.tipo,
        motivo: form.motivo.trim() || undefined,
      }
      const criado = await bloqueioAPI.criar(requisicao)
      setBloqueios(prev =>
        [...prev, criado].sort(
          (a, b) =>
            new Date(a.inicioEm).getTime() - new Date(b.inicioEm).getTime(),
        ),
      )
      fecharModal()
      exibirNotificacao('Horário registrado com sucesso!', 'success', 4000)
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Erro ao registrar o horário.'
      setErroForm(msg)
    } finally {
      setSalvando(false)
    }
  }

  /* ── criar bloqueio recorrente ── */
  const handleSubmitRecorrente = async (e: React.FormEvent) => {
    e.preventDefault()
    setErroForm('')

    if (!profissionalUuid) {
      setErroForm('Não foi possível identificar o profissional.')
      return
    }
    if (formRecorrente.diasSemana.length === 0) {
      setErroForm('Selecione ao menos um dia da semana.')
      return
    }
    if (formRecorrente.horaFim <= formRecorrente.horaInicio) {
      setErroForm('O horário de término deve ser posterior ao de início.')
      return
    }

    setSalvando(true)
    try {
      const novasRegras: BloqueioRecorrente[] = []
      for (const dia of formRecorrente.diasSemana) {
        const requisicao: BloqueioRecorrenteRequisicao = {
          profissionalUuid,
          diaSemana: dia,
          horaInicio: formRecorrente.horaInicio + ':00',
          horaFim: formRecorrente.horaFim + ':00',
          tipo: formRecorrente.tipo,
          motivo: formRecorrente.motivo.trim() || undefined,
        }
        const criada = await bloqueioAPI.criarRecorrente(requisicao)
        novasRegras.push(criada)
      }
      setRecorrentes(prev => [...prev, ...novasRegras])
      fecharModal()
      exibirNotificacao(
        novasRegras.length === 1
          ? 'Regra recorrente criada com sucesso!'
          : `${novasRegras.length} regras recorrentes criadas!`,
        'success',
        4000,
      )
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || 'Erro ao criar regra recorrente.'
      setErroForm(msg)
    } finally {
      setSalvando(false)
    }
  }

  /* ── excluir bloqueio único ── */
  const handleExcluir = async (bloqueio: BloqueioHorario) => {
    if (bloqueio.profissionalUuid !== profissionalUuid) return

    if (!confirm('Tem certeza que deseja remover este bloqueio?')) return
    try {
      await bloqueioAPI.remover(bloqueio.id)
      setBloqueios(prev => prev.filter(b => b.uuid !== bloqueio.uuid))
      exibirNotificacao('Bloqueio removido com sucesso!', 'success', 4000)
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        'Erro ao remover o bloqueio.'
      exibirNotificacao(String(msg), 'error', 6000)
    }
  }

  /* ── excluir regra recorrente ── */
  const handleExcluirRecorrente = async (regra: BloqueioRecorrente) => {
    if (
      !confirm(
        `Remover regra recorrente de ${DIAS_SEMANA.find(d => d.valor === regra.diaSemana)?.label}? Todos os bloqueios futuros desse dia serão cancelados.`,
      )
    )
      return
    try {
      await bloqueioAPI.removerRecorrente(regra.id)
      setRecorrentes(prev => prev.filter(r => r.uuid !== regra.uuid))
      exibirNotificacao('Regra recorrente removida!', 'success', 4000)
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || 'Erro ao remover a regra.'
      exibirNotificacao(String(msg), 'error', 6000)
    }
  }

  const abrirModal = () => {
    const agora = new Date()
    agora.setMinutes(0, 0, 0)
    const depois = new Date(agora)
    depois.setHours(depois.getHours() + 1)
    const toLocal = (d: Date) =>
      `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    setForm({
      inicioEm: toLocal(agora),
      fimEm: toLocal(depois),
      tipo: 'INDISPONIVEL',
      motivo: '',
    })
    setFormRecorrente({
      diasSemana: [],
      horaInicio: '08:00',
      horaFim: '18:00',
      tipo: 'INDISPONIVEL',
      motivo: '',
    })
    setModoModal('UNICO')
    setErroForm('')
    setModalAberto(true)
  }

  const fecharModal = () => {
    setModalAberto(false)
    setErroForm('')
  }

  const toggleDia = (dia: DiaSemana) => {
    setFormRecorrente(f => ({
      ...f,
      diasSemana: f.diasSemana.includes(dia)
        ? f.diasSemana.filter(d => d !== dia)
        : [...f.diasSemana, dia],
    }))
  }

  /* ── lista filtrada ── */
  const bloqueiosFiltrados = (
    filtroTipo
      ? bloqueios.filter(b => b.tipo === filtroTipo)
      : bloqueios
  ).filter(b => !b.recorrenteUuid)

  /* ── contagens por tipo ── */
  const contagens = (tipo: TipoBloqueio) =>
    bloqueios.filter(b => b.tipo === tipo && !b.recorrenteUuid).length

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
              <Plus size={16} strokeWidth={2.5} />
              Registrar Bloqueio
            </button>
          </div>

          {/* ── Resumo por tipo ── */}
          <div className="bloqueios-resumo">
            {TIPOS.map(({ valor, icone: Icone }) => (
              <div
                key={valor}
                className={`resumo-card resumo-card--${valor}`}
                onClick={() =>
                  setFiltroTipo(prev => (prev === valor ? '' : valor))
                }
                style={{ cursor: 'pointer' }}
                title={`Filtrar por ${TIPO_BLOQUEIO_LABELS[valor]}`}
              >
                <div className="resumo-icone">
                  <Icone size={20} strokeWidth={1.75} />
                </div>
                <div className="resumo-info">
                  <span className="resumo-numero">{contagens(valor)}</span>
                  <span className="resumo-rotulo">
                    {TIPO_BLOQUEIO_LABELS[valor]}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {/* ── Regras Recorrentes ── */}
          {recorrentes.length > 0 && (
            <div className="recorrentes-secao">
              <div className="recorrentes-titulo">
                <Repeat size={15} strokeWidth={2} />
                Regras recorrentes
              </div>
              <div className="recorrentes-lista">
                {recorrentes.map(regra => {
                  const diaInfo = DIAS_SEMANA.find(d => d.valor === regra.diaSemana)
                  const tipoInfo = TIPOS.find(t => t.valor === regra.tipo)
                  const TipoIcone = tipoInfo?.icone
                  return (
                    <div
                      key={regra.uuid}
                      className={`recorrente-item recorrente-item--${regra.tipo}`}
                    >
                      <span className={`bloqueio-tipo-badge badge--${regra.tipo}`}>
                        {TipoIcone && <TipoIcone size={12} strokeWidth={2} />}
                        {TIPO_BLOQUEIO_LABELS[regra.tipo]}
                      </span>
                      <span className="recorrente-dia">{diaInfo?.label}</span>
                      <span className="recorrente-horario">
                        <Clock size={11} strokeWidth={2} />
                        {formatarHora(regra.horaInicio)} → {formatarHora(regra.horaFim)}
                      </span>
                      {regra.motivo && (
                        <span className="recorrente-motivo">"{regra.motivo}"</span>
                      )}
                      <button
                        className="btn-excluir-bloqueio"
                        onClick={() => handleExcluirRecorrente(regra)}
                        title="Remover regra recorrente"
                      >
                        <Trash2 size={15} strokeWidth={1.75} />
                      </button>
                    </div>
                  )
                })}
              </div>
            </div>
          )}

          {/* ── Filtros ── */}
          <div className="bloqueios-filtros">
            <div className="filtro-grupo">
              <label>De</label>
              <input
                type="date"
                className="filtro-input"
                value={dataInicio}
                onChange={e => {
                  setDataInicio(e.target.value)
                  setFiltroRapido('PERSONALIZADO')
                }}
              />
            </div>
            <div className="filtro-grupo">
              <label>Até</label>
              <input
                type="date"
                className="filtro-input"
                value={dataFim}
                onChange={e => {
                  setDataFim(e.target.value)
                  setFiltroRapido('PERSONALIZADO')
                }}
              />
            </div>
            <div className="filtros-rapidos">
              {(
                [
                  ['SEMANA', 'Esta semana'],
                  ['MES', 'Este mês'],
                  ['30DIAS', 'Próx. 30 dias'],
                ] as [FiltroRapido, string][]
              ).map(([val, label]) => (
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
                onChange={e =>
                  setFiltroTipo(e.target.value as TipoBloqueio | '')
                }
              >
                <option value="">Todos</option>
                {TIPOS.map(({ valor }) => (
                  <option key={valor} value={valor}>
                    {TIPO_BLOQUEIO_LABELS[valor]}
                  </option>
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
              <div className="bloqueios-vazio-icone">
                <Calendar size={32} strokeWidth={1.5} />
              </div>
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
                const duracao = calcularDuracao(
                  bloqueio.inicioEm,
                  bloqueio.fimEm,
                )
                const tipoInfo = TIPOS.find(t => t.valor === bloqueio.tipo)

                const TipoIcone = tipoInfo?.icone
                return (
                  <div
                    key={bloqueio.id}
                    className={`bloqueio-card bloqueio-card--${bloqueio.tipo}`}
                  >
                    <span
                      className={`bloqueio-tipo-badge badge--${bloqueio.tipo}`}
                    >
                      {TipoIcone && <TipoIcone size={12} strokeWidth={2} />}
                      {TIPO_BLOQUEIO_LABELS[bloqueio.tipo]}
                    </span>

                    <div className="bloqueio-info">
                      <div className="bloqueio-datas">
                        <span className="bloqueio-data-inicio">
                          {ini.data} {ini.hora}
                        </span>
                        <span className="bloqueio-seta">→</span>
                        <span className="bloqueio-data-fim">
                          {ini.data === fim.data
                            ? fim.hora
                            : `${fim.data} ${fim.hora}`}
                        </span>
                      </div>
                      <div className="bloqueio-motivo">
                        {bloqueio.motivo ? (
                          `"${bloqueio.motivo}"`
                        ) : (
                          <span className="bloqueio-motivo-vazio">
                            Sem motivo informado
                          </span>
                        )}
                      </div>
                      <div className="bloqueio-duracao">
                        <Clock size={11} strokeWidth={2} />
                        {duracao}
                      </div>
                    </div>

                    {bloqueio.profissionalUuid === profissionalUuid && (
                      <button
                        className="btn-excluir-bloqueio"
                        onClick={() => handleExcluir(bloqueio)}
                        title="Remover bloqueio"
                      >
                        <Trash2 size={16} strokeWidth={1.75} />
                      </button>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </div>

        {/* ── Modal ── */}
        {modalAberto && (
          <div
            className="modal-overlay"
            onClick={e => e.target === e.currentTarget && fecharModal()}
          >
            <div className="modal-conteudo">
              <div className="modal-cabecalho">
                <h2>
                  <Ban size={18} strokeWidth={2} /> Registrar Bloqueio
                </h2>
                <button
                  className="modal-fechar"
                  onClick={fecharModal}
                  aria-label="Fechar"
                >
                  ✕
                </button>
              </div>

              {/* ── Toggle único / recorrente ── */}
              <div className="modo-toggle">
                <button
                  type="button"
                  className={`modo-btn${modoModal === 'UNICO' ? ' ativo' : ''}`}
                  onClick={() => { setModoModal('UNICO'); setErroForm('') }}
                >
                  <Calendar size={14} strokeWidth={2} />
                  Data específica
                </button>
                <button
                  type="button"
                  className={`modo-btn${modoModal === 'RECORRENTE' ? ' ativo' : ''}`}
                  onClick={() => { setModoModal('RECORRENTE'); setErroForm('') }}
                >
                  <RefreshCw size={14} strokeWidth={2} />
                  Dia da semana
                </button>
              </div>

              {/* ── Formulário único ── */}
              {modoModal === 'UNICO' && (
                <form onSubmit={handleSubmitUnico}>
                  <div className="modal-corpo">
                    <div className="campo-linha">
                      <div className="campo">
                        <label>Início</label>
                        <input
                          type="datetime-local"
                          className="campo-input"
                          value={form.inicioEm}
                          onChange={e =>
                            setForm(f => ({ ...f, inicioEm: e.target.value }))
                          }
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
                          onChange={e =>
                            setForm(f => ({ ...f, fimEm: e.target.value }))
                          }
                          required
                        />
                      </div>
                    </div>

                    <div className="campo">
                      <label>Tipo de bloqueio</label>
                      <div className="tipo-opcoes">
                        {TIPOS.map(({ valor, icone: Icone }) => (
                          <div
                            key={valor}
                            className={`tipo-opcao tipo-opcao--${valor}`}
                          >
                            <input
                              type="radio"
                              id={`tipo-${valor}`}
                              name="tipo"
                              value={valor}
                              checked={form.tipo === valor}
                              onChange={() =>
                                setForm(f => ({ ...f, tipo: valor }))
                              }
                            />
                            <label
                              htmlFor={`tipo-${valor}`}
                              className="tipo-opcao-label"
                            >
                              <span className={`tipo-dot tipo-dot--${valor}`} />
                              <Icone size={14} strokeWidth={2} />
                              {TIPO_BLOQUEIO_LABELS[valor]}
                            </label>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="campo">
                      <label>
                        Motivo <span>(opcional)</span>
                      </label>
                      <textarea
                        className="campo-textarea"
                        placeholder="Descreva o motivo da indisponibilidade…"
                        value={form.motivo}
                        onChange={e =>
                          setForm(f => ({ ...f, motivo: e.target.value }))
                        }
                        maxLength={255}
                      />
                    </div>

                    {erroForm && (
                      <p className="campo-erro">
                        <AlertTriangle size={13} strokeWidth={2} />
                        {erroForm}
                      </p>
                    )}
                  </div>

                  <div className="modal-rodape">
                    <button
                      type="button"
                      className="btn-cancelar"
                      onClick={fecharModal}
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      className="btn-salvar"
                      disabled={salvando}
                    >
                      {salvando ? 'Salvando…' : 'Registrar Bloqueio'}
                    </button>
                  </div>
                </form>
              )}

              {/* ── Formulário recorrente ── */}
              {modoModal === 'RECORRENTE' && (
                <form onSubmit={handleSubmitRecorrente}>
                  <div className="modal-corpo">
                    <div className="campo">
                      <label>Dias da semana</label>
                      <div className="dias-semana-selector">
                        {DIAS_SEMANA.map(({ valor, abrev, label }) => (
                          <button
                            key={valor}
                            type="button"
                            className={`dia-btn${formRecorrente.diasSemana.includes(valor) ? ' selecionado' : ''}`}
                            onClick={() => toggleDia(valor)}
                            title={label}
                          >
                            {abrev}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="campo-linha">
                      <div className="campo">
                        <label>Horário início</label>
                        <input
                          type="time"
                          className="campo-input"
                          value={formRecorrente.horaInicio}
                          onChange={e =>
                            setFormRecorrente(f => ({
                              ...f,
                              horaInicio: e.target.value,
                            }))
                          }
                          required
                        />
                      </div>
                      <div className="campo">
                        <label>Horário fim</label>
                        <input
                          type="time"
                          className="campo-input"
                          value={formRecorrente.horaFim}
                          min={formRecorrente.horaInicio}
                          onChange={e =>
                            setFormRecorrente(f => ({
                              ...f,
                              horaFim: e.target.value,
                            }))
                          }
                          required
                        />
                      </div>
                    </div>

                    <div className="campo">
                      <label>Tipo de bloqueio</label>
                      <div className="tipo-opcoes">
                        {TIPOS.map(({ valor, icone: Icone }) => (
                          <div
                            key={valor}
                            className={`tipo-opcao tipo-opcao--${valor}`}
                          >
                            <input
                              type="radio"
                              id={`rec-tipo-${valor}`}
                              name="rec-tipo"
                              value={valor}
                              checked={formRecorrente.tipo === valor}
                              onChange={() =>
                                setFormRecorrente(f => ({ ...f, tipo: valor }))
                              }
                            />
                            <label
                              htmlFor={`rec-tipo-${valor}`}
                              className="tipo-opcao-label"
                            >
                              <span className={`tipo-dot tipo-dot--${valor}`} />
                              <Icone size={14} strokeWidth={2} />
                              {TIPO_BLOQUEIO_LABELS[valor]}
                            </label>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="campo">
                      <label>
                        Motivo <span>(opcional)</span>
                      </label>
                      <textarea
                        className="campo-textarea"
                        placeholder="Ex.: Fechado aos sábados…"
                        value={formRecorrente.motivo}
                        onChange={e =>
                          setFormRecorrente(f => ({
                            ...f,
                            motivo: e.target.value,
                          }))
                        }
                        maxLength={255}
                      />
                    </div>

                    {erroForm && (
                      <p className="campo-erro">
                        <AlertTriangle size={13} strokeWidth={2} />
                        {erroForm}
                      </p>
                    )}
                  </div>

                  <div className="modal-rodape">
                    <button
                      type="button"
                      className="btn-cancelar"
                      onClick={fecharModal}
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      className="btn-salvar"
                      disabled={salvando}
                    >
                      {salvando
                        ? 'Salvando…'
                        : formRecorrente.diasSemana.length > 1
                          ? `Criar ${formRecorrente.diasSemana.length} regras`
                          : 'Criar Regra Recorrente'}
                    </button>
                  </div>
                </form>
              )}
            </div>
          </div>
        )}
      </RotaProtegida>
    </Layout>
  )
}
