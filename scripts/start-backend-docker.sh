#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DRY_RUN="${ERP_SCRIPT_DRY_RUN:-0}"
cd "$ROOT_DIR"

if [ "$DRY_RUN" = "1" ]; then
  echo "DRY RUN: docker compose -f docker/dev/docker-compose.yml --profile backend up --build"
else
  docker compose -f docker/dev/docker-compose.yml --profile backend up --build
fi
