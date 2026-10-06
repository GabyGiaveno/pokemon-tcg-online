import {
  AfterViewInit,
  Directive,
  ElementRef,
  NgZone,
  OnDestroy,
  inject,
  input,
} from '@angular/core';
import VanillaTilt from 'vanilla-tilt';

type TiltOptions = {
  [key: string]: unknown;
};

declare module 'vanilla-tilt' {
  export interface TiltOptions {
    [key: string]: unknown;
  }

  export const VanillaTilt: {
    init(element: HTMLElement, options?: Partial<TiltOptions>): void;
  };
}

type VanillaTiltElement = HTMLElement & {
  vanillaTilt?: {
    destroy: () => void;
  };
};

@Directive({
  selector: '[appVanillaTilt]',
})
export class VanillaTiltDirective implements AfterViewInit, OnDestroy {
  readonly appVanillaTilt = input<Partial<TiltOptions>>({});
  private readonly elementRef = inject(ElementRef<VanillaTiltElement>);
  private readonly ngZone = inject(NgZone);

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      VanillaTilt.init(this.elementRef.nativeElement, {
        max: 14,
        speed: 450,
        perspective: 900,
        scale: 1.03,
        glare: true,
        'max-glare': 0.35,
        gyroscope: false,
        ...this.appVanillaTilt(),
      });
    });
  }

  ngOnDestroy(): void {
    this.elementRef.nativeElement.vanillaTilt?.destroy();
  }
}
