import { AnamneseForm } from './FichaAnamnese'
import { EnfermagemForm } from './FichaEnfermagem'

// ── Obrigatoriedade para finalizar a consulta ────────────────────
//
// Regra acordada:
//   • Obrigatórios ...... textos, seleções, datas/horas, escalas e as
//                         perguntas Sim/Não (radio), que têm estado
//                         "não respondido" de verdade.
//   • Isentos ........... checkboxes (desmarcado já é a resposta "não tem"),
//                         as observações/detalhes atrelados a outro campo,
//                         e a seção "Plano / ações futuras" da ficha de
//                         curativos — lá a observação É a ação, e exigir
//                         todas pediria condutas contraditórias.
//
// Campos rotulados "(se houver)" também ficam de fora: exigi-los
// contradiria o próprio rótulo da ficha.
//
// As mensagens usam o mesmo texto do rótulo exibido na tela, para o
// profissional achar o campo sem tradução mental.

/** Texto, select, data, hora ou escala preenchidos. */
const preenchido = (valor?: string | null): boolean =>
  (valor ?? '').trim() !== ''

/** Pergunta Sim/Não respondida — `undefined` é "não respondida". */
const respondido = (valor?: boolean | null): boolean =>
  valor === true || valor === false

/**
 * `campo` é a chave do formulário: liga a regra ao campo na tela, que
 * mostra o asterisco de obrigatório e, depois de uma tentativa de
 * finalizar, o erro de pendência.
 */
export type Regra = {
  campo: string
  ok: boolean
  rotulo: string
  /** `false` quando a obrigatoriedade depende de outra resposta ainda não dada. */
  exigido?: boolean
}

const faltantes = (regras: Regra[]): string[] =>
  regras.filter(r => !r.ok).map(r => r.rotulo)

/** Estado que cada ficha entrega aos seus campos (ver ValidacaoFicha). */
export type EstadoValidacao = {
  obrigatorios: ReadonlySet<string>
  pendentes: ReadonlySet<string>
}

/**
 * Obrigatórios saem de todas as regras. Pendentes só aparecem depois da
 * primeira tentativa de finalizar, para não pintar de vermelho uma ficha
 * que o profissional ainda nem começou a preencher.
 */
export function estadoValidacao(
  regras: Regra[],
  mostrarPendentes: boolean,
): EstadoValidacao {
  return {
    obrigatorios: new Set(
      regras.filter(r => r.exigido !== false).map(r => r.campo),
    ),
    pendentes: new Set(
      mostrarPendentes ? regras.filter(r => !r.ok).map(r => r.campo) : [],
    ),
  }
}

// ── Anamnese (agendamentos do tipo AVALIACAO) ────────────────────

