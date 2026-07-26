# OpenSpec specs for OpenLumify

## What this is

`openspec/specs/` holds a **reverse-engineered baseline** of how OpenLumify behaves today,
backfilled from the Piyaz tracker (project `LMFY`) on 2026-07-26. It was written by reading the
execution records of the fifteen completed onboarding tasks, `LMFY-1` through `LMFY-15`, and
checking each cited path against the tree.

These specs describe **observed behaviour of inherited code**, not requirements anyone authored
ahead of time and not behaviour independently re-verified by test. Treat them as a starting
baseline that gets more trustworthy as changes delta against it.

## Why it exists

The three roadmap thrusts — front-end modernization, graph datastore replacement, public-hosting
readiness — are largely *replacement* work. The risk in replacing RequireJS with a bundler, or
Vertexium with something else, is not "does it build" but "did behaviour change", measured
against a baseline nobody had written down. These files are that baseline.

## Division of labour with Piyaz

| | Piyaz (`LMFY`) | OpenSpec |
|---|---|---|
| Owns | task status, dependencies, execution records, decisions | capability specs, proposals, spec deltas |
| Lives in | the Piyaz server | this repo, in git |
| Source of truth for | what work is happening and in what order | what the system is supposed to do |

**Piyaz remains the only task tracker.** A change's generated `tasks.md` is planning detail that
feeds the corresponding Piyaz task's `implementationPlan` — it is not a parallel task list. Do not
create Piyaz tasks from `tasks.md` line items.

## Capability → LMFY traceability

| Capability spec | Backfilled from | Delta target for |
|---|---|---|
| `core-platform` | LMFY-1 | LMFY-23 (JDK/dependency upgrade) |
| `graph-datastore` | LMFY-2, LMFY-3 | LMFY-20, LMFY-21, LMFY-22 |
| `ingest-pipeline` | LMFY-4, LMFY-5, LMFY-12 (S3) | — |
| `structured-ingest` | LMFY-11 | — |
| `web-api` | LMFY-6 | LMFY-25 (security/multi-tenancy) |
| `server-runtime` | LMFY-7 | LMFY-24 (containerization) |
| `frontend-shell` | LMFY-8 | LMFY-17, LMFY-18, LMFY-19, LMFY-26 |
| `work-products` | LMFY-9, LMFY-10 | LMFY-19 (component model) |
| `auth-and-authorization` | LMFY-12 | LMFY-25 (security/multi-tenancy) |
| `operational-tooling` | LMFY-13 | LMFY-16 (setup docs) |
| `build-and-ci` | LMFY-14, LMFY-15 | LMFY-23, LMFY-24 |

The capability boundaries mirror the Piyaz project categories with three deliberate divergences:
the `plugins` category is split into `work-products`, `structured-ingest`, and
`auth-and-authorization` (it was a grab-bag and `LMFY-25` needs auth as its own delta target);
`tooling-ops` is split into `operational-tooling` and `build-and-ci`; and S3 cloud ingest is
specced under `ingest-pipeline` rather than with auth, where `LMFY-12` had filed it.

## Workflow

```
/opsx:propose "<change>"   → proposal, design, tasks, spec deltas under openspec/changes/
/opsx:apply                → implement against the delta
/opsx:archive              → fold the delta into openspec/specs/ once shipped
openspec validate --specs  → check spec structure
```

## Known gaps in the baseline

- Requirements are derived from Piyaz acceptance criteria and execution records. Where a record
  was thin, the spec is thin — `structured-ingest` and `operational-tooling` are the least
  detailed and would benefit from a pass against the actual code.
- No spec covers the ontology subsystem, dashboard/cards, or notifications as capabilities in
  their own right; they appear only where an LMFY record touched them.
- Nothing here is test-backed. A requirement passing review does not mean a test asserts it.
