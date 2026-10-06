import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { GameSessionResponse } from './lobby-api.types';

export interface LobbySource {
  getWaitingGames(): Observable<GameSessionResponse[]>;
  createGame(deckId: number): Observable<GameSessionResponse>;
  joinGame(gameId: string, deckId: number): Observable<GameSessionResponse>;
}

export const LOBBY_SOURCE = new InjectionToken<LobbySource>('LOBBY_SOURCE');
