# Contributing to MowerCare

Thank you for your interest in MowerCare — service management for robotic lawn mower installations (issues, notifications, RBAC, multi-tenant API + Expo mobile app).

This document explains how we collaborate. For day-to-day setup, see **[docs/developer-guide.md](docs/developer-guide.md)**.

---

## Code of Conduct

Please read **[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)**. We aim for a respectful, constructive environment.

---

## Reporting bugs

- Open a **[GitHub Issue](https://github.com/alvarorg14/mowercare/issues)** with:
  - What you expected vs what happened
  - Steps to reproduce
  - Environment (OS, JDK/Node versions, API vs mobile, commit SHA if possible)
  - Redact **secrets**, tokens, emails, and customer data

---

## Suggesting features

- Use GitHub Issues or Discussions (if enabled) with the problem you’re solving and any constraints.
- Link to product docs (e.g. `_bmad-output/` PRD) only as **context** — implementation still follows maintainers’ triage.

---

## Development workflow

1. **Fork** and **branch** from `main`.
2. Keep changes **focused** — one concern per PR when possible.
3. **Run tests** before pushing:
   - API: `cd apps/api && mvn -B verify`
   - Mobile: `cd apps/mobile && npm run lint && npm run typecheck && npm test && npm run check:contrast`
4. Open a **Pull Request** with a clear description and links to related issues.

CI runs on every PR ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)); merges should be green.

---

## Branch naming

| Prefix | Use for |
|--------|---------|
| `feat/` | New features |
| `fix/` | Bug fixes |
| `docs/` | Documentation only |
| `refactor/` | Behavior-preserving code changes |
| `test/` | Test-only changes |

Examples: `feat/api-issue-filters`, `docs/architecture-update`.

---

## Commit messages

Match the existing style:

```text
type(scope): short description
```

Examples from the repo:

- `feat(api): Story 3.1 issue persistence and change history`
- `fix(mobile): push deep link to issue detail`
- `docs(testing): clarify Maestro prerequisites`

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, etc.

---

## Code conventions

- **API:** `controller` → `service` → `repository`; no DB access from controllers. **Liquibase** for schema — no Hibernate DDL in production. Errors use **Problem Details** — see [docs/api-reference.md](docs/api-reference.md).
- **JSON:** **camelCase**; **DB:** **snake_case** — see [docs/architecture.md](docs/architecture.md).
- **Mobile:** Prefer **TanStack Query** + shared `lib/*-api.ts` over ad-hoc `fetch` in screens.
- **Tenant safety:** Any new tenant-owned path must enforce **organization** scope and include **tests** that deny cross-tenant access where applicable.

---

## Testing

Start here: **[docs/testing.md](docs/testing.md)** (backend, mobile, E2E split).

---

## Security

Do **not** open public issues for **undisclosed** vulnerabilities. See **[SECURITY.md](SECURITY.md)**.

---

## License

By contributing, you agree that your contributions are licensed under the **Apache License 2.0** — see [LICENSE](LICENSE).
