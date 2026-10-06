import { ChangeDetectionStrategy, Component, computed, inject, resource, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { lastValueFrom, map } from 'rxjs';
import { TypeRail } from '../../components/type-rail/type-rail';
import { CardGrid } from '../../components/card-grid/card-grid';
import { CardDetailPanel } from '../../components/card-detail-panel/card-detail-panel';
import { FilterBar } from '../../components/filter-bar/filter-bar';
import { PokedexCard } from '../../domain/models/pokedex-card';
import { PokedexFilters } from '../../domain/models/pokedex-filters';
import { PokedexApi } from '../../data-access/services/pokedex-api.service';
import { POKEDEX_API_PROVIDERS } from '../../data-access/services/pokedex-api.provider';
import { mapCardPageResponse } from '../../data-access/mappers/card-response.mapper';
import { SettingsMenu } from '../../../../shared/components/settings-menu/settings-menu';
import { HoverSoundDirective } from '../../../../shared/directives/hover-sound.directive';
import { BackgroundMusicService } from '../../../../shared/services/background-music.service';

const PAGE_SIZE = 16;

@Component({
  selector: 'app-pokedex-page',
  imports: [TypeRail, CardGrid, CardDetailPanel, FilterBar, RouterLink, SettingsMenu, HoverSoundDirective],
  templateUrl: './pokedex-page.html',
  styleUrl: './pokedex-page.css',
  providers: [...POKEDEX_API_PROVIDERS],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PokedexPage {
  private readonly pokedexApi = inject(PokedexApi);
  private readonly backgroundMusic = inject(BackgroundMusicService);

  protected readonly filters = signal<PokedexFilters>({
    name: '',
    type: null,
    supertype: null,
    set: null,
  });

  protected readonly page = signal(0);
  protected readonly pageSize = PAGE_SIZE;

  protected readonly selectedCard = signal<PokedexCard | null>(null);

  protected readonly cardsResource = resource({
    params: () => ({ filters: this.filters(), page: this.page() }),
    loader: ({ params }) => {
      const reqParams = {
        name: params.filters.name || undefined,
        type: params.filters.type || undefined,
        supertype: params.filters.supertype || undefined,
        set: params.filters.set || undefined,
        page: params.page,
        size: this.pageSize,
      };
      return lastValueFrom(
        this.pokedexApi.getCards(reqParams).pipe(map(mapCardPageResponse)),
      );
    },
    defaultValue: { cards: [] as PokedexCard[], total: 0, page: 0, size: PAGE_SIZE },
  });

  constructor() {
    this.backgroundMusic.play();
  }

  protected readonly error = computed<string | null>(() => {
    const err = this.cardsResource.error();
    if (!err) return null;
    if (err instanceof Error) return err.message;
    return String(err);
  });

  protected onTypeToggle(type: string | null): void {
    this.filters.update(f => ({ ...f, type }));
    this.page.set(0);
  }

  protected onSearchChange(name: string): void {
    this.filters.update(f => ({ ...f, name }));
    this.page.set(0);
  }

  protected onSetChange(set: string | null): void {
    this.filters.update(f => ({ ...f, set }));
    this.page.set(0);
  }

  protected onSupertypeToggle(supertype: string | null): void {
    this.filters.update(f => ({ ...f, supertype }));
    this.page.set(0);
  }

  protected onClearFilters(): void {
    this.filters.set({ name: '', type: null, supertype: null, set: null });
    this.page.set(0);
  }

  protected onRemoveChip(chip: { kind: 'name' | 'type' | 'supertype' | 'set' }): void {
    switch (chip.kind) {
      case 'name':
        this.onSearchChange('');
        break;
      case 'type':
        this.onTypeToggle(null);
        break;
      case 'supertype':
        this.onSupertypeToggle(null);
        break;
      case 'set':
        this.onSetChange(null);
        break;
    }
  }

  protected onCardSelect(card: PokedexCard): void {
    this.selectedCard.set(card);
  }

  protected onPageChange(page: number): void {
    this.page.set(page);
  }

  protected onRetry(): void {
    this.cardsResource.reload();
  }

}
