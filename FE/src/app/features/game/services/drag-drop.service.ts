import { Injectable, OnDestroy, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { CardInstanceDto } from '../models/board-state.dto';

/** What kind of hand card is being dragged — decides the valid drop targets. */
export type DragKind = 'basic' | 'energy' | 'evolution' | 'trainer';

/** The card currently being dragged from hand. */
export interface DragPayload {
  readonly card: CardInstanceDto;
  readonly kind: DragKind;
}

/** A completed drag: the dragged card and the drop-zone token it was released over (null = nowhere). */
export interface DropEvent {
  readonly payload: DragPayload;
  /** Drop-zone token read from `data-drop` — 'ACTIVE' | 'BENCH_n', or null if released off any zone. */
  readonly target: string | null;
}

/**
 * Presentation-only drag-and-drop coordinator for hand cards (Point 4: drag-to-play).
 *
 * <p>Holds the live drag {@link #payload} and {@link #ghost} position (signals the board binds
 * to for the floating preview) and emits completed drops on {@link #drops$}. It does NOT validate
 * rules or mutate game state — the board routes a drop to a dispatch and the backend stays
 * authoritative (ARCHITECTURE.md). Component-provided so it is torn down with the board.
 */
@Injectable()
export class DragDropService implements OnDestroy {
  /** How long the ghost glides toward the drop target before vanishing (ms). */
  private static readonly SETTLE_MS = 360;

  /** The card being dragged, or `null` when no drag is in progress. */
  readonly payload = signal<DragPayload | null>(null);
  /** Viewport position of the drag ghost (follows the pointer), or `null`. */
  readonly ghost = signal<{ x: number; y: number } | null>(null);
  /** True while the ghost is gliding to the drop target (drives the CSS transition). */
  readonly settling = signal(false);

  private readonly dropsSubject = new Subject<DropEvent>();
  /** Emits once per completed drag (after pointer release). */
  readonly drops$ = this.dropsSubject.asObservable();

  private settleTimer: ReturnType<typeof setTimeout> | null = null;

  /** Begins a drag once the pointer has crossed the movement threshold. */
  begin(payload: DragPayload, x: number, y: number): void {
    this.payload.set(payload);
    this.ghost.set({ x, y });
  }

  /** Updates the ghost position while dragging. */
  move(x: number, y: number): void {
    if (this.payload()) {
      this.ghost.set({ x, y });
    }
  }

  /**
   * Completes the drag: emits the drop (target may be null). When dropped on a valid zone,
   * the ghost glides to `settleTo` and then clears; otherwise it clears immediately.
   */
  drop(target: string | null, settleTo?: { x: number; y: number } | null): void {
    const payload = this.payload();
    if (payload) {
      this.dropsSubject.next({ payload, target });
    }
    if (payload && target && settleTo) {
      this.settling.set(true);
      this.ghost.set(settleTo); // CSS transition on the ghost glides it to the target.
      this.settleTimer = setTimeout(() => this.clear(), DragDropService.SETTLE_MS);
    } else {
      this.clear();
    }
  }

  /** Aborts the drag without emitting a drop (e.g. pointercancel). */
  cancel(): void {
    this.clear();
  }

  ngOnDestroy(): void {
    if (this.settleTimer !== null) {
      clearTimeout(this.settleTimer);
    }
    this.dropsSubject.complete();
  }

  private clear(): void {
    if (this.settleTimer !== null) {
      clearTimeout(this.settleTimer);
      this.settleTimer = null;
    }
    this.payload.set(null);
    this.ghost.set(null);
    this.settling.set(false);
  }
}
