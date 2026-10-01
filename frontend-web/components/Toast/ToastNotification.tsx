'use client'

import { useEffect, useState, useRef } from 'react'
import { AlertTriangle, CheckCircle2, Info, X } from 'lucide-react'
import './ToastNotification.css'

export type TipoNotificacao = 'error' | 'success' | 'info' | 'warning'

interface PropsNotificacao {
  mensagem: string
  tipo?: TipoNotificacao
  duracao?: number
  aoFechar?: () => void
  aberto?: boolean
}

const ICONES: Record<TipoNotificacao, React.ElementType> = {
  error: AlertTriangle,
  success: CheckCircle2,
  warning: AlertTriangle,
  info: Info,
}

export default function Notificacao({
  mensagem,
  tipo = 'error',
  duracao = 5000,
  aoFechar,
  aberto: abertoExterno,
}: PropsNotificacao) {
  const [visivel, setVisivel] = useState(true)
  const [saindo, setSaindo] = useState(false)
  const refBarraProgresso = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (abertoExterno !== undefined) setVisivel(abertoExterno)
  }, [abertoExterno])

  useEffect(() => {
    if (visivel && duracao > 0 && refBarraProgresso.current) {
      const barra = refBarraProgresso.current
      barra.style.transition = 'none'
      barra.style.transform = 'scaleX(0)'
      barra.getBoundingClientRect()
      barra.style.transition = `transform ${duracao}ms linear`
      barra.style.transform = 'scaleX(1)'
    }
  }, [visivel, duracao])

  useEffect(() => {
    if (visivel && duracao > 0) {
      const t = setTimeout(fechar, duracao)
      return () => clearTimeout(t)
    }
  }, [visivel, duracao])

  const fechar = () => {
    setSaindo(true)
    setTimeout(() => {
      setVisivel(false)
      aoFechar?.()
    }, 300)
  }

  if (!visivel) return null

  const Icone = ICONES[tipo]

  return (
    <div className={`toast-notification${saindo ? ' exiting' : ''}`}>
      <div className={`toast-content toast-${tipo}`}>
        <div className="toast-icon">
          <Icone size={18} strokeWidth={2} />
        </div>

        <p className="toast-message">{mensagem}</p>

        <button
          type="button"
          onClick={fechar}
          className="toast-close-btn"
          aria-label="Fechar notificação"
        >
          <X size={16} className="toast-close-icon" />
        </button>
      </div>

      {duracao > 0 && (
        <div className="toast-progress">
          <div ref={refBarraProgresso} className="toast-progress-bar" />
        </div>
      )}
    </div>
  )
}
