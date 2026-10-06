# Design: Tablero Funcional — Complete Turn/Match Flow

> Status: updated to cover the FULL turn/match flow (all player actions + automatic phases).
> Source of truth for BE contract: `ActionType`, `GameEventType`, `TurnPhase`, `SelectionType`,
> `ActionRequest`, `BoardStateDTO`, `PendingSelectionDTO`, `KnockoutProcessor`.

## Technical Approach

The board is a single SMART container that subscribes to the authoritative `BoardStateDTO`
(REST snapshot + WebSocket `GameEvent` stream) and routes on `status`
(`SETUP | ACTIVE | FINISHED`). All real game logic lives in the BE engine; the FE is a
**projection + dispatcher**: it renders state, computes which actions are *legal to attempt*,
and POSTs `ActionRequest`s to `POST /api/games/{id}/actions`. The FE never mutates game truth
locally (optimistic patching is deferred to Phase 2).

Two interaction families:
1. **Explicit player actions** — driven by hand/field selection + a target click, or by a
   stateless button (`END_TURN`, `CONCEDE`).
2. **Automatic phases** (`DRAW`, `BETWEEN_TURNS`) — no UI action; the FE only **observes**
   `GameEvent`s and reflects them (size counters, log entries, condition animations, and the
   post-KO selection modal).

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|----------|--------|----------|-----------|
| Action dispatch model | One `interactionMode` state machine (`idle / play-basic / attach / evolve / attack / retreat / play-trainer / resolve-selection`) gating field/hand clicks | A handler per component | Single source of "what does a click mean right now"; prevents conflicting affordances |
| Prize handling | Render prize counts as **read-only**; treat `TAKE_PRIZE_CARD` as contract-ready but NOT wired | Build interactive `take-prize-modal` now | `KnockoutProcessor.awardPrizes` takes prizes AUTOMATICALLY; no BE handler consumes `prizeIndex` yet. Drawing a picker would dispatch a no-op |
| Post-KO promotion | Reuse `pending-selection-modal` driven by `pendingSelection.type === CHOOSE_ACTIVE_ON_KO` → `RESOLVE_SELECTION {benchIndex}` | Custom KO modal | Only real post-KO choice the engine exposes; `validOptions` already carries bench cardIds |
| Automatic phases | Observe-only: react to `PHASE_CHANGED`, `CARD_DRAWN`, `STATUS_EFFECT_APPLIED/CLEARED`, `DAMAGE_DEALT`, `POKEMON_HEALED`, `PRIZE_TAKEN` | Poll REST each tick | Engine already auto-advances DRAW→MAIN and runs BETWEEN_TURNS; FE just mirrors events |
| Trainer plays | One `play-trainer-modal` for ITEM/SUPPORTER/STADIUM (same confirm pattern) showing "effect pending" when unimplemented | Three bespoke modals | Identical request shape (`cardId`); most XY1 effects are not implemented in BE |
| Legality | FE computes *enabled* affordances from phase + `pendingSelection`; BE remains the validator | Trust FE legality | BE `RuleValidator` is authoritative; FE legality is UX hinting only |

## Component Tree

```
board-container (SMART — subscribes BoardStateDTO + GameEvent stream, owns interactionMode)
├─ setup-phase            (status === SETUP)
│  ├─ pokemon-placer      → SETUP_PLACE_POKEMON
│  └─ prize-setter        → SETUP_SET_PRIZES
├─ active-game            (status === ACTIVE)
│  ├─ opponent-field      (read-only; counts only for hand/deck/prizes)
│  ├─ player-field        (interactive targets: ACTIVE, BENCH_0..4)
│  ├─ hand-zone           (selectable cards; classifies basic / evolution / energy / trainer / tool)
│  ├─ action-panel        (PLAY_BASIC, PLAY_ITEM, PLAY_SUPPORTER, PLAY_STADIUM, END_TURN, CONCEDE + status text)
│  ├─ phase-indicator / game-log   (mirrors PHASE_CHANGED + event log)
│  └─ modal-host
│     ├─ card-zoom-modal
│     ├─ attack-modal             (USE_ATTACK {attackIndex})
│     ├─ target-picker-modal      (ATTACH_ENERGY / ATTACH_TOOL / EVOLVE targetPosition)
│     ├─ pending-selection-modal  (RESOLVE_SELECTION {benchIndex} — CHOOSE_ACTIVE_ON_KO)
│     ├─ play-trainer-modal       (NEW — confirm ITEM/SUPPORTER/STADIUM; "effect pending" note)
│     └─ take-prize-modal         (NEW — GATED/inactive; render only if BE wires TAKE_PRIZE_CARD)
└─ finished-screen        (status === FINISHED → winnerId + finishedReason)
```

## Data Flow

```
            POST /actions {type, …}                 GameEvent (WS)
 hand/field ───────────────────────► BE engine ──────────────────► board-container
   click                                  │                              │
     │                                    └── BoardStateDTO (REST) ───────┤
     ▼                                                                    ▼
 interactionMode ──► action-panel / modal-host ◄── phase-indicator / game-log / field counters
```

