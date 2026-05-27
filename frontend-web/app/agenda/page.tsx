'use client'

import { useState, useEffect, useCallback } from 'react'
import { ProtectedRoute } from '../../components/ProtectedRoute'
import { useAuth } from '../../contexts/AuthContext'
import { agendamentoAPI } from '../../lib/careSession'
import { AgendamentoRequisicao } from '../../tipos/appointment'
import Layout from '@/components/Layout/Layout'
import AppointmentFormModal from '@/components/AppointmentFormModal'
import './agenda.css'
import ListaAgendamentos from '@/components/ListaAgendamentos'
import { Agendamento } from '@/tipos/CareSession'

const formatarDataISO = (date: Date): string => date.toISOString().split('T')[0]

const formatarDataExibicao = (dateStr: string): string => {
  const date = new Date(dateStr + 'T00:00:00')
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  const amanha = new Date(hoje)
  amanha.setDate(amanha.getDate() + 1)

  console.log(date, 'date @@')
  console.log(hoje, 'hoje @@')
  console.log(amanha, 'amanha @@')
  if (date.toDateString() === hoje.toDateString()) return 'Hoje'
  if (date.toDateString() === amanha.toDateString()) return 'Amanhã'
  return date.toLocaleDateString('pt-BR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  })
}

export default function AgendaPage() {
  const { usuario, temPerfil } = useAuth()
  const [agendamentos, setAgendamentos] = useState<Agendamento[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedDate, setSelectedDate] = useState(formatarDataISO(new Date()))
  const [isModalOpen, setIsModalOpen] = useState(false)

  const isEnfermeiro = temPerfil('ROLE_PROFISSIONAL')
  const isAdmin = temPerfil('ROLE_ADMIN')
  const hasRequiredRole = isEnfermeiro || isAdmin

  const fetchAgendamentos = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await agendamentoAPI.getCareSessions('day', selectedDate)
      setAgendamentos(data)
    } catch (err: any) {
      setError(err.message || 'Erro ao carregar agendamentos')
      console.error('Erro:', err)
    } finally {
      setLoading(false)
    }
  }, [selectedDate])

  useEffect(() => {
    if (usuario && hasRequiredRole) {
      fetchAgendamentos()
    }
  }, [selectedDate, usuario, hasRequiredRole, fetchAgendamentos])

  const navegar = (dias: number) => {
    const date = new Date(selectedDate + 'T00:00:00')
    date.setDate(date.getDate() + dias)
    setSelectedDate(formatarDataISO(date))
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

  if (!usuario || !hasRequiredRole) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  return (
    <Layout userRole={isAdmin ? 'ROLE_ADMIN' : 'ROLE_PROFISSIONAL'}>
      <ProtectedRoute requiredRoles={['ROLE_PROFISSIONAL', 'ROLE_ADMIN']}>
        <div className="appointments-page">
          <div className="flex justify-between items-center mb-6">
            <button
              onClick={() => setIsModalOpen(true)}
              className="bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg flex items-center gap-2 transition-colors"
              title="Novo agendamento"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5"
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
          </div>

          <div className="flex items-center justify-center gap-4 mb-6">
            <button
              onClick={() => navegar(-1)}
              className="p-2 rounded-full hover:bg-gray-100 transition-colors"
              title="Dia anterior"
            >
              ◀
            </button>
            <span className="text-lg font-medium text-gray-700 min-w-[200px] text-center capitalize">
              {formatarDataExibicao(selectedDate)}
            </span>
            <button
              onClick={() => navegar(1)}
              className="p-2 rounded-full hover:bg-gray-100 transition-colors"
              title="Próximo dia"
            >
              ▶
            </button>
            <button
              onClick={() => setSelectedDate(formatarDataISO(new Date()))}
              className="text-sm text-blue-600 hover:underline"
            >
              Hoje
            </button>
          </div>

          <ListaAgendamentos
            agendamentos={agendamentos}
            carregando={loading}
            erro={error}
          />
        </div>

        <AppointmentFormModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSubmit={handleCreateAppointment}
          profissionalUuid={isEnfermeiro ? usuario.uuid : undefined}
        />
      </ProtectedRoute>
    </Layout>
  )
}
