#!/usr/bin/env bash
# Offline self-extracting installer for archinfra/nacos-skillhub.
# This file is concatenated with payload.tar.gz by packaging/offline-run/build.sh.
set -euo pipefail

PROGRAM_NAME="nacos-skillhub"
ACTION="${1:-help}"
INSTALL_MODE="binary"
NACOS_MODE="standalone"
INSTALL_DIR="/opt/nacos-skillhub"
SERVICE_NAME="nacos-skillhub"
DOCKER_NAME="nacos-skillhub"
REGISTRY=""
REGISTRY_USER=""
REGISTRY_PASS=""
SKIP_IMAGE_PREPARE="false"
YES="false"
HTTP_PORT="8848"
GRPC_PORT="9848"
RAFT_PORT="9849"
WAIT_TIMEOUT="120s"
KEEP_WORKDIR="false"
WORKDIR="${TMPDIR:-/tmp}/nacos-skillhub-installer.$$"

log() { printf '[INFO] %s\n' "$*"; }
warn() { printf '[WARN] %s\n' "$*" >&2; }
die() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }
need_cmd() { command -v "$1" >/dev/null 2>&1 || die "Missing command: $1"; }

usage() {
  cat <<'USAGE'
Usage:
  ./nacos-skillhub-<version>-<arch>.run install [options]
  ./nacos-skillhub-<version>-<arch>.run status [options]
  ./nacos-skillhub-<version>-<arch>.run uninstall [options]
  ./nacos-skillhub-<version>-<arch>.run unpack --install-dir <dir>

Actions:
  install       Install from embedded payload.
  status        Show systemd/docker status.
  uninstall     Stop and remove created service/container. Data directory is kept by default.
  unpack        Extract payload only, without starting service/container.
  help          Show this help.

Options:
  --install-mode <binary|docker|unpack>  Install mode. Default: binary.
  --nacos-mode <standalone|cluster>      Nacos startup mode. Default: standalone.
  --install-dir <dir>                    Binary install directory. Default: /opt/nacos-skillhub.
  --service-name <name>                  systemd service name. Default: nacos-skillhub.
  --docker-name <name>                   Docker container name. Default: nacos-skillhub.
  --registry <repo-prefix>               Retag/push loaded image to internal registry, for example sealos.hub:5000/kube4.
  --registry-user <user>                 Registry username.
  --registry-pass <pass>                 Registry password.
  --skip-image-prepare                   Skip docker load/tag/push.
  --http-port <port>                     Host HTTP port. Default: 8848.
  --grpc-port <port>                     Host gRPC port. Default: 9848.
  --raft-port <port>                     Host raft port. Default: 9849.
  --wait-timeout <duration>              Reserved for future wait logic. Default: 120s.
  --keep-workdir                         Do not delete temporary extracted payload.
  -y, --yes                              Skip confirmation.
  -h, --help                             Show help.

Examples:
  ./nacos-skillhub-v0.1.0-amd64.run install -y
  ./nacos-skillhub-v0.1.0-amd64.run install --install-mode docker -y
  ./nacos-skillhub-v0.1.0-amd64.run install --install-mode docker --registry sealos.hub:5000/kube4 --registry-user admin --registry-pass PASSW9RD -y
USAGE
}

shift_action() {
  case "${ACTION}" in
    install|status|uninstall|unpack|help|-h|--help) shift || true ;;
    *) die "Unknown action: ${ACTION}. Use help." ;;
  esac
}

parse_args() {
  shift_action "$@"
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --install-mode) INSTALL_MODE="${2:?}"; shift 2 ;;
      --nacos-mode) NACOS_MODE="${2:?}"; shift 2 ;;
      --install-dir) INSTALL_DIR="${2:?}"; shift 2 ;;
      --service-name) SERVICE_NAME="${2:?}"; shift 2 ;;
      --docker-name) DOCKER_NAME="${2:?}"; shift 2 ;;
      --registry) REGISTRY="${2:?}"; shift 2 ;;
      --registry-user) REGISTRY_USER="${2:?}"; shift 2 ;;
      --registry-pass) REGISTRY_PASS="${2:?}"; shift 2 ;;
      --skip-image-prepare) SKIP_IMAGE_PREPARE="true"; shift ;;
      --http-port) HTTP_PORT="${2:?}"; shift 2 ;;
      --grpc-port) GRPC_PORT="${2:?}"; shift 2 ;;
      --raft-port) RAFT_PORT="${2:?}"; shift 2 ;;
      --wait-timeout) WAIT_TIMEOUT="${2:?}"; shift 2 ;;
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
  if [[ "${YES}" == "true" ]]; then
    return 0
  fi
  printf 'Continue? [y/N] '
  read -r answer
  [[ "${answer}" == "y" || "${answer}" == "Y" ]] || die "Cancelled"
}

first_binary_tar() {
  find "${WORKDIR}/binary" -maxdepth 1 -type f \( -name '*.tar.gz' -o -name '*.tgz' -o -name '*.zip' \) | sort | head -n 1
}

