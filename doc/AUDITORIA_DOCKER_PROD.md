# Auditoria do deploy em produção — pendências

> Escopo: `docker/prod/docker-compose.prod.yml`, `docker/prod/Caddyfile`,
> `backend/Dockerfile.prod`, `frontend-web/Dockerfile.prod`,
> `docker/prod/scripts/backup.sh`. Levantado em 15/09/2026.

## 🟠 Problemas

- [ ] **`WHATSAPP_ACCESS_TOKEN` e `WHATSAPP_APP_SECRET` ainda em variável de ambiente** — irrelevante enquanto `NOTIFICACOES_HABILITADAS=false` (estão vazios). Ao ligar o WhatsApp, mova para `docker/prod/secrets/` como os outros três: arquivos `app.whatsapp.cloud-api.access-token` e `app.whatsapp.webhook.app-secret`, e placeholders correspondentes no `application-prod.yaml`.
- [ ] **Tokens de confirmação gravados inteiros no `access.log` do Caddy** — o controller já redige nos logs dele, o proxy não. → filtro `replace` no bloco `log`.
- [ ] **Imagens só com `:latest`** — rollback exige rebuild de 8–20 min com o sistema fora. → taggear com o SHA do commit.

## 🔵 Menores

- [ ] `-Djava.security.egd=file:/dev/./urandom` obsoleto desde o JDK 9 (`backend/Dockerfile.prod:53`).
- [ ] Healthcheck do backend testa só a porta TCP — a imagem tem `curl` e o actuator expõe `/actuator/health`.
- [ ] `restart: always` reinicia até depois de `docker stop` explícito — `unless-stopped` costuma ser o desejado.
- [ ] `frontend` sem healthcheck — o `depends_on` do Caddy só espera o container iniciar.
- [ ] Imagens base sem pin — `eclipse-temurin:21-jre` hoje resolve para Ubuntu 26.04.
- [ ] `logging:` só no `daemon.json` do host — some se a VPS for reconstruída.
- [ ] Sem `stop_grace_period` no Postgres — risco baixo (a imagem já usa `STOPSIGNAL SIGINT`), mas 30s é folga barata.

## ✅ Verificado e correto

- [x] `deploy.resources.limits` **é** aplicado pelo Compose v2 fora do Swarm (`memory: 128M` → `134217728` no cgroup).
- [x] `bash` existe na imagem do backend — o healthcheck atual funciona.
- [x] Postgres inalcançável de fora (`expose` + `internal: true`, sem `ports`).
- [x] Ordem dos blocos `handle` no Caddyfile — `/api/auth/session*` e `/refresh*` antes de `/api/*`.
- [x] `backup.sh` — banco antes dos anexos, verificação com `pg_restore --list` antes da retenção, `exit 2` se a cópia externa falhar.

## ✅ Corrigidos em 16/09/2026

