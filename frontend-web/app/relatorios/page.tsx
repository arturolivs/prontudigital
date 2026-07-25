'use client'

import { useCallback, useEffect, useState } from 'react'
import {
  BarChart3,
  CalendarCheck,
  CalendarX,
  Clock,
  Download,
  FileText,
  Percent,
  Sheet,
  UserX,
} from 'lucide-react'
import Layout from '@/components/Layout/Layout'
import { RotaProtegida } from '@/components/RotaProtegida'
import { useAuth } from '@/contexts/AuthContext'
import { useNotificacao } from '@/contexts/ToastContext'
import { relatorioAPI } from '@/lib/relatorio.service'
import { usuariosAPI } from '@/lib/usuario.service'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import { Usuario } from '@/tipos/autenticacao'
import {
  FormatoExportacao,
  RelatorioAtendimentos,
  RelatorioOcupacao,
} from '@/tipos/relatorio'
import { StatusAgendamento } from '@/tipos/StatusAgendamento'
import { TipoAgendamento } from '@/tipos/TipoAgendamento'
import { ROTULO_LOCAL_ATENDIMENTO } from '@/tipos/LocalAtendimento'
import './relatorios.css'

const ROTULO_STATUS: Record<StatusAgendamento, string> = {
  AGENDADO: 'Agendado',
  CONFIRMADO: 'Confirmado',
  CANCELADO: 'Cancelado',
  REMARCADO: 'Remarcado',
  REALIZADO: 'Realizado',
  NAO_COMPARECEU: 'Não compareceu',
}

const ROTULO_TIPO: Record<TipoAgendamento, string> = {
  AVALIACAO: 'Avaliação',
  TRATAMENTO: 'Tratamento',
}

const primeiroDiaDoMes = () => {
  const hoje = new Date()
  return new Date(hoje.getFullYear(), hoje.getMonth(), 1)
    .toISOString()
    .split('T')[0]
}

const hojeISO = () => new Date().toISOString().split('T')[0]

const formatarDataHora = (iso: string) =>
  new Date(iso).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })

const formatarDuracao = (minutos: number) => {
  if (minutos < 60) return `${minutos} min`
  const h = Math.floor(minutos / 60)
  const m = minutos % 60
  return m > 0 ? `${h}h${m}min` : `${h}h`
}

/** As taxas vêm nulas do backend quando não há base de comparação. */
const formatarPercentual = (valor?: number) =>
  valor === null || valor === undefined ? '—' : `${valor.toFixed(1)}%`

const formatarHoras = (valor?: number) =>
  valor === null || valor === undefined ? '—' : `${valor.toFixed(1)}h`

