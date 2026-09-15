#!/usr/bin/env bash
set -Eeuo pipefail

SOURCE=${1:?usage: restore-postgres.sh <local.dump|s3://bucket/path.dump>}
POSTGRES_DB=${POSTGRES_DB:-crmix}
POSTGRES_ADMIN_USER=${POSTGRES_ADMIN_USER:-postgres}
COMPOSE_FILES=${COMPOSE_FILES:--f docker-compose.yml}
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

if [[ "${SOURCE}" == s3://* ]]; then
  command -v aws >/dev/null || { echo "aws CLI is required for S3 restore" >&2; exit 1; }
  FILE="${TMP_DIR}/restore.dump"
  aws s3 cp "${SOURCE}" "${FILE}" --only-show-errors
  if aws s3 cp "${SOURCE}.sha256" "${FILE}.sha256" --only-show-errors 2>/dev/null; then
    EXPECTED=$(awk '{print $1}' "${FILE}.sha256")
    ACTUAL=$(sha256sum "${FILE}" | awk '{print $1}')
    [[ "${EXPECTED}" == "${ACTUAL}" ]] || { echo "backup checksum mismatch" >&2; exit 1; }
  fi
else
  FILE=${SOURCE}
  [[ -f "${FILE}" ]] || { echo "backup file not found: ${FILE}" >&2; exit 1; }
  if [[ -f "${FILE}.sha256" ]]; then
    (cd "$(dirname "${FILE}")" && sha256sum -c "$(basename "${FILE}.sha256")")
  fi
fi

cat "${FILE}" | {
  # shellcheck disable=SC2086
  docker compose ${COMPOSE_FILES} exec -T postgres \
    pg_restore -U "${POSTGRES_ADMIN_USER}" -d "${POSTGRES_DB}" --clean --if-exists --no-owner --no-privileges
}

echo "restore completed from ${SOURCE}"
