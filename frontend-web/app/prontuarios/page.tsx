'use client'

import { useState, useEffect, useCallback } from 'react'
import Link from 'next/link'
import { FileText, Search, X, Calendar, ArrowRight, Clipboard, Syringe } from 'lucide-react'
import { RotaProtegida } from '../../components/RotaProtegida'
import { useAuth } from '../../contexts/AuthContext'
import { agendamentoAPI } from '../../lib/agendamento.service'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import {
  formatarDataCurta,
  obterCorAvatar,
  obterIniciais,
} from '@/lib/paciente-ui'
import Layout from '@/components/Layout/Layout'
import { PacienteAgendamentosDTO } from '@/tipos/agendamento'
import './prontuarios.css'

const ITENS_POR_PAGINA = 10

/**
 * Não existe endpoint de listagem de prontuários — o backend expõe o
 * prontuário sempre por paciente (`/api/prontuario/pacientes/{uuid}/...`).
 * A lista vem então dos pacientes com atendimento, o mesmo endpoint que
 * `/pacientes` usa, sem filtro de status: um prontuário existe independente
 * de o atendimento ter sido realizado ou cancelado.
 */
const SEM_FILTRO_STATUS = 'TODOS'

function gerarPaginas(total: number, atual: number): (number | string)[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const paginas: (number | string)[] = [1]
  if (atual > 3) paginas.push('...')
  for (let i = Math.max(2, atual - 1); i <= Math.min(total - 1, atual + 1); i++)
    paginas.push(i)
  if (atual < total - 2) paginas.push('...')
  paginas.push(total)
  return paginas
}

function LinhaProntuario({ paciente }: { paciente: PacienteAgendamentosDTO }) {
  const total = paciente.agendamentos.length
  const ultimo = paciente.agendamentos[0]

  // Determina o tipo do último atendimento
  const tipoUltimo = ultimo?.tipo === 'AVALIACAO' ? 'Avaliação' : 'Tratamento'
  const classeTipo = ultimo?.tipo === 'AVALIACAO' ? 'pront-lista-meta-tipo--avaliacao' : 'pront-lista-meta-tipo--tratamento'

  return (
    <Link
      href={`/pacientes/${paciente.pacienteUuid}/prontuario?nome=${encodeURIComponent(paciente.nomePaciente)}&de=prontuarios`}
      className="pront-lista-item"
    >
      <div
        className="pront-lista-avatar"
        style={{ background: obterCorAvatar(paciente.pacienteUuid) }}
      >
        {obterIniciais(paciente.nomePaciente)}
      </div>

      <div className="pront-lista-info">
        <span className="pront-lista-nome">{paciente.nomePaciente}</span>
        <div className="pront-lista-meta">
          <Calendar className="pront-lista-meta-icon" size={14} />
          <span>{total} atendimento{total !== 1 ? 's' : ''}</span>
          {ultimo && (
            <>
              <span>·</span>
              <span>{formatarDataCurta(ultimo.inicioEm)}</span>
              <span className={`pront-lista-meta-tipo ${classeTipo}`}>
                {tipoUltimo}
              </span>
            </>
          )}
        </div>
      </div>

      <span className="pront-lista-abrir">
        Abrir prontuário
        <ArrowRight className="pront-lista-abrir-icon" size={16} />
      </span>
    </Link>
  )
}

