'use client'

import { useState, useEffect } from 'react'
import Modal from '@/components/Modal'
import { usuariosAPI } from '@/lib/usuario.service'
import { AgendamentoRequisicao } from '@/tipos/agendamento'
import { TipoProcedimento, ROTULO_TIPO_PROCEDIMENTO } from '@/tipos/TipoProcedimento'
import { LocalAtendimento, ROTULO_LOCAL_ATENDIMENTO } from '@/tipos/LocalAtendimento'
import { Usuario } from '@/tipos/autenticacao'
import './ModalNovoAgendamento.css'

interface PrefillNovoAgendamento {
  pacienteUuid?: string
  profissionalUuid?: string
  avaliacaoId?: number
  tipo?: 'AVALIACAO' | 'TRATAMENTO'
  tipoProcedimento?: TipoProcedimento
  localAtendimento?: LocalAtendimento
  pacienteAcamado?: boolean
}

interface Props {
  onClose: () => void
  onSalvar: (dados: AgendamentoRequisicao) => Promise<void>
  profissionalUuidAtual: string
  isAdmin: boolean
  prefill?: PrefillNovoAgendamento
}

const hojeISO = (): string => new Date().toISOString().split('T')[0]

export default function ModalNovoAgendamento({
  onClose,
  onSalvar,
  profissionalUuidAtual,
  isAdmin,
  prefill,
}: Props) {
  const [pacientes, setPacientes] = useState<Usuario[]>([])
  const [profissionais, setProfissionais] = useState<Usuario[]>([])
  const [carregandoUsuarios, setCarregandoUsuarios] = useState(true)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')

  const [pacienteUuid, setPacienteUuid] = useState(prefill?.pacienteUuid ?? '')
  const [profissionalUuid, setProfissionalUuid] = useState(
    prefill?.profissionalUuid ?? (isAdmin ? '' : profissionalUuidAtual),
  )
  const [data, setData] = useState(hojeISO())
  const [horaInicio, setHoraInicio] = useState('08:00')
  const [horaFim, setHoraFim] = useState('09:00')
  const [tipo, setTipo] = useState<'AVALIACAO' | 'TRATAMENTO'>(prefill?.tipo ?? 'AVALIACAO')
  const [tipoProcedimento, setTipoProcedimento] = useState<TipoProcedimento | ''>(prefill?.tipoProcedimento ?? '')
  const [localAtendimento, setLocalAtendimento] = useState<LocalAtendimento | ''>(prefill?.localAtendimento ?? '')
  const [pacienteAcamado, setPacienteAcamado] = useState<boolean | null>(prefill?.pacienteAcamado ?? null)
  const [avaliacaoId, setAvaliacaoId] = useState(prefill?.avaliacaoId?.toString() ?? '')
  const [observacoes, setObservacoes] = useState('')

  useEffect(() => {
    usuariosAPI
      .listarUsuarios()
      .then(todos => {
        setPacientes(todos.filter(u => u.perfis.includes('ROLE_PACIENTE')))
        setProfissionais(
          todos.filter(u => u.perfis.includes('ROLE_PROFISSIONAL')),
        )
      })
      .catch(() => setErro('Não foi possível carregar os usuários.'))
      .finally(() => setCarregandoUsuarios(false))
  }, [])

  const podeSubmeter =
    pacienteUuid &&
    profissionalUuid &&
    data &&
    horaInicio &&
    horaFim &&
    tipoProcedimento &&
    localAtendimento &&
    pacienteAcamado !== null &&
    (tipo === 'AVALIACAO' || avaliacaoId)

  const handleSubmit = async () => {
    if (!podeSubmeter) return
    setErro('')
    setSalvando(true)
    try {
      const dados: AgendamentoRequisicao = {
        pacienteUuid,
        profissionalUuid,
        inicioEm: `${data}T${horaInicio}:00`,
        fimEm: `${data}T${horaFim}:00`,
        tipo,
        tipoProcedimento: tipoProcedimento as TipoProcedimento,
        localAtendimento: localAtendimento as LocalAtendimento,
        pacienteAcamado: pacienteAcamado!,
        observacoes: observacoes.trim() || undefined,
        avaliacaoId:
          tipo === 'TRATAMENTO' && avaliacaoId
            ? Number(avaliacaoId)
            : undefined,
      }
      await onSalvar(dados)
    } catch (err: any) {
      setErro(
        err?.response?.data?.message || 'Erro ao criar agendamento.',
      )
    } finally {
      setSalvando(false)
    }
  }

  const rodape = (
    <>
      <button className="mna-btn-cancelar" onClick={onClose} disabled={salvando}>
        Cancelar
      </button>
      <button
        className="mna-btn-salvar"
        onClick={handleSubmit}
        disabled={!podeSubmeter || salvando}
      >
        {salvando ? 'Agendando…' : 'Confirmar agendamento'}
      </button>
    </>
  )

  return (
    <Modal titulo="Novo agendamento" tamanho="md" onClose={onClose} rodape={rodape}>
      {carregandoUsuarios ? (
        <div className="mna-loading">
          <div className="mna-spinner" />
          Carregando…
        </div>
      ) : (
        <div className="mna-form">
          {erro && <div className="mna-erro">{erro}</div>}

          {/* Tipo de agendamento */}
          <div className="mna-grupo">
            <label className="mna-rotulo">Tipo de agendamento</label>
            <div className="mna-opcoes">
              {(['AVALIACAO', 'TRATAMENTO'] as const).map(t => (
                <button
                  key={t}
                  type="button"
                  className={`mna-opcao-btn${tipo === t ? ' ativo' : ''}`}
                  onClick={() => setTipo(t)}
                >
                  {t === 'AVALIACAO' ? 'Avaliação' : 'Tratamento'}
                </button>
              ))}
            </div>
          </div>

          {tipo === 'TRATAMENTO' && (
            <div className="mna-grupo">
              <label className="mna-rotulo" htmlFor="mna-avaliacao">
                ID da avaliação de origem
              </label>
              <input
                id="mna-avaliacao"
                type="number"
                className="mna-input"
                placeholder="Ex: 42"
                value={avaliacaoId}
                onChange={e => setAvaliacaoId(e.target.value)}
                min={1}
              />
            </div>
          )}

          {/* Paciente */}
          <div className="mna-grupo">
            <label className="mna-rotulo" htmlFor="mna-paciente">
              Paciente
            </label>
            <select
              id="mna-paciente"
              className="mna-select"
              value={pacienteUuid}
              onChange={e => setPacienteUuid(e.target.value)}
            >
              <option value="">Selecione o paciente…</option>
              {pacientes.map(p => (
                <option key={p.uuid} value={p.uuid}>
                  {p.nomeCompleto}
                </option>
              ))}
            </select>
          </div>

          {/* Profissional (somente admin) */}
          {isAdmin && (
            <div className="mna-grupo">
              <label className="mna-rotulo" htmlFor="mna-profissional">
                Profissional
              </label>
              <select
                id="mna-profissional"
                className="mna-select"
                value={profissionalUuid}
                onChange={e => setProfissionalUuid(e.target.value)}
              >
                <option value="">Selecione o profissional…</option>
                {profissionais.map(p => (
                  <option key={p.uuid} value={p.uuid}>
                    {p.nomeCompleto}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Data e horários */}
          <div className="mna-grupo">
            <label className="mna-rotulo">Data e horário</label>
            <div className="mna-linha-datas">
              <input
                type="date"
                className="mna-input mna-input-data"
                value={data}
                onChange={e => setData(e.target.value)}
              />
              <input
                type="time"
                className="mna-input mna-input-hora"
                value={horaInicio}
                onChange={e => setHoraInicio(e.target.value)}
              />
              <span className="mna-separador">até</span>
              <input
                type="time"
                className="mna-input mna-input-hora"
                value={horaFim}
                onChange={e => setHoraFim(e.target.value)}
              />
            </div>
          </div>

          {/* Tipo de procedimento */}
          <div className="mna-grupo">
            <label className="mna-rotulo">Tipo de procedimento</label>
            <div className="mna-opcoes">
              {(['PODIATRIA', 'TRATAMENTO_FERIDAS'] as TipoProcedimento[]).map(tp => (
                <button
                  key={tp}
                  type="button"
                  className={`mna-opcao-btn${tipoProcedimento === tp ? ' ativo' : ''}`}
                  onClick={() => setTipoProcedimento(tp)}
                >
                  {ROTULO_TIPO_PROCEDIMENTO[tp]}
                </button>
              ))}
            </div>
          </div>

          {/* Local */}
          <div className="mna-grupo">
            <label className="mna-rotulo">Local de atendimento</label>
            <div className="mna-opcoes">
              {(['CLINICA', 'RESIDENCIAL'] as LocalAtendimento[]).map(l => (
                <button
                  key={l}
                  type="button"
                  className={`mna-opcao-btn${localAtendimento === l ? ' ativo' : ''}`}
                  onClick={() => setLocalAtendimento(l)}
                >
                  {ROTULO_LOCAL_ATENDIMENTO[l]}
                </button>
              ))}
            </div>
          </div>

          {/* Paciente acamado */}
          <div className="mna-grupo">
            <label className="mna-rotulo">Paciente acamado?</label>
            <div className="mna-opcoes">
              {([true, false] as boolean[]).map(v => (
                <button
                  key={String(v)}
                  type="button"
                  className={`mna-opcao-btn${pacienteAcamado === v ? ' ativo' : ''}`}
                  onClick={() => setPacienteAcamado(v)}
                >
                  {v ? 'Sim' : 'Não'}
                </button>
              ))}
            </div>
          </div>

          {/* Observações */}
          <div className="mna-grupo">
            <label className="mna-rotulo" htmlFor="mna-obs">
              Observações <span className="mna-opcional">(opcional)</span>
            </label>
            <textarea
              id="mna-obs"
              className="mna-textarea"
              rows={3}
              placeholder="Informações adicionais sobre o agendamento…"
              value={observacoes}
              onChange={e => setObservacoes(e.target.value)}
              maxLength={500}
            />
          </div>
        </div>
      )}
    </Modal>
  )
}
