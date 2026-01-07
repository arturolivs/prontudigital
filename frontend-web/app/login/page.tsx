'use client'

import { useState, FormEvent } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import './styles.css'
import { useToast } from '../../contexts/ToastContext'

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const { login, user, isLoading: authLoading } = useAuth()
  const { showToast } = useToast()

  const handleSubmit = async (e: FormEvent) => {
    showToast('erro', 'info', 6000)
    showToast('err.message', 'error', 6000)
    showToast('err.message', 'success', 6000)
    showToast('err.message', 'warning', 6000)
    e.preventDefault()
    setIsLoading(true)

    try {
      await login({ username, password })
    } catch (err: any) {
      showToast(err.message, 'info', 6000)
      showToast(err.message, 'error', 6000)
      showToast(err.message, 'success', 6000)
      showToast(err.message, 'warning', 6000)
    } finally {
      setIsLoading(false)
    }
  }

  if (authLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-2 text-sm text-gray-600">Verificando sessão...</p>
        </div>
      </div>
    )
  }

  if (user) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-2 text-sm text-gray-600">Redirecionando...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="login-page">
      <form className="login-form" onSubmit={handleSubmit}>
        <input
          type="text"
          required
          className="form-input"
          placeholder="Usuário"
          value={username}
          onChange={e => setUsername(e.target.value)}
        />
        <input
          type="password"
          required
          className="form-input"
          placeholder="Senha"
          value={password}
          onChange={e => setPassword(e.target.value)}
        />

        <button
          type="submit"
          disabled={isLoading}
          className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
        >
          {isLoading ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </div>
  )
}
