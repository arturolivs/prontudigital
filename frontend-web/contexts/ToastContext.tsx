'use client'

import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  ReactNode,
} from 'react'

type TipoNotificacao = 'error' | 'success' | 'info' | 'warning'

interface Notificacao {
  id: string
  mensagem: string
  tipo: TipoNotificacao
  duracao?: number
}

interface TipoContextoNotificacao {
  notificacoes: Notificacao[]
  exibirNotificacao: (mensagem: string, tipo?: TipoNotificacao, duracao?: number) => string
  removerNotificacao: (id: string) => void
  limparNotificacoes: () => void
}

const ContextoNotificacao = createContext<TipoContextoNotificacao | undefined>(undefined)

export function ProvedorNotificacao({ children }: { children: ReactNode }) {
  const [notificacoes, setNotificacoes] = useState<Notificacao[]>([])

  const exibirNotificacao = useCallback(
    (mensagem: string, tipo: TipoNotificacao = 'error', duracao: number = 5000) => {
      const id = Math.random().toString(36).substring(2, 9)

      setNotificacoes(prev => [...prev, { id, mensagem, tipo, duracao }])

      if (duracao > 0) {
        setTimeout(() => {
          removerNotificacao(id)
        }, duracao)
      }

      return id
    },
    [],
  )

  const removerNotificacao = useCallback((id: string) => {
    setNotificacoes(prev => prev.filter(n => n.id !== id))
  }, [])

  const limparNotificacoes = useCallback(() => {
    setNotificacoes([])
  }, [])

  return (
    <ContextoNotificacao.Provider
      value={{ notificacoes, exibirNotificacao, removerNotificacao, limparNotificacoes }}
    >
      {children}
    </ContextoNotificacao.Provider>
  )
}

export function useNotificacao() {
  const contexto = useContext(ContextoNotificacao)
  if (contexto === undefined) {
    throw new Error('useNotificacao deve ser usado dentro de ProvedorNotificacao')
  }
  return contexto
}