export default function RelatoriosPage() {
  const { temPerfil } = useAuth()
  const { exibirNotificacao } = useNotificacao()

  const isAdmin = temPerfil('ROLE_ADMIN')
  const perfilLayout = isAdmin ? 'ROLE_ADMIN' : 'ROLE_PROFISSIONAL'

  const [inicio, setInicio] = useState(primeiroDiaDoMes)
  const [fim, setFim] = useState(hojeISO)
  const [profissionalUuid, setProfissionalUuid] = useState('')
  const [status, setStatus] = useState<StatusAgendamento | ''>('')
  const [tipo, setTipo] = useState<TipoAgendamento | ''>('')

  const [profissionais, setProfissionais] = useState<Usuario[]>([])
  const [atendimentos, setAtendimentos] =
    useState<RelatorioAtendimentos | null>(null)
  const [ocupacao, setOcupacao] = useState<RelatorioOcupacao | null>(null)
  const [carregando, setCarregando] = useState(false)
  const [exportando, setExportando] = useState<string | null>(null)

  // Só o ADMIN escolhe o profissional; o profissional sempre vê a própria
  // agenda (o backend força o filtro).
  useEffect(() => {
    if (!isAdmin) return
    usuariosAPI
      .listarUsuarios(0, 200)
      .then(pagina =>
        setProfissionais(
          pagina.content.filter(u => u.perfis?.includes('PROFISSIONAL')),
        ),
      )
      .catch(() => setProfissionais([]))
  }, [isAdmin])

  const gerar = useCallback(async () => {
    if (!inicio || !fim) return
    if (fim < inicio) {
      exibirNotificacao(MENSAGENS.validacao.periodoInvertido, 'error')
      return
    }

    setCarregando(true)
    try {
      const [dadosAtendimentos, dadosOcupacao] = await Promise.all([
        relatorioAPI.atendimentos({
          inicio,
          fim,
          profissionalUuid: profissionalUuid || undefined,
          status: status || undefined,
          tipo: tipo || undefined,
        }),
        relatorioAPI.ocupacao(inicio, fim, profissionalUuid || undefined),
      ])
      setAtendimentos(dadosAtendimentos)
      setOcupacao(dadosOcupacao)
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.gerarRelatorio),
        'error',
        5000,
      )
    } finally {
      setCarregando(false)
    }
  }, [inicio, fim, profissionalUuid, status, tipo, exibirNotificacao])

  useEffect(() => {
    gerar()
    // Só na carga inicial — depois o usuário aciona pelo botão.
  }, [])

  // RF21 — o arquivo respeita os mesmos filtros da tela.
  const exportar = async (
    relatorio: 'atendimentos' | 'ocupacao',
    formato: FormatoExportacao,
  ) => {
    setExportando(`${relatorio}-${formato}`)
    try {
      if (relatorio === 'atendimentos') {
        await relatorioAPI.exportarAtendimentos(
          {
            inicio,
            fim,
            profissionalUuid: profissionalUuid || undefined,
            status: status || undefined,
            tipo: tipo || undefined,
          },
          formato,
        )
      } else {
        await relatorioAPI.exportarOcupacao(
          inicio,
          fim,
          formato,
          profissionalUuid || undefined,
        )
      }
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.exportarRelatorio),
        'error',
        5000,
      )
    } finally {
      setExportando(null)
    }
  }

  const indicadores = ocupacao
    ? [
        {
          rotulo: 'Comparecimento',
          valor: formatarPercentual(ocupacao.taxaComparecimento),
          icone: CalendarCheck,
          variante: 'ok',
        },
        {
          rotulo: 'Cancelamentos',
          valor: formatarPercentual(ocupacao.taxaCancelamento),
          icone: CalendarX,
          variante: 'alerta',
        },
        {
          rotulo: 'Faltas',
          valor: formatarPercentual(ocupacao.taxaAbsenteismo),
          icone: UserX,
          variante: 'alerta',
        },
        {
          rotulo: 'Ocupação da agenda',
          valor: formatarPercentual(ocupacao.taxaOcupacao),
          icone: Percent,
          variante: 'neutro',
        },
      ]
    : []

  return (
    <RotaProtegida perfisNecessarios={['ROLE_ADMIN', 'ROLE_PROFISSIONAL']}>
      <Layout perfil={perfilLayout}>
        <div className="rel-container">
          <header className="rel-cabecalho">
            <div className="rel-titulo">
              <BarChart3 size={22} strokeWidth={2} />
              <div>
                <h1>Relatórios</h1>
                <p>
                  Atendimentos e ocupação da agenda
                  {!isAdmin && ' — sua agenda'}
                </p>
              </div>
            </div>
          </header>

          {/* ── Filtros ── */}
          <section className="rel-filtros">
            <div className="rel-filtro">
              <label htmlFor="rel-inicio">De</label>
              <input
                id="rel-inicio"
                type="date"
                value={inicio}
                onChange={e => setInicio(e.target.value)}
              />
            </div>
            <div className="rel-filtro">
              <label htmlFor="rel-fim">Até</label>
              <input
                id="rel-fim"
                type="date"
                value={fim}
                onChange={e => setFim(e.target.value)}
              />
            </div>

            {isAdmin && (
              <div className="rel-filtro">
                <label htmlFor="rel-profissional">Profissional</label>
                <select
                  id="rel-profissional"
                  value={profissionalUuid}
                  onChange={e => setProfissionalUuid(e.target.value)}
                >
                  <option value="">Toda a clínica</option>
                  {profissionais.map(p => (
                    <option key={p.uuid} value={p.uuid}>
                      {p.nomeCompleto}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="rel-filtro">
              <label htmlFor="rel-status">Status</label>
              <select
                id="rel-status"
                value={status}
                onChange={e =>
                  setStatus(e.target.value as StatusAgendamento | '')
                }
              >
                <option value="">Todos</option>
                {(Object.keys(ROTULO_STATUS) as StatusAgendamento[]).map(s => (
                  <option key={s} value={s}>
                    {ROTULO_STATUS[s]}
                  </option>
                ))}
              </select>
            </div>

            <div className="rel-filtro">
              <label htmlFor="rel-tipo">Tipo</label>
              <select
                id="rel-tipo"
                value={tipo}
                onChange={e => setTipo(e.target.value as TipoAgendamento | '')}
              >
                <option value="">Todos</option>
                <option value="AVALIACAO">Avaliação</option>
                <option value="TRATAMENTO">Tratamento</option>
              </select>
            </div>

            <button
              className="rel-btn-gerar"
              onClick={gerar}
              disabled={carregando}
            >
              {carregando ? 'Gerando...' : 'Gerar'}
            </button>
          </section>

          {/* ── Exportação (RF21) ── */}
          <section className="rel-exportacao">
            <span className="rel-exportacao-rotulo">
              <Download size={14} strokeWidth={2} />
              Exportar
            </span>
            <button
              className="rel-btn-exportar"
              onClick={() => exportar('atendimentos', 'PDF')}
              disabled={exportando !== null}
            >
              <FileText size={14} strokeWidth={1.75} />
              Atendimentos (PDF)
            </button>
            <button
              className="rel-btn-exportar"
              onClick={() => exportar('atendimentos', 'XLSX')}
              disabled={exportando !== null}
            >
              <Sheet size={14} strokeWidth={1.75} />
              Atendimentos (Excel)
            </button>
            <button
              className="rel-btn-exportar"
              onClick={() => exportar('ocupacao', 'PDF')}
              disabled={exportando !== null}
            >
              <FileText size={14} strokeWidth={1.75} />
              Ocupação (PDF)
            </button>
            <button
              className="rel-btn-exportar"
              onClick={() => exportar('ocupacao', 'XLSX')}
              disabled={exportando !== null}
            >
              <Sheet size={14} strokeWidth={1.75} />
              Ocupação (Excel)
            </button>
            {exportando && (
              <span className="rel-exportando">Gerando arquivo...</span>
            )}
          </section>

          {/* ── Ocupação (RF20) ── */}
          {ocupacao && (
            <>
              <div className="rel-indicadores">
                {indicadores.map(ind => {
                  const Icone = ind.icone
                  return (
                    <div
                      key={ind.rotulo}
                      className={`rel-indicador rel-indicador--${ind.variante}`}
                    >
                      <div className="rel-indicador-icone">
                        <Icone size={18} strokeWidth={1.75} />
                      </div>
                      <div>
                        <span className="rel-indicador-valor">{ind.valor}</span>
                        <span className="rel-indicador-rotulo">
                          {ind.rotulo}
                        </span>
                      </div>
                    </div>
                  )
                })}
              </div>

              <section className="rel-resumo">
                <div className="rel-resumo-linha">
                  <span>Total de agendamentos</span>
                  <strong>{ocupacao.total}</strong>
                </div>
                <div className="rel-resumo-linha">
                  <span>Realizados</span>
                  <strong>{ocupacao.realizados}</strong>
                </div>
                <div className="rel-resumo-linha">
                  <span>Cancelados</span>
                  <strong>{ocupacao.cancelados}</strong>
                </div>
                <div className="rel-resumo-linha">
                  <span>Faltas</span>
                  <strong>{ocupacao.naoCompareceram}</strong>
                </div>
                <div className="rel-resumo-linha">
                  <span>Remarcados</span>
                  <strong>{ocupacao.remarcados}</strong>
                </div>
                <div className="rel-resumo-linha">
                  <span>Em aberto</span>
                  <strong>{ocupacao.emAberto}</strong>
                </div>
                <div className="rel-resumo-linha">
                  <span>
                    <Clock size={13} strokeWidth={2} /> Horas ocupadas
                  </span>
                  <strong>{formatarHoras(ocupacao.horasAgendadas)}</strong>
                </div>
                <div className="rel-resumo-linha">
                  <span>
                    <Clock size={13} strokeWidth={2} /> Horas de expediente
                  </span>
                  <strong>{formatarHoras(ocupacao.horasDisponiveis)}</strong>
                </div>
              </section>

              {ocupacao.horasDisponiveis === null ||
              ocupacao.horasDisponiveis === undefined ? (
                <p className="rel-aviso">
                  A taxa de ocupação exige um profissional selecionado que tenha
                  horários de trabalho cadastrados na tela de
                  indisponibilidades.
                </p>
              ) : null}
            </>
          )}

          {/* ── Atendimentos (RF19) ── */}
          {atendimentos && (
            <section className="rel-tabela-secao">
              <div className="rel-tabela-cabecalho">
                <h2>
                  Atendimentos
                  <span className="rel-contagem">{atendimentos.total}</span>
                </h2>
                {atendimentos.nomeProfissional && (
                  <span className="rel-escopo">
                    {atendimentos.nomeProfissional}
                  </span>
                )}
              </div>

              {atendimentos.itens.length === 0 ? (
                <p className="rel-vazio">
                  Nenhum atendimento no período com os filtros escolhidos.
                </p>
              ) : (
                <div className="rel-tabela-scroll">
                  <table className="rel-tabela">
                    <thead>
                      <tr>
                        <th>Data</th>
                        <th>Duração</th>
                        <th>Paciente</th>
                        <th>Profissional</th>
                        <th>Tipo</th>
                        <th>Procedimento</th>
                        <th>Local</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {atendimentos.itens.map(item => (
                        <tr key={item.agendamentoId}>
                          <td>{formatarDataHora(item.inicioEm)}</td>
                          <td>{formatarDuracao(item.duracaoMinutos)}</td>
                          <td>{item.nomePaciente}</td>
                          <td>{item.nomeProfissional}</td>
                          <td>{item.tipo ? ROTULO_TIPO[item.tipo] : '—'}</td>
                          <td>{item.procedimentoNome ?? '—'}</td>
                          <td>
                            {item.localAtendimento
                              ? ROTULO_LOCAL_ATENDIMENTO[item.localAtendimento]
                              : '—'}
                          </td>
                          <td>
                            <span
                              className={`rel-badge rel-badge--${item.status}`}
                            >
                              {ROTULO_STATUS[item.status]}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}
        </div>
      </Layout>
    </RotaProtegida>
  )
}
