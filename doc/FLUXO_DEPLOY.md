# Fluxo de deploy e pipelines CI/CD — ProntuDigital

> **Visão de cima.** Do commit até o sistema no ar em
> `https://purpleclin.prontudigital.com.br`, em diagramas. Descreve o estado do
> repositório em **01/10/2026**.
>
> Esta página mostra *o caminho*. Os detalhes estão nos vizinhos:
>
> | Documento | Para quê |
> |---|---|
> | [`DEPLOY.md`](./DEPLOY.md) | Preparar a VPS do zero e operar no dia a dia |
> | [`CICD.md`](./CICD.md) | Secrets, variables e o que fazer quando o pipeline falha |
> | [`GITHUB_ACTIONS.md`](./GITHUB_ACTIONS.md) | `ci.yml` e `cd.yml` lidos comando por comando |

---

## 1. Visão geral

```mermaid
flowchart LR
    dev([Desenvolvedor]) -->|push / PR| gh[(GitHub<br/>arturolivs/prontudigital)]

    subgraph actions [GitHub Actions]
        ci[CI<br/>testes, lint, build]
        cd[CD<br/>publica e implanta]
    end

    gh -->|branch ≠ main<br/>ou pull request| ci
    gh -->|push na main<br/>ou Run workflow| cd
    cd -->|chama| ci
    cd -->|docker push| ghcr[(GHCR<br/>ghcr.io/arturolivs)]
    cd -->|SSH :22022| vps

    subgraph vps [VPS — Ubuntu 22.04, São Paulo]
        deploy[scripts/deploy.sh]
        caddy[Caddy<br/>80/443 + TLS]
        front[frontend<br/>Next.js]
        back[backend<br/>Spring Boot]
        pg[(PostgreSQL)]
    end

    ghcr -->|docker pull| deploy
    deploy --> caddy & front & back & pg
    user([Clínica]) -->|HTTPS| caddy
    caddy --> front
    caddy -->|/api/*| back
    back --> pg
```

As três regras que explicam o desenho:

1. **Nada é compilado na VPS.** Maven e `next build` consomem mais memória que
   o sistema inteiro em execução e derrubariam uma máquina de 4 GB. A
   compilação acontece no Actions, e a VPS só baixa a imagem pronta.
2. **Nada chega à produção sem passar pelo CI.** O CD chama o mesmo `ci.yml`
   antes de publicar imagem. Não há atalho.
3. **O deploy é um script, não o workflow.** O `deploy.sh` vive no repositório
   (e portanto na VPS). O workflow só o invoca por SSH, e o mesmo comando roda
   à mão quando o GitHub estiver fora do ar.

---

## 2. Branches e gatilhos

```mermaid
gitGraph
    commit id: "main"
    branch config-deploy
    checkout config-deploy
    commit id: "trabalho"
    commit id: "push → CI"
    checkout main
    merge config-deploy id: "merge → CD → produção"
```

| Evento | Workflow | Resultado |
|---|---|---|
| Push em qualquer branch **exceto** `main` | CI | Testes e validação. Nada é publicado |
| Pull request (qualquer destino) | CI | Idem, com o resultado aparecendo no PR |
| Push na `main` | CD (que chama o CI) | Imagens publicadas **e deploy em produção** |
| *Actions > CD > Run workflow*, campo `tag` vazio | CD completo | Igual ao push na `main`, a partir do commit escolhido |
| *Actions > CD > Run workflow*, `tag` preenchida | CD só com deploy | Pula CI e build; implanta uma imagem já publicada. **É o rollback** |
| Dependabot (mensal) | CI, via PR | PRs de atualização de Maven, npm, imagens base e actions |

**Mudança só de documentação não faz deploy.** O CD ignora push que altera
apenas `**.md`, `doc/**`, `testes-carga/**` e imagens (`*.jpg`, `*.png`).

> Merge na `main` **é** deploy em produção. Com o environment `producao`
> configurado com *Required reviewers*, o job de implantação espera um clique
> de aprovação antes de tocar na VPS.

---

## 3. CI — `.github/workflows/ci.yml`

Quatro jobs **em paralelo**, sem dependência entre si. Um push novo na mesma
branch cancela a execução anterior.

```mermaid
flowchart TB
    gatilho([push / PR / chamado pelo CD]) --> b & f & i & c

    b["<b>backend</b><br/>JDK 21 · ./mvnw verify<br/><i>único lugar onde os testes rodam</i>"]
    f["<b>frontend</b><br/>Node 20 · npm ci<br/>lint · type-check"]
    i["<b>imagens</b> (matriz backend, frontend)<br/>docker build do Dockerfile.prod<br/><i>sem push</i>"]
    c["<b>compose</b><br/>docker compose config<br/>base e base + ghcr"]

    b --> ok{todos verdes?}
    f --> ok
    i --> ok
    c --> ok
    ok -->|sim| verde([✅ CI ok])
    ok -->|não| vermelho([❌ bloqueia o CD])
```

