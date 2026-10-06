import { Injectable, signal } from '@angular/core';
import { Observable, Subject, of } from 'rxjs';
import { ConnectionStatus, WebsocketMessage, WebsocketSource } from './websocket-source';

@Injectable({ providedIn: 'root' })
export class WebsocketMockService implements WebsocketSource {
  private readonly messagesSubject = new Subject<WebsocketMessage>();
  readonly messages$ = this.messagesSubject.asObservable();
  readonly connectionStatus = signal<ConnectionStatus>('disconnected');

  connect(_gameId: string): Observable<void> {
    this.connectionStatus.set('connected');
    return of(void 0);
  }

  disconnect(): void {
    this.connectionStatus.set('disconnected');
    this.messagesSubject.next({ topic: 'system', payload: { type: 'disconnect' } });
  }

  publish(message: WebsocketMessage): void {
    this.messagesSubject.next(message);
  }

  reset(): void {
    this.connectionStatus.set('disconnected');
    this.messagesSubject.next({ topic: 'system', payload: { type: 'reset' } });
  }
}
