#!/usr/bin/env bash
set -Eeuo pipefail

BACKUP_DIR=${BACKUP_DIR:-./backups}
BACKUP_RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-7}
BACKUP_S3_URI=${BACKUP_S3_URI:?set BACKUP_S3_URI, e.g. s3://crmix-backups/prod}
POSTGRES_DB=${POSTGRES_DB:-crmix}
POSTGRES_ADMIN_USER=${POSTGRES_ADMIN_USER:-postgres}
COMPOSE_FILES=${COMPOSE_FILES:--f docker-compose.yml}
TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
NAME="crmix-${TIMESTAMP}.dump"
FILE="${BACKUP_DIR}/${NAME}"

command -v docker >/dev/null || { echo "docker is required" >&2; exit 1; }
command -v aws >/dev/null || { echo "aws CLI is required for off-server backup" >&2; exit 1; }
mkdir -p "${BACKUP_DIR}"

# shellcheck disable=SC2086
docker compose ${COMPOSE_FILES} exec -T postgres \
  pg_dump -U "${POSTGRES_ADMIN_USER}" -d "${POSTGRES_DB}" -Fc > "${FILE}"

test -s "${FILE}" || { echo "backup is empty" >&2; exit 1; }
sha256sum "${FILE}" > "${FILE}.sha256"

aws s3 cp "${FILE}" "${BACKUP_S3_URI%/}/${NAME}" --only-show-errors
aws s3 cp "${FILE}.sha256" "${BACKUP_S3_URI%/}/${NAME}.sha256" --only-show-errors

find "${BACKUP_DIR}" -type f \( -name 'crmix-*.dump' -o -name 'crmix-*.dump.sha256' \) -mtime "+${BACKUP_RETENTION_DAYS}" -delete

echo "backup=${FILE}"
echo "remote=${BACKUP_S3_URI%/}/${NAME}"
