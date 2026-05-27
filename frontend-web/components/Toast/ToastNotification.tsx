'use client'

import { useEffect, useState, useRef } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  faExclamationTriangle,
  faCircleCheck,
  faCircleInfo,
  faXmark,
} from '@fortawesome/free-solid-svg-icons'
import './ToastNotification.css'

export type TipoNotificacao = 'error' | 'success' | 'info' | 'warning'

interface PropsNotificacao {
  mensagem: string
  tipo?: TipoNotificacao
  duracao?: number
  aoFechar?: () => void
  aberto?: boolean
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
  const refAnimacao = useRef<number>(0)

  useEffect(() => {
    if (abertoExterno !== undefined) {
      setVisivel(abertoExterno)
    }
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
      const temporizador = setTimeout(() => {
        fechar()
      }, duracao)

      return () => clearTimeout(temporizador)
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

  const obterIcone = () => {
    switch (tipo) {
      case 'error':
        return faExclamationTriangle
      case 'success':
        return faCircleCheck
      case 'warning':
        return faExclamationTriangle
      case 'info':
        return faCircleInfo
      default:
        return faExclamationTriangle
    }
  }

  const classesNotificacao = `toast-notification ${saindo ? 'exiting' : ''}`
  const classesConteudo = `toast-content toast-${tipo}`

  return (
    <div className={classesNotificacao}>
      <div className={classesConteudo}>
        <div className="toast-icon">
          <FontAwesomeIcon icon={obterIcone()} />
        </div>

        <p className="toast-message">{mensagem}</p>

        <button
          type="button"
          onClick={fechar}
          className="toast-close-btn"
          aria-label="Fechar notificação"
        >
          <FontAwesomeIcon icon={faXmark} className="toast-close-icon" />
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
