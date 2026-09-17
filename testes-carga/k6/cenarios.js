// =============================================================
// Teste de carga do ProntuDigital — RNF05 e RNF06.
//
// Um cenario por execucao, escolhido em -e CENARIO=:
//
//   smoke     1 VU, 1 min          valida o script, nao a capacidade
//   nominal   100 VUs, 13 min      o alvo declarado do RNF06
//   stress    rampa ate quebrar    acha o joelho da curva = a folga
//   soak      40 VUs, 2h           vazamento de memoria e de conexao
//   publico   so as rotas anonimas do agendamento
//
// Exemplo:
//   k6 run -e CENARIO=nominal -e BASE=https://homolog.exemplo.com.br cenarios.js
//
// NUNCA rode o gerador na mesma VPS que hospeda a stack: ele disputaria
// CPU com o que esta sendo medido e o numero sairia errado para baixo.
// =============================================================

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend } from 'k6/metrics';
import {
  BASE, QTD_PROFISSIONAIS, USUARIOS_ALVO, LIMITES,
  autenticar, cabecalhos, dataISO, dataHoraISO, sorteia,
} from './config.js';

const CENARIO = __ENV.CENARIO || 'smoke';

// -------------------------------------------------------------
// 409 e resposta legitima aqui: significa que a regra de conflito de
// horario funcionou. Sem esta linha ela contaria como falha em
// http_req_failed e derrubaria o limite de 1% por um acerto do sistema.
// 503 NAO entra: com NOTIFICACOES_HABILITADAS=false ele aparece na
// recuperacao de senha, que este teste nao exercita — se surgir, e
// problema de verdade.
// -------------------------------------------------------------
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 409));

const tempoAgenda = new Trend('tela_agenda_ms', true);

// -------------------------------------------------------------
// Cenarios
// -------------------------------------------------------------
const DEFINICOES = {
  // Prova que o script fala com a API. Nao mede capacidade.
  smoke: {
    executor: 'constant-vus',
    vus: 1,
    duration: '1m',
    exec: 'jornadaClinica',
  },

  // RNF06. Rampa de 2 min para o pool do Hikari e o JIT aquecerem —
  // medir durante a rampa mistura latencia de warm-up com latencia de
  // regime, e o p95 sai pior do que a realidade.
  nominal: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '2m', target: USUARIOS_ALVO },
      { duration: '10m', target: USUARIOS_ALVO },
      { duration: '1m', target: 0 },
    ],
    gracefulRampDown: '30s',
    exec: 'jornadaClinica',
  },

  // Arrival-rate e nao ramping-vus de proposito: com VUs, quando o
  // sistema desacelera o proprio teste desacelera junto e o ponto de
  // quebra nunca aparece. Aqui a taxa e imposta de fora; a fila cresce
  // e o joelho fica visivel.
  stress: {
    executor: 'ramping-arrival-rate',
    startRate: 5,
    timeUnit: '1s',
    preAllocatedVUs: 50,
    maxVUs: 600,
    stages: [
      { duration: '2m', target: 10 },
      { duration: '2m', target: 25 },
      { duration: '2m', target: 50 },
      { duration: '2m', target: 100 },
      { duration: '2m', target: 200 },
      { duration: '1m', target: 0 },
    ],
    exec: 'jornadaClinica',
  },

  // O unico cenario que pega vazamento de heap, conexao que nao volta
  // ao pool e OOMKilled (exit 137). Nenhum deles aparece em 10 minutos.
  soak: {
    executor: 'constant-vus',
    vus: Number(__ENV.VUS_SOAK || 40),
    duration: __ENV.DURACAO_SOAK || '2h',
    exec: 'jornadaClinica',
  },

  // As rotas anonimas do agendamento publico: e a unica superficie
  // exposta na internet sem autenticacao, entao e tambem a que um abuso
  // encontraria primeiro. Testada isolada para o numero nao se misturar
  // com o da area logada.
  publico: {
    executor: 'constant-arrival-rate',
    rate: Number(__ENV.RPS_PUBLICO || 30),
    timeUnit: '1s',
    duration: __ENV.DURACAO_PUBLICO || '5m',
    preAllocatedVUs: 20,
    maxVUs: 100,
    exec: 'agendamentoPublico',
  },
};

