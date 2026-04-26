#!/usr/bin/env bash
set -euo pipefail

script_directory_path="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${script_directory_path}/backup-support.sh"

if [[ $# -ne 1 ]]; then
  echo "Usage: mysql-restore.sh <backup-file.sql.gz>" >&2
  exit 1
fi

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:?MYSQL_DATABASE must be set.}"
MYSQL_USER="${MYSQL_USER:?MYSQL_USER must be set.}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?MYSQL_PASSWORD must be set.}"
backup_file_path="$1"
DAYMARK_BACKUP_VERIFY_TABLES="${DAYMARK_BACKUP_VERIFY_TABLES:-flyway_schema_history,user_account,daymark_entry}"
DAYMARK_RESTORE_NOTIFY_ON_SUCCESS="$(normalize_boolean "${DAYMARK_RESTORE_NOTIFY_ON_SUCCESS:-false}")"

if [[ ! -f "${backup_file_path}" ]]; then
  echo "Backup file not found: ${backup_file_path}" >&2
  exit 1
fi

export MYSQL_PWD="${MYSQL_PASSWORD}"

handle_restore_failure() {
  local line_number="$1"
  local message="MySQL restore failed. database=${MYSQL_DATABASE}, file=$(basename "${backup_file_path}"), line=${line_number}"

  echo "${message}" >&2
  send_operational_alert "mysql-restore-failed" "${message}" || true
}

trap 'handle_restore_failure ${LINENO}' ERR

checksum_file_path="${backup_file_path}.sha256"
if [[ -f "${checksum_file_path}" ]]; then
  sha256sum -c "${checksum_file_path}"
fi

gunzip -c "${backup_file_path}" | mysql \
  --host="${MYSQL_HOST}" \
  --port="${MYSQL_PORT}" \
  --user="${MYSQL_USER}" \
  --default-character-set=utf8mb4 \
  "${MYSQL_DATABASE}"

verification_results=()
IFS=',' read -r -a verify_tables <<< "${DAYMARK_BACKUP_VERIFY_TABLES}"
for table_name in "${verify_tables[@]}"; do
  trimmed_table_name="$(printf '%s' "${table_name}" | xargs)"
  if [[ -z "${trimmed_table_name}" ]]; then
    continue
  fi

  row_count="$(
    mysql \
      --host="${MYSQL_HOST}" \
      --port="${MYSQL_PORT}" \
      --user="${MYSQL_USER}" \
      --default-character-set=utf8mb4 \
      --batch \
      --skip-column-names \
      "${MYSQL_DATABASE}" \
      -e "SELECT COUNT(*) FROM \`${trimmed_table_name}\`;"
  )"
  verification_results+=("${trimmed_table_name}=${row_count}")
done

restore_message="Restore completed from $(basename "${backup_file_path}")"
if [[ ${#verification_results[@]} -gt 0 ]]; then
  restore_message="${restore_message}. verifiedTables=$(IFS=';'; echo "${verification_results[*]}")"
fi

echo "${restore_message}"
if [[ "${DAYMARK_RESTORE_NOTIFY_ON_SUCCESS}" == "true" ]]; then
  send_operational_alert "mysql-restore-succeeded" "${restore_message}" || true
fi
