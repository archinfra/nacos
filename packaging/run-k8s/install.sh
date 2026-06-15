#!/usr/bin/env bash
# Offline self-extracting Kubernetes installer for archinfra/nacos-skillhub.
# This file is concatenated with payload.tar.gz by packaging/run-k8s/build.sh.
set -euo pipefail

PROGRAM_NAME="nacos-skillhub"
ACTION="help"
NAMESPACE="nacos-system"
RELEASE_NAME="nacos-skillhub"
NACOS_MODE="standalone"
REPLICAS="1"
SERVICE_TYPE="ClusterIP"
STORAGE_CLASS=""
STORAGE_SIZE="10Gi"
REGISTRY=""
REGISTRY_USER=""
REGISTRY_PASS=""
SKIP_IMAGE_PREPARE="false"
IMAGE_PULL_POLICY="IfNotPresent"
YES="false"
WAIT_TIMEOUT="180s"
AUTH_ENABLE="false"
AUTH_TOKEN=""
IDENTITY_KEY="serverIdentity"
IDENTITY_VALUE="security"
JAVA_OPT_EXT=""
DELETE_PVC="false"
KEEP_WORKDIR="false"
WORKDIR="${TMPDIR:-/tmp}/nacos-skillhub-k8s-installer.$$"

log() { printf '[INFO] %s\n' "$*"; }
warn() { printf '[WARN] %s\n' "$*" >&2; }
die() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }
need_cmd() { command -v "$1" >/dev/null 2>&1 || die "Missing command: $1"; }

base64_decoded_len() {
  local token="$1" len
  if ! len="$(printf '%s' "${token}" | base64 -d 2>/dev/null | wc -c | tr -d ' ')"; then
    return 1
  fi
  [[ -n "${len}" ]] || return 1
  printf '%s\n' "${len}"
}

generate_nacos_auth_token() {
  local raw random_hex
  if [[ -r /dev/urandom ]]; then
    random_hex="$(head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n')"
  else
    random_hex="$(date +%s%N)-$$"
  fi
  raw="nacos-skillhub-${PROGRAM_NAME}-$(date +%s%N)-${random_hex}"
  printf '%s' "${raw}" | base64 | tr -d '\n'
}

ensure_nacos_auth_token() {
  local decoded_len raw_len
  if [[ -z "${AUTH_TOKEN}" ]]; then
    AUTH_TOKEN="$(generate_nacos_auth_token)"
    warn "--auth-token not provided. Generated a valid Base64 NACOS_AUTH_TOKEN for this installation. Save the generated Secret if you need stable credentials across reinstall."
    return 0
  fi

  if decoded_len="$(base64_decoded_len "${AUTH_TOKEN}")" && [[ "${decoded_len}" -gt 32 ]]; then
    return 0
  fi

  raw_len="${#AUTH_TOKEN}"
  if [[ "${raw_len}" -gt 32 ]]; then
    warn "--auth-token looks like a raw string, not Base64. Encoding it automatically for Nacos."
    AUTH_TOKEN="$(printf '%s' "${AUTH_TOKEN}" | base64 | tr -d '\n')"
    return 0
  fi

  die "Invalid --auth-token. Nacos requires a Base64 string whose decoded original value is longer than 32 characters. Example: printf '%s' 'your-very-long-secret-over-32-chars' | base64 -w0"
}


