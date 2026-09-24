# CI/CD — ProntuDigital

> Integração e entrega contínuas no GitHub Actions. Complementa
> [`DEPLOY.md`](./DEPLOY.md), que descreve a instalação manual da VPS —
> aqui está o que automatiza a **atualização** dela.
>
> Decisão de fundo ([`ANALISE_DEPLOY.md`](../ANALISE_DEPLOY.md) §3): **nada é
> compilado na VPS**. `docker compose up --build` roda Maven e `next build` na
> máquina de produção, consome muito mais memória que o runtime inteiro e
> derruba uma VPS pequena por OOM. O build acontece no Actions, as imagens vão
> para o GHCR e a VPS só dá `pull`.

---

## 1. Os dois workflows

| Arquivo | Quando roda | O que faz |
|---|---|---|
| [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) | PR e push em qualquer branch exceto `main`; e chamado pelo CD | Testes do backend, lint/tipos do frontend, build das duas imagens (sem publicar), validação do compose de produção |
| [`.github/workflows/cd.yml`](../.github/workflows/cd.yml) | Push na `main`; ou manualmente (*Run workflow*) | Chama o CI, publica as imagens no GHCR e implanta na VPS por SSH |

```
push na main
   │
   ├─ CI ........... backend (mvnw verify) │ frontend (lint + tsc)
   │                 imagens (build)       │ compose (config)
   │
   ├─ imagens ...... ghcr.io/<dono>/prontudigital-backend:sha-abc1234 + :latest
   │                 ghcr.io/<dono>/prontudigital-frontend:sha-abc1234 + :latest
   │
   └─ implantar .... ssh → git checkout <sha> → scripts/deploy.sh sha-abc1234
                            ├─ backup do banco e dos anexos
                            ├─ compose pull + up -d --no-build
                            ├─ espera ficar healthy
                            ├─ GET https://<dominio>/
                            └─ falhou? volta para a tag anterior
```

Esta página cobre a **configuração** e a **operação**. Para a leitura
comentada dos dois arquivos — o que cada passo faz e por que aquele comando —
veja [`GITHUB_ACTIONS.md`](./GITHUB_ACTIONS.md).

O **gateway** (Spring Cloud Gateway) ficou de fora de propósito: foi substituído
pelo Caddy e não está em nenhum compose desde então. Se voltar a ser implantado,
entra na matriz dos dois workflows.

---

## 2. Configuração do repositório (uma vez)

`Settings > Secrets and variables > Actions`.

### Variables (valores não secretos)

| Nome | Exemplo | Para quê |
|---|---|---|
| `DOMINIO` | `prontudigital.com.br` | Vira `NEXT_PUBLIC_BASE_URL` no build do frontend e alvo do teste de fumaça. **Obrigatória** — o workflow falha cedo se faltar |
| `VPS_PORT` | `22` | Porta SSH. Opcional (padrão `22`) |
| `VPS_CAMINHO` | `/opt/prontudigital` | Onde o repositório está clonado na VPS. Opcional |

> `DOMINIO` é congelada no bundle do frontend em tempo de build
> (`NEXT_PUBLIC_*`). Trocar o domínio exige **rodar o CD de novo**, não só
> mudar o `.env` da VPS.

### Secrets

| Nome | Como obter |
|---|---|
| `VPS_HOST` | IP ou hostname da VPS |
| `VPS_USER` | Usuário de deploy (não root, membro do grupo `docker`) |
| `VPS_SSH_KEY` | Chave **privada** do par criado em §3.1, conteúdo inteiro do arquivo |
| `VPS_KNOWN_HOSTS` | Saída de `ssh-keyscan -p 22 <ip-da-vps>` (rode de uma máquina em que você confia na rede) |

Não existe secret de registro: o job usa o `GITHUB_TOKEN` da própria execução
para publicar e para autenticar a VPS no GHCR. Ele expira quando o job termina,
e o último passo do workflow faz `docker logout` na VPS.

