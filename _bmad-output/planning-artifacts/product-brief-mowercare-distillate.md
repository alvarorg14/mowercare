---
title: "Product Brief Distillate: mowercare"
type: llm-distillate
source: "product-brief-mowercare.md"
created: "2026-03-26T12:00:00Z"
purpose: "Token-efficient context for downstream PRD creation"
---

# Product Brief Distillate: MowerCare

Dense context captured during product-brief discovery. Use with `product-brief-mowercare.md` for PRD work.

## Product intent

- B2B platform for **authorized dealers and installers** of **robotic lawn mowers** (e.g. Husqvarna Automower-class devices).
- **v1 wedge:** internal **issue tracking** + **notifications** + **employee roles** — **not** a full FSM, fleet OS, or client product.

## Rejected / deferred (do not re-propose without explicit change)

- **v1 ≠ full “robotic ops” platform** — no scheduling, billing, fleet OS, analytics as part of first release.
- **v1 ≠ client-facing in any form** — user confirmed **no client interaction** with the app in v1 (no logins, portal, or submissions).
- **Head-to-head with all-in-one competitors** (e.g. TurfPilot-style breadth) — **not** the v1 positioning; narrow coordination first.

## Requirements hints (for PRD)

- **Actors v1:** employees only — at minimum **admin** and **technician**; **no client role** in product for v1.
- **Issue domain:** mower-related problems; **staff-entered** in v1.
- **Notifications:** real-time or near–real time to relevant roles (channels TBD: push / email / SMS).
- **UX:** **mobile-first**; **web dashboard** explicitly later (admins / desktop).
- **Commercial:** **free** for **initial version** — define boundaries in PRD (timebox, limits, future monetization).

## Current-state workflow (user-stated)

- Largely **manual**: **paper**, inconsistent knowledge across staff.
- **Coordination gap:** not all employees aware of all issues; **poor handoffs** and visibility.

## Competitive / landscape (from research)

- **OEM:** Husqvarna Fleet Services, Automower Connect — machine/fleet angle; not a dealer’s **company-owned issue workflow** across brands.
- **Purpose-built adjacent:** e.g. TurfPilot-style **broad ops** (scheduling, billing, portal) — overlaps **long-term vision**, not **v1 scope**.
- **Generic FSM / lawn software** — can work but often heavy; v1 is **narrow coordination**.

## Technical context (hints only)

- Scale path: scheduling, maintenance history, analytics — **post-v1**.
- Future: **client-reported** issues (text, photos, status) — **explicitly post-v1**.

## Open questions (for PRD)

- **Role matrix** (permissions, assignment rules).
- **Notification design** (channels, quiet hours, escalation).
- **Issue model** (fields, states, priority, SLA if any).
- **What “initial” free period means** and **success metrics** with baselines.
- **ICP, pilots, GTM** — intentionally **not** locked in product brief; set in PRD.
- **Integrations** (OEM APIs, calendars, etc.) — unset for v1.

## Scope signals

| Area | Signal |
|------|--------|
| v1 in | Issues, notifications, employee RBAC, mobile-first, free initial |
| v1 out | Any client app use, web admin dashboard (unless reprioritized), scheduling/history/analytics/billing |
