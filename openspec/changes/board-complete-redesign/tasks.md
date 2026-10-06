# Tasks: Board Complete Redesign — From-Scratch Rewrite

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~700-1000 (Slice 0 alone) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | Slice 0 split into 0A (card+art fix+overlay) -> 0B (PlayerZone+rotation) -> 0C (board-container rewrite+deletions+CSS+responsive) |
| Delivery strategy | chained PRs, one per slice (per user instruction) |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 0A | card-art fix + new `card` component + `card-inspection-overlay` | PR-0A | Base: Produccion. Standalone, testable in isolation via Storybook-less manual render. |
| 0B | New `PlayerZone` dumb component (both perspectives) + rotation | PR-0B | Base: PR-0A merged. Uses `card` from 0A. |
| 0C | `board-container` rewrite, deletions, CSS theme, responsive, E2E | PR-0C | Base: PR-0B merged. Wires everything together. |
| 1-5 | Retreat / Energy / Evolve / Trainer / Polish | PR-1..5 | Future slices, not detailed here. |

## Slice 0 — Board Rewrite

### Phase 0.0: Card Art Fix (foundation, blocks everything visual)

- [x] 0.0.1 Rewrite `FE/src/app/features/game/services/card-art.ts`: `resolveCardArt(cardId)` splits on LAST `-`, returns `https://images.pokemontcg.io/{setId}/{number}.png`; remove random-hash pool logic
- [x] 0.0.2 Manual test: call `resolveCardArt('xy1-1')`, `resolveCardArt('sm115-1')`, `resolveCardArt('base1-4')` → verify correct CDN URLs

### Phase 0.1: New `card` Component (PR-0A)

- [x] 0.1.1 Create `FE/src/app/features/game/card/card.ts`: inputs (`card`, `cardId`, `faceDown`, `size`, `selected`, `playable`, `hp`, `conditions`, `fanIndex`, `fanTotal`), output `cardClick`, `protected artUrl = computed(...)` via fixed `resolveCardArt`
- [x] 0.1.2 Create `FE/src/app/features/game/card/card.html`: img with `(error)` handler, face-down back asset, size-variant classes, HP/condition badges, `appVanillaTilt` + `appCardGlow` directives on face-up
- [x] 0.1.3 Create `FE/src/app/features/game/card/card.css`: `aspect-ratio: 2.5/3.5`, size variants (active ~100-120px, bench ~72px, hand fan, pile thumbnail), `.image-broken` fallback style, glow `::before` layer
- [x] 0.1.4 Manual test: render `card` with real `cardInstanceDto`, verify image matches card name, broken-image fallback shows placeholder, hover glow/tilt works

### Phase 0.2: `card-inspection-overlay` (PR-0A)

- [x] 0.2.1 Create `FE/src/app/features/game/card-inspection-overlay/card-inspection-overlay.ts`: inputs (`card`, `mode`, `attacks`, `abilities`, `energyCount`), outputs (`attackSelected`, `closed`)
- [x] 0.2.2 Create `card-inspection-overlay.html`: copy Pokédex `.card-fullscreen-backdrop`/`.fullscreen-card-shell`/`.fullscreen-close` structure; `inspect` mode = card + read-only text; `inspect-attack` mode = card + attack/ability buttons
- [x] 0.2.3 Create `card-inspection-overlay.css`: copy Pokédex fullscreen blur/scanline/gold-glow/`fadeIn` blocks verbatim, 5:7 shell, `appVanillaTilt` on shell
- [x] 0.2.4 Delete `FE/src/app/features/game/attack-modal/` (all files: ts/html/css/spec if present)
- [x] 0.2.5 Manual test: open overlay in `inspect` mode (read-only) and `inspect-attack` mode (buttons render, click emits `attackSelected(index)`), ESC/backdrop/× emits `closed`

### Phase 0.3: New `PlayerZone` Component (PR-0B)

