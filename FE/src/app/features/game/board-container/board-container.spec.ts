import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { of, Subject } from 'rxjs';
import { BoardContainer } from './board-container';
import { GAME_STATE_SOURCE } from '../services/game-state-source';
import { GAME_ACTIONS_SOURCE } from '../services/game-actions-source';
import { WEBSOCKET_SOURCE, WebsocketMessage } from '../../../core/services/websocket-source';
import { APP_DATA_MODE } from '../../../core/tokens/app-data-mode';
import { BackendBoardStateDto } from '../models/game-api.dto';

function buildBoardState(): BackendBoardStateDto {
  return {
    gameId: 'game-1',
    currentPlayerId: 1,
    phase: 'MAIN',
    turnNumber: 3,
    status: 'ACTIVE',
    winnerId: null,
    finishedReason: null,
    pendingSelection: null,
    myField: {
      activePokemon: {
        instanceId: 'my-active-1',
        cardId: 'pikachu',
        hp: 60,
        attachedEnergies: [{ instanceId: 'energy-1', cardId: 'energy-basic', name: 'Basic Energy' }],
        toolCard: null,
        conditions: [],
      },
      bench: [
        { instanceId: 'my-bench-1', cardId: 'bulbasaur', hp: 70, attachedEnergies: [] },
      ],
      hand: [
        { instanceId: 'hand-1', cardId: 'charmander', name: 'Charmander', supertype: 'Pokémon', subtypes: ['Basic'] },
        { instanceId: 'hand-2', cardId: 'stage1card', name: 'Ivysaur', supertype: 'Pokémon', subtypes: ['Stage 1'] },
      ],
      deckSize: 40,
      prizeCards: [
        { instanceId: 'prize-1', cardId: 'p1', name: 'Prize 1' },
        { instanceId: 'prize-2', cardId: 'p2', name: 'Prize 2' },
      ],
      discardPile: [],
    },
    opponentField: {
      activePokemon: {
        instanceId: 'opp-active-1',
        cardId: 'squirtle',
        hp: 50,
        attachedEnergies: [],
        toolCard: null,
        conditions: [],
      },
      bench: [],
      handSize: 4,
      deckSize: 38,
      prizeCards: ['p3', 'p4', 'p5'],
      discardPile: [],
    },
  };
}

