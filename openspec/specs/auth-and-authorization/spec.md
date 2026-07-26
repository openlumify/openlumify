# Authentication and Authorization

## Purpose

Pluggable authentication providers, user and privilege administration, property-based
authorization, and account self-service flows. These extend the web tier's security surface and
operate against the core security model.

> **Provenance.** Reverse-engineered baseline describing the system **as it exists today**,
> backfilled from Piyaz record `LMFY-12`. These are observed behaviours of inherited code, not
> independently re-verified requirements. This spec is the primary delta target for security and
> multi-tenancy hardening (`LMFY-25`) — note that the baseline below has **no tenancy boundary**,
> which is the gap that work must close.

## Requirements

### Requirement: Pluggable Authentication Providers

The system SHALL authenticate users through pluggable authentication providers, shipping both
username/password and username-only implementations, selectable by configuration.

#### Scenario: Username/password authentication

- **WHEN** a user submits valid credentials to the username/password provider
- **THEN** the user is authenticated and a session is established

#### Scenario: Invalid credentials are rejected

- **WHEN** a user submits invalid credentials
- **THEN** authentication fails and no session is established

#### Scenario: Provider is swappable by configuration

- **WHEN** a different authentication provider is configured
- **THEN** it handles authentication with no change to the route layer

### Requirement: User and Privilege Administration

The system SHALL provide administrative tools to manage users, their privileges, and their
property-based authorizations.

#### Scenario: Administrator grants a privilege

- **WHEN** an administrator grants a privilege to a user
- **THEN** that user's subsequent requests to routes requiring it are permitted

#### Scenario: Administrator revokes an authorization

- **WHEN** an administrator revokes a property-based authorization from a user
- **THEN** that user no longer sees data requiring it

### Requirement: Property-Based Authorization

The system SHALL model authorization as property-based authorizations and privileges rather
than fixed roles, so visibility is evaluated per element property.

#### Scenario: Data is filtered by authorization

- **WHEN** a user queries elements carrying property visibility they lack authorization for
- **THEN** those properties are not returned

### Requirement: Account Self-Service

The system SHALL let an authenticated user change their own email address and password.

#### Scenario: User changes password

- **WHEN** an authenticated user completes the change-password flow with a valid current
  password
- **THEN** the password is updated and subsequent authentication requires the new password

#### Scenario: User changes email

- **WHEN** an authenticated user completes the change-email flow
- **THEN** the account's email address is updated

## Implementation Notes

- Implemented as web plugins under `web/plugins`: `auth-username-password`,
  `auth-username-only`, `admin-user-*`, the property-based auth/privilege plugins, and the
  `change-*` self-service flows.
- Each registers into the web-base route/security framework — see `web-api` for privilege
  enforcement at the route boundary and `core-platform` for the underlying security model.
- Decision (`LMFY-12`): authorization is property-based rather than role-based.
