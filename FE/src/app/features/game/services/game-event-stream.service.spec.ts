import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { GameEvent, GameEventStream } from './game-event-stream.service';
import { WebsocketService } from '../../../core/services/websocket.service';
import { WebsocketMessage } from '../../../core/services/websocket-source';

describe('GameEventStream', () => {
  let messages$: Subject<WebsocketMessage>;
  let stream: GameEventStream;

  beforeEach(() => {
    messages$ = new Subject<WebsocketMessage>();
    TestBed.configureTestingModule({
      providers: [
        GameEventStream,
        { provide: WebsocketService, useValue: { messages$ } },
      ],
    });
    stream = TestBed.inject(GameEventStream);
  });

  it('emits a typed event for an /events message', () => {
    const received: GameEvent[] = [];
    stream.events$.subscribe((event) => received.push(event));

    messages$.next({
      topic: '/topic/games/g1/events',
      payload: {
        gameId: 'g1',
        eventType: 'COIN_FLIPPED',
        payload: { result: 'HEADS', coinWinnerId: 7 },
        timestamp: 'now',
      },
    });

    expect(received.length).toBe(1);
    expect(received[0].type).toBe('COIN_FLIPPED');
    expect(received[0].gameId).toBe('g1');
    expect(received[0].payload['result']).toBe('HEADS');
    expect(received[0].payload['coinWinnerId']).toBe(7);
  });

  it('ignores /state-changed messages (truth reload, not an animation event)', () => {
    const received: GameEvent[] = [];
    stream.events$.subscribe((event) => received.push(event));

    messages$.next({
      topic: '/topic/games/g1/state-changed',
      payload: { gameId: 'g1', actionType: 'JOIN_GAME', status: 'SETUP' },
    });

    expect(received.length).toBe(0);
  });

  it('skips malformed /events payloads without an eventType', () => {
    const received: GameEvent[] = [];
    stream.events$.subscribe((event) => received.push(event));

    messages$.next({ topic: '/topic/games/g1/events', payload: { gameId: 'g1' } });

    expect(received.length).toBe(0);
  });
});
