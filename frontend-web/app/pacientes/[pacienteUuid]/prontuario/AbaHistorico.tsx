'use client'

import { useState, useEffect, useCallback } from 'react'
import { historicoAPI } from '../../../../lib/historico.service'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
// A timeline reaproveita a barra de topo, os estados vazios e os botões dos
// painéis de anexos/prescrições/atestados, que moram no CSS compartilhado.
import '@/components/PainelProntuario/PainelProntuario.css'
import {
  HistoricoItem,
  TipoHistorico,
  ROTULO_TIPO,
  CLASSE_TIPO,
  formatarDataHora,
} from '@/tipos/historico'

type Filtro = 'TODOS' | TipoHistorico

const FILTROS: { id: Filtro; rotulo: string }[] = [
  { id: 'TODOS', rotulo: 'Tudo' },
  { id: 'AGENDAMENTO', rotulo: 'Atendimentos' },
  { id: 'EVOLUCAO', rotulo: 'Evoluções' },
  { id: 'PRESCRICAO', rotulo: 'Prescrições' },
  { id: 'ANEXO', rotulo: 'Anexos' },
]

export default function AbaHistorico({
  pacienteUuid,
}: {
  pacienteUuid: string
}) {
  const [itens, setItens] = useState<HistoricoItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filtro, setFiltro] = useState<Filtro>('TODOS')

  const fetchHistorico = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await historicoAPI.listar(pacienteUuid)
      setItens(data)
    } catch (err) {
      setError(mensagemErro(err, MENSAGENS.erro.carregarHistorico))
    } finally {
      setLoading(false)
    }
  }, [pacienteUuid])

  useEffect(() => {
    fetchHistorico()
  }, [fetchHistorico])

  const visiveis =
    filtro === 'TODOS' ? itens : itens.filter(i => i.tipo === filtro)

  return (
    <div className="pront-aba">
      <div className="anx-toolbar">
        <div>
          <h2 className="anx-titulo">Histórico clínico</h2>
          <span className="anx-hint">
            Linha do tempo de atendimentos, evoluções, prescrições e anexos
          </span>
        </div>
      </div>

      {!loading && !error && itens.length > 0 && (
        <div
          className="hist-filtros"
          role="tablist"
          aria-label="Filtrar por tipo"
        >
          {FILTROS.map(({ id, rotulo }) => (
            <button
              key={id}
              role="tab"
              aria-selected={filtro === id}
              className={`hist-filtro${filtro === id ? ' hist-filtro-ativo' : ''}`}
              onClick={() => setFiltro(id)}
            >
              {rotulo}
            </button>
          ))}
        </div>
      )}

      {loading && (
        <div className="pront-estado">
          <div className="pnl-spinner" />
          <p>Carregando histórico...</p>
        </div>
      )}

      {error && !loading && (
        <div className="pront-estado pront-estado-erro">
          <p>{error}</p>
          <button
            className="presc-btn presc-btn-secundario"
            onClick={fetchHistorico}
          >
            Tentar novamente
          </button>
        </div>
      )}

      {!loading && !error && itens.length === 0 && (
        <div className="pront-estado">
          <p>Nenhum registro clínico para este paciente ainda.</p>
        </div>
      )}

      {!loading && !error && itens.length > 0 && visiveis.length === 0 && (
        <div className="pront-estado">
          <p>Nenhum registro deste tipo.</p>
        </div>
      )}

      {!loading && !error && visiveis.length > 0 && (
        <ol className="hist-timeline">
          {visiveis.map((item, i) => (
            <li
              key={`${item.tipo}-${item.referenciaUuid ?? i}-${item.data}`}
              className="hist-item"
            >
              <span
                className={`hist-marker hist-marker-${CLASSE_TIPO[item.tipo]}`}
                aria-hidden="true"
              />
              <div className="hist-card">
                <div className="hist-card-topo">
                  <span
                    className={`hist-badge hist-badge-${CLASSE_TIPO[item.tipo]}`}
                  >
                    {ROTULO_TIPO[item.tipo]}
                  </span>
                  <time className="hist-data" dateTime={item.data}>
                    {formatarDataHora(item.data)}
                  </time>
                </div>
                <p className="hist-titulo" title={item.titulo}>
                  {item.titulo}
                </p>
                {item.descricao && (
                  <p className="hist-descricao">{item.descricao}</p>
                )}
              </div>
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}
