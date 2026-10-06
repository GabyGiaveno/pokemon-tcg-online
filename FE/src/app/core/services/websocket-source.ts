import { InjectionToken, Signal } from '@angular/core';
import { Observable } from 'rxjs';

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'reconnecting' | 'error';

export interface WebsocketMessage {
  topic: string;
  payload: unknown;
}

export interface WebsocketSource {
  readonly messages$: Observable<WebsocketMessage>;
  readonly connectionStatus: Signal<ConnectionStatus>;
  connect(gameId: string): Observable<void>;
  disconnect(): void;
  publish(message: WebsocketMessage): void;
  reset(): void;
}

export const WEBSOCKET_SOURCE = new InjectionToken<WebsocketSource>('WEBSOCKET_SOURCE');
