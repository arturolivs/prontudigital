#!/usr/bin/env bash
# ============================================================
# RNF02 — Restauracao do ProntuDigital
#
# Uso:
#   ./restore.sh --teste  <dir_backup>   # valida SEM tocar em producao
#   ./restore.sh --real   <dir_backup>   # DESTRUTIVO: substitui os dados
#
# SEMPRE rode --teste antes de precisar do --real. Um backup que nunca
# foi restaurado e uma suposicao, nao uma garantia. O modo --teste
# restaura o banco num database temporario e extrai os anexos numa
# pasta descartavel, sem parar a aplicacao nem alterar nada.
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$COMPOSE_DIR/docker-compose.prod.yml"
VOLUME_ANEXOS="${VOLUME_ANEXOS:-prontudigital_anexos_prod}"

log()  { printf '[%s] %s\n' "$(date +'%H:%M:%S')" "$*"; }
erro() { printf '[%s] ERRO: %s\n' "$(date +'%H:%M:%S')" "$*" >&2; }
fim()  { erro "$1"; exit 1; }

uso() {
    sed -n '2,14p' "${BASH_SOURCE[0]}" | sed 's/^# \?//'
    exit 1
}

MODO="${1:-}"
DIR_BACKUP="${2:-}"

[[ "$MODO" == "--teste" || "$MODO" == "--real" ]] || uso
[[ -n "$DIR_BACKUP" && -d "$DIR_BACKUP" ]] || fim "diretorio de backup invalido: $DIR_BACKUP"

ARQ_BANCO="$DIR_BACKUP/banco.dump"
ARQ_ANEXOS="$DIR_BACKUP/anexos.tar.gz"

[[ -f "$ARQ_BANCO"  ]] || fim "banco.dump nao encontrado em $DIR_BACKUP"
[[ -f "$ARQ_ANEXOS" ]] || fim "anexos.tar.gz nao encontrado em $DIR_BACKUP"

# shellcheck disable=SC1091
set -a; source "$COMPOSE_DIR/.env"; set +a
: "${POSTGRES_USER:?}"; : "${POSTGRES_DB:?}"

cd "$COMPOSE_DIR"
psql_() { docker compose -f "$COMPOSE_FILE" exec -T postgres psql -U "$POSTGRES_USER" "$@"; }

# --- Integridade ---------------------------------------------------
# Confere os hashes antes de qualquer coisa: restaurar a partir de um
# arquivo corrompido pode ser pior que nao restaurar.
if [[ -f "$DIR_BACKUP/SHA256SUMS" ]]; then
    log "Conferindo checksums..."
    (cd "$DIR_BACKUP" && sha256sum -c SHA256SUMS --quiet) \
        || fim "checksum divergente — este backup esta corrompido"
else
    erro "AVISO: SHA256SUMS ausente; prosseguindo sem conferencia"
fi

# ===================================================================
# MODO TESTE — nao altera producao
# ===================================================================
if [[ "$MODO" == "--teste" ]]; then
    BANCO_TESTE="restore_teste_$(date +%s)"
    PASTA_TESTE="$(mktemp -d)"

    limpar() {
        log "Limpando artefatos de teste..."
        psql_ -d postgres -c "DROP DATABASE IF EXISTS $BANCO_TESTE;" >/dev/null 2>&1 || true
        rm -rf "$PASTA_TESTE"
    }
    trap limpar EXIT

    log "Restaurando banco em '$BANCO_TESTE' (temporario)..."
    psql_ -d postgres -c "CREATE DATABASE $BANCO_TESTE;" >/dev/null

    docker compose -f "$COMPOSE_FILE" exec -T postgres \
        pg_restore -U "$POSTGRES_USER" -d "$BANCO_TESTE" --no-owner /dev/stdin \
        < "$ARQ_BANCO" >/dev/null 2>&1 \
        || fim "pg_restore falhou — o dump nao presta"

    # Contagens: prova que o dump tem CONTEUDO, nao apenas estrutura.
    log "Conferindo dados restaurados:"
    for tabela in usuarios agendamentos anamneses prescricoes anexos atestados; do
        total=$(psql_ -d "$BANCO_TESTE" -tAc \
            "SELECT COUNT(*) FROM $tabela;" 2>/dev/null || echo "n/d")
        printf '    %-16s %s\n' "$tabela" "$total"
    done

    log "Extraindo anexos para conferencia..."
    tar xzf "$ARQ_ANEXOS" -C "$PASTA_TESTE"
    qtd=$(find "$PASTA_TESTE" -type f | wc -l)
    log "Anexos extraidos com sucesso: $qtd arquivo(s)"

    # Cruzamento: cada registro em `anexos` deve ter arquivo no volume.
    # E aqui que se descobre backup dessincronizado — o unico jeito de
    # detectar isso e restaurando os dois lados juntos.
    registros=$(psql_ -d "$BANCO_TESTE" -tAc "SELECT COUNT(*) FROM anexos;" 2>/dev/null || echo 0)
    registros=$(echo "$registros" | tr -d '[:space:]')
    if [[ "$registros" =~ ^[0-9]+$ ]] && (( registros > qtd )); then
        erro "ATENCAO: $registros registros em 'anexos' para $qtd arquivo(s) no volume."
        erro "         Ha registros sem arquivo — downloads quebrariam apos restaurar."
        exit 3
    fi

    log "RESTORE VALIDADO — este backup e utilizavel."
    exit 0
