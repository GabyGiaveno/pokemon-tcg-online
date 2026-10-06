# ARCHITECTURE.md

> **Estado actual del proyecto y puntos críticos:** ver
> `docs/architecture/006-estado-actual-y-puntos-criticos.md`.
> Las decisiones arquitectónicas históricas están en `docs/architecture/00X-*.md`.

## System Overview

This project is a multiplayer digital implementation of the Pokémon Trading Card Game (Pokémon TCG), specifically targeting the XY1 format.

The system is designed around a deterministic backend-authoritative architecture where ALL game rules, state transitions, validations, and effect resolutions are executed exclusively on the backend.

The frontend is a rendering and interaction layer only.

The architecture prioritizes:
- deterministic gameplay
- modularity
- testability
- maintainability
- AI-assisted development
- strict separation of concerns
- domain-driven organization
- extensibility for future card expansions

---

# Core Architectural Principles

## 1. Backend Authoritative

The backend is the single source of truth.

The frontend:
- MUST NOT validate rules
- MUST NOT calculate damage
- MUST NOT mutate authoritative game state
- MUST NOT resolve effects

The frontend ONLY:
- renders state
- captures user interaction
- sends player intents/actions
- displays events

ALL rule enforcement belongs to the backend Game Engine.

---

## 2. Deterministic Engine

The Game Engine MUST behave deterministically.

Given:
- identical GameState
- identical Action sequence

The engine MUST always produce:
- identical resulting GameState
- identical generated Events

The engine MUST NOT:
- depend on frontend state
- depend on UI timing
- depend on asynchronous randomness
- access external services during resolution

---

## 3. State-Driven Architecture

The entire game operates through explicit state transitions.

Core equation:

```text
CurrentState + Action → NewState + Events
```

Game logic MUST be modeled as:
- Actions
- Validators
- Resolvers
- Events
- State mutations

NOT as:
- UI callbacks
- controller logic
- websocket handlers

---

## 4. Engine Isolation

The Game Engine MUST be framework-agnostic.

The engine MUST NOT:
- depend on Angular
- depend on Spring Controllers
- depend on WebSockets
- depend on persistence implementations
- depend on HTTP concepts
- depend on DTOs

The engine SHOULD behave like a pure domain module/library.

---

## 5. Explicit Domain Ownership

Every responsibility MUST have a clear owner.

| Responsibility | Owner |
|---|---|
| Rule validation | Validators |
| State mutation | Resolvers |
| Event generation | Engine |
| Rendering | Frontend |
| Persistence | Repository layer |
| Realtime communication | WebSocket layer |
| Authentication | Security layer |

Responsibilities MUST NOT overlap.

---

# System Layers

## Frontend Layer (Angular)

Responsibilities:
- render board state
- handle drag/drop interactions
- send actions
- subscribe to websocket updates
- display animations
- render game logs

Forbidden:
- game rule validation
- damage calculation
- authoritative state mutation
- effect resolution

Frontend MUST consume backend state as immutable truth.

---

## API Layer

Includes:
- REST Controllers
- WebSocket Controllers
- DTO mapping

Responsibilities:
- transport
- serialization
- request validation
- authentication boundaries

Controllers MUST remain thin.

Controllers MUST NOT:
- contain game logic
- mutate GameState directly
- resolve effects

Controllers MUST delegate to Application Services.

---

## Application Layer

Responsibilities:
- orchestration
- session coordination
- transaction boundaries
- persistence coordination
- engine invocation

Application Services:
- convert DTOs → Actions
- invoke GameEngine
- persist snapshots
- publish events

Application Services MUST NOT:
- implement game rules
- calculate gameplay outcomes

---

## Core Game Engine

The Core Game Engine is the heart of the system.

Responsibilities:
- rule validation
- state transitions
- attack resolution
- effect processing
- knockout handling
- turn flow
- status effects
- event generation

The engine processes:
- GameState
- GameAction

The engine produces:
- updated GameState
- GameEvents

Primary flow:

```text
Action
→ Validation
→ Resolution
→ Event Generation
→ State Update
```

---

## Persistence Layer

Responsibilities:
- snapshot storage
- match recovery
- deck persistence
- player persistence
- audit logs

Persistence MUST remain independent from engine logic.

Repositories MUST NOT:
- contain gameplay logic
- calculate rules
- mutate engine state directly

---

# Game Engine Internal Architecture

The engine is divided into specialized modules.

## Engine Module Structure

```text
game/
├── engine/
├── state/
├── action/
├── event/
├── validator/
├── resolver/
├── effect/
├── model/
├── phase/
└── exception/
```

