import { Injectable, inject, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ConnectionStatus, WebsocketMessage, WebsocketSource } from './websocket-source';
import { AuthTokenService } from '../../features/auth/data-access/auth-token.service';

/**
 * Real STOMP over SockJS WebSocket implementation.
 *
 * Connects to the backend's /ws endpoint with the JWT token in the CONNECT
 * frame headers, subscribes to game event topics, and exposes incoming
 * messages via the `messages$` observable.
 */
@Injectable({ providedIn: 'root' })
export class WebsocketStompService implements WebsocketSource {
  private readonly authToken = inject(AuthTokenService);
  private readonly baseUrl = '';

  private readonly messagesSubject = new Subject<WebsocketMessage>();
  readonly messages$ = this.messagesSubject.asObservable();

  readonly connectionStatus = signal<ConnectionStatus>('disconnected');

  private client: Client | null = null;
  private currentGameId: string | null = null;
  private activeSubscriptions: Array<{ destination: string; unsubscribe: () => void }> = [];
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 5;

  connect(gameId: string): Observable<void> {
    this.currentGameId = gameId;
    // Do NOT reset reconnectAttempts here — that would break the max-reconnect
    // guard in attemptReconnect(). Reset only after a successful onConnect.
    const isReconnectAttempt = this.connectionStatus() === 'reconnecting'
      || this.reconnectAttempts > 0;

    const connect$ = new Subject<void>();

    if (this.client) {
      this.cleanupClient();
    }

    const token = this.authToken.value;
    this.connectionStatus.set(isReconnectAttempt ? 'reconnecting' : 'connecting');

    this.client = new Client({
      // El handshake HTTP de SockJS (/ws/info, /ws/.../xhr) no puede mandar headers desde
      // el browser: el JWT viaja como ?token= (el filtro del BE lo acepta solo en /ws/**).
      webSocketFactory: () => new SockJS(
        token ? `${this.baseUrl}/ws?token=${encodeURIComponent(token)}` : `${this.baseUrl}/ws`,
      ),
      connectHeaders: token
        ? { Authorization: `Bearer ${token}` }
        : {},
      debug: () => {
        /* keep quiet in production */
      },
      reconnectDelay: 0, // we handle manual reconnect
      onConnect: () => {
        this.connectionStatus.set('connected');
        this.reconnectAttempts = 0;
        this.subscribeToGameTopics(gameId);

        // If we just recovered after a failed reconnect, notify consumers
        // so they can re-sync game state (REST snapshot).
        if (isReconnectAttempt) {
          this.messagesSubject.next({ topic: 'system', payload: { type: 'reconnected' } });
        }

        connect$.next(void 0);
        connect$.complete();
      },
      onStompError: (frame) => {
        console.error('[WebsocketStompService] STOMP error:', frame.headers['message']);
        this.connectionStatus.set('error');
        connect$.error(new Error(frame.headers['message'] ?? 'STOMP error'));
        connect$.complete();
      },
      onWebSocketClose: () => {
        const status = this.connectionStatus();

        // Unexpected disconnect while connected → try to recover.
        if (status === 'connected') {
          this.connectionStatus.set('disconnected');
          this.attemptReconnect();
          return;
        }

        // Connection failed while (re)connecting → keep trying.
        if (status === 'connecting' || status === 'reconnecting') {
          connect$.error(new Error('WebSocket connection closed during handshake'));
          connect$.complete();
          this.attemptReconnect();
          return;
        }

        // In 'disconnected' or 'error' state the client was already cleaned up.
      },
    });

    this.client.activate();

    return connect$.asObservable();
  }

  disconnect(): void {
    this.currentGameId = null;
    this.unsubscribeAll();
    this.cleanupClient();
    this.connectionStatus.set('disconnected');
  }

  publish(message: WebsocketMessage): void {
    if (!this.client?.connected) {
      console.warn('[WebsocketStompService] Cannot publish: not connected');
      return;
    }

    const destination = this.toDestination(message.topic);
    this.client.publish({
      destination,
      body: typeof message.payload === 'string' ? message.payload : JSON.stringify(message.payload),
    });
  }

  reset(): void {
    this.disconnect();
    this.reconnectAttempts = 0;
    this.messagesSubject.next({ topic: 'system', payload: { type: 'reset' } });
  }

  private subscribeToGameTopics(gameId: string): void {
    const topics = [
      `/topic/games/${gameId}/events`,
      `/topic/games/${gameId}/state-changed`,
    ];

    for (const destination of topics) {
      if (!this.client) {
        return;
      }

      const subscription = this.client.subscribe(destination, (message: IMessage) => {
        let payload: unknown;
        try {
          payload = JSON.parse(message.body);
        } catch {
          payload = message.body;
        }

        this.messagesSubject.next({
          topic: destination,
          payload,
        });
      });

      this.activeSubscriptions.push({
        destination,
        unsubscribe: () => subscription.unsubscribe(),
      });
    }
  }

  private attemptReconnect(): void {
    if (!this.currentGameId || this.reconnectAttempts >= this.maxReconnectAttempts) {
      if (this.currentGameId) {
        console.warn('[WebsocketStompService] Max reconnect attempts reached');
        this.connectionStatus.set('error');
      }
      return;
    }

    this.reconnectAttempts++;
    this.connectionStatus.set('reconnecting');

    setTimeout(() => {
      if (this.currentGameId) {
        this.connect(this.currentGameId).subscribe({
          error: () => {
            // reconnect error is already handled in onStompError / onWebSocketClose
          },
        });
      }
    }, 1000 * this.reconnectAttempts);
  }

  private unsubscribeAll(): void {
    for (const sub of this.activeSubscriptions) {
      sub.unsubscribe();
    }
    this.activeSubscriptions = [];
  }

  private cleanupClient(): void {
    if (this.client) {
      try {
        this.client.deactivate();
      } catch {
        // ignore deactivate errors
      }
      this.client = null;
    }
  }

  /**
   * Convert a short topic name like "games/123/state-changed"
   * to a STOMP destination like "/topic/games/123/state-changed".
   */
  private toDestination(topic: string): string {
    if (topic.startsWith('/')) {
      return topic;
    }
    return `/topic/${topic}`;
  }
}
