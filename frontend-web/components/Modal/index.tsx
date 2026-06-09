'use client'

import { useEffect, ReactNode } from 'react'
import './Modal.css'

interface ModalProps {
  titulo: ReactNode
  onClose: () => void
  children: ReactNode
  rodape?: ReactNode
  tamanho?: 'sm' | 'md' | 'lg'
  semPaddingCorpo?: boolean
  ariaDescribedby?: string
}

export default function Modal({
  titulo,
  onClose,
  children,
  rodape,
  tamanho = 'md',
  semPaddingCorpo = false,
  ariaDescribedby,
}: ModalProps) {
  useEffect(() => {
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [])

  useEffect(() => {
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKey)
    return () => document.removeEventListener('keydown', handleKey)
  }, [onClose])

  return (
    <div
      className="modal-gen-overlay"
      onClick={e => e.target === e.currentTarget && onClose()}
      role="presentation"
    >
      <div
        className={`modal-gen modal-gen--${tamanho}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-gen-titulo"
        aria-describedby={ariaDescribedby}
      >
        <div className="modal-gen-header">
          <h2 className="modal-gen-titulo" id="modal-gen-titulo">
            {titulo}
          </h2>
          <button
            className="modal-gen-fechar"
            onClick={onClose}
            aria-label="Fechar modal"
          >
            ✕
          </button>
        </div>

        <div
          className={`modal-gen-body${semPaddingCorpo ? ' modal-gen-body--sem-padding' : ''}`}
        >
          {children}
        </div>

        {rodape && <div className="modal-gen-footer">{rodape}</div>}
      </div>
    </div>
  )
}
