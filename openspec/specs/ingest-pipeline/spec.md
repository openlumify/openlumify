# Ingest Pipeline

## Purpose

The asynchronous graph-property-worker (GPW) pipeline that enriches graph elements after data
lands. A runner consumes work items from the model queue, loads the affected graph element, and
fans it out through a chain of registered extractor workers that turn raw documents into
structured entities, relationships, and properties.

> **Provenance.** Reverse-engineered baseline describing the system **as it exists today**,
> backfilled from Piyaz records `LMFY-4`, `LMFY-5`, and the cloud-ingest portion of `LMFY-12`.
> These are observed behaviours of inherited code, not independently re-verified requirements.

## Requirements

### Requirement: Queue-Driven Worker Execution

The system SHALL run a worker process that consumes work items from the model queue and loads
the target graph element through the graph model layer before dispatching it.

#### Scenario: Work item drives element load

- **WHEN** a work item is consumed from the queue
- **THEN** the referenced graph element is loaded via the graph model
- **AND** the element is dispatched through the registered worker chain

### Requirement: Worker Plugin Registration

The system SHALL let extractor plugins register into the worker chain by extending the shared
plugin base, so new extractors are added without modifying the runner.

#### Scenario: New extractor participates without runner changes

- **WHEN** a plugin extending the GPW plugin base is present on the classpath
- **THEN** it is registered into the worker chain at startup
- **AND** the runner requires no modification

### Requirement: Property Change Fan-Out

The system SHALL fan each property change out to every registered worker that declares interest
in it, so enrichment composes across independent workers.

#### Scenario: One change reaches multiple workers

- **WHEN** a property on an element changes
- **THEN** every registered worker handling that property is invoked

### Requirement: Text and MIME Type Extraction

The system SHALL extract text content and detect MIME type from ingested files across supported
document formats, writing both back as properties on the graph element.

#### Scenario: Document yields text and MIME type

- **WHEN** a document is ingested
- **THEN** extracted text and a detected MIME type are written as properties on the element

### Requirement: Entity Extraction From Text

The system SHALL extract email addresses, phone numbers, and zipcodes from extracted text and
create corresponding entity vertices in the graph.

#### Scenario: Email address becomes an entity

- **WHEN** extracted text contains an email address
- **THEN** an entity vertex for that address exists in the graph and is related to the source
  element

#### Scenario: Extractors are independently pluggable

- **WHEN** one entity extractor is disabled
- **THEN** the remaining extractors continue to run

### Requirement: MIME Type To Ontology Mapping

The system SHALL map a detected MIME type to the corresponding ontology concept, so ingested
files are typed against the ontology rather than left as raw blobs.

#### Scenario: Detected type resolves to a concept

- **WHEN** an element carries a detected MIME type with a configured mapping
- **THEN** the element is assigned the mapped ontology concept

### Requirement: Cloud Source Ingest

The system SHALL pull source data from S3 into the platform for processing by the ingest
pipeline.

#### Scenario: S3 object is ingested

- **WHEN** an S3 source is configured and ingest is triggered
- **THEN** the object is retrieved and enters the ingest pipeline as a graph element

## Implementation Notes

- The GPW runner and worker chain are implemented in
  `core/core/src/main/java/org/openlumify/core/ingest/graphProperty/` (`GraphPropertyRunner`,
  `GraphPropertyWorker`, `GraphPropertyThreadedWrapper`), driven by
  `core/core/src/main/java/org/openlumify/core/process/GraphPropertyRunnerProcess.java`.
- The plugin base is `graph-property-worker/graph-property-worker-plugin-base`.
- Concrete extractors live under `graph-property-worker/plugins`: `tika-text-extractor`,
  `tika-mime-type`, `email-extractor`, `phone-number-extractor`, `zipcode-extractor`,
  `mime-type-ontology-mapper`.
- Cloud ingest is `web/plugins/ingest-cloud-s3`.
- Decision (`LMFY-5`): Apache Tika chosen for text extraction and MIME detection.
- Decision (`LMFY-4`): workers operate on graph properties via the model layer, never raw storage.

> **Discrepancy noted during backfill.** The `LMFY-4` execution record cites a
> `graph-property-worker/graph-property-worker` module for the runner. No such module exists;
> the runner is in `core/core` as recorded above. The Piyaz record should be corrected.