| Job | O que garante | Ao falhar |
|---|---|---|
| **backend** | Os testes do Spring passam. O `Dockerfile.prod` compila com `-DskipTests`, então **é só aqui** que eles rodam | Relatórios do Surefire ficam como artifact por 7 dias |
| **frontend** | ESLint e `tsc` limpos. O `next build` fica no job `imagens`, para não compilar duas vezes | Saída do lint/tsc no log |
| **imagens** | Os dois `Dockerfile.prod` continuam construindo. Usa o cache do Actions por serviço | Log do build |
| **compose** | O `docker-compose.prod.yml` interpola com o `.env` mínimo, e a sobreposição do GHCR **não deixa nenhum `build:`**. Se deixasse, a VPS tentaria compilar | `::error::` apontando a causa |

---

## 4. CD — `.github/workflows/cd.yml`

```mermaid
flowchart TB
    start([push na main<br/>ou Run workflow]) --> prep

    prep["<b>preparar</b><br/>tag = sha-&lt;7 chars&gt; ou a informada<br/>registro = ghcr.io/arturolivs<br/>confere a variable DOMINIO"]

    prep --> tem_tag{tag informada?}

    tem_tag -->|não| ci["<b>ci</b><br/>chama ci.yml"]
    ci --> img["<b>imagens</b> (matriz)<br/>build com NEXT_PUBLIC_BASE_URL=https://DOMINIO<br/>push :sha-xxxxxxx e :latest"]
    img --> impl

    tem_tag -->|sim: rollback| impl

    impl["<b>implantar</b> — environment producao<br/>(aguarda aprovação, se configurada)"]
    impl --> s1[1. chave SSH + known_hosts dos secrets]
    s1 --> s2[2. docker login ghcr.io na VPS<br/>token via stdin]
    s2 --> s3[3. git fetch + checkout --detach &lt;sha&gt;<br/>compose, Caddyfile e scripts na versão da imagem]
    s3 --> s4[4. deploy.sh sha-xxxxxxx]
    s4 --> s5[5. docker logout — sempre, mesmo com falha]
    s5 --> s6([Resumo no job: tag, site, resultado])
```

**Um deploy nunca é cancelado no meio.** O `concurrency` do CD enfileira
execuções em vez de cancelar, porque um `up -d` interrompido deixaria a pilha
meio velha, meio nova.

**Credenciais.** Não existe senha de registro guardada no GitHub. O
`GITHUB_TOKEN` da própria execução publica a imagem e autentica a VPS, e expira
quando o job termina. Na VPS entram só a chave SSH (`VPS_SSH_KEY`) e a
impressão do host (`VPS_KNOWN_HOSTS`). Lista completa em
[`CICD.md`](./CICD.md) §2.

**O domínio entra no build.** `NEXT_PUBLIC_BASE_URL` fica congelada dentro do
bundle do frontend. Por isso a variable `DOMINIO` é conferida no primeiro job,
e trocar de domínio exige rodar o CD de novo, não só editar o `.env` da VPS.

---

## 5. O deploy na VPS — `docker/prod/scripts/deploy.sh`

```mermaid
flowchart TB
    inicio([deploy.sh &lt;tag&gt;]) --> pre

    pre{"compose, .env e<br/>3 segredos existem?"}
    pre -->|não| abort0([❌ aborta sem tocar em nada])
    pre -->|sim| antiga[lê IMAGE_TAG atual do .env<br/>= versão para onde voltar]

    antiga --> no_ar{postgres no ar?}
    no_ar -->|sim| bkp[backup.sh<br/>banco + anexos]
    no_ar -->|não: 1º deploy| grava
    bkp -->|falhou| abort1([❌ aborta — migration<br/>não tem rollback])
    bkp -->|ok| grava

    grava[grava IMAGE_TAG=&lt;tag&gt; no .env] --> pull[compose pull backend frontend]
    pull -->|falhou| rb
    pull --> up[compose up -d --no-build]
    up -->|falhou| rb

    up --> saude{"até 240 s:<br/>backend healthy?<br/>frontend e caddy running?"}
    saude -->|unhealthy ou<br/>tempo esgotado| rb
    saude -->|sim| fumaca{"GET https://DOMINIO/<br/>até 6 tentativas"}
    fumaca -->|sem 2xx| rb
    fumaca -->|2xx| prune[docker image prune]
    prune --> fim([✅ deploy concluído])

    rb["<b>rollback automático</b><br/>imprime ps + logs do backend<br/>restaura IMAGE_TAG anterior<br/>compose up -d"] --> erro([❌ sai com erro → CD vermelho])
```

