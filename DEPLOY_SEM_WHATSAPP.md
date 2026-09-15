# Implantação em produção **sem** o envio de mensagens por WhatsApp

> Cenário: subir o ProntuDigital em produção com a integração da Meta
> (WhatsApp Cloud API) **desligada**, de forma a ligá-la depois sem mexer em
> código.
>
> Este documento cobre só o recorte "sem mensageria". As pendências gerais de
> primeiro deploy (DNS, backup, firewall, LGPD) continuam valendo e estão em
> [`CHECKLIST_PRODUCAO.md`](./CHECKLIST_PRODUCAO.md) — leia os dois.
>
> Levantamento feito em **14/09/2026**, lendo o estado atual do repositório.

---

## 1. Resumo

O código **já previa** um interruptor (`app.notificacoes.habilitadas`), mas ele
não estava exposto em produção: `application-prod.yaml` não o declarava, então
valia o `true` herdado de `application.yaml`. Além disso, as quatro variáveis do
WhatsApp eram obrigatórias no boot em produção.

Feitos os ajustes da §2, subir sem WhatsApp passa a ser uma questão de **uma
linha no `.env`**:

```dotenv
NOTIFICACOES_HABILITADAS=false
```

Não há rebuild de imagem, remoção de serviço nem edição de código envolvida.

---

## 2. Ajustes aplicados no código

| # | Arquivo | O que mudou | Por quê |
|---|---|---|---|
| 2.1 | `backend/src/main/resources/application-prod.yaml` | Declara `app.notificacoes.habilitadas: ${APP_NOTIFICACOES_HABILITADAS:false}` | Sem isso, produção herdava `true` de `application.yaml` e os schedulers rodariam tentando falar com a Meta a cada 15 min |
| 2.2 | `application-prod.yaml` | As 4 variáveis do WhatsApp ganharam default vazio (`${...:}`) | Eram obrigatórias no boot. Sem elas o Spring falha ao resolver o placeholder e o container entra em crash-loop — a aplicação inteira, não só a mensageria |
| 2.3 | `AutenticacaoServiceImpl.solicitarRecuperacaoSenha` | Com a flag em `false`, responde **503** e mensagem orientando procurar o administrador | O código de recuperação **só** trafega por WhatsApp. Antes, a tentativa estourava `IllegalStateException` → 500 genérico, com stack trace no log a cada tentativa |
| 2.4 | `WhatsappWebhookController` | Com a flag em `false`, `GET`/`POST /api/whatsapp/webhook` respondem **404** | É o único endpoint público, sem JWT, que aceita POST com corpo arbitrário — e, com `app-secret` vazio, a assinatura **não** é conferida (`WhatsappWebhookServiceImpl.assinaturaValida` devolve `true`). Desligado, ele não deve existir para o mundo |
| 2.5 | `GlobalExceptionHandler`, `RecuperacaoSenhaIndisponivelException` (novo), `messages.properties` | Mapeia a nova exceção para 503 com mensagem em português | Suporte ao item 2.3 |
| 2.6 | `docker/prod/docker-compose.prod.yml` | Passa `APP_NOTIFICACOES_HABILITADAS: ${NOTIFICACOES_HABILITADAS:-false}`; as 4 do WhatsApp aceitam valor vazio | Expõe o interruptor no `.env`, com padrão seguro |
| 2.7 | `docker/prod/.env.prod.example` | Nova seção documentando o interruptor e o que ele desliga | — |

**Validação executada:** `mvnw test` → **360 testes, 0 falhas**.
`docker compose config` com o `.env.prod.example` → OK.

> Nada foi removido. O código de mensageria continua inteiro, testado e
> versionado; só não é executado enquanto a flag estiver em `false`.

---

## 3. O que deixa de funcionar — e o que fazer no lugar

Com `NOTIFICACOES_HABILITADAS=false`, o sistema sobe **completo**: agenda,
prontuário, anexos, atestados, relatórios, agendamento público, login. O que
para é:

### 3.1 Lembretes e confirmação de consulta

Os três schedulers (`AgendamentoNotificacaoScheduler`) continuam agendados a
cada 15 min, mas retornam de imediato e registram só um `DEBUG`. Ou seja:

- **não sai** o lembrete de 48h;
- **não sai** o pedido de confirmação de 24h;
- **não há** cancelamento automático de quem não confirma até 2h antes.

O último item é um efeito **desejável** de estar desligado: sem mensagem
enviada, cancelar por "falta de confirmação" puniria um paciente que nunca foi
avisado. A guarda já existe no serviço (só libera agendamento que teve
`CONFIRMACAO_24H` com status `ENVIADO`), mas com a flag desligada o scheduler
nem chega lá.

**No lugar:** a confirmação passa a ser feita pela recepção — a tela de agenda
já permite mudar o status do agendamento manualmente.

### 3.2 Recuperação de senha pelo próprio usuário — **atenção**

`POST /api/auth/recuperar-senha/solicitar` é o único fluxo de "esqueci minha
senha" e depende 100% do WhatsApp; não existe fallback por e-mail no código.
Desligado, responde 503 com a orientação de procurar o administrador.

