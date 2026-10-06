import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { AuthTokenService } from '../../auth/data-access/auth-token.service';
import { BackendBoardStateDto } from '../models/game-api.dto';
import { GameStateSource } from './game-state-source';

@Injectable({ providedIn: 'root' })
export class GameStateMockService implements GameStateSource {
  constructor(private readonly authToken: AuthTokenService) {}

  getBoardState(gameId: string): Observable<BackendBoardStateDto> {
    const playerId = this.authToken.playerIdValue ?? 1;
    const seed = this.hashGameId(gameId);
    const scenario = this.getScenario(gameId);

    if (scenario === 'empty') {
      return of(this.buildEmptyBoard(gameId, playerId));
    }

    if (scenario === 'late') {
      return of(this.buildLateGameBoard(gameId, playerId, seed));
    }

    if (scenario === 'blue') {
      return of(this.buildBlueBoard(gameId, playerId, seed));
    }

    if (scenario === 'red') {
      return of(this.buildRedBoard(gameId, playerId, seed));
    }

    return of(this.buildDefaultBoard(gameId, playerId, seed));
  }

  private hashGameId(gameId: string): number {
    return [...gameId].reduce((sum, char) => sum + char.charCodeAt(0), 0);
  }

  private pick<T>(values: readonly T[], seed: number): T {
    return values[seed % values.length];
  }

  private getScenario(gameId: string): 'default' | 'blue' | 'red' | 'late' | 'empty' {
    const id = gameId.toLowerCase();
    if (id.includes('empty')) return 'empty';
    if (id.includes('late')) return 'late';
    if (id.includes('blue')) return 'blue';
    if (id.includes('red')) return 'red';
    return 'default';
  }

  private buildDefaultBoard(gameId: string, playerId: number, seed: number): BackendBoardStateDto {
    return this.buildBalancedBoard(gameId, playerId, seed, {
      phase: this.pick(['MAIN', 'ATTACK', 'SETUP', 'END_TURN'] as const, seed),
      myPrizeCount: 3 + (seed % 3),
      oppPrizeCount: 3 + ((seed + 1) % 3),
      myDeckSize: 40 + (seed % 10),
      oppDeckSize: 40 + ((seed + 3) % 10),
      myTurn: seed % 2 === 0,
      myActiveHp: 120 + (seed % 40),
      oppActiveHp: 130 + ((seed + 5) % 45),
      myHandSize: 4,
      opponentHandSize: 4 + (seed % 2),
      myActiveCardId: this.pick(['pkm-mock-001', 'pkm-mock-007', 'pkm-mock-012'], seed),
      oppActiveCardId: this.pick(['pkm-opp-001', 'pkm-opp-007', 'pkm-opp-012'], seed + 1),
    });
  }

  private buildBlueBoard(gameId: string, playerId: number, seed: number): BackendBoardStateDto {
    return this.buildBalancedBoard(gameId, playerId, seed, {
      phase: 'MAIN',
      myPrizeCount: 4,
      oppPrizeCount: 2,
      myDeckSize: 34 + (seed % 8),
      oppDeckSize: 44 + (seed % 6),
      myTurn: true,
      myActiveHp: 150,
      oppActiveHp: 110,
      myBenchTweaks: [20, 15, 10, 5, 0],
      oppBenchTweaks: [-10, -5, 0, 5, 10],
      myHandSize: 6,
      opponentHandSize: 3,
      myActiveCardId: 'pkm-mock-001',
      oppActiveCardId: 'pkm-opp-014',
      myActiveConditions: [],
      oppActiveConditions: ['CONFUSED'],
      myToolCardId: 'tool-choice-belt',
      myToolName: 'Choice Belt',
      myEnergyCount: 3,
      oppEnergyCount: 1,
    });
  }

