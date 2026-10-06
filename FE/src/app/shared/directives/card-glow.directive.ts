import { Directive, ElementRef, HostListener, NgZone, Renderer2, RendererStyleFlags2, inject } from '@angular/core';

/**
 * Tracks the cursor position over a `.card` element and exposes it as
 * `--glow-x` / `--glow-y` CSS custom properties (percentages), consumed by
 * the `.card::before` radial-gradient glow layer defined in `styles.css`.
 */
@Directive({
  selector: '[appCardGlow]',
})
export class CardGlowDirective {
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly renderer = inject(Renderer2);
  private readonly ngZone = inject(NgZone);

  @HostListener('mousemove', ['$event'])
  onMouseMove(event: MouseEvent): void {
    this.ngZone.runOutsideAngular(() => {
      const rect = this.elementRef.nativeElement.getBoundingClientRect();
      const width = rect.width || 1;
      const height = rect.height || 1;
      const x = ((event.clientX - rect.left) / width) * 100;
      const y = ((event.clientY - rect.top) / height) * 100;

      this.setGlowPosition(`${x}%`, `${y}%`);
    });
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    this.ngZone.runOutsideAngular(() => {
      this.setGlowPosition('50%', '50%');
    });
  }

  private setGlowPosition(x: string, y: string): void {
    this.renderer.setStyle(this.elementRef.nativeElement, '--glow-x', x, RendererStyleFlags2.DashCase);
    this.renderer.setStyle(this.elementRef.nativeElement, '--glow-y', y, RendererStyleFlags2.DashCase);
  }
}
