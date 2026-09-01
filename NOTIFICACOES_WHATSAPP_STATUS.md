# 📋 Notificações por WhatsApp — o que foi feito e o que falta

Registro das mudanças feitas na branch `config-whatsapp` em **31/08/2026** e das
pendências que sobraram. Complementa dois documentos que continuam valendo:

- **`WHATSAPP.md`** — o porquê da integração, contas da Meta, custos, a regra das 24h.
- **`TESTE_WHATSAPP_DEV.md`** — o passo a passo para testar em desenvolvimento.

---

## 1. O que motivou tudo isto

Um agendamento era criado, o backend enviava a solicitação de confirmação, a Meta
respondia `HTTP 200`, o banco gravava `ENVIADO` — **e a mensagem não chegava no
celular.** Sem log e sem retorno de entrega, não havia como saber se o problema
estava no scheduler, no envio ou na Meta.

Três causas distintas apareceram na investigação, e as três foram tratadas.

---

## 2. Feito

### 2.1 Confirmação disparada 1 minuto após a criação

Antes a solicitação de confirmação saía numa janela de 22h–26h **antes da
consulta**. Agora sai logo após a criação do agendamento.

- `AgendamentoNotificacaoScheduler:86` — `processarConfirmacoesAgendamento`,
  `cron = "0 * * * * *"` (roda a cada minuto).
- Seleciona por `criadoEm`, não por `inicioEm`:
  `AgendamentoRepository.findPendentesDeConfirmacaoPorCriacao` (`:78`).
- Dois parâmetros novos em `application.yaml`:

  | Chave | Padrão | Papel |
  |---|---|---|
  | `app.notificacoes.confirmacao.atraso-minutos` | `1` | espera após a criação |
  | `app.notificacoes.confirmacao.janela-minutos` | `60` | janela retroativa varrida a cada execução — cobre o período em que a aplicação esteve fora do ar |

  Em produção: `APP_CONFIRMACAO_ATRASO_MINUTOS` e `APP_CONFIRMACAO_JANELA_MINUTOS`.

- Efeito colateral tratado: com o envio imediato, um agendamento marcado para dali
  a menos de 2h geraria um link **já expirado**. Nesse caso a validade passa a ser
  o horário da consulta, e a mensagem informa a data-limite real em vez do texto
  fixo *"até 2h antes"* (`NotificacaoWhatsappServiceImpl:112`).

### 2.2 Log em todo o fluxo

Prefixo único `[WHATSAPP]` — um `grep` mostra o caminho inteiro:

```bash
docker logs -f prontudigital-backend-dev | grep "\[WHATSAPP\]"
```

| Prefixo | Etapa |
|---|---|
| `[WHATSAPP][SCHEDULER …]` | janela consultada, quantos encontrou, quantos falharam |
| `[WHATSAPP][LEMBRETE_48H]` / `[CONFIRMACAO]` | montagem, token, envio, motivo de cada skip |
| `[WHATSAPP]` (cliente) | chamada HTTP: status, tempo e **corpo do erro da Meta** |
| `[WHATSAPP][LINK]` / `[RESPOSTA]` | clique do paciente e efeito no agendamento |
| `[WHATSAPP][EXPIRACAO]` | cancelamento por falta de confirmação |
| `[WHATSAPP][RECUPERACAO-SENHA]` | código de recuperação enviado por WhatsApp |

Pontos que antes eram cegos e agora aparecem:

- A resposta da Cloud API ia só para `log.debug` — invisível em produção, que roda
  em `INFO`. Agora sucesso e falha são `INFO`/`ERROR`, com o corpo do erro (é onde
  vêm os códigos `190`, `131047`, `131030`).
- Paciente sem telefone cadastrado causava falha silenciosa; agora é `WARN`.
- `WhatsappCloudApiClient` e `AutenticacaoServiceImpl` não tinham logger nenhum.
- Todo `log.error` passou a carregar a exceção, não só `e.getMessage()`.

**Privacidade:** telefones saem mascarados (`55*******5323`,
`WhatsappCloudApiClient.mascararTelefone:128`) e tokens de confirmação só nos 8
primeiros caracteres — os logs não expõem o número do paciente nem o link
clicável. O código de recuperação de senha nunca é logado.

**Níveis:** essencial em `INFO` (aparece em produção); diagnóstico em `DEBUG`. Dev
já sobe com `DEBUG` nos pacotes `notificacao` e `compartilhado.clientes`; em
produção, `LOG_LEVEL_WHATSAPP=DEBUG` liga sem alterar código.

### 2.3 Fuso horário — 3h de defasagem

Os logs expuseram um bug que ia muito além do WhatsApp. No **mesmo `INSERT`**:

