'use client'

import { useState, useEffect } from 'react'
import { Agendamento } from '@/tipos/agendamento'
import { agendamentoAPI } from '@/lib/agendamento.service'
import { useNotificacao } from '@/contexts/ToastContext'
import Modal from '@/components/Modal'
import './procedimento.css'

// ── Helpers ──────────────────────────────────────────────────────────────────

const formatarHora = (iso: string) =>
  new Date(iso).toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
  })

const formatarData = (iso: string) =>
  new Date(iso).toLocaleDateString('pt-BR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })

const formatarDataCurta = (iso: string) =>
  new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })

const calcularDuracao = (inicio: string, fim: string): string => {
  const diff = Math.floor(
    (new Date(fim).getTime() - new Date(inicio).getTime()) / 60000,
  )
  if (diff < 60) return `${diff} min`
  const h = Math.floor(diff / 60)
  const m = diff % 60
  return m > 0 ? `${h}h${m}min` : `${h}h`
}

const ROTULO_STATUS: Record<string, string> = {
  AGENDADO: 'Agendado',
  CONFIRMADO: 'Confirmado',
  REALIZADO: 'Realizado',
  CANCELADO: 'Cancelado',
  REMARCADO: 'Remarcado',
  NAO_COMPARECEU: 'Não compareceu',
}

const ROTULO_TIPO: Record<string, string> = {
  AVALIACAO: 'Avaliação',
  TRATAMENTO: 'Tratamento',
}

// ── Sub-componentes ───────────────────────────────────────────────────────────

function TipoBadge({ tipo }: { tipo: string }) {
  const classe =
    tipo === 'AVALIACAO' ? 'proc-tipo-avaliacao' : 'proc-tipo-tratamento'
  return (
    <span className={`proc-tipo-badge ${classe}`}>
      {tipo === 'AVALIACAO' ? '⚕' : '💊'} {ROTULO_TIPO[tipo] ?? tipo}
    </span>
  )
}

function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`proc-status proc-status--${status}`}>
      {ROTULO_STATUS[status] ?? status}
    </span>
  )
}

function HistoricoItem({
  item,
  isAtual,
  index,
}: {
  item: Agendamento
  isAtual: boolean
  index: number
}) {
  const isRealizado = item.status === 'REALIZADO'

  const dotClasse = isAtual
    ? 'proc-hist-dot--atual'
    : isRealizado
      ? 'proc-hist-dot--realizado'
      : item.tipo === 'AVALIACAO'
        ? 'proc-hist-dot--avaliacao'
        : 'proc-hist-dot--tratamento'

  return (
    <div
      className={`proc-historico-item${isAtual ? ' proc-historico-item--atual' : ''}`}
    >
      <div className={`proc-hist-dot ${dotClasse}`}>
        {isRealizado && !isAtual ? '✓' : index + 1}
      </div>
      <div className="proc-hist-info">
        <div className="proc-hist-esq">
          <span className="proc-hist-data">
            {formatarDataCurta(item.inicioEm)}
          </span>
          <span className="proc-hist-tipo">
            {ROTULO_TIPO[item.tipo] ?? item.tipo}
            {item.tipo === 'TRATAMENTO' && !isAtual && ` #${index}`}
          </span>
        </div>
        {isAtual ? (
          <span className="proc-hist-atual-label">Esta consulta</span>
        ) : (
          <StatusBadge status={item.status} />
        )}
      </div>
    </div>
  )
}

// ── Modal principal ───────────────────────────────────────────────────────────

interface ModalProcedimentoProps {
  agendamento: Agendamento
  onClose: () => void
  onConcluido: (atualizado: Agendamento) => void
}

