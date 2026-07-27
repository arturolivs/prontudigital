#!/usr/bin/env bash
# ============================================================
# RNF02 — Backup do ProntuDigital
#
# Salva os DOIS estados que importam:
#   1. Banco PostgreSQL  (pg_dump formato custom, comprimido)
#   2. Volume de anexos  (arquivos do prontuario — RF15)
#
# Backup so do banco NAO serve: os anexos ficam em filesystem
# (ArmazenamentoService), e o banco guarda apenas a chave. Restaurar
# um sem o outro deixa o prontuario com downloads quebrados.
#
# Uso:
#   ./backup.sh                 # backup completo
#   DESTINO=/mnt/hd ./backup.sh # sobrescreve o destino
#
# Instalacao no cron (diario as 3h) — ver README.md deste diretorio.
# ============================================================

set -euo pipefail

# --- Localizacao ---------------------------------------------------
# Resolve o diretorio do proprio script para que o cron funcione
# independente do diretorio de onde foi chamado.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$COMPOSE_DIR/docker-compose.prod.yml"

# --- Configuracao --------------------------------------------------
DESTINO="${DESTINO:-/var/backups/prontudigital}"
RETENCAO_DIAS="${RETENCAO_DIAS:-14}"
RETENCAO_SEMANAIS="${RETENCAO_SEMANAIS:-8}"
VOLUME_ANEXOS="${VOLUME_ANEXOS:-prontudigital_anexos_prod}"

CARIMBO="$(date +%Y-%m-%d_%H%M%S)"
DIA_SEMANA="$(date +%u)"   # 7 = domingo

log()  { printf '[%s] %s\n'  "$(date +'%H:%M:%S')" "$*"; }
erro() { printf '[%s] ERRO: %s\n' "$(date +'%H:%M:%S')" "$*" >&2; }

# Qualquer falha aborta o backup e deixa rastro. Backup que falha em
# silencio e pior que backup nenhum: cria confianca infundada.
falhar() {
    erro "$1"
    erro "BACKUP ABORTADO — nenhum arquivo valido foi gerado em $CARIMBO"
    exit 1
}

# --- Pre-condicoes -------------------------------------------------
[[ -f "$COMPOSE_FILE" ]] || falhar "compose nao encontrado: $COMPOSE_FILE"
[[ -f "$COMPOSE_DIR/.env" ]] || falhar "arquivo .env nao encontrado em $COMPOSE_DIR"

# shellcheck disable=SC1091
set -a; source "$COMPOSE_DIR/.env"; set +a

: "${POSTGRES_USER:?POSTGRES_USER ausente no .env}"
: "${POSTGRES_DB:?POSTGRES_DB ausente no .env}"

cd "$COMPOSE_DIR"

docker compose -f "$COMPOSE_FILE" ps --status running --services 2>/dev/null \
    | grep -qx postgres \
    || falhar "container do postgres nao esta rodando"

DIR_DIARIO="$DESTINO/diarios/$CARIMBO"
mkdir -p "$DIR_DIARIO"

ARQ_BANCO="$DIR_DIARIO/banco.dump"
ARQ_ANEXOS="$DIR_DIARIO/anexos.tar.gz"

log "Destino: $DIR_DIARIO"

# --- 1. Banco ------------------------------------------------------
# Formato custom (-Fc): comprimido e restauravel seletivamente com
# pg_restore, diferente do SQL puro.
#
# ORDEM IMPORTA: o banco vem ANTES dos anexos. Os dois dumps nao sao
# atomicos entre si, entao algo gravado no meio do processo cai em um
# e nao no outro. Nesta ordem, um anexo criado durante o backup vira
# arquivo orfao (inofensivo, so ocupa espaco). Na ordem inversa
# viraria registro sem arquivo — download quebrado no prontuario.
log "Exportando banco '$POSTGRES_DB'..."
if ! docker compose -f "$COMPOSE_FILE" exec -T postgres \
        pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc --no-owner \
        > "$ARQ_BANCO" 2>/tmp/pg_dump_erro.log; then
    erro "$(cat /tmp/pg_dump_erro.log)"
    falhar "pg_dump falhou"
fi

[[ -s "$ARQ_BANCO" ]] || falhar "dump do banco saiu vazio"

