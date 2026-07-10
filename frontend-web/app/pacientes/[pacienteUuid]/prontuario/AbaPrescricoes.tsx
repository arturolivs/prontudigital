'use client'

import { useState, useEffect, useCallback, FormEvent } from 'react'
import { useNotificacao } from '../../../../contexts/ToastContext'
import { prescricaoAPI } from '../../../../lib/prescricao.service'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import Modal from '@/components/Modal'
import {
  Prescricao,
  PrescricaoRequisicao,
  TipoPrescricao,
  ROTULO_TIPO_PRESCRICAO,
} from '@/tipos/prescricao'

const formatarDataHora = (dataString: string): string =>
  new Date(dataString).toLocaleString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })

const FORM_VAZIO: PrescricaoRequisicao = {
  tipo: 'MEDICAMENTO',
  descricao: '',
  posologia: '',
  frequencia: '',
  duracao: '',
  orientacoes: '',
}

function FormularioPrescricao({
  inicial,
  salvando,
  onSalvar,
  onCancelar,
}: {
  inicial: PrescricaoRequisicao
  salvando: boolean
  onSalvar: (dados: PrescricaoRequisicao) => void
  onCancelar: () => void
}) {
  const [form, setForm] = useState<PrescricaoRequisicao>(inicial)
  const [erroDescricao, setErroDescricao] = useState<string | null>(null)

  const atualizar = (campo: keyof PrescricaoRequisicao, valor: string) =>
    setForm(prev => ({ ...prev, [campo]: valor }))

  const submeter = (e: FormEvent) => {
    e.preventDefault()
    if (!form.descricao.trim()) {
      setErroDescricao(MENSAGENS.validacao.descricaoPrescricaoObrigatoria)
      return
    }
    onSalvar(form)
  }

  const isMedicamento = form.tipo === 'MEDICAMENTO'

  return (
    <form className="presc-form" onSubmit={submeter}>
      <div className="presc-form-grupo">
        <label htmlFor="tipo">Tipo</label>
        <div className="presc-tipo-toggle">
          {(['MEDICAMENTO', 'CUIDADO'] as TipoPrescricao[]).map(t => (
            <button
              type="button"
              key={t}
              className={`presc-tipo-btn${form.tipo === t ? ' presc-tipo-ativo' : ''}`}
              onClick={() => atualizar('tipo', t)}
            >
              {ROTULO_TIPO_PRESCRICAO[t]}
            </button>
          ))}
        </div>
      </div>

      <div className="presc-form-grupo">
        <label htmlFor="descricao">
          {isMedicamento ? 'Medicamento' : 'Cuidado'} <span aria-hidden>*</span>
        </label>
        <input
          id="descricao"
          type="text"
          value={form.descricao}
          onChange={e => {
            atualizar('descricao', e.target.value)
            if (erroDescricao) setErroDescricao(null)
          }}
          placeholder={
            isMedicamento
              ? 'Ex.: Sulfadiazina de prata 1% - creme'
              : 'Ex.: Curativo com hidrocoloide'
          }
          maxLength={1000}
          autoFocus
        />
        {erroDescricao && <span className="presc-erro">{erroDescricao}</span>}
      </div>

      <div className="presc-form-grupo">
        <label htmlFor="posologia">
          {isMedicamento ? 'Posologia / dose' : 'Técnica / observações'}
        </label>
        <textarea
          id="posologia"
          value={form.posologia ?? ''}
          onChange={e => atualizar('posologia', e.target.value)}
          rows={2}
          maxLength={1000}
          placeholder={
            isMedicamento
              ? 'Ex.: Aplicar camada fina sobre a ferida'
              : 'Ex.: Limpeza com SF 0,9% antes da cobertura'
          }
        />
      </div>

      <div className="presc-form-linha">
        <div className="presc-form-grupo">
          <label htmlFor="frequencia">Frequência</label>
          <input
            id="frequencia"
            type="text"
            value={form.frequencia ?? ''}
            onChange={e => atualizar('frequencia', e.target.value)}
            placeholder="Ex.: A cada 12h"
            maxLength={255}
          />
        </div>
        <div className="presc-form-grupo">
          <label htmlFor="duracao">Duração</label>
          <input
            id="duracao"
            type="text"
            value={form.duracao ?? ''}
            onChange={e => atualizar('duracao', e.target.value)}
            placeholder="Ex.: 7 dias"
            maxLength={255}
          />
        </div>
      </div>

      <div className="presc-form-grupo">
        <label htmlFor="orientacoes">Orientações ao paciente/cuidador</label>
        <textarea
          id="orientacoes"
          value={form.orientacoes ?? ''}
          onChange={e => atualizar('orientacoes', e.target.value)}
          rows={3}
          maxLength={5000}
        />
      </div>

      <div className="presc-form-acoes">
        <button
          type="button"
          className="presc-btn presc-btn-secundario"
          onClick={onCancelar}
          disabled={salvando}
        >
          Cancelar
        </button>
        <button
          type="submit"
          className="presc-btn presc-btn-primario"
          disabled={salvando}
        >
          {salvando ? 'Salvando...' : 'Salvar'}
        </button>
      </div>
    </form>
  )
}

