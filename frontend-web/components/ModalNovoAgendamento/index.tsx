'use client'

import { useState, useEffect, useCallback } from 'react'
import Modal from '@/components/Modal'
import SelectAutocomplete, { OpcaoSAC } from '@/components/SelectAutocomplete'
import { usuariosAPI } from '@/lib/usuario.service'
import { procedimentoAPI } from '@/lib/procedimento.service'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import { useNotificacao } from '@/contexts/ToastContext'
import { AgendamentoRequisicao } from '@/tipos/agendamento'
import { Procedimento } from '@/tipos/procedimento'
import { TipoProcedimento } from '@/tipos/TipoProcedimento'
import {
  LocalAtendimento,
  ROTULO_LOCAL_ATENDIMENTO,
} from '@/tipos/LocalAtendimento'
import './ModalNovoAgendamento.css'

interface PrefillNovoAgendamento {
  pacienteUuid?: string
  profissionalUuid?: string
  avaliacaoId?: number
  tipo?: 'AVALIACAO' | 'TRATAMENTO'
  tipoProcedimento?: TipoProcedimento
  procedimentoId?: number
  localAtendimento?: LocalAtendimento
  pacienteAcamado?: boolean
  data?: string
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
  const { exibirNotificacao } = useNotificacao()
  const [salvando, setSalvando] = useState(false)

  const [pacienteUuid, setPacienteUuid] = useState(prefill?.pacienteUuid ?? '')
  const [profissionalUuid, setProfissionalUuid] = useState(
    prefill?.profissionalUuid ?? (isAdmin ? '' : profissionalUuidAtual),
  )
  const [data, setData] = useState(prefill?.data ?? hojeISO())
  const [horaInicio, setHoraInicio] = useState('08:00')
  const [horaFim, setHoraFim] = useState('09:00')
  const [tipo, setTipo] = useState<'AVALIACAO' | 'TRATAMENTO'>(
    prefill?.tipo ?? 'AVALIACAO',
  )
  const [procedimentos, setProcedimentos] = useState<Procedimento[]>([])
  const [procedimentoId, setProcedimentoId] = useState<number | null>(
    prefill?.procedimentoId ?? null,
  )
  const [localAtendimento, setLocalAtendimento] = useState<
    LocalAtendimento | ''
  >(prefill?.localAtendimento ?? '')
  const [pacienteAcamado, setPacienteAcamado] = useState<boolean | null>(
    prefill?.pacienteAcamado ?? null,
  )
  const [avaliacaoId, setAvaliacaoId] = useState(
    prefill?.avaliacaoId?.toString() ?? '',
  )

  useEffect(() => {
    procedimentoAPI
      .listar()
      .then(lista => {
        setProcedimentos(lista)
        // Prefill legado por código do enum (links antigos)
        if (prefill?.tipoProcedimento && !prefill?.procedimentoId) {
          const legado = lista.find(p => p.codigo === prefill.tipoProcedimento)
          if (legado) setProcedimentoId(legado.id)
        }
      })
      .catch(() =>
        exibirNotificacao(MENSAGENS.erro.carregarProcedimentos, 'error', 6000),
      )
  }, [])

  const carregarPacientes = useCallback(async (): Promise<OpcaoSAC[]> => {
    const todos = await usuariosAPI.listarUsuarios(0, 1000)
    return todos.content
      .filter(u => u.perfis.includes('PACIENTE'))
      .map(u => ({ label: u.nomeCompleto, value: u.uuid }))
  }, [])

  const carregarProfissionais = useCallback(async (): Promise<OpcaoSAC[]> => {
    const todos = await usuariosAPI.listarUsuarios(0, 1000)
    return todos.content
      .filter(u => u.perfis.includes('PROFISSIONAL'))
      .map(u => ({ label: u.nomeCompleto, value: u.uuid }))
  }, [])

  const podeSubmeter =
    pacienteUuid &&
    profissionalUuid &&
    data &&
    horaInicio &&
    horaFim &&
    procedimentoId !== null &&
    localAtendimento &&
    pacienteAcamado !== null &&
    (tipo === 'AVALIACAO' || avaliacaoId)

  const handleSubmit = async () => {
    if (!podeSubmeter) return
    setSalvando(true)
    try {
      const dados: AgendamentoRequisicao = {
        pacienteUuid,
        profissionalUuid,
        inicioEm: `${data}T${horaInicio}:00`,
        fimEm: `${data}T${horaFim}:00`,
        tipo,
        procedimentoId: procedimentoId!,
        localAtendimento: localAtendimento as LocalAtendimento,
        pacienteAcamado: pacienteAcamado!,
        avaliacaoId:
          tipo === 'TRATAMENTO' && avaliacaoId
            ? Number(avaliacaoId)
            : undefined,
      }
      await onSalvar(dados)
    } catch (err: any) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.criarAgendamento),
        'error',
        6000,
      )
    } finally {
      setSalvando(false)
    }
  }

  const rodape = (
    <>
      <button
        className="mna-btn-cancelar"
        onClick={onClose}
        disabled={salvando}
      >
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
    <Modal
      titulo="Novo agendamento"
      tamanho="md"
      onClose={onClose}
      rodape={rodape}
    >
      <div className="mna-form">
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
          <SelectAutocomplete
            id="mna-paciente"
            placeholder="Buscar paciente pelo nome…"
            value={pacienteUuid}
            onChange={setPacienteUuid}
            carregarOpcoes={carregarPacientes}
          />
        </div>

        {/* Profissional (somente admin) */}
        {isAdmin && (
          <div className="mna-grupo">
            <label className="mna-rotulo" htmlFor="mna-profissional">
              Profissional
            </label>
            <SelectAutocomplete
              id="mna-profissional"
              placeholder="Buscar profissional pelo nome…"
              value={profissionalUuid}
              onChange={setProfissionalUuid}
              carregarOpcoes={carregarProfissionais}
            />
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

        {/* Procedimento */}
        <div className="mna-grupo">
          <label className="mna-rotulo">Procedimento</label>
          <div className="mna-opcoes">
            {procedimentos.map(p => (
              <button
                key={p.id}
                type="button"
                className={`mna-opcao-btn${procedimentoId === p.id ? ' ativo' : ''}`}
                onClick={() => setProcedimentoId(p.id)}
              >
                {p.nome}
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
      </div>
    </Modal>
  )
}
