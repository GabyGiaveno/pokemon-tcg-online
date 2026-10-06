export type GameStatus = 'WAITING' | 'SETUP' | 'ACTIVE' | 'FINISHED';

export interface GameSessionResponse {
  gameId: string;
  status: GameStatus;
  player1Username: string;
  player2Username: string | null;
  createdAt: string;
  prizeCardsCount: number;
}

export interface CreateGameRequest {
  deckId: number;
}

export interface JoinGameRequest {
  deckId: number;
}