if (!DEFINICOES[CENARIO]) {
  throw new Error(
    'CENARIO desconhecido: "' + CENARIO + '". Use um de: '
    + Object.keys(DEFINICOES).join(', '));
}

// O cenario publico nao produz amostra nenhuma nas tags da area logada;
// declarar limites para elas ali daria "passou" sobre zero dado.
const LIMITES_PUBLICO = {
  'http_req_failed': ['rate<0.01'],
  'http_req_duration{tipo:publico}': LIMITES['http_req_duration{tipo:publico}'],
};

export const options = {
  scenarios: { [CENARIO]: DEFINICOES[CENARIO] },
  thresholds: CENARIO === 'publico' ? LIMITES_PUBLICO : LIMITES,
  // Deixa o resumo com os percentis que interessam ao RNF, nao so a media.
  summaryTrendStats: ['avg', 'min', 'med', 'p(95)', 'p(99)', 'max'],
  // Certificado invalido tem que falhar o teste, nao ser tolerado: e
  // exatamente o que o §7.4 do DEPLOY.md manda verificar.
  insecureSkipTLSVerify: false,
};

// -------------------------------------------------------------
// setup() — roda UMA vez, antes dos VUs.
//
// Busca aqui o que o teste precisa conhecer (profissionais, pacientes,
// procedimentos) em vez de gerar dentro do loop: descoberta repetida
// vira carga artificial que nao existe no uso real.
// -------------------------------------------------------------
export function setup() {
  // TODAS as chamadas daqui levam a tag `setup`, que NAO tem limite
  // declarado. Motivo, visto na primeira execucao real (17/09/2026):
  // setup() roda uma vez so, com a JVM fria e sem JIT, e produz 2 ou 3
  // amostras. Marcadas como `publico`, essas duas amostras viravam o
  // p95 inteiro da tag — 679ms contra um limite de 500ms, reprovando o
  // teste por causa do aquecimento e nao do sistema.
  const tokenAdmin = autenticar(http, check, 'carga_admin', undefined, 'setup');

  const listaProfissionais = http
    .get(BASE + '/api/public/profissionais', { tags: { tipo: 'setup' } })
    .json();
  const profissionais = listaProfissionais.map(function (p) { return p.uuid; });

  const procedimentos = http
    .get(BASE + '/api/procedimentos', { tags: { tipo: 'setup' } })
    .json()
    .map(function (p) { return p.id; });

  const pacientes = http
    .get(BASE + '/api/usuarios?page=0&size=200', cabecalhos(tokenAdmin, 'setup'))
    .json('content')
    .filter(function (u) { return u.username && u.username.indexOf('carga_pac_') === 0; })
    .map(function (u) { return u.uuid; });

  // Mapa nome -> uuid dos profissionais de carga. Necessario porque
  // /api/public/profissionais nao garante ordem, e o VU precisa agendar
  // para o MESMO profissional com que se autenticou: para um perfil
  // PROFISSIONAL, GET /agendamentos/agenda ignora o parametro e devolve
  // sempre a agenda propria — com o uuid trocado, o cenario de escrita
  // gravaria numa agenda e o de leitura olharia outra.
  const porNome = {};
  listaProfissionais.forEach(function (p) { porNome[p.nomeCompleto] = p.uuid; });

  if (!profissionais.length || !pacientes.length || !procedimentos.length) {
    throw new Error(
      'Base sem massa de teste. Rode testes-carga/seed.sql antes — um teste '
      + 'contra tabela vazia aprova qualquer indice e nao mede nada.');
  }

  console.log('massa: ' + profissionais.length + ' profissionais, '
    + pacientes.length + ' pacientes, ' + procedimentos.length + ' procedimentos');

  return {
    profissionais: profissionais,
    porNome: porNome,
    pacientes: pacientes,
    procedimentos: procedimentos,
  };
}

// Cada VU loga uma vez e guarda o token pelo resto da execucao.
// Ver o comentario de autenticar() em config.js: relogar a cada iteracao
// transforma o teste num benchmark de BCrypt.
let token;
let meuProfissional;

