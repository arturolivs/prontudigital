# Checklist de instalação em produção — ProntuDigital

> Cenário assumido: **VPS 4 GB, São Paulo, Docker** (decisão já tomada em
> [`ANALISE_DEPLOY.md`](./ANALISE_DEPLOY.md)). Este documento não repete a
> escolha de fornecedor — foca no que falta **no código, nas variáveis de
> ambiente e na operação do servidor** para o primeiro deploy não quebrar.
>
> Data do levantamento: **11/08/2026**, feito lendo o estado atual do repo
> (`docker/prod/*`, `application-prod.yaml`, `Dockerfile.prod` de backend e
> frontend). Reconfira se o código mudar depois desta data.

---

## Resumo executivo

| Categoria | Quantidade | Bloqueia o deploy? |
|---|---|---|
| Bloqueadores de código | 3 — **✅ corrigidos em 11/08/2026** (ver §1) | Não mais — falta só preencher as credenciais reais no `.env` |
| Variáveis de ambiente a preencher | 9 obrigatórias + 2 recomendadas | Sim |
| Pendências de infraestrutura/servidor | 6 | Parcial (app sobe, mas fica sem rede de proteção) |
| Pendências de segurança/LGPD | 3 | Não bloqueia tecnicamente, mas é prontuário de saúde |

---

## 1. Bloqueadores de código — ✅ corrigidos

> Os três itens abaixo foram aplicados no repo em 11/08/2026. Fica o
> registro do problema original e do que foi mudado — o que falta agora é
> só preencher valores reais no `.env` (§2), não mais editar código.

### 1.1 — Backend não inicia sem credenciais do WhatsApp — **CRÍTICO**

`backend/src/main/resources/application-prod.yaml:52-53`:

```yaml
whatsapp:
  cloud-api:
    phone-number-id: ${WHATSAPP_PHONE_NUMBER_ID}
    access-token:    ${WHATSAPP_ACCESS_TOKEN}
```

Sem `:` de valor padrão. Se `WHATSAPP_PHONE_NUMBER_ID` /
`WHATSAPP_ACCESS_TOKEN` não existirem no ambiente do container, o Spring
falha ao resolver o placeholder **no boot** (`IllegalArgumentException:
Could not resolve placeholder`) — o container `backend` entra em
crash-loop, não é só a função de WhatsApp que quebra, é a aplicação
inteira. (O default vazio em `WhatsappCloudApiClient:24-25` não salva: ele
só se aplica se a chave estiver **ausente**, e aqui ela existe com um
placeholder não resolvido.)

Hoje `docker/prod/docker-compose.prod.yml` (seção `backend.environment`)
**não passa essas duas variáveis**, e `docker/prod/.env.prod.example`
também não as lista.

**Por que isso importa além de subir:** a recuperação de senha
(`AutenticacaoServiceImpl.solicitarRecuperacaoSenha`) usa o WhatsApp Cloud
API como **único canal** — não existe fallback por e-mail no código. Sem
essas credenciais reais e válidas, ninguém recupera senha em produção.

**Correção aplicada:** `WHATSAPP_PHONE_NUMBER_ID` e `WHATSAPP_ACCESS_TOKEN`
já são repassadas ao container em `docker-compose.prod.yml`
(`backend.environment`) e listadas como placeholder em `.env.prod.example`.

**Ainda pendente — ação humana, não código:**
Criar um app no Meta for Developers, ativar o produto WhatsApp, pegar o
`phone_number_id` e gerar um **token permanente** (o token de teste padrão
expira em 24h — não serve para produção; token de usuário do sistema
— System User — é o correto). Sem preencher esses dois valores reais no
`docker/prod/.env`, o boot continua falhando, agora por env var vazia em
vez de ausente.

### 1.2 — Links de confirmação de agendamento apontam para `localhost` — **CRÍTICO**

`backend/src/main/resources/application.yaml:38`:

```yaml
notificacoes:
  url-base-confirmacao: "http://localhost:8080/api/confirmacao"
```

`application-prod.yaml` **não sobrescreve** essa propriedade. Como os
profiles do Spring fazem merge (prod não redefine a chave, o valor da
base vale), em produção o WhatsApp vai mandar para o paciente um link
literal `http://localhost:8080/api/confirmacao/{token}/confirmar`
(`NotificacaoWhatsappServiceImpl:104-105`) — inacessível fora do próprio
servidor.

