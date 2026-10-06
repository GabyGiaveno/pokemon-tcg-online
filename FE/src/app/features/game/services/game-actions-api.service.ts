import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActionRequest, GameActionApiRequestDto, GameActionApiResponseDto } from '../models/game-actions.dto';
import { GameActionsSource } from './game-actions-source';

@Injectable({ providedIn: 'root' })
export class GameActionsApiService implements GameActionsSource {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/games';

  dispatchAction(gameId: string, actionRequest: ActionRequest): Observable<GameActionApiResponseDto> {
    return this.http.post<GameActionApiResponseDto>(`${this.baseUrl}/${gameId}/actions`, this.toApiRequest(actionRequest));
  }

  private toApiRequest(actionRequest: ActionRequest): GameActionApiRequestDto {
    const payload = this.isRecord(actionRequest.payload) ? actionRequest.payload : {};

    return {
      type: this.toBackendActionType(actionRequest.action),
      ...(payload as Record<string, unknown>),
    } as GameActionApiRequestDto;
  }

  private toBackendActionType(action: ActionRequest['action']): GameActionApiRequestDto['type'] {
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

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
  }
}