fi

# ===================================================================
# MODO REAL — destrutivo
# ===================================================================
cat <<AVISO

  ########################################################
  #  ATENCAO: OPERACAO DESTRUTIVA                        #
  ########################################################

  Isto SUBSTITUI os dados atuais de producao:

    Banco  : $POSTGRES_DB  (sera recriado do zero)
    Anexos : volume $VOLUME_ANEXOS (sera esvaziado)

  Origem : $DIR_BACKUP
$( [[ -f "$DIR_BACKUP/manifesto.txt" ]] && sed 's/^/    /' "$DIR_BACKUP/manifesto.txt" )

  Tudo que existe hoje e que nao esteja neste backup sera PERDIDO.

AVISO

read -rp "Digite exatamente RESTAURAR para confirmar: " confirmacao
[[ "$confirmacao" == "RESTAURAR" ]] || fim "cancelado pelo operador"

# Rede de seguranca: fotografa o estado atual antes de destrui-lo.
# Se o backup restaurado se revelar errado, ainda ha de onde voltar.
SEGURANCA="/var/backups/prontudigital/pre-restore/$(date +%Y-%m-%d_%H%M%S)"
log "Salvando estado atual em $SEGURANCA (rede de seguranca)..."
mkdir -p "$SEGURANCA"
docker compose -f "$COMPOSE_FILE" exec -T postgres \
    pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc --no-owner > "$SEGURANCA/banco.dump" || true
docker run --rm -v "$VOLUME_ANEXOS":/dados:ro -v "$SEGURANCA":/backup alpine:3 \
    tar czf /backup/anexos.tar.gz -C /dados . || true

# A aplicacao precisa parar: com o backend conectado, o DROP DATABASE
# falha e o Flyway pode recriar schema no meio da restauracao.
log "Parando aplicacao..."
docker compose -f "$COMPOSE_FILE" stop backend frontend

log "Recriando banco..."
psql_ -d postgres -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity
      WHERE datname = '$POSTGRES_DB' AND pid <> pg_backend_pid();" >/dev/null
psql_ -d postgres -c "DROP DATABASE IF EXISTS $POSTGRES_DB;" >/dev/null
psql_ -d postgres -c "CREATE DATABASE $POSTGRES_DB OWNER $POSTGRES_USER;" >/dev/null

log "Restaurando dados..."
docker compose -f "$COMPOSE_FILE" exec -T postgres \
    pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner /dev/stdin \
    < "$ARQ_BANCO" \
    || fim "pg_restore falhou — aplicacao segue PARADA; estado anterior em $SEGURANCA"

log "Restaurando anexos..."
docker run --rm \
    -v "$VOLUME_ANEXOS":/dados \
    -v "$DIR_BACKUP":/backup:ro \
    alpine:3 \
    sh -c 'rm -rf /dados/* /dados/..?* 2>/dev/null; tar xzf /backup/anexos.tar.gz -C /dados' \
    || fim "falha ao restaurar anexos — aplicacao segue PARADA"

log "Subindo aplicacao..."
docker compose -f "$COMPOSE_FILE" start backend frontend

log ""
log "RESTAURACAO CONCLUIDA."
log "Estado anterior preservado em: $SEGURANCA"
log "Confira o sistema antes de apagar essa pasta."
