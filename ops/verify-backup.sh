#!/usr/bin/env bash
set -Eeuo pipefail

SOURCE=${1:?usage: verify-backup.sh <local.dump|s3://bucket/path.dump>}
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

if [[ "${SOURCE}" == s3://* ]]; then
  command -v aws >/dev/null || { echo "aws CLI is required" >&2; exit 1; }
  FILE="${TMP_DIR}/verify.dump"
  aws s3 cp "${SOURCE}" "${FILE}" --only-show-errors
else
  FILE=${SOURCE}
fi

test -s "${FILE}" || { echo "backup is empty or missing" >&2; exit 1; }
docker run --rm -i -v "$(cd "$(dirname "${FILE}")" && pwd):/backup:ro" postgres:16-alpine \
  pg_restore --list "/backup/$(basename "${FILE}")" >/dev/null

echo "backup archive is readable: ${SOURCE}"
