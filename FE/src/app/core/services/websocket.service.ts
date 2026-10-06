import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { WebsocketMessage, WEBSOCKET_SOURCE } from './websocket-source';

/**
 * Facade over the injected WEBSOCKET_SOURCE (real STOMP or mock).
 */
@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private readonly source = inject(WEBSOCKET_SOURCE);

  readonly messages$ = this.source.messages$;
  readonly connectionStatus = this.source.connectionStatus;

  connect(gameId: string): Observable<void> {
    return this.source.connect(gameId);
  }

  disconnect(): void {
    this.source.disconnect();
  }

  publish(message: WebsocketMessage): void {
    this.source.publish(message);
  }

  reset(): void {
    this.source.reset();
  }
}
