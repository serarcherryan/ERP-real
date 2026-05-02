#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DRY_RUN="${ERP_SCRIPT_DRY_RUN:-0}"
cd "$ROOT_DIR"

if [ "$DRY_RUN" != "1" ] && ! command -v docker >/dev/null 2>&1; then
  echo "docker is required for the dev backend environment." >&2
  exit 1
fi

if [ "$DRY_RUN" != "1" ] && ! command -v mvn >/dev/null 2>&1; then
  echo "mvn is required for the backend app." >&2
  exit 1
fi

if [ "$DRY_RUN" = "1" ]; then
  echo "DRY RUN: docker compose -f docker/dev/docker-compose.yml up -d postgres"
else
  docker compose -f docker/dev/docker-compose.yml up -d postgres
  echo "Waiting for PostgreSQL to become healthy..."
  until docker compose -f docker/dev/docker-compose.yml exec -T postgres pg_isready -U erp -d erp_real >/dev/null 2>&1; do
    sleep 2
  done
fi
cd apps/backend
if [ "$DRY_RUN" = "1" ]; then
  echo "DRY RUN: cd apps/backend && SERVER_PORT=${BACKEND_PORT:-8080} SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run"
else
  SERVER_PORT="${BACKEND_PORT:-8080}" SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
fi
