---
stepsCompleted:
  - step-01-init
  - step-02-discovery
  - step-02b-vision
  - step-02c-executive-summary
  - step-03-success
  - step-04-journeys
  - step-05-domain
  - step-06-innovation
  - step-07-project-type
  - step-08-scoping
  - step-09-functional
  - step-10-nonfunctional
  - step-11-polish
  - step-12-complete
classification:
  projectType: saas_b2b
  domain: general
  complexity: low
  projectContext: greenfield
inputDocuments:
  - "_bmad-output/planning-artifacts/product-brief-mowercare.md"
  - "_bmad-output/planning-artifacts/product-brief-mowercare-distillate.md"
documentCounts:
  briefs: 2
  research: 0
  brainstorming: 0
  projectDocs: 0
workflowType: prd
prdStatus: complete
completed: "2026-03-26T18:30:00Z"
---

# Product Requirements Document - mowercare

**Author:** Alvaro
**Date:** 2026-03-26

## Executive Summary

**MowerCare** is a **B2B SaaS** product for **authorized dealers and installers** who **install and maintain robotic lawn mowers** (e.g. Husqvarna Automower-class equipment). Teams today rely heavily on **manual methods** (paper, ad hoc messages), so **not everyone sees the same issues**, **handoffs fail**, and **response is slow**.

**MowerCare v1** gives **employees only** a **shared system** to **create, view, assign, and progress mower-related issues**, with **near–real-time notifications** and **role-based access** (at minimum **admin** and **technician**). The product is **mobile-first** to match field work. **End customers do not use MowerCare in v1** — no client accounts, portals, or submissions. The **initial version is free**; boundaries of “initial” and any future pricing are defined later in this PRD.

**Success looks like** a team that runs active mower issues **primarily in the app** (not parallel paper trails), **responds faster** to new work, and **coordinates** with fewer duplicates and missed steps than today.

### What Makes This Special

- **Scope discipline:** v1 is **issues + notifications + employee roles** — not scheduling, billing, fleet OS, analytics, or client-facing flows. That keeps adoption and explanation simple versus **heavy FSM** or **all-in-one “robotic ops”** platforms.
- **Company-owned workflow:** Unlike **OEM connectivity** tools that center the manufacturer’s view of machines, MowerCare centers **the service organization’s** open work, ownership, and history across brands and roles.
- **Core insight:** The gap is **fragmented awareness**, not “lack of software” in the abstract. **Shared visibility** plus **timely notifications** and **clear roles** address coordination **before** expanding into a full service-management suite.

## Project Classification

| Dimension | Value |
|-----------|--------|
| **Project type** | **saas_b2b** — B2B SaaS with teams, RBAC, and multi-company potential; **mobile-first** delivery in v1. |
| **Domain** | **general** — field-service coordination for SMB-style operations; **no specialized regulated vertical** assumed at v1. |
| **Complexity** | **low** (domain classification) — still requires solid **security**, **privacy**, and **UX** discipline. |
| **Project context** | **greenfield** — new product; **no** existing product documentation in-repo at PRD start. |

## Success Criteria

### User Success

- **Single source of truth:** For day-to-day mower service work, **active issues** are **recorded and updated in MowerCare** so the team is not relying on **parallel paper or side channels** for status.
- **Faster awareness:** When a **new or materially updated** issue appears, **relevant employees** are **notified in near–real time** so response is **faster than today’s manual habits** (exact baseline and target window to be set with early adopters).
- **Clear ownership:** Issues have **understood responsibility** (who is expected to act next), reducing **duplicate visits** and **missed handoffs** versus ad hoc coordination.
- **Daily habit:** **Core field and office staff** use the app **during active service periods** so the system stays current.

### Business Success

- **Operational proof:** The organization can show **fewer coordination failures** (missed handoffs, duplicate effort) attributable to **invisible or untracked** work — measured **qualitatively first**, then with simple counts once stable.
- **Adoption depth:** **Authorized dealer / installer** teams **sustain use** beyond a one-week trial (specific retention definition in rollout plan).
- **Commercial clarity:** **Free “initial”** period is **defined** (duration and limits) so customers and the product team share **expectations**; any path to **paid** features is **explicit** when introduced (not required for v1 launch narrative).

