export interface BackendBoardStateDto {
  gameId: string;
  currentPlayerId: number;
  phase: string;
  turnNumber: number;
  myField: BackendPlayerFieldDto;
  opponentField: BackendOpponentFieldDto;
  /** READY_CHECK | SETUP | ACTIVE | FINISHED — lifecycle de la partida. */
  status?: string | null;
  winnerId?: number | null;
  finishedReason?: string | null;
  /** Elección pendiente del engine (ej. promoción post-KO). */
  pendingSelection?: BackendPendingSelectionDto | null;
  /** READY_CHECK: estado del "Listo" de cada lado y ganador del coin flip (quién elige). */
  myReady?: boolean;
  opponentReady?: boolean;
  coinFlipWinnerId?: number | null;
  /** Sudden Death: increments with each Sudden Death round (0 = no sudden death yet). */
  suddenDeathRound?: number | null;
}

export interface BackendPendingSelectionDto {
  type: string;
  ownerPlayerId: number;
  validOptions: string[];
  prompt?: string;
  revealedCardIds?: string[];
  selectionCount?: number;
}

export interface BackendPlayerFieldDto {
  activePokemon: BackendActivePokemonDto | null;
  bench: BackendBenchPokemonDto[];
  hand: BackendCardInstanceDto[];
  deckSize: number;
  prizeCards: BackendCardInstanceDto[];
  discardPile: BackendCardInstanceDto[];
}

export interface BackendOpponentFieldDto {
  activePokemon: BackendActivePokemonDto | null;
  bench: BackendBenchPokemonDto[];
  handSize: number;
  deckSize: number;
  prizeCards: (string | null)[];
  discardPile: BackendCardInstanceDto[];
}

export interface BackendActivePokemonDto {
  instanceId: string;
  cardId: string;
  hp: number;
  maxHp?: number;
  attachedEnergies: BackendCardInstanceDto[];
  toolCard: BackendCardInstanceDto | null;
  conditions: string[];
}

export interface BackendBenchPokemonDto {
  instanceId: string;
  cardId: string;
  hp: number;
  maxHp?: number;
  attachedEnergies: BackendCardInstanceDto[];
}

export interface BackendCardInstanceDto {
  instanceId: string;
  cardId: string;
  name: string;
  /** Subtipos PTCG (ej. "Basic"). Opcional: no todos los endpoints lo completan aún. */
  subtypes?: string[];
  /** Categoría PTCG: "Pokémon" | "Energy" | "Trainer". Para clasificar cartas de la mano. */
  supertype?: string;
}
