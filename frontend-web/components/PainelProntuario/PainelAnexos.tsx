'use client'

import { useState, useEffect, useCallback, useRef, ChangeEvent } from 'react'
import { useNotificacao } from '@/contexts/ToastContext'
import { anexoAPI } from '@/lib/anexo.service'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import Modal from '@/components/Modal'
import {
  Anexo,
  TIPOS_ACEITOS,
  TAMANHO_MAXIMO_BYTES,
  ehImagem,
  formatarTamanho,
} from '@/tipos/anexo'
import { BarraPainel, EstadoCarregando, PainelProps, Secao } from './secao'

const formatarDataHora = (dataString: string): string =>
  new Date(dataString).toLocaleString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })

function CardAnexo({
  anexo,
  miniatura,
  onAbrir,
  onExcluir,
  podeExcluir,
}: {
  anexo: Anexo
  miniatura?: string
  onAbrir: () => void
  onExcluir: () => void
  podeExcluir: boolean
}) {
  const imagem = ehImagem(anexo.tipoConteudo)
  return (
    <div className="anx-card">
      <button
        type="button"
        className="anx-preview"
        onClick={onAbrir}
        aria-label={`Abrir ${anexo.nomeOriginal}`}
      >
        {imagem ? (
          miniatura ? (
            <img
              src={miniatura}
              alt={anexo.nomeOriginal}
              className="anx-thumb"
            />
          ) : (
            <div className="anx-thumb-placeholder">
              <div className="pnl-spinner" />
            </div>
          )
        ) : (
          <div className="anx-thumb-placeholder anx-thumb-pdf">
            <span className="anx-pdf-icon">PDF</span>
          </div>
        )}
      </button>

      <div className="anx-card-corpo">
        <span className="anx-nome" title={anexo.nomeOriginal}>
          {anexo.nomeOriginal}
        </span>
        <span className="anx-meta">
          {formatarTamanho(anexo.tamanhoBytes)} ·{' '}
          {formatarDataHora(anexo.criadoEm)}
        </span>
      </div>

      <div className="anx-card-acoes">
        <button className="presc-icon-btn" onClick={onAbrir}>
          Abrir
        </button>
        <button
          className="presc-icon-btn presc-icon-btn-perigo"
          onClick={onExcluir}
          disabled={!podeExcluir}
          aria-label="Excluir anexo"
        >
          Excluir
        </button>
      </div>
    </div>
  )
}

