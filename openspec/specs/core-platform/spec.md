# Core Platform

## Purpose

The base runtime layer in `core/core` that every other module composes against. It provides
dependency-injection bootstrap, configuration loading, the security and authorization model,
caching, and component lifecycle management, and it establishes the `org.openlumify` package
namespace and plugin-binding contract used across the Maven multi-module build.

> **Provenance.** Reverse-engineered baseline describing the system **as it exists today**,
> backfilled from Piyaz record `LMFY-1`. These are observed behaviours of inherited code, not
> independently re-verified requirements. Modernization work should land as spec deltas against
> this file rather than editing it in place.

## Requirements

### Requirement: Dependency Injection Bootstrap

The system SHALL provide a dependency-injection container that loads configuration and binds
registered components at startup, so downstream modules resolve their collaborators without
constructing them directly.

#### Scenario: Container starts with registered components

- **WHEN** the platform bootstraps
- **THEN** configuration is loaded and every registered component is bound in the DI container
- **AND** downstream modules resolve bound components through the injection helper

#### Scenario: Unresolvable binding fails fast

- **WHEN** a component declares a dependency with no registered binding
- **THEN** bootstrap fails at startup rather than at first use

### Requirement: Configuration Subsystem

The system SHALL load platform configuration from the configured sources and expose typed
configuration values to any component in the container.

#### Scenario: Component reads configuration

- **WHEN** a component requests a configuration value
- **THEN** the value resolved at bootstrap is returned

### Requirement: Security and Authorization Primitives

The system SHALL expose a security and authorization model to downstream modules, providing the
user, privilege, and authorization abstractions the web tier and model layer enforce against.

#### Scenario: Downstream module checks authorization

- **WHEN** a module evaluates whether a user may perform an operation
- **THEN** the core security model resolves the user's privileges and authorizations

### Requirement: Caching and Component Lifecycle

The system SHALL provide caching services and manage component lifecycle (start, stop) for
registered components.

#### Scenario: Lifecycle shutdown

- **WHEN** the platform shuts down
- **THEN** lifecycle-managed components are stopped in dependency order

### Requirement: Plugin Discovery Across Modules

The system SHALL discover and resolve plugin modules across the Maven multi-module build, so
plugins register into the container without the core being modified.

#### Scenario: New plugin module is discovered

- **WHEN** a plugin module is present on the classpath
- **THEN** its registered components are discovered and bound at bootstrap
- **AND** no change to `core/core` is required to enable it

## Implementation Notes

- Implemented under `core/core/src/main/java/org/openlumify/core`.
- Runtime baseline is Java 8; see `build-and-ci` for the build and toolchain baseline.
- Decision (`LMFY-1`): a single core platform module acts as the DI/bootstrap root so all
  plugins bind against one container.
