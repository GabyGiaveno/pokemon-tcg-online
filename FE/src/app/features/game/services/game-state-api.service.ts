import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BackendBoardStateDto } from '../models/game-api.dto';
import { GameStateSource } from './game-state-source';

@Injectable({ providedIn: 'root' })
export class GameStateApiService implements GameStateSource {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/games';

  getBoardState(gameId: string): Observable<BackendBoardStateDto> {
    return this.http.get<BackendBoardStateDto>(`${this.baseUrl}/${gameId}/state`);
  }
}
