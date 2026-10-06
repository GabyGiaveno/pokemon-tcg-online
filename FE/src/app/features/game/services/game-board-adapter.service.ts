import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import {
  CardDto,
  GameStateDto,
  PlayerStateDto,
  PokemonInPlayDto,
  StatusCondition,
} from '../models/game-state.dto';
import {
  ActivePokemonDto,
  BenchPokemonDto,
  BoardStateDto,
  CardInstanceDto,
  OpponentFieldDto,
  PlayerFieldDto,
} from '../models/board-state.dto';
import { computeActionAvailability } from './action-availability.util';

/**
 * Adapts the BE {@link BoardStateDto} into the FE-friendly {@link GameStateDto}.
 *
 * <p>Known limitations (future sessions):
 * <ul>
 *   <li>BE doesn't return {@code name} on active/bench Pokémon → uses {@code cardId} as fallback</li>
 *   <li>BE doesn't return stadium card → always {@code null}</li>
 *   <li>BE doesn't return game log → always empty array; events come via WebSocket</li>
 * </ul>
 */
@Injectable({ providedIn: 'root' })
export class GameBoardAdapterService {
  private readonly http = inject(HttpClient);

  /** Fetches the board state from the BE and adapts it to the FE model. */
  fetchState(gameId: string, playerName: string, opponentName: string): Observable<GameStateDto> {
    return this.http.get<BoardStateDto>(`/api/games/${gameId}/state`).pipe(
      map((be) => this.adaptToGameState(be, playerName, opponentName)),
    );
  }

  // ---------------------------------------------------------------
  //  Top-level adapt
  // ---------------------------------------------------------------

  private adaptToGameState(be: BoardStateDto, playerName: string, opponentName: string): GameStateDto {
    return {
      gameId: be.gameId,
      turn: be.isMyTurn ? 'PLAYER' : 'OPPONENT',
      phase: be.phase ?? '',
      player: this.adaptMyField(be.myField, playerName),
      opponent: this.adaptOpponentField(be.opponentField, opponentName),
      stadium: null,
      log: [],
      availableActions: computeActionAvailability(
        be.phase ?? '',
        be.isMyTurn,
        be.myField.activePokemon !== null,
        (be.myField.bench ?? []).some((b) => b !== null),
        be.opponentField.activePokemon !== null,
      ),
    };
  }

  // ---------------------------------------------------------------
  //  Player fields
  // ---------------------------------------------------------------

  private adaptMyField(field: PlayerFieldDto, name: string): PlayerStateDto {
    const prizes = field.prizeCards ?? [];
    const prizesRemaining = prizes.filter((p) => p !== null).length;
    const totalPrizes = 6;
    return {
      name,
      active: field.activePokemon ? this.adaptActivePokemon(field.activePokemon) : null,
      bench: this.adaptBench(field.bench),
      hand: field.hand.map((c) => this.adaptCardInstance(c)),
      deckCount: field.deckSize,
      discardCount: field.discardPile.length,
      prizesRemaining,
      prizesTaken: totalPrizes - prizesRemaining,
    };
  }

  private adaptOpponentField(field: OpponentFieldDto, name: string): PlayerStateDto {
    const prizes = field.prizeCards ?? [];
    const prizesRemaining = prizes.filter((p) => p !== null).length;
    const totalPrizes = 6;
    return {
      name,
      active: field.activePokemon ? this.adaptActivePokemon(field.activePokemon) : null,
      bench: this.adaptBench(field.bench),
      hand: this.buildHiddenHand(field.handSize),
      deckCount: field.deckSize,
      discardCount: field.discardPile.length,
      prizesRemaining,
      prizesTaken: totalPrizes - prizesRemaining,
    };
  }

  // ---------------------------------------------------------------
  //  Sub-entity adapters
  // ---------------------------------------------------------------

  private adaptActivePokemon(pokemon: ActivePokemonDto): PokemonInPlayDto {
    const maxHp = pokemon.maxHp ?? pokemon.hp;
    return {
      cardId: pokemon.cardId,
      name: pokemon.cardId,
      hp: pokemon.hp,
      maxHp,
      damage: Math.max(0, maxHp - pokemon.hp),
      energies: (pokemon.attachedEnergies ?? []).map((e) => this.adaptCardInstance(e)),
      tool: pokemon.toolCard ? this.adaptCardInstance(pokemon.toolCard) : null,
      status: this.adaptConditions(pokemon.conditions ?? []),
    };
  }

  private adaptBenchPokemon(pokemon: BenchPokemonDto): PokemonInPlayDto {
    const maxHp = pokemon.maxHp ?? pokemon.hp;
    return {
      cardId: pokemon.cardId,
      name: pokemon.cardId,
      hp: pokemon.hp,
      maxHp,
      damage: Math.max(0, maxHp - pokemon.hp),
      energies: (pokemon.attachedEnergies ?? []).map((e) => this.adaptCardInstance(e)),
      tool: null,
      status: [],
    };
  }

  private adaptBench(bench: (BenchPokemonDto | null)[]): (PokemonInPlayDto | null)[] {
    // Normalize to exactly 5 bench slots
    const slots = bench ?? [];
    const padded = [...slots, null, null, null, null, null].slice(0, 5);
    return padded.map((p) => (p ? this.adaptBenchPokemon(p) : null));
  }

  private adaptCardInstance(c: CardInstanceDto): CardDto {
    return { cardId: c.cardId, name: c.name };
  }

  /** Builds hidden placeholder cards for the opponent's hand. */
  private buildHiddenHand(size: number): CardDto[] {
    return Array.from({ length: size }, (_, i) => ({
      cardId: `hidden-${i}`,
      name: 'Card',
    }));
  }

  private adaptConditions(conditions: string[]): StatusCondition[] {
    const valid: StatusCondition[] = ['ASLEEP', 'BURNED', 'CONFUSED', 'PARALYZED', 'POISONED'];
    return conditions.filter((c): c is StatusCondition => valid.includes(c as StatusCondition));
  }
}
