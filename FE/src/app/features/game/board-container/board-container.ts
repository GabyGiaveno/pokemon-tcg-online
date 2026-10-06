import { ChangeDetectionStrategy, Component, computed, effect, inject, NgZone, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { PlayerZone, CardClickEvent, ZoneClickEvent } from '../player-zone/player-zone';
import { Card } from '../card/card';
import { PhaseIndicator } from '../phase-indicator/phase-indicator';
import { Notifications } from '../notifications/notifications';
import { WaitingScreen } from '../waiting-screen/waiting-screen';
import { CardInspectionOverlay } from '../card-inspection-overlay/card-inspection-overlay';
import { LogRibbon } from '../log-ribbon/log-ribbon';
import { CoinFlip } from '../coin-flip/coin-flip';
import { ReadyCheck } from '../ready-check/ready-check';
import { ReadyCheckApiService } from '../services/ready-check-api.service';
import { AuthTokenService } from '../../auth/data-access/auth-token.service';
import { GameStateService } from '../services/game-state.service';
import { GameActionsService } from '../services/game-actions.service';
import { CardDetailService } from '../services/card-detail.service';
import { GameEvent, GameEventStream } from '../services/game-event-stream.service';
import { BoardAnimationService } from '../services/board-animation.service';
import { GameFeedService } from '../services/game-feed.service';
import { DragDropService, DropEvent } from '../services/drag-drop.service';
import { WebsocketService } from '../../../core/services/websocket.service';
import { BackgroundMusicService } from '../../../shared/services/background-music.service';
import { AvailableAction, PendingSelectionDto } from '../models/game-state.dto';
import { BenchPokemonDto, CardInstanceDto } from '../models/board-state.dto';
import { resolveCardArt } from '../services/card-art';
import { CardDetailDto } from '../models/card-detail.dto';
import { isBasicPokemon, isEnergy, isEvolution, isTool, isTrainer, trainerActionType } from '../services/card-classify';
import { formatGameFinishReason } from '../services/game-finish-reason';

/** Smart component: top-level board orchestrator. Routes by game status and wires zones together. */
@Component({
  selector: 'app-board-container',
  templateUrl: './board-container.html',
  styleUrl: './board-container.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    PlayerZone,
    Card,
    PhaseIndicator,
    Notifications,
    WaitingScreen,
    CardInspectionOverlay,
    LogRibbon,
    CoinFlip,
    ReadyCheck,
    RouterLink,
  ],
  providers: [BoardAnimationService, DragDropService, GameFeedService],
})
export class BoardContainer implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly gameStateService = inject(GameStateService);
  private readonly gameActions = inject(GameActionsService);
  private readonly cardDetailService = inject(CardDetailService);
  private readonly gameEventStream = inject(GameEventStream);
  private readonly animations = inject(BoardAnimationService);
  private readonly dragDrop = inject(DragDropService);
  private readonly readyCheckApi = inject(ReadyCheckApiService);
  private readonly authToken = inject(AuthTokenService);
  private readonly websocket = inject(WebsocketService);
  private readonly ngZone = inject(NgZone);
  private readonly backgroundMusic = inject(BackgroundMusicService);
  protected readonly gameFeedService = inject(GameFeedService);

  protected readonly gameId: string;

  // ── Core board state (real BE DTOs) ──
  protected readonly board = computed(() => this.gameStateService.board());
  protected readonly opponentField = computed(() => this.board()?.opponentField ?? null);
  protected readonly playerField = computed(() => this.board()?.myField ?? null);
  protected readonly phase = computed(() => this.board()?.phase ?? null);
  protected readonly turnNumber = computed(() => this.board()?.turnNumber ?? 0);
  protected readonly isMyTurn = computed(() => this.board()?.isMyTurn ?? false);
  protected readonly hand = computed<CardInstanceDto[]>(() => this.playerField()?.hand ?? []);

  // ── Transient interaction state (Phase 2 spatial/visual targeting) ──
  protected readonly interactionMode = computed(() => this.gameStateService.interactionMode());
  protected readonly selectedCard = computed(() => this.gameStateService.selectedCard());
  protected readonly selectedCardId = computed(() => this.selectedCard()?.instanceId ?? null);
  protected readonly pendingSelection = computed(() => this.gameStateService.gameState()?.pendingSelection ?? null);

  // ── Card inspection overlay (PR-0B side-zoom) ──
  /** Card currently shown in the inspection overlay, or `null` when closed. */
  protected readonly inspectedCard = signal<CardInstanceDto | null>(null);
  /** 'inspect' for any card, 'inspect-attack' only for the player's own active. */
  protected readonly inspectMode = signal<'inspect' | 'inspect-attack'>('inspect');
  protected readonly inspectDetail = signal<CardDetailDto | null>(null);
  protected readonly inspectEnergyCount = computed(() => this.playerField()?.activePokemon?.attachedEnergies.length ?? 0);

  // ── Drag & drop (Point 4: tap = inspect, drag = play; see DragDropService) ──
  /** Floating drag preview (card art following the pointer), or null when not dragging. */
  protected readonly dragGhost = computed(() => {
    const payload = this.dragDrop.payload();
    const pos = this.dragDrop.ghost();
    if (!payload || !pos) return null;
    return { art: resolveCardArt(payload.card.cardId), x: pos.x, y: pos.y };
  });
  /** Kind of card being dragged (drives drop-zone highlighting in the player's zone), or null. */
  protected readonly dragKind = computed(() => this.dragDrop.payload()?.kind ?? null);
  /** True while the ghost is gliding to the drop target (enables its CSS transition). */
  protected readonly dragSettling = computed(() => this.dragDrop.settling());
  /** Latest coin flip to animate (card-triggered or opening flip), or null. */
  protected readonly coinFlip = this.animations.coinFlip;

  /**
   * Latest discrete game event from the `/events` channel. Presentation-only —
   * animation overlays (coin flip, mulligan, draw) consume this; it never drives
   * game truth (that comes from the `state-changed` REST reload).
   */
  protected readonly latestGameEvent = signal<GameEvent | null>(null);

  // ── Slice 5 combat animations (presentation-only; see BoardAnimationService) ──
  protected readonly playerActiveAnim = this.animations.playerActive;
  protected readonly opponentActiveAnim = this.animations.opponentActive;
  protected readonly screenFlash = this.animations.screenFlash;
  /** Floating damage number for each half (null when the current one targets the other side). */
  protected readonly playerFloatingDamage = computed(() => {
    const f = this.animations.floatingDamage();
    return f?.side === 'player' ? f.amount : null;
  });
  protected readonly opponentFloatingDamage = computed(() => {
    const f = this.animations.floatingDamage();
    return f?.side === 'opponent' ? f.amount : null;
  });

  // ── Soft on-card alerts (PART 3) ──
  /** instanceId of the card that should show the soft red alert glow, or `null`. */
  protected readonly alertCardId = signal<string | null>(null);
  /** Soft, auto-fading reason text shown alongside the on-card red glow (e.g. BE rejection). */
  protected readonly alertMessage = signal<string | null>(null);
  private alertTimeout: ReturnType<typeof setTimeout> | null = null;

  // ── Sudden Death state ──
  /** Current Sudden Death round (0 = not in sudden death). Set from SUDDEN_DEATH_START event or state reload. */
  protected readonly suddenDeathRound = signal(0);
  /** Controls visibility of the full-screen Sudden Death transition overlay. Auto-dismisses after 3s. */
  protected readonly showSuddenDeathOverlay = signal(false);

  // ── Legacy GameStateDto-backed signals (action panel, log, status overlays) ──
  protected readonly availableActions = computed(() => this.gameStateService.availableActions());
  protected readonly loading = computed(() => this.gameStateService.loading());
  protected readonly error = computed(() => this.gameStateService.error());
  /**
   * Game is truly finished only when status is FINISHED AND there is a real winner
   * (not a DRAW that precedes a Sudden Death round). A DRAW winnerId or SUDDEN_DEATH
   * status must not show the game-over overlay.
   */
  protected readonly gameFinished = computed(() => {
    const state = this.gameStateService.gameState();
    // SUDDEN_DEATH status means the game is being reset into a new round — not a real game over.
    if (state?.status === 'SUDDEN_DEATH') return false;
    if (state?.status !== 'FINISHED') return false;
    // If a Sudden Death overlay is showing, the board is mid-transition — suppress game-over.
    if (this.showSuddenDeathOverlay()) return false;
    return true;
  });
  protected readonly waiting = computed(() => this.gameStateService.gameState()?.status === 'WAITING');
  protected readonly iWon = computed(() => {
    const state = this.gameStateService.gameState();
    return state?.winnerId != null && state.winnerId === this.authToken.playerIdValue;
  });
  protected readonly finishedReason = computed(() =>
    formatGameFinishReason(this.gameStateService.gameState()?.finishedReason),
  );
  protected readonly inSetup = computed(() => {
    const state = this.gameStateService.gameState();
    return state?.status === 'SETUP' || state?.phase === 'SETUP';
  });

  /** Sudden Death round from the latest loaded game state (used to sync after board reload). */
  protected readonly suddenDeathRoundFromState = computed(() =>
    this.gameStateService.gameState()?.suddenDeathRound ?? 0,
  );

  // ── Opening READY_CHECK (botón "Listo" + coin flip + el ganador elige) ──
  protected readonly inReadyCheck = computed(() => this.gameStateService.gameState()?.status === 'READY_CHECK');
  protected readonly myReady = computed(() => this.gameStateService.gameState()?.myReady ?? false);
  protected readonly opponentReady = computed(() => this.gameStateService.gameState()?.opponentReady ?? false);
  protected readonly coinDecided = computed(() => this.gameStateService.gameState()?.coinFlipWinnerId != null);
  protected readonly iWonCoin = computed(() => {
    const winner = this.gameStateService.gameState()?.coinFlipWinnerId;
    return winner != null && winner === this.authToken.playerIdValue;
  });

  protected readonly myPendingSelection = computed(() => {
    const pending = this.pendingSelection();
    const myId = this.authToken.playerIdValue;
    return pending && myId !== null && pending.ownerPlayerId === myId ? pending : null;
  });

  protected readonly opponentPendingSelection = computed(() => {
    const pending = this.pendingSelection();
    const myId = this.authToken.playerIdValue;
    return pending && (myId === null || pending.ownerPlayerId !== myId) ? pending : null;
  });

  /**
   * Pending selection de tipo CHOOSE_ACTIVE_ON_KO (promoción post-KO).
   * Retorna el pendingSelection original si es KO; null si no.
   */
  protected readonly myKoSelection = computed(() => {
    const pending = this.myPendingSelection();
    return pending?.type === 'CHOOSE_ACTIVE_ON_KO' ? pending : null;
  });

  /**
   * Opciones renderizables para la promoción post-KO.
   * Cada opción incluye la cardId y datos del Pokémon en banca para
   * renderizar `<app-card>` como selector visual.
   * Si el backend proveyó validOptions, se filtran los slots válidos;
   * si no, se fallbackea a todos los slots no nulos de la banca.
   */
  protected readonly koBenchOptions = computed<{
    benchIndex: number;
    cardId: string;
    hp: number;
    maxHp: number;
    energyCount: number;
  }[] | null>(() => {
    const pending = this.myKoSelection();
    if (!pending) return null;

    const bench = this.playerField()?.bench ?? [];
    const validOptions = new Set((pending.validOptions ?? []).map((option) => String(option).toUpperCase()));

    const result: {
      benchIndex: number;
      cardId: string;
      hp: number;
      maxHp: number;
      energyCount: number;
    }[] = [];

    const pushOption = (pokemon: BenchPokemonDto, benchIndex: number): void => {
      result.push({
        benchIndex,
        cardId: pokemon.cardId,
        hp: pokemon.hp,
        maxHp: pokemon.maxHp,
        energyCount: pokemon.attachedEnergies?.length ?? 0,
      });
    };

    for (let i = 0; i < bench.length; i++) {
      const pokemon = bench[i];
      if (!pokemon) continue;

      // Si el BE especificó opciones válidas, respetarlas
      if (validOptions.size > 0) {
        const instanceId = pokemon.instanceId.toUpperCase();
        const isMatch = validOptions.has(instanceId)
          || validOptions.has(pokemon.cardId.toUpperCase())
          || validOptions.has(String(i))
          || validOptions.has(String(i + 1))
          || validOptions.has(`BENCH_${i}`)
          || validOptions.has(`BENCH_${i + 1}`);
        if (!isMatch) continue;
      }

      pushOption(pokemon, i);
    }

    // Defensive UI fallback: a pending KO selection must never leave the game
    // stuck without choices just because the backend encoded validOptions in an
    // unexpected format. Backend validation remains authoritative on click.
    if (result.length === 0 && validOptions.size > 0) {
      for (let i = 0; i < bench.length; i++) {
        const pokemon = bench[i];
        if (pokemon) pushOption(pokemon, i);
      }
    }

    return result.length > 0 ? result : null;
  });

  /** Pending selection de tipo REORDER_DECK. */
  protected readonly myReorderDeckSelection = computed(() => {
    const pending = this.myPendingSelection();
    return pending?.type === 'REORDER_DECK' ? pending : null;
  });

  /** Orden mutable de las cartas reveladas (instanceId + cardId + imageUrl). */
  protected readonly reorderDeckCards = signal<{ instanceId: string; cardId: string; imageUrl: string }[]>([]);

  /** Tracks which instanceIds the player has picked in the card picker overlay. */
  protected readonly pickerSelected = signal<string[]>([]);

  /** Pending selection for SEARCH_DECK, PLACE_ON_BENCH, CHOOSE_FROM_DISCARD — card picker overlay. */
  protected readonly myCardPickerSelection = computed(() => {
    const pending = this.myPendingSelection();
    if (!pending) return null;
    const types = ['SEARCH_DECK', 'PLACE_ON_BENCH', 'CHOOSE_FROM_DISCARD'];
    if (!types.includes(pending.type)) return null;
    const items = (pending.validOptions ?? []).map((instanceId, i) => ({
      instanceId,
      cardId: pending.revealedCardIds?.[i] ?? instanceId,
    }));
    return {
      type: pending.type,
      prompt: pending.prompt ?? 'Elegí una carta',
      items,
      selectionCount: pending.selectionCount ?? 1,
    };
  });

  /** Pending selection for SHUFFLE_POKEMON_TO_DECK — pick a bench Pokémon to return. */
  protected readonly myShufflePokemonSelection = computed(() => {
    const pending = this.myPendingSelection();
    if (!pending || pending.type !== 'SHUFFLE_POKEMON_TO_DECK') return null;
    return {
      prompt: pending.prompt ?? 'Elegí un Pokémon para devolver al mazo',
      validOptions: pending.validOptions,
    };
  });

  /** Pending selection for SWITCH_POKEMON — pick a valid bench Pokémon to promote to Active. */
  protected readonly mySwitchPokemonSelection = computed(() => {
    const pending = this.myPendingSelection();
    if (!pending || pending.type !== 'SWITCH_POKEMON') return null;

    const validOptions = new Set(pending.validOptions ?? []);
    const bench = this.playerField()?.bench ?? [];
    const options = bench
      .map((pokemon, benchIndex) => ({ pokemon, benchIndex }))
      .filter((option): option is { pokemon: BenchPokemonDto; benchIndex: number } => {
        if (!option.pokemon) return false;
        if (validOptions.size === 0) return true;
        return validOptions.has(option.pokemon.instanceId)
          || validOptions.has(String(option.benchIndex))
          || validOptions.has(`BENCH_${option.benchIndex}`);
      })
      .map(({ pokemon, benchIndex }) => ({
        instanceId: pokemon.instanceId,
        cardId: pokemon.cardId,
        benchIndex,
      }));

    return {
      prompt: pending.prompt ?? 'Elegí un Pokémon de tu banca para promover a activo',
      options,
    };
  });

  /**
   * Pending selection de tipo NO SOPORTADO — se muestra un mensaje
   * informativo en lugar de un panel de selección falso.
   */
  protected readonly pendingSelectionUnsupported = computed<PendingSelectionDto | null>(() => {
    const pending = this.myPendingSelection();
    const supported = ['CHOOSE_ACTIVE_ON_KO', 'REORDER_DECK', 'SEARCH_DECK', 'PLACE_ON_BENCH', 'CHOOSE_FROM_DISCARD', 'SHUFFLE_POKEMON_TO_DECK', 'SWITCH_POKEMON'];
    return pending && !supported.includes(pending.type) ? pending : null;
  });

  protected readonly errorMessages = signal<string[]>([]);

  /** Whether the right-side action drawer is expanded into view. */
  protected readonly drawerOpen = signal(false);

  // ── WebSocket connection status (robustness indicator) ──
  protected readonly wsStatus = this.websocket.connectionStatus;

  private wsConnectSub: Subscription | null = null;
  private wsMessagesSub: Subscription | null = null;
  private gameEventsSub: Subscription | null = null;
  private dropsSub: Subscription | null = null;

  constructor() {
    this.gameId = this.route.snapshot.paramMap.get('gameId') ?? 'offline';

    // When a REORDER_DECK selection arrives, initialise the mutable card order from it.
    effect(() => {
      const sel = this.myReorderDeckSelection();
      if (!sel) return;
      const cards = (sel.validOptions ?? []).map((instanceId, i) => {
        const cardId = sel.revealedCardIds?.[i] ?? '';
        return { instanceId, cardId, imageUrl: this.cardImageUrl(cardId) };
      });
      this.reorderDeckCards.set(cards);
    });

    // Reset picker selection when the card picker overlay is no longer relevant.
    effect(() => {
      if (!this.myCardPickerSelection()) {
        this.pickerSelected.set([]);
      }
    });

    // Keep suddenDeathRound in sync with the authoritative game state after each board reload.
    effect(() => {
      const round = this.suddenDeathRoundFromState();
      if (round > 0) {
        this.suddenDeathRound.set(round);
      }
    });
  }

  ngOnInit(): void {
    this.gameStateService.loadGameState(this.gameId);
    const myPlayerId = this.authToken.playerIdValue;
    if (myPlayerId !== null) this.gameFeedService.configure(myPlayerId);
    this.connectWebSocket();
    void this.backgroundMusic.play(0.015);
  }

  ngOnDestroy(): void {
    this.disconnectWebSocket();
    this.gameStateService.clear();
    if (this.alertTimeout) {
      clearTimeout(this.alertTimeout);
    }
  }

  // ── WebSocket ──

  private connectWebSocket(): void {
    // Discrete game events drive transient animations only (presentation layer).
    // STOMP callbacks fire outside Angular's zone, so re-enter it for change detection.
    // A completed hand-card drag routes to the matching dispatch (presentation → intent).
    this.dropsSub = this.dragDrop.drops$.subscribe((drop) => this.handleDrop(drop));

    this.gameEventsSub = this.gameEventStream.events$.subscribe((event) => {
      this.ngZone.run(() => {
        this.latestGameEvent.set(event);
        this.animations.dispatch(event, {
          isMyTurn: this.isMyTurn(),
          myPlayerId: this.authToken.playerIdValue,
        });

        if (event.type === 'SUDDEN_DEATH_START') {
          const round = typeof event.payload?.['suddenDeathRound'] === 'number'
            ? event.payload['suddenDeathRound']
            : (this.suddenDeathRound() + 1);
          this.suddenDeathRound.set(round);
          this.showSuddenDeathOverlay.set(true);
          // Auto-dismiss after 3 seconds, then reload the board to get the new SETUP state.
          setTimeout(() => {
            this.showSuddenDeathOverlay.set(false);
            this.gameStateService.loadGameState(this.gameId);
          }, 3000);
        }
      });
    });

    this.wsMessagesSub = this.websocket.messages$.subscribe((msg) => {
      // Reconnection success: we may have missed state-changed events while
      // the WebSocket was down. Reload via REST to recover the real state.
      if (msg.topic === 'system' && (msg.payload as Record<string, unknown>)?.['type'] === 'reconnected') {
        this.ngZone.run(() => this.gameStateService.loadGameState(this.gameId));
        return;
      }

      if (typeof msg.topic === 'string' && msg.topic.includes('state-changed')) {
        // STOMP/SockJS callbacks fire OUTSIDE Angular's zone, so a plain
        // signal update here wouldn't trigger change detection (stale view
        // until F5). Re-enter the zone so the reload reflects immediately.
        this.ngZone.run(() => this.gameStateService.loadGameState(this.gameId));
      }
    });

    this.wsConnectSub = this.websocket.connect(this.gameId).subscribe({
      error: () => {
        console.warn('[BoardContainer] WebSocket connection failed, game works via REST');
      },
    });
  }

  private disconnectWebSocket(): void {
    this.wsConnectSub?.unsubscribe();
    this.wsConnectSub = null;
    this.wsMessagesSub?.unsubscribe();
    this.wsMessagesSub = null;
    this.gameEventsSub?.unsubscribe();
    this.gameEventsSub = null;
    this.dropsSub?.unsubscribe();
    this.dropsSub = null;
    this.websocket.disconnect();
  }

  // ── Hand interaction ──

  /** Click on a hand card — selects it for the next action/target (Phase 2 wires the rest). */
  protected handleCardSelected(card: CardInstanceDto): void {
    const current = this.gameStateService.selectedCard();
    if (current?.instanceId === card.instanceId) {
      this.gameStateService.clearInteraction();
      return;
    }

    this.gameStateService.selectedCard.set(card);

    if (this.inSetup()) {
      this.gameStateService.interactionMode.set('selecting-target');
      return;
    }

    if (isBasicPokemon(card)) {
      this.gameStateService.interactionMode.set('play-basic');
      return;
    }

    if (isEnergy(card)) {
      // Energy: next click on a Pokémon (active/bench) attaches it.
      this.gameStateService.interactionMode.set('energy-select');
      return;
    }

    if (isEvolution(card)) {
      // Evolution: next click on an in-play Pokémon (active/bench) evolves it.
      this.gameStateService.interactionMode.set('evolve-select');
      return;
    }

    if (isTool(card)) {
      // Pokémon Tool: next click on a Pokémon (active/bench) attaches it.
      this.gameStateService.interactionMode.set('tool-target');
      return;
    }

    if (isTrainer(card)) {
      // Trainer: confirm, then play (most XY1 trainers need no target).
      this.gameStateService.interactionMode.set('trainer-select');
      return;
    }

    this.gameStateService.interactionMode.set('selecting-target');
  }

  /** Click on a placement-target zone (empty active/bench) of the player's own field. */
  protected handleZoneClicked(target: ZoneClickEvent): void {
    const selected = this.selectedCard();

    if (!selected) {
      return;
    }

    if (this.inSetup()) {
      // SETUP: place basic Pokémon from hand into active/bench
      this.placeBasic(selected.instanceId, this.toTargetPosition(target));
      return;
    }

    if (this.interactionMode() === 'play-basic') {
      this.playBasicPokemon(selected.instanceId, target);
      return;
    }

    // Phase 2 will dispatch the appropriate action based on `target` + `selected`.
    this.clearInteraction();
  }

  // ── Card inspection overlay (PR-0B side-zoom) ──

  /**
   * Routes a `cardClicked` event from either `PlayerZone` instance:
   * - own active (not in SETUP): resolve attacks then open overlay in `inspect-attack`.
   * - hand card in SETUP/MAIN: existing selection flow (placement/play-basic).
   * - any other card (bench, opponent, prizes, discard): open overlay in `inspect`.
   */
  protected handleCardClicked(event: CardClickEvent): void {
    const { card, origin } = event;

    // Retreat target selection: clicking a bench Pokémon promotes it; anything else cancels.
    if (this.interactionMode() === 'retreat-select') {
      if (origin === 'bench' && event.index !== undefined) {
        this.retreat(event.index);
      } else {
        this.cancelRetreat();
      }
      return;
    }

    // Tool attachment: clicking a Pokémon (active/bench) attaches the selected Pokémon Tool.
    if (this.interactionMode() === 'tool-target') {
      if (origin === 'active') {
        this.attachTool('ACTIVE');
      } else if (origin === 'bench' && event.index !== undefined) {
        this.attachTool(`BENCH_${event.index}`);
      } else if (origin === 'hand') {
        this.handleCardSelected(card);
      } else {
        this.clearInteraction();
      }
      return;
    }

    // Energy attachment: clicking a Pokémon (active/bench) attaches the selected energy.
    if (this.interactionMode() === 'energy-select') {
      if (origin === 'active') {
        this.attachEnergy('ACTIVE');
      } else if (origin === 'bench' && event.index !== undefined) {
        this.attachEnergy(`BENCH_${event.index}`);
      } else if (origin === 'hand') {
        this.handleCardSelected(card); // re-select a different hand card
      } else {
        this.clearInteraction();
      }
      return;
    }

    // Evolution: clicking an in-play Pokémon (active/bench) evolves it with the selected card.
    if (this.interactionMode() === 'evolve-select') {
      if (origin === 'active') {
        this.evolve('ACTIVE');
      } else if (origin === 'bench' && event.index !== undefined) {
        this.evolve(`BENCH_${event.index}`);
      } else if (origin === 'hand') {
        this.handleCardSelected(card);
      } else {
        this.clearInteraction();
      }
      return;
    }

    // Hand cards (own field only): inspect FIRST, then play/place by dragging (Point 4).
    if (origin === 'hand') {
      this.openCardInspection(card, 'inspect');
      return;
    }

    // Any board card: own active (not SETUP) shows clickable attacks; everything else read-only.
    const myActive = this.playerField()?.activePokemon;
    const isOwnActive = origin === 'active' && myActive?.instanceId === card.instanceId;
    this.openCardInspection(card, isOwnActive && !this.inSetup() ? 'inspect-attack' : 'inspect');
  }

  /**
   * Opens the inspection overlay for any card and loads its full detail (Pokédex-style,
   * in Spanish). In 'inspect-attack' mode (the player's own active) the attacks are
   * clickable to dispatch USE_ATTACK; otherwise the detail is read-only.
   */
  private openCardInspection(card: CardInstanceDto, mode: 'inspect' | 'inspect-attack'): void {
    this.inspectMode.set(mode);
    this.inspectDetail.set(null);
    this.inspectedCard.set(card);
    this.cardDetailService.getCardDetail(card.cardId).subscribe({
      next: (detail) => {
        this.inspectDetail.set(detail);
        this.inspectedCard.set({ ...card, name: detail.name || card.name });
      },
      error: () => this.pushCardAlert(card.instanceId, 'No se pudo cargar el detalle de la carta.'),
    });
  }

  /** Dispatches USE_ATTACK with the chosen attack index. */
  protected handleAttackSelected(attackIndex: number): void {
    const targetCard = this.inspectedCard();

    this.gameActions.dispatchAction(this.gameId, {
      action: 'attack',
      payload: { attackIndex },
    }).subscribe({
      next: (response) => {
        if (!response.success) {
          this.pushCardAlert(targetCard?.instanceId ?? null, response.error ?? 'No se pudo ejecutar el ataque.');
          return;
        }
        // The attack may have "succeeded" at the action level but been CANCELLED
        // inside the resolution chain (e.g. wrong energy type). Surface that reason.
        const failure = this.findAttackFailure(response.events);
        if (failure) {
          this.pushCardAlert(targetCard?.instanceId ?? null, failure);
        }
        this.closeInspectOverlay();
        this.refreshState();
      },
      error: () => this.pushCardAlert(targetCard?.instanceId ?? null, 'Error al ejecutar el ataque.'),
    });
  }

  /** Reads attack-resolution events to detect a cancelled attack and build a readable reason. */
  private findAttackFailure(events: { type: string; payload: unknown }[] | undefined): string | null {
    if (!events) return null;
    for (const ev of events) {
      if (ev.type !== 'ATTACK_DECLARED') continue;
      const p = ev.payload as Record<string, unknown> | null;
      const code = p?.['errorCode'];
      if (code === 'INSUFFICIENT_ENERGY') {
        const missing = p?.['missingType'];
        return `Energía insuficiente para el ataque${missing ? ` (falta ${missing})` : ''}.`;
      }
      if (code === 'INVALID_ATTACK_CONTEXT') {
        return 'No se pudo resolver el ataque (datos de la carta inválidos).';
      }
    }
    return null;
  }

  /**
   * Routes a completed hand-card drag to the matching dispatch, by card kind and the
   * drop-zone token ('ACTIVE' | 'BENCH_n'). Dropping off any zone (null) is a no-op.
   * Each dispatch reports its own success/failure (refresh on success, card alert on rejection).
   */
  private handleDrop(drop: DropEvent): void {
    const { payload, target } = drop;
    if (!target) {
      return; // released off any drop zone — cancel silently.
    }
    const card = payload.card;

    if (this.inSetup()) {
      this.placeBasic(card.instanceId, target);
      return;
    }

    switch (payload.kind) {
      case 'basic': {
        const zone: ZoneClickEvent = target === 'ACTIVE'
          ? { type: 'active' }
          : { type: 'bench', index: Number(target.slice('BENCH_'.length)) };
        this.playBasicPokemon(card.instanceId, zone);
        break;
      }
      case 'energy':
        this.gameStateService.selectedCard.set(card);
        this.attachEnergy(target);
        break;
      case 'evolution':
        this.gameStateService.selectedCard.set(card);
        this.evolve(target);
        break;
      case 'trainer':
        this.gameStateService.selectedCard.set(card);
        this.playSelectedTrainer();
        break;
    }
  }

  /** Closes the inspection overlay (called from the overlay's `closed` output, or after a successful action). */
  protected closeInspectOverlay(): void {
    this.inspectedCard.set(null);
    this.inspectDetail.set(null);
    this.inspectMode.set('inspect');
    this.clearInteraction();
  }


  /** Clears any pending selection/targeting state. */
  protected clearInteraction(): void {
    this.gameStateService.clearInteraction();
  }

  // ── Ready check (opening) ──

  /** Player pressed "Listo". The board reloads via the `state-changed` WebSocket. */
  protected onReady(): void {
    this.readyCheckApi.markReady(this.gameId).subscribe({
      next: () => this.refreshState(),
      error: () => this.pushCardAlert(null, 'No se pudo marcar Listo.'),
    });
  }

  /** Coin-flip winner chose who starts ('ME' or 'OPPONENT'); game advances to SETUP. */
  protected onChooseFirst(starter: 'ME' | 'OPPONENT'): void {
    this.readyCheckApi.chooseFirst(this.gameId, starter).subscribe({
      next: () => this.refreshState(),
      error: () => this.pushCardAlert(null, 'No se pudo definir quién empieza.'),
    });
  }

  // ── Actions ──

  protected handleAction(action: AvailableAction): void {
    if (action === 'concede') {
      this.concede();
      return;
    }

    if (action === 'retreat') {
      this.beginRetreat();
      return;
    }

    // Card-driven actions are performed by clicking the card on the board/hand,
    // not by a blind panel dispatch. Guide the player instead of sending an
    // incomplete payload the BE would reject.
    const guidance: Partial<Record<AvailableAction, string>> = {
      attack: 'Tocá tu Pokémon activo para elegir un ataque.',
      playEnergy: 'Tocá una energía de tu mano y luego el Pokémon a cargar.',
      evolve: 'Tocá una evolución de tu mano y luego el Pokémon a evolucionar.',
      playTrainer: 'Tocá una carta de entrenador de tu mano para jugarla.',
    };
    if (guidance[action]) {
      this.pushCardAlert(null, guidance[action]!);
      return;
    }

    // Remaining no-target actions (e.g. endTurn) dispatch directly.
    this.gameActions.dispatchAction(this.gameId, { action, payload: {} }).subscribe({
      next: (response) => {
        if (response.success) {
          this.clearInteraction();
          this.refreshState();
        } else {
          this.pushCardAlert(null, response.error ?? `No se pudo ejecutar ${action}.`);
        }
      },
      error: () => this.pushCardAlert(null, `Error al ejecutar ${action}.`),
    });
  }

  // ── Retreat (Slice 1) ──

  /** Whether the player is currently choosing a bench Pokémon to retreat into. */
  protected readonly retreatSelecting = computed(() => this.interactionMode() === 'retreat-select');

  /** Show the "Retirar" button in the active-Pokémon inspection overlay when a bench exists. */
  protected readonly canRetreatFromOverlay = computed(() =>
    this.inspectMode() === 'inspect-attack'
    && !this.inSetup()
    && (this.playerField()?.bench?.some((b) => b !== null) ?? false),
  );

  /** "Retirar" pressed inside the active-Pokémon overlay: close it and start bench selection. */
  protected onRetreatFromOverlay(): void {
    this.closeInspectOverlay();
    this.beginRetreat();
  }

  /** Enter retreat target-selection: the player picks a bench Pokémon to promote to Active. */
  private beginRetreat(): void {
    const field = this.playerField();
    if (!field?.activePokemon) {
      this.pushCardAlert(null, 'No tenés Pokémon activo para retirar.');
      return;
    }
    if (!field.bench.some((b) => b !== null)) {
      this.pushCardAlert(field.activePokemon.instanceId, 'No hay Pokémon en la banca para promover.');
      return;
    }
    this.drawerOpen.set(false);
    this.gameStateService.interactionMode.set('retreat-select');
  }

  /** Dispatch RETREAT, promoting the bench Pokémon at `benchIndex` to Active. */
  private retreat(benchIndex: number): void {
    this.gameActions.dispatchAction(this.gameId, {
      action: 'retreat',
      payload: { benchIndex },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.clearInteraction();
          this.refreshState();
        } else {
          this.pushCardAlert(
            this.playerField()?.activePokemon?.instanceId ?? null,
            response.error ?? 'No se pudo retirar (¿energía insuficiente?).',
          );
        }
      },
      error: () => this.pushCardAlert(null, 'Error al retirar.'),
    });
  }

  /** Cancel the in-progress retreat selection. */
  protected cancelRetreat(): void {
    this.clearInteraction();
  }

  // ── Attach Energy (Slice 2) ──

  /** Whether the player is choosing a Pokémon to attach the selected energy to. */
  protected readonly energySelecting = computed(() => this.interactionMode() === 'energy-select');

  /** The energy card currently selected for attachment (drives the hint banner). */
  protected readonly energyCardName = computed(() => this.selectedCard()?.name ?? 'la energía');

  /** Dispatch ATTACH_ENERGY: attach the selected energy to the Pokémon at `targetPosition`. */
  private attachEnergy(targetPosition: string): void {
    const energy = this.selectedCard();
    if (!energy) {
      this.clearInteraction();
      return;
    }
    this.gameActions.dispatchAction(this.gameId, {
      action: 'playEnergy',
      payload: { cardInstanceId: energy.instanceId, targetPosition },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.clearInteraction();
          this.refreshState();
        } else {
          this.pushCardAlert(energy.instanceId, response.error ?? 'No se pudo atachar la energía.');
        }
      },
      error: () => this.pushCardAlert(energy.instanceId, 'Error al atachar la energía.'),
    });
  }

  /** Cancel the in-progress energy attachment. */
  protected cancelEnergy(): void {
    this.clearInteraction();
  }

  // ── Evolve (Slice 3) ──

  /** Whether the player is choosing an in-play Pokémon to evolve. */
  protected readonly evolveSelecting = computed(() => this.interactionMode() === 'evolve-select');

  /** Name of the evolution card currently selected (drives the hint banner). */
  protected readonly evolveCardName = computed(() => this.selectedCard()?.name ?? 'la evolución');

  /** Dispatch EVOLVE_POKEMON: evolve the Pokémon at `targetPosition` with the selected card. */
  private evolve(targetPosition: string): void {
    const evolution = this.selectedCard();
    if (!evolution) {
      this.clearInteraction();
      return;
    }
    this.gameActions.dispatchAction(this.gameId, {
      action: 'evolve',
      payload: { cardInstanceId: evolution.instanceId, targetPosition },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.clearInteraction();
          this.refreshState();
        } else {
          this.pushCardAlert(evolution.instanceId, response.error ?? 'No se pudo evolucionar (¿stage o turno inválido?).');
        }
      },
      error: () => this.pushCardAlert(evolution.instanceId, 'Error al evolucionar.'),
    });
  }

  /** Cancel the in-progress evolution. */
  protected cancelEvolve(): void {
    this.clearInteraction();
  }

  // ── Play Trainer (Slice 4) ──

  /** Whether a trainer card is selected and awaiting confirmation to play. */
  protected readonly trainerSelecting = computed(() => this.interactionMode() === 'trainer-select');

  /** Name of the trainer card pending confirmation (drives the hint banner). */
  protected readonly trainerCardName = computed(() => this.selectedCard()?.name ?? 'el entrenador');

  /** Play the selected Trainer, routing by subtype to PLAY_ITEM / PLAY_SUPPORTER / PLAY_STADIUM. */
  protected playSelectedTrainer(): void {
    const trainer = this.selectedCard();
    if (!trainer) {
      this.clearInteraction();
      return;
    }
    this.gameActions.dispatchAction(this.gameId, {
      action: trainerActionType(trainer),
      payload: { cardInstanceId: trainer.instanceId },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.clearInteraction();
          this.refreshState();
        } else {
          this.pushCardAlert(trainer.instanceId, response.error ?? 'No se pudo jugar el entrenador.');
        }
      },
      error: () => this.pushCardAlert(trainer.instanceId, 'Error al jugar el entrenador.'),
    });
  }

  /** Cancel the in-progress trainer play. */
  protected cancelTrainer(): void {
    this.clearInteraction();
  }

  // ── Attach Tool (Slice 5) ──

  /** Whether the player is choosing a Pokémon to attach the selected Pokémon Tool to. */
  protected readonly toolTargetSelecting = computed(() => this.interactionMode() === 'tool-target');

  /** Name of the tool card currently selected (drives the hint banner). */
  protected readonly toolCardName = computed(() => this.selectedCard()?.name ?? 'la herramienta');

  /** Dispatch ATTACH_TOOL: attach the selected Pokémon Tool to the Pokémon at `targetPosition`. */
  private attachTool(targetPosition: string): void {
    const tool = this.selectedCard();
    if (!tool) {
      this.clearInteraction();
      return;
    }
    this.gameActions.dispatchAction(this.gameId, {
      action: 'attachTool',
      payload: { cardInstanceId: tool.instanceId, targetPosition },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.clearInteraction();
          this.refreshState();
        } else {
          this.pushCardAlert(tool.instanceId, response.error ?? 'No se pudo equipar la herramienta.');
        }
      },
      error: () => this.pushCardAlert(tool.instanceId, 'Error al equipar la herramienta.'),
    });
  }

  /** Cancel the in-progress tool attachment. */
  protected cancelTool(): void {
    this.clearInteraction();
  }

  // ── Card Picker (SEARCH_DECK / PLACE_ON_BENCH / CHOOSE_FROM_DISCARD) ──

  /** Toggle a card in/out of the picker selection (up to selectionCount). */
  protected togglePickerCard(instanceId: string): void {
    const sel = this.myCardPickerSelection();
    if (!sel) return;
    const current = this.pickerSelected();
    if (current.includes(instanceId)) {
      this.pickerSelected.set(current.filter(id => id !== instanceId));
    } else if (current.length < sel.selectionCount) {
      this.pickerSelected.set([...current, instanceId]);
    }
  }

  /** Confirm the card picker selection and dispatch resolveSelection. */
  protected resolveCardPicker(): void {
    this.gameActions.dispatchAction(this.gameId, {
      action: 'resolveSelection',
      payload: { orderedInstanceIds: this.pickerSelected() },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.pickerSelected.set([]);
          this.refreshState();
        } else {
          this.pushError(response.error ?? 'No se pudo confirmar la selección.');
        }
      },
      error: () => this.pushError('Error al confirmar la selección.'),
    });
  }

  /** Skip the card picker (send empty selection). */
  protected skipCardPicker(): void {
    this.gameActions.dispatchAction(this.gameId, {
      action: 'resolveSelection',
      payload: { orderedInstanceIds: [] },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.pickerSelected.set([]);
          this.refreshState();
        } else {
          this.pushError(response.error ?? 'No se pudo pasar la selección.');
        }
      },
      error: () => this.pushError('Error al pasar la selección.'),
    });
  }

  /** Dispatch resolveSelection with a single Pokémon instanceId (SHUFFLE_POKEMON_TO_DECK). */
  protected resolveShufflePokemon(instanceId: string): void {
    this.gameActions.dispatchAction(this.gameId, {
      action: 'resolveSelection',
      payload: { orderedInstanceIds: [instanceId] },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.refreshState();
        } else {
          this.pushError(response.error ?? 'No se pudo devolver el Pokémon al mazo.');
        }
      },
      error: () => this.pushError('Error al devolver el Pokémon al mazo.'),
    });
  }

  /** Dispatch resolveSelection with benchIndex for SWITCH_POKEMON. */
  protected resolveSwitchPokemon(benchIndex: number): void {
    this.gameActions.dispatchAction(this.gameId, {
      action: 'resolveSelection',
      payload: { benchIndex },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.refreshState();
        } else {
          this.pushError(response.error ?? 'No se pudo cambiar el Pokémon.');
        }
      },
      error: () => this.pushError('Error al cambiar el Pokémon.'),
    });
  }

  /** Confirm SETUP phase. */
  protected confirmSetup(): void {
    this.gameActions.dispatchAction(this.gameId, { action: 'endTurn' }).subscribe({
      next: (response) => {
        if (response.success) {
          this.refreshState();
        } else {
          this.pushError(response.error ?? 'No se pudo confirmar la preparación.');
        }
      },
      error: () => this.pushError('Error al confirmar la preparación.'),
    });
  }

  /** Resolve a pending selection (e.g. choosing a bench Pokémon to promote after a KO). */
  protected resolveSelection(benchIndex: number): void {
    this.gameActions.dispatchAction(this.gameId, {
      action: 'resolveSelection',
      payload: { benchIndex },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.refreshState();
        } else {
          this.pushError(response.error ?? 'No se pudo resolver la selección.');
        }
      },
      error: () => this.pushError('Error al resolver la selección.'),
    });
  }

  /** Move a card in the reorder list up (-1) or down (+1). */
  protected moveReorderCard(index: number, direction: -1 | 1): void {
    const cards = [...this.reorderDeckCards()];
    const target = index + direction;
    if (target < 0 || target >= cards.length) return;
    [cards[index], cards[target]] = [cards[target], cards[index]];
    this.reorderDeckCards.set(cards);
  }

  /** Confirm the chosen deck order. */
  protected resolveReorderDeck(): void {
    const orderedInstanceIds = this.reorderDeckCards().map(c => c.instanceId);
    this.gameActions.dispatchAction(this.gameId, {
      action: 'resolveSelection',
      payload: { orderedInstanceIds },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.refreshState();
        } else {
          this.pushError(response.error ?? 'No se pudo confirmar el orden.');
        }
      },
      error: () => this.pushError('Error al confirmar el orden del mazo.'),
    });
  }

  /** Builds a Pokemon TCG image URL from a cardId like "xy1-25". */
  private cardImageUrl(cardId: string): string {
    const [set, num] = cardId.split('-');
    return set && num ? `https://images.pokemontcg.io/${set}/${num}_hires.png` : '';
  }

  /** Play a Basic Pokémon from hand to an empty active/bench slot (MAIN phase). */
  private playBasicPokemon(cardInstanceId: string, target: ZoneClickEvent): void {
    const targetPosition = this.toTargetPosition(target);

    this.gameActions.dispatchAction(this.gameId, {
      action: 'playBasicPokemon',
      payload: { cardInstanceId, targetPosition },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.clearInteraction();
          this.refreshState();
        } else {
          this.pushCardAlert(cardInstanceId, response.error ?? 'No se pudo jugar el Pokémon básico.');
        }
      },
      error: () => this.pushCardAlert(cardInstanceId, 'Error al jugar el Pokémon básico.'),
    });
  }

  private placeBasic(cardInstanceId: string, targetPosition?: string): void {
    this.gameActions.dispatchAction(this.gameId, {
      action: 'setupPlacePokemon',
      payload: { cardInstanceId, ...(targetPosition ? { targetPosition } : {}) },
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.clearInteraction();
          this.refreshState();
        } else {
          this.pushCardAlert(cardInstanceId, response.error ?? 'No se pudo colocar el Pokémon.');
        }
      },
      error: () => this.pushCardAlert(cardInstanceId, 'Error al colocar el Pokémon.'),
    });
  }

  private toTargetPosition(target: ZoneClickEvent): string {
    return target.type === 'active' ? 'ACTIVE' : `BENCH_${target.index}`;
  }

  private concede(): void {
    this.gameActions.dispatchAction(this.gameId, { action: 'concede' }).subscribe({
      next: (response) => {
        if (response.success) {
          this.errorMessages.set(['Partida concedida.']);
          this.refreshState();
        } else {
          this.pushError(response.error ?? 'No se pudo conceder.');
        }
      },
      error: () => this.pushError('Error al conceder.'),
    });
  }

  private refreshState(): void {
    // If the WebSocket is connected, the backend's state-changed event is the
    // primary source — it arrives milliseconds after the action response and
    // triggers loadGameState via the messages$ subscription.  Skip the manual
    // REST call to avoid a wasteful double refresh.
    if (this.websocket.connectionStatus() === 'connected') {
      return;
    }

    // Fallback: no realtime channel available → poll REST after a short delay.
    setTimeout(() => {
      this.gameStateService.loadGameState(this.gameId);
    }, 100);
  }

  /**
   * Fallback for non-card-specific errors (kept minimal, PART 3): pushes a
   * text notification only when no relevant card can be targeted.
   */
  private pushError(message: string): void {
    this.errorMessages.set([...this.errorMessages(), message]);
    this.pushCardAlert(null, message);
  }

  /**
   * Soft on-card alert (PART 3): sets a red glow/pulse on the relevant card
   * for ~2.5s. Falls back to the player's active Pokémon if no `cardInstanceId`
   * is given, and to the text notification list if there's no card at all.
   */
  private pushCardAlert(cardInstanceId: string | null, message: string): void {
    const targetId = cardInstanceId ?? this.playerField()?.activePokemon?.instanceId ?? null;

    if (this.alertTimeout) {
      clearTimeout(this.alertTimeout);
    }

    // Red glow on the relevant card (when known) + a soft, auto-fading reason text.
    this.alertCardId.set(targetId);
    this.alertMessage.set(message);
    this.alertTimeout = setTimeout(() => {
      this.alertCardId.set(null);
      this.alertMessage.set(null);
      this.alertTimeout = null;
    }, 3500);
  }

  protected dismissNotification(index: number): void {
    this.errorMessages.update((msgs) => msgs.filter((_, i) => i !== index));
  }

  /** Toggles the right-side action drawer open/closed. */
  protected toggleDrawer(): void {
    this.drawerOpen.update((open) => !open);
  }
}