- [ ] 0.3.1 Create `FE/src/app/features/game/player-zone/player-zone.ts`: inputs (`perspective`, `field`, `playerName`, `prizesLeft`, `isMyTurn`, `interactionMode`, `selectedCardId`), outputs (`cardClicked`, `zoneClicked`)
- [ ] 0.3.2 Create `player-zone.html`: active slot, 5 bench slots, 6 prize slots, deck pile, discard pile — all using `card` component; hand rendered only if `perspective==='player'` (fan), opponent shows backs+count badge
- [ ] 0.3.3 Create `player-zone.css`: `.player-zone--opponent { transform: rotate(180deg); }`, ornate slot frames, empty-slot dotted Pokéball outline, fan-hand layout for player perspective
- [ ] 0.3.4 Manual test: render `PlayerZone` twice (player + opponent perspective) with mock `PlayerFieldDto`/`OpponentFieldDto`, verify rotation applies only to opponent and clicks on rotated cards still emit correct `cardClicked` event with right card data

### Phase 0.4: `board-container` Rewrite (PR-0C)

- [ ] 0.4.1 Rewrite `board-container.html`: compose `<app-player-zone perspective="opponent">`, Pokéball center motif, `<app-player-zone perspective="player">`, `<app-card-inspection-overlay>`, keep `phase-indicator`/`action-panel`/`notifications`/`waiting-screen`
- [ ] 0.4.2 Rewrite `board-container.ts`: preserve all existing signals/computeds (`board`, `playerField`, `opponentField`, `phase`, `turnNumber`, `isMyTurn`, `hand`, `interactionMode`, `selectedCard`, `pendingSelection`, `gameFinished`, `waiting`, `iWon`, `inSetup`, `errorMessages`, `drawerOpen`); preserve WS reload + `refreshState`/`pushError`/`toggleDrawer`
- [ ] 0.4.3 Add `handleCardClicked(e: CardClickEvent)` in `board-container.ts`: own-active → resolve attacks via `CardDetailService` then open overlay `inspect-attack`; hand card in SETUP/MAIN → existing selection flow OR overlay `inspect`; bench/opponent card → overlay `inspect`
- [ ] 0.4.4 Rename `openAttackModal` → `openInspectActive`; add `handleAttackSelected` listening to overlay's `attackSelected` output, dispatching `{ action:'attack', payload:{ attackIndex } }` (same as before), close overlay + `refreshState()` on success, `pushError` on failure
- [ ] 0.4.5 Preserve `handleZoneClicked`, `placeBasic`, `playBasicPokemon`, `confirmSetup`, `resolveSelection`, `concede` dispatch flows unchanged (verify action strings → wire types via `game-actions-api.service.ts#toBackendActionType` untouched)

### Phase 0.5: Delete Old Components (PR-0C)

- [ ] 0.5.1 Delete `FE/src/app/features/game/opponent-field/` (all files)
- [ ] 0.5.2 Delete `FE/src/app/features/game/player-field/` (all files)
- [ ] 0.5.3 Delete `FE/src/app/features/game/active-zone/` (all files)
- [ ] 0.5.4 Delete `FE/src/app/features/game/bench-zone/` (all files)
- [ ] 0.5.5 Delete `FE/src/app/features/game/other-zones/` (all files)
- [ ] 0.5.6 Delete `FE/src/app/features/game/hand-zone/` (all files, including `card-instance.ts`)
- [ ] 0.5.7 Grep remaining references to deleted components/selectors across `FE/src/app/features/game/**` and remove stale imports/declarations

### Phase 0.6: CSS Theme (PR-0C)