### Technical Success

- **Reliability:** Core flows (**auth, issue CRUD, notifications**) are **dependable** for daily operations; outages are **rare and recoverable**.
- **Security & privacy:** **Employee accounts** and **issue data** are protected with **sound authentication**, **authorization by role**, and **safe handling** of customer/site information entered by staff.
- **Notification delivery:** Notifications reach the **intended recipients** under normal conditions (policy for **delivery channel**, **failures**, and **preferences** documented).

### Measurable Outcomes

| Outcome | How we’ll know (v1) |
|--------|----------------------|
| **Visibility** | **Most** active mower issues exist **only** in-app for teams that commit to the pilot (no hard % until baseline). |
| **Timeliness** | **Shorter** time from **issue creation** to **first acknowledgment** than pre-MowerCare baseline (**baseline TBD**). |
| **Coordination** | **Fewer** reported duplicate visits / missed handoffs tied to **unknown** open work (**qual → simple tallies**). |
| **Engagement** | **Daily** active use by **core** staff during season (**definition**: e.g. opens + at least one meaningful action/week — **finalize in rollout**). |

## Product Scope

Phased detail (MVP boundaries, post-MVP, risks) is in **## Project Scoping & Phased Development**. Summary:

- **MVP:** Employee-only **issues**, **notifications**, **RBAC**, **mobile-first**, **free initial**; **no** client surfaces, **web admin**, or **third-party integrations** in MVP unless scope changes.
- **Growth:** **Web** admin, **client-reported** issues, **scheduling**, **history**, **analytics**, **integrations** as prioritized.
- **Vision:** Full **service-management** positioning on a **trusted** internal issue core.

## User Journeys

### Field technician — happy path (“New issue on route”)

**Persona — Mateo**, technician for an **authorized dealer**, spends his day between customer sites.

**Opening:** At a customer property he finds an **Automower** fault (e.g. boundary error). He used to scribble a note or text someone; often **the office did not see it the same day**.

**Rising action:** He opens **MowerCare**, **creates an issue** tied to the **site/customer context** (as defined in requirements), adds a **short description** and **severity/priority** if available, **saves**. **Relevant colleagues** get a **notification** near–real time.

**Climax:** Back at the shop, dispatch **already sees** the issue—**no duplicate call** from the customer before the team acts.

**Resolution:** Mateo’s team **coordinates from one list**; he feels **less anxious** about “information dying in my pocket.”

**Capabilities implied:** Mobile **create issue**, **structured fields**, **list/detail**, **notifications**, **role visibility**.

### Field technician — edge case (“Handoff and confusion”)

**Persona — Mateo** again.

**Opening:** He **starts** an issue, then realizes he **assigned the wrong colleague** or the **status** should be “waiting on parts.”

**Rising action:** He **updates** the issue (assignment and/or state), adds a **comment** if supported. **Notifications** reflect the change so nobody works the wrong ticket.

**Edge:** **Spotty connectivity** — the app must behave **predictably** (clear save state, retry or offline messaging — exact behavior in **non-functional / UX** requirements).

**Resolution:** **No silent failures**; the team still trusts the **single source of truth**.

**Capabilities implied:** **Edit/update**, **assignment rules**, **history** of changes (who changed what, when), **robust error handling**, **notifications** on meaningful updates.

### Office / dispatch lead — success path (“Triage the day”)

**Persona — Lucia**, runs the board from the office for the same dealer.

**Opening:** She used to **chase paper** and **group chats** to know what’s open.

**Rising action:** She opens MowerCare, sees a **queue** of **open issues**, **sorts/filters** by priority or age (per MVP scope), **assigns** or **reassigns**, **nudges** field staff via workflow (comments or status). She gets **notifications** for **new** or **escalated** items.

