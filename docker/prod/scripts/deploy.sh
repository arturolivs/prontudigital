#!/usr/bin/env bash
# ============================================================
# Deploy do ProntuDigital na VPS
#
# Sobe uma versao ja construida no GitHub Actions e publicada no GHCR.
# Nada e compilado aqui: `up --build` na VPS estoura a memoria da
# maquina pequena (ANALISE_DEPLOY.md §3).
#
# Uso:
#   ./deploy.sh <tag>            # ex.: ./deploy.sh sha-3d0989f
#
# Variaveis opcionais:
#   PULAR_BACKUP=1   nao tira backup antes de subir (use com cuidado:
#                    migration do Flyway nao tem rollback)
#   ESPERA_MAX=240   segundos de espera pela saude dos containers
#   REGISTRO=...     origem das imagens (padrao: ghcr.io/arturolivs)
#
# Quem chama normalmente e o job `implantar` de
# .github/workflows/cd.yml, por SSH. Rodar a mao e legitimo — inclusive
# para voltar atras: `./deploy.sh <tag-antiga>`.
# ============================================================

set -euo pipefail

# --- Localizacao ---------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_BASE="$COMPOSE_DIR/docker-compose.prod.yml"
COMPOSE_GHCR="$COMPOSE_DIR/docker-compose.ghcr.yml"
ARQUIVO_ENV="$COMPOSE_DIR/.env"

ESPERA_MAX="${ESPERA_MAX:-240}"

registrar() { printf '\n== %s\n' "$*"; }
falhar()    { printf '\nERRO: %s\n' "$*" >&2; exit 1; }

compose() {
  docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_GHCR" "$@"
}

# --- Entrada -------------------------------------------------------
TAG_NOVA="${1:-}"
[[ -n "$TAG_NOVA" ]] || falhar "informe a tag da imagem. Ex.: $0 sha-3d0989f"

[[ -f "$COMPOSE_BASE" ]] || falhar "compose nao encontrado: $COMPOSE_BASE"
[[ -f "$COMPOSE_GHCR" ]] || falhar "sobreposicao nao encontrada: $COMPOSE_GHCR"
[[ -f "$ARQUIVO_ENV"  ]] || falhar "arquivo .env nao encontrado em $COMPOSE_DIR (ver doc/DEPLOY.md §5)"

for segredo in db.password jwt.secret admin.senha; do
  [[ -f "$COMPOSE_DIR/secrets/$segredo" ]] \
    || falhar "segredo ausente: secrets/$segredo (ver docker/prod/secrets/README.md)"
done

cd "$COMPOSE_DIR"

# --- Tag anterior, para poder voltar --------------------------------
# Sai do proprio .env: e o unico lugar onde o estado do que esta no ar
# fica registrado de forma legivel depois que o deploy termina.
TAG_ANTIGA="$(sed -n 's/^IMAGE_TAG=//p' "$ARQUIVO_ENV" | tail -n 1)"

gravar_tag() {
  local tag="$1"
  if grep -q '^IMAGE_TAG=' "$ARQUIVO_ENV"; then
    sed -i "s|^IMAGE_TAG=.*|IMAGE_TAG=$tag|" "$ARQUIVO_ENV"
  else
    printf '\n# Tag da imagem no ar. Gravada por scripts/deploy.sh.\nIMAGE_TAG=%s\n' \
      "$tag" >> "$ARQUIVO_ENV"
  fi
}

voltar_atras() {
  if [[ -z "$TAG_ANTIGA" ]]; then
    registrar "sem tag anterior registrada no .env — nada para restaurar"
    return
  fi
  registrar "Restaurando a versao anterior ($TAG_ANTIGA)"
  gravar_tag "$TAG_ANTIGA"
  # O || true e proposital: aqui ja estamos no caminho de erro, e o
  # diagnostico impresso depois vale mais que abortar o rollback.
  compose up -d --no-build --remove-orphans || true
}

diagnostico() {
  registrar "Estado dos containers"
  compose ps || true
  registrar "Ultimas linhas do backend"
  compose logs --tail 80 backend || true
}

