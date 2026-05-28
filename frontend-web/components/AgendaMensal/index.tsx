'use client'

import React, { useMemo } from 'react'
import { Agendamento } from '@/tipos/CareSession'
import './AgendaMensal.css'

interface PropsAgendaMensal {
  agendamentos: Agendamento[]
  carregando: boolean
  erro: string | null
  dataSelecionada: string
  aoSelecionarDia: (data: string) => void
}

const DIAS_SEMANA = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']

const formatarDataISO = (date: Date): string => date.toISOString().split('T')[0]

const formatarHora = (dataString: string): string =>
  new Date(dataString).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })

const diaDaSemanaParaOffset = (dia: number): number => (dia === 0 ? 6 : dia - 1)

const AgendaMensal: React.FC<PropsAgendaMensal> = ({
  agendamentos,
  carregando,
  erro,
  dataSelecionada,
  aoSelecionarDia,
}) => {
  const dataRef = new Date(dataSelecionada + 'T00:00:00')
  const ano = dataRef.getFullYear()
  const mes = dataRef.getMonth()

  const celulas = useMemo(() => {
    const primeiroDia = new Date(ano, mes, 1)
    const ultimoDia = new Date(ano, mes + 1, 0)
    const offset = diaDaSemanaParaOffset(primeiroDia.getDay())
    const totalCelulas = Math.ceil((offset + ultimoDia.getDate()) / 7) * 7

    return Array.from({ length: totalCelulas }, (_, i) => {
      const numDia = i - offset + 1
      if (numDia < 1 || numDia > ultimoDia.getDate()) return null
      return new Date(ano, mes, numDia)
    })
  }, [ano, mes])

  const agendamentosPorDia = useMemo(() => {
    const mapa: Record<string, Agendamento[]> = {}
    agendamentos.forEach(a => {
      const chave = new Date(a.inicioEm).toISOString().split('T')[0]
      if (!mapa[chave]) mapa[chave] = []
      mapa[chave].push(a)
    })
    return mapa
  }, [agendamentos])

  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)

  if (carregando) {
    return (
      <div className="mensal-estado">
        <div className="mensal-spinner" />
        <p>Carregando agendamentos...</p>
      </div>
    )
  }

  if (erro) {
    return (
      <div className="mensal-estado mensal-estado-erro">
        <p>{erro}</p>
      </div>
    )
  }

  return (
    <div className="mensal-container">
      <div className="mensal-cabecalho">
        {DIAS_SEMANA.map(d => (
          <div key={d} className="mensal-cab-dia">{d}</div>
        ))}
      </div>

      <div className="mensal-grid">
        {celulas.map((dia, idx) => {
          if (!dia) {
            return <div key={`vazio-${idx}`} className="mensal-celula mensal-celula-fora" />
          }

          const chave = formatarDataISO(dia)
          const aptos = agendamentosPorDia[chave] || []
          const isHoje = dia.toDateString() === hoje.toDateString()
          const isSelecionado = chave === dataSelecionada
          const temAptos = aptos.length > 0

          return (
            <div
              key={chave}
              className={`mensal-celula${isHoje ? ' mensal-celula-hoje' : ''}${isSelecionado ? ' mensal-celula-selecionada' : ''}${temAptos ? ' mensal-celula-com-aptos' : ''}`}
              onClick={() => aoSelecionarDia(chave)}
              role="button"
              tabIndex={0}
              onKeyDown={e => e.key === 'Enter' && aoSelecionarDia(chave)}
              aria-label={`${dia.toLocaleDateString('pt-BR')}, ${aptos.length} agendamento${aptos.length !== 1 ? 's' : ''}`}
            >
              <span className={`mensal-num${isHoje ? ' mensal-num-hoje' : ''}`}>
                {dia.getDate()}
              </span>

              {temAptos && (
                <div className="mensal-aptos">
                  {aptos.slice(0, 3).map(a => (
                    <div
                      key={a.id}
                      className={`mensal-apto-item ${a.tipo === 'AVALIACAO' ? 'mensal-item-avaliacao' : 'mensal-item-tratamento'}`}
                      title={`${a.nomePaciente} — ${formatarHora(a.inicioEm)}`}
                    >
                      <span className="mensal-apto-hora">{formatarHora(a.inicioEm)}</span>
                      <span className="mensal-apto-nome">
                        {a.nomePaciente.split(' ')[0]}
                      </span>
                    </div>
                  ))}
                  {aptos.length > 3 && (
                    <div className="mensal-apto-mais">+{aptos.length - 3} mais</div>
                  )}
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default AgendaMensal
