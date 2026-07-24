'use client'

import { Activity, BookOpen, ClipboardCheck, Eye, Scissors } from 'lucide-react'
import {
  AvaliacaoEvolucao,
  BordasFerida,
  EvolucaoEnfermagem,
  EvolucaoEnfermagemRequisicao,
  ExsudatoTime,
  InfeccaoInflamacao,
  TecidoLeito,
  TipoDesbridamento,
  TipoFerida,
  TipoLimpeza,
} from '@/tipos/agendamento'
import {
  BloqueadoTag,
  CampoNumero,
  CampoSelect,
  CampoSimNao,
  CampoTexto,
  CampoTextarea,
  CheckItem,
  SubSecao,
} from './campos'

// ── Ficha de Evolução de Enfermagem ──────────────────────────────
// Modelo: ajustar_campos/modelos/FICHA DE EVOLUÇÃO DE ENFERMAGEM.pdf
// Preenchida apenas em agendamentos do tipo AVALIACAO.

/** Campos numéricos ficam como texto enquanto o profissional digita. */
type CamposNumericos =
  | 'comprimento'
  | 'largura'
  | 'profundidade'
  | 'dorEscala'
  | 'retornoDias'

export type EnfermagemForm = Omit<
  EvolucaoEnfermagemRequisicao,
  CamposNumericos
> &
  Record<CamposNumericos, string>

export const ENFERMAGEM_INICIAL: EnfermagemForm = {
  comprimento: '',
  largura: '',
  profundidade: '',
  dorEscala: '',
  retornoDias: '',
}

// ── Opções (modelo TIME – European Wound Management Association) ──

const OPCOES_TIPO_FERIDA: { value: TipoFerida; label: string }[] = [
  { value: 'CIRURGICA', label: 'Cirúrgica' },
  { value: 'TRAUMATICA', label: 'Traumática' },
  { value: 'ULCERA_VENOSA', label: 'Úlcera venosa' },
  {
    value: 'LESAO_PRESSAO',
    label: 'Lesão por pressão (classificação NPIAP)',
  },
  { value: 'PE_DIABETICO', label: 'Pé diabético' },
  { value: 'OUTRA', label: 'Outra' },
]

const OPCOES_TECIDO: { value: TecidoLeito; label: string }[] = [
  { value: 'GRANULACAO', label: 'Granulação' },
  { value: 'ESFACELO', label: 'Esfacelo' },
  { value: 'NECROSE', label: 'Necrose' },
  { value: 'EPITELIZACAO', label: 'Epitelização' },
]

const OPCOES_INFECCAO: { value: InfeccaoInflamacao; label: string }[] = [
  { value: 'AUSENTE', label: 'Ausente' },
  { value: 'EXSUDATO_PURULENTO', label: 'Exsudato purulento' },
  { value: 'ODOR', label: 'Odor' },
  { value: 'DOR', label: 'Dor' },
]

const OPCOES_EXSUDATO: { value: ExsudatoTime; label: string }[] = [
  { value: 'AUSENTE', label: 'Ausente' },
  { value: 'PEQUENO', label: 'Pequeno' },
  { value: 'MODERADO', label: 'Moderado' },
  { value: 'INTENSO', label: 'Intenso' },
]

const OPCOES_BORDAS: { value: BordasFerida; label: string }[] = [
  { value: 'INTEGRAS', label: 'Íntegras' },
  { value: 'MACERADAS', label: 'Maceradas' },
  { value: 'DESCOLADAS', label: 'Descoladas' },
  { value: 'EPITELIZANDO', label: 'Epitelizando' },
]

const OPCOES_LIMPEZA: { value: TipoLimpeza; label: string }[] = [
  { value: 'SF_09', label: 'SF 0,9%' },
  { value: 'OUTRO', label: 'Outro' },
]

const OPCOES_DESBRIDAMENTO: { value: TipoDesbridamento; label: string }[] = [
  { value: 'NAO', label: 'Não' },
  { value: 'AUTOLITICO', label: 'Autolítico' },
  { value: 'INSTRUMENTAL', label: 'Instrumental' },
  { value: 'ENZIMATICO', label: 'Enzimático' },
]

