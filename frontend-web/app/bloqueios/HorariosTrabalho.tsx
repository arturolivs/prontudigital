'use client'

import { useCallback, useEffect, useState } from 'react'
import { CalendarClock, Clock, Plus, Trash2 } from 'lucide-react'
import { horarioTrabalhoAPI } from '@/lib/horarioTrabalho.service'
import { DIAS_SEMANA_TRABALHO, HorarioTrabalho } from '@/tipos/horarioTrabalho'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import { useNotificacao } from '@/contexts/ToastContext'

// RF05 — expediente semanal do profissional. É o complemento dos bloqueios:
// aqui fica quando ele atende; lá, as exceções dentro desse expediente.
// Sem nenhuma janela cadastrada o backend não restringe os agendamentos.

const formatarHora = (hora: string) => hora.slice(0, 5)

interface HorariosTrabalhoProps {
  profissionalUuid: string | null
}

export default function HorariosTrabalho({
  profissionalUuid,
}: HorariosTrabalhoProps) {
  const { exibirNotificacao } = useNotificacao()

  const [horarios, setHorarios] = useState<HorarioTrabalho[]>([])
  const [carregando, setCarregando] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [formAberto, setFormAberto] = useState(false)
  const [diasSemana, setDiasSemana] = useState<number[]>([])
  const [horaInicio, setHoraInicio] = useState('08:00')
  const [horaFim, setHoraFim] = useState('18:00')

  const carregar = useCallback(async () => {
    if (!profissionalUuid) return
    setCarregando(true)
    try {
      setHorarios(await horarioTrabalhoAPI.listar(profissionalUuid))
    } catch {
      exibirNotificacao(MENSAGENS.erro.carregarHorariosTrabalho, 'error')
    } finally {
      setCarregando(false)
    }
  }, [profissionalUuid, exibirNotificacao])

  useEffect(() => {
    carregar()
  }, [carregar])

  const alternarDia = (dia: number) =>
    setDiasSemana(prev =>
      prev.includes(dia) ? prev.filter(d => d !== dia) : [...prev, dia],
    )

  const handleCriar = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!profissionalUuid) return

    if (diasSemana.length === 0) {
      exibirNotificacao(MENSAGENS.validacao.selecioneDiaSemana, 'error')
      return
    }
    if (horaFim <= horaInicio) {
      exibirNotificacao(MENSAGENS.validacao.horarioTerminoInvalido, 'error')
      return
    }

    setSalvando(true)
    try {
      // Um POST por dia: o endpoint cadastra uma janela por vez.
      const criadas = await Promise.all(
        diasSemana.map(dia =>
          horarioTrabalhoAPI.criar({
            profissionalUuid,
            diaSemana: dia,
            horaInicio: `${horaInicio}:00`,
            horaFim: `${horaFim}:00`,
          }),
        ),
      )
      setHorarios(prev => [...prev, ...criadas])
      setDiasSemana([])
      setFormAberto(false)
      exibirNotificacao(MENSAGENS.sucesso.horarioTrabalhoCriado, 'success')
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.criarHorarioTrabalho),
        'error',
      )
    } finally {
      setSalvando(false)
    }
  }

  const handleExcluir = async (horario: HorarioTrabalho) => {
    try {
      await horarioTrabalhoAPI.remover(horario.id)
      setHorarios(prev => prev.filter(h => h.id !== horario.id))
      exibirNotificacao(MENSAGENS.sucesso.horarioTrabalhoRemovido, 'success')
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.removerHorarioTrabalho),
        'error',
      )
    }
  }

  if (!profissionalUuid) return null

  return (
    <div className="recorrentes-secao">
      <div className="recorrentes-titulo">
        <CalendarClock size={15} strokeWidth={2} />
        Horários de trabalho
        <button
          type="button"
          className="ht-btn-adicionar"
          onClick={() => setFormAberto(aberto => !aberto)}
        >
          <Plus size={14} strokeWidth={2} />
          {formAberto ? 'Cancelar' : 'Adicionar'}
        </button>
      </div>

      {formAberto && (
        <form className="ht-form" onSubmit={handleCriar}>
          <div className="ht-dias">
            {DIAS_SEMANA_TRABALHO.map(dia => (
              <button
                key={dia.valor}
                type="button"
                className={`ht-dia${
                  diasSemana.includes(dia.valor) ? ' ht-dia--ativo' : ''
                }`}
                onClick={() => alternarDia(dia.valor)}
                aria-pressed={diasSemana.includes(dia.valor)}
              >
                {dia.abrev}
              </button>
            ))}
          </div>
          <div className="ht-horas">
            <label>
              Das
              <input
                type="time"
                value={horaInicio}
                onChange={e => setHoraInicio(e.target.value)}
                className="filtro-input"
              />
            </label>
            <label>
              às
              <input
                type="time"
                value={horaFim}
                onChange={e => setHoraFim(e.target.value)}
                className="filtro-input"
              />
            </label>
            <button type="submit" className="ht-btn-salvar" disabled={salvando}>
              {salvando ? 'Salvando...' : 'Salvar'}
            </button>
          </div>
        </form>
      )}

      {carregando ? (
        <p className="ht-vazio">Carregando...</p>
      ) : horarios.length === 0 ? (
        <p className="ht-vazio">
          Nenhum horário definido — o profissional pode ser agendado a qualquer
          hora.
        </p>
      ) : (
        <div className="recorrentes-lista">
          {horarios.map(horario => (
            <div key={horario.id} className="recorrente-item ht-item">
              <span className="recorrente-dia">
                {
                  DIAS_SEMANA_TRABALHO.find(d => d.valor === horario.diaSemana)
                    ?.label
                }
              </span>
              <span className="recorrente-horario">
                <Clock size={11} strokeWidth={2} />
                {formatarHora(horario.horaInicio)} →{' '}
                {formatarHora(horario.horaFim)}
              </span>
              <button
                className="btn-excluir-bloqueio"
                onClick={() => handleExcluir(horario)}
                title="Remover horário de trabalho"
              >
                <Trash2 size={15} strokeWidth={1.75} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
