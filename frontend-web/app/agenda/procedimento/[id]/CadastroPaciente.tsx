'use client'

import { UserCog } from 'lucide-react'
import { Usuario, AtualizarPerfilRequisicao } from '@/tipos/autenticacao'
import {
  mascaraCEP,
  mascaraCPF,
  mascaraTelefone,
  somenteDigitos,
} from '@/lib/mascaras'
import { Campo, CampoTexto, MudancaCampo, SubSecao } from './campos'

// ── Cadastro do paciente ─────────────────────────────────────────
//
// Ao agendar, o paciente pode se cadastrar informando apenas nome e telefone
// (CadastrarPacienteDTO). Os demais campos do RF04 — CPF, data de nascimento e
// endereço — ficam em branco. Esta ficha permite completá-los durante o
// atendimento, quando o paciente está presente para informar.
//
// ATENÇÃO: `PATCH /api/usuarios/{id}/perfil` SUBSTITUI nomeCompleto, email,
// telefone, cpf e dataNascimento pelo que vier na requisição — não é um patch
// parcial de verdade (só o endereço é preservado quando ausente). Por isso o
// formulário carrega todos os campos e reenvia todos: omitir o telefone
// apagaria justamente o dado que o paciente informou no agendamento.

export type CadastroPacienteForm = {
  nomeCompleto: string
  email: string
  telefone: string
  cpf: string
  dataNascimento: string
  cep: string
  logradouro: string
  numero: string
  complemento: string
  bairro: string
  cidade: string
  uf: string
}

export const CADASTRO_PACIENTE_INICIAL: CadastroPacienteForm = {
  nomeCompleto: '',
  email: '',
  telefone: '',
  cpf: '',
  dataNascimento: '',
  cep: '',
  logradouro: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  uf: '',
}

/** Preenche o formulário com o que já existe no cadastro. */
export function cadastroPacienteParaFormulario(
  usuario: Usuario,
): CadastroPacienteForm {
  const e = usuario.endereco
  return {
    nomeCompleto: usuario.nomeCompleto ?? '',
    email: usuario.email ?? '',
    // O backend guarda sem máscara; a máscara existe só na edição.
    telefone: mascaraTelefone(usuario.telefone ?? ''),
    cpf: mascaraCPF(usuario.cpf ?? ''),
    // <input type="date"> exige yyyy-MM-dd, que é o formato que a API devolve.
    dataNascimento: usuario.dataNascimento ?? '',
    cep: mascaraCEP(e?.cep ?? ''),
    logradouro: e?.logradouro ?? '',
    numero: e?.numero ?? '',
    complemento: e?.complemento ?? '',
    bairro: e?.bairro ?? '',
    cidade: e?.cidade ?? '',
    uf: e?.uf ?? '',
  }
}

const vazioParaUndefined = (valor: string): string | undefined =>
  valor.trim() || undefined

export function cadastroPacienteParaApi(
  form: CadastroPacienteForm,
): AtualizarPerfilRequisicao {
  return {
    // Único campo obrigatório do DTO (@NotBlank) — vai sempre.
    nomeCompleto: form.nomeCompleto.trim(),
    email: vazioParaUndefined(form.email),
    telefone: vazioParaUndefined(form.telefone),
    // CPF e CEP são gravados apenas com dígitos.
    cpf: somenteDigitos(form.cpf),
    dataNascimento: vazioParaUndefined(form.dataNascimento),
    endereco: {
      cep: somenteDigitos(form.cep),
      logradouro: vazioParaUndefined(form.logradouro),
      numero: vazioParaUndefined(form.numero),
      complemento: vazioParaUndefined(form.complemento),
      bairro: vazioParaUndefined(form.bairro),
      cidade: vazioParaUndefined(form.cidade),
      uf: form.uf.trim().toUpperCase() || undefined,
    },
  }
}

