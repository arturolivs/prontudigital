'use client'

import { useCallback, useEffect, useState } from 'react'
import { FileDown, Plus, Trash2 } from 'lucide-react'
import Modal from '@/components/Modal'
import ModalConfirmacao from '@/components/ModalConfirmacao'
import { atestadoAPI } from '@/lib/atestado.service'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import { useNotificacao } from '@/contexts/ToastContext'
import { Atestado, ROTULO_TIPO_ATESTADO, TipoAtestado } from '@/tipos/atestado'
import { BarraPainel, EstadoCarregando, PainelProps, Secao } from './secao'

// RF17 — emissão de atestados. O PDF não é guardado: o backend regera a partir
// do registro toda vez, então baixar duas vezes dá o mesmo documento.

const formatarDataHora = (iso: string) =>
  new Date(iso).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })

export default function PainelAtestados({
  pacienteUuid,
  onNomePaciente,
  desabilitado = false,
  variante = 'aba',
}: PainelProps) {
  const { exibirNotificacao } = useNotificacao()

  const [atestados, setAtestados] = useState<Atestado[]>([])
  const [carregando, setCarregando] = useState(true)
  const [modalAberto, setModalAberto] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [paraExcluir, setParaExcluir] = useState<Atestado | null>(null)

  const [tipo, setTipo] = useState<TipoAtestado>('COMPARECIMENTO')
  const [dias, setDias] = useState('')
  const [cid, setCid] = useState('')
  const [observacoes, setObservacoes] = useState('')

  const carregar = useCallback(async () => {
    setCarregando(true)
    try {
      const dados = await atestadoAPI.listar(pacienteUuid)
      setAtestados(dados)
      if (dados.length > 0 && onNomePaciente) {
        onNomePaciente(dados[0].nomePaciente)
      }
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.carregarAtestados),
        'error',
      )
    } finally {
      setCarregando(false)
    }
  }, [pacienteUuid, onNomePaciente, exibirNotificacao])

  useEffect(() => {
    carregar()
  }, [carregar])

  const limparFormulario = () => {
    setTipo('COMPARECIMENTO')
    setDias('')
    setCid('')
    setObservacoes('')
  }

  const abrirModal = () => {
    limparFormulario()
    setModalAberto(true)
  }

  const emitir = async () => {
    if (tipo === 'AFASTAMENTO' && !dias) {
      exibirNotificacao(MENSAGENS.validacao.diasAfastamentoObrigatorio, 'error')
      return
    }

    setSalvando(true)
    try {
      const novo = await atestadoAPI.emitir(pacienteUuid, {
        tipo,
        // O backend recusa dias em COMPARECIMENTO.
        diasAfastamento: tipo === 'AFASTAMENTO' ? Number(dias) : undefined,
        cid: cid.trim() || undefined,
        observacoes: observacoes.trim() || undefined,
      })
      setAtestados(prev => [novo, ...prev])
      setModalAberto(false)
      exibirNotificacao(MENSAGENS.sucesso.atestadoEmitido, 'success')
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.emitirAtestado),
        'error',
        5000,
      )
    } finally {
      setSalvando(false)
    }
  }

  const baixar = async (atestado: Atestado) => {
    try {
      await atestadoAPI.baixarPdf(pacienteUuid, atestado.uuid)
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.baixarAtestado),
        'error',
      )
    }
  }

  const confirmarExclusao = async () => {
    if (!paraExcluir) return
    try {
      await atestadoAPI.excluir(pacienteUuid, paraExcluir.uuid)
      setAtestados(prev => prev.filter(a => a.uuid !== paraExcluir.uuid))
      exibirNotificacao(MENSAGENS.sucesso.atestadoRemovido, 'success')
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.excluirAtestado),
        'error',
      )
    } finally {
      setParaExcluir(null)
    }
  }

  if (carregando) {
    return (
      <Secao variante={variante}>
        <EstadoCarregando texto="Carregando atestados..." />
      </Secao>
    )
  }

  return (
    <Secao variante={variante}>
      <BarraPainel titulo="Atestados" variante={variante}>
        <button
          className="presc-btn presc-btn-primario"
          onClick={abrirModal}
          disabled={desabilitado}
        >
          <Plus size={16} strokeWidth={2} />
          Emitir atestado
        </button>
      </BarraPainel>

      {atestados.length === 0 ? (
        <div className="pront-estado">
          <p>Nenhum atestado emitido para este paciente.</p>
        </div>
      ) : (
        <ul className="atst-lista">
          {atestados.map(atestado => (
            <li key={atestado.uuid} className="atst-item">
              <div className="atst-info">
                <div className="atst-linha-titulo">
                  <span className={`atst-badge atst-badge--${atestado.tipo}`}>
                    {ROTULO_TIPO_ATESTADO[atestado.tipo]}
                  </span>
                  {atestado.diasAfastamento && (
                    <span className="atst-dias">
                      {atestado.diasAfastamento} dia(s)
                    </span>
                  )}
                  {atestado.cid && (
                    <span className="atst-cid">CID {atestado.cid}</span>
                  )}
                </div>
                {atestado.observacoes && (
                  <p className="atst-observacoes">{atestado.observacoes}</p>
                )}
                <span className="atst-meta">
                  {formatarDataHora(atestado.criadoEm)}
                  {atestado.nomeProfissional &&
                    ` — ${atestado.nomeProfissional}`}
                </span>
              </div>
              <div className="atst-acoes">
                <button
                  className="presc-icon-btn"
                  onClick={() => baixar(atestado)}
                  title="Abrir PDF"
                >
                  <FileDown size={16} strokeWidth={1.75} />
                </button>
                <button
                  className="presc-icon-btn"
                  onClick={() => setParaExcluir(atestado)}
                  disabled={desabilitado}
                  title="Remover atestado"
                >
                  <Trash2 size={16} strokeWidth={1.75} />
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {modalAberto && (
        <Modal
          titulo="Emitir atestado"
          onClose={() => setModalAberto(false)}
          rodape={
            <>
              <button
                type="button"
                className="presc-btn presc-btn-secundario"
                onClick={() => setModalAberto(false)}
                disabled={salvando}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="presc-btn presc-btn-primario"
                onClick={emitir}
                disabled={salvando}
              >
                {salvando ? 'Emitindo...' : 'Emitir'}
              </button>
            </>
          }
        >
          <div className="presc-form">
            <div className="presc-form-grupo">
              <label htmlFor="atst-tipo">Tipo</label>
              <select
                id="atst-tipo"
                value={tipo}
                onChange={e => {
                  setTipo(e.target.value as TipoAtestado)
                  setDias('')
                }}
              >
                <option value="COMPARECIMENTO">Comparecimento</option>
                <option value="AFASTAMENTO">Afastamento</option>
              </select>
            </div>

            {tipo === 'AFASTAMENTO' && (
              <div className="presc-form-grupo">
                <label htmlFor="atst-dias">Dias de afastamento</label>
                <input
                  id="atst-dias"
                  type="number"
                  min={1}
                  value={dias}
                  onChange={e => setDias(e.target.value)}
                  placeholder="3"
                />
              </div>
            )}

            <div className="presc-form-grupo">
              <label htmlFor="atst-cid">CID (opcional)</label>
              <input
                id="atst-cid"
                type="text"
                value={cid}
                onChange={e => setCid(e.target.value)}
                maxLength={10}
                placeholder="L97"
              />
              <small className="atst-ajuda">
                Informar o CID depende do consentimento do paciente.
              </small>
            </div>

            <div className="presc-form-grupo">
              <label htmlFor="atst-obs">Observações</label>
              <textarea
                id="atst-obs"
                rows={3}
                value={observacoes}
                onChange={e => setObservacoes(e.target.value)}
                placeholder="Texto adicional que entra no corpo do atestado"
              />
            </div>
          </div>
        </Modal>
      )}

      {paraExcluir && (
        <ModalConfirmacao
          titulo="Remover atestado"
          mensagem="O atestado será removido do prontuário. Deseja continuar?"
          onConfirmar={confirmarExclusao}
          onCancelar={() => setParaExcluir(null)}
        />
      )}
    </Secao>
  )
}
