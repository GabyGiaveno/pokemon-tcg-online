import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { WebsocketService } from '../../../core/services/websocket.service';

/**
 * A discrete, presentation-only game event coming from the backend
 * `/topic/games/{id}/events` STOMP channel.
 *
 * The backend `GameEventMessage` shape is `{ gameId, eventType, payload, timestamp }`;
 * this is the normalized FE view of it.
 */
export interface GameEvent {
  /** Event type name, e.g. `COIN_FLIPPED`, `CARD_DRAWN`, `MULLIGAN`, `TURN_ORDER_DECIDED`. */
  readonly type: string;
  /** Event data map (never carries private information such as opponent card ids). */
  readonly payload: Record<string, unknown>;
  readonly gameId: string;
}

/**
 * Consumes the backend `/events` channel and re-exposes it as a typed,
 * presentation-only stream of discrete game events.
 *
 * <p>The board still reloads authoritative state on `state-changed` (unchanged);
 * this stream drives transient ANIMATIONS only. It never derives or mutates game
 * state — the "frontend never calculates rules" invariant holds.
 */
@Injectable({ providedIn: 'root' })
export class GameEventStream {
  private readonly websocket = inject(WebsocketService);

  /** Discrete game events in arrival order, filtered to the `/events` topic. */
  readonly events$: Observable<GameEvent> = this.websocket.messages$.pipe(
    filter((message) => typeof message.topic === 'string' && message.topic.endsWith('/events')),
    map((message) => this.toGameEvent(message.payload)),
    filter((event): event is GameEvent => event !== null),
  );

  /** Normalizes a raw `/events` payload (`{ gameId, eventType, payload }`) into a `GameEvent`. */
  private toGameEvent(raw: unknown): GameEvent | null {
    if (typeof raw !== 'object' || raw === null) {
      return null;
    }
    const message = raw as { gameId?: unknown; eventType?: unknown; payload?: unknown };
    if (typeof message.eventType !== 'string') {
      return null;
    }
    return {
      type: message.eventType,
      gameId: typeof message.gameId === 'string' ? message.gameId : '',
      payload:
        typeof message.payload === 'object' && message.payload !== null
          ? (message.payload as Record<string, unknown>)
          : {},
    };
  }
}
