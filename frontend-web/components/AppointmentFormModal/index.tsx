'use client'

import { useState } from 'react'
import { AgendamentoRequisicao } from '@/tipos/appointment'
import './styles.css'

interface PropsModalFormularioAgendamento {
  aberto: boolean
  aoFechar: () => void
  aoEnviar: (dados: AgendamentoRequisicao) => void
  profissionalUuid?: string
}

const formularioInicial = (
  profissionalUuid?: string,
): Partial<AgendamentoRequisicao> => ({
  pacienteUuid: '',
  profissionalUuid: profissionalUuid ?? '',
  inicioEm: '',
  fimEm: '',
  tipo: 'AVALIACAO',
  observacoes: '',
  avaliacaoId: undefined,
})

export default function ModalFormularioAgendamento({
  aberto,
  aoFechar,
  aoEnviar,
  profissionalUuid,
}: PropsModalFormularioAgendamento) {
  const [dadosFormulario, setDadosFormulario] = useState<
    Partial<AgendamentoRequisicao>
  >(formularioInicial(profissionalUuid))
  const [erros, setErros] = useState<Record<string, string>>({})

  const aoAlterar = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >,
  ) => {
    const { name, value } = e.target
    setDadosFormulario(prev => ({
      ...prev,
      [name]:
        name === 'avaliacaoId' ? (value ? Number(value) : undefined) : value,
    }))
    if (erros[name]) setErros(prev => ({ ...prev, [name]: '' }))
  }

  const validarFormulario = (): boolean => {
    const novosErros: Record<string, string> = {}

    if (!dadosFormulario.pacienteUuid?.trim())
      novosErros.pacienteUuid = 'UUID do paciente é obrigatório'
    if (!dadosFormulario.profissionalUuid?.trim())
      novosErros.profissionalUuid = 'UUID do profissional é obrigatório'
    if (!dadosFormulario.inicioEm)
      novosErros.inicioEm = 'Data/hora de início é obrigatória'
    if (!dadosFormulario.fimEm)
      novosErros.fimEm = 'Data/hora de término é obrigatória'
    if (dadosFormulario.tipo === 'TRATAMENTO' && !dadosFormulario.avaliacaoId)
      novosErros.avaliacaoId = 'ID da avaliação é obrigatório para tratamentos'

    if (dadosFormulario.inicioEm && dadosFormulario.fimEm) {
      if (new Date(dadosFormulario.fimEm) <= new Date(dadosFormulario.inicioEm))
        novosErros.fimEm = 'Término deve ser após o início'
    }

    setErros(novosErros)
    return Object.keys(novosErros).length === 0
  }

  const aoSubmeter = (e: React.FormEvent) => {
    e.preventDefault()
    if (!validarFormulario()) return

    aoEnviar(dadosFormulario as AgendamentoRequisicao)
    setDadosFormulario(formularioInicial(profissionalUuid))
    setErros({})
  }

  const aoCancelar = () => {
    setDadosFormulario(formularioInicial(profissionalUuid))
    setErros({})
    aoFechar()
  }

  if (!aberto) return null

  return (
    <div className="modal-overlay" onClick={aoCancelar}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="text-xl font-bold text-[#2b6cb0]">Novo Agendamento</h2>
          <button
            onClick={aoCancelar}
            className="text-gray-400 hover:text-gray-600"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-6 w-6"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <form onSubmit={aoSubmeter} className="modal-form">
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="pacienteUuid" className="form-label">
                UUID do Paciente *
              </label>
              <input
                type="text"
                id="pacienteUuid"
                name="pacienteUuid"
                value={dadosFormulario.pacienteUuid || ''}
                onChange={aoAlterar}
                className={`form-input ${erros.pacienteUuid ? 'border-red-500' : ''}`}
                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              />
              {erros.pacienteUuid && (
                <span className="error-message">{erros.pacienteUuid}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="profissionalUuid" className="form-label">
                UUID do Profissional *
              </label>
              <input
                type="text"
                id="profissionalUuid"
                name="profissionalUuid"
                value={dadosFormulario.profissionalUuid || ''}
                onChange={aoAlterar}
                readOnly={!!profissionalUuid}
                className={`form-input ${profissionalUuid ? 'bg-gray-100 cursor-not-allowed' : ''} ${erros.profissionalUuid ? 'border-red-500' : ''}`}
                placeholder="Preenchido automaticamente"
              />
              {erros.profissionalUuid && (
                <span className="error-message">{erros.profissionalUuid}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="inicioEm" className="form-label">
                Data/Hora Início *
              </label>
              <input
                type="datetime-local"
                id="inicioEm"
                name="inicioEm"
                value={dadosFormulario.inicioEm || ''}
                onChange={aoAlterar}
                className={`form-input ${erros.inicioEm ? 'border-red-500' : ''}`}
              />
              {erros.inicioEm && (
                <span className="error-message">{erros.inicioEm}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="fimEm" className="form-label">
                Data/Hora Término *
              </label>
              <input
                type="datetime-local"
                id="fimEm"
                name="fimEm"
                value={dadosFormulario.fimEm || ''}
                onChange={aoAlterar}
                className={`form-input ${erros.fimEm ? 'border-red-500' : ''}`}
              />
              {erros.fimEm && (
                <span className="error-message">{erros.fimEm}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="tipo" className="form-label">
                Tipo *
              </label>
              <select
                id="tipo"
                name="tipo"
                value={dadosFormulario.tipo || 'AVALIACAO'}
                onChange={aoAlterar}
                className="form-input"
              >
                <option value="AVALIACAO">Avaliação</option>
                <option value="TRATAMENTO">Tratamento</option>
              </select>
            </div>

            {dadosFormulario.tipo === 'TRATAMENTO' && (
              <div className="form-group">
                <label htmlFor="avaliacaoId" className="form-label">
                  ID da Avaliação de Origem *
                </label>
                <input
                  type="number"
                  id="avaliacaoId"
                  name="avaliacaoId"
                  value={dadosFormulario.avaliacaoId ?? ''}
                  onChange={aoAlterar}
                  className={`form-input ${erros.avaliacaoId ? 'border-red-500' : ''}`}
                  placeholder="ID do agendamento de avaliação"
                  min={1}
                />
                {erros.avaliacaoId && (
                  <span className="error-message">{erros.avaliacaoId}</span>
                )}
              </div>
            )}

            <div className="form-group col-span-2">
              <label htmlFor="observacoes" className="form-label">
                Observações
              </label>
              <textarea
                id="observacoes"
                name="observacoes"
                value={dadosFormulario.observacoes || ''}
                onChange={aoAlterar}
                className="form-input"
                placeholder="Observações opcionais sobre o agendamento"
                rows={3}
              />
            </div>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              onClick={aoCancelar}
              className="btn-secondary"
            >
              Cancelar
            </button>
            <button type="submit" className="btn-primary">
              Criar Agendamento
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