export function regrasAnamnese(f: AnamneseForm): Regra[] {
  return [
    // Identificação complementar. `responsavelCuidador` está fora: o rótulo
    // na ficha é "Responsável / cuidador (se houver)".
    { campo: 'profissao', ok: preenchido(f.profissao), rotulo: 'Profissão' },

    // História da ferida
    {
      campo: 'motivoConsulta',
      ok: preenchido(f.motivoConsulta),
      rotulo: 'Motivo da consulta',
    },
    {
      campo: 'tempoExistenciaFerida',
      ok: preenchido(f.tempoExistenciaFerida),
      rotulo: 'Tempo de existência da ferida',
    },
    {
      campo: 'dataInicioAproximada',
      ok: preenchido(f.dataInicioAproximada),
      rotulo: 'Data aproximada de início',
    },
    {
      campo: 'comoFeridaSurgiu',
      ok: preenchido(f.comoFeridaSurgiu),
      rotulo: 'Como a ferida surgiu',
    },
    {
      campo: 'tratamentosAnteriores',
      ok: preenchido(f.tratamentosAnteriores),
      rotulo: 'Tratamentos anteriores realizados',
    },
    {
      campo: 'curativosPrevios',
      ok: preenchido(f.curativosPrevios),
      rotulo: 'Uso de curativos prévios',
    },

    // Histórico de saúde (Sim/Não)
    {
      campo: 'diabetesMellitus',
      ok: respondido(f.diabetesMellitus),
      rotulo: 'Diabetes mellitus',
    },
    {
      campo: 'hipertensaoArterial',
      ok: respondido(f.hipertensaoArterial),
      rotulo: 'Hipertensão arterial',
    },
    {
      campo: 'doencaVenosaCronica',
      ok: respondido(f.doencaVenosaCronica),
      rotulo: 'Doença venosa crônica',
    },
    {
      campo: 'doencaArterialPeriferica',
      ok: respondido(f.doencaArterialPeriferica),
      rotulo: 'Doença arterial periférica',
    },
    {
      campo: 'insuficienciaRenal',
      ok: respondido(f.insuficienciaRenal),
      rotulo: 'Insuficiência renal',
    },
    { campo: 'cancer', ok: respondido(f.cancer), rotulo: 'Câncer' },
    {
      campo: 'problemasNeurologicos',
      ok: respondido(f.problemasNeurologicos),
      rotulo: 'Problemas neurológicos',
    },
    {
      campo: 'historicoCirurgias',
      ok: respondido(f.historicoCirurgias),
      rotulo: 'Histórico de cirurgias',
    },

    // Hábitos de vida
    { campo: 'tabagismo', ok: respondido(f.tabagismo), rotulo: 'Tabagismo' },
    {
      campo: 'consumoAlcool',
      ok: respondido(f.consumoAlcool),
      rotulo: 'Consumo de álcool',
    },
    {
      campo: 'alimentacaoEstadoNutricional',
      ok: preenchido(f.alimentacaoEstadoNutricional),
      rotulo: 'Alimentação / estado nutricional',
    },
    {
      campo: 'ingestaoHidrica',
      ok: respondido(f.ingestaoHidrica),
      rotulo: 'Ingestão hídrica',
    },

    // Mobilidade e funcionalidade
    {
      campo: 'deambulaSozinho',
      ok: respondido(f.deambulaSozinho),
      rotulo: 'Deambula sozinho?',
    },
    {
      campo: 'acamadoOuCadeirante',
      ok: respondido(f.acamadoOuCadeirante),
      rotulo: 'Acamado ou cadeirante',
    },
    {
      campo: 'usoDispositivos',
      ok: respondido(f.usoDispositivos),
      rotulo: 'Uso de dispositivos',
    },
    {
      campo: 'mudancaPosicaoLeito',
      ok: respondido(f.mudancaPosicaoLeito),
      rotulo: 'Mudança de posição no leito',
    },

    // Outros
    {
      campo: 'examesRecentes',
      ok: respondido(f.examesRecentes),
      rotulo: 'Exames recentes',
    },
    {
      campo: 'redeApoio',
      ok: preenchido(f.redeApoio),
      rotulo: 'Rede de apoio',
    },
    {
      campo: 'acompanhamentoMedico',
      ok: respondido(f.acompanhamentoMedico),
      rotulo: 'Faz algum acompanhamento médico?',
    },
  ]
}

export function validarAnamnese(f: AnamneseForm): string[] {
  return faltantes(regrasAnamnese(f))
}

// ── Evolução de Enfermagem (AVALIACAO) ───────────────────────────

