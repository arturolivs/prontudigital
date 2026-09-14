# 🧪 Teste ponta a ponta — WhatsApp em desenvolvimento

> Roteiro para validar o envio de notificações WhatsApp no ambiente de dev, usando o
> **número de teste gratuito da Meta**. Nenhum passo aqui custa dinheiro nem envolve
> conta de cliente.
>
> Contexto e decisões de arquitetura: ver [`WHATSAPP.md`](./WHATSAPP.md).
> Última revisão: **26/08/2026**.

---

## 0. Pré-requisitos

- [ ] App **`prontudigital-dev`** criado em `developers.facebook.com`, tipo **Empresa**,
      com o produto **WhatsApp** adicionado e **Modo do aplicativo: desenvolvimento**
- [ ] Ambiente dev subindo normalmente (`docker compose -f docker/dev/docker-compose.yml up -d`)
- [ ] Seu celular cadastrado como destinatário na Meta (passo 1.3)

> ⚠️ **Armadilha na criação do app.** Ao criar o app, na tela *"Casos de uso"* escolha
> **`Outro`** (embaixo de *"Procurando outra coisa?"*), **nunca** *"Conectar-se com os
> clientes pelo WhatsApp"*. O caso de uso do WhatsApp monta o app para o fluxo de
> *Tech Provider / Embedded Signup* — ele adiciona **Login do Facebook para Empresas**
> e **não entrega número de teste**. Só a "experiência antiga" (`Outro`) dá acesso à
> lista *"Adicionar produtos ao seu app"* com o WhatsApp.

---

## 1. Obter as credenciais na Meta

Painel do app → menu lateral **WhatsApp** → **`Etapa 1. Experimente`**.

1. **Reivindicar um número de teste** — a Meta cria automaticamente um número americano
   (ex.: `+1 (555) 195-7825`) e uma WABA de teste. Anote:

   | Campo na tela | Onde usar |
   |---|---|
   | **Phone Number ID** | `WHATSAPP_PHONE_NUMBER_ID` |
   | **WhatsApp Business account ID** | não é usado pelo código |

2. **Gerar token** — botão azul ao lado de *"Token de acesso"*. Copie o valor (`EAA...`).

   > ⏳ **O token expira em 24h.** Se o envio parar de funcionar do nada em dev, é a
   > primeira coisa a checar. Basta voltar aqui e gerar outro.

3. **Cadastrar o destinatário** — no campo `Para:` / `Destinatário`, use
   *"Gerenciar lista de números de telefone"* e adicione **seu celular**. A Meta manda
   um código de verificação pelo WhatsApp. **Até 5 números**, e o envio para qualquer
   número fora dessa lista falha.

4. **Sanity check no próprio painel** — clique em **`Enviar mensagem`** e confirme que
   chegou. O botão fica desabilitado enquanto não houver token gerado. Se isso não
   funcionar, não adianta seguir para o código.

---

## 2. Configurar o projeto

No `docker/dev/.env` (gitignored — o token **não** vai para o repositório):

```env
WHATSAPP_PHONE_NUMBER_ID=1308261632373006
WHATSAPP_ACCESS_TOKEN=EAA...
```

Recriar o backend para que as variáveis entrem no container:

```bash
docker compose -f docker/dev/docker-compose.yml up -d backend
```

> As variáveis são repassadas em `docker/dev/docker-compose.yml:75-76`. Sem esse
> repasse, preencher o `.env` não tem efeito nenhum — o Spring lê string vazia,
> `WhatsappCloudApiClient:27` marca `configurado = false` e todo envio vira `FALHA`.

Confirmar que chegaram:

```bash
docker exec prontudigital-backend-dev printenv | grep WHATSAPP_PHONE_NUMBER_ID
```

---

## 3. Teste direto na API (isola a Meta do sistema)

Antes de envolver o backend, valide as credenciais cruas:

```bash
curl -i -X POST "https://graph.facebook.com/v25.0/<PHONE_NUMBER_ID>/messages" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"messaging_product":"whatsapp","to":"5581979125323","type":"text","text":{"body":"teste prontudigital"}}'
```

**Resposta esperada — HTTP 200:**

```json
{
  "messaging_product": "whatsapp",
  "contacts": [{ "input": "5581979125323", "wa_id": "558179125323" }],
  "messages": [{ "id": "wamid.HBgMNTU4MTc5MTI1MzIz..." }]
}
```