install_binary() {
  local archive
  archive="$(first_binary_tar)"
  [[ -n "${archive}" ]] || die "No binary distribution archive found in payload/binary"
  need_cmd tar
  log "Installing binary distribution to ${INSTALL_DIR}"
  confirm
  mkdir -p "${INSTALL_DIR}"
  case "${archive}" in
    *.tar.gz|*.tgz)
      tar -xzf "${archive}" -C "${INSTALL_DIR}" --strip-components=1 || tar -xzf "${archive}" -C "${INSTALL_DIR}"
      ;;
    *.zip)
      need_cmd unzip
      unzip -q "${archive}" -d "${INSTALL_DIR}.tmp"
      shopt -s dotglob nullglob
      local entries=("${INSTALL_DIR}.tmp"/*)
      if [[ ${#entries[@]} -eq 1 && -d "${entries[0]}" ]]; then
        cp -a "${entries[0]}"/. "${INSTALL_DIR}"/
      else
        cp -a "${INSTALL_DIR}.tmp"/. "${INSTALL_DIR}"/
      fi
      rm -rf "${INSTALL_DIR}.tmp"
      ;;
    *) die "Unsupported binary archive: ${archive}" ;;
  esac
  chmod +x "${INSTALL_DIR}"/bin/*.sh 2>/dev/null || true

  if command -v systemctl >/dev/null 2>&1 && [[ "$(id -u)" -eq 0 ]]; then
    log "Creating systemd service ${SERVICE_NAME}"
    cat > "/etc/systemd/system/${SERVICE_NAME}.service" <<EOF
[Unit]
Description=ArchInfra Nacos SkillHub
After=network-online.target
Wants=network-online.target

[Service]
Type=forking
WorkingDirectory=${INSTALL_DIR}
Environment=MODE=${NACOS_MODE}
ExecStart=/bin/bash ${INSTALL_DIR}/bin/startup.sh -m ${NACOS_MODE}
ExecStop=/bin/bash ${INSTALL_DIR}/bin/shutdown.sh
Restart=on-failure
RestartSec=10
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOF
    systemctl daemon-reload
    systemctl enable "${SERVICE_NAME}"
    systemctl restart "${SERVICE_NAME}"
    systemctl --no-pager status "${SERVICE_NAME}" || true
  else
    warn "systemd not available or not root. Start manually: ${INSTALL_DIR}/bin/startup.sh -m ${NACOS_MODE}"
  fi
}

retarget_ref() {
  local default_ref="$1" leaf
  leaf="${default_ref##*/}"
  printf '%s/%s\n' "${REGISTRY%/}" "${leaf}"
}

prepare_images() {
  local index line name tar_name load_ref default_ref platform pull dockerfile target_ref image_tar
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
  prepare_images
  local image_ref
  image_ref="$(first_image_ref)"
  [[ -n "${image_ref}" ]] || die "No image ref found in payload"
  log "Starting docker container ${DOCKER_NAME} with image ${image_ref}"
  confirm
  docker rm -f "${DOCKER_NAME}" >/dev/null 2>&1 || true
  docker run -d \
    --name "${DOCKER_NAME}" \
    --restart unless-stopped \
    -e MODE="${NACOS_MODE}" \
    -p "${HTTP_PORT}:8848" \
    -p "${GRPC_PORT}:9848" \
    -p "${RAFT_PORT}:9849" \
    "${image_ref}"
  docker ps --filter "name=${DOCKER_NAME}"
}

unpack_payload() {
  log "Payload extracted to ${INSTALL_DIR}"
  confirm
  mkdir -p "${INSTALL_DIR}"
  cp -a "${WORKDIR}"/. "${INSTALL_DIR}"/
}

show_status() {
  if command -v systemctl >/dev/null 2>&1; then
    systemctl --no-pager status "${SERVICE_NAME}" || true
  fi
  if command -v docker >/dev/null 2>&1; then
    docker ps -a --filter "name=${DOCKER_NAME}" || true
  fi
}

uninstall_all() {
  log "Uninstalling service/container created by this installer. Data directory is kept: ${INSTALL_DIR}"
  confirm
  if command -v systemctl >/dev/null 2>&1 && [[ "$(id -u)" -eq 0 ]]; then
    systemctl stop "${SERVICE_NAME}" >/dev/null 2>&1 || true
    systemctl disable "${SERVICE_NAME}" >/dev/null 2>&1 || true
    rm -f "/etc/systemd/system/${SERVICE_NAME}.service"
    systemctl daemon-reload || true
  fi
  if command -v docker >/dev/null 2>&1; then
    docker rm -f "${DOCKER_NAME}" >/dev/null 2>&1 || true
  fi
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
  unpack)
    extract_payload
    unpack_payload
    ;;
  install)
    case "${INSTALL_MODE}" in
      binary)
        extract_payload
        install_binary
        ;;
      docker)
        extract_payload
        install_docker
        ;;
      unpack)
        extract_payload
        unpack_payload
        ;;
      *) die "Unsupported --install-mode: ${INSTALL_MODE}" ;;
    esac
    ;;
esac
exit 0

__PAYLOAD_BELOW__
