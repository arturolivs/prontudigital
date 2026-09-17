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
sistema receba dado real de paciente com segurança. O 1.1 e o 1.2 já foram
corrigidos no código; os outros dois se resolvem na execução, nos passos
indicados.

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

> A resposta para quem não é ADMIN é **403**, inclusive sem credencial
> nenhuma: a configuração não declara `authenticationEntryPoint`, então vale o
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
a partir de `ADMIN_SENHA` do `.env` (§5).

Ele é deliberadamente conservador — **não faz nada** quando já existe qualquer
usuário com perfil ADMIN. Ou seja: trocar a senha pela tela não é desfeito no
próximo `restart`, e o `.env` não é uma porta dos fundos permanente.

**Validação:** as 26 migrations aplicadas em sequência num Postgres 16 limpo,
sem erro; `usuarios` nasce com **0 linhas**; `mvnw test` → **377 testes, 0
falhas**.

> Nenhum comando de limpeza pós-boot é mais necessário. O que a §8 pede agora é
> a troca da senha inicial, não a remoção de usuários de demonstração.

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
Combine com a §5: a senha de `ADMIN_SENHA` é a única credencial da instalação
até que outros usuários sejam criados — guarde-a num gerenciador. Detalhes em [`DEPLOY_SEM_WHATSAPP.md`](../DEPLOY_SEM_WHATSAPP.md).

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
printf '%s' "$(openssl rand -base64 18)" > secrets/admin.senha   # 1º acesso

