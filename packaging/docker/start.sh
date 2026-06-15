#!/usr/bin/env sh
set -eu

NACOS_HOME="${NACOS_HOME:-/home/nacos}"
MODE="${MODE:-standalone}"
LOG_DIR="${NACOS_HOME}/logs"

log() { printf '[nacos-start] %s\n' "$*"; }

cd "${NACOS_HOME}"
mkdir -p "${LOG_DIR}"

log "starting nacos with mode=${MODE}"
bin/startup.sh -m "${MODE}"

# startup.sh starts Nacos in the background. Keep the container foreground process alive
# by following the first log file that Nacos actually creates. Different Nacos
# versions/images may create startup.log, start.out, or nacos.log at different times.
for i in $(seq 1 180); do
  for file in \
    "${LOG_DIR}/startup.log" \
    "${LOG_DIR}/start.out" \
    "${LOG_DIR}/nacos.log" \
    "${LOG_DIR}/nacos.log.0"; do
    if [ -s "${file}" ]; then
      log "following log file: ${file}"
      exec tail -n +1 -F "${file}"
    fi
  done
  sleep 1
done

log "Nacos started but no known log file appeared under ${LOG_DIR}; keeping container alive."
ls -lah "${LOG_DIR}" || true
exec tail -f /dev/null
