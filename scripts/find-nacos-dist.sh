#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"
DIST="$(find distribution/target -type f -name 'nacos-server-*.tar.gz' | sort | tail -n 1 || true)"
if [[ -z "${DIST}" ]]; then
  DIST="$(find distribution/target -type f -name '*.tar.gz' | sort | tail -n 1 || true)"
fi
if [[ -z "${DIST}" ]]; then
  echo "No nacos distribution tar.gz found under distribution/target" >&2
  exit 1
fi
printf '%s\n' "${DIST}"
