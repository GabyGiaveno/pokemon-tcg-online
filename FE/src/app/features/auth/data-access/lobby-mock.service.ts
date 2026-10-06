import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { AuthTokenService } from './auth-token.service';
import { GameSessionResponse } from './lobby-api.types';
import { LobbySource } from './lobby-source';

@Injectable({ providedIn: 'root' })
export class LobbyMockService implements LobbySource {
  private readonly storageKey = 'mock_lobby_games';

  constructor(private readonly authToken: AuthTokenService) {}

  getWaitingGames(): Observable<GameSessionResponse[]> {
    return of(this.readGames().filter((game) => game.status === 'WAITING'));
  }

  createGame(deckId: number): Observable<GameSessionResponse> {
    const game = this.buildGame(deckId, 'WAITING');
    const games = [game, ...this.readGames()];
    this.saveGames(games);
    return of(game);
  }

  joinGame(gameId: string, deckId: number): Observable<GameSessionResponse> {
    const games = this.readGames();
    const index = games.findIndex((game) => game.gameId === gameId);
    if (index === -1) {
      const joined = this.buildGame(deckId, 'ACTIVE', gameId);
      this.saveGames([joined, ...games]);
      return of(joined);
    }

    const updated = {
      ...games[index],
      status: 'ACTIVE' as const,
      player2Username: this.authToken.usernameValue ?? 'Jugador 2',
    };
    games[index] = updated;
    this.saveGames(games);
    return of(updated);
  }

  private readGames(): GameSessionResponse[] {
    const raw = localStorage.getItem(this.storageKey);
    if (raw) {
      try {
        const games = JSON.parse(raw) as GameSessionResponse[];
        if (Array.isArray(games) && games.length > 0) {
          return games;
        }
      } catch {
        // fall through to seed data
      }
    }

    const seedGames = this.buildSeedGames();
    this.saveGames(seedGames);
    return seedGames;
  }

  private saveGames(games: GameSessionResponse[]): void {
    localStorage.setItem(this.storageKey, JSON.stringify(games));
  }

  private buildGame(deckId: number, status: GameSessionResponse['status'], gameId?: string): GameSessionResponse {
    const currentUser = this.authToken.usernameValue ?? 'Entrenador';
    const id = gameId ?? `mock-game-${Date.now()}`;
    return {
      gameId: id,
      status,
      player1Username: currentUser,
      player2Username: status === 'ACTIVE' ? 'Bot' : null,
      createdAt: new Date().toISOString(),
      prizeCardsCount: deckId === 1 ? 1 : 6,
    };
  }

  private buildSeedGames(): GameSessionResponse[] {
    return [
      this.buildSeedGame('blue-arena', 'Misty', 'WAITING'),
      this.buildSeedGame('red-rush', 'Brock', 'WAITING'),
      this.buildSeedGame('late-endgame', 'Gary', 'WAITING'),
    ];
  }

  private buildSeedGame(gameId: string, player1Username: string, status: GameSessionResponse['status']): GameSessionResponse {
    return {
      gameId,
      status,
      player1Username,
      player2Username: status === 'ACTIVE' ? 'Bot' : null,
      createdAt: new Date().toISOString(),
      prizeCardsCount: 6,
    };
  }
}