| coluna | valor gravado | origem |
|---|---|---|
| `criado_em` | `2026-09-01T00:26:00` | `@CreationTimestamp` → fuso da JVM = **UTC** |
| `enviado_em` | `2026-08-31T21:26:00` | `LocalDateTime.now(clock)` → `ClockConfig` = **America/Sao_Paulo** |

O container não definia `TZ`, então a JVM caía em UTC enquanto o `Clock` injetado
usava `America/Sao_Paulo`. A janela do scheduler era calculada em `-03` e comparada
com um `criado_em` em UTC — **nunca casaria**.

Correção: `TZ: America/Sao_Paulo` no serviço `backend` e `TZ` + `PGTZ` no
`postgres`, nos dois compose. O banco entrou junto porque as colunas são
`TIMESTAMP` sem fuso e ~18 migrações usam `DEFAULT CURRENT_TIMESTAMP`, gravado pelo
relógio do banco.

O alcance é maior que o scheduler: são 28 pontos usando `@CreationTimestamp`,
`@UpdateTimestamp` ou `LocalDateTime.now()` sem `Clock` — inclusive o `@PreUpdate`
de `Agendamento` — todos 3h fora do resto do sistema. Provável explicação também do
`incoerencia-horarios.png` que está na raiz do repositório.

### 2.4 Webhook de status da Meta

`HTTP 200` significa apenas que a Meta **aceitou** a mensagem. A entrega — ou a
falha — só chega depois, por callback. Sem isso, "o paciente ignorou" e "a mensagem
nunca chegou" eram indistinguíveis.

**Endpoints** (`WhatsappWebhookController`, públicos no `SecurityConfig`):

- `GET /api/whatsapp/webhook` — handshake da Meta; devolve o `hub.challenge` se o
  verify token bater, senão `403`.
- `POST /api/whatsapp/webhook` — recebe os status. Valida o `X-Hub-Signature-256`
  (HMAC-SHA256 do corpo bruto, comparação em tempo constante) e responde **200
  mesmo em erro de processamento**, para a Meta não reentregar em laço.

**Correlação:** o gargalo era não existir ligação entre a mensagem enviada e o
callback. `enviarMensagemTexto` (`:60`) passou a devolver o `wamid`, gravado em
`log_notificacoes_whatsapp.mensagem_id` — migração
**`V30__adiciona_rastreio_entrega_whatsapp.sql`**, que também trouxe `entregue_em`,
`lido_em`, `erro_codigo` e `erro_detalhe`.

| Status da Meta | Efeito |
|---|---|
| `sent` | preenche `enviado_em` se vazio |
| `delivered` | `entregue_em` |
| `read` | `lido_em` (e `entregue_em`, se o `delivered` não chegou) |
| `failed` | `status = FALHA` + código e motivo, em `log.error` |
| mensagem recebida do paciente | `INFO` avisando que a janela de 24h abriu |

No `failed` com `131047` o próprio log explica o que fazer. Um `failed` que chegue
depois de o paciente ter clicado no link **não** sobrescreve
`CONFIRMADO`/`RECUSADO`.

Efeito na regra de negócio: `processarNaoConfirmados` (`:283`) só cancela
agendamentos cuja notificação está `ENVIADO`. Com o `failed` registrado, o paciente
deixa de perder a consulta por uma falha de envio nossa.

**Configuração:**

| Variável | Dev | Produção |
|---|---|---|
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | opcional | **obrigatória** |
| `WHATSAPP_APP_SECRET` | vazio = assinatura não conferida (`WARN` no log) | **obrigatória** |

Em produção não há valor padrão de propósito: a aplicação não sobe sem eles, porque
o endpoint é público e sem o app secret qualquer um forjaria um status de entrega.

### 2.5 Arquivos

**Criados**

```
backend/src/main/java/.../notificacao/controladores/WhatsappWebhookController.java
backend/src/main/java/.../notificacao/dto/WhatsappWebhookDTO.java
backend/src/main/java/.../notificacao/servicos/WhatsappWebhookService.java
backend/src/main/java/.../notificacao/servicos/impl/WhatsappWebhookServiceImpl.java
backend/src/main/resources/db/migracoes/V30__adiciona_rastreio_entrega_whatsapp.sql
backend/src/test/java/.../notificacao/servicos/impl/WhatsappWebhookServiceImplTest.java
```

**Alterados:** `AgendamentoRepository`, `AgendamentoNotificacaoScheduler`,
`NotificacaoWhatsappServiceImpl`, `LogNotificacaoWhatsapp`,
`LogNotificacaoWhatsappRepository`, `WhatsappCloudApiClient`,
`ConfirmacaoAgendamentoController`, `AutenticacaoServiceImpl`, `SecurityConfig`,
`application.yaml`, `application-prod.yaml`, os dois `docker-compose`, os dois
`.env.*.example` e `TESTE_WHATSAPP_DEV.md`.

