# NexaBank Offshore/Onshore Collaboration Guide

> This document defines the operating model for distributed delivery across onshore (US East) and offshore (India) teams. It covers service ownership, Git conventions, ceremony schedules, and defect escalation paths.

---

## Team Structure

| Team | Location | Time Zone | Hours (Local) | Hours (EST Overlap) |
|---|---|---|---|---|
| Architecture & Tech Lead | New York, NY | EST (UTC-5) | 9am–6pm | 9am–6pm |
| Account & Auth Squad | Hyderabad, India | IST (UTC+5:30) | 9am–6pm | 10:30pm–7:30am |
| Transaction Squad | Hyderabad, India | IST (UTC+5:30) | 9am–6pm | 10:30pm–7:30am |
| Notification & Loan Squad | Pune, India | IST (UTC+5:30) | 9am–6pm | 10:30pm–7:30am |
| Frontend Squad | New York, NY | EST (UTC-5) | 9am–6pm | 9am–6pm |

**Overlap Window:** 8:30am–10:00am EST (6:00pm–7:30pm IST)
All synchronous ceremonies must fall within this window.

---

## Service Ownership Matrix

| Service | Primary Owner | Offshore Squad | Onshore Review Required |
|---|---|---|---|
| `infrastructure/eureka-server` | Architecture Lead (Onshore) | — | Yes (all changes) |
| `infrastructure/config-server` | Architecture Lead (Onshore) | — | Yes (all changes) |
| `infrastructure/api-gateway` | Architecture Lead (Onshore) | Account Squad (assist) | Yes (all changes) |
| `services/account-service` | Account & Auth Squad (Offshore) | — | Yes (security changes) |
| `services/transaction-service` | Transaction Squad (Offshore) | — | Yes (ledger logic changes) |
| `services/notification-service` | Notification & Loan Squad (Offshore) | — | No (standard PR review) |
| `services/loan-service` | Notification & Loan Squad (Offshore) | — | Yes (AI/scoring logic) |
| `ai-layer` | Architecture Lead (Onshore) | Notification & Loan Squad (assist) | Yes (model changes) |
| `frontend/nexabank-ui` | Frontend Squad (Onshore) | — | No (standard PR review) |
| `docker-compose.yml` | Architecture Lead (Onshore) | — | Yes (all changes) |
| `Jenkinsfile` / `.github/workflows/` | Architecture Lead (Onshore) | — | Yes (all changes) |

**"Onshore Review Required"** means: do not merge until a US-based tech lead has approved the PR, not just passed automated checks.

---

## Git Branching Convention

```
main              ← Production-ready. Tagged for every release.
  │
  └── develop     ← Integration branch. All feature branches merge here.
        │
        ├── feature/NEXA-123-add-overdraft-protection
        ├── feature/NEXA-456-loan-credit-score-fix
        ├── bugfix/NEXA-789-balance-rounding-error
        └── hotfix/NEXA-001-prod-gateway-timeout     ← Branches from main
```

### Branch Naming Convention

```
{type}/{ticket-id}-{short-description}

Types:
  feature/  — new functionality
  bugfix/   — non-critical bug fix
  hotfix/   — production emergency (branches from main, NOT develop)
  chore/    — non-functional work (config, refactor, docs)
  test/     — adding or fixing tests only

Examples:
  feature/NEXA-312-kafka-dead-letter-queue
  bugfix/NEXA-410-notification-duplicate-records
  hotfix/NEXA-002-gateway-jwt-expiry-crash
  chore/NEXA-180-update-spring-boot-3.3.5
```

### Commit Message Format (Conventional Commits)

```
{type}({scope}): {short description}

{optional body — what and why, not how}

{optional footer — e.g., NEXA-123, BREAKING CHANGE: ...}
```

Examples:
```
feat(transaction): add NDM inbound file processor with control total validation

Previously only outbound NDM files were generated. This adds inbound processing
triggered at startup, matching the real Connect:Direct file hand-off pattern.

NEXA-312
```

```
fix(account): prevent negative balance on concurrent withdrawal requests

Added SELECT FOR UPDATE in AccountRepository to acquire a row-level lock
before balance check. Resolves race condition under load test.

NEXA-410
```

### Pull Request Rules

1. Branch from `develop` (not `main`) for all feature/bugfix work
2. PR title must follow Conventional Commits format
3. PR description must include: **What changed**, **Why**, **How to test**, **JIRA ticket link**
4. Minimum 1 approval required; 2 approvals required for changes to account-service security or api-gateway
5. CI checks must pass (GitHub Actions `ci.yml`) before merge is permitted
6. Squash merge preferred for feature branches; merge commit for release branches
7. Delete branch after merge

---

## Sprint Ceremonies (Time Zone Aligned)

All times are **EST**. Offshore joins at IST equivalent.

