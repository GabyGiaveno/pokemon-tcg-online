import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DeckValidationResponse } from '../../models/deck-validation.model';

@Component({
  selector: 'app-deck-validation-panel',
  imports: [],
  templateUrl: './deck-validation-panel.html',
  styleUrl: './deck-validation-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckValidationPanel {
  readonly validation = input<DeckValidationResponse | null>(null);
  readonly totalCards = input<number>(0);

  protected countPercent(): number {
    const total = this.totalCards();
    if (total <= 0) return 0;
    return Math.min(100, (total / 60) * 100);
  }
}
