#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UI_DIR="${ROOT_DIR}/console-ui-next"
STATIC_DIR="${ROOT_DIR}/console/src/main/resources/static/next"

log() { printf '[frontend] %s\n' "$*"; }
die() { printf '[frontend][ERROR] %s\n' "$*" >&2; exit 1; }

[[ -d "${UI_DIR}" ]] || die "console-ui-next directory not found: ${UI_DIR}"
[[ -f "${UI_DIR}/package.json" ]] || die "package.json not found: ${UI_DIR}/package.json"
command -v npm >/dev/null 2>&1 || die "npm is required"
command -v node >/dev/null 2>&1 || die "node is required"

log "Node: $(node -v), npm: $(npm -v)"
cd "${UI_DIR}"

if [[ -f package-lock.json ]]; then
  log "Installing dependencies with npm ci"
  npm ci
else
  log "Installing dependencies with npm install"
  npm install
fi

log "Building console-ui-next and copying static files into backend console"
npm run build

[[ -f "${STATIC_DIR}/index.html" ]] || die "frontend static index.html not found after build: ${STATIC_DIR}/index.html"
log "Frontend static files are ready: ${STATIC_DIR}"
