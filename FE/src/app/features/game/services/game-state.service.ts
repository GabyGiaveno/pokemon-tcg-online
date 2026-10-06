import { inject, Injectable, signal } from '@angular/core';
import { AuthTokenService } from '../../auth/data-access/auth-token.service';
import { ActionAvailabilityDto, CardDto, GameLogEntryDto, GameStateDto, PlayerStateDto, PokemonInPlayDto } from '../models/game-state.dto';
import { mapBackendBoardStateToGameState } from './game-state.mapper';
import { mapBackendBoardStateToBoardState } from './board-state.mapper';
import { GAME_STATE_SOURCE } from './game-state-source';
import { resolveCardArt } from './card-art';
import { BoardStateDto, CardInstanceDto } from '../models/board-state.dto';

/** Transient UI interaction modes for spatial/visual targeting (Phase 2). */
export type InteractionMode = 'idle' | 'selecting-card' | 'selecting-target' | 'awaiting-selection' | 'play-basic' | 'attack-select' | 'retreat-select' | 'energy-select' | 'evolve-select' | 'trainer-select' | 'tool-target';

@Injectable({ providedIn: 'root' })
export class GameStateService {
  private readonly authToken = inject(AuthTokenService);
  private readonly source = inject(GAME_STATE_SOURCE);
  private readonly gameStateSignal = signal<GameStateDto | null>(this.buildFallbackState('loading'));
  private readonly boardSignal = signal<BoardStateDto | null>(null);
  private readonly errorSignal = signal<string | null>(null);
  private readonly loadingSignal = signal(false);

  /** Transient: current spatial/visual interaction mode (Phase 2 targeting). */
  readonly interactionMode = signal<InteractionMode>('idle');
  /** Transient: the card the player has selected to act with (hand/board). */
  readonly selectedCard = signal<CardInstanceDto | null>(null);

  readonly gameState = this.gameStateSignal.asReadonly();
  /** Real BE board state (BoardStateDto), used by the new board component tree. */
  readonly board = this.boardSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();

  readonly playerState = () => this.gameStateSignal()?.player ?? null;
  readonly opponentState = () => this.gameStateSignal()?.opponent ?? null;
  readonly availableActions = () => this.gameStateSignal()?.availableActions ?? null;
  readonly gameLog = () => this.gameStateSignal()?.log ?? [];
  readonly turn = () => this.gameStateSignal()?.turn ?? null;
  readonly phase = () => this.gameStateSignal()?.phase ?? '';
  readonly stadium = () => this.gameStateSignal()?.stadium ?? null;
  readonly gameId = () => this.gameStateSignal()?.gameId ?? '';

  loadGameState(gameId: string): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    this.source.getBoardState(gameId).subscribe({
      next: (response) => {
        const mapped = mapBackendBoardStateToGameState(
          response,
          this.authToken.usernameValue ?? 'You',
          this.authToken.playerIdValue,
        );

        this.gameStateSignal.set(mapped);
        this.boardSignal.set(mapBackendBoardStateToBoardState(response, this.authToken.playerIdValue));
        this.loadingSignal.set(false);
      },
      error: () => {
        this.errorSignal.set('No se pudo cargar el estado de la partida.');
        this.gameStateSignal.set(this.buildFallbackState(gameId));
        this.boardSignal.set(null);
        this.loadingSignal.set(false);
      },
    });
  }

  updateGameState(state: GameStateDto): void {
    this.gameStateSignal.set(state);
    this.errorSignal.set(null);
  }

  appendLogEntry(entry: GameLogEntryDto): void {
    this.gameStateSignal.update((state) => {
      if (!state) {
        return state;
      }

      return { ...state, log: [...state.log, entry] };
    });
  }

  updateAvailableActions(actions: ActionAvailabilityDto): void {
    this.gameStateSignal.update((state) => {
      if (!state) {
        return state;
      }

      return { ...state, availableActions: actions };
    });
  }

  updatePlayerState(player: PlayerStateDto): void {
    this.gameStateSignal.update((state) => {
      if (!state) {
        return state;
      }

      return { ...state, player };
    });
  }

  updateOpponentState(opponent: PlayerStateDto): void {
    this.gameStateSignal.update((state) => {
      if (!state) {
        return state;
      }

      return { ...state, opponent };
    });
  }

  clear(): void {
    this.gameStateSignal.set(this.buildFallbackState('cleared'));
    this.boardSignal.set(null);
    this.errorSignal.set(null);
    this.loadingSignal.set(false);
    this.clearInteraction();
  }

  setError(message: string | null): void {
    this.errorSignal.set(message);
  }

  /** Reset transient interaction state (selection/targeting) to idle. */
  clearInteraction(): void {
    this.interactionMode.set('idle');
    this.selectedCard.set(null);
  }

  private buildFallbackState(gameId: string): GameStateDto {
    const makeCard = (id: string, name: string): CardDto => ({ cardId: id, name, types: [] });
    const makePokemon = (id: string, name: string): PokemonInPlayDto => ({
      cardId: id,
      name,
      imageUrl: resolveCardArt(id),
      hp: 120,
      maxHp: 120,
      damage: 0,
      energies: [makeCard(`${id}-e1`, 'Basic Energy')],
      tool: null,
      status: [],
    });

    const disabled = { enabled: false, reason: 'La disponibilidad real se conectará luego al backend.' };

    return {
      gameId,
      turn: 'PLAYER',
      phase: 'MAIN',
      player: {
        name: this.authToken.usernameValue ?? 'You',
        active: makePokemon(`${gameId}-fallback-my-active`, 'Fallback Mon'),
        bench: [
          makePokemon(`${gameId}-fallback-my-bench-1`, 'Fallback Mon A'),
          makePokemon(`${gameId}-fallback-my-bench-2`, 'Fallback Mon B'),
          makePokemon(`${gameId}-fallback-my-bench-3`, 'Fallback Mon C'),
          null,
          null,
        ],
        hand: [
          makeCard(`${gameId}-fallback-h-1`, 'Fallback Card'),
          makeCard(`${gameId}-fallback-h-2`, 'Fallback Card'),
        ],
        deckCount: 40,
        discardCount: 0,
        prizesRemaining: 6,
        prizesTaken: 0,
      },
      opponent: {
        name: 'Opponent',
        active: makePokemon(`${gameId}-fallback-opp-active`, 'Fallback Mon'),
        bench: [
          makePokemon(`${gameId}-fallback-opp-bench-1`, 'Fallback Mon A'),
          null,
          null,
          null,
          null,
        ],
        hand: [],
        deckCount: 40,
        discardCount: 0,
        prizesRemaining: 6,
        prizesTaken: 0,
      },
      stadium: null,
      log: [
        { id: `${gameId}-log-1`, timestamp: '--:--', message: 'Fallback board loaded', type: 'SYSTEM' },
      ],
      availableActions: {
        attack: disabled,
        retreat: disabled,
        playEnergy: disabled,
        playTrainer: disabled,
        evolve: disabled,
        endTurn: disabled,
      },
    };
  }

}