> 📌 **200 com `wamid` significa "aceita", não "entregue".** Confirme no celular. Uma
> falha posterior (número inválido, saldo, política) só apareceria via webhook, que o
> projeto não implementa.
>
> 📌 Repare que o `wa_id` volta **sem o 9º dígito** (`558179125323`). É o comportamento
> conhecido de números brasileiros antigos. Não afeta o envio — a Meta resolve o
> mapeamento. Mas se um dia for implementado webhook de resposta, o `wa_id` recebido
> **não vai bater** com o telefone salvo no banco.

Se este passo falhar, o problema é de credencial ou de allow-list, não do ProntuDigital.
Ver [seção 6](#6-erros-comuns).

---

## 4. Teste ponta a ponta pelo sistema

### 4.1 Preparar os dados

1. Cadastre (ou ajuste) um **paciente de teste** com telefone `81979125323`.

   > O `normalizarTelefone` remove tudo que não é dígito e prefixa `55` quando sobram
   > 11 dígitos ou menos (`WhatsappCloudApiClient:60-66`). Então `(81) 97912-5323`,
   > `81979125323` e `5581979125323` funcionam igual. O que **não** funciona é um
   > número que não esteja na allow-list da Meta.

2. Confirme que as notificações estão ligadas — `app.notificacoes.habilitadas` deve ser
   `true` (`application.yaml:37`). Com `false`, os três schedulers retornam imediatamente
   e nada acontece.

### 4.2 Lembrete de 48h

Crie um agendamento com início **~48h à frente** (status `AGENDADO` ou `REMARCADO`).

- Janela do scheduler: **46h a 50h** (`AgendamentoNotificacaoScheduler:35-38`)
- Frequência: **a cada 15 minutos**, no minuto cheio (`cron = "0 */15 * * * *"`)
- Guarda de idempotência: só envia uma vez por agendamento+tipo
  (`NotificacaoWhatsappServiceImpl:52-54`) — para reenviar, apague a linha do log

Acompanhe:

```bash
docker logs -f prontudigital-backend-dev | grep -iE "SCHEDULER 48h|LEMBRETE_48H|Cloud API"
```

**Esperado:** `[WHATSAPP][SCHEDULER 48h] 1 agendamento(s) encontrado(s) para lembrete`,
seguido de `[WHATSAPP] Cloud API aceitou a mensagem para 55*******8888: HTTP 200 em … ms`
e `[WHATSAPP][LEMBRETE_48H] Lembrete enviado para o agendamento …`, e a mensagem no celular.

### 4.3 Solicitação de confirmação

Crie um segundo agendamento com início daqui a **~24h** (a janela varrida é de 22h a 26h).

- Gatilho: **24h antes** do horário da consulta (janela de 22h–26h)
- Frequência: **a cada 15 minutos** (`cron = "0 */15 * * * *"`)
- A mensagem traz dois links, montados a partir de `app.notificacoes.url-base-confirmacao`
- O token expira **2h antes** do horário da consulta; se o agendamento for criado com
  menos de 2h de antecedência, o link vale até o horário da consulta

Acompanhe:

```bash
docker logs -f prontudigital-backend-dev | grep -iE "SCHEDULER 24h|\[CONFIRMACAO\]|Cloud API"
```

> ⚠️ Em dev os links apontam para `http://localhost:8080/api/confirmacao/...`
> (`application.yaml:38`), que **não abre no celular**. É esperado. Para testar o fluxo,
> copie o token do banco e chame o endpoint da sua máquina (passo 4.4).
>
> Em produção isso é uma **pendência aberta** — ver `WHATSAPP.md` §11.

### 4.4 Confirmar / recusar

```bash
# pegar o token
docker exec -it prontudigital-db-dev psql -U user_admin -d prontudigital \
  -c "SELECT id, agendamento_id, tipo, status, token_confirmacao, token_expira_em
      FROM log_notificacoes_whatsapp
      WHERE tipo = 'CONFIRMACAO_24H' ORDER BY id DESC LIMIT 1;"
```

```bash
# confirmar presenca
curl -i "http://localhost:8080/api/confirmacao/<TOKEN>/confirmar"

# ou recusar (cancela o agendamento e aciona a fila de espera)
curl -i "http://localhost:8080/api/confirmacao/<TOKEN>/recusar"
```

São endpoints **públicos**, sem autenticação (`ConfirmacaoAgendamentoController`).

**Verificar o efeito:**

| Ação | `log_notificacoes_whatsapp.status` | `agendamentos.status` |
|---|---|---|
| `/confirmar` | `CONFIRMADO` | `CONFIRMADO` |
| `/recusar` | `RECUSADO` | cancelado + fila de espera acionada |
| Token usado 2x | erro `TokenConfirmacaoInvalidoException` | inalterado |
| Após `token_expira_em` | `EXPIRADO` + erro | inalterado |

### 4.5 Acompanhar os logs do fluxo

Todo o caminho da mensagem é logado com o prefixo `[WHATSAPP]`, então um único grep
mostra o fluxo inteiro:

```bash
docker logs -f prontudigital-backend-dev | grep "\[WHATSAPP\]"
```

| Prefixo | Etapa |
|---|---|
| `[WHATSAPP][SCHEDULER …]` | varredura periódica: janela consultada, quantos agendamentos, quantos com erro |
| `[WHATSAPP][LEMBRETE_48H]` | montagem e envio do lembrete |
| `[WHATSAPP][CONFIRMACAO]` | geração do token, validade do link e envio da solicitação |
| `[WHATSAPP]` (cliente) | chamada HTTP à Cloud API: status, tempo de resposta e corpo do erro |
| `[WHATSAPP][LINK]` / `[WHATSAPP][RESPOSTA]` | clique do paciente no link e efeito no agendamento |
| `[WHATSAPP][EXPIRACAO]` | cancelamento por falta de confirmação e volta para a fila de espera |
| `[WHATSAPP][RECUPERACAO-SENHA]` | código de recuperação de senha enviado por WhatsApp |

Em dev o nível `DEBUG` já vem ligado para esses pacotes (`application.yaml`), o que
acrescenta o conteúdo da mensagem, a resposta crua da Meta e os motivos de cada
agendamento ter sido ignorado. Em produção o padrão é `INFO`; para depurar, suba
`LOG_LEVEL_WHATSAPP=DEBUG`.

> 🔒 Telefones aparecem mascarados (`55*******8888`) e os tokens de confirmação
> só nos 8 primeiros caracteres — os logs não expõem o número do paciente nem o
> link clicável.

---

## 5. Conferir o resultado no banco

```bash
docker exec -it prontudigital-db-dev psql -U user_admin -d prontudigital -c \
"SELECT id, agendamento_id, tipo, status, telefone, enviado_em, entregue_em, lido_em,
        erro_codigo, respondido_em
 FROM log_notificacoes_whatsapp ORDER BY id DESC LIMIT 10;"
```

| `status` | Significado |
|---|---|
| `PENDENTE` | registro criado, envio ainda não tentado |
| `ENVIADO` | a Meta **aceitou** (HTTP 200) — ainda não é entrega |
| `FALHA` | exceção no envio, ou a Meta reportou `failed` no webhook (veja `erro_codigo`) |
| `CONFIRMADO` / `RECUSADO` | paciente respondeu pelo link |
| `EXPIRADO` | link acessado depois de `token_expira_em` |

`entregue_em` e `lido_em` só são preenchidos com o webhook configurado (seção 7).
**`ENVIADO` com `entregue_em` nulo é exatamente o sintoma de mensagem que não chegou.**

> 🔇 **`FALHA` não interrompe nada.** O `try/catch`
> (`NotificacaoWhatsappServiceImpl:87` e `171`) grava o status e segue — o agendamento
> não é afetado e a API não devolve erro nenhum. O motivo, porém, sempre aparece como
> `log.error` com o `[WHATSAPP]` correspondente (inclusive o corpo da resposta da Meta).

Para repetir um teste, apague o registro (a guarda de idempotência bloqueia o reenvio):

```sql
DELETE FROM log_notificacoes_whatsapp WHERE agendamento_id = <ID>;
```

---

## 6. Erros comuns

| Sintoma | Causa provável | Correção |
|---|---|---|
| `FALHA` com *"WhatsApp não configurado"* | `.env` vazio ou variável não chegou ao container | `docker exec prontudigital-backend-dev printenv \| grep WHATSAPP` |
| HTTP 401, código **190** | **Token expirou (24h)** | Gerar outro na *Etapa 1* |
| Código **131047** | Fora da janela de 24h e sem template aprovado | Em dev: responda à mensagem pelo celular para abrir a janela. Em produção: exige template (`WHATSAPP.md` §7) |
| Código **131030** | Destinatário fora da allow-list | Cadastrar em *Etapa 1 → Gerenciar lista de números* |
| HTTP 200 mas nada chega | Aceito pela Meta e descartado depois — quase sempre a janela de 24h (131047) | Configurar o webhook (seção 7); é ele que revela o motivo real |
| Nenhum log de scheduler | `app.notificacoes.habilitadas=false`, ou agendamento fora da janela | Conferir o horário do agendamento |
| Scheduler roda mas não envia | Já existe registro para agendamento+tipo | `DELETE` do log |

---

## 7. Webhook de status de entrega

Sem ele, `HTTP 200` é tudo o que se sabe — e 200 significa apenas que a Meta
**aceitou** a mensagem. A entrega, e principalmente a falha, chegam depois por
callback em `POST /api/whatsapp/webhook`.

### 7.1 Expor a URL

A Meta precisa alcançar a aplicação pela internet, com HTTPS. Em dev, um túnel:

```bash
ngrok http 9090          # ou: cloudflared tunnel --url http://localhost:9090
```

### 7.2 Configurar

No `.env` (`docker/dev/.env`):

```bash
WHATSAPP_WEBHOOK_VERIFY_TOKEN=qualquer-texto-que-voce-escolher
WHATSAPP_APP_SECRET=            # Meta → Configurações do app → Básico
```

`WHATSAPP_APP_SECRET` vazio faz o backend **aceitar callbacks sem conferir a
assinatura** (e avisar no log). Serve para destravar o teste em dev; em produção
as duas variáveis são obrigatórias e a aplicação não sobe sem elas.

Reinicie o backend e cadastre no painel da Meta — *App → WhatsApp → Configuração
→ Webhook → Editar*:

| Campo | Valor |
|---|---|
| URL de callback | `https://<seu-tunel>/api/whatsapp/webhook` |
| Verificar token | o mesmo `WHATSAPP_WEBHOOK_VERIFY_TOKEN` |
| Campos | assinar **`messages`** |

Ao salvar, a Meta faz um `GET` de handshake. No log:
`[WHATSAPP][WEBHOOK] Handshake de verificacao aceito`.

Para testar o handshake sem o painel:

```bash
curl -i "http://localhost:9090/api/whatsapp/webhook?hub.mode=subscribe\
&hub.verify_token=qualquer-texto-que-voce-escolher&hub.challenge=12345"
# 200 e o corpo "12345" = ok; 403 = token divergente
```

### 7.3 O que passa a aparecer

```bash
docker logs -f prontudigital-backend-dev | grep "\[WHATSAPP\]\[WEBHOOK\]"
```

| Log | Significado |
|---|---|
| `mensagem ENTREGUE no aparelho de 55*******5323` | chegou; grava `entregue_em` |
| `mensagem LIDA por …` | o paciente abriu; grava `lido_em` |
| `a Meta NAO entregou a mensagem … codigo 131047` | **não chegou**; grava `FALHA` + `erro_codigo`/`erro_detalhe` |
| `Mensagem recebida de … janela de 24h aberta` | o paciente escreveu para a clínica |

A correlação é feita pelo `wamid` que a Meta devolve no envio e que agora fica em
`log_notificacoes_whatsapp.mensagem_id`.

> Um `failed` que chegue depois de o paciente já ter confirmado ou recusado
> registra o erro mas **não** sobrescreve `CONFIRMADO`/`RECUSADO`.

> ⚠️ Registros gravados antes desta versão não têm `mensagem_id`; os callbacks
> deles caem em `Status '…' para o wamid …, que nao esta no log` (DEBUG) e são
> ignorados. Só vale para mensagens enviadas daqui em diante.

---

## 8. O que este teste **não** cobre

Passar aqui **não** significa que vai funcionar com paciente real. As diferenças:

- **Texto livre vs. template.** `enviarMensagemTexto()` manda `"type": "text"`
  (`WhatsappCloudApiClient:44-48`). Com o número de teste e a janela aberta isso passa;
  com paciente real que nunca respondeu, a Meta recusa com **131047**.
  → `WHATSAPP.md` §7 e §11.
- **URL de confirmação.** Em dev é `localhost`. Em produção precisa ser o domínio real —
  hoje `application-prod.yaml` não sobrescreve a chave. → `WHATSAPP.md` §11.
- **Forma de pagamento.** Número de teste é grátis. Em produção, sem cartão cadastrado na
  WABA o envio simplesmente para. → `WHATSAPP.md` §8.
- **Retentativa.** Não existe. Uma instabilidade momentânea da Meta = lembrete perdido em
  definitivo. O webhook agora **registra** a falha (`FALHA` + `erro_codigo`), mas ninguém
  reenvia. → `WHATSAPP.md` §11.