describe('BoardContainer', () => {
  let wsMessagesSubject: Subject<WebsocketMessage>;

  beforeEach(async () => {
    wsMessagesSubject = new Subject<WebsocketMessage>();

    await TestBed.configureTestingModule({
      imports: [BoardContainer],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'game-1' } } } },
        { provide: GAME_STATE_SOURCE, useValue: { getBoardState: () => of(buildBoardState()) } },
        { provide: GAME_ACTIONS_SOURCE, useValue: { dispatchAction: () => of({ success: true, actionType: 'noop', events: [] }) } },
        {
          provide: WEBSOCKET_SOURCE,
          useValue: {
            messages$: wsMessagesSubject.asObservable(),
            connectionStatus: () => 'disconnected' as const,
            connect: () => of(undefined),
            disconnect: () => undefined,
            publish: () => undefined,
            reset: () => undefined,
          },
        },
        { provide: APP_DATA_MODE, useValue: 'api' },
      ],
    }).compileComponents();
  });

  it('renders the board from a real BoardStateDto', () => {
    const fixture = TestBed.createComponent(BoardContainer);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;

    // Post board-rewrite the surface renders two parameterized app-player-zone
    // (opponent + player) instead of separate opponent/player/hand components.
    expect(root.querySelectorAll('app-player-zone').length).toBe(2);
    expect(root.querySelector('app-phase-indicator')).toBeTruthy();
    expect(root.querySelector('app-action-panel')).toBeTruthy();
  });

  it('exposes BoardStateDto fields (myField/opponentField), not the legacy GameStateDto shape', () => {
    const fixture = TestBed.createComponent(BoardContainer);
    fixture.detectChanges();

    const instance = fixture.componentInstance;
    const board = instance['board']();

    expect(board).toBeTruthy();
    expect(board?.myField.activePokemon?.cardId).toBe('pikachu');
    expect(board?.opponentField.activePokemon?.cardId).toBe('squirtle');
    expect(board?.opponentField.handSize).toBe(4);
  });

  describe('PLAY_BASIC_POKEMON (Slice E2)', () => {
    it('enters play-basic mode when a Basic Pokémon card is selected from hand', () => {
      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const instance = fixture.componentInstance;
      const basicCard = instance['hand']()[0];

      instance['handleCardSelected'](basicCard);

      expect(instance['interactionMode']()).toBe('play-basic');
      expect(instance['selectedCard']()?.instanceId).toBe('hand-1');
    });

    it('does not enter play-basic mode for a non-Basic Pokémon card', () => {
      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const instance = fixture.componentInstance;
      const stage1Card = instance['hand']()[1];

      instance['handleCardSelected'](stage1Card);

      expect(instance['interactionMode']()).not.toBe('play-basic');
    });

    it('dispatches PLAY_BASIC_POKEMON with targetPosition BENCH_n when an empty bench slot is clicked', () => {
      const dispatchAction = jasmine.createSpy('dispatchAction').and.returnValue(of({ success: true, actionType: 'PLAY_BASIC_POKEMON', events: [] }));

      TestBed.overrideProvider(GAME_ACTIONS_SOURCE, { useValue: { dispatchAction } });

      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const instance = fixture.componentInstance;
      const basicCard = instance['hand']()[0];

      instance['handleCardSelected'](basicCard);
      instance['handleZoneClicked']({ type: 'bench', index: 2 });

      expect(dispatchAction).toHaveBeenCalledWith('game-1', {
        action: 'playBasicPokemon',
        payload: { cardInstanceId: 'hand-1', targetPosition: 'BENCH_2' },
      });
    });

    it('dispatches PLAY_BASIC_POKEMON with targetPosition ACTIVE when the active slot is clicked', () => {
      const dispatchAction = jasmine.createSpy('dispatchAction').and.returnValue(of({ success: true, actionType: 'PLAY_BASIC_POKEMON', events: [] }));

      TestBed.overrideProvider(GAME_ACTIONS_SOURCE, { useValue: { dispatchAction } });

      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const instance = fixture.componentInstance;
      const basicCard = instance['hand']()[0];

      instance['handleCardSelected'](basicCard);
      instance['handleZoneClicked']({ type: 'active' });

      expect(dispatchAction).toHaveBeenCalledWith('game-1', {
        action: 'playBasicPokemon',
        payload: { cardInstanceId: 'hand-1', targetPosition: 'ACTIVE' },
      });
    });

    it('clears interaction state after a successful PLAY_BASIC_POKEMON dispatch', () => {
      const dispatchAction = jasmine.createSpy('dispatchAction').and.returnValue(of({ success: true, actionType: 'PLAY_BASIC_POKEMON', events: [] }));

      TestBed.overrideProvider(GAME_ACTIONS_SOURCE, { useValue: { dispatchAction } });

      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const instance = fixture.componentInstance;
      const basicCard = instance['hand']()[0];

      instance['handleCardSelected'](basicCard);
      instance['handleZoneClicked']({ type: 'bench', index: 1 });

      expect(instance['interactionMode']()).toBe('idle');
      expect(instance['selectedCard']()).toBeNull();
    });
  });

  describe('USE_ATTACK (Slice E3)', () => {
    function flushCardDetail(httpMock: HttpTestingController, cardId: string, attacks: unknown[]): void {
      const req = httpMock.expectOne(`/api/cards/${cardId}`);
      req.flush({
        id: cardId,
        name: 'Pikachu',
        supertype: 'Pokémon',
        subtypes: ['Basic'],
        hp: 60,
        types: ['Lightning'],
        cardSetId: 'base1',
        cardSetName: 'Base',
        imageUrlSmall: '',
        imageUrlLarge: '',
        evolvesFrom: null,
        attacks: JSON.stringify(attacks),
        weaknesses: null,
        resistances: null,
        retreatCost: [],
        aceTactician: false,
      });
    }

    // Post board-rewrite, the attack flow uses the card-inspection overlay
    // (inspectMode 'inspect-attack') instead of the legacy attack modal.
    const ownActiveClick = {
      card: { instanceId: 'my-active-1', cardId: 'pikachu', name: 'Pikachu' },
      origin: 'active' as const,
    };

    it('opens the attack inspection overlay with resolved attacks when the active Pokémon is clicked', () => {
      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const instance = fixture.componentInstance;
      const httpMock = TestBed.inject(HttpTestingController);

      instance['handleCardClicked'](ownActiveClick);
      flushCardDetail(httpMock, 'pikachu', [
        { name: 'Thunder Shock', cost: ['Lightning'], damage: '20', text: '' },
        { name: 'Thunder', cost: ['Lightning', 'Lightning', 'Colorless', 'Colorless'], damage: '100', text: '' },
      ]);
      fixture.detectChanges();

      expect(instance['inspectMode']()).toBe('inspect-attack');
      expect(instance['inspectedCard']()?.name).toBe('Pikachu');
      expect(instance['inspectDetail']()?.attacks.length).toBe(2);

      httpMock.verify();
    });

    it('exposes the attached energy count for affordability in the overlay', () => {
      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const instance = fixture.componentInstance;
      const httpMock = TestBed.inject(HttpTestingController);

      instance['handleCardClicked'](ownActiveClick);
      flushCardDetail(httpMock, 'pikachu', [
        { name: 'Thunder Shock', cost: ['Lightning'], damage: '20', text: '' },
        { name: 'Thunder', cost: ['Lightning', 'Lightning', 'Colorless', 'Colorless'], damage: '100', text: '' },
      ]);
      fixture.detectChanges();

      // myField.activePokemon has exactly 1 attached energy (see buildBoardState).
      expect(instance['inspectEnergyCount']()).toBe(1);

      httpMock.verify();
    });

    it('dispatches USE_ATTACK with the selected attackIndex and closes the overlay on success', () => {
      const dispatchAction = jasmine.createSpy('dispatchAction').and.returnValue(of({ success: true, actionType: 'USE_ATTACK', events: [] }));
      TestBed.overrideProvider(GAME_ACTIONS_SOURCE, { useValue: { dispatchAction } });

      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const instance = fixture.componentInstance;
      const httpMock = TestBed.inject(HttpTestingController);

      instance['handleCardClicked'](ownActiveClick);
      flushCardDetail(httpMock, 'pikachu', [
        { name: 'Thunder Shock', cost: ['Lightning'], damage: '20', text: '' },
      ]);
      fixture.detectChanges();

      instance['handleAttackSelected'](0);

      expect(dispatchAction).toHaveBeenCalledWith('game-1', {
        action: 'attack',
        payload: { attackIndex: 0 },
      });
      expect(instance['inspectedCard']()).toBeNull();

      httpMock.verify();
    });

    it('does not open the attack overlay when a hand card is selected and the active zone is clicked', () => {
      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const instance = fixture.componentInstance;
      const basicCard = instance['hand']()[0];

      instance['handleCardSelected'](basicCard);
      instance['handleZoneClicked']({ type: 'active' });

      // With a card selected, clicking the active zone routes to play-basic, not the attack overlay.
      expect(instance['inspectedCard']()).toBeNull();

      const httpMock = TestBed.inject(HttpTestingController);
      httpMock.verify();
    });
  });

  // ──────────────────────────────────────────────────────────────────
  //  Fase 3 — WebSocket realtime tests
  // ──────────────────────────────────────────────────────────────────
  describe('WebSocket integration', () => {

    it('1: state-changed message triggers loadGameState', () => {
      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const service = fixture.componentInstance['gameStateService'];
      spyOn(service, 'loadGameState');

      wsMessagesSubject.next({
        topic: '/topic/games/game-1/state-changed',
        payload: {},
      });

      expect(service.loadGameState).toHaveBeenCalledWith('game-1');
    });

    it('2: system reconnected message triggers loadGameState', () => {
      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const service = fixture.componentInstance['gameStateService'];
      spyOn(service, 'loadGameState');

      wsMessagesSubject.next({
        topic: 'system',
        payload: { type: 'reconnected' },
      });

      expect(service.loadGameState).toHaveBeenCalledWith('game-1');
    });

    it('3: no duplicate refresh after action when WS is connected', () => {
      // Override WS source so connectionStatus returns 'connected'
      TestBed.overrideProvider(WEBSOCKET_SOURCE, {
        useValue: {
          messages$: wsMessagesSubject.asObservable(),
          connectionStatus: () => 'connected' as const,
          connect: () => of(undefined),
          disconnect: () => undefined,
          publish: () => undefined,
          reset: () => undefined,
        },
      });

      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const service = fixture.componentInstance['gameStateService'];
      spyOn(service, 'loadGameState');

      // Dispatch an action that triggers refreshState internally
      fixture.componentInstance['handleAction']('endTurn');

      // refreshState returns early when WS is connected (no setTimeout call)
      expect(service.loadGameState).not.toHaveBeenCalled();
    });

    it('4: fallback REST refresh after action when WS is disconnected', fakeAsync(() => {
      const fixture = TestBed.createComponent(BoardContainer);
      fixture.detectChanges();

      const service = fixture.componentInstance['gameStateService'];
      spyOn(service, 'loadGameState');

      fixture.componentInstance['handleAction']('endTurn');

      // Not yet — setTimeout for 100ms hasn't fired
      expect(service.loadGameState).not.toHaveBeenCalled();

      tick(150);

      // Fallback kicked in after the delay
      expect(service.loadGameState).toHaveBeenCalledWith('game-1');
    }));

  });
});
