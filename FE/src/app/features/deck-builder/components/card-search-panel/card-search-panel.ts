import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { CardSearchParams } from '../../models/card.model';

@Component({
  selector: 'app-card-search-panel',
  imports: [],
  templateUrl: './card-search-panel.html',
  styleUrl: './card-search-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardSearchPanel {
  readonly searchTerm = input<string>('');
  readonly loading = input<boolean>(false);

  readonly searchChange = output<string>();
  readonly filterChange = output<CardSearchParams>();

  protected readonly supertypes: string[] = ['Pokémon', 'Trainer', 'Energy'];
  protected readonly types: string[] = [
    'Colorless', 'Darkness', 'Dragon', 'Fairy', 'Fighting',
    'Fire', 'Grass', 'Lightning', 'Metal', 'Psychic', 'Water',
  ];

  protected readonly selectedSupertype = signal<string>('');
  protected readonly selectedType = signal<string>('');

  protected readonly typeColors: Record<string, string> = {
    Colorless: '#a8a878',
    Darkness: '#705848',
    Dragon:   '#7038f8',
    Fairy:    '#ee99ac',
    Fighting: '#c03028',
    Fire:     '#f08030',
    Grass:    '#78c850',
    Lightning:'#f8d030',
    Metal:    '#b8b8d0',
    Psychic:  '#f85888',
    Water:    '#6890f0',
  };

  protected onSearchInput(value: string): void {
    this.searchChange.emit(value);
  }

  protected onSupertypeChange(value: string): void {
    this.selectedSupertype.set(value);
    this.emitFilter();
  }

  protected onTypeSelect(type: string): void {
    this.selectedType.set(type);
    this.emitFilter();
  }

  private emitFilter(): void {
    const params: CardSearchParams = {};
    const supertype = this.selectedSupertype();
    const type = this.selectedType();
    if (supertype) params.supertype = supertype;
    if (type) params.type = type;
    this.filterChange.emit(params);
  }
}
