# Análise de plataformas para deploy — ProntuDigital

> Documento de decisão. Cenário considerado: **clínica com 2 profissionais**, volume
> baixo de requisições, sem previsão de crescimento acelerado.
>
> Data da análise: **27/07/2026**. Os preços mudam — reconfira antes de contratar.

---

## 1. O que a aplicação exige

Os requisitos abaixo saem do `docker/prod/docker-compose.prod.yml` e dos
`Dockerfile.prod`. Eles eliminam boa parte das opções sozinhos.

| Requisito | Origem no repo | Consequência |
|---|---|---|
| 4 containers em rede interna | rede `interna: internal: true` | Plataforma que só roda 1 processo obriga a desmontar o arranjo |
| ~2 GB RAM em runtime | Postgres 512M + backend 1G + frontend 384M + Caddy 128M | 2 GB é o piso; 4 GB é o confortável |
| **Disco persistente para anexos** | `anexos_data:/dados/anexos` + `ArmazenamentoService` | Filesystem efêmero **quebra os anexos do prontuário** |
| Postgres com volume | `postgres_data` | Idem |
| JVM com Flyway no boot | `start_period: 60s` no healthcheck | Scale-to-zero = 30–60 s de espera na primeira request |
| Caddy dono do TLS + same-origin | `Caddyfile`, `NEXT_PUBLIC_BASE_URL=https://${DOMINIO}` | Separar front e API em domínios diferentes ressuscita o CORS e exige rebuild da imagem |
| Backup = banco + anexos | `docker/prod/scripts/backup.sh` | Assume volumes Docker e cron no host |

O ponto decisivo é o terceiro: **os anexos vivem em filesystem, não no banco** — o
banco guarda apenas a chave de armazenamento. Isso desqualifica de saída qualquer
plataforma sem volume persistente de verdade.

---

## 2. Comparativo

| Plataforma | Custo/mês | Atrito de migração | Latência BR | Veredito |
|---|---|---|---|---|
| **VPS 4 GB São Paulo** (Lightsail / Vultr / Magalu) | ~US$ 24 (≈ R$ 130) | **Zero** — `docker compose up -d` | ~10 ms | ✅ Melhor equilíbrio |
| **VPS 4 GB Hetzner** (EU/EUA) | ~€ 5 (≈ R$ 32) | Zero | ~200 ms | ✅ Mais barato viável |
| **Oracle Cloud Always Free** (GRU, ARM) | R$ 0 | Baixo (rebuild ARM64) | ~10 ms | ⚠️ Ótimo no papel, frágil |
| **VPS + Coolify / Dokploy** | VPS + R$ 0 | Baixo | conforme VPS | ✅ Se quiser painel / deploy por git |
| **Render** | ~US$ 28 (3 serviços + PG) | Alto — desmontar compose | sem região BR | ❌ Caro e pior |
| **Railway** | ~US$ 15–25 (uso) | Alto | sem região BR | ❌ Custo imprevisível |
| **Fly.io** | ~US$ 10–20 | Alto — volumes por máquina | tem GRU | ⚠️ Scale-to-zero briga com a JVM |
| **Vercel + Neon + backend avulso** | ~US$ 0–20 | **Muito alto** | misto | ❌ Reintroduz CORS |
| **AWS ECS + RDS** | ~US$ 80+ | Muito alto | ~10 ms | ❌ Overkill absoluto |

> Preços aproximados, conferir no checkout. A Hetzner aumentou preços em
> **15/06/2026** (linha CX subiu ~1,3–1,4x); regiões brasileiras da AWS e da Vultr
> cobram prêmio sobre a tabela dos EUA. Conversão a R$ 5,50/US$.

---

## 3. Recomendação

**Uma VPS de 4 GB em São Paulo, rodando o `docker-compose.prod.yml` como está.**

O motivo é simples: o custo de engenharia da conteinerização já foi pago. Rede
interna isolada, TLS automático, healthcheck, limites de recurso e backup dos dois
estados — está tudo pronto e testado. Qualquer PaaS obriga a **desmontar** esse
arranjo (o Caddy vira redundante, o Postgres vira serviço gerenciado, os volumes
viram outra coisa) para no fim pagar mais caro por menos controle. Para 2
profissionais, seria pagar um prêmio de elasticidade que nunca será usado.

### Dois ajustes que valem mais que a escolha do fornecedor

**1. Não fazer build no servidor.**
O `docker compose up --build` compila Maven *e* roda `next build` — isso consome
bem mais RAM que os 2 GB do runtime e derruba uma VPS pequena por OOM. Construir
as imagens no GitHub Actions, publicar no GHCR e deixar a VPS só dando `pull`.
Com isso **2 GB passam a bastar**, e a conta cai pela metade.

