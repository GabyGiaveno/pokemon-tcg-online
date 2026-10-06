import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendBoardStateDto } from '../models/game-api.dto';

export interface GameStateSource {
  getBoardState(gameId: string): Observable<BackendBoardStateDto>;
}

export const GAME_STATE_SOURCE = new InjectionToken<GameStateSource>('GAME_STATE_SOURCE');