| Ceremony | Frequency | Time (EST) | Duration | Participants |
|---|---|---|---|---|
| Daily Standup | Mon–Fri | 9:00am EST (6:30pm IST) | 15 min | All squads |
| Sprint Planning | Bi-weekly Monday | 9:00am EST | 2 hours | Tech leads + squad leads |
| Sprint Review / Demo | Bi-weekly Friday | 9:00am EST | 1 hour | All squads + stakeholders |
| Retrospective | Bi-weekly Friday | 10:00am EST | 1 hour | All squads |
| Architecture Review | Monthly, 1st Monday | 9:00am EST | 1.5 hours | Architecture Lead + squad leads |
| Incident Review | Within 48h of P1/P2 | TBD | 1 hour | All affected squads |

### Standup Format
```
Each squad gives one update (< 2 minutes):
1. What did we complete since last standup?
2. What are we working on today?
3. Any blockers that need onshore assistance?
```

Blockers from offshore are captured in JIRA and escalated to the US tech lead by **10:30am EST** at the latest.

---

## Defect Escalation Path

### Severity Definitions

| Priority | Definition | Examples |
|---|---|---|
| **P1 — Critical** | Production down / data loss / security breach | Gateway not routing, accounts not accessible, JWT bypass |
| **P2 — High** | Core feature broken, workaround exists | Transfers failing, Kafka consumer stopped, NDM file missing |
| **P3 — Medium** | Degraded functionality, workaround exists | Notification not sent, loan AI fallback triggered unexpectedly |
| **P4 — Low** | Cosmetic / minor issue | UI alignment, log noise, non-critical validation |

### Escalation Procedure

```
P1 — Critical
  1. Offshore squad immediately pages US Tech Lead via Slack #nexa-incidents
  2. Tech Lead acknowledges within 15 minutes (24/7)
  3. Bridge call started within 30 minutes
  4. Hotfix branch from main: hotfix/NEXA-{id}-{description}
  5. Fix deployed within 4 hours with post-deploy verification
  6. Incident report within 24 hours

P2 — High
  1. Offshore squad creates JIRA ticket (P2 label) + posts in #nexa-bugs
  2. US Tech Lead reviews at next standup (9:00am EST)
  3. Fix scheduled within current sprint
  4. Follow-up verification required before sprint close

P3 — Medium
  1. JIRA ticket created, #nexa-bugs notification
  2. Triaged in next sprint planning
  3. Resolved within 2 sprints

P4 — Low
  1. JIRA ticket created
  2. Backlog grooming determines priority
```

### Escalation Contacts

| Role | Name | Contact | Availability |
|---|---|---|---|
| US Tech Lead | [Tech Lead] | Slack: @techlead | 9am–6pm EST (emergency 24/7) |
| Offshore Squad Lead (Account) | [Squad Lead] | Slack: @acct-lead | 9am–6pm IST |
| Offshore Squad Lead (Transaction) | [Squad Lead] | Slack: @txn-lead | 9am–6pm IST |
| DevOps/SRE | [SRE] | Slack: @sre-oncall | 24/7 rotation |

---

## Code Review Standards

### What reviewers check for

**All PRs:**
- [ ] Unit tests cover the new logic (minimum 80% line coverage on new code)
- [ ] No hardcoded secrets, credentials, or environment-specific URLs
- [ ] Exception handling uses `GlobalExceptionHandler` / `ProblemDetail` pattern
- [ ] Logging uses SLF4J (not `System.out.println`)
- [ ] No `@Transactional` on controller methods

**Database changes (any Flyway migration):**
- [ ] Uses Oracle-compatible SQL (sequences, not SERIAL; NUMERIC not FLOAT; uppercase names)
- [ ] Migration is backward-compatible (no column drops without deprecation period)
- [ ] Indexes added for any FK column or frequently queried column
- [ ] Reviewed by US Tech Lead before merge

**Security-sensitive changes (auth, JWT, permissions):**
- [ ] Reviewed by US Tech Lead regardless of offshore squad ownership
- [ ] No new endpoints added to the public route list without explicit approval

---

## Knowledge Transfer Protocol

When an offshore team member completes a complex feature:

1. **Written walkthrough** — Add a comment block at the top of the key class explaining the approach and any non-obvious decisions
2. **ADR (if architectural)** — Create a new `docs/architecture-decision-records/ADR-NNN-title.md`
3. **Demo in Sprint Review** — Live walkthrough in the bi-weekly demo
4. **Learning doc update** — If a new concept is introduced, add it to `docs/learning/`

This prevents knowledge silos and ensures any team member can own any service after 1 sprint of onboarding.

---

## Environment Topology

| Environment | Branch | Deploy Trigger | Approval Required |
|---|---|---|---|
| **Local** | any | Manual: `docker-compose up` | None |
| **Dev** | `develop` | Auto on merge | None |
| **Staging** | `main` | Manual (GitHub Environment gate) | US Tech Lead |
| **Production** | `main` (tagged) | Manual + CAB approval | US Tech Lead + CAB |

**CAB (Change Advisory Board):** All production deployments require a change ticket with: description, rollback plan, test evidence, and deployment window. Standard change window is Tuesdays and Thursdays, 9pm–11pm EST (low traffic period).
