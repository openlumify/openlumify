# Graph Datastore

## Purpose

The persistence and query abstraction for entities and relationships, plus the supporting
non-graph storage and work-queue primitives. Graph storage is provided by Vertexium over
Accumulo with Elasticsearch indexing, with a parallel in-memory implementation for tests and
dev runs. Structured metadata uses SimpleORM, and work queuing uses an in-memory queue backend.

> **Provenance.** Reverse-engineered baseline describing the system **as it exists today**,
> backfilled from Piyaz records `LMFY-2` and `LMFY-3`. These are observed behaviours of inherited
> code, not independently re-verified requirements. This spec is the primary delta target for the
> graph datastore modernization thrust (`LMFY-20`, `LMFY-21`, `LMFY-22`).

## Requirements

### Requirement: Graph Persistence and Indexing

The system SHALL persist vertices and edges to Accumulo through Vertexium and index them to
Elasticsearch, so entities and relationships are both durably stored and searchable.

#### Scenario: Entity is written and retrievable

- **WHEN** a vertex or edge is written through the graph model
- **THEN** it is persisted to Accumulo
- **AND** it is indexed to Elasticsearch and returned by a matching search query

#### Scenario: Property update reindexes

- **WHEN** a property on an existing element changes
- **THEN** the element's index entry reflects the new value

### Requirement: In-Memory Graph Implementation

The system SHALL provide an in-process graph implementation exposing the same graph API as the
Accumulo-backed implementation, so tests and development runs do not require standing up
Accumulo or Elasticsearch.

#### Scenario: Tests run without external services

- **WHEN** the in-memory model is bound as the graph implementation
- **THEN** graph reads and writes succeed with no Accumulo or Elasticsearch instance running

### Requirement: Graph Model Conformance Harness

The system SHALL provide a shared test harness that exercises every graph model implementation
against the same conformance suite, so implementations remain behaviourally interchangeable.

#### Scenario: Both implementations satisfy the same suite

- **WHEN** the shared conformance harness runs
- **THEN** the Accumulo-backed and in-memory implementations both pass it

### Requirement: Structured Metadata Storage

The system SHALL persist and read structured, non-graph platform metadata through a SimpleORM
backed row store, kept distinct from the graph store.

#### Scenario: Metadata round-trip

- **WHEN** structured metadata is written through the SimpleORM model
- **THEN** a subsequent read returns the same record

### Requirement: Work Queue Dispatch

The system SHALL provide a work-queue repository that dispatches and consumes work items,
backing the asynchronous ingest pipeline.

#### Scenario: Work item is dispatched and consumed

- **WHEN** a work item is pushed onto the queue
- **THEN** a registered consumer receives it exactly once

## Implementation Notes

- Implemented as `model-vertexium`, `model-vertexium-inmemory`, `model-vertexium-test`,
  `model-simpleorm`, and `model-queue-inmemory` under `core/plugins`.
- All bind into the core DI container as selectable model implementations; the active
  implementation is a configuration choice, not a compile-time one.
- Decision (`LMFY-2`): Vertexium chosen as the graph abstraction over Accumulo +
  Elasticsearch 5.5.
- Decision (`LMFY-3`): the in-memory work queue is the default/dev queue backend.
