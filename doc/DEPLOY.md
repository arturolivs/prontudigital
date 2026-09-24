# Deploy em produção — ProntuDigital

> **Runbook de execução.** Da VPS vazia ao sistema no ar, na ordem em que os
> passos precisam acontecer. Escrito lendo o estado atual do repositório em
> **23/09/2026** (branch `config-deploy`, a partir de `1cbeecb`).
>
> Este documento **não** decide plataforma nem repete análises — ele executa.
> O que cada documento vizinho cobre está na §0.
>
> **O que mudou desde a revisão de 15/09:** a VPS deixou de compilar. O build
> acontece no GitHub Actions, as imagens vão para o GHCR e aqui só se faz
> `pull` (`docker/prod/docker-compose.ghcr.yml` + `scripts/deploy.sh`). Todo
> passo que antes dizia `up -d --build` mudou — inclusive o **primeiro** deploy.

---

## 0. Mapa dos documentos

| Documento | Responde |
|---|---|
| **este** (`doc/DEPLOY.md`) | *Como* subir, passo a passo, e como operar depois |
| [`CICD.md`](./CICD.md) | *Como a atualização se automatiza* — os dois workflows, os secrets do repositório, o que o `deploy.sh` faz e como voltar atrás |
| [`doc/Requisitos.md`](./Requisitos.md) | Os RNF citados aqui — RNF02 (backup), RNF05/RNF06 (desempenho) |
| [`docker/prod/secrets/README.md`](../docker/prod/secrets/README.md) | Os três segredos em arquivo — o que cada um faz e como trocar |
| [`docker/prod/scripts/README.md`](../docker/prod/scripts/README.md) | Backup e restauração (RNF02) em detalhe |
| [`testes-carga/README.md`](../testes-carga/README.md) | *Se aguenta* — teste de carga do RNF05/RNF06 e os critérios de folga |

---

## 1. Bloqueadores levantados nesta análise

Os quatro itens abaixo **não impedem a pilha de subir** — impedem que o sistema
receba dado real de paciente com segurança. O 1.1 e o 1.2 já foram corrigidos
no código; os outros dois se resolvem na execução, nos passos indicados.

### 1.1 — Registro público com escolha de perfil — ✅ **corrigido em 15/09/2026**

**O problema:** `POST /api/auth/registrar` era público e o serviço aceitava a
lista de perfis **vinda do corpo da requisição**, sem validar quem pedia
(`AutenticacaoServiceImpl.java:78` — `.perfis(request.perfis())`). Um POST
anônimo com `"perfis":["ADMIN"]` devolvia 201 e entregava acesso total ao
prontuário de todos os pacientes.

**O que mudou:**

| Arquivo | Mudança |
|---|---|
| `SecurityConfig.java` | `"/api/auth/registrar"` saiu de `AUTH_POST_PUBLICOS`; passa a cair em `.anyRequest().authenticated()` |
| `AutenticacaoController.java` | `@PreAuthorize("hasRole('ADMIN')")` no método `registrar` |
| `frontend-web/lib/usuario.service.ts` | `criarUsuario` usava `axios` puro, **sem Authorization**; passou a usar uma instância com o interceptor de token — sem isso a tela *Dashboard > Usuários* quebraria com 403 |
| `AutenticacaoControllerSegurancaTest.java` (novo) | 6 testes: nega anônimo, PROFISSIONAL e PACIENTE; permite ADMIN; confirma que `/login` e `/cadastrar-paciente` seguem abertos |

O cadastro público de paciente continua em `/api/auth/cadastrar-paciente`, que
**não** aceita perfis — o `PACIENTE` é fixado no serviço
(`UsuarioServiceImpl:270-272`).

**Validação:** `mvnw test` → **377 testes, 0 falhas**; `tsc --noEmit` e
`eslint` limpos no frontend.

> A resposta para quem não é ADMIN é **403**, inclusive sem credencial nenhuma:
> a configuração não declara `authenticationEntryPoint`, então vale o
> `Http403ForbiddenEntryPoint` padrão — mesmo comportamento do resto da API.

### 1.2 — Dados de exemplo semeados por migration — ✅ **corrigido em 15/09/2026**

**O problema:** `V8__insert_dados_exemplo.sql` rodava em **todo** ambiente — o
Flyway não distingue perfil. Criava `admin` (perfil ADMIN), dois enfermeiros e
10 pacientes fictícios, todos com o mesmo hash BCrypt, cuja senha em claro é
`senha123` — valor que circulava no repositório e no histórico do Git. No
minuto em que o domínio respondesse, existiria um **ADMIN com senha pública**.

**O que mudou:** as migrations deixaram de carregar dado de aplicação. Como o
banco de produção ainda não existe, elas foram **renumeradas de `V1` a `V26`**,
sem as lacunas que havia em `V14`, `V15` e `V21`.