function garantirSessao(dados) {
  if (token) return;

  // O MESMO indice decide o login e o uuid — ver o comentario de porNome
  // em setup(). Os 100 VUs se distribuem entre os 4 profissionais da
  // massa, que e quantos uma clinica desse porte tem; o que escala e o
  // numero de sessoes simultaneas, nao o de funcionarios.
  const indice = ((__VU - 1) % QTD_PROFISSIONAIS) + 1;
  token = autenticar(http, check, 'carga_prof_' + indice);

  meuProfissional = dados.porNome['Profissional de Carga ' + indice];
  if (!meuProfissional) {
    throw new Error('Profissional de Carga ' + indice + ' nao esta na lista '
      + 'publica. A massa do seed.sql confere com QTD_PROFISSIONAIS?');
  }
}

// -------------------------------------------------------------
// Jornada da area logada.
//
// A proporcao espelha o uso de uma clinica, nao uma distribuicao
// uniforme: recepcao e agenda o dia inteiro, relatorio uma vez por
// semana. Testar tudo em partes iguais superestima o custo dos
// relatorios e subestima o da agenda.
//
//   60% leitura (agenda, lista de pacientes)
//   20% escrita (agendar, confirmar, cancelar)
//   10% prontuario (detalhe do atendimento)
//    5% relatorio
//    5% documento (PDF/XLSX)
// -------------------------------------------------------------
// Fatia de relatorio + exportacao. O padrao 0.10 (5% + 5%) e o que foi usado
// nas medidas de 17/09/2026 do §4.1 e §4.2 do README — e e PESADO de proposito:
// a 12 req/s, 10% dao ~900 relatorios em 13 minutos, o que nenhuma clinica de
// quatro profissionais gera. Serve para estressar o caminho mais caro.
//
// Para dimensionar hardware, use um valor realista:
//   -e PESO_RELATORIO=0.005   ~ algumas dezenas de relatorios por dia
//
// A fatia que sai daqui volta para leitura, que e o uso de fato dominante.
const PESO_RELATORIO = Number(__ENV.PESO_RELATORIO || 0.10);

export function jornadaClinica(dados) {
  garantirSessao(dados);

  const escritaAte    = 0.20;
  const prontuarioAte = escritaAte + 0.10;
  const relatorioAte  = prontuarioAte + PESO_RELATORIO / 2;
  const documentoAte  = prontuarioAte + PESO_RELATORIO;

  const sorte = Math.random();
  if (sorte < escritaAte) escrever(dados);
  else if (sorte < prontuarioAte) prontuario();
  else if (sorte < relatorioAte) relatorio();
  else if (sorte < documentoAte) documento();
  else lerAgenda();

  // Think time. Sem ele, 100 VUs viram ~100 req/s — dez vezes o que 100
  // pessoas de verdade geram — e o teste reprova uma configuracao que
  // atenderia. Ver o README sobre a traducao do RNF06.
  sleep(5 + Math.random() * 10);
}

function lerAgenda() {
  group('agenda', function () {
    const tipo = sorteia(['DIA', 'DIA', 'DIA', 'SEMANA', 'MES']);
    const r = http.get(
      BASE + '/api/agendamentos/agenda?data=' + dataISO(0) + '&tipo=' + tipo,
      cabecalhos(token, 'leitura'));
    check(r, { 'agenda 200': function (x) { return x.status === 200; } });
    tempoAgenda.add(r.timings.duration, { visualizacao: tipo });

    // A recepcao quase sempre abre a lista de pacientes junto.
    if (Math.random() < 0.5) {
      const p = http.get(
        BASE + '/api/agendamentos/pacientes?page=0&size=10',
        cabecalhos(token, 'leitura'));
      check(p, { 'pacientes 200': function (x) { return x.status === 200; } });
    }
  });
}

function escrever(dados) {
  group('escrita', function () {
    const vaga = proximaVaga();
    const corpo = JSON.stringify({
      pacienteUuid: sorteia(dados.pacientes),
      profissionalUuid: meuProfissional,
      inicioEm: dataHoraISO(vaga.dia, vaga.hora, 0),
      fimEm: dataHoraISO(vaga.dia, vaga.hora, 30),
      tipo: 'AVALIACAO',
      procedimentoId: sorteia(dados.procedimentos),
      localAtendimento: 'CLINICA',
      pacienteAcamado: false,
    });

    const r = http.post(BASE + '/api/agendamentos', corpo,
      cabecalhos(token, 'escrita'));
    // 409 = conflito de horario, resposta correta do sistema (ver o
    // setResponseCallback no topo). Qualquer outra coisa e falha.
    check(r, {
      'agendou (201) ou conflitou (409)': function (x) {
        return x.status === 201 || x.status === 409;
      },
    });

    if (r.status === 201) {
      const id = r.json('id');
      const acao = Math.random() < 0.7 ? 'confirmar' : 'cancelar';
      const a = http.patch(BASE + '/api/agendamentos/' + id + '/' + acao, null,
        cabecalhos(token, 'escrita'));
      check(a, { 'transicao 204': function (x) { return x.status === 204; } });
    }
  });
}

