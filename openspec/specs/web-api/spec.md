# Web API

## Purpose

The HTTP surface the front end and external clients call. A route layer exposes vertex, edge,
search, workspace, product, ontology, admin, user, and security operations, backed by a
parameter-provider framework that injects typed request parameters and privilege filters that
gate each route against the core security model.

> **Provenance.** Reverse-engineered baseline describing the system **as it exists today**,
> backfilled from Piyaz record `LMFY-6`. These are observed behaviours of inherited code, not
> independently re-verified requirements. This spec is a delta target for the public-hosting
> readiness thrust (`LMFY-25`).

## Requirements

### Requirement: REST Resource Routes

The system SHALL expose REST routes covering vertex, edge, search, workspace, product,
ontology, admin, user, and security operations, reading and writing through the graph model.

#### Scenario: Client reads a vertex

- **WHEN** an authenticated client requests a vertex by id
- **THEN** the vertex is loaded through the graph model and returned

#### Scenario: Client runs a search

- **WHEN** an authenticated client submits a search query
- **THEN** matching elements are returned from the indexed graph

### Requirement: Typed Parameter Injection

The system SHALL let routes declare typed request parameters that the parameter-provider
framework resolves and injects, so handlers do not parse raw request data.

#### Scenario: Declared parameter is injected

- **WHEN** a route declares a typed parameter
- **THEN** the framework resolves it from the request and passes it to the handler

#### Scenario: Missing or malformed parameter is rejected

- **WHEN** a required declared parameter is absent or cannot be coerced to its type
- **THEN** the request is rejected before the handler runs

### Requirement: Privilege Enforcement

The system SHALL reject requests to routes for which the calling user lacks the required
privilege, enforced by filters layered over the core security model.

#### Scenario: Unprivileged request is rejected

- **WHEN** a user without the required privilege calls a privileged route
- **THEN** the request is rejected and the handler does not run

#### Scenario: Privileged request proceeds

- **WHEN** a user holding the required privilege calls the same route
- **THEN** the handler runs

## Implementation Notes

- Implemented under `web/web-base/src/main/java/org/openlumify/web`.
- Decision (`LMFY-6`): request handling is centralized in a route + parameter-provider
  framework so endpoints declare typed inputs and privilege requirements declaratively.
- Authorization semantics are specified in `auth-and-authorization`; the runtime and packaging
  that serve these routes are specified in `server-runtime`.
