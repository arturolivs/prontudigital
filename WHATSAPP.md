# 📱 WhatsApp no ProntuDigital — quem configura o quê

> Documento operacional. Explica a divisão de responsabilidades entre **você (dev do
> ProntuDigital)** e **a clínica cliente** na integração com a WhatsApp Cloud API,
> o passo a passo de cada lado, e o que ainda falta no código.
>
> Última revisão: **14/08/2026**. As regras e os preços da Meta mudam com frequência —
> reconfira na [documentação oficial](https://developers.facebook.com/docs/whatsapp/cloud-api)
> antes de fechar contrato com um cliente.

---

## 1. Resposta curta às duas perguntas

**"O número comercial é da clínica?"**
Sim. O número, a conta e a fatura são **da clínica**. Cada instalação do ProntuDigital
roda em modelo silo (uma VPS, um banco, um `.env` por cliente), então cada clínica
aponta o backend dela para o **próprio** número.

**"Eu preciso da minha conta de desenvolvedor Meta com meu número?"**
Sim, mas **só para desenvolver e testar** — nunca para produção de cliente. A Meta dá um
número de teste gratuito por App; ele envia apenas para até 5 destinatários que você
cadastra na mão. É com ele que você valida o `WhatsappCloudApiClient` sem gastar nada e
sem depender de cliente nenhum.

O que **não** deve acontecer: você criar um App na sua conta pessoal e colocar todos os
clientes atrás dele. Isso faria você virar o responsável legal pelas mensagens de
terceiros perante a Meta, com o número da sua conta bloqueado se **qualquer** cliente
levar denúncias de spam.

**Sim, você precisa informar coisas ao cliente** — e a mais importante delas está na
[seção 5](#5-o-aviso-que-o-cliente-precisa-ouvir-antes-de-assinar): o número que ele
conectar à API **para de funcionar no aplicativo WhatsApp Business do celular**.

---

## 2. Vocabulário da Meta (sem isso o painel não faz sentido)

| Termo | O que é | De quem é |
|---|---|---|
| **Conta de desenvolvedor Meta** | Login em `developers.facebook.com`. Qualquer conta do Facebook vira uma. | Sua (dev) e a do cliente, separadas |
| **Meta Business Portfolio** (antigo Business Manager) | O "CNPJ" dentro da Meta. Agrupa contas, números e ativos. | **Da clínica** |
| **App** | O aplicativo criado no painel de desenvolvedor, com o produto "WhatsApp" adicionado. Gera as credenciais. | **Da clínica** (criado por você, com acesso a você) |
| **WABA** (WhatsApp Business Account) | A conta WhatsApp de negócio vinculada ao Business Portfolio. Guarda os templates e a reputação. | **Da clínica** |
| **Phone Number ID** | ID numérico do número dentro da WABA. **Não é o telefone** — é o identificador interno. Vai em `WHATSAPP_PHONE_NUMBER_ID`. | Da clínica |
| **Access Token** | Credencial de envio. Vai em `WHATSAPP_ACCESS_TOKEN`. | Da clínica |
| **Template** (message template) | Modelo de mensagem pré-aprovado pela Meta. Obrigatório para iniciar conversa. Ver [seção 7](#7-a-regra-das-24h--o-ponto-mais-importante-deste-documento). | Da clínica |
| **BSP** (Business Solution Provider) | Revendedor oficial (Twilio, 360dialog, Infobip…). Simplifica o onboarding, cobra por cima do preço da Meta. | Opcional, ver [seção 10](#10-alternativas-à-integração-direta) |

---

## 3. Os dois modelos possíveis — e por que só um serve aqui

### Modelo A — cada clínica é dona dos próprios ativos ✅ **(o que você deve usar)**

Cada cliente tem seu Business Portfolio, sua WABA, seu número e seu App. Você entra como
**administrador/desenvolvedor convidado** no App dele, pega as credenciais, e coloca no
`.env` daquela instalação.

- ✅ Coerente com o modelo silo: um cliente, uma instalação, um número, um `.env`
- ✅ A fatura da Meta vai direto para o cartão do cliente — você não intermedia dinheiro
- ✅ Um cliente com má reputação não derruba os outros
- ✅ Zero burocracia de parceria com a Meta
- ⚠️ Exige acompanhar o cliente no onboarding (~1h, uma vez por cliente)

### Modelo B — você como Tech Provider ❌ **(não vale a pena agora)**

Você teria um App único, viraria **Tech Provider** da Meta, e as clínicas conectariam as
WABAs delas via *Embedded Signup* dentro do ProntuDigital.

- ❌ Exige verificação de empresa **sua**, App Review e aceite de termos de Tech Provider
- ❌ Exige multi-tenancy no código — hoje as credenciais são `@Value` de env var, uma só por
  instância (`WhatsappCloudApiClient:21-32`). Suportar N clínicas exigiria mover as
  credenciais para o banco e resolvê-las por requisição
- ❌ Faz sentido a partir de dezenas de clientes, não com 2–5

> **Decisão registrada:** ProntuDigital usa o **Modelo A**. O código atual (uma credencial
> por instância, via variável de ambiente) já está correto para ele — não mexa nisso.

---

## 4. Passo a passo — VOCÊ (dev), uma única vez

Isso é para o seu ambiente de desenvolvimento. Não repete por cliente.

1. Acesse `developers.facebook.com` → **Meus Apps** → **Criar App**.
2. Na tela **"Casos de uso"**, escolha **`Outro`** — a opção solta embaixo de
   *"Procurando outra coisa?"*, marcada com o aviso *"This option is going away soon"*.
   Depois, tipo de app **Empresa/Business**.

   > ⚠️ **NÃO escolha o caso de uso "Conectar-se com os clientes pelo WhatsApp"**
   > (filtro *Business Messaging*), por mais que o nome pareça o certo. Esse caso de uso
   > monta o app para o fluxo de **Tech Provider / Embedded Signup** — o [Modelo B](#modelo-b--você-como-tech-provider--não-vale-a-pena-agora)
   > que este documento descarta. Ele adiciona **Login do Facebook para Empresas** no lugar
   > do produto WhatsApp, e o painel resultante **não tem número de teste, Phone Number ID
   > nem token**. Só a "experiência antiga" (`Outro`) dá acesso à lista
   > *"Adicionar produtos ao seu app"*.
   >
   > Se você já criou o app pelo caminho errado, não tente consertar: crie outro.

3. No painel do App, em **"Adicionar produtos ao seu app"**, escolha **WhatsApp** → **Configurar**.
4. A Meta cria automaticamente uma WABA de teste e um **número de teste** (norte-americano,
   gratuito, mensagens ilimitadas para destinatários cadastrados).
5. Em *WhatsApp → **Etapa 1. Experimente*** (o nome atual da antiga "Configuração da API"),
   cadastre **seu celular** no campo `Para:` → *"Gerenciar lista de números de telefone"*
   (até 5 números). Você recebe um código de verificação no WhatsApp.
   Mantenha o app em **Modo: desenvolvimento** — é ele que garante o número de teste gratuito.
6. Copie o **Phone Number ID** e gere o **token temporário** (validade de 24h — serve para dev).
7. Coloque no `docker/dev/.env`:

   ```env
   WHATSAPP_PHONE_NUMBER_ID=123456789012345
   WHATSAPP_ACCESS_TOKEN=EAAG...
   ```

8. Rode o backend e dispare um envio. Note que **com o número de teste você consegue
   mandar texto livre** para os destinatários cadastrados — o que mascara o problema
   descrito na [seção 7](#7-a-regra-das-24h--o-ponto-mais-importante-deste-documento).
   Não confunda "funcionou em dev" com "vai funcionar em produção".

> 📘 O roteiro detalhado de validação — com os comandos, as consultas ao
> `log_notificacoes_whatsapp` e a tabela de erros comuns — está em
> [`TESTE_WHATSAPP_DEV.md`](./TESTE_WHATSAPP_DEV.md).

> ⚠️ O token temporário expira em 24h. Se o envio parar de funcionar do nada em dev, é isso.
> Para um token de dev que dura, crie um **usuário do sistema** (seção 5, passo 7) na sua
> própria WABA de teste.

---

## 5. O aviso que o cliente precisa ouvir ANTES de assinar

Este é o item que mais gera atrito depois — diga na proposta comercial, não na implantação:

> **O número conectado à API não funciona mais no aplicativo WhatsApp Business do celular.**
> Ao migrar um número para a Cloud API, ele deixa de abrir no app. Toda a comunicação
> passa a ser via sistema.

Consequência prática, e a recomendação:

| Cenário | Recomendação |
|---|---|
| A clínica **usa o número comercial no dia a dia** para conversar com pacientes | **Contrate um chip novo, exclusivo para o sistema.** Um pré-pago resolve. Divulgue-o como "número de avisos automáticos — não responda". Custa ~R$ 15/mês e evita todo o problema |
| A clínica **não usa** o número comercial para conversar (só recebe ligação) | Pode migrar o número existente |
| A clínica **quer os dois** no mesmo número | A Meta tem um recurso de *coexistência* (app WhatsApp Business + Cloud API no mesmo número). Verifique disponibilidade e restrições antes de prometer — não conte com isso |

Outros pontos a comunicar:

- **O número precisa estar livre.** Se já existe uma conta WhatsApp comum ou Business nele,
  ela precisa ser apagada antes de registrar na API. Isso **apaga o histórico de conversas**
  daquele número.
- **O número não pode ser 0800 nem ramal VoIP** que não receba SMS ou chamada de voz — a
  verificação da Meta exige um dos dois.
- **A Meta cobra por mensagem.** Ver [seção 8](#8-custos). O cartão é da clínica.
- **Templates precisam de aprovação prévia.** Mudar o texto do lembrete não é mexer numa
  configuração do sistema: é submeter um template novo e esperar a Meta aprovar.

---

## 6. Passo a passo — O CLIENTE (com você conduzindo)

Reserve ~1h com alguém que tenha poder de decisão na clínica (precisa de CNPJ, cartão e
acesso ao celular do número).

**Antes da reunião, peça ao cliente:**
- [ ] CNPJ e razão social exatos (como no cartão CNPJ)
- [ ] Cartão de crédito internacional em nome da empresa
- [ ] O celular com o chip do número que vai ser usado, em mãos
- [ ] Definir o **nome de exibição** (aparece para o paciente). Precisa ter relação com a
      marca — "Clínica Passo Firme" passa; "Avisos" é reprovado

**Durante a reunião:**

1. **Business Portfolio** — em `business.facebook.com`, criar o portfólio da clínica com
   CNPJ e razão social. Se a clínica já tem página no Facebook/Instagram, usar o portfólio
   existente.
2. **App** — em `developers.facebook.com`, com o login **do cliente**, criar App tipo
   Empresa e vinculá-lo ao portfólio do passo 1.
3. **Produto WhatsApp** — adicionar ao App e criar a WABA de produção.
4. **Registrar o número** — adicionar o número da clínica, escolher o nome de exibição e
   verificar por SMS ou chamada.
5. **Você entra como desenvolvedor** — o cliente adiciona seu usuário Meta como
   *Desenvolvedor* ou *Administrador* do App. É isso que permite você dar manutenção sem
   pedir senha ao cliente toda vez.
6. **Verificação da empresa** — enviar contrato social ou cartão CNPJ. Leva de horas a
   alguns dias. Enquanto não sai, o número fica limitado a **250 destinatários únicos por
   24h** — o que, para uma clínica de 2 a 5 profissionais, já é mais que suficiente para
   operar. Não trave a implantação esperando isso.
7. **Token permanente** — em *Configurações do negócio → Usuários → Usuários do sistema*,
   criar um usuário do sistema tipo **Admin**, dar acesso à WABA e ao App, e gerar token
   com as permissões `whatsapp_business_messaging` e `whatsapp_business_management`.
   **Marque "nunca expira".**

   > 🔐 Esse token é uma credencial de produção. Copie direto para o `.env` do servidor.
   > Não passe por WhatsApp, e-mail ou planilha, e não commite.

8. **Forma de pagamento** — cadastrar o cartão da clínica na WABA. **Sem isso o envio para
   por saldo insuficiente**, e a falha aparece só como `FALHA` no `log_notificacoes_whatsapp`.
9. **Templates** — submeter os dois templates da [seção 7](#7-a-regra-das-24h--o-ponto-mais-importante-deste-documento).
10. **Credenciais** — anotar `Phone Number ID` e o token, e preencher o `.env` da instalação.

---

## 7. A regra das 24h — o ponto mais importante deste documento

A Meta divide as mensagens em duas categorias:

| | Quando | Formato permitido | Custo |
|---|---|---|---|
| **Iniciada pelo cliente** (*service*) | Até 24h após o paciente mandar uma mensagem | Texto livre | Gratuito |
| **Iniciada pela empresa** | Fora da janela de 24h | **Somente template aprovado** | Pago por mensagem |

**Lembrete de 48h e confirmação de 24h são sempre iniciados pela empresa.** O paciente não
mandou nada antes — o scheduler é que disparou (`AgendamentoNotificacaoScheduler:30` e `:52`).

### ⚠️ Implicação para o código atual

`WhatsappCloudApiClient.enviarMensagemTexto()` monta `"type": "text"` — texto livre
(`WhatsappCloudApiClient.java:44-48`). Em produção, para um paciente que nunca respondeu,
a Meta recusa com **erro 131047** ("mais de 24 horas desde a última resposta do cliente").

O resultado no ProntuDigital não é uma explosão visível: o `try/catch` do
`NotificacaoWhatsappServiceImpl` (linhas 81-86 e 136-141) grava `StatusNotificacao.FALHA`
e segue. **O sistema parece funcionar e nenhuma mensagem chega.** Pior: o scheduler de
expiração (`processarNaoConfirmados`) só cancela agendamentos cuja notificação está
`ENVIADO`, então nada é cancelado indevidamente — o efeito é silêncio total, não caos.

**Isso precisa ser corrigido antes do primeiro cliente real.** Ver
[seção 11](#11-pendências-no-código).

### Os dois templates a submeter

Categoria **Utilidade** (`utility`) nos dois casos — é a categoria certa para transação e
a mais barata. Se você escrever algo promocional junto, a Meta reclassifica como Marketing
e o custo sobe ~8x.

**Template 1 — `lembrete_consulta_48h`**

```
Olá, {{1}}! Lembrete: você tem uma consulta com {{2}} marcada para {{3}}.
Em caso de dúvidas, entre em contato conosco.
```
Parâmetros: `{{1}}` nome do paciente, `{{2}}` nome do profissional, `{{3}}` data/hora formatada.

**Template 2 — `confirmacao_consulta_24h`** (com dois botões de URL dinâmica)

```
Corpo:
Olá, {{1}}! Sua consulta com {{2}} está marcada para {{3}}.
Por favor, confirme sua presença nos botões abaixo.
Se não responder até 2h antes do horário, a vaga será liberada.

Botão 1 (URL dinâmica) — "Confirmar"
  https://SEU-DOMINIO/api/confirmacao/{{1}}     → parâmetro: <token>/confirmar

Botão 2 (URL dinâmica) — "Cancelar"
  https://SEU-DOMINIO/api/confirmacao/{{1}}     → parâmetro: <token>/recusar
```

Isso preserva exatamente o fluxo já implementado em `ConfirmacaoAgendamentoController`
(`GET /api/confirmacao/{token}/confirmar` e `/recusar`) — só muda o transporte da
mensagem. **O domínio base do botão fica fixo no template**, o que significa um template
por instalação/domínio de cliente. É uma consequência aceitável do modelo silo.

> Alternativa futura: botões de *resposta rápida* em vez de URL. Fica mais elegante para o
> paciente (não abre navegador), mas exige implementar um **webhook** para receber a
> resposta — endpoint público, validação de assinatura `X-Hub-Signature-256` e verificação
> do token de challenge. Não vale o esforço agora.

---

## 8. Custos

Desde julho de 2025 a Meta cobra **por mensagem entregue**, não mais por conversa de 24h.

| Item | Custo aproximado (Brasil) |
|---|---|
| Conta, App, WABA, número de teste | **Gratuito** |
| Mensagem de **serviço** (resposta dentro da janela de 24h) | **Gratuita** |
| Template de **utilidade** (lembrete, confirmação) | ~US$ 0,008 por mensagem |
| Template de **marketing** | ~US$ 0,06 por mensagem — **não use** |
| Template de **autenticação** | ~US$ 0,03 por mensagem |

**Estimativa para uma clínica de 2 profissionais:** ~8 consultas/dia × 22 dias = ~176
consultas/mês × 2 mensagens (lembrete + confirmação) = ~352 mensagens de utilidade
≈ **US$ 2,80/mês (~R$ 15)**. É irrelevante frente ao custo da VPS — mas o cartão precisa
estar cadastrado, senão o envio simplesmente para.

> Templates de utilidade enviados **dentro** de uma janela de atendimento aberta costumam
> ser isentos, e a Meta mantém faixas de isenção que mudam de tempos em tempos. Confirme
> em `developers.facebook.com/docs/whatsapp/pricing` — não repasse esses números ao cliente
> como se fossem contratuais.

---

## 9. Configuração no ProntuDigital

### Variáveis de ambiente

| Variável | Onde é lida | Valor |
|---|---|---|
| `WHATSAPP_PHONE_NUMBER_ID` | `application.yaml:45`, `application-prod.yaml:52` | Phone Number ID da WABA do cliente |
| `WHATSAPP_ACCESS_TOKEN` | `application.yaml:46`, `application-prod.yaml:53` | Token permanente do usuário do sistema |
| `APP_NOTIFICACOES_URL_BASE_CONFIRMACAO` | `application.yaml:38` | `https://SEU-DOMINIO/api/confirmacao` |
| `APP_NOTIFICACOES_HABILITADAS` | `application.yaml:37` | `true` \| `false` |

### Modo degradado (implantar sem WhatsApp)

O sistema **funciona sem WhatsApp**. Duas formas de desligar:

- `APP_NOTIFICACOES_HABILITADAS=false` → os três schedulers retornam imediatamente
  (`AgendamentoNotificacaoScheduler:32,54,76`). **Atenção:** isso também desliga
  `processarNaoConfirmados`, ou seja, ninguém cancela agendamento por falta de confirmação.
  É o comportamento correto — sem aviso enviado, não pode haver punição por não responder.
- Deixar `WHATSAPP_PHONE_NUMBER_ID` e `WHATSAPP_ACCESS_TOKEN` vazios em **dev** → o cliente
  lança `IllegalStateException` (`WhatsappCloudApiClient:40-42`), o service captura e grava
  `FALHA` no log. O agendamento não é afetado.

Isso permite implantar a clínica na segunda-feira e resolver a burocracia da Meta na
semana seguinte, sem bloquear o resto do sistema. **Use isso como estratégia de
implantação**: entregue o sistema funcionando, ative o WhatsApp depois.

---

## 10. Alternativas à integração direta

| Opção | Quando considerar | Ressalva |
|---|---|---|
| **Cloud API direta** (atual) | Padrão. Mais barato, sem intermediário | Onboarding manual por cliente |
| **BSP oficial** (360dialog, Twilio, Infobip) | Cliente que não quer lidar com painel da Meta | Cobra por cima do preço da Meta; ainda precisa do número dedicado |
| **API não oficial** (Z-API, Evolution API, Baileys) | ❌ **Não use em produção** | Viola os Termos de Serviço da Meta. O número **é banido**, sem aviso e sem recurso. Para um sistema de saúde vendido a clínicas, é risco de responsabilidade sua, não do cliente |

Trocar a Cloud API por um BSP é uma mudança **local** ao `WhatsappCloudApiClient` — o resto
do módulo `notificacao` não sabe qual provedor está por baixo. Se um cliente exigir BSP,
é uma classe nova implementando o mesmo contrato, não uma refatoração.

---

## 11. Pendências no código

Levantadas ao escrever este documento. Nenhuma impede o desenvolvimento, todas impedem o
primeiro cliente real.

### Resolvidas (branch `config-whatsapp`, 26/08/2026)

- [x] **`docker-compose.prod.yml` repassa as variáveis de WhatsApp ao backend**
      (`docker/prod/docker-compose.prod.yml:64-65`). Antes o Spring não resolvia o
      placeholder de `application-prod.yaml` e a aplicação **não subia** em produção.
- [x] **`app.notificacoes.url-base-confirmacao` sobrescrito em produção.**
      `application-prod.yaml:49` lê `${APP_URL_BASE_CONFIRMACAO}`, derivada de `DOMINIO`
      no compose (`docker-compose.prod.yml:66`). Antes os links enviados ao paciente
      apontariam para o `localhost` **dele**.
- [x] **`.env.prod.example` com a seção de WhatsApp** (`docker/prod/.env.prod.example:22-23`).
- [x] **Ambiente de dev repassa as credenciais.** `docker/dev/docker-compose.yml:75-76` e
      `docker/dev/.env.dev.example`. Sem isso, preencher o `.env` não tinha efeito nenhum.

### Abertas

- [ ] **Envio por template em vez de texto livre.** Ver [seção 7](#7-a-regra-das-24h--o-ponto-mais-importante-deste-documento).
      Adicionar um `enviarTemplate(telefone, nomeTemplate, parametros)` ao
      `WhatsappCloudApiClient` e trocar as duas chamadas em `NotificacaoWhatsappServiceImpl`
      (linhas 77 e 132). **É a única pendência que bloqueia o primeiro cliente real.**
- [ ] **Retentativa.** O campo `tentativas` existe na entidade `LogNotificacaoWhatsapp` e é
      sempre gravado como `1`. Nada relê registros com status `FALHA`. Uma instabilidade
      momentânea da Meta hoje significa lembrete perdido em definitivo.
- [ ] **Versão da Graph API fixa no `.yaml`.** Hoje `v25.0`
      (`application.yaml:44`, `application-prod.yaml:53`). A Meta suporta cada versão por
      ~2 anos — vale revisar periodicamente, senão o envio quebra sem aviso.

---

## 12. Resumo de uma página (para colar na proposta comercial)

> **Integração WhatsApp — o que a clínica precisa providenciar**
>
> 1. Um **número de celular exclusivo** para o sistema (recomendamos um chip novo — o número
>    conectado deixa de funcionar no app WhatsApp Business do celular).
> 2. **CNPJ e contrato social** para verificação junto à Meta.
> 3. Um **cartão de crédito internacional** em nome da empresa, cadastrado na Meta. O custo
>    estimado é de **R$ 15 a R$ 30 por mês** para o volume de uma clínica de 2 a 5
>    profissionais, cobrado diretamente pela Meta.
> 4. **Uma reunião de aproximadamente 1 hora** com um responsável, para criação das contas.
>
> A conta e o número são **da clínica**, não do ProntuDigital. Isso significa que a clínica
> mantém o controle e a portabilidade do seu canal de comunicação com os pacientes.
>
> O sistema opera normalmente sem o WhatsApp ativo — apenas os lembretes e confirmações
> automáticas ficam indisponíveis até a conclusão do processo junto à Meta.
