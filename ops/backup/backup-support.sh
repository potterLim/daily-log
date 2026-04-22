#!/usr/bin/env bash
set -euo pipefail

DAY_LOG_ALERT_WEBHOOK_URL="${DAY_LOG_ALERT_WEBHOOK_URL:-}"

json_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  printf '%s' "${value}"
}

send_operational_alert() {
  local alert_type="$1"
  local message="$2"

  if [[ -z "${DAY_LOG_ALERT_WEBHOOK_URL}" ]]; then
    return 0
  fi

  local payload
  payload="$(printf '{"application":"dayLog","alertType":"%s","message":"%s"}' "${alert_type}" "$(json_escape "${message}")")"

  if command -v curl >/dev/null 2>&1; then
    curl -fsS -X POST \
      -H 'Content-Type: application/json' \
      -d "${payload}" \
      "${DAY_LOG_ALERT_WEBHOOK_URL}" >/dev/null
    return 0
  fi

  if command -v wget >/dev/null 2>&1; then
    printf '%s' "${payload}" \
      | wget -qO- \
        --header='Content-Type: application/json' \
        --post-data=- \
        "${DAY_LOG_ALERT_WEBHOOK_URL}" >/dev/null
    return 0
  fi

  echo "Operational alert could not be sent because neither curl nor wget is available." >&2
  return 1
}

normalize_boolean() {
  local value="${1:-false}"
  printf '%s' "${value,,}"
}