usage() {
  cat <<'USAGE'
Usage:
  ./nacos-skillhub-<version>-<arch>-k8s.run install [options]
  ./nacos-skillhub-<version>-<arch>-k8s.run status [options]
  ./nacos-skillhub-<version>-<arch>-k8s.run uninstall [options]
  ./nacos-skillhub-<version>-<arch>-k8s.run unpack --install-dir <dir>

Actions:
  install       Load/push image, render manifests, and kubectl apply.
  status        Show Kubernetes resources.
  uninstall     Delete Deployment/Service/ServiceAccount. PVC is kept by default.
  unpack        Extract embedded payload only.
  help          Show this help.

Options:
  -n, --namespace <ns>                   Kubernetes namespace. Default: nacos-system.
  --release-name <name>                  Kubernetes resource name prefix. Default: nacos-skillhub.
  --nacos-mode <standalone|cluster>      Nacos startup mode. Default: standalone.
  --replicas <n>                         Deployment replicas. Default: 1. Standalone should stay 1.
  --service-type <ClusterIP|NodePort|LoadBalancer>  Kubernetes Service type. Default: ClusterIP.
  --storage-class <name>                 PVC storageClassName. Empty means cluster default.
  --storage-size <size>                  PVC size. Default: 10Gi.
  --registry <repo-prefix>               Internal registry prefix, for example sealos.hub:5000/kube4.
  --registry-user <user>                 Registry username.
  --registry-pass <pass>                 Registry password.
  --skip-image-prepare                   Skip docker load/tag/push.
  --image-pull-policy <policy>           IfNotPresent|Always|Never. Default: IfNotPresent.
  --auth-enable <true|false>             Enable Nacos auth. Default: false.
  --auth-token <token>                   Nacos auth token. Required when auth is enabled in production.
  --identity-key <key>                   Nacos identity key. Default: serverIdentity.
  --identity-value <value>               Nacos identity value. Default: security.
  --java-opt-ext <value>                 Extra JAVA_OPT_EXT passed to pod.
  --wait-timeout <duration>              kubectl rollout timeout. Default: 180s.
  --delete-pvc                           Also delete PVC during uninstall.
  --keep-workdir                         Do not delete temporary extracted payload.
  -y, --yes                              Skip confirmation.
  -h, --help                             Show help.

Examples:
  ./nacos-skillhub-v0.1.0-amd64-k8s.run install -n a11 --registry sealos.hub:5000/kube4 --registry-user admin --registry-pass PASSW9RD -y
  ./nacos-skillhub-v0.1.0-amd64-k8s.run status -n a11
  ./nacos-skillhub-v0.1.0-amd64-k8s.run uninstall -n a11 -y
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
      -n|--namespace) NAMESPACE="${2:?}"; shift 2 ;;
      --release-name) RELEASE_NAME="${2:?}"; shift 2 ;;
      --nacos-mode) NACOS_MODE="${2:?}"; shift 2 ;;
      --replicas) REPLICAS="${2:?}"; shift 2 ;;
      --service-type) SERVICE_TYPE="${2:?}"; shift 2 ;;
      --storage-class) STORAGE_CLASS="${2:?}"; shift 2 ;;
      --storage-size) STORAGE_SIZE="${2:?}"; shift 2 ;;
      --registry) REGISTRY="${2:?}"; shift 2 ;;
      --registry-user) REGISTRY_USER="${2:?}"; shift 2 ;;
      --registry-pass) REGISTRY_PASS="${2:?}"; shift 2 ;;
      --skip-image-prepare) SKIP_IMAGE_PREPARE="true"; shift ;;
      --image-pull-policy) IMAGE_PULL_POLICY="${2:?}"; shift 2 ;;
      --auth-enable) AUTH_ENABLE="${2:?}"; shift 2 ;;
      --auth-token) AUTH_TOKEN="${2:?}"; shift 2 ;;
      --identity-key) IDENTITY_KEY="${2:?}"; shift 2 ;;
      --identity-value) IDENTITY_VALUE="${2:?}"; shift 2 ;;
      --java-opt-ext) JAVA_OPT_EXT="${2:?}"; shift 2 ;;
      --wait-timeout) WAIT_TIMEOUT="${2:?}"; shift 2 ;;
      --delete-pvc) DELETE_PVC="true"; shift ;;
      --install-dir) INSTALL_DIR_COMPAT="${2:?}"; shift 2 ;;
      --docker-name|--http-port|--grpc-port|--raft-port)
        die "$1 is a Docker installer option. Use the -docker.run package."
        ;;
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
  [[ -f "${WORKDIR}/manifests/nacos-skillhub-standalone.yaml.tmpl" ]] || die "Payload is missing k8s manifest template"
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

  if [[ -z "${REGISTRY}" ]]; then
    warn "--registry not provided. The manifest will use the embedded image ref. In multi-node Kubernetes, nodes may not be able to pull it. Prefer --registry for offline delivery."
  fi

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

sed_escape() {
  printf '%s' "$1" | sed -e 's/[\\&#]/\\&/g'
}

