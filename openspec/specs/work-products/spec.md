# Work Products

## Purpose

Pluggable analyst-facing visualizations that register into the front-end shell and open within
a workspace. Two ship today: an interactive node-link graph for exploring entities and
relationships, and an OpenLayers map for exploring geo-located entities spatially.

> **Provenance.** Reverse-engineered baseline describing the system **as it exists today**,
> backfilled from Piyaz records `LMFY-9` and `LMFY-10`. These are observed behaviours of
> inherited code, not independently re-verified requirements.

## Requirements

### Requirement: Work Product Registration

The system SHALL let a visualization register as a selectable work product in the front-end
shell and open within an analyst's workspace, without the shell being modified per product.

#### Scenario: Product is selectable in a workspace

- **WHEN** a work-product plugin is present
- **THEN** it appears as a selectable product in the shell
- **AND** it opens within the analyst's current workspace

### Requirement: Graph Visualization

The system SHALL render entities and relationships as an interactive node-link diagram
supporting layout, selection, and graph manipulation.

#### Scenario: Analyst explores the graph

- **WHEN** the analyst opens the graph product in a workspace containing entities
- **THEN** entities render as nodes and relationships as edges
- **AND** the analyst can lay out, select, and manipulate them

### Requirement: Map Visualization

The system SHALL plot geo-located entities on an OpenLayers map so analysts can explore spatial
relationships.

#### Scenario: Geo entities appear on the map

- **WHEN** the analyst opens the map product in a workspace containing geo-located entities
- **THEN** those entities are plotted at their coordinates

### Requirement: Geo Layer Formats

The system SHALL load and render GeoJSON and KML layer sources on the map.

#### Scenario: GeoJSON layer renders

- **WHEN** a GeoJSON layer source is added to the map
- **THEN** its features render as a map layer

#### Scenario: KML layer renders

- **WHEN** a KML layer source is added to the map
- **THEN** its features render as a map layer

## Implementation Notes

- Implemented under `web/plugins/graph-product` and `web/plugins/map-product`.
- Both consume vertex, edge, and spatial data from the web-base API — see `web-api`.
- Decision (`LMFY-9`): graph visualization is a self-contained work-product plugin rather than
  shell-embedded, which is what makes the registration contract above load-bearing.
- Decision (`LMFY-10`): OpenLayers chosen as the map rendering engine.
