import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Card } from '../card/card';
import { BenchPokemonDto, CardInstanceDto, OpponentFieldDto, PlayerFieldDto } from '../models/board-state.dto';
import { ActionAvailabilityDto, AvailableAction } from '../models/game-state.dto';
import { InteractionMode } from '../services/game-state.service';
import { ActiveAnim } from '../services/board-animation.service';
import { DragKind, DragPayload } from '../services/drag-drop.service';
import { CardDragDirective } from '../directives/card-drag.directive';
import { isBasicPokemon, isEnergy, isEvolution, isTrainer } from '../services/card-classify';

/** Origin zone of a clicked card — used by `board-container` to route inspection/placement. */
export type CardClickOrigin = 'active' | 'bench' | 'hand' | 'prize' | 'discard' | 'deck';

/** Emitted when any card slot is clicked, identifying its origin and (for bench) index. */
export interface CardClickEvent {
  card: CardInstanceDto;
  origin: CardClickOrigin;
  index?: number;
}

/** Emitted when an empty/placement-target zone is clicked (active/bench placement targets). */
export interface ZoneClickEvent {
  type: 'active' | 'bench' | 'discard';
  index?: number;
}

/**
 * Dumb, reusable component: renders ONE half of the board (one player's
 * complete field — active, bench, prizes, deck, discard, hand) using the
 * shared `card` component. Instantiated TWICE by `board-container`: once
 * per `perspective: 'player'` (bottom, full hand visibility) and once per
 * `perspective: 'opponent'` (top, rotated 180°, hand shown as backs+count).
 */
@Component({
  selector: 'app-player-zone',
  templateUrl: './player-zone.html',
  styleUrl: './player-zone.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Card, CardDragDirective],
})
export class PlayerZone {
  /** Which half of the board this instance renders. */
  readonly perspective = input.required<'player' | 'opponent'>();
  /** Field DTO — `PlayerFieldDto` for perspective 'player', `OpponentFieldDto` for 'opponent'. */
  readonly field = input.required<PlayerFieldDto | OpponentFieldDto | null>();
  /** Display name shown near the zone (player info portrait area). */
  readonly playerName = input<string>('');
  /** Whether it's currently this side's turn — drives the active-card glow. */
  readonly isMyTurn = input(false);
  /** Current interaction mode — drives slot highlighting (e.g. placement targets). */
  readonly interactionMode = input<InteractionMode>('idle');
  /** Currently selected hand card's instanceId — highlights it in the fan. */
  readonly selectedCardId = input<string | null>(null);
  /** Instance id of the card that should show a soft red alert glow (PART 3). */
  readonly alertCardId = input<string | null>(null);
  /** Transient combat animation on this side's active slot (Slice 5). */
  readonly activeAnim = input<ActiveAnim>(null);
  /** Floating damage number to show over this side's active slot, or `null` for none (Slice 5). */
  readonly floatingDamage = input<number | null>(null);
  /** Retreat target-selection active: highlight bench Pokémon as promotable (Slice 1). */
  readonly retreatSelecting = input(false);
  /** Energy attachment active: highlight active + bench Pokémon as targets (Slice 2). */
  readonly energySelecting = input(false);
  /** Evolution active: highlight active + bench Pokémon as evolve targets (Slice 3). */
  readonly evolveSelecting = input(false);
  /** Tool attachment active: highlight active + bench Pokémon as tool targets. */
  readonly toolTargeting = input(false);
  /** Kind of hand card currently being dragged (drives drop-target highlight). Player side only. */
  readonly dragKind = input<DragKind | null>(null);
  /** Available actions — only rendered on perspective 'player'. */
  readonly actions = input<ActionAvailabilityDto | null>(null);

  /** A card slot was clicked — board-container opens the inspection overlay or routes a selection. */
  readonly cardClicked = output<CardClickEvent>();
  /** An empty/placement-target zone was clicked (active/bench placement during SETUP/MAIN). */
  readonly zoneClicked = output<ZoneClickEvent>();
  /** An action button was clicked — forwarded to board-container. */
  readonly execute = output<AvailableAction>();

  protected readonly isOpponent = computed(() => this.perspective() === 'opponent');

  protected readonly activePokemon = computed(() => this.field()?.activePokemon ?? null);

  /** Always exactly 5 bench slots — real Pokémon fill from the start, the rest render empty. */
  protected readonly benchSlots = computed<(BenchPokemonDto | null)[]>(() => {
    const bench = this.field()?.bench ?? [];
    return Array.from({ length: 5 }, (_, i) => bench[i] ?? null);
  });

  /** Always exactly 6 prize slots (face-down or empty). */
  protected readonly prizeSlots = computed<(string | CardInstanceDto | null)[]>(() => {
    const prizes = this.field()?.prizeCards ?? [];
    return Array.from({ length: 6 }, (_, i) => prizes[i] ?? null);
  });

  /** Remaining prize cards (non-null slots) — shown as a count badge over the prize pile. */
  protected readonly prizeCount = computed(() => this.prizeSlots().filter((p) => p !== null).length);

