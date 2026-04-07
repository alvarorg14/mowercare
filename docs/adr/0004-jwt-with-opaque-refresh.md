# ADR 0004: JWT access tokens with opaque refresh tokens

## Status

Accepted

## Context

Mobile clients need **short-lived access** credentials and a way to **renew sessions** without re-entering passwords. Options included **session cookies**, **JWT-only** (long-lived), **OAuth2 authorization server**, and **JWT access + opaque refresh** stored server-side.

## Decision

- **Access token:** signed **JWT** (HS256) with claims including `sub`, `organizationId`, and `role`.
- **Refresh token:** **opaque** random token stored **hashed** in `refresh_tokens`, with **rotation** and **revocation** on logout.

## Consequences

**Positive**

- Stateless validation for most requests (JWT).
- Refresh tokens can be **revoked** and **rotated** — important for deactivated users and logout.
- Familiar pattern for mobile apps.

**Negative / trade-offs**

- JWTs cannot be trivially revoked before expiry — keep **access TTL short** (default PT15M).
- Secret management for `MOWERCARE_JWT_SECRET` is critical.

## Related

- [../authentication.md](../authentication.md)  
