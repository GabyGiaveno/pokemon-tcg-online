# Proposal: Board Complete Redesign — Production-Ready Game Board

## Intent

The FE game board renders zones correctly (board-rewrite Slice 0) but is **functionally broken** for most actions. `board-container.handleAction()` (lines 248-254) builds payloads with only `cardInstanceId` — it omits `targetPosition` (energy/evolve), `benchIndex` (retreat), and trainer subtype routing. The BE rejects these with validation errors. Backend is VERIFIED 100% complete (all action types in `MainPhaseState`, contract in `GameActionApiRequestDto`). Only working flows: CONCEDE, PLAY_BASIC, USE_ATTACK, SETUP. We close the FE gap AND deliver a production visual (Pokémon + Hearthstone, 3D table). Now: the engine is proven, so FE is the sole blocker to a playable game.

## Scope

### In Scope
- Visual rewrite: themed background, CSS 3D table (rotateX/preserve-3d), collapsible right action drawer, fan hand, hover/zone-highlight animations, responsive (desktop/tablet/mobile)
- Remove game-log UI entirely
- Target-picker modals (clone attack-modal pattern) for: RETREAT (benchIndex), ATTACH_ENERGY (card + targetPosition), EVOLVE (card + targetPosition), PLAY_TRAINER (ITEM/SUPPORTER/STADIUM routing)
- Complete-payload dispatch per action type before sending to BE
- Remove dead mock code from `game-state.service.ts` (`simulateAction`, `buildMockActions`, `decorateMockState`)

### Out of Scope
- BE changes (verified complete; only confirm trainer subtypes in CardInstanceDTO)
- TAKE_PRIZE manual picking (still BE-gated, prizes auto-awarded)
- New animation libs (CSS-only; vanilla-tilt optional)
- Optimistic client-side patching (state refresh after each action stays)

## Capabilities

### New Capabilities
- `board-visual`: 3D table layout, themed background, fan hand, action drawer, responsive breakpoints, no game-log
- `action-retreat`: bench-target picker → RETREAT with benchIndex
- `action-attach-energy`: hand-card + field-target combo → ATTACH_ENERGY with cardInstanceId + targetPosition
- `action-evolve`: evolution + field-target combo → EVOLVE_POKEMON with cardInstanceId + targetPosition
- `action-play-trainer`: trainer selector → PLAY_ITEM/PLAY_SUPPORTER/PLAY_STADIUM by subtype

### Modified Capabilities
- None at spec level (board-rewrite artifacts live in Engram, not openspec/specs)

## Approach

Replicate the PROVEN attack-modal pattern (`board-container.ts` 191-229): select card → open picker modal showing valid targets with highlighting → confirm → dispatch COMPLETE payload → toast + refresh. Keep board-container smart, zones dumb. Add one picker modal per action family. Build payloads matching `GameActionApiRequestDto`. Progressive slices: 0 Visual base → 1 Retreat → 2 Attach Energy → 3 Evolve → 4 Trainer → 5 Polish. Each slice = one chained PR, independently testable.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `board-container/board-container.*` | Modified | Replace generic handleAction; wire pickers; drawer |
| `board-container/board-container.css` | Modified | 3D perspective, themed bg, responsive |
| `attack-modal/` + new picker modals | New | Retreat/energy/evolve/trainer target pickers |
| `hand-zone/`, `player-field/`, zones | Modified | Fan layout, highlighting, 3D tilt |
| `game-log/` | Removed | Drop log UI |
| `services/game-state.service.ts` | Modified | Remove simulateAction/mock helpers |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Core gameplay regression | Med | Progressive slices; per-slice manual E2E; keep working flows untouched |
| Payload mismatch vs BE | Low | Contract verified in DTO; copy attack-modal dispatch shape |
| 3D CSS breaks responsive | Med | Breakpoint testing per device; perspective scoped to container |
| Trainer subtypes missing in DTO | Low | Confirm CardInstanceDTO.subtypes before Slice 4 |

## Rollback Plan

Each slice is one chained PR; revert that PR independently. Visual base (Slice 0) is isolated from action logic — a visual revert leaves functional flows intact, and vice versa. Old board already deleted in board-rewrite, so baseline is current Produccion.

## Dependencies

- Verified: BE all action types, `GameActionApiRequestDto` contract, attack-modal pattern, mock code present in game-state.service.ts
- Confirm before Slice 4: CardInstanceDTO.subtypes populated for trainer cards

## Success Criteria

- [ ] RETREAT, ATTACH_ENERGY, EVOLVE, PLAY_TRAINER dispatch complete payloads and BE accepts them
- [ ] 3D themed board renders responsively (desktop/tablet/mobile), drawer toggles, no game-log
- [ ] Existing flows (CONCEDE/PLAY_BASIC/ATTACK/SETUP) still work
- [ ] Mock code removed from game-state.service.ts; tsc clean; tests green
- [ ] Full game playable end-to-end against real BE

## Slices (apply order)

0. **Visual base** — Hearthstone layout, 3D perspective, drawer, no logs
1. **Retreat** — bench-target picker, dispatch benchIndex
2. **Attach Energy** — energy + target combo, dispatch cardInstanceId + targetPosition
3. **Evolve** — evolution + target combo
4. **Play Trainer** — trainer selector with subtype mapping
5. **Polish** — animations, final UX