### Environment `producao`

`Settings > Environments > New environment > producao`.

Sem nenhuma regra ele só serve para o link "View deployment" aparecer no PR.
Vale ativar **Required reviewers** se quiser que todo deploy dependa de um
clique — em prontuário eletrônico, geralmente vale.

---

## 3. Preparação da VPS (uma vez)

Pressupõe a VPS já instalada conforme `DEPLOY.md` §4–§7: Docker, `.env`
preenchido, `secrets/` criados e a pilha já tendo subido pelo menos uma vez.

### 3.1 Par de chaves do deploy

Na **sua máquina**:

```bash
ssh-keygen -t ed25519 -C 'github-actions-prontudigital' -f ~/.ssh/pd_deploy -N ''
```

Na VPS, autorize a pública:

```bash
mkdir -p ~/.ssh && chmod 700 ~/.ssh
cat >> ~/.ssh/authorized_keys      # cole o conteúdo de pd_deploy.pub
chmod 600 ~/.ssh/authorized_keys
```

O conteúdo de `~/.ssh/pd_deploy` (a privada) vai para o secret `VPS_SSH_KEY`
e some da sua máquina depois.

### 3.2 A VPS precisa conseguir `git fetch`

O deploy faz `git checkout --detach <sha>` na VPS para que compose, `Caddyfile`
e scripts fiquem na mesma versão da imagem. Se o repositório for privado,
cadastre uma **deploy key** (`Settings > Deploy keys`, só leitura) com a chave
pública da VPS:

```bash
ssh-keygen -t ed25519 -C "vps-prontudigital" -f ~/.ssh/id_ed25519 -N ''
cat ~/.ssh/id_ed25519.pub        # cole em Deploy keys, sem "Allow write access"
ssh -T git@github.com            # confirma
```

### 3.3 O usuário de deploy roda Docker sem sudo

```bash
sudo usermod -aG docker "$USER"   # precisa sair e entrar de novo na sessão
docker ps                         # tem de funcionar sem sudo
```

O passo do backup escreve em `/var/backups/prontudigital` (`DEPLOY.md` §9) —
garanta que esse diretório exista e pertença ao usuário de deploy, senão o
primeiro deploy automático aborta no backup (o que é o comportamento desejado,
mas melhor descobrir agora):

```bash
sudo mkdir -p /var/backups/prontudigital
sudo chown "$USER" /var/backups/prontudigital
```

### 3.4 Migrar a pilha que está no ar para as imagens do GHCR

Da primeira vez, a VPS ainda roda imagens construídas localmente
(`prontudigital/backend:latest`). Rode o CD uma vez (push na `main` ou *Run
workflow*): ele publica as imagens, grava `IMAGE_TAG` no `.env` e recria os
containers a partir do registro. A partir daí, todo comando manual de compose
em produção usa **os dois arquivos**:

```bash
cd /opt/prontudigital/docker/prod
docker compose -f docker-compose.prod.yml -f docker-compose.ghcr.yml ps
```

Compose v2.24 ou mais novo (o `!reset` da sobreposição depende disso):
`docker compose version`.

---

## 4. Como o deploy acontece

Quem faz o trabalho é [`docker/prod/scripts/deploy.sh`](../docker/prod/scripts/deploy.sh),
que vive no repositório — e portanto na VPS. O workflow só o invoca por SSH.
Isso é proposital: **o mesmo deploy pode ser feito à mão**, sem GitHub, com o
mesmo script e o mesmo resultado.

```bash
cd /opt/prontudigital/docker/prod/scripts
./deploy.sh sha-3d0989f
```

Na ordem:

1. **Backup** do banco e dos anexos (`backup.sh`). Falhou, o deploy aborta —
   migration do Flyway não tem rollback, e voltar a imagem não volta o esquema.
   Escape: `PULAR_BACKUP=1 ./deploy.sh <tag>`.
