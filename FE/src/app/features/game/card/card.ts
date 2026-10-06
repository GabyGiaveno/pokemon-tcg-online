import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';
import { CardInstanceDto } from '../models/board-state.dto';
import { resolveCardArt } from '../services/card-art';
import { CardGlowDirective } from '../../../shared/directives/card-glow.directive';
import { VanillaTiltDirective } from '../../../shared/directives/vanilla-tilt.directive';

/** Visual size variant — drives fixed dimensions in `card.css`, all at 2.5:3.5 ratio. */
export type CardSize = 'active' | 'bench' | 'hand' | 'prize' | 'pile';

/**
 * Dumb, reusable component: renders a single card slot at a fixed 2.5:3.5
 * ratio for any zone (active/bench/hand/prize/deck/discard). Resolves art
 * via the deterministic `resolveCardArt`, handles face-down backs and
 * broken-image fallbacks, and carries the cursor glow + tilt directives.
 */
@Component({
  selector: 'app-card',
  templateUrl: './card.html',
  styleUrl: './card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CardGlowDirective, VanillaTiltDirective],
})
export class Card {
  /** Full card data, when known. `null` renders an empty slot outline. */
  readonly card = input<CardInstanceDto | null>(null);
  /** Card id only — used for face-down piles (prize/deck) where full data isn't available. */
  readonly cardId = input<string | null>(null);
  /** Renders the card back instead of art; no glow/tilt for face-down cards. */
  readonly faceDown = input(false);
  /** Size variant — maps to fixed dimensions in `card.css`. */
  readonly size = input<CardSize>('bench');
  /** Highlights the card as the current selection. */
  readonly selected = input(false);
  /** Shows a gold "playable" ring (e.g. hand basics in MAIN). */
  readonly playable = input(false);
  /** Current HP, shown on the HP bar of active/bench cards. */
  readonly hp = input<number | null>(null);
  /** Max HP — drives the HP bar fill ratio. */
  readonly maxHp = input<number | null>(null);
  /** Number of energies attached — shown as a small ⚡ counter. */
  readonly energyCount = input(0);
  /** Status condition badges (ZZZ/PAR/BRN/PSN/CFZ). */
  readonly conditions = input<string[]>([]);
  /** Index of this card within a fanned hand (0-based). */
  readonly fanIndex = input(0);
  /** Total number of cards in the fanned hand. */
  readonly fanTotal = input(1);
  /** Soft red glow/pulse — shown when a dispatched action targeting this card failed. */
  readonly alert = input(false);

  readonly cardClick = output<CardInstanceDto>();

  protected readonly artUrl = computed(() => resolveCardArt(this.card()?.cardId ?? this.cardId() ?? ''));
  protected readonly hasArt = computed(() => this.artUrl().length > 0);
  protected readonly displayName = computed(() => this.card()?.name ?? '');

  /** Whether to render the HP bar (active/bench cards with HP data). */
  protected readonly showHp = computed(() => this.hp() !== null && (this.maxHp() ?? 0) > 0);
  /** HP fill percentage (0-100). */
  protected readonly hpPct = computed(() => {
    const max = this.maxHp() ?? 0;
    const cur = this.hp() ?? 0;
    return max > 0 ? Math.max(0, Math.min(100, Math.round((cur / max) * 100))) : 0;
  });

  /** Set to true when the art `<img>` fails to load (404) — toggles the placeholder fallback. */
  protected readonly imageBroken = signal(false);

  constructor() {
    // Reset the broken-image flag whenever the resolved art URL changes
    // (e.g. the same `card` instance is reused for a different card via @for track).
    effect(() => {
      this.artUrl();
      this.imageBroken.set(false);
    });
  }

  protected onImgError(): void {
    this.imageBroken.set(true);
  }

  protected handleClick(): void {
    const c = this.card();
    if (c) {
      this.cardClick.emit(c);
    }
  }
}
