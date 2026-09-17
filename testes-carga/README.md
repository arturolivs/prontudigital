# Testes de carga — RNF05 e RNF06

> Como medir se a configuração do [`doc/DEPLOY.md`](../doc/DEPLOY.md) — VPS de
> 4 GB / 2 vCPU, quatro containers com limites declarados — atende o sistema, e
> com quanta folga.
>
> Os dois requisitos em jogo estão em [`doc/Requisitos.md`](../doc/Requisitos.md):
> **RNF05** (carregamento < 3 s, linha 60) e **RNF06** (100 usuários simultâneos,
> linha 63). Ambos estão marcados ⚪ *não medido*. Isto aqui é o instrumento para
> mudar essa marca.

---

## 0. O que existe nesta pasta

| Arquivo | Para quê |
|---|---|
| `k6/config.js` | Limites (thresholds), login e helpers compartilhados |
| `k6/cenarios.js` | Os cinco cenários |
| `seed.sql` | Massa de teste: 505 usuários, ~19 mil agendamentos, 2 anos de histórico |
| `limpar.sql` | Desfaz o `seed.sql` e o que o teste gravou por cima |
| `coletar-metricas.sh` | Amostra `docker stats` e `/actuator/prometheus` durante a execução |

> **Estado:** executados contra a stack real em 17/09/2026 (Docker Desktop,
> backend + Postgres do `docker-compose.prod.yml`, limites de cgroup conferidos
> e idênticos aos da VPS). O `smoke` sai com **exit 0**. O `nominal`
> **reprovou**, e por larga margem — o resultado e o diagnóstico estão no §4.1.
> Antes de qualquer ajuste de infraestrutura, leia o §6.0.

---

## 1. Antes de qualquer coisa: três decisões que determinam se o número vale

### 1.1 O gerador não roda na VPS medida

O k6 consome CPU proporcional à carga que gera. Rodando na mesma máquina de
2 vCPU, ele disputa processador com o backend e o Postgres, e o resultado sai
pior do que a realidade — você reprovaria uma configuração que atende. Rode do
seu desktop, ou de uma VM descartável na mesma região (São Paulo, para o RTT ser
o da clínica e não o de outro continente).

### 1.2 "100 usuários simultâneos" não é 100 requisições por segundo

O RNF06 fala de **pessoas**, não de tráfego. Cem pessoas numa clínica ficam com a
tela aberta e agem em rajadas: abrem a agenda, atendem, voltam. Com *think time*
de 5 a 15 segundos — o que `jornadaClinica()` usa — 100 VUs geram algo perto de
**10 req/s**.

Um script sem think time transformaria os mesmos 100 VUs em ~100 req/s: dez vezes
o requisito. É o erro mais comum em teste de carga e ele reprova infraestrutura
adequada. Se você quiser testar 100 req/s, use o cenário `stress` e chame pelo
nome — é margem, não é o RNF06.

### 1.3 Base vazia não mede nada

Desde a correção da §1.2 do DEPLOY.md o banco nasce sem usuário e sem
agendamento. Contra tabela vazia, todo índice parece ótimo e todo plano de query
cabe no cache. O `seed.sql` existe por isso.

### 1.4 Onde rodar

**Nunca contra produção com dado real.** A janela certa é a do §11 do DEPLOY.md,
"antes de expor": a stack já está de pé no domínio definitivo, ainda sem nenhum
paciente cadastrado. É o único momento em que se mede o ambiente real sem risco.
Depois disso, use uma segunda VPS idêntica.

---

## 2. Preparo

### 2.1 Instalar o k6 (na máquina geradora, não na VPS)

```bash
# Linux
sudo gpg -k && sudo gpg --no-default-keyring \
  --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt update && sudo apt install k6

# macOS
brew install k6

# Windows
winget install k6 --source winget
```

Sem instalar nada: `docker run --rm -i -v "$PWD/k6:/scripts" grafana/k6 run /scripts/cenarios.js`
(mas a rede do container muda a medição de latência; prefira o binário).

### 2.2 Carregar a massa

```bash
cd /opt/prontudigital/docker/prod
docker compose -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < ../../testes-carga/seed.sql
```