export default function ModalProcedimento({
  agendamento,
  onClose,
  onConcluido,
}: ModalProcedimentoProps) {
  const { exibirNotificacao } = useNotificacao()

  const [finalizando, setFinalizando] = useState(false)

  const [historico, setHistorico] = useState<Agendamento[]>([])
  const [carregandoHist, setCarregandoHist] = useState(false)

  const jaRealizado = agendamento.status === 'REALIZADO'
  const ehTratamento = agendamento.tipo === 'TRATAMENTO'

  // Busca histórico quando é um tratamento
  useEffect(() => {
    if (!ehTratamento || !agendamento.avaliacaoId) return
    setCarregandoHist(true)
    agendamentoAPI
      .getTratamentosPorAvaliacao(agendamento.avaliacaoId)
      .then(dados => setHistorico(dados))
      .catch(() => {})
      .finally(() => setCarregandoHist(false))
  }, [ehTratamento, agendamento.avaliacaoId])

  const finalizarConsulta = async () => {
    setFinalizando(true)
    try {
      await agendamentoAPI.concluirAgendamento(agendamento.id)
      exibirNotificacao('Consulta finalizada com sucesso!', 'success', 4000)
      onConcluido({ ...agendamento, status: 'REALIZADO' })
      onClose()
    } catch {
      exibirNotificacao('Erro ao finalizar consulta.', 'error', 5000)
    } finally {
      setFinalizando(false)
    }
  }

  // Monta a lista do histórico com avaliação + tratamentos ordenados
  const listaHistorico = [...historico].sort(
    (a, b) => new Date(a.inicioEm).getTime() - new Date(b.inicioEm).getTime(),
  )

  const rodape = (
    <div className="proc-footer">
      <button className="proc-btn-cancelar" onClick={onClose}>
        Fechar
      </button>

      {jaRealizado ? (
        <button
          className="proc-btn-finalizar proc-btn-finalizar--realizado"
          disabled
        >
          ✓ Consulta realizada
        </button>
      ) : (
        <button
          className="proc-btn-finalizar"
          onClick={finalizarConsulta}
          disabled={finalizando}
        >
          {finalizando ? 'Finalizando…' : '✓ Finalizar consulta'}
        </button>
      )}
    </div>
  )

  return (
    <Modal
      titulo={agendamento.nomePaciente ?? 'Paciente'}
      onClose={onClose}
      tamanho="md"
      rodape={rodape}
    >
      <div className="proc-body">
        {/* Tipo + nome */}
        <div className="proc-header-meta">
          <TipoBadge tipo={agendamento.tipo} />
          <span className="proc-paciente-nome">{agendamento.nomePaciente}</span>
        </div>

        {/* Informações da consulta */}
        <div className="proc-secao">
          <div className="proc-secao-titulo">📋 Informações da consulta</div>
          <div className="proc-info-grid">
            <div className="proc-info-item">
              <span className="proc-info-icone">📅</span>
              <div className="proc-info-conteudo">
                <span className="proc-info-rotulo">Data</span>
                <span
                  className="proc-info-valor"
                  style={{ textTransform: 'capitalize' }}
                >
                  {formatarData(agendamento.inicioEm)}
                </span>
              </div>
            </div>
            <div className="proc-info-item">
              <span className="proc-info-icone">⏰</span>
              <div className="proc-info-conteudo">
                <span className="proc-info-rotulo">Horário</span>
                <span className="proc-info-valor">
                  {formatarHora(agendamento.inicioEm)} –{' '}
                  {formatarHora(agendamento.fimEm)}{' '}
                  <span
                    style={{
                      fontWeight: 400,
                      color: 'var(--color-text-muted)',
                      fontSize: '0.8rem',
                    }}
                  >
                    ({calcularDuracao(agendamento.inicioEm, agendamento.fimEm)})
                  </span>
                </span>
              </div>
            </div>
            {agendamento.nomeProfissional && (
              <div className="proc-info-item">
                <span className="proc-info-icone">👤</span>
                <div className="proc-info-conteudo">
                  <span className="proc-info-rotulo">Profissional</span>
                  <span className="proc-info-valor">
                    {agendamento.nomeProfissional}
                  </span>
                </div>
              </div>
            )}
            <div className="proc-info-item">
              <span className="proc-info-icone">●</span>
              <div className="proc-info-conteudo">
                <span className="proc-info-rotulo">Status</span>
                <StatusBadge status={agendamento.status} />
              </div>
            </div>
          </div>
        </div>

        {/* Histórico (apenas tratamentos) */}
        {ehTratamento && (
          <div className="proc-secao">
            <div className="proc-secao-titulo">🕐 Histórico da série</div>
            {carregandoHist ? (
              <div className="proc-hist-loading">
                <div className="proc-hist-spinner" />
                Carregando histórico…
              </div>
            ) : listaHistorico.length === 0 ? (
              <div className="proc-hist-loading">
                Nenhum registro anterior encontrado.
              </div>
            ) : (
              <div className="proc-historico-lista">
                {listaHistorico.map((item, idx) => (
                  <HistoricoItem
                    key={item.id}
                    item={item}
                    isAtual={item.id === agendamento.id}
                    index={idx}
                  />
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </Modal>
  )
}