export function regrasEnfermagem(f: EnfermagemForm): Regra[] {
  return [
    // 1. Dados da avaliação. `diagnosticoMedico` fora: rótulo "(se houver)".
    {
      campo: 'dataAvaliacao',
      ok: preenchido(f.dataAvaliacao),
      rotulo: 'Data da avaliação',
    },
    {
      campo: 'horaAvaliacao',
      ok: preenchido(f.horaAvaliacao),
      rotulo: 'Hora da avaliação',
    },
    {
      campo: 'medicamentosRelevantes',
      ok: preenchido(f.medicamentosRelevantes),
      rotulo: 'Uso de medicamentos relevantes',
    },

    // 2. Avaliação da ferida (TIME)
    {
      campo: 'localizacaoAnatomica',
      ok: preenchido(f.localizacaoAnatomica),
      rotulo: 'Localização anatômica',
    },
    {
      campo: 'tipoFerida',
      ok: preenchido(f.tipoFerida),
      rotulo: 'Tipo de ferida',
    },
    // Só exigido quando o tipo escolhido foi "Outra".
    {
      campo: 'tipoFeridaOutra',
      exigido: f.tipoFerida === 'OUTRA',
      ok: f.tipoFerida !== 'OUTRA' || preenchido(f.tipoFeridaOutra),
      rotulo: 'Outro tipo de ferida — qual?',
    },
    { campo: 'dimensoes', ok: preenchido(f.dimensoes), rotulo: 'Dimensões' },
    {
      campo: 'comprimento',
      ok: preenchido(f.comprimento),
      rotulo: 'Comprimento (cm)',
    },
    { campo: 'largura', ok: preenchido(f.largura), rotulo: 'Largura (cm)' },
    {
      campo: 'profundidade',
      ok: preenchido(f.profundidade),
      rotulo: 'Profundidade (cm)',
    },
    {
      campo: 'tunelizacao',
      ok: respondido(f.tunelizacao),
      rotulo: 'Tunelização',
    },
    {
      campo: 'descolamento',
      ok: respondido(f.descolamento),
      rotulo: 'Descolamento',
    },
    {
      campo: 'tecidoLeito',
      ok: preenchido(f.tecidoLeito),
      rotulo: 'T — Tecido no leito',
    },
    {
      campo: 'infeccaoInflamacao',
      ok: preenchido(f.infeccaoInflamacao),
      rotulo: 'I — Infecção/inflamação',
    },
    { campo: 'exsudato', ok: preenchido(f.exsudato), rotulo: 'M — Exsudato' },
    {
      campo: 'exsudatoTipo',
      ok: preenchido(f.exsudatoTipo),
      rotulo: 'Tipo do exsudato',
    },
    { campo: 'bordas', ok: preenchido(f.bordas), rotulo: 'E — Bordas' },
    {
      campo: 'dorEscala',
      ok: preenchido(f.dorEscala),
      rotulo: 'Dor (escala 0–10)',
    },
    {
      campo: 'pelePerilesional',
      ok: preenchido(f.pelePerilesional),
      rotulo: 'Pele perilesional',
    },
    {
      campo: 'sinaisVitais',
      ok: preenchido(f.sinaisVitais),
      rotulo: 'Sinais vitais',
    },
    { campo: 'pa', ok: preenchido(f.pa), rotulo: 'PA' },
    { campo: 'fc', ok: preenchido(f.fc), rotulo: 'FC' },
    { campo: 'fr', ok: preenchido(f.fr), rotulo: 'FR' },
    { campo: 'temp', ok: preenchido(f.temp), rotulo: 'Temp' },

    // 4. Conduta realizada
    { campo: 'limpeza', ok: preenchido(f.limpeza), rotulo: 'Limpeza' },
    {
      campo: 'limpezaOutro',
      exigido: f.limpeza === 'OUTRO',
      ok: f.limpeza !== 'OUTRO' || preenchido(f.limpezaOutro),
      rotulo: 'Outra solução de limpeza — qual?',
    },
    {
      campo: 'desbridamento',
      ok: preenchido(f.desbridamento),
      rotulo: 'Desbridamento',
    },
    {
      campo: 'coberturaPrimaria',
      ok: preenchido(f.coberturaPrimaria),
      rotulo: 'Cobertura primária',
    },
    {
      campo: 'coberturaSecundaria',
      ok: preenchido(f.coberturaSecundaria),
      rotulo: 'Cobertura secundária',
    },
    { campo: 'fixacao', ok: preenchido(f.fixacao), rotulo: 'Fixação' },
    {
      campo: 'orientacoesPaciente',
      ok: preenchido(f.orientacoesPaciente),
      rotulo: 'Orientações ao paciente',
    },

    // 5. Avaliação da evolução
    {
      campo: 'avaliacaoEvolucao',
      ok: preenchido(f.avaliacaoEvolucao),
      rotulo: 'Evolução',
    },
    {
      campo: 'reducaoArea',
      ok: respondido(f.reducaoArea),
      rotulo: 'Redução da área',
    },

    // 6. Plano — os checkboxes são isentos; o retorno é dado de agenda.
    {
      campo: 'retornoDias',
      ok: preenchido(f.retornoDias),
      rotulo: 'Retorno em (dias)',
    },
  ]
}

