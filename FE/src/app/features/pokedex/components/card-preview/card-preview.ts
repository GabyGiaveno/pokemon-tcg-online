import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { PokedexCard } from '../../domain/models/pokedex-card';
import { getTypeColor } from '../../domain/constants/pokemon-types';
import { HoverSoundDirective } from '../../../../shared/directives/hover-sound.directive';

/**
 * Preview de una carta dentro de la grilla central.
 */
@Component({
  selector: 'app-card-preview',
  imports: [HoverSoundDirective],
  templateUrl: './card-preview.html',
  styleUrl: './card-preview.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardPreview {
  readonly card = input.required<PokedexCard>();
  readonly selected = input<boolean>(false);

  readonly select = output<PokedexCard>();

  protected get accentColor(): string {
    const types = this.card().types;
    return types.length > 0 ? getTypeColor(types[0]) : '#a8a878';
  }

  protected onClick(): void {
    this.select.emit(this.card());
  }

  protected onImgError(event: Event): void {
    (event.target as HTMLImageElement).classList.add('image-broken');
  }
}