# --- Backup antes de mexer ------------------------------------------
# Migration do Flyway nao tem rollback: se a versao nova alterar o
# esquema, voltar a imagem antiga nao volta o banco. Por isso o backup
# e obrigatorio por padrao e uma falha dele aborta o deploy.
#
# So faz sentido se ja existe algo no ar — no primeiro deploy nao ha
# banco para copiar.
if [[ "${PULAR_BACKUP:-0}" != "1" ]] \
   && docker compose -f "$COMPOSE_BASE" ps --status running --services 2>/dev/null | grep -q postgres; then
  registrar "Backup antes do deploy"
  "$SCRIPT_DIR/backup.sh" || falhar "backup falhou — deploy abortado (use PULAR_BACKUP=1 para ignorar)"
else
  registrar "Backup dispensado (pilha parada ou PULAR_BACKUP=1)"
fi

# --- Baixar e subir --------------------------------------------------
registrar "Deploy: $TAG_ANTIGA -> $TAG_NOVA"
gravar_tag "$TAG_NOVA"

if ! compose pull backend frontend; then
  voltar_atras
  falhar "pull das imagens $TAG_NOVA falhou (a tag existe no registro? o docker login foi feito?)"
fi

if ! compose up -d --no-build --remove-orphans; then
  diagnostico
  voltar_atras
  falhar "subida dos containers falhou"
fi

# --- Esperar ficar saudavel -------------------------------------------
# O backend tem healthcheck de verdade (requisicao a /actuator/health,
# ver backend/docker-healthcheck.sh) e start_period de 60s: Flyway +
# boot da JVM. Frontend e Caddy nao tem healthcheck — deles basta
# provar que continuam em execucao, isto e, que nao entraram em
# crash-loop.
registrar "Aguardando saude (limite: ${ESPERA_MAX}s)"

estado() { docker inspect -f "$2" "$1" 2>/dev/null || echo desconhecido; }

decorrido=0
while :; do
  saude_backend="$(estado prontudigital-backend '{{.State.Health.Status}}')"
  estado_front="$(estado prontudigital-frontend '{{.State.Status}}')"
  estado_caddy="$(estado prontudigital-caddy '{{.State.Status}}')"

  if [[ "$saude_backend" == healthy && "$estado_front" == running && "$estado_caddy" == running ]]; then
    registrar "Containers saudaveis apos ${decorrido}s"
    break
  fi

  if [[ "$saude_backend" == unhealthy ]]; then
    diagnostico
    voltar_atras
    falhar "backend subiu mas ficou unhealthy na versao $TAG_NOVA"
  fi

  if (( decorrido >= ESPERA_MAX )); then
    diagnostico
    voltar_atras
    falhar "tempo esgotado: backend=$saude_backend frontend=$estado_front caddy=$estado_caddy"
  fi

  sleep 5
  decorrido=$(( decorrido + 5 ))
done

# --- Fumaca, pela porta da frente --------------------------------------
# Container saudavel prova que o processo responde; isto prova que o
# caminho publico inteiro responde — DNS, TLS do Caddy e proxy para o
# frontend. /actuator nao entra aqui: o Caddyfile nao o expoe.
set -a; source "$ARQUIVO_ENV"; set +a

if [[ -n "${DOMINIO:-}" ]] && command -v curl >/dev/null 2>&1; then
  registrar "Teste de fumaca em https://$DOMINIO/"
  tentativa=1
  until curl -fsS -o /dev/null --max-time 15 "https://$DOMINIO/"; do
    if (( tentativa >= 6 )); then
      diagnostico
      voltar_atras
      falhar "https://$DOMINIO/ nao respondeu 2xx apos 6 tentativas"
    fi
    printf '  tentativa %s falhou, repetindo em 10s\n' "$tentativa"
    tentativa=$(( tentativa + 1 ))
    sleep 10
  done
  registrar "Site respondendo"
else
  registrar "Teste de fumaca pulado (DOMINIO vazio ou curl ausente)"
fi

# --- Limpeza ------------------------------------------------------------
# Imagem antiga nao referenciada por nenhum container. Sem isto o disco
# da VPS enche em alguns meses de deploys.
registrar "Removendo imagens orfas"
docker image prune -f >/dev/null

registrar "Deploy concluido: $TAG_NOVA"
compose ps