# --- 2. Anexos -----------------------------------------------------
# Container efemero monta o volume somente-leitura. Evita depender do
# caminho interno do Docker no host, que muda entre distros.
log "Arquivando anexos (volume $VOLUME_ANEXOS)..."
if ! docker run --rm \
        -v "$VOLUME_ANEXOS":/dados:ro \
        -v "$DIR_DIARIO":/backup \
        alpine:3 \
        tar czf /backup/anexos.tar.gz -C /dados . 2>/tmp/tar_erro.log; then
    erro "$(cat /tmp/tar_erro.log)"
    falhar "falha ao arquivar os anexos"
fi

# --- 3. Verificacao ------------------------------------------------
# Um dump que nao pode ser lido nao e backup. pg_restore --list nao
# restaura nada: apenas le o indice, o que ja detecta truncamento e
# corrupcao — as duas falhas mais comuns.
log "Verificando integridade..."

docker compose -f "$COMPOSE_FILE" exec -T postgres \
    pg_restore --list /dev/stdin < "$ARQ_BANCO" > /dev/null 2>&1 \
    || falhar "dump do banco esta corrompido ou truncado"

tar tzf "$ARQ_ANEXOS" > /dev/null 2>&1 \
    || falhar "arquivo de anexos esta corrompido"

# Manifesto: permite conferir tamanho e integridade sem restaurar.
{
    echo "data_backup=$CARIMBO"
    echo "banco=$POSTGRES_DB"
    echo "tamanho_banco=$(du -h "$ARQ_BANCO" | cut -f1)"
    echo "tamanho_anexos=$(du -h "$ARQ_ANEXOS" | cut -f1)"
    echo "qtd_anexos=$(tar tzf "$ARQ_ANEXOS" | grep -cv '/$' || true)"
} > "$DIR_DIARIO/manifesto.txt"

(cd "$DIR_DIARIO" && sha256sum banco.dump anexos.tar.gz > SHA256SUMS)

log "Banco:  $(du -h "$ARQ_BANCO"  | cut -f1)"
log "Anexos: $(du -h "$ARQ_ANEXOS" | cut -f1)"

# --- 4. Copia semanal ----------------------------------------------
# Domingo tambem entra na pasta de semanais, com retencao maior. Sem
# isso, uma corrupcao percebida so depois de 15 dias nao teria ponto
# de retorno.
if [[ "$DIA_SEMANA" == "7" ]]; then
    mkdir -p "$DESTINO/semanais"
    cp -r "$DIR_DIARIO" "$DESTINO/semanais/$CARIMBO"
    log "Copia semanal registrada"
fi

# --- 5. Retencao ---------------------------------------------------
# Roda DEPOIS da verificacao: nunca apagar backup antigo antes de
# confirmar que o novo presta.
log "Aplicando retencao (${RETENCAO_DIAS}d diarios / ${RETENCAO_SEMANAIS} semanais)..."

find "$DESTINO/diarios" -mindepth 1 -maxdepth 1 -type d \
    -mtime "+$RETENCAO_DIAS" -exec rm -rf {} + 2>/dev/null || true

if [[ -d "$DESTINO/semanais" ]]; then
    # shellcheck disable=SC2012
    ls -1dt "$DESTINO/semanais"/*/ 2>/dev/null \
        | tail -n "+$((RETENCAO_SEMANAIS + 1))" \
        | xargs -r rm -rf
fi

# --- 6. Copia externa ----------------------------------------------
# O backup ainda esta no MESMO servidor que a aplicacao. Perder o
# servidor perde os dois. Configure COMANDO_COPIA_EXTERNA no .env
# para enviar a copia para fora (rclone, rsync, aws s3...).
#
# Exemplo no .env:
#   COMANDO_COPIA_EXTERNA="rclone sync /var/backups/prontudigital remoto:prontudigital"
if [[ -n "${COMANDO_COPIA_EXTERNA:-}" ]]; then
    log "Enviando copia externa..."
    if eval "$COMANDO_COPIA_EXTERNA"; then
        log "Copia externa concluida"
    else
        # Nao aborta: o backup local ja e valido. Mas grita, porque
        # backup so no proprio servidor nao protege contra perda dele.
        erro "COPIA EXTERNA FALHOU — backup existe apenas neste servidor"
        exit 2
    fi
else
    erro "AVISO: COMANDO_COPIA_EXTERNA nao configurado."
    erro "       O backup esta somente neste servidor. Se ele for perdido,"
    erro "       os dados da clinica vao junto. Configure uma copia externa."
fi

log "Backup concluido: $DIR_DIARIO"