export default function ProntuariosPage() {
  const { usuario, temPerfil } = useAuth()
  const [pacientes, setPacientes] = useState<PacienteAgendamentosDTO[]>([])
  const [totalPaginas, setTotalPaginas] = useState(1)
  const [totalPacientes, setTotalPacientes] = useState(0)
  const [loading, setLoading] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [busca, setBusca] = useState('')
  const [paginaAtual, setPaginaAtual] = useState(1)

  const isAdmin = temPerfil('ROLE_ADMIN')
  const isProfissional = temPerfil('ROLE_PROFISSIONAL')
  const temPerfilNecessario = isAdmin || isProfissional

  useEffect(() => {
    setPaginaAtual(1)
  }, [busca])

  const buscarPacientes = useCallback(
    async (pagina: number, buscaAtual: string) => {
      try {
        setLoading(true)
        setErro(null)
        const data = await agendamentoAPI.listarPacientesComAgendamentos(
          buscaAtual,
          SEM_FILTRO_STATUS,
          pagina - 1,
          ITENS_POR_PAGINA,
        )
        setPacientes(data.content)
        setTotalPaginas(Math.max(1, data.totalPages))
        setTotalPacientes(data.totalElements)
      } catch (err) {
        setErro(mensagemErro(err, MENSAGENS.erro.carregarPacientes))
      } finally {
        setLoading(false)
      }
    },
    [],
  )

  useEffect(() => {
    if (!usuario || !temPerfilNecessario) return
    // Debounce só quando há texto: a primeira carga não deve esperar.
    const timer = setTimeout(
      () => buscarPacientes(paginaAtual, busca),
      busca ? 400 : 0,
    )
    return () => clearTimeout(timer)
  }, [usuario, temPerfilNecessario, paginaAtual, busca, buscarPacientes])

  const paginaValida = Math.min(paginaAtual, totalPaginas)

  if (!usuario || !temPerfilNecessario) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
      </div>
    )
  }

  return (
    <Layout perfil={isAdmin ? 'ROLE_ADMIN' : 'ROLE_PROFISSIONAL'}>
      <RotaProtegida perfisNecessarios={['ROLE_PROFISSIONAL', 'ROLE_ADMIN']}>
        <div className="pront-lista-page">
          <div className="pront-lista-header">
            <div className="pront-lista-titulo">
              <h1>Prontuários</h1>
              {!loading && !erro && (
                <span className="pront-lista-total">
                  {totalPacientes} paciente{totalPacientes !== 1 ? 's' : ''}
                </span>
              )}
            </div>
            <span className="pront-lista-periodo">Últimos 3 meses</span>
          </div>

          <div className="pront-lista-busca-wrapper">
            <Search size={16} className="pront-lista-busca-icon" />
            <input
              type="text"
              className="pront-lista-busca"
              placeholder="Buscar paciente..."
              value={busca}
              onChange={e => setBusca(e.target.value)}
            />
            {busca && (
              <button
                className="pront-lista-busca-limpar"
                onClick={() => setBusca('')}
                aria-label="Limpar busca"
              >
                <X size={14} />
              </button>
            )}
          </div>

          {loading && (
            <div className="pront-lista-estado">
              <div className="pront-lista-spinner" />
              <p>Carregando prontuários...</p>
            </div>
          )}

          {erro && !loading && (
            <div className="pront-lista-estado pront-lista-estado-erro">
              <p>{erro}</p>
              <button
                className="pront-lista-retry"
                onClick={() => buscarPacientes(paginaAtual, busca)}
              >
                Tentar novamente
              </button>
            </div>
          )}

          {!loading && !erro && pacientes.length === 0 && (
            <div className="pront-lista-estado">
              <FileText size={48} strokeWidth={1.5} color="#d1d5db" />
              <p>
                {busca
                  ? 'Nenhum paciente encontrado para esta busca.'
                  : 'Nenhum prontuário disponível — ainda não há atendimentos registrados nos últimos 3 meses.'}
              </p>
            </div>
          )}

          {!loading && !erro && pacientes.length > 0 && (
            <>
              <div className="pront-lista">
                {pacientes.map(p => (
                  <LinhaProntuario key={p.pacienteUuid} paciente={p} />
                ))}
              </div>

              {totalPaginas > 1 && (
                <div className="pront-paginacao">
                  <button
                    className="pront-paginacao-btn"
                    onClick={() => setPaginaAtual(p => Math.max(1, p - 1))}
                    disabled={paginaValida === 1}
                  >
                    ← Anterior
                  </button>
                  {gerarPaginas(totalPaginas, paginaValida).map((p, i) =>
                    typeof p === 'string' ? (
                      <span
                        key={`ellipsis-${i}`}
                        className="pront-paginacao-ellipsis"
                      >
                        …
                      </span>
                    ) : (
                      <button
                        key={`page-${p}`}
                        className={`pront-paginacao-btn pront-paginacao-num${paginaValida === p ? ' pront-paginacao-ativa' : ''}`}
                        onClick={() => setPaginaAtual(p)}
                      >
                        {p}
                      </button>
                    ),
                  )}
                  <button
                    className="pront-paginacao-btn"
                    onClick={() =>
                      setPaginaAtual(p => Math.min(totalPaginas, p + 1))
                    }
                    disabled={paginaValida === totalPaginas}
                  >
                    Próximo →
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </RotaProtegida>
    </Layout>
  )
}
