import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { PokedexFilters } from '../../domain/models/pokedex-filters';
import { getTypeColor } from '../../domain/constants/pokemon-types';
import { HoverSoundDirective } from '../../../../shared/directives/hover-sound.directive';

export interface SetOption {
  id: string;
  name: string;
}

interface FilterChip {
  kind: 'name' | 'type' | 'supertype' | 'set';
  label: string;
  color?: string;
}

/**
 * Franja inferior — búsqueda, selector de expansión, supertype y chips de filtros activos.
 * Mientras no exista GET /api/cards/sets, `sets` puede recibir una lista corta predefinida.
 */
@Component({
  selector: 'app-filter-bar',
  imports: [HoverSoundDirective],
  templateUrl: './filter-bar.html',
  styleUrl: './filter-bar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilterBar {
  readonly filters = input.required<PokedexFilters>();
  readonly sets = input<SetOption[]>([
    { id: 'xy1', name: 'XY Base Set' },
    { id: 'xy2', name: 'Flashfire' },
  ]);
  readonly searchChange = output<string>();
  readonly setChange = output<string | null>();
  readonly supertypeToggle = output<string | null>();
  readonly clearFilters = output<void>();
  readonly removeChip = output<FilterChip>();

  protected readonly supertypes: { id: string; label: string; icon: string }[] = [
    { id: 'Pokémon', label: 'Pokémon', icon: '🐾' },
    { id: 'Trainer', label: 'Entrenador', icon: '🎒' },
    { id: 'Energy', label: 'Energía', icon: '⚡' },
  ];

  protected readonly activeChips = computed<FilterChip[]>(() => {
    const f = this.filters();
    const chips: FilterChip[] = [];
    if (f.name) chips.push({ kind: 'name', label: `"${f.name}"` });
    if (f.type) chips.push({ kind: 'type', label: f.type, color: getTypeColor(f.type) });
    if (f.supertype) chips.push({ kind: 'supertype', label: f.supertype });
    if (f.set) chips.push({ kind: 'set', label: f.set });
    return chips;
  });

  protected onSearchInput(value: string): void {
    this.searchChange.emit(value);
  }

  protected onSetChange(value: string): void {
    this.setChange.emit(value || null);
  }

  protected onSupertypeClick(id: string): void {
    this.supertypeToggle.emit(this.filters().supertype === id ? null : id);
  }


}
