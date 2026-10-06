import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Fullscreen loading overlay with a spinning Poké Ball, themed to match the game
 * (gold trim + serif caption). Reused as a transition screen (e.g. login → lobby).
 */
@Component({
  selector: 'app-pokeball-loader',
  templateUrl: './pokeball-loader.html',
  styleUrl: './pokeball-loader.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PokeballLoader {
  /** Caption shown under the spinning Poké Ball. */
  readonly caption = input<string>('Cargando…');
}
