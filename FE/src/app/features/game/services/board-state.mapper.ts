import {
  BackendActivePokemonDto,
  BackendBenchPokemonDto,
  BackendBoardStateDto,
  BackendCardInstanceDto,
} from '../models/game-api.dto';
import {
  ActivePokemonDto,
  BenchPokemonDto,
  BoardStateDto,
  CardInstanceDto,
  OpponentFieldDto,
  PlayerFieldDto,
} from '../models/board-state.dto';

const VALID_PHASES = ['SETUP', 'DRAW', 'MAIN', 'ATTACK', 'BETWEEN_TURNS'] as const;

/** Maps the raw backend board-state response into the FE `BoardStateDto` used by the board UI. */
export function mapBackendBoardStateToBoardState(
  dto: BackendBoardStateDto,
  localPlayerId: number | null,
): BoardStateDto {
  const isMyTurn = localPlayerId !== null && dto.currentPlayerId === localPlayerId;
  const phase = VALID_PHASES.includes(dto.phase as typeof VALID_PHASES[number])
    ? (dto.phase as BoardStateDto['phase'])
    : null;

  return {
    gameId: dto.gameId,
    currentPlayerId: dto.currentPlayerId,
    phase,
    turnNumber: dto.turnNumber,
    isMyTurn,
    myField: mapPlayerField(dto.myField),
    opponentField: mapOpponentField(dto.opponentField),
  };
}

function mapPlayerField(field: BackendBoardStateDto['myField']): PlayerFieldDto {
  // Defensive: in WAITING/SETUP the BE may send a field with empty/absent lists.
  return {
    activePokemon: mapActivePokemon(field.activePokemon),
    bench: (field.bench ?? []).map(mapBenchPokemon),
    hand: (field.hand ?? []).map(mapCardInstance),
    deckSize: field.deckSize ?? 0,
    prizeCards: (field.prizeCards ?? []).map(mapCardInstance),
    discardPile: (field.discardPile ?? []).map(mapCardInstance),
  };
}

function mapOpponentField(field: BackendBoardStateDto['opponentField']): OpponentFieldDto {
  return {
    activePokemon: mapActivePokemon(field.activePokemon),
    bench: (field.bench ?? []).map(mapBenchPokemon),
    handSize: field.handSize ?? 0,
    deckSize: field.deckSize ?? 0,
    prizeCards: field.prizeCards ?? [],
    discardPile: (field.discardPile ?? []).map(mapCardInstance),
  };
}

function mapActivePokemon(pokemon: BackendActivePokemonDto | null): ActivePokemonDto | null {
  if (!pokemon) {
    return null;
  }

  return {
    instanceId: pokemon.instanceId,
    cardId: pokemon.cardId,
    hp: pokemon.hp,
    maxHp: pokemon.maxHp ?? pokemon.hp,
    attachedEnergies: (pokemon.attachedEnergies ?? []).map(mapCardInstance),
    toolCard: pokemon.toolCard ? mapCardInstance(pokemon.toolCard) : null,
    conditions: pokemon.conditions ?? [],
  };
}

function mapBenchPokemon(pokemon: BackendBenchPokemonDto | null): BenchPokemonDto | null {
  if (!pokemon) {
    return null;
  }

  return {
    instanceId: pokemon.instanceId,
    cardId: pokemon.cardId,
    hp: pokemon.hp,
    maxHp: pokemon.maxHp ?? pokemon.hp,
    attachedEnergies: (pokemon.attachedEnergies ?? []).map(mapCardInstance),
  };
}

function mapCardInstance(card: BackendCardInstanceDto): CardInstanceDto {
  return {
    instanceId: card.instanceId,
    cardId: card.cardId,
    name: card.name,
    subtypes: card.subtypes,
    supertype: card.supertype,
  };
}
