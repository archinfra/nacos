#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"
bash scripts/build-frontend.sh
bash scripts/build-backend.sh --skip-frontend