**Climax:** She **prevents two techs** heading to the **same** fault because **ownership is visible**.

**Resolution:** Her job shifts from **detective work** to **prioritization**.

**Capabilities implied:** **Role: admin or dispatcher** (exact naming in RBAC), **queue views**, **filter/sort** (as scoped), **assign/reassign**, **notifications** tuned by role.

### Organization admin — “Turn the system on for our shop”

**Persona — Jordan**, owner or ops manager.

**Opening:** Wants **authorized employees** in the app, **no customers** in v1.

**Rising action:** **Invites users** or creates accounts (per auth model), assigns **roles** (**admin** vs **technician**), **deactivates** leavers. Ensures **only staff** access **customer/site** data entered for issues.

**Resolution:** **Trust** that MowerCare is **internal-only** and **role-appropriate** for v1.

**Capabilities implied:** **User provisioning**, **RBAC**, **tenant/company** model (if multi-entity), **audit** of admin actions as needed.

### API / integration (v1)

**Not in scope for MVP** unless explicitly added later. **OEM or billing integrations** belong in **roadmap / technical requirements**, not v1 journeys.

### Journey Requirements Summary

| Area | Capabilities suggested by journeys |
|------|-------------------------------------|
| **Issues** | Create, read, update, assign, resolve/close; meaningful **states**; **history** of changes. |
| **Notifications** | Near–real time for **create/update** events; **role-aware** routing. |
| **Roles** | At least **technician** and **admin** (dispatch may map to admin or separate role — **finalize in domain/FR**). |
| **Mobile UX** | Fast **create** on site; **trustworthy** save under **variable network**. |
| **Administration** | **User lifecycle**, **invites**, **role assignment**, **no client accounts** in v1. |
| **Security** | **Employee-only** access; **least privilege** by role. |

## Domain-Specific Requirements

MowerCare operates in a **general business / field-service** context: **no dedicated regulated vertical** (e.g. healthcare, payments) is assumed for v1. The following still applies because the product stores **business operational data** and often **customer- or site-related details** entered by **employees**.

### Compliance & regulatory

- **Data protection (geography):** If a **dealer/installer** serves **EU/UK** end customers or processes **personal data** of individuals in those regions, the organization remains the **data controller** for that relationship; MowerCare should support **lawful processing**, **purpose limitation**, **data subject rights** (access/erasure where applicable), and **subprocessor transparency** at a level appropriate for **SMB B2B SaaS** (exact legal packaging is **outside** this PRD; **implementation** must align with counsel).
- **Industry-specific regimes:** **No** claim of compliance with **HIPAA**, **PCI DSS** (unless payment features are added), or similar **unless** explicitly scoped in a future release.
- **Record-keeping:** Dealers may have **warranty**, **service**, or **OEM** obligations; the product should allow **export or retention** aligned with **customer support** needs (specific retention targets in **NFR / admin policy**).

### Technical constraints

- **Security baseline:** **Authentication** for **employees only**, **role-based access**, **encryption in transit** (TLS), and **encryption at rest** for stored issue and account data (details in technical specs).
- **Privacy-by-design:** **Minimize** fields that identify end customers; **restrict** visibility by **role**; support **deletion** or **anonymization** when accounts or sites are retired (workflow TBD).
- **Auditability:** **Administrative** and **security-relevant** actions should be **traceable** enough for **internal accountability** (scope of audit log in FR/NFR).

### Integration requirements (domain-facing)

- **None mandatory for v1** from a **regulatory** standpoint. Optional **OEM or dealer systems** integrations are **roadmap** items unless explicitly added to MVP.

### Risk mitigations

| Risk | Mitigation (product direction) |
|------|--------------------------------|
| **Customer PII** in free-text issue notes | **Guidance** in UX; **role-based** access; future **retention** rules |
| **Cross-border** data | **Region** / **deployment** choices documented for production |
| **Over-promising compliance** | Marketing claims stay **aligned** with actual controls and legal review |

## SaaS B2B Specific Requirements

### Project-Type Overview

