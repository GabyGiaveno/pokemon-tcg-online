import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { GameSessionResponse } from './lobby-api.types';
import { LOBBY_SOURCE } from './lobby-source';

@Injectable({ providedIn: 'root' })
export class LobbyApiService {
  private readonly source = inject(LOBBY_SOURCE);

  getWaitingGames(): Observable<GameSessionResponse[]> {
    return this.source.getWaitingGames();
  }

  createGame(deckId: number): Observable<GameSessionResponse> {
    return this.source.createGame(deckId);
  }

  joinGame(gameId: string, deckId: number): Observable<GameSessionResponse> {
    return this.source.joinGame(gameId, deckId);
  }
}
