import React, { useMemo } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  faTriangleExclamation,
  faCalendarXmark,
  faClock,
  faHourglass,
} from '@fortawesome/free-solid-svg-icons'
import { CareSession } from '@/types/CareSession'

export type GroupedSessions = Record<string, CareSession[]>

interface CareSessionProps {
  sessions?: CareSession[]
  loading?: boolean
  error?: string | null
}

const careTypeLabel: Record<string, string> = {
  PODEATRIA: 'Podiatria',
  TRATAMENTO_FERIDAS: 'Tratamento de feridas',
}

const sessionTypeLabel: Record<string, string> = {
  AVALIACAO: 'Avaliação',
  TRATAMENTO: 'Tratamento',
}

const statusBadgeClass: Record<string, string> = {
  AVALIACAO: 'bg-emerald-50 text-emerald-800 border border-emerald-200',
  TRATAMENTO: 'bg-blue-50 text-blue-800 border border-blue-200',
}

const borderAccentClass: Record<string, string> = {
  PODEATRIA: 'border-l-emerald-500',
  TRATAMENTO_FERIDAS: 'border-l-blue-500',
}

// ─── Duration ────────────────────────────────────────────────────────────────

const DurationDisplay: React.FC<{ start: string; end: string }> = ({
  start,
  end,
}) => {
  const duration = useMemo(() => {
    const diffMins = Math.floor(
      (new Date(end).getTime() - new Date(start).getTime()) / 60000,
    )
    if (diffMins < 60) return `${diffMins} min`
    const h = Math.floor(diffMins / 60)
    const m = diffMins % 60
    return m > 0 ? `${h}h${m}min` : `${h}h`
  }, [start, end])

  return (
    <span className="inline-flex items-center gap-1 text-xs text-gray-400">
      <FontAwesomeIcon icon={faHourglass} className="w-3 h-3" />
      {duration}
    </span>
  )
}

// ─── Status Badge ─────────────────────────────────────────────────────────────

const StatusBadge: React.FC<{ sessionType: string }> = ({ sessionType }) => {
  const label = sessionTypeLabel[sessionType] ?? sessionType
  const cls =
    statusBadgeClass[sessionType] ??
    'bg-gray-100 text-gray-600 border border-gray-200'

  return (
    <span
      className={`inline-flex items-center justify-center rounded-full px-3 py-1 text-xs font-semibold tracking-wide uppercase whitespace-nowrap ${cls}`}
    >
      {label}
    </span>
  )
}

// ─── Date Header ──────────────────────────────────────────────────────────────

