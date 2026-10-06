import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DeckBuilderCard } from '../../models/deck-builder-state.model';

@Component({
  selector: 'app-deck-current-list',
  imports: [],
  templateUrl: './deck-current-list.html',
  styleUrl: './deck-current-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckCurrentList {
  readonly deckCards = input<DeckBuilderCard[]>([]);

  readonly increaseQuantity = output<string>();
  readonly decreaseQuantity = output<string>();
  readonly removeCard = output<string>();

  protected onImgError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.classList.add('image-broken');
  }

  protected onIncrease(cardId: string): void {
    this.increaseQuantity.emit(cardId);
  }

  protected onDecrease(cardId: string): void {
    this.decreaseQuantity.emit(cardId);
  }

  protected onRemove(cardId: string): void {
    this.removeCard.emit(cardId);
  }
}