O que cada verificação prova:

| Verificação | Prova que… |
|---|---|
| `backend` **healthy** | A JVM subiu, **o Flyway aplicou as migrations** e `/actuator/health` responde. Há 60 s de tolerância inicial |
| `frontend` e `caddy` **running** | Não entraram em crash-loop. Nenhum dos dois tem healthcheck próprio |
| `GET https://DOMINIO/` | O caminho público inteiro funciona: DNS, certificado do Caddy e proxy até o Next.js |

> **Rollback de imagem não desfaz migration.** Se a versão nova alterou o
> esquema do banco, o `deploy.sh` volta a imagem, mas o banco continua no
> esquema novo. Por isso o backup vem antes de tudo, e a saída nesse caso é o
> `restore.sh` ([`DEPLOY.md`](./DEPLOY.md) §10.5).

---

## 6. Ordem de subida dos containers

```mermaid
sequenceDiagram
    autonumber
    participant D as deploy.sh
    participant P as postgres
    participant B as backend
    participant F as frontend
    participant C as caddy
    participant LE as Let's Encrypt

    D->>P: up
    P-->>P: healthy (~15 s)
    D->>B: up (depende do postgres healthy)
    B->>P: Flyway: migrations pendentes
    B-->>B: 1º boot: BootstrapAdminRunner cria o ADMIN
    B-->>B: healthy (/actuator/health)
    D->>F: up (depende do backend healthy)
    D->>C: up
    C->>LE: pede/renova certificado (desafio na porta 80)
    LE-->>C: certificado
    D->>C: GET https://DOMINIO/
    C->>F: proxy
    F-->>D: 200 → deploy concluído
```

---

## 7. Caminhos fora do normal

| Situação | Caminho | Detalhe |
|---|---|---|
| **Voltar uma versão** | *Run workflow* com `tag = sha-<anterior>` | Pula CI e build. Tags publicadas: *Packages* do repositório |
| **Actions fora do ar, imagem já publicada** | Na VPS: `git checkout --detach <sha>` + `deploy.sh sha-<sha>` | Mesmo script, mesmo resultado. [`DEPLOY.md`](./DEPLOY.md) §6.2 |
| **Actions fora do ar, imagem não existe** | `up -d --build` na própria VPS | Emergência: risco real de travar a VPS por falta de memória. [`DEPLOY.md`](./DEPLOY.md) §6.4 |
| **Deploy sem backup** | `PULAR_BACKUP=1 deploy.sh <tag>` | Só com consciência: migration não volta |
| **Restaurar dados** | `restore.sh --real <carimbo>` | Destrutivo, pede confirmação digitada |

---

## 8. Onde mora cada configuração

```mermaid
flowchart LR
    subgraph github [GitHub — Settings › Actions]
        v1[variable DOMINIO]
        v2[variable VPS_PORT = 22022]
        s1[secrets VPS_HOST, VPS_USER,<br/>VPS_SSH_KEY, VPS_KNOWN_HOSTS]
        env[environment producao]
    end
    subgraph vpsconf [VPS — /opt/prontudigital/docker/prod]
        e1[.env<br/>DOMINIO, ACME_EMAIL, flags, IMAGE_TAG]
        e2[secrets/<br/>db.password, jwt.secret, admin.senha]
    end
    subgraph repo [Repositório]
        r1[compose, Caddyfile, scripts]
    end

    v1 -->|build| bundle[bundle do frontend]
    e1 --> caddy2[Caddy + backend]
    e2 --> caddy2
    r1 -->|checkout a cada deploy| vpsconf
```

| Quer mudar… | Onde | Precisa de |
|---|---|---|
| Domínio | DNS + variable `DOMINIO` + `.env` da VPS | Rodar o CD (o frontend é reconstruído) |
| Flag ou variável do backend | `.env` da VPS | `up -d backend` com a sobreposição do GHCR. `restart` não basta |
| Segredo | `secrets/` na VPS | Recriar o container que o usa |
| Compose, `Caddyfile`, scripts | Repositório | Merge na `main`. Edição direta na VPS some no próximo deploy |
| Porta SSH, chave, IP da VPS | Secrets e variables do GitHub | Nada: vale no próximo deploy |