Espere a contagem no fim: ~505 usuários de carga e ~19 mil agendamentos. O script
**recusa rodar** se encontrar mais de um usuário fora do prefixo `carga_` — é o
guarda-corpo contra apontar para o banco errado.

> O `seed.sql` usa `pgcrypto` (`crypt(..., gen_salt('bf', 10))`) para gerar o
> BCrypt no próprio banco. Nenhum hash de senha volta para o repositório, e a
> força 10 é a mesma do `BCryptPasswordEncoder` de `SecurityConfig.java:87` — o
> custo de CPU por login no teste é o custo real.

### 2.3 Depurar SQL, quando precisar

`application-prod.yaml` silencia `org.hibernate.SQL` em produção — e precisa
silenciar explicitamente, porque `logging.level.root: WARN` **não** alcança um
logger nomeado no `application.yaml` base. Para investigar uma consulta lenta,
sem rebuild:

```bash
# no .env, ou como override temporário
LOG_LEVEL_SQL=DEBUG
docker compose -f docker-compose.prod.yml up -d backend   # recria, não `restart`
```

Foi assim que o §6.0 foi diagnosticado. **Desligue depois**: o volume é enorme e
derruba o próprio teste. O `BasicBinder` fica fora dessa chave de propósito — é
ele que despeja os parâmetros da query, ou seja CPF e dado clínico, no log.

### 2.4 Começar a coleta interna

Numa sessão SSH separada, **antes** de disparar o k6:

```bash
cd /opt/prontudigital/testes-carga
TOKEN_ADMIN=$(curl -s -X POST https://SEU-DOMINIO/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"carga_admin","senha":"CargaTeste!2026"}' | jq -r .accessToken) \
  ./coletar-metricas.sh 5 ./saida-nominal
```

---

## 3. Os cinco cenários

```bash
cd testes-carga/k6
export BASE=https://SEU-DOMINIO
```

| Ordem | Comando | Duração | O que responde |
|---|---|---|---|
| 1 | `k6 run -e CENARIO=smoke -e BASE=$BASE cenarios.js` | 1 min | O script está correto. Não mede capacidade |
| 2 | `k6 run -e CENARIO=nominal -e BASE=$BASE cenarios.js` | 13 min | **RNF06** — atende 100 simultâneos? |
| 3 | `k6 run -e CENARIO=stress -e BASE=$BASE cenarios.js` | 11 min | Onde quebra. É daqui que sai a palavra "folga" |
| 4 | `k6 run -e CENARIO=soak -e BASE=$BASE cenarios.js` | 2 h | Vazamento de heap, conexão que não volta ao pool, `exit 137` |
| 5 | `k6 run -e CENARIO=publico -e BASE=$BASE cenarios.js` | 5 min | A superfície anônima do agendamento |

Variáveis úteis: `-e USUARIOS_ALVO=150`, `-e VUS_SOAK=60`, `-e DURACAO_SOAK=4h`,
`-e RPS_PUBLICO=50`.

O **mix** do cenário logado espelha o uso de uma clínica, não uma distribuição
uniforme: 60% leitura de agenda, 20% escrita, 10% prontuário, 5% relatório, 5%
exportação de documento. Partes iguais superestimariam o custo dos relatórios e
subestimariam o da agenda, que é o que roda o dia inteiro.

---

## 4. Critérios de aceite

O k6 sai com **código ≠ 0** quando um limite estoura, então "passou no teste de
carga" é verificável e não uma leitura de gráfico. Os limites estão em
`k6/config.js`, cada um com a origem declarada em comentário:

| Tag | p95 | Por quê |
|---|---|---|
| `leitura` | 800 ms | Acima disso a navegação já parece travada, muito antes dos 3 s |
| `escrita` | 1500 ms | Passa por validação de conflito e grava histórico |
| `relatorio` | 3000 ms | Agrega meses de agendamento; é a consulta mais pesada |
| `documento` | 5000 ms | PDF/XLSX. Fora do RNF05 de propósito: é download, não navegação |
| `publico` | 500 ms | Rota anônima, sem JOIN pesado |
| `login` | 2000 ms | Isolado porque é BCrypt puro — ver §6.1 |
| `http_req_failed` | < 1% | 409 de conflito **não** conta como falha |

Duas tags **não** têm limite, de propósito:

- **`pagina`** — o RNF05 é carregamento percebido e o k6 mede TTFB. Um limite
  aqui daria um "passou" sobre a métrica errada; quem responde é o Lighthouse
  (§5).
- **`setup`** — as chamadas de `setup()` rodam uma vez, com a JVM fria. Na
  primeira execução real elas estavam marcadas como `publico` e duas amostras de
  aquecimento reprovaram a tag inteira (679 ms contra o limite de 500 ms). Hoje
  ficam isoladas.

Mas o k6 sozinho não autoriza dizer "atende com folga". Para isso, **todos**
estes precisam valer no platô:

- [ ] p95 dentro dos limites acima
- [ ] CPU de cada container **abaixo de 70%** (`docker-stats.csv`)
- [ ] **swap em zero** — se o swap da §3.2 do DEPLOY.md for tocado, a memória
      está no limite mesmo que o p95 esteja bom
- [ ] nenhum container com `exit 137` (OOMKilled): `docker inspect <nome> --format '{{.State.ExitCode}}'`
- [ ] `hikaricp_connections_pending` ≈ 0 — se houver fila, o pool é o gargalo
- [ ] pausa de GC < 200 ms (`jvm_gc_pause_seconds_max`)
- [ ] no soak: heap após Full GC **estável**. Crescimento linear é vazamento
- [ ] cenário `stress` quebrando em **≥ 2–3× a carga alvo**

Sem o último item, "passou" só diz que passou naquele ponto — não diz que há
margem.

---

## 4.1 Resultado do `nominal` — 17/09/2026: **reprovado**

100 VUs, 13 min, 3.429 iterações, 5.587 requisições, 7,08 req/s.

| Tag | Limite | Medido | |
|---|---|---|---|
| `leitura` | p95 < 800 ms | **15,81 s** | ✗ 20× acima |
| `escrita` | p95 < 1500 ms | **11,49 s** | ✗ |
| `relatorio` | p95 < 3 s | **22,64 s** | ✗ |
| `documento` | p95 < 5 s | **17,80 s** | ✗ |
| `login` | p95 < 2 s | **9,86 s** | ✗ |
| `http_req_failed` | < 1% | 0,05% | ✓ |

O sistema **não caiu** — 99,94% dos checks passaram, nenhum timeout de pool,
nenhum OOMKilled. Ele ficou inutilizável por lentidão, que é o modo de falha
mais difícil de perceber em produção: nada quebra, tudo demora.

### Onde está o gargalo

| Evidência | Valor | Leitura |
|---|---|---|
| CPU do backend | **219,6%** (teto 200%) | saturado |
| CPU do Postgres | **109,4%** (teto 100%) | saturado |
| Memória backend / db | 55% / 26% | **folgada** |
| `OOMKilled` | false, 0 restarts | memória não é o problema |
| `hikaricp_connections_acquire_seconds_max` | **10,95 s** | houve fila de ~11 s por conexão |
| `hikaricp_connections_timeout_total` | 0 | ninguém estourou os 30 s |
| `jvm_gc_pause_seconds_max` (major) | 0,476 s | alto, mas não domina |

A cadeia causal, de trás para frente:

1. o N+1 do §6.0 faz cada leitura de agenda pedir ~1.000 round-trips ao banco;
2. o Postgres satura o seu 1 vCPU;
3. as conexões ficam ocupadas por muito mais tempo;
4. o pool de 20 esgota e as requisições passam a **esperar até 11 s só para
   pegar conexão**;
5. o backend satura os seus 2 vCPU serializando ~1.000 resultsets por
   requisição.

Isto confirma a hipótese do §6.2 — mas com a causa invertida em relação ao que
eu supunha. O pool de 20 não é grande demais para o banco: ele é **pequeno
demais para o tempo que cada conexão fica presa**. Aumentá-lo não resolveria;
só moveria a fila para dentro do Postgres.

Por rota, do próprio backend:

| Rota | p95 servidor | Chamadas |
|---|---|---|
| `/api/agendamentos/agenda` | **8,05 s** | 2.393 |
| `/api/relatorios/atendimentos/exportar` | 6,41 s | 187 |
| `/api/agendamentos/pacientes` | 0,80 s | 1.010 |
| `/api/agendamentos/{id}` | 0,03 s | 357 |

