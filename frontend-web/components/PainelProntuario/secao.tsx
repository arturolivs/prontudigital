'use client'

import { ReactNode } from 'react'
import './PainelProntuario.css'

/**
 * Peças comuns aos painéis de anexos, prescrições e atestados.
 *
 * Os três painéis são renderizados em dois contextos: como aba do prontuário
 * do paciente e como card da tela de procedimento. A diferença visual entre
 * eles está concentrada aqui, na `variante`.
 */

export type VariantePainel = 'aba' | 'card'

export interface PainelProps {
  pacienteUuid: string
  /** Recebe o nome do paciente quando a listagem o traz (usado no cabeçalho do prontuário). */
  onNomePaciente?: (nome: string) => void
  /**
   * Bloqueia as ações de escrita — enviar, criar, editar, excluir. A listagem
   * e a leitura (abrir anexo, baixar PDF) continuam disponíveis.
   */
  desabilitado?: boolean
  /**
   * `aba` — o painel é a própria seção da página (prontuário).
   * `card` — o painel mora dentro de um `.proc-card`, que não tem padding
   * próprio e já exibe o título da seção no cabeçalho.
   */
  variante?: VariantePainel
}

export function Secao({
  variante = 'aba',
  children,
}: {
  variante?: VariantePainel
  children: ReactNode
}) {
  return (
    <div
      className={`pnl-secao${variante === 'card' ? ' pnl-secao--card' : ''}`}
    >
      {children}
    </div>
  )
}

/**
 * Barra do topo do painel: título, subtítulo e o botão de ação principal.
 * Na variante `card` o título é omitido — o cabeçalho do card já o exibe.
 */
export function BarraPainel({
  titulo,
  hint,
  variante = 'aba',
  children,
}: {
  titulo: string
  hint?: string
  variante?: VariantePainel
  children?: ReactNode
}) {
  const semTitulo = variante === 'card'
  return (
    <div
      className={`anx-toolbar${semTitulo && !hint ? ' anx-toolbar--acao-unica' : ''}`}
    >
      {semTitulo ? (
        hint && <span className="anx-hint">{hint}</span>
      ) : (
        <div>
          <h2 className="anx-titulo">{titulo}</h2>
          {hint && <span className="anx-hint">{hint}</span>}
        </div>
      )}
      {children}
    </div>
  )
}

export function EstadoCarregando({ texto }: { texto: string }) {
  return (
    <div className="pront-estado">
      <div className="pnl-spinner" />
      <p>{texto}</p>
    </div>
  )
}
