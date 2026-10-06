# Design: Board Complete Redesign — From-Scratch Rewrite (Slice 0)

> This design SUPERSEDES the previous "adapt existing components" design. The user
> rejected the adaptation approach. We DELETE the current zone component tree and
> REBUILD it from scratch to match `TCG_BOARD.md` (wooden frame, Pokéball center,
> green surface, ornate zones, fan hand, 2.5:3.5 cards, rival rotated 180°). The
> smart `board-container` is rewritten but PRESERVES its real BE dispatch logic.

## Technical Approach

One reusable dumb `PlayerZone` component is instantiated TWICE inside a rewritten
smart `board-container`: once with `perspective: 'player'` (bottom half, full
visibility, fan hand) and once with `perspective: 'opponent'` (top half, rotated
180°, hand shown as backs + count). This is `TCG_BOARD.md` implementation note 1
taken literally. Every card slot in every zone renders the SAME new reusable
`card` component at a fixed 2.5:3.5 ratio; `card` resolves its image through the
FIXED `resolveCardArt(cardId)` (deterministic pokemontcg.io URL) and carries the
`appVanillaTilt` + `appCardGlow` directives for the dynamic glow.

The current `attack-modal` is DELETED and replaced by a single fullscreen
`card-inspection-overlay` that reuses the Pokédex inspection CSS (blur backdrop,
gold glow, scanlines, 5:7 card). Clicking ANY card opens it. For the player's own
active Pokémon it additionally renders attacks/abilities as selectable buttons
that dispatch `USE_ATTACK` directly — same dispatch the old `attack-modal` did,
new shell.

Scope is Slice 0 only: frame, zones, fan hand, glow/hover, Pokéball center, the
inspection overlay, and the card-art bug fix. Particle canvas and combat
animations (attack lunge / KO / damage float / energy-fly) are DEFERRED to a
later polish slice. The BE contract, the action-string → wire-type translation
(`game-actions-api.service.ts#toBackendActionType`), and the mapper/DTOs are
UNCHANGED.

## Architecture Decisions (ADR-style)

### ADR-1: Single reusable `PlayerZone` instantiated twice (player + opponent)
- **Decision**: One dumb `PlayerZone` component with a `perspective` input drives
  both halves. Delete `opponent-field` and `player-field` as separate components.
- **Rationale**: `TCG_BOARD.md` note 1 mandates "una sola mitad = un componente
  reutilizable, instanciado dos veces". A single component guarantees structural
  symmetry, halves the CSS surface, and makes the 180° rival rotation a one-line
  transform instead of a duplicated mirror layout.
- **Rejected**: Keep `player-field` + `opponent-field` as twins (current state).
  Rejected because the two drifted apart, duplicate slot markup, and the previous
  "adapt" design the user rejected was built on top of exactly this split.

