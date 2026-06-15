#!/usr/bin/env bash
# Offline self-extracting Docker installer for archinfra/nacos-skillhub.
# This file is concatenated with payload.tar.gz by packaging/run-docker/build.sh.
set -euo pipefail

PROGRAM_NAME="nacos-skillhub"
ACTION="help"
NACOS_MODE="standalone"
DOCKER_NAME="nacos-skillhub"
REGISTRY=""
REGISTRY_USER=""
REGISTRY_PASS=""
SKIP_IMAGE_PREPARE="false"
YES="false"
HTTP_PORT="8848"
GRPC_PORT="9848"
RAFT_PORT="9849"
AUTH_ENABLE="false"
AUTH_TOKEN=""
IDENTITY_KEY="serverIdentity"
IDENTITY_VALUE="security"
JAVA_OPT_EXT=""
KEEP_WORKDIR="false"
WORKDIR="${TMPDIR:-/tmp}/nacos-skillhub-docker-installer.$$"

log() { printf '[INFO] %s\n' "$*"; }
warn() { printf '[WARN] %s\n' "$*" >&2; }
die() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }
need_cmd() { command -v "$1" >/dev/null 2>&1 || die "Missing command: $1"; }

usage() {
  cat <<'USAGE'
Usage:
  ./nacos-skillhub-<version>-<arch>-docker.run install [options]
  ./nacos-skillhub-<version>-<arch>-docker.run status [options]
  ./nacos-skillhub-<version>-<arch>-docker.run uninstall [options]
  ./nacos-skillhub-<version>-<arch>-docker.run unpack --install-dir <dir>

Actions:
  install       Load image and start a Docker container.
  status        Show Docker container status.
  uninstall     Stop and remove the Docker container.
  unpack        Extract embedded payload only.
  help          Show this help.

Options:
  --nacos-mode <standalone|cluster>      Nacos startup mode. Default: standalone.
  --docker-name <name>                   Docker container name. Default: nacos-skillhub.
  --registry <repo-prefix>               Retag/push loaded image to internal registry, for example sealos.hub:5000/kube4.
  --registry-user <user>                 Registry username.
  --registry-pass <pass>                 Registry password.
  --skip-image-prepare                   Skip docker load/tag/push.
  --http-port <port>                     Host HTTP port. Default: 8848.
  --grpc-port <port>                     Host gRPC port. Default: 9848.
  --raft-port <port>                     Host raft port. Default: 9849.
  --auth-enable <true|false>             Enable Nacos auth. Default: false.
  --auth-token <token>                   Nacos auth token. Required when auth is enabled in production.
  --identity-key <key>                   Nacos identity key. Default: serverIdentity.
  --identity-value <value>               Nacos identity value. Default: security.
  --java-opt-ext <value>                 Extra JAVA_OPT_EXT passed to container.
  --keep-workdir                         Do not delete temporary extracted payload.
  -y, --yes                              Skip confirmation.
  -h, --help                             Show help.

Examples:
  ./nacos-skillhub-v0.1.0-amd64-docker.run install -y
  ./nacos-skillhub-v0.1.0-amd64-docker.run install --auth-enable true --auth-token '<32+ chars>' -y
  ./nacos-skillhub-v0.1.0-amd64-docker.run install --registry sealos.hub:5000/kube4 --registry-user admin --registry-pass PASSW9RD -y
USAGE
}