- Explicit action: select card (sets `interactionMode`) → click legal target → POST `ActionRequest`
  → BE emits events → re-render from fresh `BoardStateDTO`.
- Automatic phase: BE auto-runs → emits `PHASE_CHANGED` / `CARD_DRAWN` / status-tick events →
  FE updates counters, appends log, animates conditions (Phase 2); if KO sets `pendingSelection`,
  `modal-host` opens `pending-selection-modal`.

## Interaction Flows (per action)

| Action | Trigger | Request fields | Notes |
|--------|---------|----------------|-------|
| PLAY_BASIC_POKEMON | select basic in hand → click empty ACTIVE (if empty) or BENCH_n (<5) | `cardId` | mode `play-basic`; highlight valid slot(s) |
| EVOLVE_POKEMON | select evolution → click matching in-play target | `cardId`, `targetPosition` | target-picker-modal |
| ATTACH_ENERGY | select energy → click target | `cardId`, `targetPosition` | one/turn (BE-enforced) |
| ATTACH_TOOL | select tool → click target | `cardId`, `targetPosition` | — |
| RETREAT | retreat affordance on active → pick bench slot | `benchIndex` | mode `retreat` |
| USE_ATTACK | attack-modal on active | `attackIndex` | ATTACK phase |
| PLAY_ITEM / PLAY_SUPPORTER / PLAY_STADIUM | select trainer → play-trainer-modal → confirm | `cardId` | shows "effect pending" if unimplemented (card consumed) |
| END_TURN | action-panel button | none | enabled only in MAIN/ATTACK and `!pendingSelection` |
| CONCEDE | action-panel button | none | always enabled |
| RESOLVE_SELECTION | pending-selection-modal (post-KO) | `benchIndex` | type `CHOOSE_ACTIVE_ON_KO` |
| ~~TAKE_PRIZE_CARD~~ | — | `prizeIndex` | **GATED**: no BE handler; prizes auto-awarded |

## Automatic Phase Handling

| Phase | UI action | FE observes | FE reaction |
|-------|-----------|-------------|-------------|
| DRAW | none | `PHASE_CHANGED(DRAW)`, `CARD_DRAWN`, `DECK_OUT`, `PHASE_CHANGED(MAIN)` | silent deck−/hand+ counters, optional log "drew a card"; no modal |
| BETWEEN_TURNS | none | `PHASE_CHANGED(BETWEEN_TURNS)`, `STATUS_EFFECT_APPLIED/CLEARED`, `DAMAGE_DEALT`, `POKEMON_HEALED`, `POKEMON_KNOCKED_OUT`, `PRIZE_TAKEN` | log + condition animations (Phase 2); if KO → `pendingSelection` → modal |

> Event-name correction vs. brief: BE has **no `STATUS_EFFECT_TICK`**. Condition ticks surface as
> `STATUS_EFFECT_APPLIED` / `STATUS_EFFECT_CLEARED` plus `DAMAGE_DEALT` / `POKEMON_HEALED`.

## File Changes (FE — indicative)

| File | Action | Description |
|------|--------|-------------|
| `active-game/action-panel/*` | Create | Trainer-play buttons, END_TURN, CONCEDE, status line |
| `modal-host/play-trainer-modal/*` | Create | Confirm ITEM/SUPPORTER/STADIUM; "effect pending" note |
| `modal-host/take-prize-modal/*` | Create (behind flag) | Inactive until BE wires `TAKE_PRIZE_CARD` |
| `board-container` event handlers | Modify | Handle PHASE_CHANGED / CARD_DRAWN / status-tick / PRIZE_TAKEN |
| `interactionMode` machine | Modify | Add `play-basic` and `play-trainer` modes |

## Revised Slice Breakdown

| Slice | Scope |
|-------|-------|
| 0 | Zones (existing) — no changes |
| 1 | **Play Basic Pokémon** (new, simplest warmup) |
| 2 | Retreat (revised from old Slice 1) |
| 3 | Attack |
| 4 | Attach / Evolve |
| 5 | Play Trainers (ITEM/SUPPORTER/STADIUM, shared modal) |
| 6 | End Turn / Concede + observe DRAW & BETWEEN_TURNS |
| 7 | Take Prize Card — **deferred/gated** until BE wires the handler |
| 8 | SETUP phase with prizes (expand) |
| 9 | Pending Selection (CHOOSE_ACTIVE_ON_KO) |
| 10 | Phase 2: animations, 3D, optimistic patch |

## Open Questions

- [ ] Will BE wire `TAKE_PRIZE_CARD {prizeIndex}` (interactive prize pick), or keep auto-award? Slice 7 depends on this.
- [ ] Should automatic `CARD_DRAWN` show a transient log entry or stay fully silent?
- [ ] Are condition animations (Slice 10) driven purely by `DAMAGE_DEALT`/`POKEMON_HEALED`, or do we need richer payloads?