export default function PainelAnexos({
  pacienteUuid,
  onNomePaciente,
  desabilitado = false,
  variante = 'aba',
}: PainelProps) {
  const { exibirNotificacao } = useNotificacao()

  const [anexos, setAnexos] = useState<Anexo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [excluindo, setExcluindo] = useState<Anexo | null>(null)
  const [miniaturas, setMiniaturas] = useState<Record<string, string>>({})

  const inputRef = useRef<HTMLInputElement>(null)
  const urlsRef = useRef<string[]>([])
  const carregadasRef = useRef<Set<string>>(new Set())
  const montadoRef = useRef(true)

  const fetchAnexos = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await anexoAPI.listar(pacienteUuid)
      setAnexos(data)
      if (data[0]?.pacienteNome) onNomePaciente?.(data[0].pacienteNome)
    } catch (err) {
      setError(mensagemErro(err, MENSAGENS.erro.carregarAnexos))
    } finally {
      setLoading(false)
    }
  }, [pacienteUuid, onNomePaciente])

  useEffect(() => {
    fetchAnexos()
  }, [fetchAnexos])

  // Carrega as miniaturas das imagens sob demanda (dedup via carregadasRef).
  //
  // O descarte de um download em andamento é decidido por `montadoRef`, e não
  // por uma flag do próprio efeito. Este efeito re-executa sempre que `anexos`
  // troca de identidade (StrictMode em dev roda o fetch duas vezes; enviar ou
  // excluir um anexo chama fetchAnexos de novo), e usar uma flag por execução
  // fazia o download em voo ser abandonado sem limpar `carregadasRef` — o uuid
  // ficava marcado como carregado, a execução seguinte o pulava e a miniatura
  // travava no spinner para sempre.
  useEffect(() => {
    anexos.forEach(async anexo => {
      if (!ehImagem(anexo.tipoConteudo)) return
      if (carregadasRef.current.has(anexo.uuid)) return
      carregadasRef.current.add(anexo.uuid)
      try {
        const blob = await anexoAPI.baixarConteudo(pacienteUuid, anexo.uuid)
        const url = URL.createObjectURL(blob)
        if (!montadoRef.current) {
          // Chegou depois de sair da tela: ninguém vai exibir, e o cleanup de
          // desmontagem já passou por urlsRef. Revoga na hora para não vazar.
          URL.revokeObjectURL(url)
          return
        }
        urlsRef.current.push(url)
        setMiniaturas(prev => ({ ...prev, [anexo.uuid]: url }))
      } catch {
        // Libera o uuid para que uma nova tentativa seja possível.
        carregadasRef.current.delete(anexo.uuid)
      }
    })
  }, [anexos, pacienteUuid])

  // Ciclo de vida do componente + revogação dos object URLs.
  useEffect(() => {
    montadoRef.current = true
    return () => {
      montadoRef.current = false
      urlsRef.current.forEach(URL.revokeObjectURL)
      urlsRef.current = []
    }
  }, [])

  const abrirSeletor = () => inputRef.current?.click()

  const aoEscolherArquivo = async (e: ChangeEvent<HTMLInputElement>) => {
    const arquivo = e.target.files?.[0]
    e.target.value = '' // permite reenviar o mesmo arquivo depois
    if (!arquivo) return

    if (
      !TIPOS_ACEITOS.includes(arquivo.type as (typeof TIPOS_ACEITOS)[number])
    ) {
      exibirNotificacao(MENSAGENS.erro.anexoTipoInvalido, 'error')
      return
    }
    if (arquivo.size > TAMANHO_MAXIMO_BYTES) {
      exibirNotificacao(MENSAGENS.erro.anexoTamanho, 'error')
      return
    }

    try {
      setEnviando(true)
      await anexoAPI.enviar(pacienteUuid, arquivo)
      exibirNotificacao(MENSAGENS.sucesso.anexoEnviado, 'success')
      await fetchAnexos()
    } catch (err) {
      exibirNotificacao(mensagemErro(err, MENSAGENS.erro.enviarAnexo), 'error')
    } finally {
      setEnviando(false)
    }
  }

  const abrir = async (anexo: Anexo) => {
    try {
      const blob = await anexoAPI.baixarConteudo(pacienteUuid, anexo.uuid)
      const url = URL.createObjectURL(blob)
      window.open(url, '_blank', 'noopener')
      setTimeout(() => URL.revokeObjectURL(url), 60000)
    } catch (err) {
      exibirNotificacao(mensagemErro(err, MENSAGENS.erro.abrirAnexo), 'error')
    }
  }

  const confirmarExclusao = async () => {
    if (!excluindo) return
    try {
      setEnviando(true)
      await anexoAPI.excluir(pacienteUuid, excluindo.uuid)
      exibirNotificacao(MENSAGENS.sucesso.anexoExcluido, 'success')
      setExcluindo(null)
      await fetchAnexos()
    } catch (err) {
      exibirNotificacao(mensagemErro(err, MENSAGENS.erro.excluirAnexo), 'error')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Secao variante={variante}>
      <BarraPainel
        titulo="Exames e documentos"
        hint="Imagens (JPG, PNG, WEBP) ou PDF · até 10 MB"
        variante={variante}
      >
        <button
          className="presc-btn presc-btn-primario"
          onClick={abrirSeletor}
          disabled={enviando || desabilitado}
        >
          {enviando ? 'Enviando...' : '+ Enviar anexo'}
        </button>
      </BarraPainel>
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp,application/pdf"
        onChange={aoEscolherArquivo}
        hidden
      />

      {loading && <EstadoCarregando texto="Carregando anexos..." />}

      {error && !loading && (
        <div className="pront-estado pront-estado-erro">
          <p>{error}</p>
          <button
            className="presc-btn presc-btn-secundario"
            onClick={fetchAnexos}
          >
            Tentar novamente
          </button>
        </div>
      )}

      {!loading && !error && anexos.length === 0 && (
        <div className="pront-estado">
          <p>Nenhum anexo enviado para este paciente.</p>
          <button
            className="presc-btn presc-btn-primario"
            onClick={abrirSeletor}
            disabled={enviando || desabilitado}
          >
            Enviar primeiro anexo
          </button>
        </div>
      )}

      {!loading && !error && anexos.length > 0 && (
        <div className="anx-grid">
          {anexos.map(a => (
            <CardAnexo
              key={a.uuid}
              anexo={a}
              miniatura={miniaturas[a.uuid]}
              onAbrir={() => abrir(a)}
              onExcluir={() => setExcluindo(a)}
              podeExcluir={!desabilitado}
            />
          ))}
        </div>
      )}

      {excluindo && (
        <Modal
          titulo="Excluir anexo"
          onClose={() => (enviando ? null : setExcluindo(null))}
          tamanho="sm"
          rodape={
            <>
              <button
                className="presc-btn presc-btn-secundario"
                onClick={() => setExcluindo(null)}
                disabled={enviando}
              >
                Cancelar
              </button>
              <button
                className="presc-btn presc-btn-perigo"
                onClick={confirmarExclusao}
                disabled={enviando}
              >
                {enviando ? 'Excluindo...' : 'Excluir'}
              </button>
            </>
          }
        >
          <p>
            Deseja excluir o anexo <strong>{excluindo.nomeOriginal}</strong>?
            Esta ação não pode ser desfeita.
          </p>
        </Modal>
      )}
    </Secao>
  )
}
