export interface GameStateDto {
  gameId: string;
  turn: 'PLAYER' | 'OPPONENT';
  phase: string;
  player: PlayerStateDto;
  opponent: PlayerStateDto;
  stadium: CardDto | null;
  log: GameLogEntryDto[];
  availableActions: ActionAvailabilityDto;
  /** READY_CHECK | SETUP | ACTIVE | FINISHED (opcional: los mocks no lo setean). */
  status?: string | null;
  winnerId?: number | null;
  finishedReason?: string | null;
  pendingSelection?: PendingSelectionDto | null;
  /** READY_CHECK: "Listo" de cada lado + ganador del coin flip (el que elige quién empieza). */
  myReady?: boolean;
  opponentReady?: boolean;
  coinFlipWinnerId?: number | null;
  /** Sudden Death round counter (0 = normal game, >=1 = sudden death round number). */
  suddenDeathRound?: number | null;
}

export interface PendingSelectionDto {
  type: string;
  ownerPlayerId: number;
  validOptions: string[];
  prompt?: string;
  /** For REORDER_DECK / SEARCH_DECK / CHOOSE_FROM_DISCARD: cardId for each instanceId in validOptions (same order). */
  revealedCardIds?: string[];
  selectionCount?: number;
}

export interface PlayerStateDto {
  name: string;
  active: PokemonInPlayDto | null;
  bench: (PokemonInPlayDto | null)[];
  hand: CardDto[];
  deckCount: number;
  discardCount: number;
  prizesRemaining: number;
  prizesTaken: number;
}

export interface PokemonInPlayDto {
  cardId: string;
  name: string;
  imageUrl?: string;
  hp: number;
  maxHp: number;
  damage: number;
  energies: CardDto[];
  tool: CardDto | null;
  status: StatusCondition[];
}

export interface CardDto {
  cardId: string;
  name: string;
  imageUrl?: string;
  types?: string[];
  /** Subtipos PTCG (ej. "Basic", "Stage 1", "EX"). Usado para clasificar cartas jugables de la mano. */
  subtypes?: string[];
}

export type StatusCondition = 'ASLEEP' | 'BURNED' | 'CONFUSED' | 'PARALYZED' | 'POISONED';

export interface GameLogEntryDto {
  id: string;
  timestamp: string;
  message: string;
  type: GameLogEntryType;
}

export type GameLogEntryType = 'ACTION' | 'DAMAGE' | 'HEAL' | 'EVOLVE' | 'ENERGY' | 'TRAINER' | 'STATUS' | 'KNOCKOUT' | 'PRIZE' | 'TURN' | 'SYSTEM';

export interface ActionState {
  enabled: boolean;
  reason?: string;
}

export interface ActionAvailabilityDto {
  attack: ActionState;
  retreat: ActionState;
  playEnergy: ActionState;
  playTrainer: ActionState;
  evolve: ActionState;
  endTurn: ActionState;
  concede?: ActionState;
  playBasicPokemon?: ActionState;
}

export type AvailableAction = 'attack' | 'retreat' | 'playEnergy' | 'playTrainer' | 'playItem' | 'playSupporter' | 'playStadium' | 'evolve' | 'endTurn' | 'setupPlacePokemon' | 'setupSetPrizes' | 'concede' | 'playBasicPokemon' | 'resolveSelection' | 'attachTool';