**Verificação:** 360 testes passam (14 novos para o webhook); a migração V30 foi
validada contra o Postgres de dev em transação com `ROLLBACK`.

---

## 3. Pendências

### 3.1 Bloqueia o primeiro cliente real

- [ ] **Envio por template em vez de texto livre.** É a pendência mais importante e
      já estava aberta em `WHATSAPP.md` §11. Hoje `enviarMensagemTexto` manda
      `"type": "text"`, que a Meta **só entrega dentro da janela de 24h** — ou seja,
      apenas para quem escreveu para a clínica nas últimas 24h. Para um paciente que
      nunca respondeu, a mensagem é descartada com `131047`. Falta um
      `enviarTemplate(telefone, nomeTemplate, parametros)` no cliente e trocar as
      duas chamadas no `NotificacaoWhatsappServiceImpl`.
      → `WHATSAPP.md` §7 tem os dois templates a submeter.

      *É também o que impede o teste em dev de funcionar sem antes mandar uma
      mensagem do celular para o número de teste.*

- [ ] **Nenhuma retentativa.** O campo `tentativas` existe e é sempre gravado como
      `1`. Nada relê registros em `FALHA`. O webhook agora **registra** a falha, mas
      ninguém reenvia — uma instabilidade momentânea da Meta continua significando
      lembrete perdido em definitivo.

### 3.2 Consequências das mudanças desta branch

- [ ] **Dados anteriores ao ajuste de `TZ` estão 3h deslocados.** As linhas gravadas
      antes ficaram em UTC; as novas ficam em `-03`. Não há como o código
      distinguir. Em dev, o caminho limpo é `docker compose down -v`. Em produção,
      **se já houver dados**, seria um `UPDATE ... - interval '3 hours'` nas tabelas
      afetadas — decisão que precisa ser sua, não automática.

- [ ] **Registros anteriores à V30 não têm `mensagem_id`.** Os callbacks deles caem
      em `Status '…' para o wamid …, que nao esta no log` (DEBUG) e são ignorados. O
      rastreio de entrega só vale para mensagens enviadas daqui em diante.

- [ ] **Frontend continua em UTC.** `node:20-alpine` não traz `tzdata`, então
      definir `TZ` no container exigiria um `apk add tzdata` no Dockerfile. Se
      alguma tela ainda mostrar horário divergente, é aqui.

### 3.3 Regras de negócio a decidir

- [ ] **`processarNaoConfirmados` x agendamento criado em cima da hora.** A rotina
      cancela o agendamento e devolve o paciente à fila de espera se não houver
      confirmação até 2h antes da consulta. Com o envio agora imediato, um
      agendamento criado com menos de 2h de antecedência pode receber a mensagem e
      ser cancelado quase em seguida, sem tempo hábil de resposta. Uma saída é só
      expirar se a notificação foi enviada há mais de X minutos.

- [ ] **Confirmação por resposta, não por link.** Como as mensagens recebidas já
      chegam pelo webhook, dá para aceitar *"responda 1 para confirmar"* em vez de
      depender do link clicável — hoje o ponto mais frágil do fluxo em produção.
      Nada disso está implementado: as mensagens recebidas são apenas logadas, não
      persistidas.

### 3.4 Operação

- [ ] **O webhook exige URL pública com HTTPS.** Em dev, um túnel
      (`ngrok http 9090`) e o cadastro no painel da Meta a cada nova URL.
      → `TESTE_WHATSAPP_DEV.md` §7.

- [ ] **Token de acesso de dev expira em 24h.** O da Etapa 1 do painel é temporário;
      quando expira, o envio falha com `190`. Para produção é preciso o token
      permanente de usuário do sistema (`WHATSAPP.md` §4).

- [ ] **Versão da Graph API fixa em `v25.0`** (`application.yaml`,
      `application-prod.yaml`). A Meta suporta cada versão por ~2 anos; sem revisão
      periódica o envio quebra sem aviso.

- [ ] **Sem teste do `WhatsappWebhookController`.** Os 14 testes novos cobrem o
      serviço (handshake, assinatura, os quatro status, payload inválido). O
      controller — mapeamento dos parâmetros `hub.*`, o `403` de assinatura inválida,
      o `200` mesmo em erro — não tem teste de `MockMvc`.

---

## 4. Próximo passo imediato

Para a mensagem finalmente chegar no celular, sem escrever mais nenhuma linha de
código:

1. Do celular cadastrado, mande qualquer mensagem para o número de teste da Meta —
   isso abre a janela de 24h.
2. `DELETE FROM log_notificacoes_whatsapp WHERE agendamento_id = <ID>;` para liberar
   o reenvio.
3. Em até 1 minuto o scheduler reenvia.

O passo 1 deixa de ser necessário quando os templates da pendência 3.1 estiverem no
lugar.
