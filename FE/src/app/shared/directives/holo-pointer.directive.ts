import { AfterViewInit, Directive, ElementRef, NgZone, OnDestroy, inject } from '@angular/core';

/**
 * Updates CSS variables used by the holographic card layers.
 *
 * Important: this directive never writes transform/rotate values. VanillaTilt
 * remains the only owner of 3D movement.
 */
@Directive({
  selector: '[appHoloPointer]',
})
export class HoloPointerDirective implements AfterViewInit, OnDestroy {
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly ngZone = inject(NgZone);
  private readonly host = this.elementRef.nativeElement;

  private readonly pointerMoveHandler = (event: PointerEvent) => this.updatePointer(event);
  private readonly resetHandler = () => this.resetPointer();

  ngAfterViewInit(): void {
    this.resetPointer();

    this.ngZone.runOutsideAngular(() => {
      this.host.addEventListener('pointermove', this.pointerMoveHandler, { passive: true });
      this.host.addEventListener('pointerleave', this.resetHandler);
      this.host.addEventListener('pointercancel', this.resetHandler);
    });
  }

  ngOnDestroy(): void {
    this.host.removeEventListener('pointermove', this.pointerMoveHandler);
    this.host.removeEventListener('pointerleave', this.resetHandler);
    this.host.removeEventListener('pointercancel', this.resetHandler);
  }

  private updatePointer(event: PointerEvent): void {
    const rect = this.host.getBoundingClientRect();
    const width = rect.width || 1;
    const height = rect.height || 1;

    const px = this.clamp((event.clientX - rect.left) / width, 0, 1);
    const py = this.clamp((event.clientY - rect.top) / height, 0, 1);
    const posx = px * 100;
    const posy = py * 100;
    const dx = px - 0.5;
    const dy = py - 0.5;
    const hyp = Math.min(1, Math.sqrt(dx * dx + dy * dy) * 2);

    this.host.style.setProperty('--mx', `${posx}%`);
    this.host.style.setProperty('--my', `${posy}%`);
    this.host.style.setProperty('--posx', `${posx}%`);
    this.host.style.setProperty('--posy', `${posy}%`);
    this.host.style.setProperty('--hyp', `${hyp}`);
    this.host.style.setProperty('--holo-opacity', `${0.18 + hyp * 0.42}`);
  }

  private resetPointer(): void {
    this.host.style.setProperty('--mx', '50%');
    this.host.style.setProperty('--my', '50%');
    this.host.style.setProperty('--posx', '50%');
    this.host.style.setProperty('--posy', '50%');
    this.host.style.setProperty('--hyp', '0');
    this.host.style.setProperty('--holo-opacity', '0.18');
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(max, Math.max(min, value));
  }
}
