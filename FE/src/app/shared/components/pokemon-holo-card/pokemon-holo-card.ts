import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import type { TiltOptions } from 'vanilla-tilt';
import { HoloPointerDirective } from '../../directives/holo-pointer.directive';
import { VanillaTiltDirective } from '../../directives/vanilla-tilt.directive';
import type { CardFinish } from '../../utils/card-finish.util';

@Component({
  selector: 'app-pokemon-holo-card',
  imports: [VanillaTiltDirective, HoloPointerDirective],
  templateUrl: './pokemon-holo-card.html',
  styleUrl: './pokemon-holo-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PokemonHoloCard {
  readonly imageUrl = input.required<string>();
  readonly name = input.required<string>();
  readonly finish = input<CardFinish>('normal');
  readonly tiltOptions = input<Partial<TiltOptions> | undefined>(undefined);

  protected readonly defaultTiltOptions: Partial<TiltOptions> = {
    max: 18,
    speed: 500,
    perspective: 1000,
    scale: 1.04,
    glare: false,
    gyroscope: false,
  };

  protected onImgError(event: Event): void {
    (event.target as HTMLImageElement).classList.add('image-broken');
  }
}
