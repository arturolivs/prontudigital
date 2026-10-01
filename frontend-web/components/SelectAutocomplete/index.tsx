'use client'

import { useState, useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import './SelectAutocomplete.css'

export interface OpcaoSAC {
  label: string
  value: string
}

interface Props {
  id?: string
  placeholder?: string
  value: string
  onChange: (value: string) => void
  carregarOpcoes: () => Promise<OpcaoSAC[]>
  className?: string
  disabled?: boolean
}

export default function SelectAutocomplete({
  id,
  placeholder = 'Selecione…',
  value,
  onChange,
  carregarOpcoes,
  className = '',
  disabled = false,
}: Props) {
  const [opcoes, setOpcoes] = useState<OpcaoSAC[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erroCarreg, setErroCarreg] = useState('')
  const [textoInput, setTextoInput] = useState('')
  const [aberto, setAberto] = useState(false)
  const [indice, setIndice] = useState(-1)
  const [posDropdown, setPosDropdown] = useState({ top: 0, left: 0, width: 0 })

  const inputRef = useRef<HTMLInputElement>(null)
  const listaRef = useRef<HTMLUListElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  const carregarRef = useRef(carregarOpcoes)
  useEffect(() => {
    let cancelado = false
    setCarregando(true)
    carregarRef
      .current()
      .then(data => {
        if (!cancelado) setOpcoes(data)
      })
      .catch(() => {
        if (!cancelado) setErroCarreg('Não foi possível carregar as opções.')
      })
      .finally(() => {
        if (!cancelado) setCarregando(false)
      })
    return () => {
      cancelado = true
    }
  }, [])

  useEffect(() => {
    if (aberto) return
    const opcao = opcoes.find(o => o.value === value)
    setTextoInput(opcao?.label ?? '')
  }, [value, opcoes, aberto])

  useEffect(() => {
    if (!aberto) return
    function onClickFora(e: MouseEvent) {
      const dentroDaLista = listaRef.current?.contains(e.target as Node)
      const dentroDoInput = containerRef.current?.contains(e.target as Node)
      if (!dentroDoInput && !dentroDaLista) fechar()
    }
    document.addEventListener('mousedown', onClickFora)
    return () => document.removeEventListener('mousedown', onClickFora)
  }, [aberto, value, opcoes])

  useEffect(() => {
    if (indice >= 0 && listaRef.current) {
      const item = listaRef.current.children[indice] as HTMLElement
      item?.scrollIntoView({ block: 'nearest' })
    }
  }, [indice])

  const opcoesFiltradas = opcoes.filter(o =>
    o.label.toLowerCase().includes(textoInput.toLowerCase()),
  )

  function calcularPos() {
    if (inputRef.current) {
      const r = inputRef.current.getBoundingClientRect()
      setPosDropdown({ top: r.bottom + 4, left: r.left, width: r.width })
    }
  }

  function abrir() {
    calcularPos()
    setTextoInput('')
    setIndice(-1)
    setAberto(true)
  }

  function fechar() {
    setAberto(false)
    setIndice(-1)
    const opcao = opcoes.find(o => o.value === value)
    setTextoInput(opcao?.label ?? '')
  }

  function selecionarOpcao(opcao: OpcaoSAC) {
    onChange(opcao.value)
    setTextoInput(opcao.label)
    setAberto(false)
    setIndice(-1)
  }

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    setTextoInput(e.target.value)
    setIndice(-1)
    if (!aberto) calcularPos()
    setAberto(true)
    if (!e.target.value) onChange('')
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (!aberto) {
      if (e.key === 'ArrowDown' || e.key === 'Enter') {
        e.preventDefault()
        abrir()
      }
      return
    }
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setIndice(prev => Math.min(prev + 1, opcoesFiltradas.length - 1))
        break
      case 'ArrowUp':
        e.preventDefault()
        setIndice(prev => Math.max(prev - 1, -1))
        break
      case 'Enter':
        e.preventDefault()
        if (indice >= 0 && opcoesFiltradas[indice])
          selecionarOpcao(opcoesFiltradas[indice])
        break
      case 'Escape':
        e.preventDefault()
        fechar()
        break
      case 'Tab':
        fechar()
        break
    }
  }

  const dropdownStyle: React.CSSProperties = {
    position: 'fixed',
    top: posDropdown.top,
    left: posDropdown.left,
    width: posDropdown.width,
    zIndex: 9999,
  }

  return (
    <div ref={containerRef} className="sac-container">
      <div className="sac-input-wrapper">
        <input
          ref={inputRef}
          id={id}
          type="text"
          className={`sac-input${className ? ` ${className}` : ''}`}
          placeholder={carregando ? 'Carregando…' : placeholder}
          value={textoInput}
          onChange={handleInputChange}
          onFocus={abrir}
          onKeyDown={handleKeyDown}
          disabled={disabled || carregando}
          autoComplete="off"
          role="combobox"
          aria-expanded={aberto}
          aria-autocomplete="list"
        />
        <span
          className={`sac-seta${aberto ? ' sac-seta--aberta' : ''}`}
          aria-hidden="true"
        />
      </div>

      {aberto &&
        typeof window !== 'undefined' &&
        createPortal(
          <ul
            ref={listaRef}
            className="sac-lista"
            style={dropdownStyle}
            role="listbox"
          >
            {erroCarreg ? (
              <li className="sac-item sac-item--estado">{erroCarreg}</li>
            ) : opcoesFiltradas.length === 0 ? (
              <li className="sac-item sac-item--estado">
                Nenhum resultado encontrado
              </li>
            ) : (
              opcoesFiltradas.map((opcao, i) => (
                <li
                  key={opcao.value}
                  className={[
                    'sac-item',
                    value === opcao.value ? 'sac-item--selecionado' : '',
                    i === indice ? 'sac-item--ativo' : '',
                  ]
                    .filter(Boolean)
                    .join(' ')}
                  role="option"
                  aria-selected={value === opcao.value}
                  onMouseDown={() => selecionarOpcao(opcao)}
                >
                  {opcao.label}
                </li>
              ))
            )}
          </ul>,
          document.body,
        )}
    </div>
  )
}
