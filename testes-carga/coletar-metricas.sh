#!/usr/bin/env bash
# =============================================================
# Coleta, DENTRO DA VPS, o que o k6 nao enxerga de fora.
#
# O k6 mede latencia e erro pela borda. Quando o p95 estoura, ele nao
# diz se foi CPU, heap, GC, fila no pool do Hikari ou disco — e sem essa
# resposta o resultado do teste nao vira acao. Este script amostra as
# duas fontes que respondem:
#
#   docker stats          CPU, memoria e rede por container
#   /actuator/prometheus  heap, GC, pool do Hikari, latencia por rota
#
# Rode-o ANTES de disparar o k6 e interrompa com Ctrl+C depois.
#
#   ./coletar-metricas.sh 5 ./saida-nominal
#
# Custo: uma requisicao a cada intervalo. Nao e carga; e observacao.
# =============================================================
set -euo pipefail

INTERVALO="${1:-5}"
DESTINO="${2:-./coleta-$(date +%Y%m%d-%H%M%S)}"
COMPOSE="${COMPOSE:-/opt/prontudigital/docker/prod/docker-compose.prod.yml}"

# O actuator exige ADMIN (SecurityConfig). Sem token, o script ainda
# coleta docker stats — degradar e melhor que nao coletar nada.
TOKEN="${TOKEN_ADMIN:-}"

mkdir -p "$DESTINO"
echo "coletando a cada ${INTERVALO}s em ${DESTINO} — Ctrl+C para encerrar"

if [ -z "$TOKEN" ]; then
  echo "AVISO: TOKEN_ADMIN vazio; so docker stats sera coletado."
  echo "  TOKEN_ADMIN=\$(curl -s -X POST https://SEU-DOMINIO/api/auth/login \\"
  echo "    -H 'Content-Type: application/json' \\"
  echo "    -d '{\"username\":\"carga_admin\",\"senha\":\"CargaTeste!2026\"}' | jq -r .accessToken)"
fi

echo "carimbo,container,cpu_pct,mem_uso,mem_pct" > "${DESTINO}/docker-stats.csv"

encerrar() {
  echo
  echo "coleta encerrada. Resumo rapido:"
  echo "  pico de CPU por container:"
  awk -F, 'NR>1 {gsub(/%/,"",$3); if ($3+0 > max[$2]) max[$2]=$3+0}
           END {for (c in max) printf "    %-28s %s%%\n", c, max[c]}' \
      "${DESTINO}/docker-stats.csv" | sort
  echo "  arquivos em ${DESTINO}"
  exit 0
}
trap encerrar INT TERM

while true; do
  CARIMBO=$(date +%Y-%m-%dT%H:%M:%S)

  # --no-stream: uma amostra e sai. Sem isso o docker stats ocupa o
  # terminal e nunca retorna.
  docker stats --no-stream --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}}' \
    | sed "s/^/${CARIMBO},/" >> "${DESTINO}/docker-stats.csv" || true

  if [ -n "$TOKEN" ]; then
    # De dentro da rede do Docker: /actuator nao e roteado pelo Caddy,
    # entao nao ha como pedir isto pelo dominio publico.
    docker compose -f "$COMPOSE" exec -T backend \
      bash -c "exec 3<>/dev/tcp/127.0.0.1/8080 &&
        printf 'GET /actuator/prometheus HTTP/1.1\r\nHost: localhost\r\nAuthorization: Bearer ${TOKEN}\r\nConnection: close\r\n\r\n' >&3 &&
        cat <&3" 2>/dev/null \
      | grep -E '^(jvm_memory_used_bytes|jvm_gc_pause|hikaricp_connections|http_server_requests_seconds_count|process_cpu_usage|system_cpu_usage)' \
      | sed "s/^/${CARIMBO} /" >> "${DESTINO}/prometheus.txt" || true
  fi

  sleep "$INTERVALO"
done
