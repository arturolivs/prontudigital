#!/usr/bin/env bash
# =============================================================
# Healthcheck do container do backend.
#
# Por que um script e nao uma linha no docker-compose.prod.yml:
# a requisicao HTTP precisa de CRLF (\r\n) entre os cabecalhos, e
# escapar isso dentro do YAML do Compose gera quebra de linha real
# em vez da sequencia literal — o pedido sai malformado.
#
# Por que /dev/tcp e nao curl: a imagem de runtime e a
# eclipse-temurin:21-jre, que nao garante curl nem wget. /dev/tcp e
# recurso do proprio bash, que a imagem tem. Por isso tambem o
# shebang e bash e nao sh: no Debian/Ubuntu /bin/sh e o dash, que
# nao implementa /dev/tcp.
#
# O que ele prova, e que a versao anterior (so abrir o socket) nao
# provava: que a aplicacao responde. Um backend com o pool do banco
# esgotado mantem a porta 8080 aberta e passava no teste antigo.
# =============================================================
set -euo pipefail

PORTA="${HEALTHCHECK_PORTA:-8080}"
CAMINHO="${HEALTHCHECK_CAMINHO:-/actuator/health}"

exec 3<>"/dev/tcp/127.0.0.1/${PORTA}"

printf 'GET %s HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' \
  "${CAMINHO}" >&3

# O corpo sai como {"status":"UP"} — `show-details: never` no
# application-prod.yaml mantem o resto fora. DOWN, 401, 404 ou
# resposta vazia nao casam, e o grep devolve 1.
grep -q '"status":"UP"' <&3