- [ ] 0.6.1 `board-container.css`: wooden frame (48-64px, `radial-gradient`/`box-shadow`, `#3b1f0a`→`#6b3a1f`, gold corner ornaments `#c9a84c`/`#f0d060`)
- [ ] 0.6.2 `board-container.css`: green play surface (`#1a472a`→`#0d2b18`), opponent half red tint `rgba(180,30,30,0.15)`, player half blue tint `rgba(20,60,140,0.15)`
- [ ] 0.6.3 `board-container.css`: Pokéball center motif (CSS pseudo-element/SVG, `pointer-events:none`, low opacity) at midline
- [ ] 0.6.4 `player-zone.css`: active-card pulsing glow (`box-shadow: 0 0 20px 6px rgba(255,220,0,0.6)`, 2s infinite), gold frame for player active / silver for opponent active
- [ ] 0.6.5 `player-zone.css`: fan-hand panel (`rgba(0,0,0,0.5)` bg, gold border, per-card `rotate()`+`translateY()` keyed off `fanIndex/fanTotal`, hover `scale(1.08)` + raised z-index + drop-shadow, 150ms)
- [ ] 0.6.6 Add CSS comments marking particle canvas (`#particle-effects`) and combat-animation keyframes as TODO for polish slice; keep canvas in DOM inert

### Phase 0.7: Responsive (PR-0C)

- [ ] 0.7.1 Desktop (>1024px): full two-half board, both fans + side zones visible, active cards 100-120px
- [ ] 0.7.2 Tablet (768-1024px): frame ~32px, prizes/deck/discard collapse to compact stacks with count badges, fan hand reduced visible count + scroll-overflow
- [ ] 0.7.3 Mobile (<768px): opponent half compresses to status strip (active+counts), player half full, fan hand becomes horizontal scroll strip, overlay shell `min(88vw, 23rem)`, action drawer toggle preserved

### Phase 0.8: Manual E2E Verification (PR-0C)

- [ ] 0.8.1 With `APP_DATA_MODE='api'` and real BE running: board structure renders (frame, both PlayerZones, Pokéball center, hand)
- [ ] 0.8.2 Card art matches card name across active/bench/hand/prizes/deck/discard for both players
- [ ] 0.8.3 Click any card opens `card-inspection-overlay`; own active opens `inspect-attack` mode with working attack buttons dispatching `USE_ATTACK`
- [ ] 0.8.4 Verify preserved BE flows: play-basic (E2), setup placement (E8), KO-promotion resolveSelection (E9), concede, WS `state-changed` reload, `pendingSelection` banners
- [ ] 0.8.5 No console errors at desktop/tablet/mobile widths

## Future Slices (not detailed — separate change cycles)

- Slice 1: Retreat (bench-target picker)  ← IN PROGRESS
- Slice 2: Attach Energy
- Slice 3: Evolve
- Slice 4: Play Trainer
- Slice 5: Polish — particle canvas + combat animations (attack lunge / KO / damage float / energy-fly)
- **Slice 6: Deep FE polish (assisted, MANDATORY)** — a thorough, hands-on polishing pass over the
  whole frontend with the user iterating on each detail: visual fidelity vs TCG_BOARD.md, spacing/sizing,
  card art + name correctness, animations, responsive at every breakpoint, micro-interactions, and a11y.
  This is an explicit requirement: the board is a final-product surface, not a template — it must be
  refined to production quality with the user in the loop, not shipped at "good enough".
  - [ ] 6.dead-code: Delete orphaned legacy adapter `FE/.../game/services/game-board-adapter.service.ts`
    (GameBoardAdapterService — adapts BE BoardStateDto → legacy GameStateDto; nothing injects it, board
    now uses board-state.mapper.ts + game-state.mapper.ts; its javadoc documents stale BE limitations
    like "no maxHp" that no longer hold). Also remove the legacy `GameStateDto`/related types in
    `models/game-state.dto.ts` IF only this adapter references them (grep first).

> Note: many Phase 0.6/0.7 items above were addressed live during interactive iteration (theme tokens
> moved to global :root, click-safe 3D tilt, fixed bench/prize slots, waiting-screen restyle, WAITING
> null-field + CORS fixes). Slice 6 is the consolidated deep-polish pass that supersedes the remaining
> cosmetic items once functionality (Slices 1-4) is complete.