### ADR-2: Rival half rotated 180° via CSS `transform: rotate(180deg)`
- **Decision**: The opponent `PlayerZone` instance gets `transform: rotate(180deg)`
  on its root. Cards inside are NOT counter-rotated — they face the player, which
  is the authentic tabletop view (rival's cards upside-down toward me).
- **Rationale**: Matches `TCG_BOARD.md` ("Misma estructura, rotada 180°"). Pointer
  events survive rotation: CSS rotation transforms the hit-box with the element, so
  clicks still land on the visually-correct slot. The overlay (which must read
  upright) is rendered at `board-container` level OUTSIDE the rotated subtree, so
  inspected cards are never upside-down. See ADR-6 for the hit-test caveat.
- **Rejected**: JS-mirrored layout (manually swap rows for opponent). Rejected as
  brittle and divergent from note 1's single-component intent.

### ADR-3: Fullscreen inspection overlay replaces `attack-modal`
- **Decision**: Delete `attack-modal`. Add ONE `card-inspection-overlay` that
  serves two modes via an input: `inspect` (hand/bench/any card — read-only) and
  `inspect-attack` (own active — shows attacks/abilities as dispatch buttons).
- **Rationale**: `TCG_BOARD.md` "Animacion esperada" requires hand/bench/active
  cards to be inspectable AND the active to fire attacks FROM the zoomed card, with
  the Pokédex inspection animation + the card glow. One overlay with a mode switch
  is simpler than two components and reuses the Pokédex CSS verbatim.
- **Rejected**: Keep `attack-modal` for attacks + a separate inspect modal.
  Rejected — two overlays, duplicated backdrop/scanline CSS, and the user asked for
  attack selection to live ON the inspected card.

### ADR-4: Deterministic `resolveCardArt(cardId)` (BUG FIX)
- **Decision**: Replace the random-hash-against-12-image pool with a deterministic
  URL: split `cardId` on the LAST dash → `{setId}` + `{number}` →
  `https://images.pokemontcg.io/{setId}/{number}.png`. Provide a placeholder
  fallback for broken images (handled in `card` via `(error)`).
- **Rationale**: `cardId` is `{setId}-{number}` (e.g. `xy1-1`, and set ids can
  contain digits/dashes such as `sm115` or `base1`). The current hash picks a
  RANDOM image, so the art NEVER matches the card name — the root visual bug. The
  pokemontcg.io CDN serves `/{setId}/{number}.png` deterministically.
- **Rejected**: Bundle local art. Rejected — 1000s of cards, no asset pipeline in
  scope, CDN is already the source the old pool pointed at.

### ADR-5: New single `card` component used by every zone
- **Decision**: One `card` component renders active/bench/hand/prize/deck/discard
  slots via a `size` variant input. Delete `card-instance`. `card` owns
  `resolveCardArt`, `appVanillaTilt`, `appCardGlow`, face-down rendering, and the
  broken-image fallback.
- **Rationale**: `TCG_BOARD.md` note 4 — cards are ALWAYS 2.5:3.5 in every zone. A
  single component enforces ratio and art resolution in one place and keeps zones
  dumb.
- **Rejected**: Per-zone bespoke card markup. Rejected — ratio/art drift, the exact
  failure mode of the current code.

### ADR-6: Overlay rendered outside the rotated subtree; click bubbles up
- **Decision**: `PlayerZone` emits `cardClicked` / `zoneClicked`; the overlay is
  opened by `board-container` and rendered at the container root (z-index above the
  board, NOT inside the rotated opponent half).
- **Rationale**: Keeps inspected cards upright regardless of which half was clicked,
  and avoids `pointer-events` ambiguity from nested transformed stacking contexts.
  The only hit-test caveat with `rotate(180deg)` is that fan-hand overlap order
  must be authored so the rotated opponent fan still picks the intended card; we
  sidestep it entirely because the opponent hand is backs-only (no per-card click).

## Component Tree (NEW)

```
board-container  (SMART — rewritten, keeps BE dispatch logic)
├── phase-indicator                         (KEPT, unchanged)
├── action-panel  (drawer)                  (KEPT, unchanged)
├── notifications / waiting-screen          (KEPT, unchanged)
├── PlayerZone [perspective='opponent']     (NEW, rotated 180°)
│   ├── card (active slot)                  (NEW reusable)
│   ├── card ×5 (bench, face-up)            (NEW reusable)
│   ├── card ×6 (prizes, face-down)
│   ├── card (deck, face-down + count)
│   ├── card (discard, top face-up + count)
│   └── opponent hand: card ×handSize (backs only, count badge)
├── stadium  (shared center slot)           (NEW, optional render)
├── Pokéball center motif                   (CSS pseudo-element / SVG)
├── PlayerZone [perspective='player']       (NEW)
│   ├── card (active slot)
│   ├── card ×5 (bench, face-up)
│   ├── card ×6 (prizes, face-down)
│   ├── card (deck, face-down + count)
│   ├── card (discard, top face-up + count)
│   └── player hand: fan of card (face-up, selectable)
└── card-inspection-overlay                 (NEW — replaces attack-modal)
```

### Files DELETED (rebuilt from scratch)
- `FE/src/app/features/game/opponent-field/` (component + html + css + spec)
- `FE/src/app/features/game/player-field/` (component + html + css + spec)
- `FE/src/app/features/game/active-zone/` (component + html + css)
- `FE/src/app/features/game/bench-zone/` (component + html + css)
- `FE/src/app/features/game/other-zones/` (component + html + css)
- `FE/src/app/features/game/hand-zone/` (component + html + css + spec)
- `FE/src/app/features/game/hand-zone/card-instance.*` (replaced by new `card`)
- `FE/src/app/features/game/attack-modal/` (replaced by `card-inspection-overlay`)

### Files NEW
- `FE/src/app/features/game/player-zone/player-zone.{ts,html,css}`
- `FE/src/app/features/game/card/card.{ts,html,css}`
- `FE/src/app/features/game/card-inspection-overlay/card-inspection-overlay.{ts,html,css}`
- (optional) `FE/src/app/features/game/stadium/stadium.{ts,html,css}` — may be a
  slot inside `board-container.html` if trivial; promote to component only if markup grows.

### Files MODIFIED
- `FE/src/app/features/game/board-container/board-container.{ts,html,css}` — rewritten shell, BE dispatch preserved.
- `FE/src/app/features/game/services/card-art.ts` — deterministic URL fix.
- `FE/src/app/features/game/phase-indicator/`, `action-panel/`, `notifications/`,
  `waiting-screen/` — UNCHANGED (imported as-is).

## Component Contracts

### PlayerZone (dumb)
```ts
// inputs
perspective:   input.required<'player' | 'opponent'>();
field:         input.required<PlayerFieldDto | OpponentFieldDto | null>();
playerName:    input<string>('');
prizesLeft:    input<number>(6);
isMyTurn:      input<boolean>(false);
interactionMode: input<InteractionMode>('idle');  // for slot highlighting
selectedCardId:  input<string | null>(null);       // highlight selected hand card
// outputs
cardClicked = output<CardClickEvent>();   // { card, origin: 'active'|'bench'|'hand'|'prize'|'discard'|'deck', index? }
zoneClicked = output<ZoneClickEvent>();    // { type: 'active'|'bench', index? } — placement targets
```
- `perspective === 'opponent'`: root host gets class `player-zone--opponent`
  whose CSS applies `transform: rotate(180deg)`. Hand is rendered as
  `handSize` face-down backs + a count badge; bench/active still face-up (rival
  Pokémon are public). `PlayerFieldDto` vs `OpponentFieldDto` is discriminated by
  `perspective` (player has `hand: CardInstanceDto[]`, opponent has `handSize: number`).
- `perspective === 'player'`: full-visibility fan hand, selectable; clicking a hand
  card emits `cardClicked{origin:'hand'}`.
- **Rotation + hit-testing**: rotation is a CSS transform on the root only. Child
  click handlers fire on the (rotated) element the user actually sees, so hit-boxes
  stay correct. No `pointer-events` override needed because opponent hand cards are
  non-interactive backs. The overlay opens at container level (upright), so a
  clicked opponent card is shown right-side-up.

### card (dumb, reusable everywhere)
```ts
// inputs
card:      input<CardInstanceDto | null>(null);   // null => empty slot outline
cardId:    input<string | null>(null);            // for prize/deck face-down where only id is known
faceDown:  input<boolean>(false);
size:      input<'active' | 'bench' | 'hand' | 'prize' | 'pile'>('bench');
selected:  input<boolean>(false);
playable:  input<boolean>(false);                  // gold playable ring (hand basics in MAIN)
hp:        input<number | null>(null);             // active/bench overlay
conditions: input<string[]>([]);                   // status badges (ZZZ/PAR/BRN/PSN/CFZ)
fanIndex:  input<number>(0);
fanTotal:  input<number>(1);
// output
cardClick = output<CardInstanceDto>();
```
- Image: `protected artUrl = computed(() => resolveCardArt(this.card()?.cardId ?? this.cardId() ?? ''))`.
- Face-down: render the card-back asset instead of `artUrl`; ignore glow/tilt for backs.
- Broken image: `<img (error)="...">` toggles an `image-broken` class → CSS shows a
  name/placeholder fallback (same pattern as Pokédex `image-fallback`).
- Directives: `appVanillaTilt` (3D tilt + glare) and `appCardGlow` (cursor-tracked
  radial via `--glow-x/--glow-y`) on face-up cards. `size` maps to fixed widths from
  `TCG_BOARD.md` (active ~100–120px, bench ~72px, hand fan, pile thumbnails), all at
  ratio 2.5:3.5 via `aspect-ratio: 2.5 / 3.5`.

### card-inspection-overlay (dumb — replaces attack-modal)
```ts
// inputs
card:        input.required<CardInstanceDto>();   // the inspected card
mode:        input<'inspect' | 'inspect-attack'>('inspect');
attacks:     input<AttackDetailDto[]>([]);         // populated only in inspect-attack
abilities:   input<{ name: string; text: string }[]>([]); // optional, shown read-only
energyCount: input<number>(0);                     // attached energy on active (cost gating display)
// outputs
attackSelected = output<number>();                 // attackIndex → board-container dispatches USE_ATTACK
closed         = output<void>();
```
- **Reuses Pokédex CSS verbatim**: `.card-fullscreen-backdrop` (blur + scanlines +
  gold radial via `::before`), `.fullscreen-card-shell` (5:7, gold border, glow),
  `.fullscreen-close`, `fadeIn` keyframe, `appVanillaTilt` on the shell. Copy the
  relevant blocks from `pokedex/.../card-detail-panel.css` into the overlay css (or
  lift them to a shared partial — see CSS architecture).
- **inspect mode** (hand / bench / any non-own-active card): show big card + name +
  any read-only attack/ability text. No buttons. Backdrop/ESC/× → `closed`.
- **inspect-attack mode** (player's own active): below the big card, render each
  attack as a selectable button (name, cost pips, damage) and abilities as labeled
  buttons; clicking an attack emits `attackSelected(index)`. Energy gating is
  display-only in Slice 0 (BE remains the source of truth for legality; a rejected
  attack surfaces via `notifications`, exactly as today).
- **USE_ATTACK wiring**: `board-container` listens to `attackSelected` and dispatches
  the IDENTICAL flow the old `attack-modal` used:
  `gameActions.dispatchAction(gameId, { action: 'attack', payload: { attackIndex } })`
  → on success close overlay + `refreshState()`, on failure `pushError(...)`. The
  attack list is still resolved via `CardDetailService.getCardDetail(active.cardId)`
  before opening (unchanged from `openAttackModal()`).

### board-container (smart — rewritten shell, BE logic preserved)
- Keeps ALL existing signals/computeds: `board`, `playerField`, `opponentField`,
  `phase`, `turnNumber`, `isMyTurn`, `hand`, `interactionMode`, `selectedCard`,
  `pendingSelection`, `myPendingSelection`, `opponentPendingSelection`,
  `gameFinished`, `waiting`, `iWon`, `inSetup`, `errorMessages`, `drawerOpen`.
- Keeps WebSocket connect/reload (`state-changed` → `loadGameState`), `refreshState`
  (300ms debounce reload), `pushError`, `dismissNotification`, `toggleDrawer`.
- Replaces `<app-opponent-field>` / `<app-player-field>` / `<app-hand-zone>` with two
  `<app-player-zone>` and the overlay. `handleZoneClicked`, `handleCardSelected`,
  `placeBasic`, `playBasicPokemon`, `confirmSetup`, `resolveSelection`, `concede`,
  `handleAttackSelected`, `openAttackModal` (renamed `openInspectActive`) are PRESERVED.
- New: `handleCardClicked(e: CardClickEvent)` — routes: own-active → resolve attacks
  then open overlay in `inspect-attack`; hand card in SETUP/MAIN → existing
  selection flow OR open overlay in `inspect` (inspect-only when not a placement
  target); bench/opponent card → overlay `inspect`.

## CSS Architecture (mapped to TCG_BOARD.md palette)

- **Wooden frame**: `board-container` root — thick `48–64px` border simulated with
  layered `radial-gradient` + inner `box-shadow`, palette `#3b1f0a → #6b3a1f`, gold
  ornaments `#c9a84c / #f0d060` in the 4 corners (energy-type medallions as CSS).
- **Green play surface**: `#1a472a → #0d2b18` radial, felt texture. Opponent half
  tinted `rgba(180,30,30,0.15)` (red), player half `rgba(20,60,140,0.15)` (blue).
- **Pokéball center**: pseudo-element / inline SVG at the mid-line between halves,
  `pointer-events:none`, low opacity relief.
- **Ornate slots**: bench/active slots are rounded-rect/oval frames with thin gold
  ornament borders; empty slots use `rgba(255,255,255,0.2)` dotted outline + faint
  Pokéball icon (`TCG_BOARD.md` §3–§4).
- **Active glow**: pulsing `box-shadow: 0 0 20px 6px rgba(255,220,0,0.6)` (2s
  infinite) on the active card when it's that player's turn; gold frame for player,
  silver for opponent.
- **Fan hand panel**: bottom strip, `rgba(0,0,0,0.5)` panel + thin gold border;
  cards overlap via `transform: rotate()` + `translateY()` keyed off `fanIndex/fanTotal`;
  hover lifts (`scale(1.08)`, raised z-index, drop-shadow, 150ms).
- **Card glow**: `.card::before` radial-gradient reading `--glow-x/--glow-y`
  (already wired in `styles.css` via `appCardGlow`).
- **Keyframes**: reuse `fadeIn` and the active-glow pulse. **Combat animations
  (attack lunge, KO, damage float, energy-fly) and the particle canvas are
  DEFERRED** — reference them in CSS comments as TODO for the polish slice; the
  `#particle-effects` canvas stays in the DOM but inert (no draw loop) this slice.
- **Shared partial**: extract the Pokédex fullscreen blocks
  (`.card-fullscreen-backdrop`, `.fullscreen-card-shell`, `.fullscreen-close`,
  scanline `::before`, `fadeIn`) into a shared css the overlay imports, OR copy them
  into the overlay css. Prefer copy in Slice 0 to avoid touching the Pokédex; mark a
  follow-up to deduplicate.

## Real BE Wiring — Preserved Unchanged (Slice 0)

These dispatch flows carry over verbatim (same action strings → same wire types via
`game-actions-api.service.ts#toBackendActionType`):

| Flow | Action string | Wire type | Source method (preserved) |
|---|---|---|---|
| Play basic (MAIN, E2) | `playBasicPokemon` | `PLAY_BASIC_POKEMON` | `playBasicPokemon()` |
| Setup placement (E8) | `setupPlacePokemon` | `SETUP_PLACE_POKEMON` | `placeBasic()` |
| Confirm setup | `endTurn` | `END_TURN` | `confirmSetup()` |
| KO promotion (E9) | `resolveSelection` | `RESOLVE_SELECTION` | `resolveSelection()` |
| Attack (E3) | `attack` | `USE_ATTACK` | `handleAttackSelected()` (now fired from overlay) |
| Concede | `concede` | `CONCEDE` | `concede()` |

`pendingSelection` (own vs opponent) banners and the WebSocket `state-changed`
reload are preserved exactly.

**NOT in this rewrite** (their own later slices): `RETREAT` (benchIndex),
`ATTACH_ENERGY` (cardInstanceId + targetPosition), `EVOLVE_POKEMON`,
`PLAY_ITEM/PLAY_SUPPORTER/PLAY_STADIUM` (trainer subtype routing). The current
`toBackendActionType` already maps stubs for `retreat`/`playEnergy`/`evolve`/
`playTrainer`; we leave them untouched and do NOT wire UI for them here.

## Responsive (TCG_BOARD.md)

- **Desktop (>1024px)**: full two-half board inside the wooden frame; both fans and
  all side zones (prizes column, deck, discard) visible; active cards 100–120px.
- **Tablet (768–1024px)**: frame thinner (~32px); side zones (prizes/deck/discard)
  collapse to compact stacks with count badges; fan hand reduced card count visible,
  scroll-overflow for the rest.
- **Mobile (<768px)**: vertical priority — opponent half compressed to a status
  strip (active + counts), player half full; fan hand becomes a horizontally
  scrollable strip; overlay uses `min(88vw, 23rem)` shell (Pokédex mobile rule).
  Action drawer (`action-panel`) already collapses; keep its toggle.

## Open Questions / Risks

- **APP_DATA_MODE='api'**: this board is API-only; a real BE must be running.
  Mock/offline paths are out of scope — verify `game-state.service` resolves to the
  API source before manual E2E.
- **pokemontcg.io image availability**: deterministic URLs assume every `cardId` set
  exists on the CDN. Some custom/local set ids may 404 → fallback placeholder MUST
  render cleanly (covered by `card` broken-image handling). Risk: silent gaps if a
  set id mismatches the CDN's naming; mitigate by logging first-load errors.
- **`number` segment parsing**: split on the LAST dash so multi-dash ids (rare) and
  numeric set ids (`base1`, `sm115`) resolve correctly; assumes BE `cardId` always
  follows `{setId}-{number}`. Validate against a sample of real game card ids.
- **180° rotation + pointer-events**: low risk because opponent hand is
  non-interactive backs and the overlay renders upright at container level; still,
  manually verify clicks on rotated opponent active/bench land on the right card.
- **hires vs normal image weight**: `.png` (normal) chosen over `_hires.png` to keep
  the board light; the overlay could opt into hires for the zoomed view — deferred
  decision, default to normal everywhere in Slice 0.
- **Stadium**: BE board DTO has no stadium field today; render the slot as a
  decorative empty center unless/until the DTO exposes it. Flagged, not blocking.

## Out of Scope (this slice)

Particle canvas draw loop; combat animations (attack lunge / KO / damage float /
energy-fly / draw-card slide); retreat / attach-energy / evolve / play-trainer UI;
optimistic client patching; BE changes; TAKE_PRIZE manual picking.
