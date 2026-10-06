# Design: Tablero Funcional (Game Board Redesign)

> Source artifacts: `sdd/tablero-funcional/proposal` + `sdd/tablero-funcional/design-decisions` (Q1–Q5).
> Status: design (architectural HOW). Tasks come next.

## Technical Approach

Evolve the EXISTING Angular 20 board feature (`FE/src/app/features/game/`) into a contract-driven,
slice-by-slice playable surface over the already-real BE pipeline (`BoardStateDTO` / `ActionRequest` /
`GameEvent`). We do NOT start greenfield: the repo already has a signal-based `GameStateService`, a smart
`Board` container, dumb zone components using `input()/output()`, STOMP WS wired to `/events` and
`/state-changed`, and `pendingSelection` modeled with owner discrimination. The design's job is to (a) add
the missing action-picker UIs (attack / target / bench), (b) introduce a transient **interaction** signal so
the board becomes a click-target surface, (c) add a **card-detail lookup** (prerequisite for attack metadata),
and (d) layer the design-system + animation intent per the user's "Design > Functionality" principle.

Per user vision: animation is central, text is minimal, cards are inspectable in 3D zoom, and the aesthetic is
coherent (single design system). Functionality lands first per slice; polish/3D is Slice 6 (out of current scope).

---

## 1. Architecture Overview (Component Tree)

Smart/dumb split. ONE smart component injects services; everything else is input-driven + emits output events.

```
board-container (SMART)                  ← injects GameStateService, GameActionsService, AuthTokenService
│  signals: board, isMyTurn, phase, pendingSelection, interaction
│  routes by status: WAITING | SETUP | ACTIVE | FINISHED
│
├─ waiting-screen            (dumb)      status === WAITING
├─ setup-phase               (dumb)      status === SETUP  (Slice 4)
│
├─ opponent-field            (dumb)      read-only, hidden zones as counts
│   ├─ active-zone (side=opponent)
│   ├─ bench-zone  (side=opponent)
│   ├─ discard-zone, deck-zone, prize-zone (counts/face-down)
│
├─ player-field              (dumb)      interactive
│   ├─ active-zone (side=player)         → (select) emits instanceId
│   ├─ bench-zone  (side=player)         → (selectSlot) emits benchIndex
│   ├─ discard-zone, deck-zone, prize-zone
│
├─ hand-zone                 (dumb)      → (select) emits instanceId; supports "select mode" highlight
├─ action-panel              (dumb)      shows only legal actions → (actionConfirmed) emits ActionRequest
├─ game-log / notifications  (dumb)
│
└─ modal-host                (dumb)      lifecycle owned by board-container
    ├─ card-zoom-modal       3D zoom inspect (Q3) — detail ON the card
    ├─ attack-modal          (Slice 2)  lists attacks (cost/damage) from active cardId
    ├─ target-picker-modal   (Slice 2)  only when attack is multi-target
    └─ pending-selection-modal (Slice 5) invasive blocker; owner-only
```

Dumb components NEVER inject `GameStateService`. They receive a projected view + emit intent. `board-container`
translates intent → `interaction` state or `dispatch(ActionRequest)`.

---

## 2. State Management (GameStateService — signals)

Builds on the existing root-provided service. The board's render input is the whole `BoardStateDTO`.

| Signal | Type | Source |
|--------|------|--------|
| `board()` | `BoardStateDTO \| null` | raw, authoritative (re-fetched) |
| `isMyTurn()` | `boolean` | derived: `board().isMyTurn` |
| `phase()` | `TurnPhase` | derived: SETUP/DRAW/MAIN/ATTACK/BETWEEN_TURNS |
| `status()` | `GameStatus` | derived: routes the screen |
| `pendingSelection()` | `PendingSelectionDTO \| null` | when non-null → LOCKED |
| `interaction()` | `Interaction` (transient) | "user is selecting target" state |

`Interaction` (transient, NOT from BE):
```ts
type InteractionMode = 'idle' | 'attack-select' | 'target-pick' | 'attach-target' | 'retreat-target';
interface Interaction {
  mode: InteractionMode;
  sourceInstanceId?: string;   // selected hand/active card (targeting uses instanceId)
  cardId?: string;             // for lookup / attach validation
  attackIndex?: number;        // chosen attack awaiting target
  pendingActionType?: BackendGameActionType;
}
```

Methods:
- `dispatch(ActionRequest)` — POST to BE; on success `refresh()`, on error surface to notifications, board unchanged.
- `refresh()` — re-fetch `BoardStateDTO` (authoritative; race-free vs optimistic patch).
- `setInteractionMode(mode, data)` — track in-progress selection; triggers zone highlight.
- `clearInteraction()` — reset to `idle` after dispatch or Escape.

**Key invariant (from proposal):** targeting always uses `instanceId`; display/lookup uses `cardId`.
`targetPosition` string is `"ACTIVE" | "BENCH_0".."BENCH_4"`. `attackIndex` is 0-based against the card's attack list.

