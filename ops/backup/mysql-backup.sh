#!/usr/bin/env bash
set -euo pipefail

script_directory_path="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${script_directory_path}/backup-support.sh"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:?MYSQL_DATABASE must be set.}"
MYSQL_USER="${MYSQL_USER:?MYSQL_USER must be set.}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?MYSQL_PASSWORD must be set.}"
DAYMARK_BACKUP_DIR="${DAYMARK_BACKUP_DIR:-/backups}"
DAYMARK_BACKUP_RETENTION_DAYS="${DAYMARK_BACKUP_RETENTION_DAYS:-14}"
DAYMARK_BACKUP_NOTIFY_ON_SUCCESS="$(normalize_boolean "${DAYMARK_BACKUP_NOTIFY_ON_SUCCESS:-false}")"

timestamp="$(date +"%Y%m%d-%H%M%S")"
backup_file_path="${DAYMARK_BACKUP_DIR}/daymark-${timestamp}.sql.gz"
checksum_file_path="${backup_file_path}.sha256"

mkdir -p "${DAYMARK_BACKUP_DIR}"

export MYSQL_PWD="${MYSQL_PASSWORD}"

handle_backup_failure() {
  local line_number="$1"
  local message="MySQL backup failed. database=${MYSQL_DATABASE}, line=${line_number}"

  echo "${message}" >&2
  send_operational_alert "mysql-backup-failed" "${message}" || true
}

trap 'handle_backup_failure ${LINENO}' ERR

mysqldump \
  --host="${MYSQL_HOST}" \
  --port="${MYSQL_PORT}" \
  --user="${MYSQL_USER}" \
  --default-character-set=utf8mb4 \
  --single-transaction \
  --quick \
  --routines \
  --events \
  "${MYSQL_DATABASE}" \
  | gzip > "${backup_file_path}"

sha256sum "${backup_file_path}" > "${checksum_file_path}"

find "${DAYMARK_BACKUP_DIR}" -type f -name 'daymark-*.sql.gz' -mtime +"${DAYMARK_BACKUP_RETENTION_DAYS}" -delete
find "${DAYMARK_BACKUP_DIR}" -type f -name 'daymark-*.sql.gz.sha256' -mtime +"${DAYMARK_BACKUP_RETENTION_DAYS}" -delete

backup_file_size_bytes="$(wc -c < "${backup_file_path}")"
backup_message="MySQL backup completed. database=${MYSQL_DATABASE}, file=$(basename "${backup_file_path}"), sizeBytes=${backup_file_size_bytes}"

echo "${backup_message}"
if [[ "${DAYMARK_BACKUP_NOTIFY_ON_SUCCESS}" == "true" ]]; then
  send_operational_alert "mysql-backup-succeeded" "${backup_message}" || true
fi
