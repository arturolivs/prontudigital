'use client'

import { useState, useEffect, useCallback, useMemo } from 'react'
import { RotaProtegida } from '@/components/RotaProtegida'
import { useAuth } from '@/contexts/AuthContext'
import { agendamentoAPI } from '@/lib/careSession'
import LayoutPaciente from '@/components/LayoutPaciente'
import { Agendamento } from '@/tipos/CareSession'
import './minha-agenda.css'

type Aba = 'proximos' | 'historico'

const ROTULO_STATUS: Record<string, string> = {
  AGENDADO: 'Agendado',
  CONFIRMADO: 'Confirmado',
  CANCELADO: 'Cancelado',
  REMARCADO: 'Remarcado',
  REALIZADO: 'Realizado',
  NAO_COMPARECEU: 'Não compareceu',
}

const CLASSE_STATUS: Record<string, string> = {
  AGENDADO: 'ma-status-agendado',
  CONFIRMADO: 'ma-status-confirmado',
  CANCELADO: 'ma-status-cancelado',
  REMARCADO: 'ma-status-remarcado',
  REALIZADO: 'ma-status-realizado',
  NAO_COMPARECEU: 'ma-status-nao-compareceu',
}

const ICONE_STATUS: Record<string, string> = {
  AGENDADO: '🕐',
  CONFIRMADO: '✅',
  CANCELADO: '❌',
  REMARCADO: '🔄',
  REALIZADO: '✔️',
  NAO_COMPARECEU: '⚠️',
}

const formatarHora = (iso: string): string =>
  new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })

const formatarDataCompleta = (iso: string): string =>
  new Date(iso).toLocaleDateString('pt-BR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })

const formatarDataCurta = (iso: string): string =>
  new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })

const calcularDuracao = (inicio: string, fim: string): string => {
  const diff = Math.floor((new Date(fim).getTime() - new Date(inicio).getTime()) / 60000)
  if (diff < 60) return `${diff} min`
  const h = Math.floor(diff / 60)
  const m = diff % 60
  return m > 0 ? `${h}h${m}min` : `${h}h`
}

const isProximo = (a: Agendamento): boolean => {
  const agora = new Date()
  const inicio = new Date(a.inicioEm)
  return (
    inicio >= agora &&
    (a.status === 'AGENDADO' || a.status === 'CONFIRMADO' || a.status === 'REMARCADO')
  )
}

const CardAgendamento = ({ agendamento, destaque }: { agendamento: Agendamento; destaque?: boolean }) => (
  <div className={`ma-card${agendamento.tipo === 'AVALIACAO' ? ' ma-card-avaliacao' : ' ma-card-tratamento'}${destaque ? ' ma-card-destaque' : ''}`}>
    <div className="ma-card-lateral">
      <span className="ma-card-dia-num">
        {new Date(agendamento.inicioEm).getDate()}
      </span>
      <span className="ma-card-mes">
        {new Date(agendamento.inicioEm).toLocaleDateString('pt-BR', { month: 'short' })}
      </span>
    </div>

    <div className="ma-card-corpo">
      <div className="ma-card-linha-topo">
        <span className="ma-card-profissional">{agendamento.nomeProfissional}</span>
        <span className={`ma-card-status ${CLASSE_STATUS[agendamento.status] || ''}`}>
          {ICONE_STATUS[agendamento.status]} {ROTULO_STATUS[agendamento.status] ?? agendamento.status}
        </span>
      </div>

      <div className="ma-card-linha-meio">
        <span className="ma-card-tipo">
          {agendamento.tipo === 'AVALIACAO' ? 'Avaliação' : 'Tratamento'}
        </span>
      </div>

      <div className="ma-card-linha-rodape">
        <span className="ma-card-horario">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
          </svg>
          {formatarHora(agendamento.inicioEm)} – {formatarHora(agendamento.fimEm)}
        </span>
        <span className="ma-card-duracao">{calcularDuracao(agendamento.inicioEm, agendamento.fimEm)}</span>
      </div>
    </div>
  </div>
)

const ResumoCard = ({ label, valor, cor }: { label: string; valor: number; cor: string }) => (
  <div className="ma-resumo-card" style={{ borderTopColor: cor }}>
    <span className="ma-resumo-valor" style={{ color: cor }}>{valor}</span>
    <span className="ma-resumo-label">{label}</span>
  </div>
)

