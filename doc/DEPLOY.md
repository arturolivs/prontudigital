# Deploy em produção — ProntuDigital

> **Runbook de execução.** Da VPS vazia ao sistema no ar, na ordem em que os
> passos precisam acontecer. Escrito lendo o estado atual do repositório em
> **15/09/2026** (branch `config-deploy`, a partir de `76ea09c`).
>
> Este documento **não** decide plataforma nem repete análises — ele executa.
> O que cada documento vizinho cobre está na §0.

---

## 0. Mapa dos documentos

| Documento | Responde |
|---|---|
| **este** (`doc/DEPLOY.md`) | *Como* subir, passo a passo, e como operar depois |
| [`ANALISE_DEPLOY.md`](../ANALISE_DEPLOY.md) | *Onde* hospedar — comparativo de plataformas e a decisão (VPS 4 GB em São Paulo) |
| [`CHECKLIST_PRODUCAO.md`](../CHECKLIST_PRODUCAO.md) | *O que faltava* no código e na infra — levantamento de 11/08/2026 |
| [`DEPLOY_SEM_WHATSAPP.md`](../DEPLOY_SEM_WHATSAPP.md) | O recorte de subir com a mensageria desligada e ligá-la depois |
| [`docker/prod/scripts/README.md`](../docker/prod/scripts/README.md) | Backup e restauração (RNF02) em detalhe |

---

## 1. Bloqueadores levantados nesta análise

Os quatro itens abaixo **não impedem o `docker compose up`** — impedem que o
sistema receba dado real de paciente com segurança. O 1.1 já foi corrigido no
código; os outros três se resolvem na execução, nos passos indicados.

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

**Validação:** `mvnw test` → **366 testes, 0 falhas**; `tsc --noEmit` e
`eslint` limpos no frontend.

> A resposta para quem não é ADMIN é **403**, inclusive sem credencial
> nenhuma: a configuração não declara `authenticationEntryPoint`, então vale o
> `Http403ForbiddenEntryPoint` padrão — mesmo comportamento do resto da API.

### 1.2 — A migration `V8` semeia 13 usuários de demonstração em produção

`backend/src/main/resources/db/migracoes/V8__insert_dados_exemplo.sql` roda em
**todo** ambiente — o Flyway não distingue perfil. Ela cria:

- `admin` (perfil ADMIN), `enfermeiro`, `enfermeiro.santos` e 10 pacientes
  fictícios;
- todos com o **mesmo hash BCrypt**, cuja senha em claro é `senha123` — valor
  que circula no repositório e no histórico do Git.

Ou seja: no minuto em que o domínio responde, existe um **ADMIN com senha
pública**. Era o mais grave dos dois: diferente do 1.1, não depende de o
atacante descobrir nada além do nome do produto. Continua aberto — só se fecha
na operação (§8), porque a migration precisa existir pelos perfis que ela cria.

A mesma migration cria os três registros de `perfis` (ADMIN / PROFISSIONAL /
PACIENTE), **que a aplicação precisa** — por isso a saída não é apagar a
migration, e sim limpar os usuários depois do primeiro boot: **§8**.

### 1.3 — Logs do Docker sem rotação

Nenhum serviço de `docker/prod/docker-compose.prod.yml` declara `logging:`, e o
backend escreve tudo em `stdout`. Com o driver `json-file` padrão e sem limite,
o log cresce até encher o disco — e quem morre junto é o Postgres, no mesmo
volume. O Caddy é a exceção: já rotaciona sozinho (`Caddyfile:18-21`).
Resolvido no passo **§3.4**.

### 1.4 — Sem WhatsApp, ninguém recupera a própria senha

Com `NOTIFICACOES_HABILITADAS=false` (o padrão recomendado para o primeiro
deploy), `POST /api/auth/recuperar-senha/solicitar` responde **503**: o código
de recuperação só trafega por WhatsApp, não há fallback por e-mail. Quem
esquecer a senha depende do ADMIN redefinir em *Dashboard > Usuários*.
Combine com a §8: o ADMIN real precisa existir e alguém precisa saber a senha
dele. Detalhes em [`DEPLOY_SEM_WHATSAPP.md`](../DEPLOY_SEM_WHATSAPP.md).

---

## 2. Pré-requisitos