| Mudança | Detalhe |
|---|---|
| `V8__insert_dados_exemplo.sql` | **apagada** — os 13 usuários e o hash público saíram do repositório |
| `V2__criacao_tabela_perfis.sql` | recebeu os três `perfis` (ADMIN / PROFISSIONAL / PACIENTE) que estavam na `V8`. São dado de **referência**, não de exemplo: `UsuarioServiceImpl:270` e `:455` os buscam por nome e lançam `PerfilNaoEncontradoException` sem eles |
| `V10` (era `V11`) | saiu o `UPDATE usuarios SET acesso_ativado = TRUE` — backfill de linhas que não existem mais |
| `V18` (era `V22`) | saiu o `UPDATE agendamentos SET procedimento_id = …`, idem. O `INSERT` dos dois procedimentos ficou: sem nenhuma linha ali a tela pública de agendamento não oferece nada |
| `V24` (era `V28`) | inalterada — a linha única de `configuracao_clinica` é exigida pelo `CHECK (id = 1)` e evita tratar "configuração ausente" em todo endpoint |
| `BootstrapAdminRunner.java` (novo) | cria o **primeiro ADMIN** a partir do ambiente, no primeiro boot em que o banco não tem nenhum |
| `BootstrapAdminRunnerTest.java` (novo) | 11 testes: cria uma vez, não recria, recusa senha curta, não colide com username/e-mail existente |

**Por que o runner precisa existir:** com a `V8` fora e a `/api/auth/registrar`
fechada pela §1.1, não sobrava caminho para o primeiro acesso — o cadastro
público só produz `PACIENTE`. O runner fecha essa lacuna sem reintroduzir senha
versionada: o hash é gerado no boot, pelo mesmo `PasswordEncoder` da aplicação,
a partir de `secrets/admin.senha` (§5.1).

Ele é deliberadamente conservador — **não faz nada** quando já existe qualquer
usuário com perfil ADMIN, quando a senha tem menos de **8 caracteres** ou
quando o username/e-mail já pertence a outro usuário. Ou seja: trocar a senha
pela tela não é desfeito no próximo `restart`, e o `.env` não é uma porta dos
fundos permanente.

**Validação:** as 26 migrations aplicadas em sequência num Postgres 16 limpo,
sem erro; `usuarios` nasce com **0 linhas**; `mvnw test` → **377 testes, 0
falhas**.

> Nenhum comando de limpeza pós-boot é mais necessário. O que a §8 pede agora é
> a troca da senha inicial, não a remoção de usuários de demonstração.

### 1.3 — Logs do Docker sem rotação

Nenhum serviço de `docker/prod/docker-compose.prod.yml` declara `logging:`, e o
backend escreve tudo em `stdout`. Com o driver `json-file` padrão e sem limite,
o log cresce até encher o disco — e quem morre junto é o Postgres, no mesmo
volume. O Caddy é a exceção: já rotaciona sozinho (`Caddyfile:38-43` —
`roll_size 10mb`, `roll_keep 5`). Resolvido no passo **§3.4**, no daemon do
Docker, e não no compose: assim vale para qualquer container que subir depois.

### 1.4 — Sem WhatsApp, ninguém recupera a própria senha

Com `NOTIFICACOES_HABILITADAS=false` (o padrão recomendado para o primeiro
deploy), `POST /api/auth/recuperar-senha/solicitar` responde **503**: o código
de recuperação só trafega por WhatsApp, não há fallback por e-mail. Quem
esquecer a senha depende do ADMIN redefinir em *Dashboard > Usuários*.
Combine com a §5: a senha de `secrets/admin.senha` é a única credencial da
instalação até que outros usuários sejam criados — guarde-a num gerenciador.
Além da recuperação de senha, ficam desligados com a flag em `false`: o
lembrete de 48 h, o pedido de confirmação de 24 h, o cancelamento automático de
quem não confirma até 2 h antes e o webhook `/api/whatsapp/webhook`, que passa
a responder 404. Nada disso exige rebuild para voltar: basta trocar a flag,
preencher as quatro `WHATSAPP_*` e recriar o backend. A lista completa está
comentada no próprio `docker/prod/.env.prod.example`.

---

## 2. Pré-requisitos

| Item | Valor esperado | Por quê |
|---|---|---|
| VPS | 4 GB RAM, 2 vCPU, 40 GB SSD, região São Paulo | Os limites declarados nos containers somam ~2 GB (1 G backend + 512 M Postgres + 384 M frontend + 128 M Caddy); o resto é folga para o SO, o `pg_dump` do backup e picos |
| SO | **Ubuntu 22.04 ou 24.04 LTS** | Os passos da §3 usam `apt`, `ufw` e o cron do host. AlmaLinux e Rocky rodam a pilha igualmente bem, mas trocam `ufw` por `firewalld` e trazem **SELinux em `enforcing`**: os bind mounts (`Caddyfile`, `secrets/`) passam a exigir rótulo `:z`/`:Z`, e o sintoma quando falta é *permission denied* com o arquivo visivelmente presente no host |
| Domínio | registrado, com acesso ao painel de DNS | O Caddy emite o certificado sozinho, mas exige DNS resolvendo |
| Acesso | SSH com chave; usuário **não-root**, com `sudo` e no grupo `docker` | É o mesmo usuário que o CD usa por SSH (`CICD.md` §3) |
| Git | acesso de **leitura** ao repositório a partir da VPS | A VPS usa do repo o compose, o `Caddyfile` e os scripts — **não** o código-fonte |
| GHCR | `docker login ghcr.io` na VPS, ou o CD fazendo isso por ela | As imagens nascem privadas, herdando a visibilidade do repositório |