---

## 3. Architecture Decisions (ADR)

| # | Decision | Alternatives rejected | Rationale |
|---|----------|----------------------|-----------|
| D1 | **Events = invalidation signals → `refresh()`** authoritative re-fetch | Optimistic patch / client-side reducer | Race-free, matches known-buggy BE (review 2026-06-11: energy-on-attack, lossy mapper). Eventual consistency is fine for a turn-based game. Optimistic patch deferred to Slice 6. |
| D2 | **Click → select → highlight valid targets → click target → dispatch** | Drag-and-drop | Most testable, least error-prone, works on touch. Q2 confirmed spatial/visual targeting: the board IS the target. DnD evaluated post-MVP. |
| D3 | **`interaction` signal in service** (not local component state) | Per-component local signals | Targeting spans multiple components (hand → zones); single source keeps highlight + dispatch consistent and unit-testable. |
| D4 | **Card-detail lookup service** keyed by `cardId` | Read attacks from board DTO | Board DTO carries `cardId` ONLY — attack name/cost/damage absent. Slice 2 PREREQUISITE. Lookup cached client-side. |
| D5 | **`pendingSelection` driven by `ownerPlayerId === myId`** | Drive by `isMyTurn` | Defender promotes after KO, so owner ≠ turn player. Already correctly modeled in existing `board.ts` — preserve it. |
| D6 | **Invasive blocking modal for pendingSelection** (Q4) | Toast + inline highlight | BE rejects all other actions while pending; a blocker prevents illegal clicks and animates attention. |
| D7 | **Smart/dumb, one container** | Multiple smart components | Single injection point; dumb zones stay reusable + smoke-testable. Matches existing pattern. |
| D8 | **Modal host owned by container** | Each modal self-manages | Centralized lifecycle; only one modal at a time; container coordinates with `interaction`. |
| D9 | **Card zoom = CSS 3D transform** (`@angular/animations` + transform3d), Phase-2 tilt | Heavy 3D libs (three.js) | Q3 wants 3D inspect with detail ON the card; CSS transforms cover zoom/tilt without bundle cost. |

---

## 4. Component Breakdown (Slice-by-Slice)

| Slice | Adds | Action dispatched |
|-------|------|-------------------|
| **0 Zones** | `board-container` routing + all `*-field`/zone/`card-instance` dumb components rendering full `BoardStateDTO` (opponent hidden zones as counts) | none |
| **1 Retreat** | `bench-zone` (player) click → `setInteractionMode('retreat-target', {benchIndex})`; `action-panel` "Retreat?" → confirm | `RETREAT { benchIndex }` |
| **2 Attack** | click active → `attack-modal` (zoom). Resolve attacks via **card-detail lookup** by active `cardId`; filter by attached energy; show cost/damage. Single-target → auto-target opponent active; multi → `target-picker-modal` | `USE_ATTACK { attackIndex }` |
| **3 Attach/Evolve** | `hand-zone` select mode → `setInteractionMode('attach-target', {sourceInstanceId, cardId})`; zones highlight valid targets (ACTIVE/BENCH by card type) → click zone | `ATTACH_ENERGY` / `ATTACH_TOOL` / `EVOLVE_POKEMON { cardId, targetPosition }` |
| **4 SETUP** | `setup-phase` special layout: pick active (must be basic), bench, prizes; "Ready" | `SETUP_PLACE_POKEMON` / `SETUP_SET_PRIZES` → `END_TURN` |
| **5 pendingSelection** | `pending-selection-modal`: renders only when `pendingSelection() && ownerPlayerId === myId`; blocks all else; shows prompt + valid bench options | `RESOLVE_SELECTION { benchIndex }` |
| **6 Polish (out of scope)** | animations, reflections, 3D tilt, optimistic patch | — |

---

## 5. Interaction Flow (Attack with targeting — canonical example)

```
1. User clicks active Pokémon (player-field emits select → instanceId)
2. board-container → setInteractionMode('attack-select', {sourceInstanceId, cardId})
3. attack-modal opens (zoom); attacks resolved via card-detail lookup(cardId), filtered by energy
4. User clicks attack #i
5. Is attack i multi-target?
     ├─ no  → auto-target opponent active
     └─ yes → target-picker-modal → user confirms target
6. board-container.dispatch(USE_ATTACK { attackIndex: i })
7. BE processes → GameEvent[] (DAMAGE_DEALT, maybe POKEMON_KNOCKED_OUT…)
8. WS event arrives on /events or /state-changed → GameStateService.refresh()
9. board() updates → zones re-render (damage/KO); clearInteraction()
```

---

## 6. State Diagram (interaction machine)

