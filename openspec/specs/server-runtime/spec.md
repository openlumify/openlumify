# Server Runtime

## Purpose

The deployable artifact and the runtime that serves it. A container-agnostic server abstraction
sits above a Tomcat implementation, and a Maven assembly packages the web-base routes, the
front-end webapp, and WEB-INF configuration into a single deployable WAR.

> **Provenance.** Reverse-engineered baseline describing the system **as it exists today**,
> backfilled from Piyaz record `LMFY-7`. These are observed behaviours of inherited code, not
> independently re-verified requirements. This spec is the primary delta target for
> containerization and deployment work (`LMFY-24`).

## Requirements

### Requirement: WAR Assembly

The system SHALL assemble a deployable WAR bundling the web-base routes, the front-end webapp,
and WEB-INF configuration.

#### Scenario: Assembly produces a complete artifact

- **WHEN** the WAR assembly runs
- **THEN** the artifact contains the web-base route classes, the built front-end webapp, and
  WEB-INF configuration

### Requirement: Servlet Container Boot

The system SHALL boot the web application from the assembled WAR on a Tomcat 8.5 servlet
container and serve the REST API and front end from a single origin.

#### Scenario: Deployed WAR serves the application

- **WHEN** the WAR is deployed to the Tomcat container and started
- **THEN** the REST API responds
- **AND** the front-end shell is served from the same origin

### Requirement: Container-Agnostic Server Abstraction

The system SHALL keep the server abstraction separate from the Tomcat implementation, so an
alternative container can be introduced without changing the abstraction's consumers.

#### Scenario: Consumers depend only on the abstraction

- **WHEN** a module needs to start or address the server
- **THEN** it depends on the server-base abstraction rather than the Tomcat implementation

## Implementation Notes

- Implemented under `web/server` (server-base abstraction plus the tomcat-server runtime) with
  the assembly driven by `config/assembly.xml` into `web/war/src/main/webapp/WEB-INF`.
- Decision (`LMFY-7`): the front end is served from the WAR rather than a separate origin —
  note this constrains any future split-origin or CDN deployment.
- Decision (`LMFY-7`): server-base is split from tomcat-server specifically to keep the
  abstraction container-agnostic.
