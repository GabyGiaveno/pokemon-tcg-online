import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BackendBoardStateDto } from '../models/game-api.dto';

/**
 * HTTP client for the opening READY_CHECK phase: marking "Listo" and (for the coin-flip
 * winner) choosing who takes the first turn. The board reloads via the WebSocket
 * `state-changed` notification, so callers don't need to consume the response state.
 */
@Injectable({ providedIn: 'root' })
export class ReadyCheckApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/games';

  /** Marks the current player as ready. Backend triggers the coin flip once both are ready. */
  markReady(gameId: string): Observable<BackendBoardStateDto> {
    return this.http.post<BackendBoardStateDto>(`${this.baseUrl}/${gameId}/ready`, {});
  }

  /** Coin-flip winner chooses who starts ('ME' or 'OPPONENT'); backend advances to SETUP. */
  chooseFirst(gameId: string, starter: 'ME' | 'OPPONENT'): Observable<BackendBoardStateDto> {
    return this.http.post<BackendBoardStateDto>(`${this.baseUrl}/${gameId}/choose-first`, { starter });
  }
}
