# Auditoria do deploy em produção — pendências

> Escopo: `docker/prod/docker-compose.prod.yml`, `docker/prod/Caddyfile`,
> `backend/Dockerfile.prod`, `frontend-web/Dockerfile.prod`,
> `docker/prod/scripts/backup.sh`. Levantado em 15/09/2026.

## 🟠 Problemas

- [ ] **Nenhum `.dockerignore`** — contexto do frontend com 596 MB (500 MB de `node_modules`, 95 MB de `.next`); pior, o `COPY . .` sobrescreve o `node_modules` instalado pelo `npm ci` com o do host. → criar `frontend-web/.dockerignore` (`node_modules`, `.next`, `.env*`).
- [ ] **A validação do `.env` na §5 do DEPLOY.md não valida nada** — `docker compose config` sai com exit 0 mesmo com variável ausente, só emite warning. → usar `${VAR:?mensagem}` nas obrigatórias.
- [ ] **`MaxRAMPercentage=75` com `memory: 1G`** — heap de 768 MB + metaspace + pilhas encosta no limite; risco de `OOMKilled` (exit 137). → 60–65%.
- [ ] **Tokens de confirmação gravados inteiros no `access.log` do Caddy** — o controller já redige nos logs dele, o proxy não. → filtro `replace` no bloco `log`.
- [ ] **Imagens só com `:latest`** — rollback exige rebuild de 8–20 min com o sistema fora. → taggear com o SHA do commit.

## 🔒 Segurança

- [ ] **Sem hardening nos quatro containers** — `security_opt: ["no-new-privileges:true"]` e `cap_drop: [ALL]`; `read_only: true` + `tmpfs` só no frontend (backend precisa de `/tmp`, Caddy precisa de `NET_BIND_SERVICE`).
- [ ] **Caddyfile sem `Content-Security-Policy` e `Permissions-Policy`** — `X-Frame-Options: DENY` sozinho não cobre navegador moderno.
- [ ] **Caddy sem `admin off`** — a API admin escuta em `:2019` dentro do container.
- [ ] **Caddy sem `email` no ACME** — nenhum aviso de expiração de certificado.
- [ ] **Caddy sem `request_body { max_size 12MB }`** — POST grande atravessa o proxy até o Spring recusar.
- [ ] **Segredos em variável de ambiente** — `JWT_SECRET`, senha do Postgres e `ADMIN_SENHA` visíveis em `docker inspect` e `/proc/1/environ` para quem está no grupo `docker`.

## 🔵 Menores

- [ ] `-Djava.security.egd=file:/dev/./urandom` obsoleto desde o JDK 9 (`backend/Dockerfile.prod:49`).
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

- [x] **Upload de anexo e avatar falhava** — `/dados/anexos` não existia na imagem, o volume nascia `root:root` e o backend roda como `prontu`. `backend/Dockerfile.prod` agora faz `mkdir -p /dados/anexos && chown -R prontu:prontu /dados` antes do `USER`, e o volume nomeado herda esse dono ao ser criado. Em instalação que já subiu antes desta correção o volume existente continua `root:root` — ver a nota da §7 do `DEPLOY.md`.
- [x] **Backend sem saída para a internet** — estava só na rede `interna` (`internal: true`), sem DNS nem egress. Agora está em `interna` + `externa` no `docker-compose.prod.yml`; sem `ports:` continua inalcançável de fora, e `graph.facebook.com` passa a ser resolvível para quando o WhatsApp for ligado.
- [x] `DEPLOY.md` §7 ganhou as verificações 7.7 (escrita em `/dados/anexos`) e 7.8 (egress até `graph.facebook.com:443`).

## ✅ Corrigidos em 15/09/2026

- [x] `ADMIN_NOME` sem aspas em `.env.prod.example` — quebrava o `backup.sh`, que lê o arquivo com `source` sob `set -euo pipefail` (`da: command not found`, exit 127). Backup noturno morreria calado.
- [x] §8.2 do `DEPLOY.md` dizia "não é preciso reiniciar nada" ao apagar `ADMIN_SENHA` — a variável continua no ambiente do container em execução até um `up -d backend`.
