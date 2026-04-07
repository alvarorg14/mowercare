# Security policy

## Supported versions

| Version | Supported |
|---------|-----------|
| `main` branch / latest snapshot (`0.0.1-SNAPSHOT`) | Yes |

We do not maintain separate long-term release branches yet; security fixes land on `main`.

---

## Reporting a vulnerability

**Please do not** file a **public** GitHub issue for undisclosed security vulnerabilities — that can put users at risk.

Preferred options:

1. **GitHub Security Advisories** — use **[Private vulnerability reporting](https://github.com/alvarorg14/mowercare/security/advisories)** if enabled for the repository.
2. If you cannot use GitHub’s private reporting, contact the maintainers through a **private** channel they publish on the repository profile or org page.

Include:

- Description of the issue and impact
- Steps to reproduce (proof-of-concept if safe)
- Affected components (API, mobile, CI)
- Whether you believe it is actively exploitable

Redact secrets, tokens, and customer data from reports.

---

## What to expect

- **Acknowledgement** within a few business days when possible.
- **Triage** — severity and fix plan.
- **Coordinated disclosure** — we’ll work with you on a reasonable timeline before public disclosure.

We cannot offer a paid bug bounty by default; credit in release notes or advisories is given when desired.

---

## Scope (in scope)

- Authentication and session handling (`/api/v1/auth/*`, JWT, refresh tokens)
- Tenant isolation and RBAC bypass attempts
- Injection, deserialization, or path traversal in the API
- Dependency vulnerabilities with a clear upgrade path

## Out of scope (examples)

- Social engineering
- Denial-of-service without a clear, practical fix
- Issues in third-party services outside this repository (report to vendors)
- **Secrets accidentally committed** — rotate credentials immediately; use private disclosure

---

## Related

- [docs/authentication.md](docs/authentication.md) — how auth is designed  
- [CONTRIBUTING.md](CONTRIBUTING.md) — general contribution flow  