O impacto imediato é pequeno: **o frontend não tem tela de "esqueci minha
senha"** — o endpoint só é alcançável por chamada direta à API.

**Porém**, e isto pede decisão consciente antes de operar com usuários reais:
`PATCH /api/usuarios/{id}/senha` exige a **senha atual**
(`UsuarioServiceImpl.alterarSenha:192`). Não existe, portanto, um "resetar senha
do usuário" à disposição do ADMIN. Com o WhatsApp desligado, quem esquece a
senha só é recuperado por intervenção direta no banco:

```bash
# 1) Gerar o hash BCrypt de uma senha provisória.
#    -B = BCrypt, -C 10 = mesmo custo do BCryptPasswordEncoder padrão do Spring.
docker run --rm httpd:2-alpine htpasswd -nbBC 10 "" 'SenhaProvisoria123' | cut -d: -f2

# 2) Aplicar no banco (cole o hash gerado acima entre as aspas).
cd docker/prod
docker compose -f docker-compose.prod.yml exec postgres \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "UPDATE usuarios SET senha_hash = '<hash-bcrypt>' WHERE username = '<usuario>';"
```

O usuário troca a senha depois em **Meu perfil**, aí sim com a senha atual em
mãos.

**Recomendação** — escolher uma destas antes de liberar para uso real:

1. ligar o WhatsApp (§6), que resolve o fluxo na origem; ou
2. criar um endpoint de reset restrito a ADMIN, sem exigir a senha atual —
   funcionalidade nova, não entra neste deploy; ou
3. aceitar o procedimento manual acima e documentá-lo para quem opera.

### 3.3 Achado de segurança pré-existente — decidir antes do go-live

`POST /api/auth/ativar-acesso` é **público** e, informando apenas um **telefone**
cadastrado, sobrescreve e-mail, username e senha do usuário
(`UsuarioServiceImpl.ativarAcesso:279-297`), sem verificar se o acesso já havia
sido ativado. Na prática é tomada de conta com um dado que não é segredo.

Isto **não** foi introduzido pelo desligamento do WhatsApp e não é corrigido
aqui — mas, com o canal de mensagens fora, ele passa a ser o único caminho de
autoatendimento que sobra, então merece decisão explícita antes de produção
(exigir um código enviado por algum canal, ou restringir a quem ainda está com
`acesso_ativado = false`).

### 3.4 Webhook de status de entrega

`/api/whatsapp/webhook` responde 404 enquanto a flag estiver em `false`. Não há
o que cadastrar no painel da Meta neste deploy.

---

## 4. Passo a passo da implantação

Pré-requisitos inalterados: VPS com Docker e Docker Compose, domínio registrado
e com registro **A** apontando para o IP da VPS **antes** do primeiro `up` (o
Caddy emite o certificado por HTTP-01 e falha sem DNS resolvendo).

### Passo 1 — Clonar o repositório na VPS

```bash
git clone <url-do-repo> prontudigital
cd prontudigital/docker/prod
```

### Passo 2 — Criar o `.env` de produção

```bash
cp .env.prod.example .env
```

### Passo 3 — Preencher o `.env`

Obrigatórias:

| Variável | Como obter |
|---|---|
| `DOMINIO` | Domínio já apontado para a VPS |
| `POSTGRES_DB` / `POSTGRES_USER` | À escolha |
| `POSTGRES_PASSWORD` | `openssl rand -base64 24` |
| `JWT_SECRET` | `openssl rand -base64 64` — **mínimo 64 caracteres** (HS512; abaixo disso o `JwtTokenProvider` derruba o boot) |
| `JWT_ACCESS_EXPIRATION_MS` | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 dias — igual ao `maxAge` do cookie `__pd_rt`) |

**Da mensageria — o ponto deste documento:**

```dotenv
NOTIFICACOES_HABILITADAS=false

# Deixe as quatro EM BRANCO. Não invente valor de placeholder.
WHATSAPP_PHONE_NUMBER_ID=
WHATSAPP_ACCESS_TOKEN=
WHATSAPP_WEBHOOK_VERIFY_TOKEN=
WHATSAPP_APP_SECRET=
```

Recomendadas (backup — ver `docker/prod/scripts/README.md`):
`COMANDO_COPIA_EXTERNA`, `RETENCAO_DIAS`, `RETENCAO_SEMANAIS`.

`APP_URL_BASE_CONFIRMACAO` **não** vai no `.env`: é derivada de `DOMINIO` dentro
do compose. Ela continua sendo calculada mesmo com o WhatsApp desligado — é
inofensiva, só não será usada por ninguém.

### Passo 4 — Conferir o `.env` antes de subir

```bash
docker compose -f docker-compose.prod.yml config | grep NOTIFICACOES
```

Deve aparecer `APP_NOTIFICACOES_HABILITADAS: "false"`.

### Passo 5 — Subir a stack

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