chmod 600 secrets/*
cat secrets/admin.senha    # anote num gerenciador antes de seguir
```

Detalhes de cada um e o que fazer depois do primeiro acesso:
`docker/prod/secrets/README.md`. O Compose recusa subir se faltar algum:

```
error while creating mount source path ... secrets/jwt.secret: no such file or directory
```

### 5.2 `.env`

```bash
cp .env.prod.example .env
chmod 600 .env
```

| Variável | Obrigatória | Observação |
|---|---|---|
| `DOMINIO` | ✅ | Sem `https://` e sem barra. Usado pelo Caddy, pelo build do frontend e no link de confirmação |
| `ACME_EMAIL` | ✅ | E-mail da conta Let's Encrypt — recebe o aviso de certificado a vencer. **O Caddy não sobe sem ele** |
| `POSTGRES_DB` / `POSTGRES_USER` | ✅ | Pode manter os valores do exemplo |
| `JWT_ACCESS_EXPIRATION_MS` | ✅ | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | ✅ | `604800000` (7 dias) — precisa bater com o `maxAge` do cookie `__pd_rt` |
| `ADMIN_USERNAME` | ✅ | Login do primeiro ADMIN. Sem ele **ninguém consegue entrar**: o banco nasce sem usuário nenhum (§1.2) |
| `ADMIN_NOME` / `ADMIN_EMAIL` / `ADMIN_TELEFONE` | ⬜ | Só aparência e contato; o e-mail, se informado, precisa ser único |
| `NOTIFICACOES_HABILITADAS` | ✅ | **`false` no primeiro deploy.** Ver §1.4 e `DEPLOY_SEM_WHATSAPP.md` |
| `WHATSAPP_*` (4) | ⬜ | Deixe vazias enquanto a flag acima for `false` |
| `COMANDO_COPIA_EXTERNA` | ⚠️ | Vazia = backup mora no disco que deveria proteger. Ver §9 |
| `RETENCAO_DIAS` / `RETENCAO_SEMANAIS` | ⬜ | Padrão 14 / 8 |

A senha do banco, o segredo do JWT e a senha do primeiro ADMIN **não estão
nesta tabela porque não são variáveis de ambiente** — são os três arquivos da
§5.1.

Valide antes de subir. As obrigatórias da tabela estão declaradas como
`${VAR:?mensagem}` no bloco `x-env-obrigatorias` do
`docker-compose.prod.yml`, então o comando abaixo **sai com erro** se alguma
faltar ou estiver vazia (com `${VAR}` puro o Compose só emitiria um warning e
substituiria por string vazia — o `up` seguiria adiante com um `JWT_SECRET` em
branco):

```bash
docker compose -f docker-compose.prod.yml config >/dev/null && echo OK

# Faltando, a saída é esta — e vale igual para o `up`:
# error while interpolating x-env-obrigatorias.[]: required variable
#   JWT_SECRET is missing a value: defina JWT_SECRET no .env (openssl rand -base64 64)
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
3. **`backend`** inicia, o **Flyway aplica as 26 migrations** (`V1` a `V26`) e
   a JVM sobe. O healthcheck tem `start_period: 60s`. Logo após o boot, procure
   esta linha — é o ADMIN inicial sendo criado (§1.2):

   ```
   INFO  c.p.b.a.config.BootstrapAdminRunner : ADMIN inicial 'seu.usuario' criado.
   ```

   Se aparecer `Nenhum ADMIN no banco e APP_BOOTSTRAP_ADMIN_USERNAME/SENHA nao
   informados`, o `.env` está sem `ADMIN_USERNAME`/`ADMIN_SENHA`: preencha e
   `docker compose -f docker-compose.prod.yml restart backend`.
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

# 7.3 — o banco tem exatamente UM usuário, o ADMIN inicial, e mais nenhum
docker compose -f docker-compose.prod.yml exec postgres   psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"   -c 'SELECT u.username, p.nome FROM usuarios u
        JOIN usuario_perfis up ON up.usuario_id = u.id
        JOIN perfis p ON p.id = up.perfil_id;'

# 7.4 — TLS válido e emitido pelo Let's Encrypt
curl -sSI https://SEU-DOMINIO | head -1

# 7.5 — API respondendo através do Caddy (não 404 do Next)
curl -s https://SEU-DOMINIO/api/configuracao | head -c 200

# 7.6 — backend ouvindo (sem porta publicada no host)
docker compose -f docker-compose.prod.yml exec backend \
  bash -c 'echo > /dev/tcp/127.0.0.1/8080' && echo "backend ouvindo"

# 7.7 — diretório de anexos gravável pelo usuário do backend
docker compose -f docker-compose.prod.yml exec backend \
  sh -c 'touch /dados/anexos/.escrita && rm /dados/anexos/.escrita' \
  && echo "anexos graváveis"

# 7.8 — saída para a internet (o WhatsApp Cloud API depende dela)
docker compose -f docker-compose.prod.yml exec backend \
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
> permissões da imagem quando cria o volume — reconstruir a imagem não
> conserta um volume que já existe. Ajuste uma vez:
>
> ```bash
> docker compose -f docker-compose.prod.yml exec -u root backend \
>   chown -R prontu:prontu /dados
> ```

Pela interface:

- `https://SEU-DOMINIO` carrega a tela de login com o nome da clínica;
- o agendamento público lista procedimentos (vieram da `V18`: Podiatria e
  Tratamento de Feridas);
- `https://SEU-DOMINIO/swagger-ui.html` **deve dar 404** — o Swagger está
  desabilitado em produção (`application-prod.yaml:81-85`).

---

## 8. Endurecimento pós-primeiro-boot — **obrigatório, mesmo dia**

Não há mais usuário de demonstração para apagar (§1.2): o banco nasce com três
`perfis`, dois `procedimentos`, uma linha de `configuracao_clinica` e **nenhum
usuário** além do ADMIN que o próprio backend criou a partir do `.env`. O que
falta é tirar a senha inicial de circulação.

**8.1 — Entre com `ADMIN_USERNAME` / `ADMIN_SENHA`** e troque a senha em
*Dashboard > Meu perfil*. Guarde a nova num gerenciador: sem WhatsApp não há
autoatendimento de recuperação (§1.4), e este é o único ADMIN da instalação.

**8.2 — Esvazie `secrets/admin.senha`** — esvazie, não apague: o Compose exige
que o arquivo exista.

```bash
cd /opt/prontudigital/docker/prod
: > secrets/admin.senha
docker compose -f docker-compose.prod.yml up -d backend
```

O runner só age quando o banco não tem nenhum ADMIN, então a partir daqui ele
não faz mais nada em boot algum — esvaziar não muda comportamento, tira a senha
do disco. O `up -d` recria o container para que ele também deixe de enxergar o
arquivo com o conteúdo antigo; `restart` reaproveita o container atual.

**8.3 — Crie os usuários reais** em *Dashboard > Usuários*: o ADMIN da clínica
(pessoa de verdade, com telefone correto) e os profissionais. Saia, entre com o
ADMIN real e confirme que ele enxerga usuários, agenda e relatórios.

> Se quiser deixar de usar o usuário criado pelo `.env`, **desative-o** pela
> tela em vez de apagar — um ADMIN sem nenhum registro associado pode ser
> removido, mas desativar evita a checagem de `RESTRICT` em `agendamentos`.

**8.4 — Confirme que o registro público está fechado** (§1.1) — a correção já
está no código, isto aqui é a verificação de que a imagem em produção a tem:

```bash
curl -s -o /dev/null -w '%{http_code}
' -X POST https://SEU-DOMINIO/api/auth/registrar   -H 'Content-Type: application/json'   -d '{"username":"teste_bloqueio","senha":"Teste12345","nomeCompleto":"t","email":"t@t.com","perfis":["ADMIN"]}'
```

Esperado: **403**. Se vier **201**, a imagem em produção é anterior à correção
de 15/09/2026 — apague o usuário criado e suba a versão nova antes de divulgar
o endereço para a clínica.

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
