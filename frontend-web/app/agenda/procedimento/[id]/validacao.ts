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

type Regra = { ok: boolean; rotulo: string }

const faltantes = (regras: Regra[]): string[] =>
  regras.filter(r => !r.ok).map(r => r.rotulo)

// ── Anamnese (agendamentos do tipo AVALIACAO) ────────────────────

export function validarAnamnese(f: AnamneseForm): string[] {
  return faltantes([
    // Identificação complementar. `responsavelCuidador` está fora: o rótulo
    // na ficha é "Responsável / cuidador (se houver)".
    { ok: preenchido(f.profissao), rotulo: 'Profissão' },

    // História da ferida
    { ok: preenchido(f.motivoConsulta), rotulo: 'Motivo da consulta' },
    {
      ok: preenchido(f.tempoExistenciaFerida),
      rotulo: 'Tempo de existência da ferida',
    },
    {
      ok: preenchido(f.dataInicioAproximada),
      rotulo: 'Data aproximada de início',
    },
    { ok: preenchido(f.comoFeridaSurgiu), rotulo: 'Como a ferida surgiu' },
    {
      ok: preenchido(f.tratamentosAnteriores),
      rotulo: 'Tratamentos anteriores realizados',
    },
    { ok: preenchido(f.curativosPrevios), rotulo: 'Uso de curativos prévios' },

    // Histórico de saúde (Sim/Não)
    { ok: respondido(f.diabetesMellitus), rotulo: 'Diabetes mellitus' },
    { ok: respondido(f.hipertensaoArterial), rotulo: 'Hipertensão arterial' },
    { ok: respondido(f.doencaVenosaCronica), rotulo: 'Doença venosa crônica' },
    {
      ok: respondido(f.doencaArterialPeriferica),
      rotulo: 'Doença arterial periférica',
    },
    { ok: respondido(f.insuficienciaRenal), rotulo: 'Insuficiência renal' },
    { ok: respondido(f.cancer), rotulo: 'Câncer' },
    {
      ok: respondido(f.problemasNeurologicos),
      rotulo: 'Problemas neurológicos',
    },
    { ok: respondido(f.historicoCirurgias), rotulo: 'Histórico de cirurgias' },

    // Hábitos de vida
    { ok: respondido(f.tabagismo), rotulo: 'Tabagismo' },
    { ok: respondido(f.consumoAlcool), rotulo: 'Consumo de álcool' },
    {
      ok: preenchido(f.alimentacaoEstadoNutricional),
      rotulo: 'Alimentação / estado nutricional',
    },
    { ok: respondido(f.ingestaoHidrica), rotulo: 'Ingestão hídrica' },

    // Mobilidade e funcionalidade
    { ok: respondido(f.deambulaSozinho), rotulo: 'Deambula sozinho?' },
    { ok: respondido(f.acamadoOuCadeirante), rotulo: 'Acamado ou cadeirante' },
    { ok: respondido(f.usoDispositivos), rotulo: 'Uso de dispositivos' },
    {
      ok: respondido(f.mudancaPosicaoLeito),
      rotulo: 'Mudança de posição no leito',
    },

    // Outros
    { ok: respondido(f.examesRecentes), rotulo: 'Exames recentes' },
    { ok: preenchido(f.redeApoio), rotulo: 'Rede de apoio' },
    {
      ok: respondido(f.acompanhamentoMedico),
      rotulo: 'Faz algum acompanhamento médico?',
    },
  ])
}

// ── Evolução de Enfermagem (AVALIACAO) ───────────────────────────