  private buildRedBoard(gameId: string, playerId: number, seed: number): BackendBoardStateDto {
    return this.buildBalancedBoard(gameId, playerId, seed, {
      phase: 'ATTACK',
      myPrizeCount: 2,
      oppPrizeCount: 4,
      myDeckSize: 42,
      oppDeckSize: 36,
      myTurn: false,
      myActiveHp: 90,
      oppActiveHp: 170,
      myBenchTweaks: [-10, -5, 0, 5, 10],
      oppBenchTweaks: [0, 10, 5, 15, 20],
      myHandSize: 3,
      opponentHandSize: 5,
      myActiveCardId: 'pkm-mock-014',
      oppActiveCardId: 'pkm-opp-001',
      myActiveConditions: ['POISONED', 'CONFUSED'],
      oppActiveConditions: [],
      myToolCardId: null,
      myEnergyCount: 1,
      oppEnergyCount: 4,
    });
  }

  private buildLateGameBoard(gameId: string, playerId: number, seed: number): BackendBoardStateDto {
    return this.buildBalancedBoard(gameId, playerId, seed, {
      phase: 'END_TURN',
      myPrizeCount: 1,
      oppPrizeCount: 1,
      myDeckSize: 10,
      oppDeckSize: 8,
      myTurn: seed % 2 === 0,
      myActiveHp: 60,
      oppActiveHp: 50,
      myBenchTweaks: [-20, -10, 0, 0, 0],
      oppBenchTweaks: [-15, -5, 0, 0, 0],
      discardCount: 6,
      opponentDiscardCount: 7,
      myHandSize: 2,
      opponentHandSize: 2,
      myActiveCardId: 'pkm-mock-008',
      oppActiveCardId: 'pkm-opp-008',
      myActiveConditions: ['ASLEEP'],
      oppActiveConditions: ['BURNED'],
      myEnergyCount: 2,
      oppEnergyCount: 2,
    });
  }

  private buildEmptyBoard(gameId: string, playerId: number): BackendBoardStateDto {
    return {
      gameId,
      currentPlayerId: playerId,
      phase: 'SETUP',
      turnNumber: 1,
      myField: {
        activePokemon: null,
        bench: [null, null, null, null, null].filter((slot): slot is never => false) as never,
        hand: [],
        deckSize: 0,
        prizeCards: [],
        discardPile: [],
      },
      opponentField: {
        activePokemon: null,
        bench: [null, null, null, null, null].filter((slot): slot is never => false) as never,
        handSize: 0,
        deckSize: 0,
        prizeCards: [],
        discardPile: [],
      },
    };
  }