/** Campos do RF04 que ainda faltam — usado para sinalizar no cabeçalho. */
export function camposPendentes(form: CadastroPacienteForm): number {
  const obrigatoriosDoCadastroCompleto = [
    form.cpf,
    form.dataNascimento,
    form.cep,
    form.logradouro,
    form.numero,
    form.bairro,
    form.cidade,
    form.uf,
  ]
  return obrigatoriosDoCadastroCompleto.filter(v => !v.trim()).length
}

export default function CadastroPaciente({
  form,
  onChange,
  onSalvar,
  salvando,
  carregando,
}: {
  form: CadastroPacienteForm
  onChange: (campo: keyof CadastroPacienteForm) => (e: MudancaCampo) => void
  onSalvar: () => void
  salvando: boolean
  carregando: boolean
}) {
  const pendentes = camposPendentes(form)

  return (
    <div className="proc-card proc-full">
      <div className="proc-card-header">
        <UserCog size={16} />
        Cadastro do paciente
        {!carregando && pendentes > 0 && (
          <span className="proc-cadastro-pendente">
            {pendentes} campo{pendentes !== 1 ? 's' : ''} a completar
          </span>
        )}
      </div>

      {carregando ? (
        <div className="proc-cadastro-carregando">
          <div className="proc-spinner" />
          <p>Carregando cadastro…</p>
        </div>
      ) : (
        <div className="proc-form">
          <p className="proc-legenda">
            No agendamento o paciente informa apenas nome e telefone. Complete
            aqui os dados do cadastro (RF04) — os campos já preenchidos vêm
            carregados.
          </p>

          <SubSecao titulo="Dados pessoais" />
          <div className="proc-form-grid">
            <CampoTexto
              label="Nome completo"
              valor={form.nomeCompleto}
              onChange={onChange('nomeCompleto')}
              maxLength={150}
              placeholder="Nome civil completo"
            />
            <CampoTexto
              label="Data de nascimento"
              tipo="date"
              valor={form.dataNascimento}
              onChange={onChange('dataNascimento')}
            />
          </div>

          <div className="proc-form-grid">
            <CampoTexto
              label="CPF"
              valor={form.cpf}
              onChange={onChange('cpf')}
              placeholder="000.000.000-00"
            />
            <CampoTexto
              label="Telefone"
              valor={form.telefone}
              onChange={onChange('telefone')}
              maxLength={20}
              placeholder="(00) 0 0000-0000"
            />
          </div>

          <CampoTexto
            label="E-mail"
            valor={form.email}
            onChange={onChange('email')}
            maxLength={150}
            placeholder="paciente@email.com"
          />

          <SubSecao titulo="Endereço" />
          <div className="proc-form-grid">
            <CampoTexto
              label="CEP"
              valor={form.cep}
              onChange={onChange('cep')}
              placeholder="00000-000"
            />
            <CampoTexto
              label="Logradouro"
              valor={form.logradouro}
              onChange={onChange('logradouro')}
              maxLength={150}
              placeholder="Rua, avenida…"
            />
          </div>

          <div className="proc-form-grid-3">
            <CampoTexto
              label="Número"
              valor={form.numero}
              onChange={onChange('numero')}
              maxLength={20}
              placeholder="123"
            />
            <CampoTexto
              label="Complemento"
              valor={form.complemento}
              onChange={onChange('complemento')}
              maxLength={100}
              placeholder="Apto, bloco…"
            />
            <CampoTexto
              label="Bairro"
              valor={form.bairro}
              onChange={onChange('bairro')}
              maxLength={100}
            />
          </div>

          <div className="proc-form-grid">
            <CampoTexto
              label="Cidade"
              valor={form.cidade}
              onChange={onChange('cidade')}
              maxLength={100}
            />
            <Campo label="UF">
              <input
                className="proc-campo-input"
                value={form.uf}
                onChange={onChange('uf')}
                maxLength={2}
                placeholder="PE"
                style={{ textTransform: 'uppercase' }}
              />
            </Campo>
          </div>

          <div className="proc-cadastro-acoes">
            <button
              className="proc-btn-finalizar"
              onClick={onSalvar}
              disabled={salvando}
            >
              {salvando ? 'Salvando…' : 'Salvar cadastro'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
