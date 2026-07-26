# Operational Tooling

## Purpose

The operator-facing entry points for running and maintaining the platform: command-line tools
for data loading, schema management, and maintenance; a Maven archetype for scaffolding new
plugin modules; and developer scaffolding for running the platform locally.

> **Provenance.** Reverse-engineered baseline describing the system **as it exists today**,
> backfilled from Piyaz record `LMFY-13`. These are observed behaviours of inherited code, not
> independently re-verified requirements. Local-setup documentation is tracked as `LMFY-16`.

## Requirements

### Requirement: Command-Line Operations

The system SHALL provide command-line tools for import, reindex, ontology-ingest, OWL handling,
migrations, graph-check, and running the GPW pipeline, driving the graph model and ingest
pipeline directly.

#### Scenario: Bulk import loads data

- **WHEN** the operator runs the import tool against a source
- **THEN** the data is written into the graph through the model layer

#### Scenario: Reindex rebuilds the search index

- **WHEN** the operator runs the reindex tool
- **THEN** the search index is rebuilt from the graph store

#### Scenario: Ontology is loaded from OWL

- **WHEN** the operator runs ontology-ingest against an OWL file
- **THEN** the ontology concepts and properties are available to the platform

#### Scenario: Graph check reports integrity problems

- **WHEN** the operator runs graph-check
- **THEN** integrity problems in the graph are reported

### Requirement: Plugin Scaffolding

The system SHALL provide a Maven archetype that scaffolds a new plugin module conforming to the
established module layout and plugin-binding contract.

#### Scenario: Archetype produces a buildable plugin

- **WHEN** the operator generates a project from the archetype
- **THEN** a plugin module is produced that builds and registers into the core container

### Requirement: Local Development Runtime

The system SHALL provide developer scaffolding that runs the platform locally without a full
production deployment.

#### Scenario: Developer runs the platform locally

- **WHEN** a developer starts the dev scaffolding
- **THEN** the platform runs locally against the in-memory model implementations

## Implementation Notes

- Implemented under `tools` (import, reindex, ontology-ingest, owl, migrations, graph-check,
  run-gpw), `archetype` (the Maven plugin archetype), `bin` (launch scripts), and `dev`
  (`dev/cli`, `dev/tomcat-server`).
- Decision (`LMFY-13`): reindex, migrations, and graph-check are first-class operational
  commands rather than ad-hoc scripts.
