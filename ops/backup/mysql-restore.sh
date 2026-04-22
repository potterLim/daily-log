#!/usr/bin/env bash
set -euo pipefail

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

if [[ ! -f "${backup_file_path}" ]]; then
  echo "Backup file not found: ${backup_file_path}" >&2
  exit 1
fi

export MYSQL_PWD="${MYSQL_PASSWORD}"

gunzip -c "${backup_file_path}" | mysql \
  --host="${MYSQL_HOST}" \
  --port="${MYSQL_PORT}" \
  --user="${MYSQL_USER}" \
  --default-character-set=utf8mb4 \
  "${MYSQL_DATABASE}"

echo "Restore completed from: ${backup_file_path}"
