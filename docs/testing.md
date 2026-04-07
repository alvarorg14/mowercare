# Testing overview

Start here for **what to run locally**, **what runs on every PR**, and **prerequisites**. Detailed guides are split by area.

## Prerequisites

| Need | Why |
|------|-----|
| **JDK 25** | Build and test the API (`apps/api`). |
| **Maven 3.9+** on `PATH` | This repo does **not** ship `./mvnw`; use `mvn` like CI. |
| **Node.js 20** (LTS) | Aligns with [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) for mobile. |
| **Docker** | Required for `mvn verify` / `mvn test` in `apps/api` — **Testcontainers** starts PostgreSQL (same as CI). |
| **PostgreSQL** | For running the API locally (not for the default unit/integration test command if you only run `mvn verify` with Testcontainers). |
| **Maestro CLI** | Only for [E2E UI flows](testing-e2e.md) (local; not in default PR CI). |

## What runs where

| Layer | Local commands | PR CI ([`ci.yml`](../.github/workflows/ci.yml)) |
|-------|----------------|---------------------------------------------------|
| **API — unit + integration** | `cd apps/api && mvn -B verify` | Job **API — verify**: `mvn -B verify` (JDK 25, Docker). |
| **Mobile — lint, typecheck, unit tests** | `cd apps/mobile` → `npm run lint`, `npm run typecheck`, `npm test` | Job **Mobile — …**: same, plus Jest prints an **informational** coverage summary (no minimum gate). |
| **Mobile — contrast** | `npm run check:contrast` | Same script in CI (**blocking** if it fails). |
| **E2E (Maestro)** | See [testing-e2e.md](testing-e2e.md) | **Not** on the default PR path (emulator/device cost and flakiness). |

## Deep dives

- [testing-backend.md](testing-backend.md) — API tests, Testcontainers, BDD naming, integration inventory.
- [testing-mobile.md](testing-mobile.md) — Jest layout, commands, coverage map of suites.
- [testing-e2e.md](testing-e2e.md) — Maestro flows, seed script, env vars, CI trade-offs.

## Coverage policy (Epic 5)

- **No enforced coverage minimums** in CI for either the API or mobile: merges are **not** blocked by JaCoCo or Jest `coverageThreshold` (none are configured today).
- **Mobile:** PR CI runs Jest with `--coverage --coverageReporters=text-summary` so logs show a **summary only**; treat it as **signal**, not a gate.
- **API:** No JaCoCo report in CI by default; add **informational** reports (e.g. JaCoCo HTML or `jacoco:report` without `check`) in a future story if needed.
- **Incremental approach:** When the team adopts thresholds, add them **gradually** (per-package or per-module) and keep **informational** jobs until numbers stabilize — document any change in this file and in [`README.md`](../README.md).
