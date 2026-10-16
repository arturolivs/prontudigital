'use client'

import { ReactNode, useEffect, useId, useRef, useState } from 'react'
import { AlertCircle } from 'lucide-react'
import { MENSAGENS } from '@/lib/mensagens'
import './CampoValidado.css'

type Controle = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement

type MotivoInvalido =
  | 'valueMissing'
  | 'typeMismatch'
  | 'tooShort'
  | 'tooLong'
  | 'patternMismatch'
  | 'rangeUnderflow'
  | 'rangeOverflow'
  | 'badInput'

const SELETOR_CONTROLE = 'input, select, textarea'

// Só controles: `form :invalid` também casaria <fieldset> e o próprio <form>.
const SELETOR_INVALIDO = 'input:invalid, select:invalid, textarea:invalid'

function mensagemPara(
  el: Controle,
  personalizadas?: Partial<Record<MotivoInvalido, string>>,
): string {
  const v = el.validity
  const m = MENSAGENS.validacao
  const minimo = (el as HTMLInputElement).minLength

  const padrao: [MotivoInvalido, boolean, string][] = [
    ['valueMissing', v.valueMissing, m.campoObrigatorio],
    [
      'typeMismatch',
      v.typeMismatch,
      el.type === 'email' ? m.emailFormato : m.formatoInvalido,
    ],
    ['tooShort', v.tooShort, `Use pelo menos ${minimo} caracteres`],
    ['tooLong', v.tooLong, m.formatoInvalido],
    ['patternMismatch', v.patternMismatch, el.title || m.formatoInvalido],
    ['rangeUnderflow', v.rangeUnderflow, m.valorMinimo],
    ['rangeOverflow', v.rangeOverflow, m.valorMaximo],
    ['badInput', v.badInput, m.formatoInvalido],
  ]

  const motivo = padrao.find(([, ativo]) => ativo)
  if (motivo) return personalizadas?.[motivo[0]] ?? motivo[2]
  // customError (setCustomValidity) e casos não mapeados.
  return el.validationMessage
}

interface CampoValidadoProps {
  /** O controle (input/select/textarea), direto ou dentro de um wrapper. */
  children: ReactNode
  /**
   * Erro vindo de validação feita em JS. Tem precedência sobre o da
   * validação nativa do navegador.
   */
  erro?: string
  /** Há um botão à direita do campo (ex.: mostrar senha): afasta o ícone. */
  acaoDireita?: boolean
  /** Sobrescreve a mensagem padrão de um motivo de invalidez. */
  mensagens?: Partial<Record<MotivoInvalido, string>>
}

/**
 * Exibe o erro de um campo no padrão do sistema — mensagem em vermelho
 * entre o label e o campo, borda vermelha e ícone de alerta — no lugar do
 * balão nativo do navegador.
 *
 * A validação continua sendo a nativa (`required`, `type="email"`,
 * `minLength`, `min`…): o componente só intercepta o evento `invalid`,
 * cancela o balão e renderiza a mensagem. O erro some assim que o valor
 * fica válido.
 */
export default function CampoValidado({
  children,
  erro,
  acaoDireita = false,
  mensagens,
}: CampoValidadoProps) {
  const raizRef = useRef<HTMLDivElement>(null)
  const mensagensRef = useRef(mensagens)
  mensagensRef.current = mensagens
  const [erroNativo, setErroNativo] = useState<string | null>(null)
  const idMensagem = useId()

  const erroExibido = erro || erroNativo

  useEffect(() => {
    const raiz = raizRef.current
    if (!raiz) return

    // `invalid` não borbulha, mas passa pela fase de captura dos ancestrais.
    const aoInvalidar = (e: Event) => {
      const el = e.target as Controle
      e.preventDefault()
      setErroNativo(mensagemPara(el, mensagensRef.current))

      // Cancelar o evento também cancela o foco automático do navegador:
      // devolve o foco só ao primeiro campo inválido do formulário.
      if (el.form?.querySelector(SELETOR_INVALIDO) === el) el.focus()
    }

    const aoEditar = (e: Event) => {
      const el = e.target as Controle
      if (!el.matches?.(SELETOR_CONTROLE)) return
      setErroNativo(atual =>
        atual === null
          ? null
          : el.validity.valid
            ? null
            : mensagemPara(el, mensagensRef.current),
      )
    }

    raiz.addEventListener('invalid', aoInvalidar, true)
    raiz.addEventListener('input', aoEditar)
    raiz.addEventListener('change', aoEditar)
    return () => {
      raiz.removeEventListener('invalid', aoInvalidar, true)
      raiz.removeEventListener('input', aoEditar)
      raiz.removeEventListener('change', aoEditar)
    }
  }, [])

  // Liga o controle à mensagem para leitores de tela.
  useEffect(() => {
    const el = raizRef.current?.querySelector(SELETOR_CONTROLE)
    if (!el) return
    if (erroExibido) {
      el.setAttribute('aria-invalid', 'true')
      el.setAttribute('aria-describedby', idMensagem)
    } else {
      el.removeAttribute('aria-invalid')
      el.removeAttribute('aria-describedby')
    }
  }, [erroExibido, idMensagem])

  const classes = [
    'campo-validado',
    erroExibido && 'campo-validado--erro',
    acaoDireita && 'campo-validado--com-acao',
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <div ref={raizRef} className={classes}>
      {erroExibido && (
        <p id={idMensagem} className="campo-validado-mensagem">
          {erroExibido}
        </p>
      )}
      <div className="campo-validado-controle">
        {children}
        {erroExibido && (
          <AlertCircle
            size={16}
            className="campo-validado-icone"
            aria-hidden="true"
          />
        )}
      </div>
    </div>
  )
}