**Correção aplicada:** `application-prod.yaml` agora sobrescreve
`app.notificacoes.url-base-confirmacao` a partir de
`APP_URL_BASE_CONFIRMACAO`; `docker-compose.prod.yml` injeta essa variável
como `https://${DOMINIO}/api/confirmacao`, reaproveitando o `DOMINIO` que
já existe no `.env` — nenhuma variável nova para preencher.

### 1.3 — Duas telas do frontend chamam `localhost:9090` em produção — **CRÍTICO**

`frontend-web/Dockerfile.prod` define como *build args* (embutidos no
bundle, não dá para trocar em runtime):

```
NEXT_PUBLIC_API_URL
NEXT_PUBLIC_API_BASE_URL
NEXT_PUBLIC_AGENDAMENTOS_API_URL
NEXT_PUBLIC_USUARIOS_API_URL
NEXT_PUBLIC_PRONTUARIO_API_URL
NEXT_PUBLIC_BLOQUEIOS_API_URL
NEXT_PUBLIC_PROCEDIMENTOS_API_URL
```

Faltam duas que o código já usa:

- `NEXT_PUBLIC_RELATORIOS_API_URL` (`lib/relatorio.service.ts:11`)
- `NEXT_PUBLIC_HORARIOS_TRABALHO_API_URL` (`lib/horarioTrabalho.service.ts:9`)

Sem elas, os dois serviços caem no fallback hardcoded
`http://localhost:9090/api/...`, que fica **congelado no bundle
JavaScript enviado ao navegador**. Resultado em produção: a página
`/relatorios` (Fase 4 — RF19/RF20/RF21) e a tela de horários de trabalho
(`/bloqueios`, RF05) tentam falar com `localhost:9090` a partir do
navegador do usuário e falham sempre.

**Correção aplicada:** as duas `ENV` foram adicionadas em
`frontend-web/Dockerfile.prod`, junto das demais, derivadas do mesmo
`NEXT_PUBLIC_BASE_URL` — nenhuma variável nova no `.env`. Precisa de
**rebuild da imagem do frontend** para ter efeito (é build arg, não
runtime) — se já existir um deploy anterior no ar, rode:
```bash
docker compose -f docker-compose.prod.yml up -d --build frontend
```

---

## 2. Checklist de variáveis de ambiente (`docker/prod/.env`)

Copiar de `docker/prod/.env.prod.example` e preencher:

