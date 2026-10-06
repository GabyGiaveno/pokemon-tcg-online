import { Directive, HostListener, inject, input } from '@angular/core';
import { SoundService } from '../services/sound.service';

/**
 * Plays a short blip when the pointer enters the host element.
 * Apply to interactive elements (buttons, nav items, cards) — respects the
 * global sound on/off preference via {@link SoundService}.
 *
 * Use `soundVariant="card"` for the softer card-hover "boop"; the default
 * `"button"` plays the techy click blip.
 */
@Directive({
  selector: '[appHoverSound]',
})
export class HoverSoundDirective {
  private readonly sound = inject(SoundService);

  /** Which blip to play: the techy button beep (default) or the softer card "boop". */
  readonly soundVariant = input<'button' | 'card'>('button');

  @HostListener('mouseenter')
  protected onMouseEnter(): void {
    if (this.soundVariant() === 'card') {
      this.sound.playCardHover();
    } else {
      this.sound.playHover();
    }
  }
}