---

# state/

Contains authoritative game state structures.

Examples:
- GameState
- PlayerState
- BoardState
- TurnState

State objects SHOULD remain serializable.

State objects SHOULD avoid behavior-heavy implementations.

---

# action/

Represents player intents.

Examples:
- PlayEnergyAction
- AttackAction
- RetreatAction
- EndTurnAction

Actions MUST:
- be immutable
- contain no business logic
- contain only intent data

Actions MUST NOT:
- mutate state
- resolve effects

---

# validator/

Responsible for rule validation ONLY.

Validators:
- verify legality
- enforce constraints
- throw rule exceptions

Validators MUST NOT:
- mutate GameState
- emit events

---

# resolver/

Responsible for state mutation and gameplay resolution.

Resolvers:
- apply damage
- resolve attacks
- apply status effects
- update prizes
- mutate GameState

Resolvers MAY:
- generate events

Resolvers MUST NOT:
- access transport layers
- access repositories directly

---

# event/

Represents domain events generated during gameplay.

Examples:
- DamageAppliedEvent
- PokemonKnockedOutEvent
- TurnEndedEvent

Events MUST be immutable.

Events SHOULD describe:
- what happened
- who caused it
- affected targets

Events MUST NOT:
- contain business logic

---

# effect/

Contains reusable gameplay effects.

Examples:
- PoisonEffect
- ParalysisEffect
- HealEffect

Effects SHOULD remain composable.

Effects MUST operate only on:
- GameContext
- GameState
- Targets

Effects MUST NOT:
- access HTTP
- access repositories
- access frontend concerns

---

# Design Patterns

The architecture explicitly uses the following patterns.

## State Pattern
Used for:
- match phases
- turn phases
- status transitions

---

## Strategy Pattern
Used for:
- attack behaviors
- effect resolution
- AI decision systems
- targeting systems

---

## Observer Pattern
Used for:
- event publishing
- websocket updates
- game logs
- replay systems

---

## Facade Pattern
Used through:
- GameEngineFacade

Provides simplified engine entrypoints. MUST remain a pure delegator: wiring +
delegation to `TurnManager`, with no game logic and no direct knowledge of the
persistence layer. The only bridge to Spring/persistence is the `CardLookup`
functional interface, wired from `CardCacheService` (see ADR 005).

---

## Chain of Responsibility
Used for:
- validation pipelines
- attack resolution stages
- modifier systems

---

# Communication Model

## REST

Used for:
- authentication
- deck management
- matchmaking
- match initialization

REST MUST NOT:
- synchronize live gameplay state continuously

---

## WebSocket

Used for:
- realtime game synchronization
- event streaming
- turn updates
- animations
- reconnect sync

WebSocket payloads SHOULD remain event-oriented.

---

# Persistence Strategy

The system uses snapshot-based persistence.

Snapshots contain:
- complete GameState
- turn state
- board state
- active effects

Additionally:
- audit logs
- event history

may be stored separately.

The engine MUST remain persistence-agnostic.

---

# Testing Philosophy

The engine MUST be heavily unit tested.

Critical systems requiring tests:
- attack resolution
- energy validation
- knockout flow
- turn flow
- effect interactions
- special conditions

Tests MUST validate:
- deterministic behavior
- state correctness
- event generation

---

# AI Development Constraints

This repository is designed for AI-assisted development.

AI-generated code MUST:
- respect architectural boundaries
- avoid business logic leakage
- follow naming conventions
- maintain deterministic behavior
- avoid framework coupling inside engine

AI agents MUST read:
- ARCHITECTURE.md
- docs/PROJECT_CONSTITUTION.md
- docs/ENGINE_SPEC.md (+ docs/ENGINE_GUIDE.md para detalle de implementación)
- docs/GAME_RULES.md
- docs/architecture/006-estado-actual-y-puntos-criticos.md (estado y puntos críticos)

before generating gameplay code.

---

# Forbidden Architectural Violations

The following are STRICTLY forbidden:

- business logic inside controllers
- frontend rule validation
- validators mutating state
- repositories resolving gameplay
- websocket handlers modifying GameState
- direct state mutation outside resolvers
- engine depending on transport frameworks
- circular dependencies between layers

Violations MUST be refactored immediately.

---

# Long-Term Scalability Goals

The architecture MUST support:
- future card expansions
- additional effect systems
- AI opponents
- replay systems
- ranked matchmaking
- spectating
- tournament systems
- analytics
- event sourcing migration

without major engine rewrites.