#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8848}"
NAMESPACE_ID="${NAMESPACE_ID:-public}"
SKILL_NAME="${SKILL_NAME:-novel-dialogue-card}"
LABEL="${LABEL:-latest}"
OUT_DIR="${OUT_DIR:-/tmp/nacos-skillhub-smoke}"

mkdir -p "${OUT_DIR}"
ZIP_FILE="${OUT_DIR}/${SKILL_NAME}.zip"
HEADERS_FILE="${OUT_DIR}/headers.txt"

printf '[INFO] Runtime download: %s\n' "${SKILL_NAME}"
curl -sS -D "${HEADERS_FILE}" -L \
  "${BASE_URL}/v3/client/ai/skills?namespaceId=${NAMESPACE_ID}&name=${SKILL_NAME}&label=${LABEL}" \
  -o "${ZIP_FILE}"

if ! grep -qi '^HTTP/.* 200' "${HEADERS_FILE}"; then
  echo '[ERROR] expected HTTP 200'
  cat "${HEADERS_FILE}"
  exit 1
fi

MD5="$(awk 'tolower($1)=="x-nacos-skill-md5:" {print $2}' "${HEADERS_FILE}" | tr -d '\r' | tail -1)"
if [[ -z "${MD5}" ]]; then
  echo '[ERROR] missing X-Nacos-Skill-Md5 header'
  cat "${HEADERS_FILE}"
  exit 1
fi

unzip -l "${ZIP_FILE}" | grep -q 'SKILL.md' || {
  echo '[ERROR] downloaded zip does not contain SKILL.md'
  exit 1
}

printf '[INFO] Runtime cache validation with md5=%s\n' "${MD5}"
curl -sS -D "${HEADERS_FILE}.304" -L \
  "${BASE_URL}/v3/client/ai/skills?namespaceId=${NAMESPACE_ID}&name=${SKILL_NAME}&label=${LABEL}&md5=${MD5}" \
  -o /dev/null

grep -qi '^HTTP/.* 304' "${HEADERS_FILE}.304" || {
  echo '[ERROR] expected HTTP 304'
  cat "${HEADERS_FILE}.304"
  exit 1
}

echo '[OK] SkillHub runtime smoke test passed.'