**A VPS não compila nada.** `up --build` roda Maven e `next build` na máquina
de produção, consome bem mais memória que o runtime inteiro e derruba uma VPS
pequena por OOM — foi essa medição que tirou o build daqui. O build vive
no GitHub Actions; aqui só há `pull`. O caminho de emergência está na §6.4, com
as ressalvas.

**Antes de qualquer coisa:** aponte o registro **A** do domínio para o IP da
VPS e confirme a propagação. Sem isso o Let's Encrypt falha e o Caddy fica em
loop de tentativa.

```bash
dig +short SEU-DOMINIO.com.br     # precisa devolver o IP da VPS
```

---

## 3. Preparo do servidor

### 3.1 Docker

```bash
sudo apt update && sudo apt upgrade -y
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
newgrp docker            # ou refaça o login
docker compose version   # precisa ser v2.24 ou mais novo
```

A versão importa: a sobreposição `docker-compose.ghcr.yml` usa `build: !reset
null` para **apagar** o bloco `build:` herdado do arquivo base, e `!reset` só
existe a partir do Compose **v2.24**. Em versão anterior o Compose continua
enxergando um contexto de build e tenta compilar na VPS quando a imagem falta —
exatamente o que se quer evitar.

### 3.2 Swap — recomendado nos 4 GB

Com o build fora da VPS, o swap deixou de ser salva-vidas de deploy, mas segue
valendo como colchão: o `pg_dump` do backup diário, o `restore.sh` e picos da
JVM acontecem com os quatro containers no ar, e 4 GB não dão muita margem. 2 GB
de swap evitam que o OOM killer escolha o Postgres.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 3.3 Fuso horário do host

```bash
sudo timedatectl set-timezone America/Sao_Paulo
```

Os containers já recebem `TZ=America/Sao_Paulo` — e o Postgres também `PGTZ`,
porque as colunas são `TIMESTAMP` sem fuso e vários `DEFAULT CURRENT_TIMESTAMP`
gravam pelo relógio do banco. Mas o cron do backup, os logs do host e
`docker logs -t` usam o relógio da máquina.

### 3.4 Rotação de log do Docker — resolve a §1.3

```bash
sudo tee /etc/docker/daemon.json >/dev/null <<'JSON'
{
  "log-driver": "local",
  "log-opts": { "max-size": "10m", "max-file": "5" }
}
JSON
sudo systemctl restart docker
```

O driver `local` comprime e rotaciona; `docker logs` continua funcionando
igual. Teto: 50 MB por container. Vale para tudo que subir depois — faça
**antes** do primeiro `up`.

### 3.5 Firewall

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow 80,443/tcp
sudo ufw allow 443/udp        # HTTP/3, publicado pelo Caddy
sudo ufw enable
```

O Postgres **não** precisa de regra: `docker-compose.prod.yml` usa `expose`
(não `ports`) e a rede `interna` é `internal: true` — o banco não é alcançável
de fora do Docker. O backend também fica só em `expose`; ele participa da rede
`externa` apenas para ter DNS e saída, não para receber conexão.

> **A saída para a internet precisa continuar aberta.** É por ela que o Caddy
> fala com o Let's Encrypt, o Docker puxa as imagens do GHCR e o backend
> alcança a WhatsApp Cloud API.

### 3.6 SSH

```bash
sudo sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
sudo sed -i 's/^#\?PermitRootLogin.*/PermitRootLogin prohibit-password/' /etc/ssh/sshd_config
sudo systemctl restart ssh
sudo apt install -y fail2ban && sudo systemctl enable --now fail2ban
```

> Confirme que sua chave funciona **em outra sessão** antes de fechar a atual.

### 3.7 Diretórios e dono

Dois diretórios precisam pertencer ao **usuário de deploy** (o mesmo que o CD
usa por SSH), e não ao root:

```bash
sudo mkdir -p /opt/prontudigital && sudo chown "$USER" /opt/prontudigital
sudo mkdir -p /var/backups/prontudigital && sudo chown "$USER" /var/backups/prontudigital
```

O de backup é o que costuma morder: o `deploy.sh` chama o `backup.sh` como o
usuário de deploy **antes** de cada atualização, e um backup que falha aborta o
deploy de propósito (migration do Flyway não tem rollback). Se o diretório for
do root, todo deploy automático para no primeiro passo. Ver §9.

---

## 4. Código no servidor

```bash
git clone <URL-DO-REPO> /opt/prontudigital
cd /opt/prontudigital
git checkout main          # ou a tag/branch que for ao ar
```

O que a VPS usa deste clone é o `docker-compose.prod.yml`, a sobreposição do
GHCR, o `Caddyfile` e os três scripts de `docker/prod/scripts/`. O código
compilado vem pronto dentro da imagem.

A cópia da VPS **não é ambiente de trabalho de ninguém**: a cada deploy o
workflow roda `git fetch --prune origin && git checkout --detach <sha>` aqui,
para que compose, `Caddyfile` e scripts fiquem exatamente na versão da imagem
que subiu. Arquivo editado solto desaparece no próximo deploy — ajuste vai no
repositório, com merge antes.

> Repositório privado? A VPS precisa conseguir `git fetch` sozinha: cadastre
> uma **deploy key** de leitura (`CICD.md` §3.2). Sem isso o passo de
> sincronização do CD falha antes de chegar ao `deploy.sh`.

Nada além do `.env` e dos `secrets/` (§5) é criado no servidor — e nenhum dos
dois é versionado.

---

## 5. Configuração — `.env` e `secrets/`

A configuração de produção está em dois lugares, de propósito:

- **`docker/prod/.env`** — o que não é segredo (domínio, nome do banco,
  expiração dos tokens, flags). Vira variável de ambiente nos containers.
- **`docker/prod/secrets/`** — um arquivo por segredo. Variável de ambiente
  aparece inteira em `docker inspect` e em `/proc/1/environ`, legível por
  qualquer um do grupo `docker`; arquivo montado em `/run/secrets` não.

### 5.1 Segredos

Gere **na VPS**, não reaproveite de lugar nenhum. O `-n` implícito do
`printf '%s'` é o que importa: com quebra de linha no fim, a senha vira
`"senha\n"` e o login falha.

```bash
cd /opt/prontudigital/docker/prod
mkdir -p secrets && chmod 700 secrets