const OPCOES_EVOLUCAO: { value: AvaliacaoEvolucao; label: string }[] = [
  { value: 'MELHORA', label: 'Melhora' },
  { value: 'ESTAVEL', label: 'Estável' },
  { value: 'PIORA', label: 'Piora' },
]

// ── Conversões formulário ↔ API ──────────────────────────────────

const numParaTexto = (n?: number): string => (n == null ? '' : String(n))

/**
 * Data e hora do modelo vêm pré-preenchidas do início do agendamento
 * enquanto a ficha ainda não foi registrada.
 */
export function enfermagemParaFormulario(
  ficha: EvolucaoEnfermagem | undefined,
  inicioEm: string,
): EnfermagemForm {
  return {
    ...ENFERMAGEM_INICIAL,
    ...ficha,
    dataAvaliacao: ficha?.dataAvaliacao ?? inicioEm.slice(0, 10),
    horaAvaliacao: (ficha?.horaAvaliacao ?? inicioEm.slice(11, 16)).slice(0, 5),
    comprimento: numParaTexto(ficha?.comprimento),
    largura: numParaTexto(ficha?.largura),
    profundidade: numParaTexto(ficha?.profundidade),
    dorEscala: numParaTexto(ficha?.dorEscala),
    retornoDias: numParaTexto(ficha?.retornoDias),
  }
}

const num = (valor: string): number | undefined =>
  valor.trim() ? Number(valor) : undefined

export function enfermagemParaApi(
  form: EnfermagemForm,
): EvolucaoEnfermagemRequisicao {
  const {
    comprimento,
    largura,
    profundidade,
    dorEscala,
    retornoDias,
    ...resto
  } = form

  const semVazios = Object.fromEntries(
    Object.entries(resto).filter(([, valor]) => valor !== '' && valor != null),
  ) as EvolucaoEnfermagemRequisicao

  return {
    ...semVazios,
    comprimento: num(comprimento),
    largura: num(largura),
    profundidade: num(profundidade),
    dorEscala: num(dorEscala),
    retornoDias: num(retornoDias),
  }
}