O build (Maven + `next build`) roda **na VPS** — não há pipeline de CI no repo.
Numa VPS de 4 GB isso é confortável; em 2 GB, não. Acompanhe:

```bash
docker compose -f docker-compose.prod.yml logs -f
```

### Passo 6 — Confirmar que o backend subiu com a mensageria desligada

```bash
docker compose -f docker-compose.prod.yml logs backend | grep -i whatsapp
```

Esperado, **uma única vez, no boot**:

```
[WHATSAPP] Cliente Cloud API NAO configurado (phone-number-id e/ou access-token
ausentes). Todos os envios serao recusados ate que as credenciais sejam informadas.
```

Esse `WARN` é o comportamento correto neste cenário — não é erro.

**Não** deve aparecer nenhuma linha `[WHATSAPP][SCHEDULER ...]` de nível INFO,
nem tentativa de chamada a `graph.facebook.com`.

### Passo 7 — Verificar que o webhook está fechado

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST https://$DOMINIO/api/whatsapp/webhook \
  -H 'Content-Type: application/json' -d '{}'
# esperado: 404
```

### Passo 8 — Smoke test funcional

- `https://<DOMINIO>` carrega a tela de login com TLS válido;
- login com o usuário administrador;
- criar um agendamento de teste para **dentro de ~48h** e confirmar que ele
  aparece na agenda **e que nenhuma mensagem é disparada** (repita o Passo 6
  após 15 minutos — a janela de execução dos schedulers);
- confirmar que esse agendamento **continua ativo** depois de passar da janela
  de 2h antes do horário, ou seja, sem cancelamento automático.

### Passo 9 — Backup (não pule)

```bash
crontab -e
# 0 3 * * * /caminho/para/prontudigital/docker/prod/scripts/backup.sh
```

Depois, valide o primeiro backup com `scripts/restore.sh --teste`. Detalhes em
`docker/prod/scripts/README.md`.

### Passo 10 — Hardening do host

`ufw` liberando só 22/80/443, SSH sem login por senha, `fail2ban`. Nada disso é
coberto pelo compose.

---

## 5. Verificação final

| Verificação | Comando | Esperado |
|---|---|---|
| Containers de pé | `docker compose -f docker-compose.prod.yml ps` | 4 serviços `Up`; `postgres` e `backend` *healthy* |
| Mensageria desligada | `docker compose ... logs backend \| grep SCHEDULER` | Nenhuma linha INFO de scheduler |
| Webhook fechado | `curl -X POST .../api/whatsapp/webhook` | `404` |
| Recuperação de senha | `curl -X POST .../api/auth/recuperar-senha/solicitar -H 'Content-Type: application/json' -d '{"telefone":"11999998888"}'` | `503` com a mensagem orientando procurar o administrador |
| Nada saindo para a Meta | `docker compose ... logs backend \| grep graph.facebook` | Vazio |

---

## 6. Como ligar o WhatsApp depois

Sem rebuild e sem tocar em código:

1. No painel da Meta, obter `phone-number-id`, **token permanente** (System
   User — não o token de teste de 24h) e o **app secret**.
2. Editar `docker/prod/.env`:
   ```dotenv
   NOTIFICACOES_HABILITADAS=true
   WHATSAPP_PHONE_NUMBER_ID=<valor>
   WHATSAPP_ACCESS_TOKEN=<token permanente>
   WHATSAPP_WEBHOOK_VERIFY_TOKEN=<texto livre, à sua escolha>
   WHATSAPP_APP_SECRET=<app secret>
   ```
3. Reiniciar só o backend:
   ```bash
   docker compose -f docker-compose.prod.yml up -d backend
   ```
4. Confirmar no log: `[WHATSAPP] Cliente Cloud API pronto: endpoint=...`
5. Cadastrar `https://<DOMINIO>/api/whatsapp/webhook` no painel da Meta — o
   handshake `GET` só passa com a flag já em `true` — usando o mesmo
   `VERIFY_TOKEN` do `.env`.
6. Testar seguindo [`TESTE_WHATSAPP_DEV.md`](./TESTE_WHATSAPP_DEV.md).

> **Ao ligar, atenção ao acervo existente.** O cancelamento por falta de
> confirmação só atinge agendamentos que **receberam** o pedido de confirmação,
> então os criados durante o período desligado não são cancelados
> retroativamente. Ainda assim, ligue fora do horário de pico e acompanhe o
> primeiro ciclo de 15 minutos.

---

## 7. Ainda pendente (independe do WhatsApp)

Continuam valendo, de `CHECKLIST_PRODUCAO.md` §3 e §4:

- cópia de backup para **fora** do servidor (`COMANDO_COPIA_EXTERNA`) e cifragem
  do dump — é prontuário de saúde;
- teste mensal de restore agendado;
- firewall e hardening do host;
- RNF01 — criptografia em repouso de campos sensíveis;
- DPA assinado com o provedor da VPS antes de operar com dados reais.

E, deste documento, as decisões das §3.2 (reset de senha pelo ADMIN) e §3.3
(`ativar-acesso` público).
