import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { HoverSoundDirective } from '../../../../shared/directives/hover-sound.directive';

/**
 * Controles de paginación de la grilla de cartas.
 * `page` es 0-indexed (igual que el Pageable del backend).
 */
@Component({
  selector: 'app-pagination',
  imports: [HoverSoundDirective],
  templateUrl: './pagination.html',
  styleUrl: './pagination.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pagination {
  readonly page = input.required<number>();
  readonly size = input.required<number>();
  readonly total = input.required<number>();

  readonly pageChange = output<number>();

  protected readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.size())));

  protected readonly visiblePages = computed(() => {
    const total = this.totalPages();
    const current = this.page();
    const maxVisible = 5;
    let start = Math.max(0, current - Math.floor(maxVisible / 2));
    const end = Math.min(total, start + maxVisible);
    start = Math.max(0, end - maxVisible);
    return Array.from({ length: end - start }, (_, i) => start + i);
  });

  protected readonly rangeLabel = computed(() => {
    if (this.total() === 0) return 'Mostrando 0 cartas';
    const from = this.page() * this.size() + 1;
    const to = Math.min(this.total(), (this.page() + 1) * this.size());
    return `Mostrando ${from}-${to} de ${this.total()}`;
  });

  protected goTo(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.page()) return;
    this.pageChange.emit(page);
  }
}
