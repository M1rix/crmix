# Incident response runbook

## First five minutes

1. Confirm impact from the public health endpoint and Grafana/Prometheus.
2. Record the UTC start time, affected tenant(s), deployment SHA and symptoms.
3. Do not restart PostgreSQL or delete data as a first response.
4. If errors began immediately after deployment, stop further rollout and prepare rollback to the previous known-good image/commit.
5. Preserve application logs using the correlation/trace ID from RFC 7807 responses.

## Backend down

- Check container health and recent logs.
- Check PostgreSQL health and connection saturation.
- Verify disk availability and host memory pressure.
- Verify required production env variables were not removed.
- If the process cannot recover safely, roll back the application version before attempting database changes.

## Elevated 5xx rate

- Split errors by route/status in Prometheus.
- Trace a representative request via `X-Correlation-Id`.
- Check external dependencies: Telegram and Payme failures must not be confused with PostgreSQL/application failures.
- If one tenant or workload triggers the spike, preserve tenant isolation; never disable RLS as a mitigation.

## Database incident

Follow `database-backup-restore.md`. Take a fresh preservation backup before destructive restore when the database is readable. Restore into an isolated environment first whenever time permits.

## Security incident

- Rotate JWT, Payme, Telegram and DB credentials according to the affected surface.
- Revoke refresh tokens when authentication compromise is suspected.
- Preserve access/application logs and timestamps.
- Do not weaken CORS, RLS, webhook secrets or edge rate limits to restore service.

## Recovery validation

Before declaring recovery:

- `/actuator/health` is UP
- backend/frontend CI is green for the deployed SHA
- owner login succeeds
- tenant A cannot observe tenant B data
- appointment read/create path works
- billing page remains reachable for suspended tenants
- Prometheus scrape is healthy and 5xx rate returned to baseline

After the incident, document root cause, detection gap, customer impact and a concrete prevention action.
