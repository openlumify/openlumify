# Front-End Shell

## Purpose

The analyst-facing single-page application served from the WAR: app shell, menubar, dashboard,
search, detail/inspector panes, field rendering, and workspace UI. Built on the inherited
Visallo front-end architecture — React 16 components mixed with Flight.js-attached behaviour,
modularized under RequireJS, built with Grunt + Babel and tested with Karma.

> **Provenance.** Reverse-engineered baseline describing the system **as it exists today**,
> backfilled from Piyaz record `LMFY-8`. These are observed behaviours of inherited code, not
> independently re-verified requirements. This spec is the primary delta target for the
> front-end modernization thrust (`LMFY-17`, `LMFY-18`, `LMFY-19`) and the accessibility
> baseline (`LMFY-26`) — the requirements below are the behaviour a bundler or component-model
> replacement must preserve.

## Requirements

### Requirement: Application Shell Layout

The system SHALL render an application shell providing the menubar, dashboard, search, and
detail/inspector panes as the analyst's primary workspace surface.

#### Scenario: Analyst opens the application

- **WHEN** an authenticated analyst loads the application
- **THEN** the menubar, dashboard, search, and detail/inspector panes render

#### Scenario: Selecting an entity populates the inspector

- **WHEN** the analyst selects an entity
- **THEN** the detail/inspector pane renders that entity's properties via the field components

### Requirement: REST-Backed Data Rendering

The system SHALL source all displayed data from the web-base REST routes, holding no
independent persistence.

#### Scenario: Graph entities are fetched and rendered

- **WHEN** the shell needs entity or relationship data
- **THEN** it calls the web-base REST routes and renders the returned entities

### Requirement: Workspace Management

The system SHALL let an analyst manage workspaces, scoping the entities, relationships, and
work products they are currently working within.

#### Scenario: Analyst switches workspace

- **WHEN** the analyst selects a different workspace
- **THEN** the shell's displayed entities and open work products reflect that workspace

### Requirement: Front-End Build Pipeline

The system SHALL produce the deployable webapp bundle from source through the front-end build,
and SHALL run the front-end test suite as part of that build.

#### Scenario: Build produces the bundle

- **WHEN** the front-end build runs
- **THEN** a webapp bundle suitable for WAR packaging is produced

#### Scenario: Front-end tests execute

- **WHEN** the front-end test suite runs
- **THEN** specs execute in a real browser engine and their results gate the build

## Implementation Notes

- Implemented under `web/war/src/main/webapp/js`; built via
  `web/war/src/main/webapp/Gruntfile.js` with Babel transpilation and Karma test running;
  dependencies in `web/war/src/main/webapp/package.json`.
- Decision (`LMFY-8`): the stack is React 16 + RequireJS module loading + Flight.js component
  attachment — inherited from Visallo, and the explicit target of `LMFY-18`/`LMFY-19`.
- Headless test execution specifics are in `build-and-ci`.
