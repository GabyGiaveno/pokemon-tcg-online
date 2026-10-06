import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GameSessionResponse, CreateGameRequest, JoinGameRequest } from './lobby-api.types';
import { LobbySource } from './lobby-source';

@Injectable({ providedIn: 'root' })
export class LobbyHttpService implements LobbySource {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/games';

  getWaitingGames(): Observable<GameSessionResponse[]> {
    return this.http.get<GameSessionResponse[]>(this.baseUrl);
  }

  createGame(deckId: number): Observable<GameSessionResponse> {
    const request: CreateGameRequest = { deckId };
    return this.http.post<GameSessionResponse>(this.baseUrl, request);
  }

  joinGame(gameId: string, deckId: number): Observable<GameSessionResponse> {
    const request: JoinGameRequest = { deckId };
    return this.http.post<GameSessionResponse>(`${this.baseUrl}/${gameId}/join`, request);
  }
}