render_manifest() {
  local image_ref storage_class_block rendered tpl
  image_ref="$(first_image_ref)"
  [[ -n "${image_ref}" ]] || die "No image ref found in payload"
  ensure_nacos_auth_token
  if [[ "${NACOS_MODE}" == "standalone" && "${REPLICAS}" != "1" ]]; then
    warn "standalone mode should use --replicas 1. Current replicas=${REPLICAS}."
  fi
  storage_class_block=""
  if [[ -n "${STORAGE_CLASS}" ]]; then
    storage_class_block="  storageClassName: ${STORAGE_CLASS}"
  fi
  tpl="${WORKDIR}/manifests/nacos-skillhub-standalone.yaml.tmpl"
  rendered="${WORKDIR}/rendered.yaml"
  sed \
    -e "s#__NAMESPACE__#$(sed_escape "${NAMESPACE}")#g" \
    -e "s#__RELEASE_NAME__#$(sed_escape "${RELEASE_NAME}")#g" \
    -e "s#__SERVICE_TYPE__#$(sed_escape "${SERVICE_TYPE}")#g" \
    -e "s#__STORAGE_CLASS_BLOCK__#$(sed_escape "${storage_class_block}")#g" \
    -e "s#__STORAGE_SIZE__#$(sed_escape "${STORAGE_SIZE}")#g" \
    -e "s#__REPLICAS__#$(sed_escape "${REPLICAS}")#g" \
    -e "s#__IMAGE_REF__#$(sed_escape "${image_ref}")#g" \
    -e "s#__IMAGE_PULL_POLICY__#$(sed_escape "${IMAGE_PULL_POLICY}")#g" \
    -e "s#__NACOS_MODE__#$(sed_escape "${NACOS_MODE}")#g" \
    -e "s#__AUTH_ENABLE__#$(sed_escape "${AUTH_ENABLE}")#g" \
    -e "s#__AUTH_TOKEN__#$(sed_escape "${AUTH_TOKEN}")#g" \
    -e "s#__IDENTITY_KEY__#$(sed_escape "${IDENTITY_KEY}")#g" \
    -e "s#__IDENTITY_VALUE__#$(sed_escape "${IDENTITY_VALUE}")#g" \
    -e "s#__JAVA_OPT_EXT__#$(sed_escape "${JAVA_OPT_EXT}")#g" \
    "${tpl}" > "${rendered}"
  printf '%s\n' "${rendered}"
}

install_k8s() {
  need_cmd kubectl
  extract_payload
  prepare_images
  local rendered
  rendered="$(render_manifest)"
  log "Applying Nacos SkillHub to namespace ${NAMESPACE}, release ${RELEASE_NAME}"
  confirm
  kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -
  kubectl apply -f "${rendered}"
  kubectl rollout status deployment/"${RELEASE_NAME}" -n "${NAMESPACE}" --timeout="${WAIT_TIMEOUT}" || true
  kubectl get deploy,po,svc,pvc -n "${NAMESPACE}" -l "app.kubernetes.io/instance=${RELEASE_NAME}"
}

unpack_payload() {
  local dir="${INSTALL_DIR_COMPAT:-./nacos-skillhub-k8s-payload}"
  extract_payload
  log "Payload extracted to ${dir}"
  confirm
  mkdir -p "${dir}"
  cp -a "${WORKDIR}"/. "${dir}"/
}

show_status() {
  need_cmd kubectl
  kubectl get deploy,po,svc,pvc -n "${NAMESPACE}" -l "app.kubernetes.io/instance=${RELEASE_NAME}" || true
}

uninstall_all() {
  need_cmd kubectl
  log "Deleting Kubernetes resources release=${RELEASE_NAME} namespace=${NAMESPACE}. PVC kept by default."
  confirm
  kubectl delete deployment,service,serviceaccount -n "${NAMESPACE}" -l "app.kubernetes.io/instance=${RELEASE_NAME}" --ignore-not-found=true || true
  if [[ "${DELETE_PVC}" == "true" ]]; then
    kubectl delete pvc -n "${NAMESPACE}" -l "app.kubernetes.io/instance=${RELEASE_NAME}" --ignore-not-found=true || true
  else
    warn "PVC kept. Add --delete-pvc to remove data PVC."
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
  unpack) unpack_payload ;;
  install) install_k8s ;;
esac
exit 0

__PAYLOAD_BELOW__
