'use client'

import React, { useMemo } from 'react'
import { Agendamento } from '@/tipos/agendamento'
import './AgendaSemanal.css'

interface PropsAgendaSemanal {
  agendamentos: Agendamento[]
  carregando: boolean
  erro: string | null
  dataSelecionada: string
  aoSelecionarDia: (data: string) => void
  onAgendamentoClick?: (agendamento: Agendamento) => void
}

const DIAS_SEMANA_CURTO = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']

const obterInicioSemana = (dateStr: string): Date => {
  const d = new Date(dateStr + 'T00:00:00')
  const dia = d.getDay()
  const diff = d.getDate() - dia + (dia === 0 ? -6 : 1)
  d.setDate(diff)
  return d
}

const formatarDataISO = (date: Date): string => date.toISOString().split('T')[0]

const formatarHora = (dataString: string): string =>
  new Date(dataString).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })

const obterClasseCard = (tipo: string): string => {
  const mapa: Record<string, string> = {
    AVALIACAO: 'semana-card-avaliacao',
    TRATAMENTO: 'semana-card-tratamento',
  }
  return mapa[tipo] || 'semana-card-default'
}

const AgendaSemanal: React.FC<PropsAgendaSemanal> = ({
  agendamentos,
  carregando,
  erro,
  dataSelecionada,
  aoSelecionarDia,
  onAgendamentoClick,
}) => {
  const diasSemana = useMemo(() => {
    const inicio = obterInicioSemana(dataSelecionada)
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(inicio)
      d.setDate(d.getDate() + i)
      return d
    })
  }, [dataSelecionada])

  const agendamentosPorDia = useMemo(() => {
    const mapa: Record<string, Agendamento[]> = {}
    diasSemana.forEach(d => { mapa[formatarDataISO(d)] = [] })
    agendamentos.forEach(a => {
      const chave = new Date(a.inicioEm).toISOString().split('T')[0]
      if (mapa[chave]) mapa[chave].push(a)
    })
    Object.keys(mapa).forEach(chave => {
      mapa[chave].sort((a, b) => new Date(a.inicioEm).getTime() - new Date(b.inicioEm).getTime())
    })
    return mapa
  }, [agendamentos, diasSemana])

  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)

  if (carregando) {
    return (
      <div className="semana-estado">
        <div className="semana-spinner" />
        <p>Carregando agendamentos...</p>
      </div>
    )
  }

  if (erro) {
    return (
      <div className="semana-estado semana-erro">
        <p>{erro}</p>
      </div>
    )
  }

  return (
    <div className="semana-wrapper">
      <div className="semana-grid">
        {diasSemana.map((dia, idx) => {
          const chave = formatarDataISO(dia)
          const aptos = agendamentosPorDia[chave] || []
          const isHoje = dia.toDateString() === hoje.toDateString()
          const isSelecionado = chave === dataSelecionada

          return (
            <div
              key={chave}
              className={`semana-coluna${isHoje ? ' semana-coluna-hoje' : ''}${isSelecionado ? ' semana-coluna-selecionada' : ''}`}
              onClick={() => aoSelecionarDia(chave)}
              role="button"
              tabIndex={0}
              onKeyDown={e => e.key === 'Enter' && aoSelecionarDia(chave)}
              aria-label={`Ver dia ${dia.toLocaleDateString('pt-BR')}`}
            >
              <div className="semana-col-header">
                <span className="semana-dia-nome">{DIAS_SEMANA_CURTO[idx]}</span>
                <span className={`semana-dia-num${isHoje ? ' semana-dia-num-hoje' : ''}`}>
                  {dia.getDate()}
                </span>
                {aptos.length > 0 && (
                  <span className="semana-badge">{aptos.length}</span>
                )}
              </div>

              <div className="semana-col-body">
                {aptos.length === 0 ? (
                  <div className="semana-vazio">Livre</div>
                ) : (
                  aptos.map(a => (
                    <div
                      key={a.id}
                      className={`semana-card ${obterClasseCard(a.tipo)}`}
                      title={`${a.nomePaciente} — ${formatarHora(a.inicioEm)} às ${formatarHora(a.fimEm)}`}
                      onClick={e => {
                        e.stopPropagation()
                        onAgendamentoClick ? onAgendamentoClick(a) : aoSelecionarDia(chave)
                      }}
                    >
                      <span className="semana-card-hora">{formatarHora(a.inicioEm)}</span>
                      <span className="semana-card-nome">{(a.nomePaciente ?? '').split(' ')[0]}</span>
                      <span className="semana-card-tipo">
                        {a.tipo === 'AVALIACAO' ? 'Aval.' : 'Trat.'}
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default AgendaSemanal