function prontuario() {
  group('prontuario', function () {
    const atras = sorteia([1, 7, 30, 90]);
    const agenda = http.get(
      BASE + '/api/agendamentos/agenda?data=' + dataISO(-atras) + '&tipo=SEMANA',
      cabecalhos(token, 'leitura'));
    if (agenda.status !== 200) return;

    const lista = agenda.json();
    if (!lista.length) return;

    const alvo = sorteia(lista);
    const d = http.get(BASE + '/api/agendamentos/' + alvo.id,
      cabecalhos(token, 'leitura'));
    check(d, { 'detalhe 200': function (x) { return x.status === 200; } });
  });
}

function relatorio() {
  group('relatorio', function () {
    const r = http.get(
      BASE + '/api/relatorios/atendimentos?inicio=' + dataISO(-90) + '&fim=' + dataISO(0),
      cabecalhos(token, 'relatorio'));
    check(r, { 'relatorio 200': function (x) { return x.status === 200; } });

    if (Math.random() < 0.5) {
      const o = http.get(
        BASE + '/api/relatorios/ocupacao?inicio=' + dataISO(-30) + '&fim=' + dataISO(0),
        cabecalhos(token, 'relatorio'));
      check(o, { 'ocupacao 200': function (x) { return x.status === 200; } });
    }
  });
}

function documento() {
  group('documento', function () {
    const formato = Math.random() < 0.7 ? 'PDF' : 'XLSX';
    const r = http.get(
      BASE + '/api/relatorios/atendimentos/exportar'
      + '?inicio=' + dataISO(-30) + '&fim=' + dataISO(0) + '&formato=' + formato,
      cabecalhos(token, 'documento'));
    check(r, {
      'exportacao 200': function (x) { return x.status === 200; },
      'exportacao nao veio vazia': function (x) { return x.body && x.body.length > 500; },
    });
  });
}

// -------------------------------------------------------------
// Jornada anonima: o que um visitante faz antes de agendar.
// -------------------------------------------------------------
export function agendamentoPublico() {
  group('publico', function () {
    const cfg = http.get(BASE + '/api/configuracao', { tags: { tipo: 'publico' } });
    check(cfg, { 'configuracao 200': function (x) { return x.status === 200; } });

    const profs = http.get(BASE + '/api/public/profissionais', { tags: { tipo: 'publico' } });
    check(profs, { 'profissionais 200': function (x) { return x.status === 200; } });

    http.get(BASE + '/api/procedimentos', { tags: { tipo: 'publico' } });

    const lista = profs.status === 200 ? profs.json() : [];
    if (lista.length) {
      const uuid = sorteia(lista).uuid;
      http.get(BASE + '/api/horarios-trabalho/public?profissionalUuid=' + uuid,
        { tags: { tipo: 'publico' } });
      http.get(
        BASE + '/api/bloqueios-horario/public?profissionalUuid=' + uuid
        + '&inicio=' + dataISO(0) + '&fim=' + dataISO(30),
        { tags: { tipo: 'publico' } });
    }
  });
}

// -------------------------------------------------------------
// Vaga futura, unica por VU.
//
// Sem isto, 100 VUs disputam os mesmos horarios, o backend responde 409
// a quase tudo e o cenario de escrita passa a medir a validacao de
// conflito em vez do caminho de gravacao. Cada VU recebe uma faixa
// propria de dias; o fim de semana e pulado porque horarios_trabalho
// (V22) so tem janela de segunda a sexta e o agendamento seria recusado
// por regra, nao por carga.
// -------------------------------------------------------------
function proximaVaga() {
  let dia = 30 + (__VU * 4) + (__ITER % 4);
  const d = new Date();
  d.setDate(d.getDate() + dia);
  const semana = d.getDay();              // 0 = domingo, 6 = sabado
  if (semana === 0) dia += 1;
  if (semana === 6) dia += 2;
  return { dia: dia, hora: 8 + (__ITER % 9) };  // 08h as 16h
}
