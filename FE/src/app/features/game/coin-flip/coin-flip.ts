import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Coin face the flip lands on. */
export type CoinResult = 'HEADS' | 'TAILS';

/**
 * Reusable coin-flip animation (CSS-only, no libs). Spins a 3D coin and lands on
 * {@link result}, showing "Cara"/"Cruz". Used both for card-triggered flips
 * (COIN_FLIPPED events) and the opening who-goes-first flip.
 */
@Component({
  selector: 'app-coin-flip',
  templateUrl: './coin-flip.html',
  styleUrl: './coin-flip.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CoinFlip {
  /** Face the coin lands on. */
  readonly result = input.required<CoinResult>();
  /** Optional caption under the coin (e.g. "Empezás vos" for the opening flip). */
  readonly caption = input<string>('');
}