const DateHeader: React.FC<{ dateKey: string; count: number }> = ({
  dateKey,
  count,
}) => {
  const label = useMemo(() => {
    const date = new Date(dateKey + 'T00:00:00')
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const tomorrow = new Date(today)
    tomorrow.setDate(today.getDate() + 1)

    if (date.toDateString() === today.toDateString()) return 'Hoje'
    if (date.toDateString() === tomorrow.toDateString()) return 'Amanhã'

    return date.toLocaleDateString('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    })
  }, [dateKey])

  return (
    <div className="flex items-center justify-between py-2">
      <div className="flex items-center gap-2">
        <FontAwesomeIcon
          icon={faCalendarXmark}
          className="w-4 h-4 text-gray-400"
        />
        <span className="text-sm font-semibold text-gray-700 capitalize">
          {label}
        </span>
      </div>
      <span className="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
        {count} agendamento{count !== 1 ? 's' : ''}
      </span>
    </div>
  )
}

// ─── Main Component ───────────────────────────────────────────────────────────

const CareSessionList: React.FC<CareSessionProps> = ({
  sessions = [],
  loading = false,
  error = null,
}) => {
  const formatTime = (iso: string) =>
    new Date(iso).toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
    })

  const groupedSessions = useMemo(() => {
    const grouped: GroupedSessions = {}

    sessions.forEach(session => {
      const key = new Date(session.scheduledStart).toISOString().split('T')[0]
      if (!grouped[key]) grouped[key] = []
      grouped[key].push(session)
    })

    return Object.fromEntries(
      Object.keys(grouped)
        .sort()
        .map(date => [
          date,
          grouped[date].sort(
            (a, b) =>
              new Date(a.scheduledStart).getTime() -
              new Date(b.scheduledStart).getTime(),
          ),
        ]),
    ) as GroupedSessions
  }, [sessions])

  const dateKeys = Object.keys(groupedSessions)
  const hasSessions = dateKeys.length > 0

  // ── Loading ──
  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20 text-gray-500">
        <div className="w-8 h-8 rounded-full border-2 border-blue-600 border-t-transparent animate-spin" />
        <p className="text-sm">Carregando agendamentos...</p>
      </div>
    )
  }

  // ── Error ──
  if (error) {
    return (
      <div className="flex items-start gap-4 rounded-xl border border-red-200 bg-red-50 p-4">
        <FontAwesomeIcon
          icon={faTriangleExclamation}
          className="w-5 h-5 text-red-500 mt-0.5 shrink-0"
        />
        <div className="min-w-0">
          <h3 className="text-sm font-semibold text-red-800 mb-1">
            Erro ao carregar agendamentos
          </h3>
          <p className="text-sm text-red-700 mb-3">{error}</p>
          <button
            className="text-sm font-medium text-white bg-red-600 hover:bg-red-700 transition-colors rounded-lg px-4 py-1.5"
            onClick={() => window.location.reload()}
          >
            Tentar novamente
          </button>
        </div>
      </div>
    )
  }

  // ── Empty ──
  if (!hasSessions) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-20 text-center">
        <FontAwesomeIcon
          icon={faCalendarXmark}
          className="w-10 h-10 text-gray-300"
        />
        <h3 className="text-base font-semibold text-gray-600">
          Nenhum agendamento encontrado
        </h3>
        <p className="text-sm text-gray-400 max-w-xs">
          Não há agendamentos para a data selecionada.
        </p>
      </div>
    )
  }

  // ── List ──
  return (
    <div className="flex flex-col gap-6 px-1 sm:px-0">
      {/* Global header */}
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-bold text-gray-900">Agendas</h1>
        <span className="text-xs text-gray-500">
          {sessions.length} agendamento{sessions.length !== 1 ? 's' : ''} no
          total
        </span>
      </div>

      {/* Groups */}
      {dateKeys.map(dateKey => (
        <section key={dateKey} className="flex flex-col gap-3">
          <DateHeader
            dateKey={dateKey}
            count={groupedSessions[dateKey].length}
          />

          <div className="flex flex-col gap-2">
            {groupedSessions[dateKey].map(session => {
              const accent =
                borderAccentClass[session.careType] ?? 'border-l-gray-300'

              return (
                <div
                  key={session.id}
                  className={`
                    group flex flex-col sm:flex-row sm:items-center gap-3
                    rounded-xl border border-gray-100 border-l-4 ${accent}
                    bg-white px-4 py-3
                    shadow-sm hover:shadow-md hover:scale-[1.01]
                    transition-all duration-200 cursor-pointer
                  `}
                >
                  {/* Horário */}
                  <div className="flex sm:flex-col items-center sm:items-start gap-3 sm:gap-0.5 shrink-0 sm:min-w-[100px]">
                    <span className="text-sm font-bold text-gray-800 tabular-nums">
                      {formatTime(session.scheduledStart)}
                    </span>
                    <span className="text-xs text-gray-400 hidden sm:block">
                      até
                    </span>
                    <span className="text-xs text-gray-500 tabular-nums sm:block">
                      {formatTime(session.scheduledEnd)}
                    </span>
                    <DurationDisplay
                      start={session.scheduledStart}
                      end={session.scheduledEnd}
                    />
                  </div>

                  {/* Divisor vertical (desktop) */}
                  <div className="hidden sm:block w-px self-stretch bg-gray-100" />

                  {/* Paciente */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-gray-900 truncate">
                      {session.patientName}
                    </p>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {careTypeLabel[session.careType] ?? session.careType}
                    </p>
                  </div>

                  {/* Badge */}
                  <div className="self-start sm:self-center">
                    <StatusBadge sessionType={session.sessionType} />
                  </div>
                </div>
              )
            })}
          </div>
        </section>
      ))}
    </div>
  )
}

export default CareSessionList
