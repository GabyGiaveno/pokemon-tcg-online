import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { GameActionApiResponseDto, ActionRequest } from '../models/game-actions.dto';

export interface GameActionsSource {
  dispatchAction(gameId: string, actionRequest: ActionRequest): Observable<GameActionApiResponseDto>;
}

export const GAME_ACTIONS_SOURCE = new InjectionToken<GameActionsSource>('GAME_ACTIONS_SOURCE');