| Item | Valor esperado | Por quê |
|---|---|---|
| VPS | 4 GB RAM, 2 vCPU, 40 GB SSD, região São Paulo | ~2 GB só de limites de container; o build roda aqui também |
| SO | Ubuntu 22.04 ou 24.04 LTS | Os scripts assumem `bash` + `cron` do host |
| Domínio | registrado, com acesso ao painel de DNS | O Caddy emite o certificado sozinho, mas exige DNS resolvendo |
| Acesso | SSH com chave, usuário com `sudo` | — |
| Git | acesso de leitura ao repositório a partir da VPS | O deploy é `git clone` + build local |

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
docker compose version   # precisa ser v2 (plugin), não docker-compose v1
```

### 3.2 Swap — recomendado nos 4 GB

O `next build` e o Maven rodam **na própria VPS** e competem por RAM com os
containers já no ar. 2 GB de swap evitam que o OOM killer derrube o Postgres
durante um deploy.

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

Os containers já recebem `TZ=America/Sao_Paulo`, mas o cron do backup, os logs
do host e `docker logs -t` usam o relógio da máquina.

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
de fora do Docker.

### 3.6 SSH

```bash
sudo sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
sudo sed -i 's/^#\?PermitRootLogin.*/PermitRootLogin prohibit-password/' /etc/ssh/sshd_config
sudo systemctl restart ssh
sudo apt install -y fail2ban && sudo systemctl enable --now fail2ban
```

> Confirme que sua chave funciona **em outra sessão** antes de fechar a atual.

---

## 4. Código no servidor

```bash
sudo mkdir -p /opt/prontudigital && sudo chown "$USER" /opt/prontudigital
git clone <URL-DO-REPO> /opt/prontudigital
cd /opt/prontudigital
git checkout main          # ou a tag/branch que for ao ar
```

Nada além do `.env` (§5) precisa ser editado no servidor. Confirme que o commit
clonado é posterior a **15/09/2026** — antes disso o registro público da §1.1
está aberto. Qualquer outro ajuste de código vai **no repositório**, com merge
antes do clone: arquivo editado solto na VPS some no próximo `git pull`.

---

## 5. Configuração — `docker/prod/.env`

```bash
cd /opt/prontudigital/docker/prod
cp .env.prod.example .env
chmod 600 .env
```

Gere os segredos **na VPS**, não reaproveite de lugar nenhum:

```bash
openssl rand -base64 24   # POSTGRES_PASSWORD
openssl rand -base64 64   # JWT_SECRET (HS512 exige chave longa)
```

| Variável | Obrigatória | Observação |
|---|---|---|
| `DOMINIO` | ✅ | Sem `https://` e sem barra. Usado pelo Caddy, pelo build do frontend e no link de confirmação |
| `POSTGRES_DB` / `POSTGRES_USER` | ✅ | Pode manter os valores do exemplo |
| `POSTGRES_PASSWORD` | ✅ | **Só é lida na criação do volume.** Trocar depois exige `ALTER USER` no banco |
| `JWT_SECRET` | ✅ | Trocar invalida todos os tokens — todo mundo é deslogado |
| `JWT_ACCESS_EXPIRATION_MS` | ✅ | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | ✅ | `604800000` (7 dias) — precisa bater com o `maxAge` do cookie `__pd_rt` |
| `NOTIFICACOES_HABILITADAS` | ✅ | **`false` no primeiro deploy.** Ver §1.4 e `DEPLOY_SEM_WHATSAPP.md` |
| `WHATSAPP_*` (4) | ⬜ | Deixe vazias enquanto a flag acima for `false` |
| `COMANDO_COPIA_EXTERNA` | ⚠️ | Vazia = backup mora no disco que deveria proteger. Ver §9 |
| `RETENCAO_DIAS` / `RETENCAO_SEMANAIS` | ⬜ | Padrão 14 / 8 |

Valide a substituição antes de subir — este comando falha se faltar variável:

```bash
docker compose -f docker-compose.prod.yml config >/dev/null && echo OK
```

> **`DOMINIO` entra no bundle do frontend em tempo de build.** Trocar o domínio
> depois exige `up -d --build frontend`, não basta reiniciar (§10.4).

---

## 6. Subir a stack

```bash
cd /opt/prontudigital/docker/prod
docker compose -f docker-compose.prod.yml up -d --build
```

O que esperar, em ordem:

1. **Build (8–20 min na primeira vez)** — Maven baixa dependências e roda
   `package -DskipTests`; o Next.js roda `npm ci` + `next build`.
2. **`postgres`** sobe e fica `healthy` (~15 s).
3. **`backend`** inicia, o **Flyway aplica as 27 migrations** (incluindo a `V8`
   da §1.2) e a JVM sobe. O healthcheck tem `start_period: 60s`.
4. **`frontend`** só inicia depois de o backend estar `healthy` (`depends_on`).
5. **`caddy`** sobe, pede o certificado ao Let's Encrypt e passa a servir 443.

Acompanhe:

```bash
docker compose -f docker-compose.prod.yml logs -f
```

---

## 7. Verificação

Rode tudo antes de considerar o deploy feito.

```bash
# 7.1 — todos os containers de pé, backend e postgres "healthy"
docker compose -f docker-compose.prod.yml ps

# 7.2 — migrations aplicadas, nenhuma com success = false
docker compose -f docker-compose.prod.yml exec postgres \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c 'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;'

# 7.3 — TLS válido e emitido pelo Let's Encrypt
curl -sSI https://SEU-DOMINIO | head -1

# 7.4 — API respondendo através do Caddy (não 404 do Next)
curl -s https://SEU-DOMINIO/api/configuracao | head -c 200

# 7.5 — backend ouvindo (exposto só na rede interna)
docker compose -f docker-compose.prod.yml exec backend \
  bash -c 'echo > /dev/tcp/127.0.0.1/8080' && echo "backend ouvindo"
```

Pela interface:

- `https://SEU-DOMINIO` carrega a tela de login com o nome da clínica;
- o agendamento público lista procedimentos (vieram da `V22`: Podiatria e
  Tratamento de Feridas);
- `https://SEU-DOMINIO/swagger-ui.html` **deve dar 404** — o Swagger está
  desabilitado em produção (`application-prod.yaml:81-85`).

---

## 8. Endurecimento pós-primeiro-boot — **obrigatório, mesmo dia**

Resolve a §1.2. Enquanto não for feito, existe um ADMIN com senha pública.

**8.1 — Entre como `admin` / `senha123`** e, em *Dashboard > Usuários*, crie o
ADMIN real da clínica (pessoa de verdade, senha forte, telefone correto) e os
profissionais. Guarde essa senha num gerenciador: sem WhatsApp não há
autoatendimento de recuperação (§1.4).

**8.2 — Saia e entre com o ADMIN real.** Confirme que ele enxerga usuários,
agenda e relatórios antes de apagar qualquer coisa.

**8.3 — Remova os 13 usuários de demonstração:**

```bash
cd /opt/prontudigital/docker/prod
docker compose -f docker-compose.prod.yml exec postgres \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

```sql
-- Confira ANTES o que vai sair (e que seu admin real NÃO está na lista):
SELECT id, username, nome_completo FROM usuarios
 WHERE username IN ('admin','enfermeiro','enfermeiro.santos','paciente',
   'paciente.souza','paciente.rodrigues','paciente.almeida','paciente.ferreira',
   'paciente.costa','paciente.lima','paciente.martins','paciente.barbosa',
   'paciente.ribeiro');

-- Apaga. usuario_perfis, refresh_tokens e codigos_recuperacao_senha têm
-- ON DELETE CASCADE; agendamentos é RESTRICT — numa base nova não há nenhum,
-- então o DELETE passa. Se falhar, é porque a demo já foi usada: nesse caso
-- desative o usuário pela tela em vez de apagar.
DELETE FROM usuarios WHERE username IN ('admin','enfermeiro','enfermeiro.santos',
  'paciente','paciente.souza','paciente.rodrigues','paciente.almeida',
  'paciente.ferreira','paciente.costa','paciente.lima','paciente.martins',
  'paciente.barbosa','paciente.ribeiro');

-- NÃO apague a tabela `perfis`: ADMIN/PROFISSIONAL/PACIENTE vêm da mesma
-- migration e a aplicação depende deles.
SELECT id, nome FROM perfis;   -- deve continuar com as 3 linhas
```

**8.4 — Confirme que o registro público está fechado** (§1.1) — a correção já
está no código, isto aqui é a verificação de que a imagem em produção a tem:

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST https://SEU-DOMINIO/api/auth/registrar \
  -H 'Content-Type: application/json' \
  -d '{"username":"teste_bloqueio","senha":"Teste12345","nomeCompleto":"t","email":"t@t.com","perfis":["ADMIN"]}'
```

Esperado: **403**. Se vier **201**, a imagem em produção é anterior à correção
de 15/09/2026 — apague o usuário criado e suba a versão nova antes de divulgar
o endereço para a clínica.

**8.5 — Preencha os dados da clínica** em *Dashboard > Configuração*: nome,
CNPJ, endereço, logo e rodapé. Eles entram no cabeçalho dos PDFs (atestados,
prescrições) e na tela de login. Sem isso os documentos saem com o nome
genérico "ProntuDigital" da `V28`.

---

## 9. Backup — o deploy não está pronto sem isso

Detalhes e justificativas em
[`docker/prod/scripts/README.md`](../docker/prod/scripts/README.md). O mínimo:

```bash
cd /opt/prontudigital/docker/prod
chmod +x scripts/*.sh
sudo mkdir -p /var/backups/prontudigital
```

Defina o destino externo no `.env` — **o banco e os anexos são copiados
juntos**, porque os arquivos do prontuário não ficam no banco:

```dotenv
COMANDO_COPIA_EXTERNA="rclone sync /var/backups/prontudigital remoto:prontudigital"
```

Agende (cron do root, 3h da manhã):

```bash
sudo crontab -e
```

```cron
0 3 * * * /opt/prontudigital/docker/prod/scripts/backup.sh >> /var/log/prontudigital-backup.log 2>&1
```

Rode **uma vez à mão** para validar antes de confiar no cron:

```bash
sudo ./scripts/backup.sh
ls -la /var/backups/prontudigital/diarios/
```

E agende na sua agenda pessoal, mensalmente:

```bash
./scripts/restore.sh --teste /var/backups/prontudigital/diarios/<carimbo>
```

> O dump sai **em claro** e contém CPF e dado clínico. Cifrar antes do envio
> externo é pendência conhecida (`CHECKLIST_PRODUCAO.md` §4) — até lá, o
> destino precisa ser cifrado e de acesso restrito.

---

## 10. Operação

### 10.1 Logs

```bash
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs --since 1h --tail 200 backend
docker compose -f docker-compose.prod.yml exec caddy tail -f /var/log/caddy/access.log
```

Nível do backend em produção: `root=WARN`, `com.prontudigital=INFO`
(`application-prod.yaml:61-68`). Para depurar a mensageria sem rebuild, defina
`LOG_LEVEL_WHATSAPP=DEBUG` no `.env` e recrie o container do backend.

### 10.2 Atualizar versão

```bash
cd /opt/prontudigital
git pull
cd docker/prod
docker compose -f docker-compose.prod.yml up -d --build
docker image prune -f        # libera as imagens antigas
```

Migrations novas são aplicadas sozinhas no boot do backend. **Tire um backup
antes** de subir versão que traga migration: o Flyway não faz rollback.

### 10.3 Reiniciar um serviço só

```bash
docker compose -f docker-compose.prod.yml restart backend
```

Mudança em variável de ambiente exige `up -d backend` (recria o container),
não `restart`. Exceção: qualquer `NEXT_PUBLIC_*` está congelada no bundle e
exige `--build frontend`.

### 10.4 Trocar de domínio

```bash
# 1. Atualize o DNS e o DOMINIO no .env, depois:
docker compose -f docker-compose.prod.yml up -d --build frontend caddy
```

O frontend precisa de rebuild (a URL da API está no bundle); o Caddy pede
certificado novo sozinho.

### 10.5 Voltar atrás

```bash
# Código: volte ao commit anterior e reconstrua
git checkout <commit-anterior> && docker compose -f docker-compose.prod.yml up -d --build

# Dados: restauração destrutiva, exige digitar RESTAURAR
./scripts/restore.sh --real /var/backups/prontudigital/diarios/<carimbo>
```

---

## 11. Checklist de corte

Marque tudo antes de entregar o endereço para a clínica.

**Antes de expor**
- [ ] DNS apontando e propagado
- [ ] `daemon.json` com rotação de log (§3.4)
- [ ] `ufw` ativo, SSH sem senha, `fail2ban` rodando
- [ ] `.env` com segredos gerados na VPS, `chmod 600`
- [ ] `NOTIFICACOES_HABILITADAS=false` (ou as 4 variáveis do WhatsApp válidas)
- [ ] Imagem construída a partir de commit posterior a 15/09/2026 (traz a correção da §1.1)

**Depois de subir**
- [ ] `docker compose ps` com backend e postgres `healthy`
- [ ] 27 migrations aplicadas, `success = true`
- [ ] HTTPS válido; Swagger devolvendo 404
- [ ] ADMIN real criado; **13 usuários de demonstração apagados** (§8)
- [ ] `/api/auth/registrar` testado como anônimo e devolvendo 403 (§8.4)
- [ ] Dados da clínica preenchidos (§8.5)
- [ ] `backup.sh` rodado à mão com sucesso e agendado no cron
- [ ] `COMANDO_COPIA_EXTERNA` configurado e verificado no destino
- [ ] Teste de restauração marcado no calendário

**Pendências assumidas, não bloqueantes** (`CHECKLIST_PRODUCAO.md` §4)
- [ ] RNF01 — criptografia em repouso de CPF e dados clínicos
- [ ] RNF03 — trilha de acesso a prontuário (quem leu o quê, quando)
- [ ] Backup cifrado antes do envio externo
- [ ] DPA assinado com o provedor da VPS