  private buildBalancedBoard(
    gameId: string,
    playerId: number,
    seed: number,
    config: {
      phase: 'MAIN' | 'ATTACK' | 'SETUP' | 'END_TURN';
      myPrizeCount: number;
      oppPrizeCount: number;
      myDeckSize: number;
      oppDeckSize: number;
      myTurn: boolean;
      myActiveHp: number;
      oppActiveHp: number;
      myBenchTweaks?: number[];
      oppBenchTweaks?: number[];
      discardCount?: number;
      opponentDiscardCount?: number;
      myHandSize?: number;
      opponentHandSize?: number;
      myActiveCardId?: string;
      oppActiveCardId?: string;
      myActiveConditions?: string[];
      oppActiveConditions?: string[];
      myToolCardId?: string | null;
      myToolName?: string | null;
      myEnergyCount?: number;
      oppEnergyCount?: number;
    },
  ): BackendBoardStateDto {
    const makeBench = (prefix: string, cardPool: string[], baseHp: number, tweaks: number[] = [0, 0, 0, 0, 0]) =>
      Array.from({ length: 5 }, (_, index) => ({
        instanceId: `${gameId}-${prefix}-bench-${index + 1}`,
        cardId: this.pick(cardPool, seed + index),
        hp: Math.max(10, baseHp + (tweaks[index] ?? 0)),
        attachedEnergies: this.buildEnergyAttachments(gameId, `${prefix}-bench-${index + 1}`, 0),
      }));

    const makeHand = (prefix: string, count: number) =>
      Array.from({ length: count }, (_, index) => ({
        instanceId: `${gameId}-${prefix}-h-${index + 1}`,
        cardId: this.pick([
          'card-energy-1',
          'card-trainer-1',
          'card-pokemon-1',
          'card-item-1',
          'card-supporter-1',
        ], seed + index),
        name: this.pick(['Basic Energy', 'Trainer', 'Pokemon', 'Item', 'Supporter'], seed + index),
      }));

    const makeActive = (
      prefix: string,
      cardId: string,
      hp: number,
      energyCount: number,
      conditions: string[] = [],
      toolCardId: string | null = null,
      toolName: string | null = null,
    ) => ({
      instanceId: `${gameId}-${prefix}-active-1`,
      cardId,
      hp,
      attachedEnergies: this.buildEnergyAttachments(gameId, `${prefix}-active-1`, energyCount),
      toolCard: toolCardId ? { instanceId: `${gameId}-${prefix}-tool-1`, cardId: toolCardId, name: toolName ?? 'Tool' } : null,
      conditions,
    });

    const prizeCards = (count: number, prefix: string) =>
      Array.from({ length: count }, (_, index) => ({
        instanceId: `${gameId}-${prefix}-pr-${index + 1}`,
        cardId: `prize-${index + 1}`,
        name: 'Prize',
      }));

    return {
      gameId,
      currentPlayerId: config.myTurn ? playerId : playerId + 1,
      phase: config.phase,
      turnNumber: 1 + (seed % 7),
      myField: {
        activePokemon: makeActive(
          'my',
          config.myActiveCardId ?? this.pick(['pkm-mock-001', 'pkm-mock-007', 'pkm-mock-012'], seed),
          config.myActiveHp,
          config.myEnergyCount ?? 1,
          config.myActiveConditions ?? [],
          config.myToolCardId ?? (seed % 2 === 0 ? 'tool-choice-belt' : null),
          config.myToolName ?? (seed % 2 === 0 ? 'Choice Belt' : null),
        ),
        bench: makeBench('my', ['pkm-mock-002', 'pkm-mock-008', 'pkm-mock-014'], 80, config.myBenchTweaks),
        hand: makeHand('my', config.myHandSize ?? 5),
        deckSize: config.myDeckSize,
        prizeCards: prizeCards(config.myPrizeCount, 'my'),
        discardPile: Array.from({ length: config.discardCount ?? 2 }, (_, index) => ({
          instanceId: `${gameId}-d-${index + 1}`,
          cardId: 'discard-1',
          name: 'Discarded',
        })),
      },
      opponentField: {
        activePokemon: makeActive(
          'opp',
          config.oppActiveCardId ?? this.pick(['pkm-opp-001', 'pkm-opp-007', 'pkm-opp-012'], seed + 1),
          config.oppActiveHp,
          config.oppEnergyCount ?? 1,
          config.oppActiveConditions ?? [],
          null,
          null,
        ),
        bench: makeBench('opp', ['pkm-opp-002', 'pkm-opp-008', 'pkm-opp-014'], 85, config.oppBenchTweaks),
        handSize: config.opponentHandSize ?? 3 + (seed % 4),
        deckSize: config.oppDeckSize,
        prizeCards: Array.from({ length: config.oppPrizeCount }, () => null),
        discardPile: Array.from({ length: config.opponentDiscardCount ?? 1 }, (_, index) => ({
          instanceId: `${gameId}-opp-d-${index + 1}`,
          cardId: 'discard-1',
          name: 'Discarded',
        })),
      },
    };
  }

  private buildEnergyAttachments(gameId: string, prefix: string, count: number): { instanceId: string; cardId: string; name: string }[] {
    return Array.from({ length: count }, (_, index) => ({
      instanceId: `${gameId}-${prefix}-e-${index + 1}`,
      cardId: 'energy-basic',
      name: 'Basic Energy',
    }));
  }
}
