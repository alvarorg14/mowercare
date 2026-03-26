---
title: "Product Brief: mowercare"
status: "complete"
created: "2026-03-26T00:00:00Z"
updated: "2026-03-26T12:00:00Z"
inputs: ["user conversation (Stages 1–3)", "web research (Stage 2 contextual discovery)", "user scope/pricing decisions (finalize)"]
---

# Product Brief: MowerCare

## Executive Summary

**MowerCare** is a focused B2B product for **authorized dealers and installers** who **install and maintain robotic lawn mowers** (for example Husqvarna Automower and similar brands). It replaces scattered, manual issue tracking with a **single place to record mower problems**, **notify the right people in real time**, and **enforce who can see and do what** through **role-based access**.

The **first version is deliberately small**: **employees only** log and manage issues—**clients do not use the app in any way in v1**. There is no pretense of being a full “operations operating system.” Later, the same foundation can grow toward **client-facing workflows** (for example client-reported issues with descriptions, photos, and status), richer **operations dashboards** (including web for admins), and eventually a broader **service management** footprint: scheduling, maintenance history, and analytics.

**Commercial model (initial):** MowerCare is **free** for the **initial version**—details of any future pricing belong in the PRD.

**Why now:** Robotic mowing is a growing service category, but many **authorized dealers and installers** still run on **paper and word of mouth**. That breaks down as soon as more than a few people are involved: **not everyone knows what is wrong, where, or who is fixing it**. MowerCare’s v1 bet is that **shared visibility plus timely alerts** is the smallest slice of software that meaningfully fixes coordination—before layering billing, fleet telemetry, or all-in-one platforms.

---

## The Problem

**Who feels it:** Service teams at **authorized dealers and installers** that support robotic mowers.

**What goes wrong today:** Work is often tracked **manually—paper notes, ad hoc calls, or messages**. Issues are **not visible to the whole team**, so technicians and office staff **duplicate effort**, **miss handoffs**, or **respond late**. The cost is not only slower fixes but **lower trust** with end customers and **more chaos** on busy days.

**Status quo alternatives:** OEM tools (for example fleet or connectivity apps) help with **machine status** but do not replace a **company-owned workflow** for “our team’s open issues, ownership, and history” across brands and internal roles. Generic field-service software can work but is often **heavy or generic** for a team that first needs **simple, mower-centric issue coordination**.

---

## The Solution

**MowerCare v1** gives the team a **shared issue system** for **internal use only**:

- **Structured issue records** for mower-related problems (entered **only by employees**).
- **Real-time notifications** so the right people learn quickly when something new or urgent appears.
- **Roles** for **employees** only in v1—for example **admin** and **technician** (exact matrix to be defined in the PRD). **No client accounts, portals, or shared links** in v1.

The experience is **mobile-first**, matching how people work on site; a **web dashboard** can follow for administrators or heavier desktop use.

---

## What Makes This Different

| Lens | MowerCare’s position |
|------|----------------------|
| **vs. all-in-one “robotic ops” platforms** | **Narrow v1:** issues + notifications + roles—not scheduling, billing, or fleet OS from day one. Easier to adopt and to explain. |
| **vs. OEM connectivity apps** | **Workflow and ownership** belong to the **service company**, not only the manufacturer’s view of the machine. |
| **vs. generic FSM** | **Purpose-framed for mower service** without forcing a full enterprise rollout for a coordination problem. |

The moat in v1 is not a unique algorithm—it is **clarity of scope**, **speed to value**, and **execution** on a painful coordination gap.

---

## Who This Serves

**Primary users (v1):**

- **Field technicians** — log issues on the go, see what is open, know what changed.
- **Office / dispatch / leads** — see the queue, prioritize, and ensure nothing falls through.

**Future (post–v1):**

- **End customers / homeowners** — potential **client-facing** reporting and status (out of scope until explicitly scheduled; not part of the initial release).

**Buyer vs. user:** Often the **owner or operations lead** at an **authorized dealer or installer** chooses the tool; **daily value** accrues to staff on the ground. **Pilot customers, ICP sizing, and go-to-market detail** stay **out of this brief** and belong in the **PRD**.

---

## Success Criteria

Success in the first phase should be **operational**, not feature-count:

| Signal | What “good” looks like |
|--------|-------------------------|
| **Visibility** | A **large majority** of active mower issues exist **only** in MowerCare—not solely on paper or in side channels. |
| **Timeliness** | **Meaningful reduction** in time from issue creation to **first acknowledgment** by the responsible role (concrete baselines and targets to be set in the **PRD**). |
| **Coordination** | Fewer **duplicate visits** or **missed handoffs** versus today (qualitative at first; measurable as processes stabilize). |
| **Adoption** | Core team uses the app **daily** during service season. |

---

## Scope

**In scope for v1**

- **Issue management** for mower-related problems (created **only by employees** of the **authorized dealer / installer**).
- **Notifications** (real-time or near–real time) to relevant **employee** roles.
- **Role-based access** separating at least **admin** and **technician** (exact matrix to be defined in PRD).
- **Mobile-first** UI.
- **Free** distribution for this **initial version** (see PRD for how long and what “initial” means).

**Explicitly out of scope for v1**

- **Any client interaction with MowerCare** — no client logins, no client app, no client portal, no client-submitted issues or media. **Employees only.**
- **Web dashboard** for admins—**later** (unless reprioritized).
- **Scheduling**, **maintenance history**, **analytics**, **billing**—**future** roadmap toward full service management.

---

## Vision

If MowerCare succeeds, it becomes the **system of record for mower service operations**: from **internal coordination** to **client transparency** (when you intentionally add it), then **scheduling and history**, and finally **analytics** that help the business run fewer surprises and more profitable routes.

That path stays credible only if **v1 earns trust** on the basics: **one shared truth** for what is broken, who knows, and what happens next.

---

## Risks and Open Questions

- **Adoption:** Moving from paper to an app requires **habit change**; v1 must be **faster than paper** for the common case, not just “digital filing.”
- **Differentiation over time:** Adjacent products may bundle more features; MowerCare must **stay disciplined** on wedge or intentionally expand.
- **Free initial version:** Define in the **PRD** what “initial” covers (duration, limits, transition) so expectations stay clear for **authorized dealers and installers**.
- **Open (for PRD):** Minimum **role matrix**, **notification channels** (push vs email vs SMS), **issue fields**, **integrations** (if any), **ICP and pilots**, and **future pricing** (if any).

---

*End of product brief.*
