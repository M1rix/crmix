# PostgreSQL backup and restore runbook

## Production policy

CRMIX production backups are PostgreSQL custom-format (`pg_dump -Fc`) archives. A backup is considered successful only after it is copied to storage outside the application server. `BACKUP_S3_URI` is therefore mandatory for the production backup command.

Recommended schedule: daily at 02:30 server-local time, retain at least 7 local copies and configure bucket lifecycle retention independently (30+ days recommended).

## Required host tools

- Docker + Docker Compose
- AWS CLI configured for the backup bucket or an S3-compatible endpoint
- write access to `BACKUP_DIR`

For S3-compatible providers, configure the AWS CLI profile/endpoint in the host environment rather than hard-coding credentials in this repository.

## Backup

```bash
BACKUP_S3_URI=s3://company-crmix-backups/prod \
BACKUP_DIR=/var/backups/crmix \
./ops/backup-postgres.sh
```

The script creates an archive and SHA-256 checksum, uploads both, then removes expired local copies.

Example cron entry:

```cron
30 2 * * * cd /srv/crmix && BACKUP_S3_URI=s3://company-crmix-backups/prod BACKUP_DIR=/var/backups/crmix ./ops/backup-postgres.sh >> /var/log/crmix-backup.log 2>&1
```

## Verify

A backup must periodically be validated rather than merely uploaded:

```bash
./ops/verify-backup.sh s3://company-crmix-backups/prod/crmix-YYYYMMDDTHHMMSSZ.dump
```

At least monthly, perform a restore rehearsal against an isolated PostgreSQL instance.

## Restore

Stop write traffic first. Record the current incident timestamp and preserve the broken database before destructive recovery.

```bash
./ops/restore-postgres.sh s3://company-crmix-backups/prod/crmix-YYYYMMDDTHHMMSSZ.dump
```

After restore, restart the backend and verify `/actuator/health`, tenant login, an appointment lookup and the Flyway schema history before reopening traffic.

## Failure handling

If upload fails, the backup command exits non-zero and must alert through the host scheduler/monitoring. Never delete the database or old remote backup during an unsuccessful backup run.
