'use client'

import { useState, useEffect } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { useAuth } from '@/contexts/AuthContext'
import { careSessionAPI } from '@/lib/careSession'
import { CareSession } from '@/tipos/CareSession'
import Layout from '@/components/Layout/Layout'
import './session-detail.css'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  faUser,
  faPhone,
  faCalendar,
  faClock,
  faMapMarkerAlt,
  faStethoscope,
  faNotesMedical,
  faBed,
  faArrowLeft,
  faCheckCircle,
  faTimesCircle,
  faClipboardCheck,
} from '@fortawesome/free-solid-svg-icons'

export default function CareSessionDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { user, hasRole } = useAuth()
  const [session, setSession] = useState<CareSession | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<'info' | 'procedures' | 'notes'>(
    'info',
  )
  const [isCompleting, setIsCompleting] = useState(false)

  const sessionId = params.id as string

  useEffect(() => {
    if (user && hasRequiredRole) {
      fetchSession()
    }
  }, [sessionId, user])

  const hasRequiredRole =
    hasRole('NURSE') || hasRole('DOCTOR') || hasRole('ADMIN')

  const fetchSession = async () => {
    try {
      setLoading(true)
      const data = await careSessionAPI.getCareSessionById(sessionId)
      setSession(data)
    } catch (err: any) {
      setError(err.message || 'Erro ao carregar sessão')
      console.error('Erro:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleStartSession = async () => {
    try {
      await careSessionAPI.updateSessionStatus(sessionId, 'IN_PROGRESS')
      fetchSession() // Refresh session data
    } catch (err: any) {
      setError(err.message || 'Erro ao iniciar sessão')
      console.error('Erro:', err)
    }
  }

  const handleCompleteSession = async () => {
    try {
      setIsCompleting(true)
      await careSessionAPI.updateSessionStatus(sessionId, 'COMPLETED')
      router.push('/care-sessions') // Redirect back to list
    } catch (err: any) {
      setError(err.message || 'Erro ao completar sessão')
      console.error('Erro:', err)
    } finally {
      setIsCompleting(false)
    }
  }

  const handleCancelSession = async () => {
    try {
      await careSessionAPI.updateSessionStatus(sessionId, 'CANCELLED')
      router.push('/care-sessions')
    } catch (err: any) {
      setError(err.message || 'Erro ao cancelar sessão')
      console.error('Erro:', err)
    }
  }

  const formatDateTime = (dateTime: string) => {
    const date = new Date(dateTime)
    return {
      date: date.toLocaleDateString('pt-BR'),
      time: date.toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit',
      }),
      full: date.toLocaleString('pt-BR'),
    }
  }

  const translateType = (type: string) => {
    const translations: Record<string, string> = {
      AVALIACAO: 'Avaliação',
      TRATAMENTO: 'Tratamento',
      PODEATRIA: 'Podiatria',
      TRATAMENTO_FERIDAS: 'Tratamento de Feridas',
    }
    return translations[type] || type
  }

  if (!user || !hasRequiredRole) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  if (loading) {
    return (
      <Layout userRole={'NURSE'}>
        <div className="session-detail-loading">
          <div className="spinner"></div>
          <p>Carregando sessão...</p>
        </div>
      </Layout>
    )
  }

  if (error || !session) {
    return (
      <Layout userRole={'NURSE'}>
        <div className="session-detail-error">
          <div className="error-icon">!</div>
          <p>{error || 'Sessão não encontrada'}</p>
          <button
            className="back-button"
            onClick={() => router.push('/care-sessions')}
          >
            <FontAwesomeIcon icon={faArrowLeft} />
            Voltar para agendamentos
          </button>
        </div>
      </Layout>
    )
  }

  const startTime = formatDateTime(session.scheduledStart)
  const endTime = formatDateTime(session.scheduledEnd)

  return (
    <Layout userRole={'NURSE'}>
      <ProtectedRoute requiredRoles={['NURSE', 'DOCTOR', 'ADMIN']}>
        <div className="session-detail-page">
          {/* Header */}
          <div className="session-header">
            <button
              className="back-button"
              onClick={() => router.push('/care-sessions')}
            >
              <FontAwesomeIcon icon={faArrowLeft} />
              Voltar
            </button>

            <div className="session-title">
              <h1>Sessão de Atendimento</h1>
              <div
                className={`session-status-badge status-${session.status.toLowerCase()}`}
              >
                {session.status === 'IN_PROGRESS' && (
                  <>
                    <FontAwesomeIcon icon={faClock} className="animate-pulse" />
                    <span>Em Andamento</span>
                  </>
                )}
                {session.status === 'SCHEDULED' && 'Agendada'}
                {session.status === 'CONFIRMED' && 'Confirmada'}
                {session.status === 'COMPLETED' && 'Concluída'}
                {session.status === 'CANCELLED' && 'Cancelada'}
              </div>
            </div>

            {session.status === 'CONFIRMED' && (
              <button
                className="start-session-btn"
                onClick={handleStartSession}
              >
                <FontAwesomeIcon icon={faClipboardCheck} />
                Iniciar Sessão
              </button>
            )}

            {session.status === 'IN_PROGRESS' && (
              <div className="action-buttons">
                <button
                  className="complete-btn"
                  onClick={handleCompleteSession}
                  disabled={isCompleting}
                >
                  <FontAwesomeIcon icon={faCheckCircle} />
                  {isCompleting ? 'Finalizando...' : 'Finalizar Sessão'}
                </button>
                <button className="cancel-btn" onClick={handleCancelSession}>
                  <FontAwesomeIcon icon={faTimesCircle} />
                  Cancelar
                </button>
              </div>
            )}
          </div>

          {/* Main Content */}
          <div className="session-content">
            {/* Patient Info Card */}
            <div className="info-card patient-card">
              <h2 className="card-title">
                <FontAwesomeIcon icon={faUser} />
                Informações do Paciente
              </h2>
              <div className="patient-details">
                <div className="patient-name">{session.patientName}</div>
                <div className="patient-contact">
                  <FontAwesomeIcon icon={faPhone} />
                  <span>{session.patientPhone}</span>
                </div>
                {session.patientPhone && (
                  <div className="patient-email">{session.patientPhone}</div>
                )}
              </div>
            </div>

            {/* Session Details Card */}
            <div className="info-card session-details-card">
              <h2 className="card-title">
                <FontAwesomeIcon icon={faCalendar} />
                Detalhes da Sessão
              </h2>

              <div className="details-grid">
                <div className="detail-item">
                  <div className="detail-label">
                    <FontAwesomeIcon icon={faClock} />
                    Horário:
                  </div>
                  <div className="detail-value">
                    {startTime.time} - {endTime.time}
                  </div>
                </div>

                <div className="detail-item">
                  <div className="detail-label">
                    <FontAwesomeIcon icon={faCalendar} />
                    Data:
                  </div>
                  <div className="detail-value">{startTime.date}</div>
                </div>

                <div className="detail-item">
                  <div className="detail-label">
                    <FontAwesomeIcon icon={faMapMarkerAlt} />
                    Local:
                  </div>
                  <div className="detail-value">
                    {session.location === 'CLINIC' ? 'Clínica' : 'Domicílio'}
                    {session.location && (
                      <div className="address">{session.location}</div>
                    )}
                  </div>
                </div>

                <div className="detail-item">
                  <div className="detail-label">
                    <FontAwesomeIcon icon={faStethoscope} />
                    Tipo de Sessão:
                  </div>
                  <div className="detail-value">
                    {translateType(session.sessionType)}
                  </div>
                </div>

                <div className="detail-item">
                  <div className="detail-label">Tipo de Cuidado:</div>
                  <div className="detail-value">
                    {translateType(session.careType)}
                  </div>
                </div>

                {session.bedriddenPatient && (
                  <div className="detail-item warning">
                    <FontAwesomeIcon icon={faBed} />
                    <span>Paciente Acamado</span>
                  </div>
                )}
              </div>
            </div>

            {/* Tabs Section */}
            <div className="tabs-section">
              <div className="tabs-header">
                <button
                  className={`tab-btn ${activeTab === 'info' ? 'active' : ''}`}
                  onClick={() => setActiveTab('info')}
                >
                  Informações
                </button>
                <button
                  className={`tab-btn ${activeTab === 'procedures' ? 'active' : ''}`}
                  onClick={() => setActiveTab('procedures')}
                >
                  Procedimentos
                </button>
                <button
                  className={`tab-btn ${activeTab === 'notes' ? 'active' : ''}`}
                  onClick={() => setActiveTab('notes')}
                >
                  Anotações
                </button>
              </div>

              <div className="tab-content">
                {activeTab === 'info' && (
                  <div className="info-tab">
                    {session.clinicalNotes && (
                      <div className="clinical-notes">
                        <h3>
                          <FontAwesomeIcon icon={faNotesMedical} />
                          Observações Clínicas
                        </h3>
                        <p>{session.clinicalNotes}</p>
                      </div>
                    )}

                    {session.clinicalNotes && (
                      <div className="medical-history">
                        <h3>Histórico Médico</h3>
                        <p>{session.clinicalNotes}</p>
                      </div>
                    )}
                  </div>
                )}

                {activeTab === 'procedures' && (
                  <div className="procedures-tab">
                    <div className="procedures-list">
                      {/* Aqui você pode adicionar uma lista de procedimentos */}
                      <p>Procedimentos serão listados aqui...</p>
                    </div>
                    <button className="add-procedure-btn">
                      Adicionar Procedimento
                    </button>
                  </div>
                )}

                {activeTab === 'notes' && (
                  <div className="notes-tab">
                    <textarea
                      className="notes-textarea"
                      placeholder="Adicione anotações sobre a sessão..."
                      rows={6}
                    />
                    <button className="save-notes-btn">Salvar Anotações</button>
                  </div>
                )}
              </div>
            </div>

            {/* Time Tracker (visible only when session is in progress) */}
            {session.status === 'IN_PROGRESS' && (
              <div className="time-tracker">
                <div className="timer">
                  <div className="timer-label">Tempo de Sessão:</div>
                  <div className="timer-value">00:45:23</div>
                </div>
                <div className="estimated-time">
                  <div className="estimated-label">Tempo Estimado:</div>
                  <div className="estimated-value">60 minutos</div>
                </div>
              </div>
            )}
          </div>
        </div>
      </ProtectedRoute>
    </Layout>
  )
}
