# Build and Continuous Integration

## Purpose

The fork's identity and its build pipeline: the `org.openlumify` namespace adopted when Visallo
was forked, and the GitHub Actions Maven workflow that builds and tests the multi-module project
on every push. A trustworthy green build is the safety net the modernization thrusts depend on.

> **Provenance.** Reverse-engineered baseline describing the system **as it exists today**,
> backfilled from Piyaz records `LMFY-14` and `LMFY-15`. These are observed behaviours, not
> independently re-verified requirements. This spec is a delta target for the runtime upgrade
> (`LMFY-23`) and deployment infrastructure (`LMFY-24`).

## Requirements

### Requirement: Namespace and Project Identity

The system SHALL use the `org.openlumify` package namespace across every module, reflecting the
fork's identity rather than the upstream Visallo one.

#### Scenario: All packages use the fork namespace

- **WHEN** the source tree is inspected
- **THEN** package declarations use `org.openlumify`

### Requirement: Continuous Integration Build

The system SHALL build and test the full multi-module project on GitHub Actions on every push,
reproducibly from a clean checkout.

#### Scenario: Push triggers a green build

- **WHEN** a commit is pushed
- **THEN** the Java CI workflow runs `mvn -B package` across every module
- **AND** the build passes from a clean checkout

#### Scenario: Failing tests break the build

- **WHEN** a module's tests fail
- **THEN** the workflow reports failure rather than passing

### Requirement: Time-Independent Tests

The system SHALL NOT make test outcomes depend on wall-clock dates, so a suite that passes today
still passes later.

#### Scenario: Future-dated fixture stays valid

- **WHEN** a test needs a timestamp in the future
- **THEN** it derives that timestamp relative to the current time rather than hardcoding a date

### Requirement: Headless Front-End Test Execution

The system SHALL execute the front-end Karma suite in a headless browser available on the CI
image, and the run SHALL actually execute specs rather than reporting a hollow pass.

#### Scenario: Karma specs run on CI

- **WHEN** the front-end suite runs on the CI image
- **THEN** the headless browser launches successfully
- **AND** a non-zero number of specs execute and their results gate the build

## Implementation Notes

- Workflow at `.github/workflows/maven.yml`; Karma configuration at
  `web/war/src/main/webapp/karma.conf.js` using a `ChromeHeadlessCI` launcher
  (base `ChromeHeadless`, `--no-sandbox --disable-gpu`).
- Decision (`LMFY-14`): migrated from Travis to GitHub Actions.
- Decision (`LMFY-15`): PhantomJS was replaced because it cannot launch on modern CI images
  (bundled OpenSSL vs OpenSSL 3.x). The "Time-Independent Tests" requirement above generalizes
  the `SystemNotificationRepositoryTestBase` hardcoded-date failure so it is not re-introduced.

### Known Baseline Gaps

- `maven.yml` Node and JDK versions are not aligned with `root/pom.xml`; the
  `frontend-maven-plugin` installs its own Node v11.13.0 regardless. Descoped in `LMFY-15`,
  and in scope for `LMFY-23`.
- The build baseline remains Java 8 — the target of `LMFY-23`.
