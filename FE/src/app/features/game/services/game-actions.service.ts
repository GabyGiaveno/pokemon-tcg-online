import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ActionRequest, GameActionApiResponseDto } from '../models/game-actions.dto';
import { GAME_ACTIONS_SOURCE } from './game-actions-source';

@Injectable({ providedIn: 'root' })
export class GameActionsService {
  private readonly source = inject(GAME_ACTIONS_SOURCE);

  dispatchAction(gameId: string, actionRequest: ActionRequest): Observable<GameActionApiResponseDto> {
    return this.source.dispatchAction(gameId, actionRequest);
  }
}
