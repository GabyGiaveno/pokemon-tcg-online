import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { PokedexCard } from '../../domain/models/pokedex-card';
import { getTypeColor } from '../../domain/constants/pokemon-types';
import { PokemonHoloCard } from '../../../../shared/components/pokemon-holo-card/pokemon-holo-card';
import { getCardFinish, type CardFinish } from '../../../../shared/utils/card-finish.util';

/**
 * Columna derecha — ficha de la carta seleccionada (carta ampliada + descripción).
 */
@Component({
  selector: 'app-card-detail-panel',
  imports: [PokemonHoloCard],
  templateUrl: './card-detail-panel.html',
  styleUrl: './card-detail-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'closeFullscreen()',
  },
})
export class CardDetailPanel {
  readonly card = input<PokedexCard | null>(null);
  readonly loading = input<boolean>(false);

  protected readonly fullscreenOpen = signal(false);

  protected readonly fullscreenTiltOptions = {
    max: 18,
    speed: 500,
    perspective: 1000,
    scale: 1.04,
    glare: false,
    gyroscope: false,
  };

  protected getTypeColor(type: string): string {
    return getTypeColor(type);
  }

  protected getFinish(card: PokedexCard): CardFinish {
    return getCardFinish(card);
  }

  protected onImgError(event: Event): void {
    (event.target as HTMLImageElement).classList.add('image-broken');
  }

  protected openFullscreen(): void {
    this.fullscreenOpen.set(true);
  }

  protected closeFullscreen(): void {
    this.fullscreenOpen.set(false);
  }
}
