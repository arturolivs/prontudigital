'use client'

import { useEffect, ReactNode } from 'react'
import './Modal.css'

interface ModalProps {
  /** Texto exibido no cabeçalho */
  titulo: string
  /** Chamado ao fechar (X, ESC ou clique no overlay) */
  onClose: () => void
  /** Conteúdo do corpo do modal */
  children: ReactNode
  /** Botões ou conteúdo fixo no rodapé (opcional) */
  rodape?: ReactNode
  /** Largura máxima: sm = 400px | md = 520px (padrão) | lg = 720px */
  tamanho?: 'sm' | 'md' | 'lg'
  /** ID do elemento que descreve o modal para acessibilidade */
  ariaDescribedby?: string
}

export default function Modal({
  titulo,
  onClose,
  children,
  rodape,
  tamanho = 'md',
  ariaDescribedby,
}: ModalProps) {
  // Trava o scroll do body enquanto o modal está montado
  useEffect(() => {
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [])

  // Fecha ao pressionar ESC
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

        <div className="modal-gen-body">{children}</div>

        {rodape && <div className="modal-gen-footer">{rodape}</div>}
      </div>
    </div>
  )
}