parse_args() {
  if [[ $# -eq 0 ]]; then
    ACTION="help"
  else
    case "$1" in
      install|status|uninstall|unpack|help) ACTION="$1"; shift ;;
      -h|--help) ACTION="help"; shift ;;
      -*) ACTION="install" ;;
      *) die "Unknown action: $1. Use help." ;;
    esac
  fi
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --nacos-mode) NACOS_MODE="${2:?}"; shift 2 ;;
      --docker-name) DOCKER_NAME="${2:?}"; shift 2 ;;
      --registry) REGISTRY="${2:?}"; shift 2 ;;
      --registry-user) REGISTRY_USER="${2:?}"; shift 2 ;;
      --registry-pass) REGISTRY_PASS="${2:?}"; shift 2 ;;
      --skip-image-prepare) SKIP_IMAGE_PREPARE="true"; shift ;;
      --http-port) HTTP_PORT="${2:?}"; shift 2 ;;
      --grpc-port) GRPC_PORT="${2:?}"; shift 2 ;;
      --raft-port) RAFT_PORT="${2:?}"; shift 2 ;;
      --auth-enable) AUTH_ENABLE="${2:?}"; shift 2 ;;
      --auth-token) AUTH_TOKEN="${2:?}"; shift 2 ;;
      --identity-key) IDENTITY_KEY="${2:?}"; shift 2 ;;
      --identity-value) IDENTITY_VALUE="${2:?}"; shift 2 ;;
      --java-opt-ext) JAVA_OPT_EXT="${2:?}"; shift 2 ;;
      --install-dir) INSTALL_DIR_COMPAT="${2:?}"; shift 2 ;;
      -n|--namespace) NAMESPACE_COMPAT="${2:?}"; warn "-n/--namespace is ignored by docker.run. Use k8s.run for Kubernetes."; shift 2 ;;
      --keep-workdir) KEEP_WORKDIR="true"; shift ;;
      -y|--yes) YES="true"; shift ;;
      -h|--help) ACTION="help"; shift ;;
      *) die "Unknown option: $1" ;;
    esac
  done
}

payload_start_offset() {
  local marker_line payload_offset skip_bytes byte_hex
  marker_line="$(awk '/^__PAYLOAD_BELOW__$/ { print NR; exit }' "$0")"
  [[ -n "${marker_line}" ]] || die "Payload marker not found"
  payload_offset="$(( $(head -n "${marker_line}" "$0" | wc -c | tr -d ' ') + 1 ))"
  skip_bytes=0
  while :; do
    byte_hex="$(dd if="$0" bs=1 skip="$((payload_offset + skip_bytes - 1))" count=1 2>/dev/null | od -An -tx1 | tr -d ' \n')"
    case "${byte_hex}" in
      0a|0d) skip_bytes=$((skip_bytes + 1)) ;;
      "") die "Payload is empty" ;;
      *) break ;;
    esac
  done
  printf '%s\n' "$((payload_offset + skip_bytes))"
}

extract_payload() {
  need_cmd tar
  rm -rf "${WORKDIR}"
  mkdir -p "${WORKDIR}"
  tail -c +"$(payload_start_offset)" "$0" | tar -xzf - -C "${WORKDIR}" || die "Failed to extract payload"
  [[ -f "${WORKDIR}/VERSION" ]] || die "Payload is missing VERSION"
}

confirm() {
  [[ "${YES}" == "true" ]] && return 0
  printf 'Continue? [y/N] '
  read -r answer
  [[ "${answer}" == "y" || "${answer}" == "Y" ]] || die "Cancelled"
}

retarget_ref() {
  local default_ref="$1" leaf
  leaf="${default_ref##*/}"
  printf '%s/%s\n' "${REGISTRY%/}" "${leaf}"
}

