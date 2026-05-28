'use client'

import { useState, useEffect, useCallback } from 'react'
import { RotaProtegida } from '../../components/RotaProtegida'
import { useAuth } from '../../contexts/AuthContext'
import { agendamentoAPI } from '../../lib/careSession'
import { Agendamento, AgendamentoRequisicao } from '../../tipos/agendamento'
import Layout from '@/components/Layout/Layout'
import './agenda.css'
import ListaAgendamentos from '@/components/ListaAgendamentos'
import AgendaSemanal from '@/components/AgendaSemanal'
import AgendaMensal from '@/components/AgendaMensal'

type TipoVisualizacao = 'day' | 'week' | 'month'

const formatarDataISO = (date: Date): string => date.toISOString().split('T')[0]

const obterInicioSemana = (dateStr: string): Date => {
  const d = new Date(dateStr + 'T00:00:00')
  const dia = d.getDay()
  d.setDate(d.getDate() - dia + (dia === 0 ? -6 : 1))
  return d
}

const formatarPeriodo = (tipo: TipoVisualizacao, date: Date): string => {
  if (tipo === 'day') {
    const hoje = new Date()
    hoje.setHours(0, 0, 0, 0)
    const amanha = new Date(hoje)
    amanha.setDate(amanha.getDate() + 1)
    if (date.toDateString() === hoje.toDateString()) return 'Hoje'
    if (date.toDateString() === amanha.toDateString()) return 'Amanhã'
    return date.toLocaleDateString('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    })
  }
  if (tipo === 'week') {
    const inicio = obterInicioSemana(formatarDataISO(date))
    const fim = new Date(inicio)
    fim.setDate(fim.getDate() + 6)
    if (inicio.getMonth() === fim.getMonth()) {
      return `${inicio.getDate()} – ${fim.getDate()} de ${fim.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}`
    }
    return `${inicio.toLocaleDateString('pt-BR', { day: 'numeric', month: 'short' })} – ${fim.toLocaleDateString('pt-BR', { day: 'numeric', month: 'short', year: 'numeric' })}`
  }
  return date.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })
}

export default function AgendaPage() {
  const { usuario, temPerfil } = useAuth()
  const [agendamentos, setAgendamentos] = useState<Agendamento[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedDate, setSelectedDate] = useState(formatarDataISO(new Date()))
  const [viewType, setViewType] = useState<TipoVisualizacao>('day')
  const [isModalOpen, setIsModalOpen] = useState(false)

  const isEnfermeiro = temPerfil('ROLE_PROFISSIONAL')
  const isAdmin = temPerfil('ROLE_ADMIN')
  const hasRequiredRole = isEnfermeiro || isAdmin

  const fetchAgendamentos = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await agendamentoAPI.getCareSessions(viewType, selectedDate)
      setAgendamentos(data)
    } catch (err: any) {
      setError(err.message || 'Erro ao carregar agendamentos')
      console.error('Erro:', err)
    } finally {
      setLoading(false)
    }
  }, [selectedDate, viewType])

  useEffect(() => {
    if (usuario && hasRequiredRole) {
      fetchAgendamentos()
    }
  }, [selectedDate, viewType, usuario, hasRequiredRole, fetchAgendamentos])

  const navegar = (direcao: number) => {
    const date = new Date(selectedDate + 'T00:00:00')
    if (viewType === 'day') date.setDate(date.getDate() + direcao)
    else if (viewType === 'week') date.setDate(date.getDate() + direcao * 7)
    else date.setMonth(date.getMonth() + direcao)
    setSelectedDate(formatarDataISO(date))
  }

  const handleDiaSelecionado = (data: string) => {
    setSelectedDate(data)
    setViewType('day')
  }

  const handleCreateAppointment = async (dados: AgendamentoRequisicao) => {
    try {
      await agendamentoAPI.criarAgendamento(dados)
      setIsModalOpen(false)
      fetchAgendamentos()
    } catch (err: any) {
      setError(err.message || 'Erro ao criar agendamento')
      console.error('Erro:', err)
    }
  }

  const dataAtual = new Date(selectedDate + 'T00:00:00')

  if (!usuario || !hasRequiredRole) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  return (
    <Layout perfil={isAdmin ? 'ROLE_ADMIN' : 'ROLE_PROFISSIONAL'}>
      <RotaProtegida perfisNecessarios={['ROLE_PROFISSIONAL', 'ROLE_ADMIN']}>
        <div className="appointments-page">
          <div className="agenda-toolbar">
            <button
              onClick={() => setIsModalOpen(true)}
              className="agenda-btn-novo"
              title="Novo agendamento"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M10 3a1 1 0 00-1 1v5H4a1 1 0 100 2h5v5a1 1 0 102 0v-5h5a1 1 0 100-2h-5V4a1 1 0 00-1-1z"
                  clipRule="evenodd"
                />
              </svg>
              Novo
            </button>

            <div className="agenda-view-switcher">
              {(['day', 'week', 'month'] as TipoVisualizacao[]).map(tipo => (
                <button
                  key={tipo}
                  className={`agenda-view-btn${viewType === tipo ? ' agenda-view-btn-ativo' : ''}`}
                  onClick={() => setViewType(tipo)}
                >
                  {tipo === 'day' ? 'Dia' : tipo === 'week' ? 'Semana' : 'Mês'}
                </button>
              ))}
            </div>
          </div>

          <div className="agenda-nav">
            <button
              onClick={() => navegar(-1)}
              className="agenda-nav-arrow"
              title={
                viewType === 'day'
                  ? 'Dia anterior'
                  : viewType === 'week'
                    ? 'Semana anterior'
                    : 'Mês anterior'
              }
            >
              ‹
            </button>
            <span className="agenda-nav-label capitalize">
              {formatarPeriodo(viewType, dataAtual)}
            </span>
            <button
              onClick={() => navegar(1)}
              className="agenda-nav-arrow"
              title={
                viewType === 'day'
                  ? 'Próximo dia'
                  : viewType === 'week'
                    ? 'Próxima semana'
                    : 'Próximo mês'
              }
            >
              ›
            </button>
            <button
              onClick={() => setSelectedDate(formatarDataISO(new Date()))}
              className="agenda-nav-hoje"
            >
              Hoje
            </button>
          </div>

          {viewType === 'day' && (
            <ListaAgendamentos
              agendamentos={agendamentos}
              carregando={loading}
              erro={error}
            />
          )}

          {viewType === 'week' && (
            <AgendaSemanal
              agendamentos={agendamentos}
              carregando={loading}
              erro={error}
              dataSelecionada={selectedDate}
              aoSelecionarDia={handleDiaSelecionado}
            />
          )}

          {viewType === 'month' && (
            <AgendaMensal
              agendamentos={agendamentos}
              carregando={loading}
              erro={error}
              dataSelecionada={selectedDate}
              aoSelecionarDia={handleDiaSelecionado}
            />
          )}
        </div>
      </RotaProtegida>
    </Layout>
  )
}
