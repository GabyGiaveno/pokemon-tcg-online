import { Directive, ElementRef, inject, input, output } from '@angular/core';
import { DragDropService, DragPayload } from '../services/drag-drop.service';

/** Pointer travel (px) beyond which a press becomes a drag instead of a tap. */
const DRAG_THRESHOLD_PX = 6;

/**
 * Makes a hand card both tappable and draggable with Pointer Events (mouse + touch, no libs).
 *
 * <p>A press released without crossing {@link DRAG_THRESHOLD_PX} emits {@link #tap} (the board
 * opens inspection). Crossing the threshold starts a drag through {@link DragDropService}; on
 * release the drop-zone token under the pointer (its `data-drop` attribute) is resolved via
 * hit-testing and reported as a drop. The element captures the pointer so the gesture keeps
 * tracking even when the finger/cursor leaves the card.
 */
@Directive({
  selector: '[appCardDrag]',
  host: {
    '(pointerdown)': 'onPointerDown($event)',
    '(pointermove)': 'onPointerMove($event)',
    '(pointerup)': 'onPointerUp($event)',
    '(pointercancel)': 'onPointerCancel($event)',
    '(dragstart)': 'onDragStart($event)',
    '[style.touch-action]': '"none"',
    '[style.user-select]': '"none"',
    '[class.app-card-drag--dragging]': 'dragging',
  },
})
export class CardDragDirective {
  /** Card + kind to drag. When `null`, the element still taps but cannot be dragged. */
  readonly appCardDrag = input<DragPayload | null>(null);
  /** Emitted on a tap (press + release without dragging) — the board opens inspection. */
  readonly tap = output<void>();

  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly dragDrop = inject(DragDropService);

  protected dragging = false;
  private pointerId: number | null = null;
  private startX = 0;
  private startY = 0;

  protected onPointerDown(event: PointerEvent): void {
    // Mouse: left button only. Touch/pen: always.
    if (event.pointerType === 'mouse' && event.button !== 0) {
      return;
    }
    this.pointerId = event.pointerId;
    this.startX = event.clientX;
    this.startY = event.clientY;
    this.dragging = false;
    this.host.nativeElement.setPointerCapture(event.pointerId);
  }

  protected onPointerMove(event: PointerEvent): void {
    if (this.pointerId !== event.pointerId) {
      return;
    }
    const payload = this.appCardDrag();
    if (!payload) {
      return;
    }
    if (this.dragging) {
      this.dragDrop.move(event.clientX, event.clientY);
      return;
    }
    const travelled = Math.hypot(event.clientX - this.startX, event.clientY - this.startY);
    if (travelled >= DRAG_THRESHOLD_PX) {
      this.dragging = true;
      this.dragDrop.begin(payload, event.clientX, event.clientY);
    }
  }

  protected onPointerUp(event: PointerEvent): void {
    if (this.pointerId !== event.pointerId) {
      return;
    }
    const wasDragging = this.dragging;
    const x = event.clientX;
    const y = event.clientY;
    this.release(event);

    if (wasDragging) {
      const zone = this.findDropZone(x, y);
      this.dragDrop.drop(zone?.dataset['drop'] ?? null, zone ? this.centerOf(zone) : null);
    } else {
      this.tap.emit();
    }
  }

  /** Cancels the browser's native image/element drag so it never preempts our pointer drag. */
  protected onDragStart(event: DragEvent): void {
    event.preventDefault();
  }

  protected onPointerCancel(event: PointerEvent): void {
    if (this.pointerId !== event.pointerId) {
      return;
    }
    const wasDragging = this.dragging;
    this.release(event);
    if (wasDragging) {
      this.dragDrop.cancel();
    }
  }

  private release(event: PointerEvent): void {
    const el = this.host.nativeElement;
    if (this.pointerId !== null && el.hasPointerCapture(event.pointerId)) {
      el.releasePointerCapture(event.pointerId);
    }
    this.pointerId = null;
    this.dragging = false;
  }

  /** Hit-tests the released point for the nearest element carrying a `data-drop` token. */
  private findDropZone(x: number, y: number): HTMLElement | null {
    const element = document.elementFromPoint(x, y);
    return element?.closest<HTMLElement>('[data-drop]') ?? null;
  }

  /** Viewport-space center of an element (the ghost glides here on drop). */
  private centerOf(element: HTMLElement): { x: number; y: number } {
    const rect = element.getBoundingClientRect();
    return { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 };
  }
}