  /** Resolves a prize slot (string id for opponent, CardInstanceDto for player) to a card id for face-down art. */
  protected prizeCardId(prize: string | CardInstanceDto | null): string | null {
    if (!prize) return null;
    return typeof prize === 'string' ? prize : prize.cardId;
  }

  protected readonly discardTop = computed<CardInstanceDto | null>(() => {
    const pile = this.field()?.discardPile ?? [];
    return pile.length > 0 ? pile[pile.length - 1] : null;
  });

  protected readonly discardCount = computed(() => this.field()?.discardPile?.length ?? 0);

  protected readonly deckSize = computed(() => this.field()?.deckSize ?? 0);

  /** Player's full hand (only meaningful for perspective 'player'). */
  protected readonly hand = computed<CardInstanceDto[]>(() => {
    const f = this.field();
    if (!f || this.isOpponent()) return [];
    return (f as PlayerFieldDto).hand ?? [];
  });

  /** Opponent's hand size (only meaningful for perspective 'opponent'). */
  protected readonly opponentHandSize = computed(() => {
    const f = this.field();
    if (!f || !this.isOpponent()) return 0;
    return (f as OpponentFieldDto).handSize ?? 0;
  });

  protected readonly opponentHandBacks = computed(() => Array.from({ length: this.opponentHandSize() }));

  /** Whether the active/bench empty slots should highlight as placement targets. */
  protected readonly isPlacing = computed(() => {
    const mode = this.interactionMode();
    return !this.isOpponent() && (mode === 'selecting-target' || mode === 'play-basic');
  });

  protected isBasicPokemon(card: CardInstanceDto): boolean {
    return isBasicPokemon(card);
  }

  protected activeCardInstance(): CardInstanceDto | null {
    const active = this.activePokemon();
    if (!active) return null;
    return {
      instanceId: active.instanceId,
      cardId: active.cardId,
      name: active.cardId,
    };
  }

  protected benchCardInstance(slot: { instanceId: string; cardId: string } | null): CardInstanceDto | null {
    if (!slot) return null;
    return { instanceId: slot.instanceId, cardId: slot.cardId, name: slot.cardId };
  }

  protected benchHp(slot: { hp: number } | null): number | null {
    return slot ? slot.hp : null;
  }

  protected benchMaxHp(slot: { maxHp: number } | null): number | null {
    return slot ? slot.maxHp : null;
  }

  protected benchEnergyCount(slot: BenchPokemonDto | null): number {
    return slot?.attachedEnergies?.length ?? 0;
  }

  // ── Click handlers ──

  protected handleActiveClick(): void {
    const card = this.activeCardInstance();
    if (card) {
      this.cardClicked.emit({ card, origin: 'active' });
    } else {
      this.zoneClicked.emit({ type: 'active' });
    }
  }

  protected handleBenchClick(index: number): void {
    const slot = this.benchSlots()[index];
    const card = this.benchCardInstance(slot);
    if (card) {
      this.cardClicked.emit({ card, origin: 'bench', index });
    } else {
      this.zoneClicked.emit({ type: 'bench', index });
    }
  }

  protected handleHandCardClick(card: CardInstanceDto): void {
    this.cardClicked.emit({ card, origin: 'hand' });
  }

  protected handleDiscardClick(): void {
    const top = this.discardTop();
    if (top) {
      this.cardClicked.emit({ card: top, origin: 'discard' });
    }
  }

  // ── Drag & drop (player perspective only) ──

  /** Builds the drag payload for a hand card; `null` on the opponent side (its hand isn't draggable). */
  protected dragPayloadFor(card: CardInstanceDto): DragPayload | null {
    if (this.isOpponent()) return null;
    return { card, kind: this.dragKindFor(card) };
  }

  private dragKindFor(card: CardInstanceDto): DragKind {
    if (isEnergy(card)) return 'energy';
    if (isTrainer(card)) return 'trainer';
    if (isEvolution(card)) return 'evolution';
    return 'basic';
  }

  /** Whether the active slot is a valid drop target for the card currently being dragged. */
  protected isActiveDroppable(): boolean {
    const kind = this.dragKind();
    if (!kind || this.isOpponent()) return false;
    const hasActive = !!this.activePokemon();
    switch (kind) {
      case 'basic': return !hasActive;            // place a basic into an empty active
      case 'energy':
      case 'evolution': return hasActive;          // target the active Pokémon
      case 'trainer': return true;                 // trainers can be dropped anywhere on your field
    }
  }

  /** Whether a given bench slot is a valid drop target for the card currently being dragged. */
  protected isBenchSlotDroppable(slot: BenchPokemonDto | null): boolean {
    const kind = this.dragKind();
    if (!kind || this.isOpponent()) return false;
    switch (kind) {
      case 'basic': return slot === null;          // empty bench slot
      case 'energy':
      case 'evolution': return slot !== null;       // a benched Pokémon
      case 'trainer': return true;
    }
  }
}