MowerCare is a **B2B SaaS** product: **authorized dealers and installers** use it as **employees** of an **organization** (tenant). Value is **shared visibility**, **notifications**, and **role-based access** to **issue** data — not a consumer app and **not** client-facing in v1.

### Technical architecture considerations

- **Delivery:** **Mobile-first** native or cross-platform clients for **iOS/Android** (exact stack in architecture); **web** is **out of MVP** unless scope changes.
- **Backend:** Multi-tenant **API** serving **auth**, **issues**, **notifications**, and **admin** operations; **real-time or near–real-time** notification channel (e.g. push + server-driven events) as per NFR.
- **Data isolation:** **Tenant-scoped** data — one dealer’s issues **must not** be visible to another tenant’s users.

### Tenant model

- **Organization = tenant:** Each **authorized dealer / installer** is an **account/organization** with its **own** users and **issue** data.
- **User membership:** Users belong to **exactly one** organization for v1 unless product explicitly adds multi-org later (default: **single org per user**).
- **Provisioning:** **Org admin** (or equivalent) **invites** or creates **employee** accounts; **no** self-serve signup for end customers in v1.

### RBAC matrix (product requirements level)

| Role (v1) | Intended use | Core permissions (directional) |
|-----------|----------------|--------------------------------|
| **Admin** | Owner, ops lead, office lead | **Manage users** (invite, deactivate, assign roles), **view/manage all org issues**, **configure** org-level settings as exposed by MVP. |
| **Technician** | Field staff | **Create and update** issues assigned to them or visible per policy, **view** relevant org issues per rules, **no** full user administration unless explicitly granted dual role. |

**Dispatch** may map to **admin** or a **future** role; **exact** permission matrix (e.g. “view all” vs “assigned only”) is **finalized** in functional requirements. **Clients** have **no** role in v1.

### Subscription tiers

- **Initial commercial model:** **Free** for the **initial** period (duration, limits, and transition — **business rules** + **NFR**).
- **Future tiers:** **Out of MVP**; avoid building **billing** until roadmap says so.

### Integration list

- **MVP:** **No required** external integrations for **core** value (issue + notify + roles).
- **Roadmap:** Optional **OEM**, **calendar**, **billing**, or **identity** integrations — **post-MVP** unless explicitly pulled in.

### Compliance requirements (SaaS B2B)

See **## Domain-Specific Requirements** and **## Non-Functional Requirements** (security, privacy, tenant isolation). **No** **HIPAA/PCI** claims unless features require them later.

### Implementation considerations

- **Onboarding:** Short path for **first admin** + **inviting** technicians; **seasonal** use implies **low friction** return after idle periods.
- **Operations:** **Monitoring** of **notification** delivery and **core** API health; **support** model for **dealers** (even if lightweight) should be defined for launch.
- **Mobile:** OS versions and **device** support targets belong in **NFR**; **push notification** reliability is **critical** for the value proposition.

## Project Scoping & Phased Development

### MVP strategy & philosophy

**MVP approach:** **Problem-solving MVP** — ship the **smallest** product that replaces **paper/siloed** awareness with a **shared issue list**, **timely notifications**, and **clear employee roles**. **Experience** and **platform** depth (web, analytics, integrations) come **after** this works in the field.

**Resource requirements (directional):** Small cross-functional team — e.g. **mobile** + **backend** + **product/design** + **QA**; exact size is an execution decision, but MVP does **not** require a large platform org.

### MVP feature set (Phase 1)

**Core user journeys supported:**

- **Field technician:** Create/update issues on site; receive and act on awareness of work.
- **Office / dispatch (admin-capable):** See queue, assign/reassign, prioritize.
- **Org admin:** Provision **employee** users and **roles**; **no** client users.

**Must-have capabilities:**

