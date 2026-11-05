#!/usr/bin/env bash
# ============================================================
# stop.sh — Detiene el servidor arrancado con ./start.sh
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$ROOT/.run/backend.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "No hay ningún servidor registrado por start.sh."
  exit 0
fi

PID="$(cat "$PID_FILE")"

if ! kill -0 "$PID" 2>/dev/null; then
  echo "El servidor ya estaba detenido."
  rm -f "$PID_FILE"
  exit 0
fi

echo "Deteniendo servidor (PID $PID)..."
kill -TERM -- "-$PID" 2>/dev/null || kill -TERM "$PID" 2>/dev/null || true

for _ in $(seq 1 20); do
  kill -0 "$PID" 2>/dev/null || break
  sleep 1
done

if kill -0 "$PID" 2>/dev/null; then
  echo "Forzando cierre..."
  kill -KILL -- "-$PID" 2>/dev/null || kill -KILL "$PID" 2>/dev/null || true
fi

rm -f "$PID_FILE"
echo "Servidor detenido."
