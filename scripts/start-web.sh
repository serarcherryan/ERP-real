#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PORT="${PORT:-5173}"
DRY_RUN="${ERP_SCRIPT_DRY_RUN:-0}"

cd "$ROOT_DIR"

if [ "$DRY_RUN" != "1" ] && ! command -v npm >/dev/null 2>&1; then
  echo "npm is required to start the web admin app." >&2
  exit 1
fi

if [ "$DRY_RUN" != "1" ] && [ ! -d "$ROOT_DIR/node_modules" ]; then
  echo "Installing dependencies..."
  npm install
fi

echo "Starting ERP web admin on port ${PORT}..."
if [ "$DRY_RUN" = "1" ]; then
  echo "DRY RUN: npm run dev:web -- --port ${PORT}"
else
  exec npm run dev:web -- --port "$PORT"
fi
