# Structured Ingest

## Purpose

The tabular-data counterpart to the unstructured GPW extractor pipeline. Analysts map columns
from structured files (spreadsheets, Parquet) onto ontology concepts and properties, and the
mapping run materializes graph vertices, edges, and properties from each row.

> **Provenance.** Reverse-engineered baseline describing the system **as it exists today**,
> backfilled from Piyaz record `LMFY-11`. These are observed behaviours of inherited code, not
> independently re-verified requirements.

## Requirements

### Requirement: Column To Ontology Mapping

The system SHALL let an analyst map columns of a structured source file to ontology concepts
and properties through an interactive mapping step before any graph data is created.

#### Scenario: Analyst maps a spreadsheet

- **WHEN** an analyst uploads a spreadsheet or Parquet file and opens the mapping UI
- **THEN** the file's columns are listed
- **AND** each column can be assigned to an ontology concept or property

#### Scenario: Mapping is reviewable before commit

- **WHEN** a mapping is defined but not yet run
- **THEN** no graph elements have been created

### Requirement: Mapping Execution

The system SHALL create graph vertices, edges, and properties from the rows of the source file
according to the defined mapping.

#### Scenario: Rows become graph elements

- **WHEN** a defined mapping is run against a structured source
- **THEN** each row produces the mapped vertices, edges, and properties in the graph
- **AND** the created elements enter the ingest pipeline for further enrichment

## Implementation Notes

- Implemented under `web/plugins/structured-ingest`: the mapping UI plus the server-side
  mapping execution.
- Decision (`LMFY-11`): implemented as a web plugin distinct from the GPW unstructured
  extractors, since the mapping step is analyst-driven rather than automatic.
- Created elements feed through the graph model and GPW pipeline — see `ingest-pipeline`.