export function validarEnfermagem(f: EnfermagemForm): string[] {
  return faltantes(regrasEnfermagem(f))
}

// ── Evolução Diária de Curativos (TRATAMENTO) ────────────────────
//
// O tipo do formulário vive em page.tsx; aqui só o contrato mínimo, para
// não criar dependência circular entre os módulos.
type CurativoCampos = {
  comprimento: string
  largura: string
  profundidade: string
  tecido: string
  infeccaoInflamacao: string
  exsudato: string
  bordas: string
  dorEscala: string
  pelePerilesional: string
  limpezaIrrigacao: string
  desbridamento: string
  coberturaPrimaria: string
  orientacoesPaciente: string
  evolucao: string
}

export function regrasCurativo(f: CurativoCampos): Regra[] {
  return [
    // 1. Avaliação diária. `odorPresente` é checkbox: isento.
    {
      campo: 'comprimento',
      ok: preenchido(f.comprimento),
      rotulo: 'Comprimento (cm)',
    },
    { campo: 'largura', ok: preenchido(f.largura), rotulo: 'Largura (cm)' },
    {
      campo: 'profundidade',
      ok: preenchido(f.profundidade),
      rotulo: 'Profundidade (cm)',
    },
    { campo: 'tecido', ok: preenchido(f.tecido), rotulo: 'Tecido (T — TIME)' },
    {
      campo: 'infeccaoInflamacao',
      ok: preenchido(f.infeccaoInflamacao),
      rotulo: 'Infecção/inflamação (I)',
    },
    { campo: 'exsudato', ok: preenchido(f.exsudato), rotulo: 'Exsudato (M)' },
    { campo: 'bordas', ok: preenchido(f.bordas), rotulo: 'Bordas (E)' },
    { campo: 'dorEscala', ok: preenchido(f.dorEscala), rotulo: 'Dor (0–10)' },
    {
      campo: 'pelePerilesional',
      ok: preenchido(f.pelePerilesional),
      rotulo: 'Pele perilesional',
    },

    // 2. Intervenções. `desbridamentoObs` é observação: isenta.
    {
      campo: 'limpezaIrrigacao',
      ok: preenchido(f.limpezaIrrigacao),
      rotulo: 'Limpeza/Irrigação',
    },
    {
      campo: 'desbridamento',
      ok: preenchido(f.desbridamento),
      rotulo: 'Desbridamento',
    },
    {
      campo: 'coberturaPrimaria',
      ok: preenchido(f.coberturaPrimaria),
      rotulo: 'Cobertura primária',
    },
    {
      campo: 'orientacoesPaciente',
      ok: preenchido(f.orientacoesPaciente),
      rotulo: 'Orientações ao paciente',
    },

    // 3. Avaliação da evolução. `observacoes` é observação: isenta.
    { campo: 'evolucao', ok: preenchido(f.evolucao), rotulo: 'Evolução' },

    // 4. Plano / ações futuras — seção inteira isenta.
  ]
}

export function validarCurativo(f: CurativoCampos): string[] {
  return faltantes(regrasCurativo(f))
}

/** Quantos rótulos entram na mensagem antes de virar "e mais N". */
const LIMITE_NA_MENSAGEM = 6

/**
 * Monta a mensagem da notificação. Com dezenas de campos possíveis, listar
 * todos viraria um parágrafo ilegível — mostra os primeiros e resume o resto.
 */
export function mensagemCamposObrigatorios(pendentes: string[]): string {
  const total = pendentes.length
  const mostrados = pendentes.slice(0, LIMITE_NA_MENSAGEM).join(', ')
  const restantes = total - LIMITE_NA_MENSAGEM

  const sufixo =
    restantes > 0 ? ` e mais ${restantes} campo${restantes > 1 ? 's' : ''}` : ''

  return total === 1
    ? `Preencha o campo obrigatório: ${mostrados}.`
    : `Preencha os ${total} campos obrigatórios: ${mostrados}${sufixo}.`
}
