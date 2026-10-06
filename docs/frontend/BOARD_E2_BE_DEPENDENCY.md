# BE Dependency: Hand Card Subtypes for PLAY_BASIC_POKÉMON

**Status**: ✅ **COMPLETED** — BE now includes `subtypes` in hand cards; E2 ready for manual E2E

## Problem

The FE PLAY_BASIC_POKÉMON feature (Slice E2) classifies hand cards by `subtypes` to identify Basic Pokémon:

```typescript
// hand-zone.ts
isBasicPokemon(card: CardDto): boolean {
  return card.subtypes?.includes('Basic') ?? false;
}
```

**Currently**: `CardInstanceDTO` (hand cards in `BoardStateDTO.myField.hand`) does NOT include `subtypes`.

**Result**: FE can render the selection UI, but the `playBasicPokemon` action never triggers in live games (always returns `false` for all hand cards).

## Requirement

Add `subtypes: string[] | null` to `CardInstanceDTO` in the hand card mapping of `EngineStateMapper.java`:

```java
// BE/src/main/java/ar/edu/utn/frc/tup/piii/mappers/EngineStateMapper.java
// In method: mapCardToInstanceDTO(card, instanceId)

CardInstanceDTO dto = CardInstanceDTO.builder()
  .cardId(card.getId())
  .instanceId(instanceId)
  .name(card.getName())
  .subtypes(card.getSubtypes())  // ← ADD THIS
  .imageUrl(card.getImageUrl())
  .build();
```

## Files Affected

**FE** (already modified, Slice E2):
- `FE/src/app/features/game/models/game-state.dto.ts` — `CardDto` has optional `subtypes?: string[]`
- `FE/src/app/features/game/board/hand-zone/hand-zone.ts` — classification logic ready

**BE** (required):
- `BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/CardInstanceDTO.java` — add `subtypes` field
- `BE/src/main/java/ar/edu/utn/frc/tup/piii/mappers/EngineStateMapper.java` — populate in hand card mapping

## Test Plan (once BE is done)

Run **T2.8 Manual E2E**:
1. Start a game, advance to MAIN phase (isMyTurn = true)
2. Click a Basic Pokémon in hand → highlights (green outline)
3. Active slot and empty bench slots pulse green (target highlight)
4. Click a target slot → `PLAY_BASIC_POKÉMON` dispatches; board refreshes with card placed
5. Verify attack/retreat/other actions still work (no regressions)

## Links

- **FE Implementation**: `FE/src/app/features/game/board/` (E2 tasks T2.1–T2.7 ✅)
- **BE Source**: `BE/src/main/java/ar/edu/utn/frc/tup/piii/mappers/EngineStateMapper.java`
- **SDD Slice**: E2 — PLAY_BASIC_POKÉMON (part of tablero-funcional change)

## Unblocking Timeline

Once BE adds `subtypes` to hand card mapping → FE E2 feature is 100% functional (no FE changes needed).