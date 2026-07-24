'use client'

import { useId } from 'react'

// Campos reutilizados pelas fichas preenchidas durante a consulta
// (Anamnese, Evolução de Enfermagem e Evolução Diária – Curativos).

export type MudancaCampo = React.ChangeEvent<
  HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
>

export function BloqueadoTag() {
  return (
    <span className="proc-obs-bloqueado-tag">
      Bloqueado — clique em Iniciar
    </span>
  )
}

export function Campo({
  label,
  children,
  className = '',
}: {
  label: string
  children: React.ReactNode
  className?: string
}) {
  return (
    <div className={`proc-campo ${className}`}>
      <label className="proc-campo-label">{label}</label>
      {children}
    </div>
  )
}

export function CheckItem({
  label,
  checked,
  onChange,
  disabled,
}: {
  label: string
  checked: boolean
  onChange: (valor: boolean) => void
  disabled?: boolean
}) {
  return (
    <label className="proc-check">
      <input
        type="checkbox"
        checked={checked}
        onChange={e => onChange(e.target.checked)}
        disabled={disabled}
      />
      <span>{label}</span>
    </label>
  )
}

/** Select de escolha única a partir de uma lista de opções da ficha. */
export function CampoSelect<T extends string>({
  label,
  opcoes,
  valor,
  onChange,
  disabled,
}: {
  label: string
  opcoes: { value: T; label: string }[]
  valor: string
  onChange: (e: MudancaCampo) => void
  disabled?: boolean
}) {
  return (
    <Campo label={label}>
      <select
        className="proc-campo-select"
        value={valor}
        onChange={onChange}
        disabled={disabled}
      >
        <option value="">Não informado</option>
        {opcoes.map(op => (
          <option key={op.value} value={op.value}>
            {op.label}
          </option>
        ))}
      </select>
    </Campo>
  )
}

/** Campo de texto de uma linha. */
export function CampoTexto({
  label,
  valor,
  onChange,
  disabled,
  placeholder,
  tipo = 'text',
}: {
  label: string
  valor: string
  onChange: (e: MudancaCampo) => void
  disabled?: boolean
  placeholder?: string
  tipo?: 'text' | 'date' | 'time'
}) {
  return (
    <Campo label={label}>
      <input
        className="proc-campo-input"
        type={tipo}
        value={valor}
        onChange={onChange}
        disabled={disabled}
        placeholder={placeholder}
      />
    </Campo>
  )
}

/** Campo numérico (dimensões, escalas, contagens). */
export function CampoNumero({
  label,
  valor,
  onChange,
  disabled,
  min = '0',
  max,
  step = '0.1',
  placeholder = '0',
}: {
  label: string
  valor: string
  onChange: (e: MudancaCampo) => void
  disabled?: boolean
  min?: string
  max?: string
  step?: string
  placeholder?: string
}) {
  return (
    <Campo label={label}>
      <input
        className="proc-campo-input"
        type="number"
        min={min}
        max={max}
        step={step}
        value={valor}
        onChange={onChange}
        disabled={disabled}
        placeholder={placeholder}
      />
    </Campo>
  )
}

/** Campo de texto multilinha. */
export function CampoTextarea({
  label,
  valor,
  onChange,
  disabled,
  placeholder,
  rows = 3,
}: {
  label: string
  valor: string
  onChange: (e: MudancaCampo) => void
  disabled?: boolean
  placeholder?: string
  rows?: number
}) {
  return (
    <Campo label={label}>
      <textarea
        className="proc-campo-textarea"
        value={valor}
        onChange={onChange}
        disabled={disabled}
        placeholder={placeholder}
        rows={rows}
      />
    </Campo>
  )
}

/**
 * Pergunta Sim/Não dos modelos. Quando `onDetalheChange` é informado,
 * exibe a caixa de texto que o modelo traz logo abaixo da pergunta.
 * O valor `undefined` representa a pergunta ainda não respondida.
 */
export function CampoSimNao({
  label,
  valor,
  onChange,
  disabled,
  detalhe,
  onDetalheChange,
  detalhePlaceholder = 'Detalhe (opcional)',
}: {
  label: string
  valor?: boolean
  onChange: (valor: boolean) => void
  disabled?: boolean
  detalhe?: string
  onDetalheChange?: (e: MudancaCampo) => void
  detalhePlaceholder?: string
}) {
  const nome = useId()
  return (
    <div className="proc-campo">
      <label className="proc-campo-label">{label}</label>
      <div className="proc-radio-grupo">
        <label className="proc-radio">
          <input
            type="radio"
            name={nome}
            checked={valor === true}
            onChange={() => onChange(true)}
            disabled={disabled}
          />
          <span>Sim</span>
        </label>
        <label className="proc-radio">
          <input
            type="radio"
            name={nome}
            checked={valor === false}
            onChange={() => onChange(false)}
            disabled={disabled}
          />
          <span>Não</span>
        </label>
      </div>
      {onDetalheChange && (
        <input
          className="proc-campo-input"
          value={detalhe ?? ''}
          onChange={onDetalheChange}
          disabled={disabled}
          placeholder={detalhePlaceholder}
        />
      )}
    </div>
  )
}

/** Linha da tabela "Intervenção × Observação" / "Ação × Observações" do modelo. */
export function LinhaObservacao({
  rotulo,
  valor,
  onChange,
  disabled,
  placeholder,
}: {
  rotulo: string
  valor: string
  onChange: (e: MudancaCampo) => void
  disabled?: boolean
  placeholder?: string
}) {
  return (
    <Campo label={rotulo}>
      <input
        className="proc-campo-input"
        value={valor}
        onChange={onChange}
        disabled={disabled}
        placeholder={placeholder}
      />
    </Campo>
  )
}

/** Subtítulo de bloco dentro de uma seção da ficha. */
export function SubSecao({ titulo }: { titulo: string }) {
  return <h3 className="proc-subsecao">{titulo}</h3>
}