export function validarEnfermagem(f: EnfermagemForm): string[] {
  return faltantes([
    // 1. Dados da avaliação. `diagnosticoMedico` fora: rótulo "(se houver)".
    { ok: preenchido(f.dataAvaliacao), rotulo: 'Data da avaliação' },
    { ok: preenchido(f.horaAvaliacao), rotulo: 'Hora da avaliação' },
    {
      ok: preenchido(f.medicamentosRelevantes),
      rotulo: 'Uso de medicamentos relevantes',
    },

    // 2. Avaliação da ferida (TIME)
    { ok: preenchido(f.localizacaoAnatomica), rotulo: 'Localização anatômica' },
    { ok: preenchido(f.tipoFerida), rotulo: 'Tipo de ferida' },
    // Só exigido quando o tipo escolhido foi "Outra".
    {
      ok: f.tipoFerida !== 'OUTRA' || preenchido(f.tipoFeridaOutra),
      rotulo: 'Outro tipo de ferida — qual?',
    },
    { ok: preenchido(f.dimensoes), rotulo: 'Dimensões' },
    { ok: preenchido(f.comprimento), rotulo: 'Comprimento (cm)' },
    { ok: preenchido(f.largura), rotulo: 'Largura (cm)' },
    { ok: preenchido(f.profundidade), rotulo: 'Profundidade (cm)' },
    { ok: respondido(f.tunelizacao), rotulo: 'Tunelização' },
    { ok: respondido(f.descolamento), rotulo: 'Descolamento' },
    { ok: preenchido(f.tecidoLeito), rotulo: 'T — Tecido no leito' },
    {
      ok: preenchido(f.infeccaoInflamacao),
      rotulo: 'I — Infecção/inflamação',
    },
    { ok: preenchido(f.exsudato), rotulo: 'M — Exsudato' },
    { ok: preenchido(f.exsudatoTipo), rotulo: 'Tipo do exsudato' },
    { ok: preenchido(f.bordas), rotulo: 'E — Bordas' },
    { ok: preenchido(f.dorEscala), rotulo: 'Dor (escala 0–10)' },
    { ok: preenchido(f.pelePerilesional), rotulo: 'Pele perilesional' },
    { ok: preenchido(f.sinaisVitais), rotulo: 'Sinais vitais' },
    { ok: preenchido(f.pa), rotulo: 'PA' },
    { ok: preenchido(f.fc), rotulo: 'FC' },
    { ok: preenchido(f.fr), rotulo: 'FR' },
    { ok: preenchido(f.temp), rotulo: 'Temp' },

    // 4. Conduta realizada
    { ok: preenchido(f.limpeza), rotulo: 'Limpeza' },
    {
      ok: f.limpeza !== 'OUTRO' || preenchido(f.limpezaOutro),
      rotulo: 'Outra solução de limpeza — qual?',
    },
    { ok: preenchido(f.desbridamento), rotulo: 'Desbridamento' },
    { ok: preenchido(f.coberturaPrimaria), rotulo: 'Cobertura primária' },
    { ok: preenchido(f.coberturaSecundaria), rotulo: 'Cobertura secundária' },
    { ok: preenchido(f.fixacao), rotulo: 'Fixação' },
    {
      ok: preenchido(f.orientacoesPaciente),
      rotulo: 'Orientações ao paciente',
    },

    // 5. Avaliação da evolução
    { ok: preenchido(f.avaliacaoEvolucao), rotulo: 'Evolução' },
    { ok: respondido(f.reducaoArea), rotulo: 'Redução da área' },

    // 6. Plano — os checkboxes são isentos; o retorno é dado de agenda.
    { ok: preenchido(f.retornoDias), rotulo: 'Retorno em (dias)' },
  ])
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

export function validarCurativo(f: CurativoCampos): string[] {
  return faltantes([
    // 1. Avaliação diária. `odorPresente` é checkbox: isento.
    { ok: preenchido(f.comprimento), rotulo: 'Comprimento (cm)' },
    { ok: preenchido(f.largura), rotulo: 'Largura (cm)' },
    { ok: preenchido(f.profundidade), rotulo: 'Profundidade (cm)' },
    { ok: preenchido(f.tecido), rotulo: 'Tecido (T — TIME)' },
    { ok: preenchido(f.infeccaoInflamacao), rotulo: 'Infecção/inflamação (I)' },
    { ok: preenchido(f.exsudato), rotulo: 'Exsudato (M)' },
    { ok: preenchido(f.bordas), rotulo: 'Bordas (E)' },
    { ok: preenchido(f.dorEscala), rotulo: 'Dor (0–10)' },
    { ok: preenchido(f.pelePerilesional), rotulo: 'Pele perilesional' },

    // 2. Intervenções. `desbridamentoObs` é observação: isenta.
    { ok: preenchido(f.limpezaIrrigacao), rotulo: 'Limpeza/Irrigação' },
    { ok: preenchido(f.desbridamento), rotulo: 'Desbridamento' },
    { ok: preenchido(f.coberturaPrimaria), rotulo: 'Cobertura primária' },
    {
      ok: preenchido(f.orientacoesPaciente),
      rotulo: 'Orientações ao paciente',
    },

    // 3. Avaliação da evolução. `observacoes` é observação: isenta.
    { ok: preenchido(f.evolucao), rotulo: 'Evolução' },

    // 4. Plano / ações futuras — seção inteira isenta.
  ])
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
