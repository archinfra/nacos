#!/usr/bin/env sh
set -eu

NACOS_HOME="${NACOS_HOME:-/home/nacos}"
APP_FILE="${NACOS_HOME}/conf/application.properties"

log() { printf '[entrypoint] %s\n' "$*"; }

set_prop() {
  key="$1"
  value="$2"
  [ -n "${value}" ] || return 0
  if [ ! -f "${APP_FILE}" ]; then
    log "application.properties not found: ${APP_FILE}"
    return 0
  fi
  escaped_key="$(printf '%s' "${key}" | sed 's/[.[\*^$()+?{}|]/\\&/g')"
  escaped_value="$(printf '%s' "${value}" | sed 's/[\\&]/\\&/g')"
  if grep -q "^${escaped_key}=" "${APP_FILE}"; then
    sed -i "s#^${escaped_key}=.*#${key}=${escaped_value}#" "${APP_FILE}"
  else
    printf '\n%s=%s\n' "${key}" "${value}" >> "${APP_FILE}"
  fi
}

generate_token() {
  if command -v od >/dev/null 2>&1 && [ -r /dev/urandom ]; then
    rand="$(od -An -tx1 -N32 /dev/urandom | tr -d ' \n')"
  else
    rand="$(date +%s)-$$"
  fi
  raw="nacos-skillhub-auto-token-$(date +%s)-${rand}"
  printf '%s' "${raw}" | base64 | tr -d '\n'
}

# Nacos source distribution startup.sh validates application.properties before Java starts.
# Therefore env vars alone are not enough in this custom image; write them back to config first.
if [ -f "${APP_FILE}" ]; then
  if [ -z "${NACOS_AUTH_TOKEN:-}" ]; then
    current_token="$(grep '^nacos.core.auth.plugin.nacos.token.secret.key=' "${APP_FILE}" | tail -n 1 | cut -d= -f2- || true)"
    if [ -z "${current_token}" ]; then
      NACOS_AUTH_TOKEN="$(generate_token)"
      export NACOS_AUTH_TOKEN
      log "NACOS_AUTH_TOKEN not provided; generated one for this container. Use a fixed Secret in production."
    fi
  fi

  set_prop "nacos.core.auth.plugin.nacos.token.secret.key" "${NACOS_AUTH_TOKEN:-}"
  set_prop "nacos.core.auth.server.identity.key" "${NACOS_AUTH_IDENTITY_KEY:-${NACOS_AUTH_IDENTITY_KEY:-serverIdentity}}"
  set_prop "nacos.core.auth.server.identity.value" "${NACOS_AUTH_IDENTITY_VALUE:-${NACOS_AUTH_IDENTITY_VALUE:-security}}"
  set_prop "nacos.core.auth.enabled" "${NACOS_AUTH_ENABLE:-}"
else
  log "Skip config injection because ${APP_FILE} does not exist."
fi

exec "$@"
