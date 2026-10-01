'use client'

import { AlertTriangle } from 'lucide-react'
import Modal from '@/components/Modal'
import './ModalConfirmacao.css'

interface ModalConfirmacaoProps {
  mensagem: string
  titulo?: string
  textoBotaoConfirmar?: string
  textoBotaoCancelar?: string
  variante?: 'perigo' | 'aviso'
  onConfirmar: () => void
  onCancelar: () => void
}

export default function ModalConfirmacao({
  mensagem,
  titulo = 'Confirmar ação',
  textoBotaoConfirmar = 'Confirmar',
  textoBotaoCancelar = 'Cancelar',
  variante = 'perigo',
  onConfirmar,
  onCancelar,
}: ModalConfirmacaoProps) {
  return (
    <Modal
      titulo={
        <>
          <AlertTriangle size={18} strokeWidth={2} />
          {titulo}
        </>
      }
      onClose={onCancelar}
      tamanho="sm"
      rodape={
        <>
          <button className="confirmacao-btn-cancelar" onClick={onCancelar}>
            {textoBotaoCancelar}
          </button>
          <button
            className={`confirmacao-btn-confirmar confirmacao-btn-confirmar--${variante}`}
            onClick={onConfirmar}
          >
            {textoBotaoConfirmar}
          </button>
        </>
      }
    >
      <p className="confirmacao-mensagem">{mensagem}</p>
    </Modal>
  )
}