prepare_images() {
  local index name tar_name load_ref default_ref platform pull dockerfile target_ref image_tar
  [[ "${SKIP_IMAGE_PREPARE}" == "true" ]] && { log "Skip image prepare"; return 0; }
  need_cmd docker
  index="${WORKDIR}/images/image-index.tsv"
  [[ -f "${index}" ]] || die "Payload is missing images/image-index.tsv"

  if [[ -n "${REGISTRY}" && -n "${REGISTRY_USER}" ]]; then
    log "Logging in to ${REGISTRY}"
    printf '%s' "${REGISTRY_PASS}" | docker login "${REGISTRY}" -u "${REGISTRY_USER}" --password-stdin
  fi

  while IFS='|' read -r name tar_name load_ref default_ref platform pull dockerfile; do
    [[ -z "${name}" || "${name}" == "name" ]] && continue
    image_tar="${WORKDIR}/images/${tar_name}"
    [[ -f "${image_tar}" ]] || die "Image tar not found: ${image_tar}"
    log "Loading image ${name} from ${tar_name}"
    case "${image_tar}" in
      *.gz) gzip -dc "${image_tar}" | docker load ;;
      *) docker load -i "${image_tar}" ;;
    esac
    if [[ -n "${REGISTRY}" ]]; then
      target_ref="$(retarget_ref "${default_ref:-${load_ref}}")"
      log "Retag ${load_ref} -> ${target_ref}"
      docker tag "${load_ref}" "${target_ref}"
      log "Push ${target_ref}"
      docker push "${target_ref}"
    fi
  done < "${index}"
}

first_image_ref() {
  local index name tar_name load_ref default_ref platform pull dockerfile
  index="${WORKDIR}/images/image-index.tsv"
  while IFS='|' read -r name tar_name load_ref default_ref platform pull dockerfile; do
    [[ -z "${name}" || "${name}" == "name" ]] && continue
    if [[ -n "${REGISTRY}" ]]; then
      retarget_ref "${default_ref:-${load_ref}}"
    else
      printf '%s\n' "${load_ref}"
    fi
    return 0
  done < "${index}"
  return 1
}

install_docker() {
  need_cmd docker
  extract_payload
  prepare_images
  local image_ref
  image_ref="$(first_image_ref)"
  [[ -n "${image_ref}" ]] || die "No image ref found in payload"
  if [[ "${AUTH_ENABLE}" == "true" && -z "${AUTH_TOKEN}" ]]; then
    die "--auth-token is required when --auth-enable true"
  fi
  log "Starting Docker container ${DOCKER_NAME} with image ${image_ref}"
  confirm
  docker rm -f "${DOCKER_NAME}" >/dev/null 2>&1 || true
  docker run -d \
    --name "${DOCKER_NAME}" \
    --restart unless-stopped \
    -e MODE="${NACOS_MODE}" \
    -e NACOS_AUTH_ENABLE="${AUTH_ENABLE}" \
    -e NACOS_AUTH_TOKEN="${AUTH_TOKEN}" \
    -e NACOS_AUTH_IDENTITY_KEY="${IDENTITY_KEY}" \
    -e NACOS_AUTH_IDENTITY_VALUE="${IDENTITY_VALUE}" \
    -e JAVA_OPT_EXT="${JAVA_OPT_EXT}" \
    -p "${HTTP_PORT}:8848" \
    -p "${GRPC_PORT}:9848" \
    -p "${RAFT_PORT}:9849" \
    "${image_ref}"
  docker ps --filter "name=${DOCKER_NAME}"
}

unpack_payload() {
  local dir="${INSTALL_DIR_COMPAT:-./nacos-skillhub-docker-payload}"
  extract_payload
  log "Payload extracted to ${dir}"
  confirm
  mkdir -p "${dir}"
  cp -a "${WORKDIR}"/. "${dir}"/
}

show_status() {
  need_cmd docker
  docker ps -a --filter "name=${DOCKER_NAME}" || true
}

uninstall_all() {
  need_cmd docker
  log "Removing Docker container ${DOCKER_NAME}"
  confirm
  docker rm -f "${DOCKER_NAME}" >/dev/null 2>&1 || true
}

cleanup() {
  if [[ "${KEEP_WORKDIR}" != "true" ]]; then
    rm -rf "${WORKDIR}" >/dev/null 2>&1 || true
  else
    log "Workdir kept: ${WORKDIR}"
  fi
}
trap cleanup EXIT

parse_args "$@"
case "${ACTION}" in
  help|-h|--help) usage ;;
  status) show_status ;;
  uninstall) uninstall_all ;;
  unpack) unpack_payload ;;
  install) install_docker ;;
esac
exit 0

__PAYLOAD_BELOW__
