/** BE DTO: a single card instance within a game (hand, discard pile, prizes). */
export interface CardInstanceDto {
  instanceId: string;
  cardId: string;
  name: string;
  /** Subtipos PTCG (ej. "Basic", "Stage 1", "EX"). Usado para clasificar cartas jugables de la mano. */
  subtypes?: string[];
  /** Categoría PTCG: "Pokémon" | "Energy" | "Trainer". Distingue energía/entrenador de Pokémon. */
  supertype?: string;
}

/** BE DTO: active Pokémon on a player's field. */
export interface ActivePokemonDto {
  instanceId: string;
  cardId: string;
  hp: number;
  maxHp: number;
  attachedEnergies: CardInstanceDto[];
  toolCard: CardInstanceDto | null;
  conditions: string[];
}

/** BE DTO: benched Pokémon. */
export interface BenchPokemonDto {
  instanceId: string;
  cardId: string;
  hp: number;
  maxHp: number;
  attachedEnergies: CardInstanceDto[];
}

/** BE DTO: the authenticated player's field (full visibility). */
export interface PlayerFieldDto {
  activePokemon: ActivePokemonDto | null;
  bench: (BenchPokemonDto | null)[];
  hand: CardInstanceDto[];
  deckSize: number;
  prizeCards: (CardInstanceDto | null)[];
  discardPile: CardInstanceDto[];
}

/** BE DTO: opponent's field (hand/deck as counts only). */
export interface OpponentFieldDto {
  activePokemon: ActivePokemonDto | null;
  bench: (BenchPokemonDto | null)[];
  handSize: number;
  deckSize: number;
  prizeCards: (string | null)[];
  discardPile: CardInstanceDto[];
}

/** BE DTO: full board state filtered for the requesting player. */
export interface BoardStateDto {
  gameId: string;
  currentPlayerId: number | null;
  phase: 'SETUP' | 'DRAW' | 'MAIN' | 'ATTACK' | 'BETWEEN_TURNS' | null;
  turnNumber: number;
  isMyTurn: boolean;
  myField: PlayerFieldDto;
  opponentField: OpponentFieldDto;
}

/** BE DTO: action request sent to POST /api/games/{id}/actions. */
export interface GameActionApiRequest {
  type: string;
  cardInstanceId?: string;
  cardId?: string;
  targetPosition?: string;
  attackIndex?: number;
  benchIndex?: number;
  prizeIndex?: number;
}

/** BE DTO: action response from POST /api/games/{id}/actions. */
export interface GameActionApiResponse {
  success: boolean;
  actionType: string;
  events: GameEventDto[];
  error?: string;
}

/** BE DTO: a discrete game event. */
export interface GameEventDto {
  type: string;
  payload: unknown;
  timestamp: string;
}

/** BE DTO: state-changed WebSocket notification payload. */
export interface GameStateChangedMessage {
  gameId: string;
  actionType: string;
  status: string;
  timestamp: string;
}