| Variável | Obrigatória | Observação |
|---|---|---|
| `DOMINIO` | Sim | Precisa ter registro A apontando pro IP da VPS **antes** de subir o Caddy (valida via HTTP-01) |
| `POSTGRES_DB` | Sim | — |
| `POSTGRES_USER` | Sim | — |
| `POSTGRES_PASSWORD` | Sim | `openssl rand -base64 24` |
| `JWT_SECRET` | Sim | `openssl rand -base64 64` — mínimo 64 caracteres (HS512) |
| `JWT_ACCESS_EXPIRATION_MS` | Sim | Sugerido no example: `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | Sim | Sugerido: `604800000` (7 dias) — igual ao `maxAge` do cookie `__pd_rt` |
| `WHATSAPP_PHONE_NUMBER_ID` | Sim | Do Meta for Developers — variável já existe no `.env.prod.example`, falta só o valor real |
| `WHATSAPP_ACCESS_TOKEN` | Sim | Token permanente (System User), não o de teste de 24h — idem |
| `COMANDO_COPIA_EXTERNA` | Recomendada | Sem isso o backup fica só no mesmo disco (ver §3) |
| `RETENCAO_DIAS` / `RETENCAO_SEMANAIS` | Opcional | Default 14/8 já é razoável |

`APP_URL_BASE_CONFIRMACAO` **não** entra nesta lista — é calculada a
partir de `DOMINIO` direto no `docker-compose.prod.yml`, não precisa de
entrada própria no `.env`.

---

## 3. Pendências de infraestrutura / operação no servidor

Estas não travam o `docker compose up`, mas deixam a aplicação sem rede
de proteção — já mapeadas em `ANALISE_DEPLOY.md` §6, reconferidas aqui:

1. **Registrar o domínio** e apontar o A record para o IP da VPS antes do
   primeiro `up` (o Caddy falha a emissão de certificado sem isso).
2. **Build na VPS, não em CI.** Não existe `.github/workflows/` no repo —
   hoje o único caminho é `docker compose -f docker-compose.prod.yml up -d
   --build` rodando o build (Maven + `next build`) na própria VPS. Numa
   VPS de **4 GB** isso é o cenário "confortável" descrito em
   `ANALISE_DEPLOY.md` (o piso de 2 GB só funciona com build em CI) —
   então não bloqueia, mas vale saber que builds futuros vão competir por
   RAM com os containers rodando. Considerar CI (GitHub Actions → GHCR)
   se o deploy for ficar mais frequente.
3. **`COMANDO_COPIA_EXTERNA` não configurado.** `backup.sh` já faz dump do
   banco + anexos, verifica integridade e aplica retenção — mas sem essa
   variável o backup mora no mesmo disco que deveria proteger. Configurar
   destino externo (rclone → B2/S3, ou rsync para outro servidor).
4. **Cron do backup não instalado.** `scripts/backup.sh` precisa ser
   agendado manualmente no servidor (`docker/prod/scripts/README.md` tem
   o passo a passo — `0 3 * * *`, diário às 3h).
5. **Teste de restore não agendado.** `scripts/restore.sh --teste` deveria
   rodar mensalmente contra o backup mais recente — hoje é um passo manual
   que ninguém dispara sozinho.
6. **Firewall / hardening básico do host** não é coberto pelo compose:
   `ufw` liberando só 80/443/22, desabilitar login SSH por senha, `fail2ban`
   — nenhuma dessas é responsabilidade do Docker.

---

## 4. Segurança / LGPD (prontuário = dado sensível, Art. 11)

Já sinalizadas em `ANALISE_DEPLOY.md` §5 e `PLANO_PROXIMOS_PASSOS.md`
Fase 5, seguem pendentes:

- **Backup em claro.** `backup.sh` gera o dump e a cópia externa sem
  cifrar — falta um passo de GPG antes do envio (dump de prontuário em
  bucket é exatamente o tipo de arquivo que não pode vazar).
- **RNF01 — criptografia em repouso** de campos sensíveis (CPF, dados
  clínicos) ainda não implementada no backend (attribute converter JPA ou
  cifragem de coluna). Não bloqueia o deploy, mas é dívida de conformidade
  já assumida no plano.
- **DPA do provedor da VPS** (Vultr/Lightsail/Magalu) — assinar e
  arquivar antes de operar com dados reais de pacientes.

---

## 5. O que já está pronto (não precisa de ação)

Para não parecer que está tudo quebrado — a parte de infraestrutura
containerizada está madura:

- `docker-compose.prod.yml`: rede interna isolada (Postgres inacessível de
  fora), limites de CPU/memória por serviço, healthchecks com
  `start_period` calibrado pro boot do Spring + Flyway.
- Caddy cuidando de TLS automático, HSTS e demais headers de segurança, e
  servindo frontend + API no mesmo domínio (zero CORS a configurar).
- Volumes persistentes corretos para Postgres, anexos e certificados do
  Caddy.
- `application-prod.yaml` já desliga Swagger, oculta stacktrace nas
  respostas de erro, e exige todas as credenciais via variável de
  ambiente (nada hardcoded).
- Dockerfiles multi-stage, usuário não-root nos dois (`backend` e
  `frontend`), JVM com `MaxRAMPercentage` respeitando o cgroup.
- `backup.sh` / `restore.sh`: par testado, ordem correta (banco antes dos
  anexos), verificação de integridade antes de aplicar retenção, cópia de
  segurança pré-restore.
- `.gitignore` cobre os três `.env` (raiz, dev, prod) — nenhum segredo
  vazou para o histórico do repo.

---

## 6. Ordem sugerida antes do primeiro deploy

1. ~~Corrigir os 3 bloqueadores do §1 (código)~~ — feito.
2. Registrar domínio e apontar DNS.
3. Criar credenciais do WhatsApp Cloud API (token permanente).
4. Preencher `docker/prod/.env` com o checklist do §2.
5. `docker compose -f docker-compose.prod.yml up -d --build` na VPS.
6. Confirmar TLS emitido e as duas telas do §1.3 funcionando
   (`/relatorios` e horários de trabalho).
7. Instalar cron do `backup.sh` + configurar `COMANDO_COPIA_EXTERNA`.
8. Rodar `restore.sh --teste` uma vez para validar o primeiro backup.
9. Agendar o teste mensal de restore no calendário.
