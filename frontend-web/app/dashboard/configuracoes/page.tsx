'use client'

import { useEffect, useRef, useState, ChangeEvent } from 'react'
import { Building2, Image as ImageIcon, Trash2, Upload } from 'lucide-react'
import { configuracaoAPI } from '@/lib/configuracao.service'
import { MENSAGENS, mensagemErro } from '@/lib/mensagens'
import {
  mascaraCEP,
  mascaraCNPJ,
  mascaraTelefone,
  somenteDigitos,
} from '@/lib/mascaras'
import {
  ConfiguracaoClinica,
  TAMANHO_MAXIMO_LOGO_BYTES,
  TIPOS_LOGO_ACEITOS,
} from '@/tipos/configuracao'
import { useNotificacao } from '@/contexts/ToastContext'
import '../usuarios/usuariosPage.css'
import './configuracoesPage.css'

// ── Configuração da clínica ──────────────────────────────────────
//
// O produto roda em modelo silo: uma instalação por cliente. Esta tela é o
// que torna cada instalação configurável sem recompilar — o que antes era
// constante no código (PdfBuilder.NOME_CLINICA) agora vive no banco e
// aparece no cabeçalho dos atestados e relatórios.

interface FormularioConfiguracao {
  nome: string
  cnpj: string
  telefone: string
  email: string
  site: string
  cep: string
  logradouro: string
  numero: string
  complemento: string
  bairro: string
  cidade: string
  uf: string
  rodapeDocumentos: string
}

const formVazio: FormularioConfiguracao = {
  nome: '',
  cnpj: '',
  telefone: '',
  email: '',
  site: '',
  cep: '',
  logradouro: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  uf: '',
  rodapeDocumentos: '',
}

function paraFormulario(c: ConfiguracaoClinica): FormularioConfiguracao {
  return {
    nome: c.nome ?? '',
    cnpj: mascaraCNPJ(c.cnpj ?? ''),
    telefone: mascaraTelefone(c.telefone ?? ''),
    email: c.email ?? '',
    site: c.site ?? '',
    cep: mascaraCEP(c.endereco?.cep ?? ''),
    logradouro: c.endereco?.logradouro ?? '',
    numero: c.endereco?.numero ?? '',
    complemento: c.endereco?.complemento ?? '',
    bairro: c.endereco?.bairro ?? '',
    cidade: c.endereco?.cidade ?? '',
    uf: c.endereco?.uf ?? '',
    rodapeDocumentos: c.rodapeDocumentos ?? '',
  }
}

const vazioParaUndefined = (v: string) => v.trim() || undefined

