# CRMIX

CRMIX is a multi-tenant CRM SaaS implemented as a modular monolith with Java 21, Spring Boot 3, PostgreSQL 16, React, TypeScript and Tailwind CSS.

## Local development

```bash
cp .env.example .env
docker compose up --build
```

Open `http://localhost:8088`. Backend health is available at `http://localhost:8088/actuator/health`.

The application DB connection uses the non-superuser `crmix_app` role so PostgreSQL Row Level Security remains enforceable.

## Git hooks

After cloning, run `git config core.hooksPath .githooks` once. CI repeats all mandatory checks; hooks are only fast local feedback.

See `docs/sot-and-roadmap.md` for the architecture contract.