A diferença entre os 8,05 s que o backend mede e os 15,81 s que o k6 vê é tempo
de **fila antes de chegar ao handler** — as threads do Tomcat estão todas
ocupadas. Metade da latência acontece antes de a requisição começar a ser
processada.

### O que este número vale, e o que não vale

**Vale** como diagnóstico. Backend e Postgres rodaram com os mesmos limites de
cgroup da VPS (`NanoCpus` 2.0 e 1.0, memória 1 GB e 512 MB — conferido com
`docker inspect`), e ambos saturaram.

**Não vale** como veredito final do RNF06, e o desvio é para o lado otimista:
aqui o host tem 8 núcleos, então os containers não disputaram CPU entre si. Na
VPS de 2 vCPU eles disputam — a soma dos limites é 4,0 sobre 2 reais — e ainda
há Caddy e frontend no caminho. **Na VPS o resultado seria pior, não melhor.**

### Conclusão

Não adianta mexer em `maximum-pool-size`, em `shared_buffers` nem no tamanho da
VPS enquanto uma tela de agenda custar ~1.000 queries. O caminho é o §6.0.
Depois dele, repetir este cenário — e só então os ajustes de infraestrutura
passam a fazer diferença mensurável.

---

## 5. O que o k6 não mede

**O RNF05 é tempo percebido, o k6 mede TTFB.** Ele não executa JavaScript, não
baixa CSS nem imagens e não renderiza nada. Os 3 segundos do requisito incluem
tudo isso. Meça em paralelo, com o sistema sob carga nominal:

```bash
npx lighthouse https://SEU-DOMINIO/login --preset=desktop --output=html \
  --output-path=./lighthouse-login.html
```

O número que interessa é o **Largest Contentful Paint**. Rode duas vezes: com o
sistema ocioso e durante o cenário `nominal`. A diferença entre os dois é quanto
o SSR do Next.js sofre com a concorrência — e ele tem o limite de CPU mais
apertado da stack (`0.5`, em `docker-compose.prod.yml`).

---

## 6. Suspeitos, em ordem

O §6.0 foi **medido**; os demais continuam sendo hipótese a confirmar.

### 6.0 N+1 na tela de agenda — medido em 17/09/2026 ⚠️

Na primeira execução do `smoke`, o p95 de `leitura` estourou e o
`/actuator/prometheus` apontou a rota: `/api/agendamentos/agenda`, p95 de
**973 ms** no servidor. Medindo por tipo de visualização, contra os 20.340
agendamentos do `seed.sql`:

| Visualização | Linhas | Tempo médio |
|---|---|---|
| `DIA` | 9 | 70 ms |
| `SEMANA` | 45 | 363 ms |
| `MES` | 198 | **1410 ms** |

Com `LOG_LEVEL_SQL=DEBUG`, uma única requisição de `MES` dispara **978 queries**:

| Tabela | Queries | |
|---|---|---|
| `agendamentos` | 1 | a consulta que interessa |
| `usuarios` | 398 | ~2 por linha (paciente + profissional), trazendo `senha_hash` junto |
| `evolucoes_enfermagem` | 198 | 1 por linha |
| `evolucoes_curativos` | 198 | 1 por linha |
| `usuario_perfis` | 178 | |

São ~4,9 queries por agendamento. A causa está na modelagem: `agendamentos`
guarda `paciente_uuid` e `profissional_uuid` como UUID solto, **sem FK** (V5), e
`AgendamentoViewDTO` precisa de `nomePaciente` e `nomeProfissional` — então cada
linha vira uma busca separada.

**Por que isto importa mais que qualquer ajuste de container:** sob a carga
nominal do RNF06 (~10 req/s), se uma fração abrir a visão mensal, são milhares de
queries por segundo contra um Postgres limitado a `cpus: "1.0"`. Nenhum tamanho
de pool resolve — a fila só muda de lugar. Subir a VPS de 4 para 8 GB também não:
o problema é número de round-trips, não memória.

Isto é um achado, não um item já corrigido. O caminho é uma projeção com `JOIN`
(ou `JOIN FETCH`) em `visualizarAgenda`, devolvendo nome e evolução na mesma
consulta. Fica fora do escopo destes scripts.

