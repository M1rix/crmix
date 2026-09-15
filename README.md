# CRMIX

CRMIX is a multi-tenant CRM SaaS implemented as a modular monolith with Java 21, Spring Boot 3, PostgreSQL 16, React, TypeScript and Tailwind CSS.

## Architecture guarantees

- package-by-feature modular monolith with bounded contexts
- `/api/v1` contract and RFC 7807 problem responses
- Flyway is the only schema authority; Hibernate runs with `ddl-auto=validate`
- tenant isolation is enforced by PostgreSQL `FORCE ROW LEVEL SECURITY`, not only application filters
- JWT access tokens + rotating opaque refresh tokens
- appointment overlap protection is enforced by a PostgreSQL exclusion constraint
- payment mutations are idempotent and Payme webhooks implement provider state transitions
- business revenue is derived only from successful appointment payments

See [`docs/architecture.md`](docs/architecture.md) and [`docs/sot-and-roadmap.md`](docs/sot-and-roadmap.md).

## Local development

```bash
cp .env.example .env
docker compose up --build
```

Open `http://localhost:8088`. Backend health is available at `http://localhost:8088/actuator/health`.

The application DB connection uses the non-superuser `crmix_app` role so PostgreSQL Row Level Security remains enforceable.

## Production deployment

1. Copy `.env.example` to a host-managed `.env` and replace every placeholder with real secrets. Never commit `.env`.
2. Configure DNS and TLS termination in front of CRMIX. The bundled Nginx expects the public proxy to forward `X-Forwarded-Proto`; production must be served over HTTPS.
3. Start the application and monitoring overlays:

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  -f docker-compose.monitoring.yml \
  up -d --build
```

4. Verify:

```bash
curl -fsS http://127.0.0.1:${APP_PORT:-8088}/actuator/health
```

5. Configure the Telegram webhook with `X-Telegram-Bot-Api-Secret-Token`, configure Payme Merchant API credentials, and set `SUPPORT_TELEGRAM_CHAT_ID`.
6. Install the backup cron from [`docs/runbooks/database-backup-restore.md`](docs/runbooks/database-backup-restore.md).
7. Keep Grafana bound to localhost or a private admin network; expose it only through authenticated infrastructure.

## Load smoke

Install k6 and use a non-production test tenant:

```bash
TENANT_SLUG=load-test \
CRMIX_EMAIL=owner@example.com \
CRMIX_PASSWORD='...' \
BASE_URL=https://crm.example.com \
k6 run ops/k6/smoke.js
```

The scenario targets 50 requests/second and fails if error rate reaches 1%, p95 exceeds 750 ms, or p99 exceeds 1500 ms.

## Operations

- Database backup/restore: [`docs/runbooks/database-backup-restore.md`](docs/runbooks/database-backup-restore.md)
- Incident response: [`docs/runbooks/incident-response.md`](docs/runbooks/incident-response.md)
- Pilot onboarding: [`docs/pilot/onboarding.md`](docs/pilot/onboarding.md)

## Git hooks and CI

After cloning, run `git config core.hooksPath .githooks` once. CI runs backend tests/build and frontend lint/tests/build. CI intentionally does **not** upload build artifacts.
