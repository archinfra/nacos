#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
INSTALL_SH="${SCRIPT_DIR}/install.sh"
NAME="nacos-skillhub"
ARCH=""
VERSION=""
DOCKER_IMAGE_TAR=""
IMAGE_REF=""
OUT_DIR="${REPO_ROOT}/dist"

log() { printf '[INFO] %s\n' "$*"; }
die() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }
need_cmd() { command -v "$1" >/dev/null 2>&1 || die "Missing command: $1"; }

usage() {
  cat <<'USAGE'
Usage:
  bash packaging/run-docker/build.sh \
    --arch amd64|arm64 \
    --version <version> \
    --docker-image <docker-image.tar|tar.gz> \
    --image-ref <image-ref> \
    --out-dir <dist-dir>

Creates:
  dist/nacos-skillhub-<version>-<arch>-docker.run
  dist/nacos-skillhub-<version>-<arch>-docker.run.sha256

This run package is for single-node Docker delivery only.
Use packaging/run-k8s/build.sh for Kubernetes delivery.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --arch) ARCH="${2:?}"; shift 2 ;;
    --version) VERSION="${2:?}"; shift 2 ;;
    --docker-image) DOCKER_IMAGE_TAR="${2:?}"; shift 2 ;;
    --image-ref) IMAGE_REF="${2:?}"; shift 2 ;;
    --out-dir) OUT_DIR="${2:?}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) die "Unknown option: $1" ;;
  esac
done

[[ "${ARCH}" == "amd64" || "${ARCH}" == "arm64" ]] || die "--arch must be amd64 or arm64"
[[ -n "${VERSION}" ]] || die "--version is required"
[[ -f "${DOCKER_IMAGE_TAR}" ]] || die "--docker-image not found: ${DOCKER_IMAGE_TAR}"
[[ -n "${IMAGE_REF}" ]] || die "--image-ref is required"
[[ -f "${INSTALL_SH}" ]] || die "install.sh not found: ${INSTALL_SH}"
grep -q '^__PAYLOAD_BELOW__$' "${INSTALL_SH}" || die "install.sh must contain marker line: __PAYLOAD_BELOW__"

need_cmd tar
need_cmd sha256sum
need_cmd awk
need_cmd gzip

mkdir -p "${OUT_DIR}"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "${WORKDIR}"' EXIT
PAYLOAD_DIR="${WORKDIR}/payload"
mkdir -p "${PAYLOAD_DIR}/images"

IMG_NAME="$(basename "${DOCKER_IMAGE_TAR}")"
PLATFORM="linux/${ARCH}"

log "Assembling Docker run payload for ${NAME} ${VERSION} ${ARCH}"
printf '%s\n' "${VERSION}" > "${PAYLOAD_DIR}/VERSION"
cp "${DOCKER_IMAGE_TAR}" "${PAYLOAD_DIR}/images/${IMG_NAME}"

cat > "${PAYLOAD_DIR}/images/image-index.tsv" <<EOF2
name|tar_name|load_ref|default_target_ref|platform|pull|dockerfile
${NAME}|${IMG_NAME}|${IMAGE_REF}|${IMAGE_REF}|${PLATFORM}||
EOF2

cat > "${PAYLOAD_DIR}/MANIFEST.txt" <<EOF2
name=${NAME}
type=docker-run
version=${VERSION}
arch=${ARCH}
platform=${PLATFORM}
image_tar=${IMG_NAME}
image_ref=${IMAGE_REF}
build_time_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF2

PAYLOAD_TGZ="${WORKDIR}/payload.tar.gz"
(
  cd "${PAYLOAD_DIR}"
  tar -czf "${PAYLOAD_TGZ}" .
)
tar -tzf "${PAYLOAD_TGZ}" >/dev/null

RUN_FILE="${OUT_DIR}/${NAME}-${VERSION}-${ARCH}-docker.run"
log "Writing ${RUN_FILE}"
cat "${INSTALL_SH}" "${PAYLOAD_TGZ}" > "${RUN_FILE}"
chmod +x "${RUN_FILE}"
sha256sum "${RUN_FILE}" > "${RUN_FILE}.sha256"

log "Created:"
ls -lah "${RUN_FILE}" "${RUN_FILE}.sha256"
