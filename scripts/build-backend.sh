#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SKIP_FRONTEND="false"
MAVEN_PROFILE="release-nacos"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-frontend) SKIP_FRONTEND="true"; shift ;;
    --profile) MAVEN_PROFILE="${2:?}"; shift 2 ;;
    -h|--help)
      cat <<USAGE
Usage: bash scripts/build-backend.sh [--skip-frontend] [--profile release-nacos]

Build Nacos backend distribution. Unless --skip-frontend is set, this script first
ensures console-ui-next is built and copied into console/src/main/resources/static/next.
USAGE
      exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

log() { printf '[backend] %s\n' "$*"; }
die() { printf '[backend][ERROR] %s\n' "$*" >&2; exit 1; }

cd "${ROOT_DIR}"
[[ -f pom.xml ]] || die "pom.xml not found in repository root"

if [[ "${SKIP_FRONTEND}" != "true" ]]; then
  if [[ ! -f console/src/main/resources/static/next/index.html ]]; then
    log "Frontend static files not found, building frontend first"
    bash scripts/build-frontend.sh
  else
    log "Frontend static files already exist, skip frontend build"
  fi
fi

MVN="mvn"
if [[ -f ./mvnw ]]; then
  chmod +x ./mvnw
  MVN="./mvnw"
fi

FLAGS=("-B" "-ntp" "-DskipTests" "-Dmaven.test.skip=true" "-Drat.skip=true" "-Dcheckstyle.skip=true")
log "Building Maven distribution with profile: ${MAVEN_PROFILE}"
if ! "${MVN}" "${FLAGS[@]}" "-P${MAVEN_PROFILE}" clean install; then
  log "Profile ${MAVEN_PROFILE} failed, falling back to normal Maven build"
  "${MVN}" "${FLAGS[@]}" clean install
fi

log "Build artifacts under distribution/target:"
find distribution/target -maxdepth 2 -type f \( -name '*.tar.gz' -o -name '*.zip' -o -name '*.jar' \) -print 2>/dev/null | sort || true