2. Grava `IMAGE_TAG=<tag>` no `.env`. É onde fica registrado o que está no ar.
3. `compose pull` + `up -d --no-build --remove-orphans`.
4. Espera o backend ficar `healthy` (o healthcheck é uma requisição real a
   `/actuator/health`) e frontend e Caddy continuarem `running`. Limite:
   240 s — `ESPERA_MAX` muda isso.
5. **Teste de fumaça** em `https://$DOMINIO/`, pela porta da frente: prova DNS,
   TLS do Caddy e o proxy até o frontend. (`/actuator` não entra: o `Caddyfile`
   não o expõe.)
6. Qualquer falha de 3 a 5 → imprime `ps` e os logs do backend, restaura a
   `IMAGE_TAG` anterior, sobe de novo e termina com erro.
7. Sucesso → `docker image prune -f`.

### Voltar atrás

*Actions > CD > Run workflow*, campo **tag** = a versão anterior
(`sha-abc1234`). Com a tag preenchida o workflow pula CI e build e só implanta.
Equivalente na VPS: `./deploy.sh sha-abc1234`.

As tags publicadas estão em `https://github.com/<dono>/prontudigital/pkgs/container/prontudigital-backend`.

> Se a versão que você está desfazendo trouxe migration, voltar a imagem **não
> volta o banco**. Aí é restauração: `./scripts/restore.sh --real <carimbo>`
> (`DEPLOY.md` §10.5).

---

## 5. Pacotes no GHCR

Os pacotes nascem **privados**, herdando a visibilidade do repositório. Funciona
assim mesmo — o workflow autentica a VPS antes do `pull`. Se preferir torná-los
públicos (dispensa o login e simplifica operação manual), é em
`Package settings > Change visibility`; nada de sensível vai na imagem, mas ela
expõe o código compilado.

Vale configurar retenção (`Package settings > Manage versions`) ou uma ação de
limpeza: cada push na `main` cria uma versão nova, e nenhuma é apagada sozinha.

---

## 6. Quando algo dá errado

| Sintoma no Actions | Causa provável |
|---|---|
| `Host key verification failed` | `VPS_KNOWN_HOSTS` vazio, com host errado ou porta diferente da usada no `ssh-keyscan` |
| `Permission denied (publickey)` | A pública do par de §3.1 não está no `authorized_keys` do usuário certo |
| `pull das imagens ... falhou` | Pacote privado e `docker login` não aconteceu, ou a tag não existe (confira o nome em *Packages*) |
| `backup falhou — deploy abortado` | `/var/backups/prontudigital` sem permissão, ou disco cheio |
| `backend subiu mas ficou unhealthy` | Quase sempre Flyway ou banco: `docker compose ... logs backend` na VPS. A versão anterior já foi restaurada automaticamente |
| `https://dominio/ não respondeu 2xx` | Caddy sem certificado (DNS/porta 80) ou frontend em crash-loop |
| Frontend no ar chamando `localhost:9090` | A variable `DOMINIO` estava errada **no momento do build**. Corrija e rode o CD de novo — mudar o `.env` da VPS não resolve |

---

## 7. O que ficou de fora

Deliberado, para não inflar o primeiro arranjo. Em ordem de proveito:

- **Scan de vulnerabilidade da imagem** (Trivy/Grype) antes do push — é
  prontuário de saúde; deve ser o próximo item.
- **Ambiente de homologação**. Hoje `main` vai direto para produção; a rede de
  proteção é o CI mais o rollback automático.
- **Testes end-to-end** contra a pilha subida (o CI só valida compose e unidades).
- **Cifrar o backup** antes de mandar para fora da máquina — pendência já
  registrada em `ANALISE_DEPLOY.md` §5, e o `COMANDO_COPIA_EXTERNA` do `.env`
  é o gancho natural.
- **Notificação de deploy** (WhatsApp/e-mail) no fim do workflow.