- [x] **Hardening dos quatro containers** — `no-new-privileges:true` e `cap_drop: [ALL]` em todos. O `postgres` recupera as cinco capabilities que o entrypoint precisa para ajustar o PGDATA e trocar de usuário (`CHOWN`, `DAC_OVERRIDE`, `FOWNER`, `SETGID`, `SETUID`) e o `caddy` recupera `NET_BIND_SERVICE` (portas 80/443). `read_only: true` + `tmpfs` só no frontend, como previsto: o backend escreve em /tmp e o Caddy em /data, /config e /var/log/caddy.
- [x] **`Content-Security-Policy` e `Permissions-Policy` no Caddyfile** — CSP com `frame-ancestors 'none'` (o que o `X-Frame-Options` fazia sozinho), `blob:` onde o app abre anexo e relatório por `URL.createObjectURL`, e `'unsafe-inline'` em script/style enquanto não houver nonce por request no Next. Permissions-Policy nega câmera, microfone, localização, pagamento e companhia.
- [x] **`admin off`** — confirmado no JSON adaptado: `"admin": {"disabled": true}`.
- [x] **`email` no ACME** — vem de `ACME_EMAIL`, novo no `.env` e nas obrigatórias do Compose. Sem ele o Caddy nem adapta a config (`wrong argument count ... 'email'`), então a validação do `.env` passa a barrar antes, com mensagem.
- [x] **`request_body { max_size 12MB }`** — no bloco do site, casando com `spring.servlet.multipart.max-request-size`.
- [x] **Segredos saíram das variáveis de ambiente** — senha do banco, segredo do JWT e senha do primeiro ADMIN viraram arquivos em `docker/prod/secrets/`, montados em /run/secrets. O `postgres` lê por `POSTGRES_PASSWORD_FILE`; o backend lê o diretório como config tree do Spring, onde **o nome do arquivo é o nome da propriedade** — daí os secrets se chamarem `spring.datasource.password`, `spring.flyway.password`, `app.jwt.secret` e `app.bootstrap-admin.senha`. Duas armadilhas encontradas testando: indireção não funciona (`password: ${db.password}` no YAML vai parar no JDBC como literal e o banco recusa), e `spring.config.import` declarado em arquivo profile-specific não é aplicado — por isso o import está no compose, como `SPRING_CONFIG_IMPORT`. `docker inspect` do backend não mostra mais nenhum segredo. `WHATSAPP_ACCESS_TOKEN` e `WHATSAPP_APP_SECRET` continuam em variável de ambiente — hoje vazios; quando o WhatsApp for ligado, o mesmo tratamento (ver 🟠).
- [x] **Build do frontend quebrava: `frontend-web/public/` não existia** — `frontend-web/Dockerfile.prod` copia `/app/public` do estágio de build, e o diretório não estava no repositório nem é gerado pelo `next build`; `COPY` com origem inexistente falha o build inteiro. → `frontend-web/public/.gitkeep` versionado, com comentário no Dockerfile avisando para não apagar o diretório. `npm run build` local confirma que os três caminhos copiados passam a existir (`.next/standalone/server.js`, `.next/static`, `public`); o build da imagem em si não foi executado — Docker parado na máquina de desenvolvimento.
- [x] **`.dockerignore` criado** — `frontend-web/.dockerignore` (tira 595 MB do contexto e impede o `COPY . .` de sobrescrever o `node_modules` do `npm ci`) e `backend/.dockerignore` (`target`, `dados`, `.git`).
- [x] **Validação do `.env` agora valida** — bloco `x-env-obrigatorias` no `docker-compose.prod.yml` com `${VAR:?mensagem}` nas sete obrigatórias; `config` e `up` saem com 1 e a mensagem da variável. `ADMIN_*` ficaram de fora de propósito: saem do `.env` depois do primeiro acesso (§8.2 do `DEPLOY.md`). A §5 do `DEPLOY.md` ganhou os dois checks que a validação não cobre (placeholder `TROQUE` e `ADMIN_*` no primeiro deploy).
- [x] **`MaxRAMPercentage` 75 → 65** (`backend/Dockerfile.prod`), com o porquê no comentário e o lembrete de revisar junto se o `memory:` do serviço mudar.
- [x] **Upload de anexo e avatar falhava** — `/dados/anexos` não existia na imagem, o volume nascia `root:root` e o backend roda como `prontu`. `backend/Dockerfile.prod` agora faz `mkdir -p /dados/anexos && chown -R prontu:prontu /dados` antes do `USER`, e o volume nomeado herda esse dono ao ser criado. Em instalação que já subiu antes desta correção o volume existente continua `root:root` — ver a nota da §7 do `DEPLOY.md`.
- [x] **Backend sem saída para a internet** — estava só na rede `interna` (`internal: true`), sem DNS nem egress. Agora está em `interna` + `externa` no `docker-compose.prod.yml`; sem `ports:` continua inalcançável de fora, e `graph.facebook.com` passa a ser resolvível para quando o WhatsApp for ligado.
- [x] `DEPLOY.md` §7 ganhou as verificações 7.7 (escrita em `/dados/anexos`) e 7.8 (egress até `graph.facebook.com:443`).

> Os seis itens de segurança foram verificados com a stack de pé (Docker Desktop, domínio `localhost`): Postgres e backend `healthy` com `cap_drop: ALL`, upload gravando em `/dados/anexos` como `prontu`, frontend servindo com `read_only`, login pelo Caddy devolvendo 200 com a senha vinda de `secrets/admin.senha` e token assinado com `secrets/jwt.secret`, corpo de 13 MB recusado com 413, e `docker inspect` do backend sem nenhuma senha.

## ✅ Corrigidos em 15/09/2026

- [x] `ADMIN_NOME` sem aspas em `.env.prod.example` — quebrava o `backup.sh`, que lê o arquivo com `source` sob `set -euo pipefail` (`da: command not found`, exit 127). Backup noturno morreria calado.
- [x] §8.2 do `DEPLOY.md` dizia "não é preciso reiniciar nada" ao apagar `ADMIN_SENHA` — a variável continua no ambiente do container em execução até um `up -d backend`.
