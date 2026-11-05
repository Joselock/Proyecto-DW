#!/usr/bin/env bash
# ============================================================
# start.sh — Arranca el backend de Spring Boot, que además
# sirve el frontend en la misma URL: http://localhost:8080
#
# Uso:
#   ./start.sh              arranca y abre el navegador
#   NO_OPEN=1 ./start.sh    arranca sin abrir el navegador
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT/backend"
FRONTEND_DIR="$ROOT/frontend"
RUN_DIR="$ROOT/.run"
PID_FILE="$RUN_DIR/backend.pid"
LOG_FILE="$RUN_DIR/backend.log"
PORT="${BACKEND_PORT:-8080}"
URL="http://localhost:$PORT"

mkdir -p "$RUN_DIR"

port_open() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") >/dev/null 2>&1
}

# Si ya está corriendo, no lo arranca otra vez.
if port_open "$PORT"; then
  echo "El servidor ya está corriendo en $URL"
  [[ "${NO_OPEN:-0}" == "1" ]] || xdg-open "$URL" >/dev/null 2>&1 || true
  exit 0
fi

echo "Arrancando Proyecto DW..."
echo "  Backend + frontend: $URL"
echo "  Log:                $LOG_FILE"

# Spring Boot sirve los archivos de frontend/ como estáticos.
export SPRING_WEB_RESOURCES_STATIC_LOCATIONS="file:$FRONTEND_DIR/"

cd "$BACKEND_DIR"

# Job control para que Maven y Java queden en su propio grupo de procesos.
set -m
nohup mvn -o -q spring-boot:run >"$LOG_FILE" 2>&1 </dev/null &
BACKEND_PID=$!
set +m

echo "$BACKEND_PID" >"$PID_FILE"
cd "$ROOT"

for _ in $(seq 1 120); do
  if port_open "$PORT"; then
    echo "Listo. Abre $URL"
    [[ "${NO_OPEN:-0}" == "1" ]] || xdg-open "$URL" >/dev/null 2>&1 || true
    exit 0
  fi

  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "El servidor se detuvo inesperadamente. Últimas líneas del log:" >&2
    tail -n 30 "$LOG_FILE" >&2 || true
    rm -f "$PID_FILE"
    exit 1
  fi

  sleep 1
done

echo "El servidor no respondió en 120 segundos. Revisa $LOG_FILE" >&2
exit 1
