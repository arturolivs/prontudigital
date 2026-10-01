// =============================================================
// Configuracao compartilhada dos cenarios de carga.
//
// Tudo que muda entre uma execucao e outra entra por variavel de
// ambiente (-e NOME=valor), para que o mesmo script rode contra a
// VPS de homologacao e contra a maquina do desenvolvedor sem edicao.
// =============================================================

// Sem barra no fim. Ex.: -e BASE=https://homolog.clinica.com.br
export const BASE = (__ENV.BASE || 'http://localhost').replace(/\/$/, '');

// Credenciais dos usuarios de carga criados por seed.sql. O seed gera
// login `carga_prof_1..N` e `carga_admin`, todos com a mesma senha.
export const SENHA_CARGA = __ENV.SENHA_CARGA || 'CargaTeste!2026';
export const QTD_PROFISSIONAIS = Number(__ENV.QTD_PROFISSIONAIS || 4);

// Alvo do RNF06. Vira o numero de VUs do cenario nominal.
export const USUARIOS_ALVO = Number(__ENV.USUARIOS_ALVO || 100);

// -------------------------------------------------------------
// Limites (thresholds).
//
// Sao criterio de aceite, nao enfeite de relatorio: o k6 sai com
// codigo != 0 quando algum estoura, entao "passou no teste de carga"
// vira um booleano verificavel e nao uma leitura de grafico.
//
// A separacao por tag `tipo` existe porque um unico p95 global mistura
// um GET de agenda com a geracao de um PDF de relatorio e nao significa
// nada. Cada faixa abaixo tem origem declarada:
//
//   leitura  800ms  — tela de agenda e busca de paciente; acima disso a
//                     navegacao ja parece travada, muito antes dos 3s.
//   escrita 1500ms  — criar/remarcar agendamento passa por validacao de
//                     conflito e escreve no historico.
//   relatorio 3000ms — agrega meses de agendamento; e a consulta mais
//                     pesada da aplicacao (RelatorioController).
//   documento 5000ms — geracao de PDF/XLSX (OpenPDF, POI). Sai da conta
//                     do RNF05 de proposito: e download, nao navegacao.
//
// NAO ha limite para `pagina`: o RNF05 fala de carregamento percebido e o
// k6 mede TTFB — ele nao executa JS, nao baixa CSS nem imagem e nao
// renderiza. Um limite aqui daria um "passou" sobre a metrica errada. Quem
// responde pelo RNF05 e o Lighthouse, com o sistema sob carga nominal (ver
// §5 do README). Tambem nao ha limite para `setup`, que roda uma vez com a
// JVM fria e nao representa regime.
// -------------------------------------------------------------
export const LIMITES = {
  'http_req_failed':                    ['rate<0.01'],
  'http_req_duration{tipo:leitura}':    ['p(95)<800'],
  'http_req_duration{tipo:escrita}':    ['p(95)<1500'],
  'http_req_duration{tipo:relatorio}':  ['p(95)<3000'],
  'http_req_duration{tipo:documento}':  ['p(95)<5000'],
  'http_req_duration{tipo:publico}':    ['p(95)<500'],
  // Login isolado porque e BCrypt puro: se ele degradar junto com o
  // resto, o gargalo e CPU, e esta linha e que denuncia.
  'http_req_duration{tipo:login}':      ['p(95)<2000'],
};

// -------------------------------------------------------------
// Login.
//
// Roda UMA vez por VU, em setup() ou no init do cenario — nunca dentro
// do loop. BCryptPasswordEncoder com forca padrao 10 (SecurityConfig)
// custa ~50-100ms de CPU pura por chamada; com 2 vCPU no limite do
// container, um script que reloga a cada iteracao mede o BCrypt e mais
// nada. A clinica real loga uma vez por turno.
// -------------------------------------------------------------
export function autenticar(http, check, username, senha = SENHA_CARGA, tipo = 'login') {
  const r = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ username, senha }),
    { headers: { 'Content-Type': 'application/json' }, tags: { tipo } },
  );
  check(r, { 'login 200': (x) => x.status === 200 });
  if (r.status !== 200) {
    throw new Error(`login falhou para ${username}: ${r.status} ${r.body}`);
  }
  return r.json('accessToken');
}

export function cabecalhos(token, tipo) {
  return {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    tags: { tipo },
  };
}

// Data no formato que os @RequestParam LocalDate esperam (ISO).
export function dataISO(deslocamentoDias = 0) {
  const d = new Date();
  d.setDate(d.getDate() + deslocamentoDias);
  return d.toISOString().slice(0, 10);
}

// LocalDateTime sem fuso — e o que AgendamentoRequestDTO recebe.
export function dataHoraISO(dias, hora, minuto = 0) {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  d.setHours(hora, minuto, 0, 0);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
       + `T${p(d.getHours())}:${p(d.getMinutes())}:00`;
}

export function sorteia(lista) {
  return lista[Math.floor(Math.random() * lista.length)];
}
