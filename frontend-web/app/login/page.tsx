// page.tsx
'use client'

import { useState, FormEvent } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { useNotificacao } from '../../contexts/ToastContext'
import { LoginRequisicao } from '@/tipos/autenticacao'

export default function Login() {
  const [username, setUsername] = useState('')
  const [senha, setSenha] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const { login, usuario, isLoading: authLoading } = useAuth()
  const { exibirNotificacao } = useNotificacao()

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setIsLoading(true)

    try {
      const credenciais: LoginRequisicao = {
        username: username.trim(),
        senha: senha,
      }
      await login(credenciais)
    } catch (err: any) {
      exibirNotificacao(err.message || 'Erro ao fazer login', 'error', 6000)
    } finally {
      setIsLoading(false)
    }
  }

  if (authLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#f4f9ff]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-[#2b6cb0] mx-auto"></div>
          <p className="mt-3 text-sm text-[#2b6cb0] font-medium">
            Verificando sessão...
          </p>
        </div>
      </div>
    )
  }

  if (usuario) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#f4f9ff]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-[#2b6cb0] mx-auto"></div>
          <p className="mt-3 text-sm text-[#2b6cb0] font-medium">
            Redirecionando...
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-[#f4f9ff] to-[#e6f4f1] p-4 md:p-8">
      <div className="flex flex-col md:flex-row w-full max-w-6xl bg-white rounded-3xl shadow-2xl overflow-hidden">
        <div className="hidden md:flex md:w-1/2 bg-gradient-to-br from-[#2b6cb0] to-[#7991bc] p-8 md:p-12 flex-col justify-center items-center text-center md:text-left">
          <div className="max-w-md mx-auto">
            {/* Logo / Ícone Principal */}
            <div className="mb-6 flex justify-center md:justify-start">
              <div className="w-20 h-20 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center shadow-lg">
                <svg
                  className="w-12 h-12 text-white"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                  />
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M9 12.75L11.25 15 15 9.75"
                  />
                </svg>
              </div>
            </div>

            <h1 className="text-4xl md:text-5xl font-bold text-white mb-4">
              Prontu<span className="text-[#e6f4f1]">Digital</span>
            </h1>
            <p className="text-white/90 text-lg mb-6 leading-relaxed">
              Sistema inteligente para agendamento de tratamentos de enfermagem
            </p>
            <div className="space-y-3 text-white/80 text-sm">
              <div className="flex items-center gap-3 justify-center md:justify-start">
                <svg
                  className="w-5 h-5 text-[#e6f4f1]"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
                <span>Gestão completa de pacientes</span>
              </div>
              <div className="flex items-center gap-3 justify-center md:justify-start">
                <svg
                  className="w-5 h-5 text-[#e6f4f1]"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                  />
                </svg>
                <span>Controle de horários e procedimentos</span>
              </div>
              <div className="flex items-center gap-3 justify-center md:justify-start">
                <svg
                  className="w-5 h-5 text-[#e6f4f1]"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>
                <span>Prontuário digital seguro</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right Side - Formulário de Login (sempre visível, ocupa toda largura no mobile) */}
        <div className="w-full md:w-1/2 p-8 md:p-12 flex items-center justify-center bg-white">
          <div className="w-full max-w-md">
            <div className="text-center mb-8">
              <h2 className="text-3xl font-bold text-[#2b6cb0]">
                Bem-vindo(a)
              </h2>
              <p className="text-gray-500 mt-2">
                Acesse sua conta para continuar
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label
                  htmlFor="username"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Usuário
                </label>
                <input
                  id="username"
                  type="text"
                  required
                  className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-[#7991bc] focus:border-[#7991bc] transition-all outline-none"
                  placeholder="Digite seu usuário"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  autoComplete="username"
                />
              </div>

              <div>
                <label
                  htmlFor="senha"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Senha
                </label>
                <input
                  id="senha"
                  type="password"
                  required
                  className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-[#7991bc] focus:border-[#7991bc] transition-all outline-none"
                  placeholder="Digite sua senha"
                  value={senha}
                  onChange={e => setSenha(e.target.value)}
                  autoComplete="current-password"
                />
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full flex justify-center items-center gap-2 py-3 px-4 bg-[#2b6cb0] hover:bg-[#1e4f82] text-white font-semibold rounded-xl shadow-md hover:shadow-lg transition-all duration-200 disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {isLoading ? (
                  <>
                    <svg
                      className="animate-spin h-5 w-5 text-white"
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      ></circle>
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      ></path>
                    </svg>
                    Entrando...
                  </>
                ) : (
                  'Entrar no sistema'
                )}
              </button>
            </form>

            <div className="mt-8 pt-6 border-t border-gray-100 text-center">
              <p className="text-xs text-gray-400">
                Sistema de agendamento de tratamentos de enfermagem • Segurança
                e confiabilidade
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
