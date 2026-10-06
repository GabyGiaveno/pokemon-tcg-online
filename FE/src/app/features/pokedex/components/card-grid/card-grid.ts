import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { PokedexCard } from '../../domain/models/pokedex-card';
import { CardPreview } from '../card-preview/card-preview';
import { Pagination } from '../pagination/pagination';

/**
 * Grilla central de cartas con paginación, estados de carga/vacío/error.
 */
@Component({
  selector: 'app-card-grid',
  imports: [CardPreview, Pagination],
  templateUrl: './card-grid.html',
  styleUrl: './card-grid.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardGrid {
  readonly cards = input<PokedexCard[]>([]);
  readonly selectedId = input<string | null>(null);
  readonly loading = input<boolean>(false);
  readonly error = input<string | null>(null);

  readonly page = input<number>(0);
  readonly size = input<number>(20);
  readonly total = input<number>(0);

  readonly cardSelect = output<PokedexCard>();
  readonly pageChange = output<number>();
  readonly retry = output<void>();

  protected readonly skeletonItems = Array.from({ length: 12 }, (_, i) => i);

  protected onCardSelect(card: PokedexCard): void {
    this.cardSelect.emit(card);
  }
}
