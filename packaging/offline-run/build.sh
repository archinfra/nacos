#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
INSTALL_SH="${SCRIPT_DIR}/install.sh"
NAME="nacos-skillhub"
ARCH=""
VERSION=""
BINARY_ARCHIVE=""
DOCKER_IMAGE_TAR=""
IMAGE_REF=""
OUT_DIR="${REPO_ROOT}/dist"

log() { printf '[INFO] %s\n' "$*"; }
die() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }
need_cmd() { command -v "$1" >/dev/null 2>&1 || die "Missing command: $1"; }

usage() {
  cat <<'USAGE'
Usage:
  bash packaging/offline-run/build.sh \
    --arch amd64|arm64 \
    --version <version> \
    --binary <nacos-server.tar.gz|zip> \
    --docker-image <docker-image.tar|tar.gz> \
    --image-ref <image-ref> \
    --out-dir <dist-dir>

This script creates a self-extracting .run installer:
  dist/nacos-skillhub-<version>-<arch>.run
  dist/nacos-skillhub-<version>-<arch>.run.sha256

The payload contains:
  VERSION
  binary/<distribution archive>
  images/<docker image tar or tar.gz>
  images/image-index.tsv
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --arch) ARCH="${2:?}"; shift 2 ;;
    --version) VERSION="${2:?}"; shift 2 ;;
    --binary) BINARY_ARCHIVE="${2:?}"; shift 2 ;;
    --docker-image) DOCKER_IMAGE_TAR="${2:?}"; shift 2 ;;
    --image-ref) IMAGE_REF="${2:?}"; shift 2 ;;
    --out-dir) OUT_DIR="${2:?}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) die "Unknown option: $1" ;;
  esac
done

[[ "${ARCH}" == "amd64" || "${ARCH}" == "arm64" ]] || die "--arch must be amd64 or arm64"
[[ -n "${VERSION}" ]] || die "--version is required"
[[ -f "${BINARY_ARCHIVE}" ]] || die "--binary not found: ${BINARY_ARCHIVE}"
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
mkdir -p "${PAYLOAD_DIR}/binary" "${PAYLOAD_DIR}/images"

BIN_NAME="$(basename "${BINARY_ARCHIVE}")"
IMG_NAME="$(basename "${DOCKER_IMAGE_TAR}")"
PLATFORM="linux/${ARCH}"

log "Assembling payload for ${NAME} ${VERSION} ${ARCH}"
printf '%s\n' "${VERSION}" > "${PAYLOAD_DIR}/VERSION"
cp "${BINARY_ARCHIVE}" "${PAYLOAD_DIR}/binary/${BIN_NAME}"
cp "${DOCKER_IMAGE_TAR}" "${PAYLOAD_DIR}/images/${IMG_NAME}"

cat > "${PAYLOAD_DIR}/images/image-index.tsv" <<EOF
name|tar_name|load_ref|default_target_ref|platform|pull|dockerfile
${NAME}|${IMG_NAME}|${IMAGE_REF}|${IMAGE_REF}|${PLATFORM}||
EOF

cat > "${PAYLOAD_DIR}/MANIFEST.txt" <<EOF
name=${NAME}
version=${VERSION}
arch=${ARCH}
platform=${PLATFORM}
binary=${BIN_NAME}
image_tar=${IMG_NAME}
image_ref=${IMAGE_REF}
build_time_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

PAYLOAD_TGZ="${WORKDIR}/payload.tar.gz"
(
  cd "${PAYLOAD_DIR}"
  tar -czf "${PAYLOAD_TGZ}" .
)
tar -tzf "${PAYLOAD_TGZ}" >/dev/null

RUN_FILE="${OUT_DIR}/${NAME}-${VERSION}-${ARCH}.run"
log "Writing ${RUN_FILE}"
cat "${INSTALL_SH}" "${PAYLOAD_TGZ}" > "${RUN_FILE}"
chmod +x "${RUN_FILE}"
sha256sum "${RUN_FILE}" > "${RUN_FILE}.sha256"

log "Created:"
ls -lah "${RUN_FILE}" "${RUN_FILE}.sha256"