- **Multi-tenant org** with **tenant-isolated** data.
- **Authentication** for **employees** only.
- **Issues:** create, list, detail, update (including assignment and status), resolve/close; **history** of material changes.
- **Notifications** (near–real time) for **meaningful** create/update events to **appropriate roles**.
- **RBAC** at least **admin** vs **technician** (exact matrix in functional requirements).
- **Mobile-first** clients for **iOS/Android**; **reliable** save and **notification** behavior under real-world conditions.
- **Free** initial offering per business rules (no **billing** dependency in MVP).

**Explicitly out of MVP:** **Client-facing** app or portal, **web admin dashboard** (unless scope changes), **third-party integrations**, **scheduling**, **analytics**, **billing**.

### Post-MVP features

**Phase 2 (growth):**

- **Web** dashboard for **admins** / power users.
- **Client-reported** issues and **richer** status (with **privacy** model).
- **Deeper** workflow: **scheduling**, **maintenance history**, **reporting** as prioritized.
- **Integrations** (OEM, calendars, identity) as needed.

**Phase 3 (expansion):**

- **Full service-management** positioning: **analytics**, **billing** links, **differentiation** vs OEM-only or generic FSM — on top of a **trusted** internal core.

### Risk mitigation strategy

| Category | Risk | Mitigation |
|----------|------|------------|
| **Technical** | **Notification** or **offline** behavior erodes trust | Clear **NFRs** for delivery and **UX** for failure/retry; **dogfood** with a pilot dealer |
| **Technical** | **Multi-tenant** data leaks | **Tenant-scoped** APIs and **tests**; security review |
| **Market** | Teams **revert to paper** | **Onboarding** + **fast** “first issue logged” moment; **free** reduces friction |
| **Market** | **Adjacent** all-in-one products | Stay **disciplined** on wedge; **position** as coordination-first |
| **Resource** | Smaller team than hoped | Cut **scope** to **issues + notify + roles** only; **no** integrations |

## Functional Requirements

### Organization & tenancy

- **FR1:** The system **can** **isolate** all operational data by **organization** such that one organization’s data is **inaccessible** to users of another organization.
- **FR2:** An **Org Admin** **can** **create** or **update** **organization profile** fields supported in MVP (e.g. organization name).
- **FR3:** A **user** **can** belong to **exactly one** organization in v1.

### Authentication & identity

- **FR4:** An **employee** **can** **sign in** to access the product using the **authentication** mechanism chosen for MVP.
- **FR5:** An **employee** **can** **sign out** of the product.
- **FR6:** The system **can** **deny access** to **unauthenticated** callers for protected capabilities.

### Roles & authorization

- **FR7:** An **Org Admin** **can** **assign** a **role** to each **employee** user from the **roles** supported in MVP (at minimum **Admin** and **Technician**).
- **FR8:** The system **can** **enforce** **role-based** permissions for **issue** and **administration** actions (exact matrix defined in acceptance criteria).
- **FR9:** A **Technician** **can** perform **issue** actions permitted for **Technicians** (e.g. create/update within policy).
- **FR10:** An **Admin** **can** perform **user-management** and **organization** actions permitted for **Admins**.

### Issue management

- **FR11:** A **Technician** or **Admin** **can** **create** an **issue** with the **required** MVP fields.
- **FR12:** An **issue** **can** **reference** **customer** or **site** context using **fields** defined for MVP (without granting access to end customers).
- **FR13:** A **Technician** or **Admin** **can** **view** a **list** of issues available to them within their organization.
- **FR14:** A **Technician** or **Admin** **can** **open** an **issue detail** view for an issue they are permitted to see.
- **FR15:** A **Technician** or **Admin** **can** **update** an issue’s **attributes** permitted by MVP (including **status** and **assignment** where allowed).
- **FR16:** A **Technician** or **Admin** **can** **assign** or **reassign** an issue to an **employee** (or self) per organization rules.
- **FR17:** A **Technician** or **Admin** **can** **resolve** or **close** an issue according to the **state model** for MVP.
- **FR18:** The system **can** **retain** a **history** of **material changes** to an issue (e.g. field changes, assignment, status) with **actor** and **timestamp**.

### Discovery & triage