```
        ┌─────────────────────────── pendingSelection != null ─────────────────┐
        │                                                                       ▼
      IDLE ──(click active)──► ATTACK_SELECT ─(pick attack)─► [multi?] ─► TARGET_PICK ─┐
        │                                          │ single                            │
        │                                          └────────────► DISPATCHING ◄────────┘
        ├──(select hand card)─► ATTACH_TARGET ─(click zone)──► DISPATCHING
        ├──(click bench)──────► RETREAT_TARGET ─(confirm)────► DISPATCHING
        │                                                          │
        └────────────────────────◄── refresh() / clearInteraction ─┘

   LOCKED  (pendingSelection != null && owner === me): only RESOLVE_SELECTION allowed.
           Escape from any *_SELECT/*_TARGET → IDLE.
```

---

## 7. GameEvent Handling (real-time, Q5)

- WS already subscribes `/topic/games/{id}/events` and `/topic/games/{id}/state-changed` (STOMP, existing).
- On ANY event: (1) optionally surface a transient toast / append to game-log; (2) `GameStateService.refresh()`
  (authoritative re-fetch, D1); (3) re-render with a transient animation (damage float, KO red-flash).
- Debounce event bursts to avoid refetch storms.
- Opponent visibility (Q5): if BE exposes a `CARD_SELECTED`-style event, show transient "Rival está
  seleccionando…" state. If not exposed, degrade gracefully (no blocker).
- Consistency model is **eventual** (re-fetch), acceptable for a turn-based game.

---

## 8. Design System (base sketch — iterate per slice)

- **Layout**: TCG-Live mirrored field. Opponent top (active centered, bench row, prize/deck/discard flanks),
  player bottom mirrored, hand as a fanned row pinned to the bottom edge. Active = focal center.
- **Color**: accent glow border on active; neutral bench with hover tint; selected hand card highlighted;
  invalid targets desaturated (opacity .5); ALL opponent zones read-only. Palette TBD (TCG-Live reference).
- **Typography**: card names bold, readable at zoom (no sub-10px); attacks/costs smaller but clear; minimal
  chrome labels (phase / turn / player). Per principle: the card speaks for itself.
- **Animations (intent — Slice 6)**: zoom modal `scale(1.2)+fade ~300ms ease-out`; damage float-up fade ~1s;
  KO red flash + scale-down; phase/turn change subtle fade+slide ~150ms; hand hover lift `translateY(-4px)`+shadow.
- **Hover/focus**: hand lift, bench highlight border, disabled actions `cursor:not-allowed; opacity:.5`.

---

## 9. Accessibility & Edge Cases

- Keyboard: Tab through zones, Enter to select, Escape cancels interaction / closes modal.
- Touch: tap select → tap target confirm (no hover dependence).
- High contrast: borders/text legible on every background; never rely on color alone (add icon/label).
- Error: invalid action → notification, board stays in prior state (no partial updates; D1).
- Opponent field: zero click handlers, visual feedback only.

---

## 10. Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit (component smoke) | each zone renders correctly given inputs; opponent hidden zones project as counts | Jasmine/Karma `input()` harness |
| Unit (service) | `setInteractionMode`/`clearInteraction` transitions; `dispatch` success→refresh, error→notify | signal assertions |
| Interaction flow | click flow A → correct `ActionRequest` (right `instanceId`/`attackIndex`/`benchIndex`/`targetPosition`) | spy on `dispatch` |
| State sync | event received → `refresh()` called → board reflects new DTO | mock WS + state source |
| Modal | pending-selection shows ONLY for owner; blocks other actions | owner vs non-owner cases |
| A11y | keyboard nav + ARIA labels | DOM + key events |

Per-slice smoke spec + manual E2E checklist; verify each action against BE before marking a slice done
(known BE bugs per review 2026-06-11 can masquerade as FE bugs).

---

## File Changes (high-level — tasks phase details exact files)

| Path (under `FE/src/app/features/game/`) | Action | Description |
|------|--------|-------------|
| `board/board.ts` (→ board-container role) | Modify | host `interaction`, modal-host lifecycle, dispatch routing |
| `services/game-state.service.ts` | Modify | add `interaction` signal + `setInteractionMode/clearInteraction/dispatch/refresh` |
| `services/card-detail.service.ts` | Create | **D4** card-detail lookup (attacks by `cardId`), cached |
| `board/active-zone`, `bench-zone`, `hand-zone` | Modify | emit instanceId/benchIndex; select/highlight modes |
| `board/modals/{card-zoom,attack,target-picker,pending-selection}-modal` | Create | modal-host children |
| `board/setup-phase` | Create | Slice 4 SETUP layout |
| `models/card-detail.dto.ts` | Create | attack metadata shape for lookup |

---

## Open Questions / Prerequisites

- [ ] **Card-detail lookup endpoint/cache** must exist before Slice 2 (attack metadata absent from board DTO). Confirm BE source for attacks-by-cardId.
- [ ] `USE_ABILITY` flow not in blocker set — add after Slice 3 only if BE-stable.
- [ ] Exact color palette + animation easing constants — refine per slice (design iterates with implementation, per user approach).
- [ ] Confirm whether BE emits an opponent `CARD_SELECTED`-style event for Q5 "rival is selecting" visibility.
