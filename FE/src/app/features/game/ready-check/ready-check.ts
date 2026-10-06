import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

/**
 * Opening READY_CHECK screen: both players press "Listo", then the coin-flip winner
 * chooses who takes the first turn. Dumb component — the board wires the inputs from
 * game state and dispatches the `ready` / `choose` outputs to the backend.
 */
@Component({
  selector: 'app-ready-check',
  templateUrl: './ready-check.html',
  styleUrl: './ready-check.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReadyCheck {
  /** Whether THIS player already pressed "Listo". */
  readonly myReady = input(false);
  /** Whether the opponent already pressed "Listo". */
  readonly opponentReady = input(false);
  /** True once both are ready and the coin has been flipped (a winner exists). */
  readonly coinDecided = input(false);
  /** True when THIS player won the coin flip (and therefore chooses who starts). */
  readonly iWonCoin = input(false);

  readonly ready = output<void>();
  readonly choose = output<'ME' | 'OPPONENT'>();

  /** Drives which block the template shows. */
  protected readonly stage = computed<'press-ready' | 'waiting-opponent' | 'choose' | 'waiting-choice'>(() => {
    if (this.coinDecided()) {
      return this.iWonCoin() ? 'choose' : 'waiting-choice';
    }
    return this.myReady() ? 'waiting-opponent' : 'press-ready';
  });
}
