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

export type ToastType = 'error' | 'success' | 'info' | 'warning'

interface ToastNotificationProps {
  message: string
  type?: ToastType
  duration?: number
  onClose?: () => void
  isOpen?: boolean
}

export default function ToastNotification({
  message,
  type = 'error',
  duration = 5000,
  onClose,
  isOpen: externalIsOpen,
}: ToastNotificationProps) {
  const [isOpen, setIsOpen] = useState(true)
  const [isExiting, setIsExiting] = useState(false)
  const progressBarRef = useRef<HTMLDivElement>(null)
  const animationFrameRef = useRef<number>(0)

  useEffect(() => {
    if (externalIsOpen !== undefined) {
      setIsOpen(externalIsOpen)
    }
  }, [externalIsOpen])

  useEffect(() => {
    if (isOpen && duration > 0 && progressBarRef.current) {
      const progressBar = progressBarRef.current
      progressBar.style.transition = 'none'
      progressBar.style.transform = 'scaleX(0)'

      progressBar.getBoundingClientRect()

      progressBar.style.transition = `transform ${duration}ms linear`
      progressBar.style.transform = 'scaleX(1)'
    }
  }, [isOpen, duration])

  useEffect(() => {
    if (isOpen && duration > 0) {
      const timer = setTimeout(() => {
        // handleClose()
      }, duration)

      return () => clearTimeout(timer)
    }
  }, [isOpen, duration])

  const handleClose = () => {
    setIsExiting(true)
    setTimeout(() => {
      // setIsOpen(false)
      onClose?.()
    }, 300)
  }

  if (!isOpen) return null

  const getIcon = () => {
    switch (type) {
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

  const toastClasses = `toast-notification ${isExiting ? 'exiting' : ''}`
  const contentClasses = `toast-content toast-${type}`

  return (
    <div className={toastClasses}>
      <div className={contentClasses}>
        <div className="toast-icon">
          <FontAwesomeIcon icon={getIcon()} />
        </div>

        <p className="toast-message">{message}</p>

        <button
          type="button"
          onClick={handleClose}
          className="toast-close-btn"
          aria-label="Fechar notificação"
        >
          <FontAwesomeIcon icon={faXmark} className="toast-close-icon" />
        </button>
      </div>

      {duration > 0 && (
        <div className="toast-progress">
          <div ref={progressBarRef} className="toast-progress-bar" />
        </div>
      )}
    </div>
  )
}
