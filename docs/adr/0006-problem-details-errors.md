# ADR 0006: RFC 7807 Problem Details for API errors

## Status

Accepted

## Context

API clients need **consistent**, **machine-readable** errors for branching UI (auth, validation, RBAC, conflicts). Alternatives include ad-hoc JSON `{ "message": "..." }`, custom envelopes, or HTTP status only.

## Decision

Return **RFC 7807** **Problem Details** (`Content-Type: application/problem+json`) with:

- `type` — stable URI, typically `urn:mowercare:problem:<CODE>`
- `title`, `status`, `detail`, `instance` as appropriate
- **`code`** — stable machine code for clients (e.g. `TENANT_ACCESS_DENIED`)

Central mapping lives in `ApiExceptionHandler` and related classes in `apps/api`.

## Consequences

**Positive**

- Mobile `lib/http.ts` can parse a **uniform** error shape.
- OpenAPI and docs can reference consistent error models.

**Negative / trade-offs**

- Slightly more verbose than a single `message` field.
- All new failure modes should get a **stable `code`** — avoid one-off strings in controllers.

## Related

- [../api-reference.md](../api-reference.md)  
