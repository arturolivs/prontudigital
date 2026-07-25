'use client'

import { useState, useRef, useEffect } from 'react'
import { Usuario } from '@/tipos/autenticacao'
import Modal from '@/components/Modal'
import {
  mascaraCEP,
  mascaraCPF,
  mascaraTelefone,
  somenteDigitos,
} from '@/lib/mascaras'
import './FormularioUsuarioModal.style.css'

const OPCOES_PERFIL = [
  { label: 'Administrador', value: 'ROLE_ADMIN' },
  { label: 'Enfermeiro', value: 'ROLE_PROFISSIONAL' },
  { label: 'Usuário', value: 'USUARIO' },
  { label: 'Paciente', value: 'ROLE_PACIENTE' },
]

// ── Combobox multi-seleção de perfis ─────────────────────────────────────────

function PerfisCombobox({
  value,
  onChange,
}: {
  value: string[]
  onChange: (perfis: string[]) => void
}) {
  const [aberto, setAberto] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function fecharAoClicarFora(e: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setAberto(false)
      }
    }
    document.addEventListener('mousedown', fecharAoClicarFora)
    return () => document.removeEventListener('mousedown', fecharAoClicarFora)
  }, [])

  const toggle = (perfil: string) =>
    onChange(
      value.includes(perfil)
        ? value.filter(p => p !== perfil)
        : [...value, perfil],
    )

  const textoSelecionados =
    value.length === 0
      ? null
      : value
          .map(v => OPCOES_PERFIL.find(o => o.value === v)?.label ?? v)
          .join(', ')

  return (
    <div className="uf-combobox" ref={containerRef}>
      <button
        type="button"
        className={`uf-combobox-trigger${aberto ? ' uf-combobox-trigger--aberto' : ''}`}
        onClick={() => setAberto(prev => !prev)}
        aria-haspopup="listbox"
        aria-expanded={aberto}
      >
        <span
          className={`uf-combobox-valor${!textoSelecionados ? ' uf-combobox-placeholder' : ''}`}
        >
          {textoSelecionados ?? 'Selecione os perfis...'}
        </span>
        <svg
          className={`uf-combobox-seta${aberto ? ' uf-combobox-seta--aberta' : ''}`}
          viewBox="0 0 20 20"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <path
            d="M5 7.5L10 12.5L15 7.5"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      {aberto && (
        <ul
          className="uf-combobox-lista"
          role="listbox"
          aria-multiselectable="true"
        >
          {OPCOES_PERFIL.map(opcao => {
            const selecionado = value.includes(opcao.value)
            return (
              <li
                key={opcao.value}
                role="option"
                aria-selected={selecionado}
                className={`uf-combobox-opcao${selecionado ? ' uf-combobox-opcao--selecionada' : ''}`}
                onClick={() => toggle(opcao.value)}
              >
                <span className="uf-combobox-check" aria-hidden="true">
                  {selecionado && (
                    <svg
                      viewBox="0 0 12 12"
                      fill="none"
                      xmlns="http://www.w3.org/2000/svg"
                    >
                      <path
                        d="M2 6L5 9L10 3"
                        stroke="currentColor"
                        strokeWidth="1.8"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  )}
                </span>
                {opcao.label}
                <span className="uf-badge-perfil">{opcao.value}</span>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}

interface FormularioUsuarioModalProps {
  user: Usuario | null
  onClose: () => void
  onSave: (data: Partial<Usuario> & { senha?: string }) => void
}

export default function FormularioUsuarioModal({
  user,
  onClose,
  onSave,
}: FormularioUsuarioModalProps) {
  const editando = user !== null

  const [formData, setFormData] = useState({
    nomeCompleto: user?.nomeCompleto ?? '',
    username: user?.username ?? '',
    email: user?.email ?? '',
    telefone: mascaraTelefone(user?.telefone ?? ''),
    senha: '',
    ativo: user?.ativo !== undefined ? user.ativo : true,
    perfis: user?.perfis ?? ['USUARIO'],
    // RF04 — dados do paciente
    cpf: mascaraCPF(user?.cpf ?? ''),
    dataNascimento: user?.dataNascimento ?? '',
    cep: mascaraCEP(user?.endereco?.cep ?? ''),
    logradouro: user?.endereco?.logradouro ?? '',
    numero: user?.endereco?.numero ?? '',
    complemento: user?.endereco?.complemento ?? '',
    bairro: user?.endereco?.bairro ?? '',
    cidade: user?.endereco?.cidade ?? '',
    uf: user?.endereco?.uf ?? '',
    // RF05 — credenciais do profissional
    coren: user?.coren ?? '',
    especialidade: user?.especialidade ?? '',
  })

  const [salvando, setSalvando] = useState(false)

  const ehProfissional = formData.perfis.includes('PROFISSIONAL')

  const set =
    (field: keyof typeof formData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setFormData(prev => ({ ...prev, [field]: e.target.value }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSalvando(true)
    try {
      const {
        cpf,
        cep,
        logradouro,
        numero,
        complemento,
        bairro,
        cidade,
        uf,
        ...resto
      } = formData

      const endereco = {
        cep: somenteDigitos(cep),
        logradouro: logradouro || undefined,
        numero: numero || undefined,
        complemento: complemento || undefined,
        bairro: bairro || undefined,
        cidade: cidade || undefined,
        uf: uf ? uf.toUpperCase() : undefined,
      }
      const enderecoPreenchido = Object.values(endereco).some(Boolean)

      const payload = {
        ...resto,
        // O backend guarda o CPF só com dígitos.
        cpf: somenteDigitos(cpf),
        dataNascimento: resto.dataNascimento || undefined,
        endereco: enderecoPreenchido ? endereco : undefined,
        // COREN/especialidade só fazem sentido para profissional.
        coren: ehProfissional ? resto.coren || undefined : undefined,
        especialidade: ehProfissional
          ? resto.especialidade || undefined
          : undefined,
        senha: editando && !resto.senha ? undefined : resto.senha,
      }

      await onSave(payload)
    } finally {
      setSalvando(false)
    }
  }

  const rodape = (
    <>
      <button type="button" className="uf-btn-cancelar" onClick={onClose}>
        Cancelar
      </button>
      <button
        type="submit"
        form="uf-form"
        className="uf-btn-salvar"
        disabled={salvando}
      >
        {salvando ? 'Salvando…' : 'Salvar'}
      </button>
    </>
  )

  return (
    <Modal
      titulo={editando ? 'Editar Usuário' : 'Novo Usuário'}
      onClose={onClose}
      rodape={rodape}
    >
      <form id="uf-form" onSubmit={handleSubmit} className="uf-form">
        <div className="uf-grupo">
          <label className="uf-label">Nome Completo</label>
          <input
            type="text"
            value={formData.nomeCompleto}
            onChange={set('nomeCompleto')}
            className="uf-input"
            placeholder="Nome completo do usuário"
            required
            autoFocus
          />
        </div>

        <div className="uf-linha-dupla">
          <div className="uf-grupo">
            <label className="uf-label">Username</label>
            <input
              type="text"
              value={formData.username}
              onChange={set('username')}
              className="uf-input"
              placeholder="nome.usuario"
              required
              autoComplete="off"
            />
          </div>
          <div className="uf-grupo">
            <label className="uf-label">
              {editando ? 'Nova Senha' : 'Senha'}
              {editando && (
                <span className="uf-label-opcional">(opcional)</span>
              )}
            </label>
            <input
              type="password"
              value={formData.senha}
              onChange={set('senha')}
              className="uf-input"
              placeholder={
                editando ? 'Deixe em branco para manter' : 'Mínimo 8 caracteres'
              }
              required={!editando}
              minLength={!editando ? 8 : undefined}
              autoComplete="new-password"
            />
          </div>
        </div>

        <div className="uf-grupo">
          <label className="uf-label">Email</label>
          <input
            type="email"
            value={formData.email}
            onChange={set('email')}
            className="uf-input"
            placeholder="usuario@exemplo.com"
            required
          />
        </div>

        <div className="uf-linha-dupla">
          <div className="uf-grupo">
            <label className="uf-label">
              Telefone<span className="uf-label-opcional">(opcional)</span>
            </label>
            <input
              type="tel"
              value={formData.telefone}
              onChange={e => setFormData(prev => ({ ...prev, telefone: mascaraTelefone(e.target.value) }))}
              className="uf-input"
              placeholder="(99) 9 9999-9999"
            />
          </div>
          <div className="uf-grupo">
            <label className="uf-label">Status</label>
            <select
              value={formData.ativo ? 'true' : 'false'}
              onChange={e =>
                setFormData(prev => ({
                  ...prev,
                  ativo: e.target.value === 'true',
                }))
              }
              className="uf-select"
            >
              <option value="true">Ativo</option>
              <option value="false">Inativo</option>
            </select>
          </div>
        </div>

        <div className="uf-grupo">
          <label className="uf-label">Perfis</label>
          <PerfisCombobox
            value={formData.perfis}
            onChange={perfis => setFormData(prev => ({ ...prev, perfis }))}
          />
        </div>

        {/* RF04 — identificação e endereço */}
        <fieldset className="uf-secao">
          <legend className="uf-secao-titulo">Dados pessoais</legend>

          <div className="uf-linha-dupla">
            <div className="uf-grupo">
              <label className="uf-label">
                CPF<span className="uf-label-opcional">(opcional)</span>
              </label>
              <input
                type="text"
                value={formData.cpf}
                onChange={e =>
                  setFormData(prev => ({
                    ...prev,
                    cpf: mascaraCPF(e.target.value),
                  }))
                }
                className="uf-input"
                placeholder="000.000.000-00"
                inputMode="numeric"
              />
            </div>
            <div className="uf-grupo">
              <label className="uf-label">
                Data de nascimento
                <span className="uf-label-opcional">(opcional)</span>
              </label>
              <input
                type="date"
                value={formData.dataNascimento}
                onChange={set('dataNascimento')}
                className="uf-input"
                max={new Date().toISOString().split('T')[0]}
              />
            </div>
          </div>

          <div className="uf-linha-dupla">
            <div className="uf-grupo">
              <label className="uf-label">CEP</label>
              <input
                type="text"
                value={formData.cep}
                onChange={e =>
                  setFormData(prev => ({
                    ...prev,
                    cep: mascaraCEP(e.target.value),
                  }))
                }
                className="uf-input"
                placeholder="00000-000"
                inputMode="numeric"
              />
            </div>
            <div className="uf-grupo">
              <label className="uf-label">Logradouro</label>
              <input
                type="text"
                value={formData.logradouro}
                onChange={set('logradouro')}
                className="uf-input"
                placeholder="Rua, avenida..."
              />
            </div>
          </div>

          <div className="uf-linha-dupla">
            <div className="uf-grupo">
              <label className="uf-label">Número</label>
              <input
                type="text"
                value={formData.numero}
                onChange={set('numero')}
                className="uf-input"
              />
            </div>
            <div className="uf-grupo">
              <label className="uf-label">Complemento</label>
              <input
                type="text"
                value={formData.complemento}
                onChange={set('complemento')}
                className="uf-input"
                placeholder="Apto, bloco..."
              />
            </div>
          </div>

          <div className="uf-linha-tripla">
            <div className="uf-grupo">
              <label className="uf-label">Bairro</label>
              <input
                type="text"
                value={formData.bairro}
                onChange={set('bairro')}
                className="uf-input"
              />
            </div>
            <div className="uf-grupo">
              <label className="uf-label">Cidade</label>
              <input
                type="text"
                value={formData.cidade}
                onChange={set('cidade')}
                className="uf-input"
              />
            </div>
            <div className="uf-grupo">
              <label className="uf-label">UF</label>
              <input
                type="text"
                value={formData.uf}
                onChange={e =>
                  setFormData(prev => ({
                    ...prev,
                    uf: e.target.value.replace(/[^A-Za-z]/g, '').slice(0, 2),
                  }))
                }
                className="uf-input"
                placeholder="SP"
                maxLength={2}
              />
            </div>
          </div>
        </fieldset>

        {/* RF05 — só aparece para quem tem o perfil PROFISSIONAL */}
        {ehProfissional && (
          <fieldset className="uf-secao">
            <legend className="uf-secao-titulo">Dados profissionais</legend>
            <div className="uf-linha-dupla">
              <div className="uf-grupo">
                <label className="uf-label">COREN</label>
                <input
                  type="text"
                  value={formData.coren}
                  onChange={set('coren')}
                  className="uf-input"
                  placeholder="COREN-SP 123456"
                  maxLength={20}
                />
              </div>
              <div className="uf-grupo">
                <label className="uf-label">Especialidade</label>
                <input
                  type="text"
                  value={formData.especialidade}
                  onChange={set('especialidade')}
                  className="uf-input"
                  placeholder="Estomaterapia"
                  maxLength={100}
                />
              </div>
            </div>
          </fieldset>
        )}
      </form>
    </Modal>
  )
}