printf '%s' "$(openssl rand -base64 24)" > secrets/db.password   # senha do banco
printf '%s' "$(openssl rand -base64 64)" > secrets/jwt.secret    # HS512 exige chave longa
printf '%s' "$(openssl rand -base64 18)" > secrets/admin.senha   # 1º acesso (mínimo 8 caracteres)

chmod 600 secrets/*
cat secrets/admin.senha    # anote num gerenciador antes de seguir
```

Detalhes de cada um e o que fazer depois do primeiro acesso:
[`docker/prod/secrets/README.md`](../docker/prod/secrets/README.md). O Compose
recusa subir se faltar algum:

```
error while creating mount source path ... secrets/jwt.secret: no such file or directory
```

O `deploy.sh` confere a existência dos três antes de tocar em qualquer coisa e
aborta com `segredo ausente: secrets/...` — o CD falha ali, sem mexer no que
está no ar.

### 5.2 `.env`

```bash
cp .env.prod.example .env
chmod 600 .env
```

| Variável | Obrigatória | Observação |
|---|---|---|
| `DOMINIO` | ✅ | Sem `https://` e sem barra. Usado pelo Caddy, no link de confirmação e no teste de fumaça do deploy |
| `ACME_EMAIL` | ✅ | E-mail da conta Let's Encrypt — recebe o aviso de certificado a vencer. **O Caddy não sobe sem ele** |
| `POSTGRES_DB` / `POSTGRES_USER` | ✅ | Pode manter os valores do exemplo |
| `JWT_ACCESS_EXPIRATION_MS` | ✅ | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | ✅ | `604800000` (7 dias) — precisa bater com o `maxAge` do cookie `__pd_rt` |
| `ADMIN_USERNAME` | ✅ no 1º deploy | Login do primeiro ADMIN. Sem ele **ninguém consegue entrar**: o banco nasce sem usuário nenhum (§1.2). Sai do `.env` depois do primeiro acesso (§8.2) |
| `ADMIN_NOME` / `ADMIN_EMAIL` / `ADMIN_TELEFONE` | ⬜ | Só aparência e contato; o e-mail, se informado, precisa ser único |
| `NOTIFICACOES_HABILITADAS` | ✅ | **`false` no primeiro deploy.** Ver §1.4 |
| `WHATSAPP_*` (4) | ⬜ | Deixe vazias enquanto a flag acima for `false` |
| `COMANDO_COPIA_EXTERNA` | ⚠️ | Vazia = backup mora no disco que deveria proteger. Ver §9 |
| `RETENCAO_DIAS` / `RETENCAO_SEMANAIS` | ⬜ | Padrão 14 / 8 |
| `IMAGE_TAG` | 🤖 | **Não preencha à mão.** Quem grava é o `scripts/deploy.sh` a cada deploy; é por esta linha que se descobre o que está no ar e para onde voltar |

A senha do banco, o segredo do JWT e a senha do primeiro ADMIN **não estão
nesta tabela porque não são variáveis de ambiente** — são os três arquivos da
§5.1.

> **O `DOMINIO` do `.env` não é o que chega ao frontend.**
> `NEXT_PUBLIC_BASE_URL` é congelada no bundle durante o build, que hoje
> acontece no Actions a partir da *variable* `DOMINIO` do repositório
> (`CICD.md` §2). As duas precisam combinar, e trocar de domínio exige rodar o
> CD de novo (§10.4).

Valide antes de subir. As obrigatórias da tabela estão declaradas como
`${VAR:?mensagem}` no bloco `x-env-obrigatorias` do `docker-compose.prod.yml`,
então o comando abaixo **sai com erro** se alguma faltar ou estiver vazia (com
`${VAR}` puro o Compose só emitiria um warning e substituiria por string
vazia — o `up` seguiria adiante com um `ACME_EMAIL` em branco):

```bash
docker compose -f docker-compose.prod.yml config >/dev/null && echo OK

# Faltando, a saída é esta — e vale igual para o `up`:
# error while interpolating x-env-obrigatorias.[]: required variable
#   ACME_EMAIL is missing a value: defina ACME_EMAIL no .env (...)
```

Duas coisas que a validação **não** pega, confira à mão:

```bash
# 1. ADMIN_USERNAME fora das obrigatórias de propósito: sai do .env depois do
#    primeiro acesso (§8.2). No PRIMEIRO deploy ele e a senha precisam estar
#    preenchidos, senão ninguém entra (§1.2)
grep -E '^ADMIN_USERNAME=.+' .env && [ -s secrets/admin.senha ] && echo 'ADMIN ok'

# 2. nenhum segredo terminando em quebra de linha (viraria parte da senha)
for f in secrets/db.password secrets/jwt.secret secrets/admin.senha; do
  [ -s "$f" ] && [ -z "$(tail -c1 "$f")" ] && echo "$f termina em \\n — refaça com printf '%s'"
done
```

---

## 6. Primeira subida

A pilha sobe a partir de imagens já publicadas no GHCR. Isso impõe uma ordem: o
CD precisa ter construído e publicado ao menos uma vez antes de a VPS conseguir
dar `pull`.

### 6.1 Caminho recomendado — deixar o CD subir

Com a §3 a §5 prontas, configure o repositório conforme [`CICD.md`](./CICD.md)
§2 e §3 (secrets `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, `VPS_KNOWN_HOSTS`;
variable `DOMINIO`) e dispare o workflow: um push na `main` ou
*Actions > CD > Run workflow*.

Ele faz, em uma tacada: CI → build das duas imagens → publicação no GHCR →
`docker login` na VPS → `git checkout --detach <sha>` → `scripts/deploy.sh
sha-<commit>` → `docker logout`. No primeiro deploy o `deploy.sh` pula o backup
sozinho (não há pilha no ar para copiar) e segue direto para `pull` + `up -d`.

### 6.2 Caminho manual — a mesma coisa, a partir da VPS

Legítimo, e é o que se usa quando o Actions está fora do ar. Exige que a tag já
exista no registro:

```bash
# uma vez: autenticar no GHCR (o pacote nasce privado, como o repositório).
# Use um PAT clássico com escopo read:packages.
echo '<SEU_PAT>' | docker login ghcr.io -u <SEU_USUARIO> --password-stdin

cd /opt/prontudigital
git fetch origin && git checkout --detach <sha-do-commit>
docker/prod/scripts/deploy.sh sha-<abc1234>
```

As tags publicadas estão em
`https://github.com/<dono>/prontudigital/pkgs/container/prontudigital-backend`.
Variáveis que o script aceita: `PULAR_BACKUP=1` (não copia antes de subir),
`ESPERA_MAX=240` (segundos de espera pela saúde), `REGISTRO=` (outra origem das
imagens).

### 6.3 O que esperar, em ordem

1. **Backup** do banco e dos anexos — pulado no primeiro deploy, obrigatório
   nos seguintes.
2. `IMAGE_TAG` gravada no `.env` e `pull` das duas imagens.
3. **`postgres`** sobe e fica `healthy` (~15 s).
4. **`backend`** inicia, o **Flyway aplica as 26 migrations** (`V1` a `V26`) e
   a JVM sobe. O healthcheck é uma requisição real a `/actuator/health`
   (`backend/docker-healthcheck.sh`), com `start_period: 60s`. Logo após o
   boot, procure esta linha — é o ADMIN inicial sendo criado (§1.2):

   ```
   INFO  c.p.b.a.config.BootstrapAdminRunner : ADMIN inicial 'seu.usuario' criado.
   ```

   Se em vez dela vier `Nenhum ADMIN no banco e username/senha do ADMIN inicial
   nao informados`, falta `ADMIN_USERNAME` no `.env` ou conteúdo em
   `secrets/admin.senha`; preencha e recrie o backend (§8.2). Se vier `A senha
   do ADMIN inicial tem menos de 8 caracteres`, gere outra.
5. **`frontend`** só inicia depois de o backend estar `healthy` (`depends_on`).
6. **`caddy`** sobe, pede o certificado ao Let's Encrypt e passa a servir 443.
7. O `deploy.sh` espera tudo ficar saudável (limite de 240 s), faz um
   `GET https://$DOMINIO/` pela porta da frente — o que prova DNS, TLS e proxy
   de uma vez — e só então declara sucesso. Falhou em qualquer ponto: ele
   imprime `ps` e os logs do backend, **restaura a `IMAGE_TAG` anterior** e sai
   com erro.

Acompanhar em outra sessão:

```bash
cd /opt/prontudigital/docker/prod
docker compose -f docker-compose.prod.yml logs -f
```

### 6.4 Emergência — construir na própria VPS

Só quando o Actions estiver indisponível e a correção não puder esperar.
Compila Maven e roda `next build` na máquina de produção, consumindo mais
memória que o runtime inteiro — é assim que se derruba a VPS por OOM:

```bash
cd /opt/prontudigital && git pull && cd docker/prod
docker compose -f docker-compose.prod.yml up -d --build
docker image prune -f        # libera as imagens antigas
```

Repare que aqui **não** entra a sobreposição do GHCR: é o único comando desta
página que usa o arquivo base sozinho para criar containers. Depois disso a
pilha roda imagens locais (`prontudigital/backend:latest`); o próximo deploy
pelo CD a traz de volta para as imagens do registro.

---

## 7. Verificação

Rode tudo antes de considerar o deploy feito. Defina os dois atalhos primeiro —
a distinção entre eles importa:

```bash
cd /opt/prontudigital/docker/prod
COMPOSE="docker compose -f docker-compose.prod.yml"
COMPOSE_GHCR="$COMPOSE -f docker-compose.ghcr.yml"
```

- **Inspecionar** (`ps`, `logs`, `exec`, `restart`) → `$COMPOSE`. O arquivo
  base sozinho já encontra os containers do projeto.
- **Criar ou recriar** (`pull`, `up`) → `$COMPOSE_GHCR`. Sem a sobreposição o
  Compose enxerga o bloco `build:` e tenta compilar aqui.

```bash
# 7.1 — todos os containers de pé, backend e postgres "healthy"
$COMPOSE ps

# 7.2 — as 26 migrations aplicadas, nenhuma com success = false
$COMPOSE exec postgres \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c 'SELECT count(*) FILTER (WHERE success)     AS aplicadas,
             count(*) FILTER (WHERE NOT success) AS falhas
        FROM flyway_schema_history;'

# 7.3 — o banco tem exatamente UM usuário, o ADMIN inicial, e mais nenhum
$COMPOSE exec postgres \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c 'SELECT u.username, p.nome FROM usuarios u
        JOIN usuario_perfis up ON up.usuario_id = u.id
        JOIN perfis p ON p.id = up.perfil_id;'

# 7.4 — TLS válido e emitido pelo Let's Encrypt
curl -sSI https://SEU-DOMINIO | head -1

# 7.5 — API respondendo através do Caddy (não 404 do Next)
curl -s https://SEU-DOMINIO/api/configuracao | head -c 200

# 7.6 — a versão no ar é a que você acha que é
grep '^IMAGE_TAG=' .env
docker inspect prontudigital-backend --format '{{.Config.Image}}'

# 7.7 — diretório de anexos gravável pelo usuário do backend
$COMPOSE exec backend \
  sh -c 'touch /dados/anexos/.escrita && rm /dados/anexos/.escrita' \
  && echo "anexos graváveis"

# 7.8 — saída para a internet (a WhatsApp Cloud API depende dela)
$COMPOSE exec backend \
  bash -c 'echo > /dev/tcp/graph.facebook.com/443' && echo "egress ok"

# 7.9 — nenhum segredo no ambiente dos containers (devem sair vazios)
docker inspect prontudigital-backend prontudigital-db \
  --format '{{range .Config.Env}}{{println .}}{{end}}' \
  | grep -iE 'password=|secret=|senha=' | grep -v '=$'

# 7.10 — cabeçalhos de segurança chegando ao navegador
curl -sI https://SEU-DOMINIO/login \
  | grep -iE 'content-security-policy|permissions-policy|strict-transport'
```

> **7.7 falhou com `Permission denied`?** O volume `prontudigital_anexos_prod`
> foi criado antes desta correção, como `root:root`. O Docker só copia dono e
> permissões da imagem quando **cria** o volume — reconstruir a imagem não
> conserta um volume que já existe. Ajuste uma vez:
>
> ```bash
> $COMPOSE exec -u root backend chown -R prontu:prontu /dados
> ```

Pela interface:

- `https://SEU-DOMINIO` carrega a tela de login com o nome da clínica;
- o agendamento público lista procedimentos (vieram da `V18`: Podiatria e
  Tratamento de Feridas);
- `https://SEU-DOMINIO/swagger-ui.html` **deve dar 404** — o Swagger está
  desabilitado em produção (`application-prod.yaml:183-187`) e o `Caddyfile`
  sequer roteia `/swagger-ui/*`.

---

## 8. Endurecimento pós-primeiro-boot — **obrigatório, mesmo dia**

Não há usuário de demonstração para apagar (§1.2): o banco nasce com três
`perfis`, dois `procedimentos`, uma linha de `configuracao_clinica` e **nenhum
usuário** além do ADMIN que o próprio backend criou. O que falta é tirar a
senha inicial de circulação.

**8.1 — Entre com `ADMIN_USERNAME` e a senha de `secrets/admin.senha`** e
troque a senha em *Dashboard > Meu perfil*. Guarde a nova num gerenciador: sem
WhatsApp não há autoatendimento de recuperação (§1.4), e este é o único ADMIN
da instalação.

**8.2 — Esvazie `secrets/admin.senha`** — esvazie, não apague: o Compose exige
que o arquivo exista, e o `deploy.sh` aborta se ele sumir.

```bash
cd /opt/prontudigital/docker/prod
: > secrets/admin.senha
docker compose -f docker-compose.prod.yml -f docker-compose.ghcr.yml up -d backend
```

A sobreposição é obrigatória neste comando: `up` recria o container e, sem ela,
o Compose tentaria construir a imagem aqui. O runner só age quando o banco não
tem nenhum ADMIN, então a partir daqui ele não faz mais nada em boot algum —
esvaziar não muda comportamento, tira a senha do disco. O `up -d` (e não
`restart`) é o que faz o container novo deixar de enxergar o conteúdo antigo.

Aproveite e tire `ADMIN_USERNAME` do `.env`: ele ficou fora das obrigatórias do
compose justamente para poder sair depois do primeiro acesso.

**8.3 — Crie os usuários reais** em *Dashboard > Usuários*: o ADMIN da clínica
(pessoa de verdade, com telefone correto) e os profissionais. Saia, entre com o
ADMIN real e confirme que ele enxerga usuários, agenda e relatórios.

> Se quiser deixar de usar o usuário criado pelo `.env`, **desative-o** pela
> tela em vez de apagar — um ADMIN sem nenhum registro associado pode ser
> removido, mas desativar evita a checagem de `RESTRICT` em `agendamentos`.

**8.4 — Confirme que o registro público está fechado** (§1.1) — a correção já
está no código; isto aqui verifica que a imagem em produção a tem:

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST https://SEU-DOMINIO/api/auth/registrar \
  -H 'Content-Type: application/json' \
  -d '{"username":"teste_bloqueio","senha":"Teste12345","nomeCompleto":"t","email":"t@t.com","perfis":["ADMIN"]}'
```

Esperado: **403**. Se vier **201**, a imagem em produção é anterior à correção
de 15/09/2026 — apague o usuário criado e implante uma tag mais nova antes de
divulgar o endereço para a clínica.

**8.5 — Preencha os dados da clínica** em *Dashboard > Configuração*: nome,
CNPJ, endereço, logo e rodapé. Eles entram no cabeçalho dos PDFs (atestados,
prescrições) e na tela de login. Sem isso os documentos saem com o nome
genérico "ProntuDigital" da `V24`.

---

## 9. Backup — o deploy não está pronto sem isso

Detalhes e justificativas em
[`docker/prod/scripts/README.md`](../docker/prod/scripts/README.md). O mínimo:

```bash
cd /opt/prontudigital/docker/prod
chmod +x scripts/*.sh
```

O diretório de destino já foi criado **no nome do usuário de deploy** na §3.7, e
isso não é detalhe: o `deploy.sh` roda o `backup.sh` como esse usuário antes de
cada atualização. Se o `cron` do **root** rodar o mesmo script, ele cria
`diarios/` pertencendo ao root e, a partir daí, todo deploy automático falha no
primeiro passo — com o backup abortado, o deploy aborta junto, por projeto.
Agende no cron do próprio usuário de deploy:

```bash
crontab -e     # sem sudo
```

```cron
0 3 * * * /opt/prontudigital/docker/prod/scripts/backup.sh >> /var/log/prontudigital-backup.log 2>&1
```

```bash
# o arquivo de log também precisa ser do usuário, não do root
sudo touch /var/log/prontudigital-backup.log
sudo chown "$USER" /var/log/prontudigital-backup.log
```

Defina o destino externo no `.env` — **o banco e os anexos são copiados
juntos**, porque os arquivos do prontuário não ficam no banco:

```dotenv
COMANDO_COPIA_EXTERNA="rclone sync /var/backups/prontudigital remoto:prontudigital"
```

Rode **uma vez à mão** para validar antes de confiar no cron:

```bash
./scripts/backup.sh
ls -la /var/backups/prontudigital/diarios/
```

O script confere o que gerou: `pg_restore --list` no dump, `tar tzf` no arquivo
de anexos, `SHA256SUMS` e um `manifesto.txt` por execução. Qualquer falha
aborta com código de saída diferente de zero — o gancho natural para um monitor
(Healthchecks.io, Uptime Kuma) no fim da linha do cron.

E agende na sua agenda pessoal, mensalmente:

```bash
./scripts/restore.sh --teste /var/backups/prontudigital/diarios/<carimbo>
```

> O dump sai **em claro** e contém CPF e dado clínico. Cifrar antes do envio
> externo é pendência conhecida (§11) — até lá, o
> destino precisa ser cifrado e de acesso restrito.

---

## 10. Operação

Valem os atalhos da §7: `$COMPOSE` para inspecionar, `$COMPOSE_GHCR` para
recriar.

### 10.1 Logs

```bash
$COMPOSE logs -f backend
$COMPOSE logs --since 1h --tail 200 backend
$COMPOSE exec caddy tail -f /var/log/caddy/access.log
```

Nível do backend em produção: `root=WARN`, `com.prontudigital=INFO`
(`application-prod.yaml:88-92`). Para depurar sem rebuild, defina
`LOG_LEVEL_WHATSAPP=DEBUG` ou `LOG_LEVEL_SQL=DEBUG` no `.env` e **recrie** o
container (`$COMPOSE_GHCR up -d backend`) — variável de ambiente não muda com
`restart`.

### 10.2 Atualizar versão

**Caminho normal: não faça nada.** Push na `main` dispara o CD, que testa,
constrói as imagens no GitHub Actions, publica no GHCR e implanta aqui — com
backup antes, espera de saúde, teste de fumaça e rollback automático se algo
falhar. Ver [`CICD.md`](./CICD.md).

À mão, com uma imagem já publicada (é o mesmo script que o CD executa):

```bash
cd /opt/prontudigital
git fetch origin && git checkout --detach <sha-do-commit>
docker/prod/scripts/deploy.sh sha-<abc1234>
```

Manter o código da VPS na mesma revisão da imagem não é zelo: o `Caddyfile` e o
compose que estão no disco é que sobem, e um backend novo com um `Caddyfile`
velho é uma combinação que ninguém testou.

Migrations novas são aplicadas sozinhas no boot do backend, e **o Flyway não
faz rollback** — por isso o backup vem antes, e por isso uma falha de backup
aborta o deploy. O `deploy.sh` cuida disso; quem passa `PULAR_BACKUP=1` está
assumindo o risco à mão.

### 10.3 Reiniciar um serviço só

```bash
$COMPOSE restart backend
```

Mudança em variável de ambiente exige recriar o container
(`$COMPOSE_GHCR up -d backend`), não `restart`. Qualquer `NEXT_PUBLIC_*` está
congelada no bundle e exige **build novo**, que só acontece no Actions (§10.4).

### 10.4 Trocar de domínio

São três lugares, nesta ordem:

1. **DNS** — registro A do novo domínio apontando para a VPS, propagado.
2. **GitHub** — `Settings > Secrets and variables > Actions > Variables`,
   variable `DOMINIO`. É dela que sai o `NEXT_PUBLIC_BASE_URL` do build.
3. **VPS** — `DOMINIO` no `docker/prod/.env`, que o Caddy lê para pedir o
   certificado e o `deploy.sh` usa no teste de fumaça.

Depois, **rode o CD** (*Actions > CD > Run workflow*, campo `tag` vazio). Só um
build novo troca a URL embutida no frontend; mudar apenas o `.env` deixa a tela
chamando o domínio antigo. O Caddy pede o certificado do nome novo sozinho.

### 10.5 Voltar atrás

```bash
# Código: Actions > CD > Run workflow, campo `tag` = versão anterior.
# Com a tag preenchida o workflow pula CI e build e só implanta.
# Na VPS, o equivalente:
docker/prod/scripts/deploy.sh sha-<versao-anterior>

# Dados: restauração destrutiva, exige digitar RESTAURAR
./scripts/restore.sh --real /var/backups/prontudigital/diarios/<carimbo>
```

A versão anterior fica registrada no `.env` (`IMAGE_TAG`) até o deploy seguinte
sobrescrevê-la — e é ela que o `deploy.sh` restaura sozinho quando um deploy
falha no meio do caminho.

> Se a versão que você está desfazendo trouxe migration, **voltar a imagem não
> volta o banco**. Aí é restauração, não rollback.

---

## 11. Checklist de corte

Marque tudo antes de entregar o endereço para a clínica.

**Antes de expor**
- [ ] DNS apontando e propagado
- [ ] `docker compose version` ≥ v2.24 (§3.1)
- [ ] `daemon.json` com rotação de log (§3.4)
- [ ] `ufw` ativo, SSH sem senha, `fail2ban` rodando
- [ ] `/opt/prontudigital` e `/var/backups/prontudigital` pertencendo ao usuário de deploy (§3.7)
- [ ] `.env` com `chmod 600` e os três arquivos em `secrets/`, gerados na VPS
- [ ] `NOTIFICACOES_HABILITADAS=false` (ou as 4 variáveis do WhatsApp válidas)
- [ ] Secrets e variables do CD configurados no GitHub (`CICD.md` §2)
- [ ] Deploy key de leitura cadastrada, se o repositório for privado (`CICD.md` §3.2)

**Depois de subir**
- [ ] `docker compose ps` com backend e postgres `healthy`
- [ ] 26 migrations aplicadas, nenhuma com `success = false`
- [ ] HTTPS válido; Swagger devolvendo 404
- [ ] `IMAGE_TAG` do `.env` batendo com a imagem em execução (§7.6)
- [ ] Senha do ADMIN inicial trocada e `secrets/admin.senha` esvaziado (§8.2)
- [ ] ADMIN real e profissionais criados (§8.3)
- [ ] `/api/auth/registrar` testado como anônimo e devolvendo 403 (§8.4)
- [ ] Dados da clínica preenchidos (§8.5)
- [ ] `backup.sh` rodado à mão com sucesso e agendado no cron **do usuário de deploy**
- [ ] `COMANDO_COPIA_EXTERNA` configurado e conferido no destino
- [ ] Teste de restauração marcado no calendário
- [ ] Um deploy pelo CD executado de ponta a ponta, e o rollback testado uma vez

**Pendências assumidas, não bloqueantes**
- [ ] RNF01 — criptografia em repouso de CPF e dados clínicos
- [ ] RNF03 — trilha de acesso a prontuário (quem leu o quê, quando)
- [ ] Backup cifrado antes do envio externo
- [ ] RNF05/RNF06 — o cenário nominal ainda reprova em relatório e exportação (`testes-carga/README.md`)
- [ ] Scan de vulnerabilidade da imagem no CI (`CICD.md` §7)
- [ ] DPA assinado com o provedor da VPS
