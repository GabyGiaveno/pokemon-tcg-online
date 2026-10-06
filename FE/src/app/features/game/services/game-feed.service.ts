import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GameEventStream } from './game-event-stream.service';
import { FeedEntry, FeedEntryCategory, FeedEntryKind } from '../models/feed-entry.model';
import { formatGameFinishReason } from './game-finish-reason';

const MAX_ENTRIES = 500;
const ENTRIES_DROP_BATCH = 100;
const MAX_TOASTS = 3;
const TOAST_TTL_MS = 4000;

@Injectable()
export class GameFeedService {
  private readonly eventStream = inject(GameEventStream);
  private readonly destroyRef = inject(DestroyRef);

  private myPlayerId: number | null = null;
  private entryCounter = 0;

  readonly entries = signal<FeedEntry[]>([]);
  readonly toasts = signal<FeedEntry[]>([]);

  constructor() {
    this.eventStream.events$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => this.handleEvent(event.type, event.payload));
  }

  configure(myPlayerId: number): void {
    this.myPlayerId = myPlayerId;
  }

  private handleEvent(type: string, payload: Record<string, unknown>): void {
    const entry = this.buildEntry(type, payload);
    if (!entry) return;

    this.entries.update((list) => {
      const next = [entry, ...list];
      return next.length > MAX_ENTRIES ? next.slice(0, MAX_ENTRIES - ENTRIES_DROP_BATCH) : next;
    });

    if (entry.isToast) {
      this.pushToast(entry);
    }
  }

  private pushToast(entry: FeedEntry): void {
    this.toasts.update((list) => {
      const next = [entry, ...list].slice(0, MAX_TOASTS);
      return next;
    });

    setTimeout(() => {
      this.toasts.update((list) => list.filter((t) => t.id !== entry.id));
    }, TOAST_TTL_MS);
  }

  private buildEntry(type: string, p: Record<string, unknown>): FeedEntry | null {
    const id = `feed-${++this.entryCounter}`;
    const ts = new Date();
    const mk = (kind: FeedEntryKind, cat: FeedEntryCategory, msg: string, toast: boolean): FeedEntry =>
      ({ id, kind, category: cat, message: msg, timestamp: ts, isToast: toast });

    switch (type) {
      case 'CARD_DRAWN': {
        const mine = typeof p['playerId'] === 'number' && p['playerId'] === this.myPlayerId;
        const deck = typeof p['cardsInDeck'] === 'number' ? ` (${p['cardsInDeck']} remaining)` : '';
        return mk('card_drawn', 'card', mine ? `You drew a card${deck}.` : 'Opponent drew a card.', false);
      }
      case 'ATTACK_DECLARED': {
        const attack = typeof p['attack'] === 'string' ? p['attack'] : 'an attack';
        return mk('attack_declared', 'combat', `"${attack}" declared!`, false);
      }
      case 'DAMAGE_DEALT': {
        const dmg = typeof p['finalDamage'] === 'number' ? p['finalDamage'] : '?';
        const hp = typeof p['hpRemaining'] === 'number' ? ` — ${p['hpRemaining']} HP remaining` : '';
        return mk('damage_dealt', 'combat', `${dmg} damage dealt${hp}.`, false);
      }
      case 'POKEMON_KNOCKED_OUT':
        return mk('pokemon_knocked_out', 'combat', 'A Pokémon was knocked out!', true);
      case 'PRIZE_TAKEN': {
        const left = typeof p['prizeCardsLeft'] === 'number' ? ` (${p['prizeCardsLeft']} left)` : '';
        const taken = typeof p['prizesTaken'] === 'number' && p['prizesTaken'] > 1 ? ` ×${p['prizesTaken']}` : '';
        return mk('prize_taken', 'game', `Prize card taken${taken}${left}.`, true);
      }
      case 'ENERGY_ATTACHED':
        return mk('energy_attached', 'card', 'Energy attached.', false);
      case 'STATUS_EFFECT_APPLIED':
        return mk('status_effect_applied', 'status', 'A status effect was applied.', false);
      case 'STATUS_EFFECT_CLEARED':
        return mk('status_effect_cleared', 'status', 'A status effect was cleared.', false);
      case 'TURN_STARTED': {
        const mine = typeof p['playerId'] === 'number' && p['playerId'] === this.myPlayerId;
        return mk('turn_started', 'game', mine ? 'Your turn.' : "Opponent's turn.", true);
      }
      case 'TURN_ENDED':
        return mk('turn_ended', 'game', 'Turn ended.', false);
      case 'PHASE_CHANGED':
        return mk('phase_changed', 'system', 'Phase changed.', false);
      // Main phase — playing cards
      case 'POKEMON_PLAYED_TO_BENCH':
        return mk('card_drawn', 'card', 'A Pokémon was played to the bench.', false);
      case 'POKEMON_PLAYED_TO_ACTIVE':
        return mk('card_drawn', 'card', 'A Pokémon was placed as Active.', false);
      case 'POKEMON_EVOLVED':
        return mk('card_drawn', 'card', 'A Pokémon evolved!', false);
      case 'TOOL_ATTACHED':
        return mk('energy_attached', 'card', 'A Pokémon Tool was attached.', false);
      case 'ENERGY_DISCARDED':
        return mk('energy_attached', 'card', 'Energy was discarded.', false);
      case 'TRAINER_PLAYED':
      case 'ITEM_PLAYED':
        return mk('trainer_played', 'card', 'An Item card was played.', false);
      case 'SUPPORTER_PLAYED':
        return mk('trainer_played', 'card', 'A Supporter card was played.', false);
      case 'STADIUM_PLAYED':
        return mk('trainer_played', 'card', 'A Stadium card was played.', false);
      case 'POKEMON_RETREATED':
        return mk('card_drawn', 'card', 'A Pokémon retreated.', false);
      // Attack phase
      case 'POKEMON_HEALED': {
        const healed = typeof p['amount'] === 'number' ? ` +${p['amount']} HP` : '';
        return mk('damage_dealt', 'combat', `Pokémon healed${healed}.`, false);
      }
      case 'DAMAGE_PREVENTED': {
        const amount = typeof p['amount'] === 'number' ? ` ${p['amount']}` : '';
        return mk('damage_dealt', 'combat', `Damage${amount} was prevented.`, false);
      }
      case 'POKEMON_RESTRICTED':
        return mk('status_effect_applied', 'status', 'A Pokémon was restricted.', false);
      case 'ABILITY_USED':
        return mk('trainer_played', 'card', 'An Ability was used.', false);
      // Game over
      case 'DECK_OUT':
        return mk('game_finished', 'game', formatGameFinishReason('DECK_OUT') ?? 'Partida finalizada.', true);
      case 'PLAYER_CONCEDED':
        return mk('game_finished', 'game', formatGameFinishReason('CONCEDE') ?? 'Partida finalizada.', true);
      case 'GAME_FINISHED': {
        const winnerId = typeof p['winnerId'] === 'number' ? p['winnerId'] : null;
        const iWon = winnerId !== null && winnerId === this.myPlayerId;
        const reason = formatGameFinishReason(typeof p['reason'] === 'string' ? p['reason'] : null);
        const suffix = reason ? ` — ${reason}` : '';
        return mk('game_finished', 'game', iWon ? `¡Ganaste!${suffix}` : `Partida finalizada${suffix}.`, true);
      }
      // Attack
      case 'COIN_FLIPPED': {
        const flip = typeof p['flip'] === 'string' ? p['flip'] : '—';
        return mk('coin_flipped', 'system', `Coin flip: ${flip}.`, false);
      }
      // Setup
      case 'SETUP_COMPLETE':
        return mk('setup_completed', 'system', 'Game setup complete.', false);
      case 'PLAYER_READY':
        return mk('player_ready', 'system', 'A player is ready.', false);
      case 'DECK_SEARCHED':
        return mk('trainer_played', 'card', 'Se buscó en el mazo.', false);
      case 'DECK_PEEKED':
        return mk('trainer_played', 'card', 'Se miraron cartas del mazo.', false);
      case 'POKEMON_PLACED':
        return mk('card_drawn', 'card', 'Un Pokémon fue puesto en la banca.', true);
      case 'POKEMON_SHUFFLED_INTO_DECK':
        return mk('card_drawn', 'card', 'Un Pokémon fue devuelto al mazo.', false);
      case 'CARD_DISCARDED':
        return mk('card_drawn', 'card', 'Se descartó una carta.', false);
      // Sudden Death
      case 'SUDDEN_DEATH_START': {
        const round = typeof p['suddenDeathRound'] === 'number' ? p['suddenDeathRound'] : 1;
        return mk('game_finished', 'game', `¡Muerte Súbita! Ronda ${round} — 1 carta de Premio cada uno.`, true);
      }
      default:
        return null;
    }
  }
}
