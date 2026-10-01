'use client'

import Notificacao from './ToastNotification'
import './ToastContainer.css'
import { useNotificacao } from '@/contexts/ToastContext'

export default function ConteinerNotificacoes() {
  const { notificacoes, removerNotificacao } = useNotificacao()

  if (notificacoes.length === 0) return null

  return (
    <div className="toast-container">
      {notificacoes.map(notificacao => (
        <Notificacao
          key={notificacao.id}
          mensagem={notificacao.mensagem}
          tipo={notificacao.tipo}
          duracao={notificacao.duracao}
          aoFechar={() => removerNotificacao(notificacao.id)}
        />
      ))}
    </div>
  )
}