export default function ConfiguracoesPage() {
  const { exibirNotificacao } = useNotificacao()

  const [form, setForm] = useState<FormularioConfiguracao>(formVazio)
  const [temLogo, setTemLogo] = useState(false)
  const [carregando, setCarregando] = useState(true)
  const [salvando, setSalvando] = useState(false)
  const [enviandoLogo, setEnviandoLogo] = useState(false)
  /** Muda a cada upload para furar o cache do navegador — a URL da logo é fixa. */
  const [versaoLogo, setVersaoLogo] = useState<string>('')

  const inputLogoRef = useRef<HTMLInputElement>(null)

  const aplicar = (c: ConfiguracaoClinica) => {
    setForm(paraFormulario(c))
    setTemLogo(c.temLogo)
    setVersaoLogo(c.atualizadoEm ?? String(Date.now()))
  }

  useEffect(() => {
    configuracaoAPI
      .buscar()
      .then(aplicar)
      .catch(err =>
        exibirNotificacao(
          mensagemErro(err, MENSAGENS.erro.carregarConfiguracao),
          'error',
          5000,
        ),
      )
      .finally(() => setCarregando(false))
  }, [exibirNotificacao])

  const alterar =
    (campo: keyof FormularioConfiguracao, mascara?: (v: string) => string) =>
    (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      const valor = mascara ? mascara(e.target.value) : e.target.value
      setForm(prev => ({ ...prev, [campo]: valor }))
    }

  const salvar = async () => {
    if (!form.nome.trim()) {
      exibirNotificacao(MENSAGENS.validacao.nomeObrigatorio, 'error', 5000)
      return
    }
    try {
      setSalvando(true)
      const atualizada = await configuracaoAPI.atualizar({
        nome: form.nome.trim(),
        // CNPJ e CEP são gravados só com dígitos.
        cnpj: somenteDigitos(form.cnpj),
        telefone: vazioParaUndefined(form.telefone),
        email: vazioParaUndefined(form.email),
        site: vazioParaUndefined(form.site),
        endereco: {
          cep: somenteDigitos(form.cep),
          logradouro: vazioParaUndefined(form.logradouro),
          numero: vazioParaUndefined(form.numero),
          complemento: vazioParaUndefined(form.complemento),
          bairro: vazioParaUndefined(form.bairro),
          cidade: vazioParaUndefined(form.cidade),
          uf: form.uf.trim().toUpperCase() || undefined,
        },
        rodapeDocumentos: vazioParaUndefined(form.rodapeDocumentos),
      })
      aplicar(atualizada)
      exibirNotificacao(MENSAGENS.sucesso.configuracaoSalva, 'success')
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.salvarConfiguracao),
        'error',
        8000,
      )
    } finally {
      setSalvando(false)
    }
  }

  const aoEscolherLogo = async (e: ChangeEvent<HTMLInputElement>) => {
    const arquivo = e.target.files?.[0]
    e.target.value = '' // permite reenviar o mesmo arquivo
    if (!arquivo) return

    // Validação espelha o backend, para o usuário não descobrir só no 422.
    if (
      !TIPOS_LOGO_ACEITOS.includes(
        arquivo.type as (typeof TIPOS_LOGO_ACEITOS)[number],
      )
    ) {
      exibirNotificacao(MENSAGENS.erro.logoTipoInvalido, 'error', 5000)
      return
    }
    if (arquivo.size > TAMANHO_MAXIMO_LOGO_BYTES) {
      exibirNotificacao(MENSAGENS.erro.logoTamanho, 'error', 5000)
      return
    }

    try {
      setEnviandoLogo(true)
      aplicar(await configuracaoAPI.enviarLogo(arquivo))
      setVersaoLogo(String(Date.now()))
      exibirNotificacao(MENSAGENS.sucesso.logoEnviada, 'success')
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.enviarLogo),
        'error',
        8000,
      )
    } finally {
      setEnviandoLogo(false)
    }
  }

  const removerLogo = async () => {
    try {
      setEnviandoLogo(true)
      aplicar(await configuracaoAPI.removerLogo())
      exibirNotificacao(MENSAGENS.sucesso.logoRemovida, 'success')
    } catch (err) {
      exibirNotificacao(
        mensagemErro(err, MENSAGENS.erro.removerLogo),
        'error',
        8000,
      )
    } finally {
      setEnviandoLogo(false)
    }
  }

  if (carregando) {
    return (
      <div className="cfg-estado">
        <div className="cfg-spinner" />
        <p>Carregando configuração…</p>
      </div>
    )
  }

  return (
    <div className="cfg-page">
      <div className="cfg-header">
        <h1>Configurações da clínica</h1>
        <p className="cfg-subtitulo">
          Estes dados aparecem no cabeçalho dos atestados e relatórios em PDF e
          na tela de login.
        </p>
      </div>

      {/* Marca */}
      <section className="cfg-card">
        <h2 className="cfg-card-titulo">
          <ImageIcon size={16} />
          Logo
        </h2>

        <div className="cfg-logo-area">
          <div className="cfg-logo-preview">
            {temLogo ? (
              // <img> e não next/image: a logo vem de um endpoint da API, com
              // dimensões desconhecidas, e o otimizador não agrega aqui.
              <img
                src={configuracaoAPI.urlLogo(versaoLogo)}
                alt="Logo da clínica"
                className="cfg-logo-img"
              />
            ) : (
              <div className="cfg-logo-vazia">
                <ImageIcon size={28} />
                <span>Sem logo</span>
              </div>
            )}
          </div>

          <div className="cfg-logo-acoes">
            <button
              className="btn-primario"
              onClick={() => inputLogoRef.current?.click()}
              disabled={enviandoLogo}
            >
              <Upload size={15} />
              {enviandoLogo
                ? 'Enviando…'
                : temLogo
                  ? 'Trocar logo'
                  : 'Enviar logo'}
            </button>

            {temLogo && (
              <button
                className="btn-secundario cfg-btn-perigo"
                onClick={removerLogo}
                disabled={enviandoLogo}
              >
                <Trash2 size={15} />
                Remover
              </button>
            )}

            <span className="cfg-hint">JPG, PNG ou WEBP · até 2 MB</span>
          </div>

          <input
            ref={inputLogoRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={aoEscolherLogo}
            hidden
          />
        </div>
      </section>

      {/* Identificação */}
      <section className="cfg-card">
        <h2 className="cfg-card-titulo">
          <Building2 size={16} />
          Identificação
        </h2>

        <div className="cfg-grid">
          <label className="cfg-campo cfg-campo--largo">
            <span>
              Nome da clínica <b className="cfg-obrigatorio">*</b>
            </span>
            <input
              value={form.nome}
              onChange={alterar('nome')}
              maxLength={150}
              placeholder="Clínica Vida &amp; Saúde"
            />
          </label>
          <label className="cfg-campo">
            <span>CNPJ</span>
            <input
              value={form.cnpj}
              onChange={alterar('cnpj', mascaraCNPJ)}
              placeholder="00.000.000/0001-00"
            />
          </label>
          <label className="cfg-campo">
            <span>Telefone</span>
            <input
              value={form.telefone}
              onChange={alterar('telefone', mascaraTelefone)}
              maxLength={20}
              placeholder="(81) 3333-3333"
            />
          </label>
          <label className="cfg-campo">
            <span>E-mail</span>
            <input
              value={form.email}
              onChange={alterar('email')}
              maxLength={150}
              placeholder="contato@clinica.com.br"
            />
          </label>
          <label className="cfg-campo">
            <span>Site</span>
            <input
              value={form.site}
              onChange={alterar('site')}
              maxLength={150}
              placeholder="www.clinica.com.br"
            />
          </label>
        </div>
      </section>

      {/* Endereço */}
      <section className="cfg-card">
        <h2 className="cfg-card-titulo">Endereço</h2>

        <div className="cfg-grid">
          <label className="cfg-campo">
            <span>CEP</span>
            <input
              value={form.cep}
              onChange={alterar('cep', mascaraCEP)}
              placeholder="00000-000"
            />
          </label>
          <label className="cfg-campo cfg-campo--largo">
            <span>Logradouro</span>
            <input
              value={form.logradouro}
              onChange={alterar('logradouro')}
              maxLength={150}
            />
          </label>
          <label className="cfg-campo">
            <span>Número</span>
            <input
              value={form.numero}
              onChange={alterar('numero')}
              maxLength={20}
            />
          </label>
          <label className="cfg-campo">
            <span>Complemento</span>
            <input
              value={form.complemento}
              onChange={alterar('complemento')}
              maxLength={100}
            />
          </label>
          <label className="cfg-campo">
            <span>Bairro</span>
            <input
              value={form.bairro}
              onChange={alterar('bairro')}
              maxLength={100}
            />
          </label>
          <label className="cfg-campo">
            <span>Cidade</span>
            <input
              value={form.cidade}
              onChange={alterar('cidade')}
              maxLength={100}
            />
          </label>
          <label className="cfg-campo cfg-campo--curto">
            <span>UF</span>
            <input
              value={form.uf}
              onChange={alterar('uf', v =>
                v
                  .replace(/[^A-Za-z]/g, '')
                  .slice(0, 2)
                  .toUpperCase(),
              )}
              placeholder="PE"
            />
          </label>
        </div>
      </section>

      {/* Documentos */}
      <section className="cfg-card">
        <h2 className="cfg-card-titulo">Documentos</h2>

        <label className="cfg-campo cfg-campo--bloco">
          <span>Rodapé dos documentos</span>
          <textarea
            value={form.rodapeDocumentos}
            onChange={alterar('rodapeDocumentos')}
            maxLength={2000}
            rows={3}
            placeholder="Ex.: Responsável técnica: Maria da Silva — COREN-PE 123456"
          />
          <small className="cfg-hint">
            Texto livre impresso no rodapé de atestados e relatórios, acima da
            linha automática de emissão.
          </small>
        </label>
      </section>

      <div className="cfg-acoes">
        <button className="btn-primario" onClick={salvar} disabled={salvando}>
          {salvando ? 'Salvando…' : 'Salvar configurações'}
        </button>
      </div>
    </div>
  )
}