> A medição acima é de Docker Desktop no Windows, 1 VU, cache frio — **não** é a
> VPS e não é o veredito do RNF. O que vale aqui é a razão 978:1, que independe
> de hardware.

### Os demais

Estes são os pontos da configuração proposta que a carga deve encontrar em
seguida. Servem como hipótese a confirmar, não como conclusão.

### 6.1 BCrypt no login

`SecurityConfig.java:87` usa `new BCryptPasswordEncoder()` — força 10, ~50–100 ms
de **CPU pura** por login. Com `cpus: "2.0"`, o teto teórico é ~20–40 logins/s se
mais nada estiver rodando.

Por isso `config.js` faz login **uma vez por VU** e reutiliza o token: um script
que reloga a cada iteração vira um benchmark de BCrypt e nada mais. A tag `login`
existe separada justamente para flagrar se *ele* degrada enquanto o resto não —
nesse caso o gargalo é CPU.

> No cenário `stress` o k6 aloca VUs ao longo da rampa, e cada VU novo faz um
> login. São até 600 BCrypts distribuídos em 11 minutos — pouco, mas é a razão
> de a tag `login` ficar fora dos limites de leitura e escrita: misturada, ela
> contaminaria o p95 com um custo que o uso real paga uma vez por turno.

### 6.2 Pool do Hikari × CPU do Postgres

`application-prod.yaml` declara `maximum-pool-size: 20` contra um Postgres com
`cpus: "1.0"` e parâmetros default (`shared_buffers` 128 MB, `work_mem` 4 MB). 20
conexões ativas em 1 vCPU não são processadas em paralelo — a fila só muda de
lugar, do pool para o banco. Para este hardware, 10–12 é mais honesto.

Métrica que decide: `hikaricp_connections_pending`. Zero = o pool está folgado
e o número atual está bom. Consistentemente > 0 = há fila, e aí vale comparar
com `hikaricp_connections_active`.

### 6.3 Memória do Postgres

`memory: 512M` com 20 conexões fazendo sort/hash nos relatórios dá 80 MB só de
`work_mem`, mais os 128 MB de `shared_buffers`. É o primeiro candidato a subir
para 768M–1G, e há folga: a soma dos limites dos quatro containers é **2,0 GB de
4 GB**.

### 6.4 CPU do frontend

`cpus: "0.5"` é o limite mais apertado da stack, e o SSR do Next está no caminho
crítico do RNF05 — o TTFB da página passa por ele. Some os limites de CPU: 1.0 +
2.0 + 0.5 + 0.5 = **4,0 vCPU declarados sobre 2 físicos**. Não é erro (limite não
é reserva), mas significa disputa no pico. É o que o cenário `stress` revela.

### 6.5 A rota pública carrega todos os usuários

`PacientePublicoController.java:34` chama `usuarioService.listarTodos()` — sem
paginação — e filtra em memória quem tem perfil `PROFISSIONAL`. A cada visita
anônima à tela de agendamento, o banco devolve a tabela `usuarios` inteira com os
perfis, para entregar quatro nomes.

Com os ~20 usuários de uma clínica isso não aparece. Com os **505 do `seed.sql`**
começa a aparecer, e é exatamente por isso que o cenário `publico` existe
separado: ele é a rota mais exposta (anônima, na internet) sobre a consulta menos
seletiva. Se o p95 de `publico` destoar do resto, o suspeito é este, não o Caddy.

A mesma forma está em `UsuarioServiceImpl.java:79`, mas ali atrás de
autenticação e de uma tela que poucos abrem.

---

## 7. Depois

```bash
# Limpar a massa
docker compose -f docker-compose.prod.yml exec -T postgres \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < ../../testes-carga/limpar.sql
```

Registre o resultado em `doc/Requisitos.md`, trocando o ⚪ do RNF05 e do RNF06
pela medida e pela data. Um requisito medido uma vez e não anotado volta a ser
"não medido" no mês seguinte.

E uma nota de contexto para quando o número vier apertado: numa VPS, capacidade é
um controle deslizante — 4 GB/2 vCPU → 8 GB/4 vCPU é um reboot, não uma
rearquitetura. O que **não** é deslizante é I/O de disco e o fato de ser host
único sem HA. Ali o plano de recuperação é o backup da §9 do DEPLOY.md, e o teste
de restauração vale tanto quanto este.