function CardPrescricao({
  prescricao,
  onEditar,
  onExcluir,
}: {
  prescricao: Prescricao
  onEditar: () => void
  onExcluir: () => void
}) {
  return (
    <div className="presc-card">
      <div className="presc-card-topo">
        <span
          className={`presc-tag presc-tag-${prescricao.tipo === 'MEDICAMENTO' ? 'medicamento' : 'cuidado'}`}
        >
          {ROTULO_TIPO_PRESCRICAO[prescricao.tipo]}
        </span>
        <div className="presc-card-acoes">
          <button
            className="presc-icon-btn"
            onClick={onEditar}
            aria-label="Editar prescrição"
          >
            Editar
          </button>
          <button
            className="presc-icon-btn presc-icon-btn-perigo"
            onClick={onExcluir}
            aria-label="Excluir prescrição"
          >
            Excluir
          </button>
        </div>
      </div>

      <h3 className="presc-card-descricao">{prescricao.descricao}</h3>

      {prescricao.posologia && (
        <p className="presc-card-posologia">{prescricao.posologia}</p>
      )}

      <div className="presc-card-meta">
        {prescricao.frequencia && (
          <span className="presc-chip">🕒 {prescricao.frequencia}</span>
        )}
        {prescricao.duracao && (
          <span className="presc-chip">📅 {prescricao.duracao}</span>
        )}
      </div>

      {prescricao.orientacoes && (
        <p className="presc-card-orientacoes">{prescricao.orientacoes}</p>
      )}

      <span className="presc-card-data">
        Registrada em {formatarDataHora(prescricao.criadoEm)}
        {prescricao.atualizadoEm ? ' · editada' : ''}
      </span>
    </div>
  )
}

