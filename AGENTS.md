# AGENTS.md

This repository uses an Engineering Harness workflow for a nursing-home ERP system. Any AI agent or engineer making changes in this repository must follow this protocol before writing code.

## Default Language

Use Chinese for user-facing explanations unless the user asks otherwise. Code, identifiers, API paths, commit messages, and technical contracts may use English.

## Required Workflow

Before implementing a feature, bug fix, refactor, or architecture change:

1. Identify the affected business domain and module.
2. Read the relevant files under `docs/`.
3. If no module design exists, create one from `docs/04-modules/module-template.md`.
4. If the change adds or modifies an API, create or update an API contract from `docs/03-apis/api-contract-template.md`.
5. If the change adds or modifies async behavior, create or update an event contract from `docs/03-apis/event-contract-template.md`.
6. If the change makes a major architectural or infrastructure decision, create an ADR from `docs/09-decisions/ADR-template.md`.
7. Implement the smallest safe code change that satisfies the contract.
8. Add or update tests according to `docs/05-quality/testing-harness.md`.
9. Check security, tenant isolation, audit logging, observability, and release impact.
10. Summarize what changed, what was verified, and any remaining risk.

## Non-Negotiable ERP Rules

- Every business record must have clear ownership by a domain/module.
- Cross-module writes must go through the owning module's service or contract.
- Multi-tenant data must be scoped by `tenant_id` and, where applicable, `facility_id`.
- Sensitive data must not be logged in plaintext.
- Resident profile, medical, medication, billing, identity, and family contact data require strict permission checks and audit trails.
- Any high-concurrency path must consider idempotency, rate limiting, caching, pagination, and degradation.
- Any state machine change must include invalid transition handling and tests.
- Any financial change must include idempotency, reconciliation, and precision handling.

## When To Create Harness Docs

Create or update Harness docs when:

- A new module is introduced.
- A module's responsibilities or data ownership changes.
- A new API, event, permission, state machine, background job, or data migration is added.
- A change affects P0/P1 workflows, performance, security, privacy, audit, or release risk.
- The implementation requires a long-term technical decision.

Small copy changes, isolated UI styling, or internal test-only changes may skip new Harness docs, but the final response should say why they were not needed.

## PR / Final Response Checklist

Every substantial change should report:

- Affected module/domain
- Harness docs created or updated
- Code changes
- Tests or checks run
- Security, audit, tenant isolation impact
- Performance/concurrency impact
- Migration/release/rollback notes
- Known limitations or follow-ups

## Preferred File Naming

Use lowercase kebab-case for Harness documents:

```text
docs/04-modules/resident-profile.md
docs/03-apis/resident-profile-create.md
docs/03-apis/care-task-completed-event.md
docs/09-decisions/ADR-0001-monorepo-architecture.md
```

