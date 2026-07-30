'use client'

import { ClipboardList } from 'lucide-react'
import { Anamnese, AnamneseRequisicao, RedeApoio } from '@/tipos/anamnese'
import {
  BloqueadoTag,
  CampoSelect,
  CampoSimNao,
  CampoTexto,
  CampoTextarea,
  CheckItem,
  hojeISO,
  SubSecao,
} from './campos'

// ── Anamnese ─────────────────────────────────────────────────────
// Modelo: ajustar_campos/modelos/Anamnese.pdf
// Preenchida em agendamentos do tipo AVALIACAO. É 1:1 com o paciente:
// um histórico que acompanha a pessoa, não a consulta.
//
// Nome, idade/nascimento, sexo e telefone do modelo vêm do cadastro do
// paciente e por isso não aparecem no formulário.

/** Campos de texto ficam como string vazia; Sim/Não sem resposta ficam `undefined`. */
export type AnamneseForm = AnamneseRequisicao

export const ANAMNESE_INICIAL: AnamneseForm = {}

const OPCOES_REDE_APOIO: { value: RedeApoio; label: string }[] = [
  { value: 'CUIDADOR', label: 'Cuidador' },
  { value: 'FAMILIAR_RESPONSAVEL', label: 'Familiar responsável' },
  {
    value: 'CONDICOES_CURATIVOS_CASA',
    label: 'Condições para realizar curativos em casa',
  },
]

/** Descarta os campos de auditoria que o backend devolve mas não recebe. */
export function anamneseParaFormulario(
  anamnese: Anamnese | null,
): AnamneseForm {
  if (!anamnese) return ANAMNESE_INICIAL
  const {
    uuid,
    pacienteUuid,
    pacienteNome,
    registradoPor,
    criadoEm,
    atualizadoEm,
    ...form
  } = anamnese
  return form
}

/** Remove strings vazias para que o backend receba `null` no lugar de ''. */
export function anamneseParaApi(form: AnamneseForm): AnamneseRequisicao {
  return Object.fromEntries(
    Object.entries(form).filter(([, valor]) => valor !== '' && valor != null),
  ) as AnamneseRequisicao
}

