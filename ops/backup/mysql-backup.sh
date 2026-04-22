#!/usr/bin/env bash
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:?MYSQL_DATABASE must be set.}"
MYSQL_USER="${MYSQL_USER:?MYSQL_USER must be set.}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?MYSQL_PASSWORD must be set.}"
DAY_LOG_BACKUP_DIR="${DAY_LOG_BACKUP_DIR:-/backups}"
DAY_LOG_BACKUP_RETENTION_DAYS="${DAY_LOG_BACKUP_RETENTION_DAYS:-14}"

timestamp="$(date +"%Y%m%d-%H%M%S")"
backup_file_path="${DAY_LOG_BACKUP_DIR}/daylog-${timestamp}.sql.gz"

mkdir -p "${DAY_LOG_BACKUP_DIR}"

export MYSQL_PWD="${MYSQL_PASSWORD}"

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

find "${DAY_LOG_BACKUP_DIR}" -type f -name 'daylog-*.sql.gz' -mtime +"${DAY_LOG_BACKUP_RETENTION_DAYS}" -delete

echo "Backup completed: ${backup_file_path}"
