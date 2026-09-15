# Pilot feedback → v2 backlog

Use one row per observed problem/request. Prefer evidence from a real workflow over feature brainstorming.

| ID | Tenant alias | Workflow | Observation | Frequency | Impact | Severity | Evidence | Decision |
|---|---|---|---|---|---|---|---|---|
| P-001 | pilot-a | scheduling | _fill during pilot_ | _daily/weekly/once_ | _time/revenue/error_ | P0–P3 | _steps/screenshot/trace id_ | triage |

## Severity

- **P0** — data isolation/security incident, data loss, payments materially wrong, or service unusable for all pilot users
- **P1** — critical business workflow blocked with no acceptable workaround
- **P2** — workflow works but is confusing/slow/error-prone
- **P3** — enhancement or convenience request

## Prioritization score

For non-P0 items, score each 1–5:

- frequency
- business impact
- number of affected roles/businesses
- confidence in evidence

Subtract 1–5 for implementation/risk cost. Highest evidence-backed scores enter v2 planning first. A loud single request is not automatically high priority.

## v2 candidates explicitly deferred from MVP

Validate these with pilots before implementation:

- calendar drag-and-drop/rescheduling
- full UI localization
- report/data export
- richer reminder/template controls

Do not promote a deferred item only because it already exists in this list; pilot evidence must justify it.