export default function AbaPrescricoes({
  pacienteUuid,
  onNomePaciente,
}: {
  pacienteUuid: string
  onNomePaciente: (nome: string) => void
}) {
  const { exibirNotificacao } = useNotificacao()

  const [prescricoes, setPrescricoes] = useState<Prescricao[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  const [modalAberto, setModalAberto] = useState(false)
  const [emEdicao, setEmEdicao] = useState<Prescricao | null>(null)
  const [excluindo, setExcluindo] = useState<Prescricao | null>(null)

  const fetchPrescricoes = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await prescricaoAPI.listar(pacienteUuid)
      setPrescricoes(data)
      if (data[0]?.pacienteNome) onNomePaciente(data[0].pacienteNome)
    } catch (err) {
      setError(mensagemErro(err, MENSAGENS.erro.carregarPrescricoes))
    } finally {
      setLoading(false)
    }
  }, [pacienteUuid, onNomePaciente])

  useEffect(() => {
    fetchPrescricoes()
  }, [fetchPrescricoes])

  const abrirNova = () => {
    setEmEdicao(null)
    setModalAberto(true)
  }

  const abrirEdicao = (p: Prescricao) => {
    setEmEdicao(p)
    setModalAberto(true)
  }

  const salvar = async (dados: PrescricaoRequisicao) => {
    try {
      setSalvando(true)
      if (emEdicao) {
        await prescricaoAPI.atualizar(pacienteUuid, emEdicao.uuid, dados)
        exibirNotificacao(MENSAGENS.sucesso.prescricaoAtualizada, 'success')
      } else {
        await prescricaoAPI.criar(pacienteUuid, dados)
        exibirNotificacao(MENSAGENS.sucesso.prescricaoCriada, 'success')
      }
      setModalAberto(false)
      setEmEdicao(null)
      await fetchPrescricoes()
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.salvarPrescricao),
        'error',
      )
    } finally {
      setSalvando(false)
    }
  }

  const confirmarExclusao = async () => {
    if (!excluindo) return
    try {
      setSalvando(true)
      await prescricaoAPI.excluir(pacienteUuid, excluindo.uuid)
      exibirNotificacao(MENSAGENS.sucesso.prescricaoExcluida, 'success')
      setExcluindo(null)
      await fetchPrescricoes()
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.excluirPrescricao),
        'error',
      )
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="pront-aba">
      <div className="anx-toolbar">
        <div>
          <h2 className="anx-titulo">Prescrições</h2>
          <span className="anx-hint">
            Medicamentos e cuidados de enfermagem
          </span>
        </div>
        <button className="presc-btn presc-btn-primario" onClick={abrirNova}>
          + Nova prescrição
        </button>
      </div>

      {loading && (
        <div className="pront-estado">
          <div className="pac-spinner" />
          <p>Carregando prescrições...</p>
        </div>
      )}

      {error && !loading && (
        <div className="pront-estado pront-estado-erro">
          <p>{error}</p>
          <button
            className="presc-btn presc-btn-secundario"
            onClick={fetchPrescricoes}
          >
            Tentar novamente
          </button>
        </div>
      )}

      {!loading && !error && prescricoes.length === 0 && (
        <div className="pront-estado">
          <p>Nenhuma prescrição registrada para este paciente.</p>
          <button className="presc-btn presc-btn-primario" onClick={abrirNova}>
            Registrar primeira prescrição
          </button>
        </div>
      )}

      {!loading && !error && prescricoes.length > 0 && (
        <div className="presc-lista">
          {prescricoes.map(p => (
            <CardPrescricao
              key={p.uuid}
              prescricao={p}
              onEditar={() => abrirEdicao(p)}
              onExcluir={() => setExcluindo(p)}
            />
          ))}
        </div>
      )}

      {modalAberto && (
        <Modal
          titulo={emEdicao ? 'Editar prescrição' : 'Nova prescrição'}
          onClose={() => (salvando ? null : setModalAberto(false))}
          tamanho="md"
        >
          <FormularioPrescricao
            inicial={
              emEdicao
                ? {
                    tipo: emEdicao.tipo,
                    descricao: emEdicao.descricao,
                    posologia: emEdicao.posologia ?? '',
                    frequencia: emEdicao.frequencia ?? '',
                    duracao: emEdicao.duracao ?? '',
                    orientacoes: emEdicao.orientacoes ?? '',
                  }
                : FORM_VAZIO
            }
            salvando={salvando}
            onSalvar={salvar}
            onCancelar={() => setModalAberto(false)}
          />
        </Modal>
      )}

      {excluindo && (
        <Modal
          titulo="Excluir prescrição"
          onClose={() => (salvando ? null : setExcluindo(null))}
          tamanho="sm"
          rodape={
            <>
              <button
                className="presc-btn presc-btn-secundario"
                onClick={() => setExcluindo(null)}
                disabled={salvando}
              >
                Cancelar
              </button>
              <button
                className="presc-btn presc-btn-perigo"
                onClick={confirmarExclusao}
                disabled={salvando}
              >
                {salvando ? 'Excluindo...' : 'Excluir'}
              </button>
            </>
          }
        >
          <p>
            Deseja excluir a prescrição <strong>{excluindo.descricao}</strong>?
            Esta ação não pode ser desfeita.
          </p>
        </Modal>
      )}
    </div>
  )
}