export default function MinhaAgendaPage() {
  const { usuario } = useAuth()
  const [agendamentos, setAgendamentos] = useState<Agendamento[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [aba, setAba] = useState<Aba>('proximos')

  const fetchAgendamentos = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await agendamentoAPI.getMeusAgendamentos()
      setAgendamentos(data)
    } catch (err: any) {
      setError(err.message || 'Erro ao carregar agendamentos')
      console.error('Erro:', err)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (usuario) fetchAgendamentos()
  }, [usuario, fetchAgendamentos])

  const { proximos, historico, resumo } = useMemo(() => {
    const ordenados = [...agendamentos].sort(
      (a, b) => new Date(a.inicioEm).getTime() - new Date(b.inicioEm).getTime(),
    )

    const prox = ordenados.filter(isProximo)
    const hist = ordenados
      .filter(a => !isProximo(a))
      .sort((a, b) => new Date(b.inicioEm).getTime() - new Date(a.inicioEm).getTime())

    return {
      proximos: prox,
      historico: hist,
      resumo: {
        total: agendamentos.length,
        proximos: prox.length,
        realizados: agendamentos.filter(a => a.status === 'REALIZADO').length,
        cancelados: agendamentos.filter(a => a.status === 'CANCELADO').length,
      },
    }
  }, [agendamentos])

  const proximoAgendamento = proximos[0]

  return (
    <LayoutPaciente>
      <RotaProtegida perfisNecessarios={['ROLE_PACIENTE']}>
        <div className="ma-page">

          <div className="ma-page-header">
            <div>
              <h1 className="ma-titulo">Minha Agenda</h1>
              <p className="ma-subtitulo">
                {new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
              </p>
            </div>
          </div>

          {!loading && !error && agendamentos.length > 0 && (
            <div className="ma-resumo">
              <ResumoCard label="Total" valor={resumo.total} cor="#2b6cb0" />
              <ResumoCard label="Próximos" valor={resumo.proximos} cor="#f59e0b" />
              <ResumoCard label="Realizados" valor={resumo.realizados} cor="#10b981" />
              <ResumoCard label="Cancelados" valor={resumo.cancelados} cor="#ef4444" />
            </div>
          )}

          {!loading && !error && proximoAgendamento && (
            <div className="ma-proximo-destaque">
              <div className="ma-proximo-label">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" />
                </svg>
                Próximo agendamento
              </div>
              <div className="ma-proximo-info">
                <div>
                  <p className="ma-proximo-data">{formatarDataCompleta(proximoAgendamento.inicioEm)}</p>
                  <p className="ma-proximo-hora">
                    {formatarHora(proximoAgendamento.inicioEm)} – {formatarHora(proximoAgendamento.fimEm)}
                    <span className="ma-proximo-duracao">{calcularDuracao(proximoAgendamento.inicioEm, proximoAgendamento.fimEm)}</span>
                  </p>
                  <p className="ma-proximo-profissional">{proximoAgendamento.nomeProfissional}</p>
                </div>
                <span className={`ma-proximo-tipo ${proximoAgendamento.tipo === 'AVALIACAO' ? 'ma-tipo-avaliacao' : 'ma-tipo-tratamento'}`}>
                  {proximoAgendamento.tipo === 'AVALIACAO' ? 'Avaliação' : 'Tratamento'}
                </span>
              </div>
            </div>
          )}

          <div className="ma-abas">
            <button
              className={`ma-aba${aba === 'proximos' ? ' ma-aba-ativa' : ''}`}
              onClick={() => setAba('proximos')}
            >
              Próximos
              {proximos.length > 0 && <span className="ma-aba-badge">{proximos.length}</span>}
            </button>
            <button
              className={`ma-aba${aba === 'historico' ? ' ma-aba-ativa' : ''}`}
              onClick={() => setAba('historico')}
            >
              Histórico
              {historico.length > 0 && <span className="ma-aba-badge">{historico.length}</span>}
            </button>
          </div>

          {loading && (
            <div className="ma-estado">
              <div className="ma-spinner" />
              <p>Carregando seus agendamentos...</p>
            </div>
          )}

          {error && !loading && (
            <div className="ma-estado ma-estado-erro">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ef4444" strokeWidth="1.5">
                <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <p>{error}</p>
              <button className="ma-retry-btn" onClick={fetchAgendamentos}>Tentar novamente</button>
            </div>
          )}

          {!loading && !error && (
            <div className="ma-lista">
              {aba === 'proximos' && (
                proximos.length === 0 ? (
                  <div className="ma-estado">
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" strokeWidth="1.5">
                      <rect x="3" y="4" width="18" height="18" rx="2" ry="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" />
                    </svg>
                    <p>Nenhum agendamento próximo.</p>
                  </div>
                ) : (
                  proximos.map((a, idx) => (
                    <CardAgendamento key={a.id} agendamento={a} destaque={idx === 0} />
                  ))
                )
              )}

              {aba === 'historico' && (
                historico.length === 0 ? (
                  <div className="ma-estado">
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" strokeWidth="1.5">
                      <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
                    </svg>
                    <p>Nenhum agendamento no histórico.</p>
                  </div>
                ) : (
                  historico.map(a => (
                    <CardAgendamento key={a.id} agendamento={a} />
                  ))
                )
              )}
            </div>
          )}

        </div>
      </RotaProtegida>
    </LayoutPaciente>
  )
}
