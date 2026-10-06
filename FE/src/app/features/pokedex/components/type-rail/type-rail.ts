import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { POKEMON_TYPES, PokemonTypeInfo } from '../../domain/constants/pokemon-types';
import { HoverSoundDirective } from '../../../../shared/directives/hover-sound.directive';

/**
 * Columna angosta izquierda con los iconos de tipo Pokémon (filtro rápido por tipo).
 */
@Component({
  selector: 'app-type-rail',
  imports: [HoverSoundDirective],
  templateUrl: './type-rail.html',
  styleUrl: './type-rail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TypeRail {
  readonly activeType = input<string | null>(null);
  readonly typeToggle = output<string | null>();

  protected readonly types: PokemonTypeInfo[] = POKEMON_TYPES;

  protected onTypeClick(typeId: string): void {
    this.typeToggle.emit(this.activeType() === typeId ? null : typeId);
  }
}
