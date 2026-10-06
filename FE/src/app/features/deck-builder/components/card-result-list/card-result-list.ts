import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CardResponse } from '../../models/card.model';

const TYPE_COLORS: Record<string, string> = {
  Colorless: '#a8a878',
  Darkness: '#705848',
  Dragon: '#7038f8',
  Fairy: '#ee99ac',
  Fighting: '#c03028',
  Fire: '#f08030',
  Grass: '#78c850',
  Lightning: '#f8d030',
  Metal: '#b8b8d0',
  Psychic: '#f85888',
  Water: '#6890f0',
};

@Component({
  selector: 'app-card-result-list',
  imports: [],
  templateUrl: './card-result-list.html',
  styleUrl: './card-result-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardResultList {
  readonly cards = input<CardResponse[]>([]);
  readonly loading = input<boolean>(false);
  readonly error = input<string | null>(null);

  readonly addCard = output<CardResponse>();
  readonly retry = output<void>();

  protected typeColor(type: string): string {
    return TYPE_COLORS[type] ?? '#7a8ab5';
  }

  protected onImgError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.classList.add('image-broken');
  }

  protected onAddClick(card: CardResponse): void {
    this.addCard.emit(card);
  }

  protected onRetryClick(): void {
    this.retry.emit();
  }
}
