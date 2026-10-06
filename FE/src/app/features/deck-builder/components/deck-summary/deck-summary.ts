import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-deck-summary',
  imports: [],
  templateUrl: './deck-summary.html',
  styleUrl: './deck-summary.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckSummary {
  readonly deckName = input<string>('');
  readonly totalCards = input<number>(0);
  readonly uniqueCards = input<number>(0);
  readonly canSave = input<boolean>(false);
  readonly canClear = input<boolean>(false);
  readonly saving = input<boolean>(false);
  readonly isEditMode = input<boolean>(false);

  readonly save = output<void>();
  readonly clearDeck = output<void>();

  protected onSaveClick(): void {
    this.save.emit();
  }

  protected onClearClick(): void {
    this.clearDeck.emit();
  }
}
