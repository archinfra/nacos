#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"
bash -n scripts/build-frontend.sh scripts/build-backend.sh scripts/build-all.sh scripts/find-nacos-dist.sh
bash -n packaging/run-docker/build.sh packaging/run-docker/install.sh
bash -n packaging/run-k8s/build.sh packaging/run-k8s/install.sh
python3 -m json.tool console-ui-next/package.json >/dev/null
[[ -f console-ui-next/build/copyFile.cjs ]] || { echo "missing copyFile.cjs" >&2; exit 1; }
node --check console-ui-next/build/copyFile.cjs
if [[ -f packaging/docker/Dockerfile ]]; then
  grep -q 'NACOS_TARBALL' packaging/docker/Dockerfile || { echo "Dockerfile missing NACOS_TARBALL" >&2; exit 1; }
fi
[[ -f packaging/k8s/nacos-skillhub-standalone.yaml.tmpl ]] || { echo "missing k8s template" >&2; exit 1; }
grep -q '__IMAGE_REF__' packaging/k8s/nacos-skillhub-standalone.yaml.tmpl || { echo "k8s template missing image placeholder" >&2; exit 1; }
echo "static verification passed"