export default function FichaAnamnese({
  valor,
  aoAlterar,
  desabilitado,
  mostrarBloqueio,
}: {
  valor: AnamneseForm
  aoAlterar: (mudanca: Partial<AnamneseForm>) => void
  desabilitado: boolean
  mostrarBloqueio: boolean
}) {
  /** Handler de campo de texto/select por nome do campo. */
  const texto =
    (campo: keyof AnamneseForm) =>
    (e: { target: { value: string } }): void =>
      aoAlterar({ [campo]: e.target.value })

  const bool = (campo: keyof AnamneseForm) => (marcado: boolean) =>
    aoAlterar({ [campo]: marcado })

  const t = (campo: keyof AnamneseForm): string =>
    (valor[campo] as string | undefined) ?? ''

  const b = (campo: keyof AnamneseForm): boolean | undefined =>
    valor[campo] as boolean | undefined

  return (
    <div className="proc-card proc-full">
      <div className="proc-card-header">
        <ClipboardList size={16} />
        Anamnese
        {mostrarBloqueio && <BloqueadoTag />}
      </div>
      <div className="proc-form">
        <p className="proc-legenda">
          Histórico do paciente, preenchido na avaliação e reaproveitado nas
          consultas seguintes. Nome, nascimento, sexo e telefone vêm do cadastro
          do paciente.
        </p>

        <SubSecao titulo="Identificação do paciente" />
        <div className="proc-form-grid">
          <CampoTexto
            label="Profissão"
            valor={t('profissao')}
            onChange={texto('profissao')}
            disabled={desabilitado}
          />
          <CampoTexto
            label="Responsável / cuidador (se houver)"
            valor={t('responsavelCuidador')}
            onChange={texto('responsavelCuidador')}
            disabled={desabilitado}
          />
        </div>

        <SubSecao titulo="Queixa principal e história da ferida" />
        <CampoTextarea
          label="Motivo da consulta"
          valor={t('motivoConsulta')}
          onChange={texto('motivoConsulta')}
          disabled={desabilitado}
          placeholder="Queixa principal do paciente"
        />
        <div className="proc-form-grid">
          <CampoTexto
            label="Tempo de existência da ferida"
            valor={t('tempoExistenciaFerida')}
            onChange={texto('tempoExistenciaFerida')}
            disabled={desabilitado}
            placeholder="Ex: 3 meses"
          />
          <CampoTexto
            label="Data aproximada de início"
            tipo="date"
            valor={t('dataInicioAproximada')}
            onChange={texto('dataInicioAproximada')}
            disabled={desabilitado}
            // A ferida já existe: não pode ter começado no futuro. O
            // AnamneseRequestDTO não valida este campo, então o `max` é a
            // única barreira — e o seletor nativo respeita.
            max={hojeISO()}
          />
        </div>
        <CampoTextarea
          label="Como a ferida surgiu (trauma, cirurgia, espontânea, pressão etc.)"
          valor={t('comoFeridaSurgiu')}
          onChange={texto('comoFeridaSurgiu')}
          disabled={desabilitado}
        />
        <CampoTextarea
          label="Tratamentos anteriores realizados"
          valor={t('tratamentosAnteriores')}
          onChange={texto('tratamentosAnteriores')}
          disabled={desabilitado}
        />
        <CampoTextarea
          label="Uso de curativos prévios (quais?)"
          valor={t('curativosPrevios')}
          onChange={texto('curativosPrevios')}
          disabled={desabilitado}
        />

        <SubSecao titulo="Histórico de saúde" />
        <div className="proc-form-grid">
          <CampoSimNao
            label="Diabetes mellitus"
            valor={b('diabetesMellitus')}
            onChange={bool('diabetesMellitus')}
            disabled={desabilitado}
            detalhe={t('diabetesMellitusDetalhe')}
            onDetalheChange={texto('diabetesMellitusDetalhe')}
          />
          <CampoSimNao
            label="Hipertensão arterial"
            valor={b('hipertensaoArterial')}
            onChange={bool('hipertensaoArterial')}
            disabled={desabilitado}
            detalhe={t('hipertensaoArterialDetalhe')}
            onDetalheChange={texto('hipertensaoArterialDetalhe')}
          />
          <CampoSimNao
            label="Doença venosa crônica"
            valor={b('doencaVenosaCronica')}
            onChange={bool('doencaVenosaCronica')}
            disabled={desabilitado}
            detalhe={t('doencaVenosaCronicaDetalhe')}
            onDetalheChange={texto('doencaVenosaCronicaDetalhe')}
          />
          <CampoSimNao
            label="Doença arterial periférica"
            valor={b('doencaArterialPeriferica')}
            onChange={bool('doencaArterialPeriferica')}
            disabled={desabilitado}
            detalhe={t('doencaArterialPerifericaDetalhe')}
            onDetalheChange={texto('doencaArterialPerifericaDetalhe')}
          />
          <CampoSimNao
            label="Insuficiência renal"
            valor={b('insuficienciaRenal')}
            onChange={bool('insuficienciaRenal')}
            disabled={desabilitado}
            detalhe={t('insuficienciaRenalDetalhe')}
            onDetalheChange={texto('insuficienciaRenalDetalhe')}
          />
          <CampoSimNao
            label="Câncer"
            valor={b('cancer')}
            onChange={bool('cancer')}
            disabled={desabilitado}
            detalhe={t('cancerDetalhe')}
            onDetalheChange={texto('cancerDetalhe')}
          />
          <CampoSimNao
            label="Problemas neurológicos"
            valor={b('problemasNeurologicos')}
            onChange={bool('problemasNeurologicos')}
            disabled={desabilitado}
            detalhe={t('problemasNeurologicosDetalhe')}
            onDetalheChange={texto('problemasNeurologicosDetalhe')}
          />
          <CampoSimNao
            label="Histórico de cirurgias"
            valor={b('historicoCirurgias')}
            onChange={bool('historicoCirurgias')}
            disabled={desabilitado}
            detalhe={t('historicoCirurgiasDetalhe')}
            onDetalheChange={texto('historicoCirurgiasDetalhe')}
          />
        </div>

        <SubSecao titulo="Medicamentos em uso" />
        <div className="proc-check-grid">
          <CheckItem
            label="Antibióticos"
            checked={b('medAntibioticos') ?? false}
            onChange={bool('medAntibioticos')}
            disabled={desabilitado}
          />
          <CheckItem
            label="Anticoagulantes"
            checked={b('medAnticoagulantes') ?? false}
            onChange={bool('medAnticoagulantes')}
            disabled={desabilitado}
          />
          <CheckItem
            label="Corticoides"
            checked={b('medCorticoides') ?? false}
            onChange={bool('medCorticoides')}
            disabled={desabilitado}
          />
          <CheckItem
            label="Insulina / hipoglicemiantes"
            checked={b('medInsulinaHipoglicemiantes') ?? false}
            onChange={bool('medInsulinaHipoglicemiantes')}
            disabled={desabilitado}
          />
          <CheckItem
            label="Outros medicamentos contínuos"
            checked={b('medOutrosContinuos') ?? false}
            onChange={bool('medOutrosContinuos')}
            disabled={desabilitado}
          />
        </div>

        <SubSecao titulo="Alergias" />
        <div className="proc-check-grid">
          <CheckItem
            label="Medicamentos"
            checked={b('alergiaMedicamentos') ?? false}
            onChange={bool('alergiaMedicamentos')}
            disabled={desabilitado}
          />
          <CheckItem
            label="Produtos tópicos"
            checked={b('alergiaProdutosTopicos') ?? false}
            onChange={bool('alergiaProdutosTopicos')}
            disabled={desabilitado}
          />
          <CheckItem
            label="Curativos ou adesivos"
            checked={b('alergiaCurativosAdesivos') ?? false}
            onChange={bool('alergiaCurativosAdesivos')}
            disabled={desabilitado}
          />
        </div>

        <SubSecao titulo="Hábitos de vida" />
        <div className="proc-form-grid">
          <CampoSimNao
            label="Tabagismo"
            valor={b('tabagismo')}
            onChange={bool('tabagismo')}
            disabled={desabilitado}
          />
          <CampoSimNao
            label="Consumo de álcool"
            valor={b('consumoAlcool')}
            onChange={bool('consumoAlcool')}
            disabled={desabilitado}
          />
        </div>
        <CampoTextarea
          label="Alimentação / estado nutricional"
          valor={t('alimentacaoEstadoNutricional')}
          onChange={texto('alimentacaoEstadoNutricional')}
          disabled={desabilitado}
        />
        <CampoSimNao
          label="Ingestão hídrica"
          valor={b('ingestaoHidrica')}
          onChange={bool('ingestaoHidrica')}
          disabled={desabilitado}
          detalhe={t('ingestaoHidricaDetalhe')}
          onDetalheChange={texto('ingestaoHidricaDetalhe')}
          detalhePlaceholder="Ex: cerca de 1,5 L por dia"
        />

        <SubSecao titulo="Mobilidade e funcionalidade" />
        <div className="proc-form-grid">
          <CampoSimNao
            label="Deambula sozinho?"
            valor={b('deambulaSozinho')}
            onChange={bool('deambulaSozinho')}
            disabled={desabilitado}
            detalhe={t('deambulaSozinhoDetalhe')}
            onDetalheChange={texto('deambulaSozinhoDetalhe')}
          />
          <CampoSimNao
            label="Acamado ou cadeirante"
            valor={b('acamadoOuCadeirante')}
            onChange={bool('acamadoOuCadeirante')}
            disabled={desabilitado}
            detalhe={t('acamadoOuCadeiranteDetalhe')}
            onDetalheChange={texto('acamadoOuCadeiranteDetalhe')}
          />
          <CampoSimNao
            label="Uso de dispositivos (bengala, andador, cadeira de rodas)"
            valor={b('usoDispositivos')}
            onChange={bool('usoDispositivos')}
            disabled={desabilitado}
            detalhe={t('usoDispositivosDetalhe')}
            onDetalheChange={texto('usoDispositivosDetalhe')}
            detalhePlaceholder="Quais dispositivos?"
          />
          <CampoSimNao
            label="Mudança de posição no leito"
            valor={b('mudancaPosicaoLeito')}
            onChange={bool('mudancaPosicaoLeito')}
            disabled={desabilitado}
            detalhe={t('mudancaPosicaoLeitoDetalhe')}
            onDetalheChange={texto('mudancaPosicaoLeitoDetalhe')}
          />
        </div>

        <SubSecao titulo="Exames, rede de apoio e acompanhamento" />
        <div className="proc-form-grid">
          <CampoSimNao
            label="Exames recentes"
            valor={b('examesRecentes')}
            onChange={bool('examesRecentes')}
            disabled={desabilitado}
          />
          <CampoSelect<RedeApoio>
            label="Rede de apoio"
            opcoes={OPCOES_REDE_APOIO}
            valor={t('redeApoio')}
            onChange={texto('redeApoio')}
            disabled={desabilitado}
          />
        </div>
        <CampoSimNao
          label="Faz algum acompanhamento médico?"
          valor={b('acompanhamentoMedico')}
          onChange={bool('acompanhamentoMedico')}
          disabled={desabilitado}
        />
        <CampoTextarea
          label="Acompanhamento médico — nome, especialidade e contato"
          valor={t('acompanhamentoMedicoDetalhe')}
          onChange={texto('acompanhamentoMedicoDetalhe')}
          disabled={desabilitado}
          placeholder={'Nome:\nEspecialidade:\nContato:'}
        />
      </div>
    </div>
  )
}