export default function FichaEnfermagem({
  valor,
  aoAlterar,
  desabilitado,
  mostrarBloqueio,
}: {
  valor: EnfermagemForm
  aoAlterar: (mudanca: Partial<EnfermagemForm>) => void
  desabilitado: boolean
  mostrarBloqueio: boolean
}) {
  const texto =
    (campo: keyof EnfermagemForm) =>
    (e: { target: { value: string } }): void =>
      aoAlterar({ [campo]: e.target.value })

  const bool = (campo: keyof EnfermagemForm) => (marcado: boolean) =>
    aoAlterar({ [campo]: marcado })

  const t = (campo: keyof EnfermagemForm): string =>
    (valor[campo] as string | undefined) ?? ''

  const b = (campo: keyof EnfermagemForm): boolean | undefined =>
    valor[campo] as boolean | undefined

  return (
    <>
      {/* 1. Dados da avaliação */}
      <div className="proc-card proc-full">
        <div className="proc-card-header">
          <ClipboardCheck size={16} />
          1. Dados da avaliação
          {mostrarBloqueio && <BloqueadoTag />}
        </div>
        <div className="proc-form">
          <div className="proc-form-grid">
            <CampoTexto
              label="Data"
              tipo="date"
              valor={t('dataAvaliacao')}
              onChange={texto('dataAvaliacao')}
              disabled={desabilitado}
            />
            <CampoTexto
              label="Hora"
              tipo="time"
              valor={t('horaAvaliacao')}
              onChange={texto('horaAvaliacao')}
              disabled={desabilitado}
            />
          </div>
          <CampoTexto
            label="Diagnóstico médico (se houver)"
            valor={t('diagnosticoMedico')}
            onChange={texto('diagnosticoMedico')}
            disabled={desabilitado}
          />

          <SubSecao titulo="Comorbidades" />
          <div className="proc-check-grid">
            <CheckItem
              label="Diabetes mellitus"
              checked={b('comorbDiabetes') ?? false}
              onChange={bool('comorbDiabetes')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Hipertensão arterial sistêmica"
              checked={b('comorbHipertensao') ?? false}
              onChange={bool('comorbHipertensao')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Doença vascular"
              checked={b('comorbDoencaVascular') ?? false}
              onChange={bool('comorbDoencaVascular')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Neuropatia"
              checked={b('comorbNeuropatia') ?? false}
              onChange={bool('comorbNeuropatia')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Outras"
              checked={b('comorbOutras') ?? false}
              onChange={bool('comorbOutras')}
              disabled={desabilitado}
            />
          </div>
          <CampoTexto
            label="Outras comorbidades — quais?"
            valor={t('comorbOutrasDetalhe')}
            onChange={texto('comorbOutrasDetalhe')}
            disabled={desabilitado}
          />
          <CampoTextarea
            label="Uso de medicamentos relevantes"
            valor={t('medicamentosRelevantes')}
            onChange={texto('medicamentosRelevantes')}
            disabled={desabilitado}
          />
        </div>
      </div>

      {/* 2. Avaliação da ferida (TIME) */}
      <div className="proc-card proc-full">
        <div className="proc-card-header">
          <Eye size={16} />
          2. Avaliação da ferida
          {mostrarBloqueio && <BloqueadoTag />}
        </div>
        <div className="proc-form">
          <p className="proc-legenda">
            <strong>Modelo TIME</strong> — European Wound Management
            Association: <strong>T</strong> (Tissue) tecido no leito ·{' '}
            <strong>I</strong> infecção/inflamação · <strong>M</strong>{' '}
            (Moisture) exsudato · <strong>E</strong> (Edge) bordas.
          </p>

          <CampoTexto
            label="Localização anatômica"
            valor={t('localizacaoAnatomica')}
            onChange={texto('localizacaoAnatomica')}
            disabled={desabilitado}
            placeholder="Ex: maléolo lateral direito"
          />
          <div className="proc-form-grid">
            <CampoSelect<TipoFerida>
              label="Tipo de ferida"
              opcoes={OPCOES_TIPO_FERIDA}
              valor={t('tipoFerida')}
              onChange={texto('tipoFerida')}
              disabled={desabilitado}
            />
            <CampoTexto
              label="Outro tipo — qual?"
              valor={t('tipoFeridaOutra')}
              onChange={texto('tipoFeridaOutra')}
              disabled={desabilitado}
            />
          </div>

          <CampoTexto
            label="Dimensões"
            valor={t('dimensoes')}
            onChange={texto('dimensoes')}
            disabled={desabilitado}
            placeholder="Ex: 4 x 3 x 0,5 cm"
          />
          <div className="proc-form-grid-3">
            <CampoNumero
              label="Comprimento (cm)"
              valor={t('comprimento')}
              onChange={texto('comprimento')}
              disabled={desabilitado}
              placeholder="0.0"
            />
            <CampoNumero
              label="Largura (cm)"
              valor={t('largura')}
              onChange={texto('largura')}
              disabled={desabilitado}
              placeholder="0.0"
            />
            <CampoNumero
              label="Profundidade (cm)"
              valor={t('profundidade')}
              onChange={texto('profundidade')}
              disabled={desabilitado}
              placeholder="0.0"
            />
          </div>

          <div className="proc-form-grid">
            <CampoSimNao
              label="Tunelização"
              valor={b('tunelizacao')}
              onChange={bool('tunelizacao')}
              disabled={desabilitado}
            />
            <CampoSimNao
              label="Descolamento"
              valor={b('descolamento')}
              onChange={bool('descolamento')}
              disabled={desabilitado}
            />
          </div>

          <div className="proc-form-grid">
            <CampoSelect<TecidoLeito>
              label="T — Tecido no leito"
              opcoes={OPCOES_TECIDO}
              valor={t('tecidoLeito')}
              onChange={texto('tecidoLeito')}
              disabled={desabilitado}
            />
            <CampoSelect<InfeccaoInflamacao>
              label="I — Infecção/inflamação"
              opcoes={OPCOES_INFECCAO}
              valor={t('infeccaoInflamacao')}
              onChange={texto('infeccaoInflamacao')}
              disabled={desabilitado}
            />
            <CampoSelect<ExsudatoTime>
              label="M — Exsudato"
              opcoes={OPCOES_EXSUDATO}
              valor={t('exsudato')}
              onChange={texto('exsudato')}
              disabled={desabilitado}
            />
            <CampoTexto
              label="Tipo do exsudato"
              valor={t('exsudatoTipo')}
              onChange={texto('exsudatoTipo')}
              disabled={desabilitado}
              placeholder="Ex: seroso, sanguinolento"
            />
            <CampoSelect<BordasFerida>
              label="E — Bordas"
              opcoes={OPCOES_BORDAS}
              valor={t('bordas')}
              onChange={texto('bordas')}
              disabled={desabilitado}
            />
            <CampoNumero
              label="Dor (escala 0–10)"
              valor={t('dorEscala')}
              onChange={texto('dorEscala')}
              disabled={desabilitado}
              max="10"
              step="1"
            />
          </div>

          <CampoTexto
            label="Pele perilesional"
            valor={t('pelePerilesional')}
            onChange={texto('pelePerilesional')}
            disabled={desabilitado}
            placeholder="Ex: íntegra, macerada, hiperemiada…"
          />

          <SubSecao titulo="Sinais vitais" />
          <CampoTexto
            label="Sinais vitais"
            valor={t('sinaisVitais')}
            onChange={texto('sinaisVitais')}
            disabled={desabilitado}
            placeholder="Observações gerais"
          />
          <div className="proc-form-grid">
            <CampoTexto
              label="PA"
              valor={t('pa')}
              onChange={texto('pa')}
              disabled={desabilitado}
              placeholder="120x80 mmHg"
            />
            <CampoTexto
              label="FC"
              valor={t('fc')}
              onChange={texto('fc')}
              disabled={desabilitado}
              placeholder="78 bpm"
            />
            <CampoTexto
              label="FR"
              valor={t('fr')}
              onChange={texto('fr')}
              disabled={desabilitado}
              placeholder="16 irpm"
            />
            <CampoTexto
              label="Temp"
              valor={t('temp')}
              onChange={texto('temp')}
              disabled={desabilitado}
              placeholder="36,5 °C"
            />
          </div>
        </div>
      </div>

      {/* 3. Diagnósticos de enfermagem */}
      <div className="proc-card proc-full">
        <div className="proc-card-header">
          <Activity size={16} />
          3. Diagnósticos de enfermagem
          {mostrarBloqueio && <BloqueadoTag />}
        </div>
        <div className="proc-form">
          <div className="proc-check-grid">
            <CheckItem
              label="Integridade da pele prejudicada"
              checked={b('diagIntegridadePele') ?? false}
              onChange={bool('diagIntegridadePele')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Integridade tissular prejudicada"
              checked={b('diagIntegridadeTissular') ?? false}
              onChange={bool('diagIntegridadeTissular')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Risco de infecção"
              checked={b('diagRiscoInfeccao') ?? false}
              onChange={bool('diagRiscoInfeccao')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Perfusão periférica ineficaz"
              checked={b('diagPerfusaoIneficaz') ?? false}
              onChange={bool('diagPerfusaoIneficaz')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Dor aguda"
              checked={b('diagDorAguda') ?? false}
              onChange={bool('diagDorAguda')}
              disabled={desabilitado}
            />
          </div>
          <CampoTextarea
            label="Outros diagnósticos"
            valor={t('diagOutros')}
            onChange={texto('diagOutros')}
            disabled={desabilitado}
          />
        </div>
      </div>

      {/* 4. Conduta realizada */}
      <div className="proc-card proc-full">
        <div className="proc-card-header">
          <Scissors size={16} />
          4. Conduta realizada
          {mostrarBloqueio && <BloqueadoTag />}
        </div>
        <div className="proc-form">
          <div className="proc-form-grid">
            <CampoSelect<TipoLimpeza>
              label="Limpeza"
              opcoes={OPCOES_LIMPEZA}
              valor={t('limpeza')}
              onChange={texto('limpeza')}
              disabled={desabilitado}
            />
            <CampoTexto
              label="Outra solução de limpeza — qual?"
              valor={t('limpezaOutro')}
              onChange={texto('limpezaOutro')}
              disabled={desabilitado}
            />
          </div>
          <CampoSelect<TipoDesbridamento>
            label="Desbridamento"
            opcoes={OPCOES_DESBRIDAMENTO}
            valor={t('desbridamento')}
            onChange={texto('desbridamento')}
            disabled={desabilitado}
          />
          <div className="proc-form-grid">
            <CampoTexto
              label="Cobertura primária"
              valor={t('coberturaPrimaria')}
              onChange={texto('coberturaPrimaria')}
              disabled={desabilitado}
            />
            <CampoTexto
              label="Cobertura secundária"
              valor={t('coberturaSecundaria')}
              onChange={texto('coberturaSecundaria')}
              disabled={desabilitado}
            />
          </div>
          <CampoTexto
            label="Fixação"
            valor={t('fixacao')}
            onChange={texto('fixacao')}
            disabled={desabilitado}
          />
          <CampoTextarea
            label="Orientações ao paciente"
            valor={t('orientacoesPaciente')}
            onChange={texto('orientacoesPaciente')}
            disabled={desabilitado}
          />
        </div>
      </div>

      {/* 5. Avaliação da evolução */}
      <div className="proc-card proc-full">
        <div className="proc-card-header">
          <BookOpen size={16} />
          5. Avaliação da evolução
          {mostrarBloqueio && <BloqueadoTag />}
        </div>
        <div className="proc-form">
          <div className="proc-form-grid">
            <CampoSelect<AvaliacaoEvolucao>
              label="Evolução"
              opcoes={OPCOES_EVOLUCAO}
              valor={t('avaliacaoEvolucao')}
              onChange={texto('avaliacaoEvolucao')}
              disabled={desabilitado}
            />
            <CampoSimNao
              label="Redução da área"
              valor={b('reducaoArea')}
              onChange={bool('reducaoArea')}
              disabled={desabilitado}
            />
          </div>
          <CampoTextarea
            label="Observações"
            valor={t('observacoes')}
            onChange={texto('observacoes')}
            disabled={desabilitado}
          />
        </div>
      </div>

      {/* 6. Plano */}
      <div className="proc-card proc-full">
        <div className="proc-card-header">
          <ClipboardCheck size={16} />
          6. Plano
          {mostrarBloqueio && <BloqueadoTag />}
        </div>
        <div className="proc-form">
          <div className="proc-check-grid">
            <CheckItem
              label="Manter conduta"
              checked={b('planoManterConduta') ?? false}
              onChange={bool('planoManterConduta')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Ajustar cobertura"
              checked={b('planoAjustarCobertura') ?? false}
              onChange={bool('planoAjustarCobertura')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Solicitar avaliação médica"
              checked={b('planoAvaliacaoMedica') ?? false}
              onChange={bool('planoAvaliacaoMedica')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Solicitar exames"
              checked={b('planoSolicitarExames') ?? false}
              onChange={bool('planoSolicitarExames')}
              disabled={desabilitado}
            />
            <CheckItem
              label="Encaminhamento"
              checked={b('planoEncaminhamento') ?? false}
              onChange={bool('planoEncaminhamento')}
              disabled={desabilitado}
            />
          </div>
          <CampoNumero
            label="Retorno em (dias)"
            valor={t('retornoDias')}
            onChange={texto('retornoDias')}
            disabled={desabilitado}
            step="1"
          />
        </div>
      </div>
    </>
  )
}
