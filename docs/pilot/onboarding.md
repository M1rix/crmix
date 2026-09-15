# CRMIX pilot onboarding

This runbook is the acceptance checklist for onboarding the first 1–2 real businesses. Do not mark a business as onboarded until every required item is verified with the owner or administrator.

## Before the call

- production SHA has green backend/frontend CI
- HTTPS, backup upload and monitoring are configured
- Telegram webhook and support chat are configured
- Payme production credentials are configured only if the business will use paid CRMIX subscription during the pilot
- create a support contact and agree on the first-week communication channel

## 45-minute onboarding

### 1. Workspace and owner

- register the tenant with the real business name/type
- verify owner login and refresh-token flow
- confirm business timezone
- explain that each business is isolated from other CRMIX tenants

### 2. Directory

Together with the owner:

- add active employees
- add real services, duration and current price
- import/create the minimum client set needed for the first live day
- verify the plan employee limit is understood

### 3. Scheduling

- create one real test appointment
- attempt an overlapping appointment and verify CRMIX rejects it
- confirm/cancel/complete a test appointment
- verify employee working-hours behavior

### 4. Telegram

- link one pilot client through the `/start <token>` flow
- confirm that the linked client appears in CRMIX
- verify reminder configuration for 24h and 2h
- send `/support Test pilot support request` and confirm it arrives in the configured support chat

### 5. Payments and analytics

- complete a test appointment
- record the real payment method and amount
- verify the payment appears in the dashboard revenue
- confirm cancellation/no-show and employee-load metrics are understandable to the owner

### 6. Billing

- show current CRMIX plan/status
- if applicable, create a Payme checkout and complete it in provider test/production mode appropriate to the pilot
- verify suspended tenants can still access billing recovery paths

## First-week support cadence

Day 1: check in after the first real operating session. Treat booking/payment/data-loss issues as P0/P1.

Day 2–3: review failed reminders, appointment conflicts, user confusion and slow workflows. Do not implement feature requests immediately; log them with evidence.

Day 5–7: run the pilot review using `feedback-backlog.md`, rank v2 requests, verify backups/alerts once more and decide whether the tenant is ready for normal support cadence.

## Pilot exit criteria

A pilot business is considered successfully onboarded only when:

- owner can manage clients/employees/services without DB/manual intervention
- staff can create and finish appointments
- overlap protection was demonstrated
- at least one appointment payment appears in analytics
- Telegram link/reminder/support flow was demonstrated when Telegram is enabled
- backup and monitoring are healthy during the pilot window
- all P0/P1 defects are closed or have an explicit rollback/workaround agreed with the owner

Actual business names, contacts and feedback must not be committed to this public repository.
