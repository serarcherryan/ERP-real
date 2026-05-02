#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WEB_PORT="${WEB_PORT:-5173}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
DRY_RUN="${ERP_SCRIPT_DRY_RUN:-0}"

cd "$ROOT_DIR"

if [ "$DRY_RUN" != "1" ] && ! command -v docker >/dev/null 2>&1; then
  echo "docker is required for the dev environment." >&2
  exit 1
fi

if [ "$DRY_RUN" != "1" ] && ! command -v npm >/dev/null 2>&1; then
  echo "npm is required for the web admin app." >&2
  exit 1
fi

if [ "$DRY_RUN" != "1" ] && ! command -v mvn >/dev/null 2>&1; then
  echo "mvn is required for the backend app." >&2
  exit 1
fi

if [ "$DRY_RUN" != "1" ] && [ ! -d "$ROOT_DIR/node_modules" ]; then
  echo "Installing npm dependencies..."
  npm install
fi

echo "Starting PostgreSQL with Docker Compose..."
if [ "$DRY_RUN" = "1" ]; then
  echo "DRY RUN: docker compose -f docker/dev/docker-compose.yml up -d postgres"
else
  docker compose -f docker/dev/docker-compose.yml up -d postgres
  echo "Waiting for PostgreSQL to become healthy..."
  until docker compose -f docker/dev/docker-compose.yml exec -T postgres pg_isready -U erp -d erp_real >/dev/null 2>&1; do
    sleep 2
  done
fi

cleanup() {
  if [ -n "${BACKEND_PID:-}" ]; then
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

echo "Starting backend on port ${BACKEND_PORT}..."
if [ "$DRY_RUN" = "1" ]; then
  echo "DRY RUN: cd apps/backend && SERVER_PORT=${BACKEND_PORT} SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run"
else
  (
    cd "$ROOT_DIR/apps/backend"
    SERVER_PORT="$BACKEND_PORT" SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
  ) &
  BACKEND_PID=$!
fi

echo "Starting web admin on port ${WEB_PORT}..."
echo "Backend: http://localhost:${BACKEND_PORT}"
echo "Web:     http://localhost:${WEB_PORT}"
if [ "$DRY_RUN" = "1" ]; then
  echo "DRY RUN: PORT=${WEB_PORT} npm run dev:web -- --port ${WEB_PORT}"
else
  PORT="$WEB_PORT" npm run dev:web -- --port "$WEB_PORT"
fi