- **FR19:** A **Technician** or **Admin** **can** **filter** and/or **sort** the issue list using **criteria** supported in MVP (e.g. status, priority, recency).

### Notifications

- **FR20:** The system **can** **generate notifications** for **meaningful issue events** (e.g. new issue, assignment change, status change) as defined for MVP.
- **FR21:** The system **can** **deliver notifications** to **eligible employees** according to **role** and **subscription** rules for MVP.
- **FR22:** An **employee** **can** **view** a **notification** or **activity** surface in-app appropriate to MVP (e.g. inbox or feed).
- **FR23:** The system **can** **send push notifications** to supported **mobile devices** where enabled and permitted.

### User administration

- **FR24:** An **Org Admin** **can** **invite** or **create** **employee** user accounts for their organization.
- **FR25:** An **Org Admin** **can** **deactivate** or **remove** **employee** access for their organization.
- **FR26:** The system **can** **prevent** **deactivated** users from **signing in** or **using** protected capabilities.

### Access model (v1)

- **FR27:** The system **can** **restrict interactive use** to **employees** of an organization; **end customers** **do not** receive **accounts** or **client** access in v1.

### Commercial & integrations (MVP boundaries)

- **FR28:** The system **can** support **core** flows **without** **payment** or **subscription billing** features in MVP (per **free initial** policy).
- **FR29:** The system **can** support **core** flows **without** **third-party product integrations** in MVP.

## Non-Functional Requirements

### Performance

- **NFR-P1:** **Interactive** actions on **mobile** (open app, load issue list, open issue detail, save an update) complete within a **target** responsive window under **typical** field conditions; exact **p95/p99** targets are set before release and validated on **reference devices**.
- **NFR-P2:** **Background** operations (e.g. **notification** registration, **sync** after reconnect) **do not** block the UI from showing **clear** loading or **saved/error** states.

### Reliability & availability

- **NFR-R1:** **Core** services (**auth**, **issue read/write**, **notification dispatch**) meet a **defined uptime** target for production (e.g. **monthly** availability); exact **SLA** is chosen for launch tier.
- **NFR-R2:** **Push notification** delivery achieves a **high** success rate under normal operating conditions; **failure modes** (token invalid, disabled permission) are **detectable** and **communicated** in UX.
- **NFR-R3:** **Data loss** of **committed** issue updates is **unacceptable**; persistence and **durability** assumptions are documented in architecture.

### Security

- **NFR-S1:** All **client–server** traffic uses **encrypted transport** (e.g. **TLS**).
- **NFR-S2:** **Sensitive data at rest** (credentials, tokens, issue content) is protected using **industry-standard** encryption and **key handling** appropriate to the deployment model.
- **NFR-S3:** **Tenant isolation** is **enforced** at the **API** and **data** layers; **cross-tenant** access is **prevented** by **tests** and **review**.
- **NFR-S4:** **Authentication** and **session** handling resist **common** abuse (e.g. **credential stuffing** mitigations proportionate to risk).
- **NFR-S5:** **Administrative** actions that affect **access** or **roles** are **auditable** to the extent required by MVP (see FR18 alignment).

### Privacy & data handling

- **NFR-PR1:** Processing of **personal data** in issues and accounts follows the **privacy** posture in **Domain-Specific Requirements** (including **region** and **retention** decisions documented for production).

### Scalability

- **NFR-SC1:** Architecture supports **multiple organizations** and **tens to hundreds** of **active users** in early production without **redesign**; **load** assumptions are documented.
- **NFR-SC2:** **Seasonal** or **peak-day** usage (e.g. **high** issue volume) **degrades gracefully** (queue latency, not silent loss).

### Accessibility & usability

- **NFR-A1:** Mobile UI follows **baseline** accessibility targets appropriate for **internal B2B** use (e.g. **text scaling**, **contrast**, **touch targets**); **formal WCAG** tiering is **explicit** if a customer requires it.

### Integration

- **NFR-I1:** **MVP** does **not** depend on **external** product integrations; **non-functional** expectations for future integrations are **out of scope** for v1.