**2. Backup fora da máquina.**
O `scripts/backup.sh` já faz a parte difícil na ordem certa (banco antes dos
anexos, para o pior caso ser arquivo órfão em vez de download quebrado). Falta o
destino externo: Backblaze B2 ou S3 custam ~US$ 1/mês nesse volume. Backup que
mora no mesmo disco não é backup.

### Custo realista anual

| Item | Valor |
|---|---|
| VPS 2 GB São Paulo (com build em CI) | ~R$ 65/mês |
| Domínio `.com.br` (registro.br) | ~R$ 40/ano |
| Backup externo (B2 / S3) | ~R$ 6/mês |
| Snapshot semanal da VPS | ~R$ 10/mês |
| **Total** | **~R$ 1.020/ano** |

Na Hetzner o mesmo cai para ~R$ 600/ano, ao custo de ~200 ms de latência e dos
dados saírem do Brasil.

---

## 4. Por que as outras opções foram descartadas

**Oracle Always Free** é tentadora — 4 vCPU ARM e 24 GB de RAM em São Paulo, de
graça. Funcionaria: Temurin, Node Alpine, Postgres e Caddy todos têm imagem
arm64. O problema não é técnico: a capacidade Ampere na região GRU é
notoriamente difícil de conseguir, e contas do tier gratuito podem ser
recuperadas pela Oracle sem SLA. Para hobby, ótimo. Para **prontuário de
clínica**, é risco operacional mal colocado.

**Vercel + Neon** parece barato porque o frontend fica no free tier, mas o
`Dockerfile.prod` embute `NEXT_PUBLIC_BASE_URL` em tempo de build e o Caddy hoje
serve front e API na mesma origem — separar os domínios traz o CORS de volta e
transforma cada mudança de URL em rebuild. E o backend Spring continuaria
precisando de casa em algum lugar. Seria trocar 1 servidor por 3 fornecedores.

**Fly.io** tem região São Paulo e é o PaaS menos ruim aqui, mas o modelo de
volumes é preso à máquina (complica o restore) e o auto-stop conflita com o boot
de ~60 s da JVM.

**Render / Railway** custam mais que a VPS recomendada, não têm região no Brasil
e exigem o mesmo trabalho de desmontar o compose.

**AWS ECS + RDS** resolve problemas de escala que esta aplicação não tem.

---

## 5. LGPD — o que a escolha de plataforma implica

Isto é um prontuário eletrônico: dados de saúde são **sensíveis** pelo Art. 11 da
LGPD. Duas implicações práticas na escolha da plataforma:

- **Residência dos dados.** A LGPD não obriga hospedagem no Brasil, mas
  transferência internacional exige base legal formal (cláusulas-padrão da
  Resolução CD/ANPD nº 19/2024). Hospedar em São Paulo elimina essa discussão —
  é o argumento mais forte contra a Hetzner, mais do que a latência.
- **Contrato de operador.** O provedor escolhido é *operador* de dados pessoais.
  AWS, Vultr, Magalu e Hetzner oferecem DPA / adendo de proteção de dados; vale
  assinar e arquivar.

**Pendência identificada:** cifrar o backup antes de enviá-lo ao storage externo.
Hoje o `backup.sh` gera o dump e envia a cópia em claro — dump de prontuário em
bucket é exatamente o tipo de arquivo que não pode vazar. Falta um passo de GPG
no script.

---

## 6. Decisão

- [ ] Escolher fornecedor da VPS (recomendado: 2 GB em São Paulo + build em CI)
- [ ] Registrar domínio `.com.br`
- [ ] Configurar pipeline de build no GitHub Actions publicando no GHCR
- [ ] Configurar destino externo do backup (B2 / S3)
- [ ] Adicionar cifragem GPG ao `scripts/backup.sh`
- [ ] Assinar o DPA do provedor

---

## Fontes

- [Hetzner Price Adjustment 15 June 2026 — Hetzner Docs](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/)
- [Standardization and price adjustment of our server products — Hetzner](https://www.hetzner.com/pressroom/standardization-and-price-adjustment-of-our-server-products/)
- [Hetzner cloud server price increases in 2026 — Northflank](https://northflank.com/blog/hetzner-cloud-server-price-increases)
- [Amazon Lightsail Pricing 2026 — Cloud Burn](https://cloudburn.io/blog/amazon-lightsail-pricing)
- [Vultr Pricing 2026 — getdeploying](https://getdeploying.com/vultr)
