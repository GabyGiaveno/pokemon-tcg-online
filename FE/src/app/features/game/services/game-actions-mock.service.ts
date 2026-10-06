import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { AuthTokenService } from '../../auth/data-access/auth-token.service';
import { ActionRequest, GameActionApiResponseDto } from '../models/game-actions.dto';
import { GameActionsSource } from './game-actions-source';
import { WebsocketService } from '../../../core/services/websocket.service';

@Injectable({ providedIn: 'root' })
export class GameActionsMockService implements GameActionsSource {
  private readonly authToken = inject(AuthTokenService);
  private readonly websocket = inject(WebsocketService);

  dispatchAction(gameId: string, actionRequest: ActionRequest): Observable<GameActionApiResponseDto> {
    this.websocket.publish({
      topic: `games/${gameId}/state-changed`,
      payload: {
        action: actionRequest.action,
        player: this.authToken.usernameValue ?? 'Entrenador',
      },
    });

    return of({
      success: true,
      actionType: this.toBackendActionType(actionRequest.action),
      events: [this.createEvent(gameId, actionRequest.action)],
      error: null,
    });
  }

  private toBackendActionType(action: ActionRequest['action']): GameActionApiResponseDto['actionType'] {
    switch (action) {
      case 'attack': return 'USE_ATTACK';
      case 'playBasicPokemon': return 'PLAY_BASIC_POKEMON';
      case 'resolveSelection': return 'RESOLVE_SELECTION';
      case 'retreat': return 'RETREAT';
      case 'playEnergy': return 'ATTACH_ENERGY';
      case 'playTrainer': return 'PLAY_SUPPORTER';
      case 'playItem': return 'PLAY_ITEM';
      case 'playSupporter': return 'PLAY_SUPPORTER';
      case 'playStadium': return 'PLAY_STADIUM';
      case 'evolve': return 'EVOLVE_POKEMON';
      case 'endTurn': return 'END_TURN';
      case 'setupPlacePokemon': return 'SETUP_PLACE_POKEMON';
      case 'setupSetPrizes': return 'SETUP_SET_PRIZES';
      case 'concede': return 'CONCEDE';
      case 'attachTool': return 'ATTACH_TOOL';
      default: throw new Error(`Unsupported action: ${action}`);
    }
  }

  private createEvent(gameId: string, action: ActionRequest['action']) {
    const timestamp = new Date().toISOString();
    const player = this.authToken.usernameValue ?? 'Entrenador';

    return {
      type: this.toEventType(action),
      payload: {
        gameId,
        player,
        action,
        message: 'Acción simulada ejecutada.',
      },
      timestamp,
    };
  }

  private toEventType(action: ActionRequest['action']): string {
    switch (action) {
      case 'attack': return 'DAMAGE';
      case 'retreat': return 'ACTION';
      case 'playEnergy': return 'ENERGY';
      case 'playTrainer': return 'TRAINER';
      case 'playItem': return 'TRAINER';
      case 'playSupporter': return 'TRAINER';
      case 'playStadium': return 'TRAINER';
      case 'evolve': return 'EVOLVE';
      case 'endTurn': return 'TURN';
      case 'setupPlacePokemon': return 'SETUP';
      case 'setupSetPrizes': return 'SETUP';
      case 'concede': return 'SYSTEM';
      case 'playBasicPokemon': return 'PLAY';
      case 'resolveSelection': return 'SELECTION';
      case 'attachTool': return 'TOOL';
      default: throw new Error(`Unsupported action: ${action}`);
    }
  }
}
