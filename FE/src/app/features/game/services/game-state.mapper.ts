import { BackendBoardStateDto } from '../models/game-api.dto';
import { CardDto, GameStateDto, PlayerStateDto, PokemonInPlayDto, StatusCondition } from '../models/game-state.dto';
import { resolveCardArt } from './card-art';
import { computeActionAvailability } from './action-availability.util';

export function mapBackendBoardStateToGameState(
  dto: BackendBoardStateDto,
  playerName: string,
  localPlayerId: number | null,
): GameStateDto {
  const isMyTurn = localPlayerId !== null && dto.currentPlayerId === localPlayerId;
  const phase = dto.phase ?? 'SETUP';

  return {
    gameId: dto.gameId,
    turn: isMyTurn ? 'PLAYER' : 'OPPONENT',
    phase,
    player: mapPlayerField(dto.myField, playerName),
    opponent: mapOpponentField(dto.opponentField),
    stadium: null,
    log: [],
    availableActions: computeActionAvailability(
      phase,
      isMyTurn,
      dto.myField.activePokemon !== null,
      dto.myField.bench.some((b) => b !== null),
      dto.opponentField.activePokemon !== null,
    ),
    status: dto.status ?? null,
    winnerId: dto.winnerId ?? null,
    finishedReason: dto.finishedReason ?? null,
    pendingSelection: dto.pendingSelection ?? null,
    myReady: dto.myReady ?? false,
    opponentReady: dto.opponentReady ?? false,
    coinFlipWinnerId: dto.coinFlipWinnerId ?? null,
    suddenDeathRound: dto.suddenDeathRound ?? 0,
  };
}

function mapPlayerField(field: BackendBoardStateDto['myField'], playerName: string): PlayerStateDto {
  // Defensive: in WAITING/SETUP the BE may send a field with empty/absent lists.
  const prizes = field.prizeCards ?? [];
  return {
    name: playerName,
    active: mapActive(field.activePokemon),
    bench: (field.bench ?? []).map((pokemon) => mapBench(pokemon)),
    hand: (field.hand ?? []).map((card) => mapCard(card)),
    deckCount: field.deckSize ?? 0,
    discardCount: (field.discardPile ?? []).length,
    prizesRemaining: prizes.length,
    prizesTaken: Math.max(0, 6 - prizes.length),
  };
}
function mapOpponentField(field: BackendBoardStateDto['opponentField']): PlayerStateDto {
  const prizes = field.prizeCards ?? [];
  return {
    name: 'Opponent',
    active: mapActive(field.activePokemon),
    bench: (field.bench ?? []).map((pokemon) => mapBench(pokemon)),
    hand: [],
    deckCount: field.deckSize ?? 0,
    discardCount: (field.discardPile ?? []).length,
    prizesRemaining: prizes.length,
    prizesTaken: Math.max(0, 6 - prizes.length),
  };
}

function mapActive(pokemon: BackendBoardStateDto['myField']['activePokemon']): PokemonInPlayDto | null {
  if (!pokemon) {
    return null;
  }

  const maxHp = pokemon.maxHp ?? pokemon.hp;
  return {
    cardId: pokemon.cardId,
    name: pokemon.cardId,
    imageUrl: resolveCardArt(pokemon.cardId),
    hp: pokemon.hp,
    maxHp,
    damage: maxHp - pokemon.hp,
    energies: pokemon.attachedEnergies.map(mapCard),
    tool: mapCard(pokemon.toolCard),
    status: (pokemon.conditions ?? []) as StatusCondition[],
  };
}

function mapBench(pokemon: BackendBoardStateDto['myField']['bench'][number]): PokemonInPlayDto {
  const maxHp = pokemon.maxHp ?? pokemon.hp;
  return {
    cardId: pokemon.cardId,
    name: pokemon.cardId,
    imageUrl: resolveCardArt(pokemon.cardId),
    hp: pokemon.hp,
    maxHp,
    damage: maxHp - pokemon.hp,
    energies: pokemon.attachedEnergies.map(mapCard),
    tool: null,
    status: [],
  };
}

function mapCard(card: { instanceId: string; cardId?: string; name: string; subtypes?: string[] } | null): CardDto {
  return {
    cardId: card?.cardId ?? card?.instanceId ?? 'unknown',
    name: card?.name ?? 'Unknown',
    imageUrl: resolveCardArt(card?.cardId ?? card?.instanceId ?? 'unknown'),
    types: [],
    subtypes: card?.subtypes,
  };
}
