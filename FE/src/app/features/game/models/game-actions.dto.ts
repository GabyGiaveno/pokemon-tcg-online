import { AvailableAction } from './game-state.dto';

export type BackendGameActionType =
  | 'USE_ATTACK'
  | 'PLAY_BASIC_POKEMON'
  | 'RESOLVE_SELECTION'
  | 'RETREAT'
  | 'ATTACH_ENERGY'
  | 'ATTACH_TOOL'
  | 'EVOLVE_POKEMON'
  | 'PLAY_ITEM'
  | 'PLAY_SUPPORTER'
  | 'PLAY_STADIUM'
  | 'END_TURN'
  | 'CONCEDE'
  | 'DRAW_CARD'
  | 'SETUP_PLACE_POKEMON'
  | 'SETUP_SET_PRIZES';

export interface GameActionApiRequestDto {
  type: BackendGameActionType;
  cardInstanceId?: string;
  cardId?: string;
  targetPosition?: string;
  attackIndex?: number;
  benchIndex?: number;
  prizeIndex?: number;
  orderedInstanceIds?: string[];
}

export interface GameActionApiEventDto {
  type: string;
  payload: unknown;
  timestamp: string;
}

export interface GameActionApiResponseDto {
  success: boolean;
  actionType: BackendGameActionType;
  events: GameActionApiEventDto[];
  error?: string | null;
}

export interface ActionRequest {
  action: AvailableAction;
  payload?: unknown;
}
