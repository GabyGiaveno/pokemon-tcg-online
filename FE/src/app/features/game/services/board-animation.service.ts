import { Injectable, OnDestroy, signal } from '@angular/core';
import { GameEvent } from './game-event-stream.service';

/** Which board half an animation targets (player = bottom, opponent = top). */
export type AnimSide = 'player' | 'opponent';

/** Transient combat animation applied to an active Pokémon slot. */
export type ActiveAnim = 'lunge' | 'hit' | 'ko' | null;

/** A floating damage number over an active slot. `key` forces a fresh render on repeats. */
export interface FloatingDamage {
  readonly side: AnimSide;
  readonly amount: number;
  readonly key: number;
}

/** Board-supplied context to map absolute backend events onto the local perspective. */
export interface AnimationContext {
  readonly isMyTurn: boolean;
  readonly myPlayerId: number | null;
}

/**
 * Presentation-only orchestration layer for Slice 5 combat animations.
 *
 * <p>Translates discrete backend {@link GameEvent}s (from `GameEventStream`) into
 * transient, auto-clearing animation signals the board template binds to. It NEVER
 * mutates or derives authoritative game state — animations are pure display of events
 * (ARCHITECTURE.md: "the frontend never calculates rules"; authoritative truth still
 * arrives via the `state-changed` REST reload).
 *
 * <p>Component-provided (NOT root singleton) so its timers are torn down with the board.
 */
@Injectable()
export class BoardAnimationService implements OnDestroy {
  // Durations mirror TCG_BOARD.md "Animaciones requeridas".
  private static readonly LUNGE_MS = 600; // attacker lunges forward
  private static readonly HIT_MS = 800; // defender shake + floating damage
  private static readonly KO_MS = 1000; // knockout fade + screen flash
  private static readonly COIN_MS = 1900; // coin spin + land + linger

  /** Combat animation currently playing on each side's active slot. */
  readonly playerActive = signal<ActiveAnim>(null);
  readonly opponentActive = signal<ActiveAnim>(null);
  /** Floating damage number over an active slot, or `null` when none is showing. */
  readonly floatingDamage = signal<FloatingDamage | null>(null);
  /** Full-screen KO flash toggle; goes true then auto-clears so the CSS animation replays. */
  readonly screenFlash = signal(false);
  /** Latest coin-flip result to animate (`key` retriggers the spin), or null. */
  readonly coinFlip = signal<{ result: 'HEADS' | 'TAILS'; key: number } | null>(null);

  private readonly timers = new Set<ReturnType<typeof setTimeout>>();
  private damageKey = 0;
  private coinKey = 0;

  /** Routes one backend event to its animation. No-op for events without a visual mapping. */
  dispatch(event: GameEvent, ctx: AnimationContext): void {
    switch (event.type) {
      case 'ATTACK_DECLARED': {
        // Failure variants carry an errorCode and mean no attack happened — skip them.
        if (event.payload['errorCode']) return;
        this.playActive(ctx.isMyTurn ? 'player' : 'opponent', 'lunge', BoardAnimationService.LUNGE_MS);
        break;
      }
      case 'DAMAGE_DEALT': {
        // Animate ATTACK damage only — it carries explicit attacker/defender. Status-tick
        // damage (poison/burn: { cardId, damage, source }, no defender) fires in BETWEEN_TURNS
        // where `isMyTurn` is ambiguous; side-resolving it by cardId is deferred to a refinement.
        if (!event.payload['defender']) return;
        const amount = this.asNumber(event.payload['finalDamage']);
        if (amount <= 0) return;
        // Attack damage lands on the defender = the opponent of whoever's turn it is.
        const side: AnimSide = ctx.isMyTurn ? 'opponent' : 'player';
        this.playActive(side, 'hit', BoardAnimationService.HIT_MS);
        this.floatingDamage.set({ side, amount, key: ++this.damageKey });
        this.schedule(() => this.floatingDamage.set(null), BoardAnimationService.HIT_MS);
        break;
      }
      case 'POKEMON_KNOCKED_OUT': {
        const owner = this.asNumber(event.payload['playerId']);
        const side: AnimSide = ctx.myPlayerId != null && owner === ctx.myPlayerId ? 'player' : 'opponent';
        this.playActive(side, 'ko', BoardAnimationService.KO_MS);
        this.flashScreen();
        break;
      }
      case 'COIN_FLIPPED': {
        const result: 'HEADS' | 'TAILS' = event.payload['flip'] === 'HEADS' ? 'HEADS' : 'TAILS';
        this.coinFlip.set({ result, key: ++this.coinKey });
        this.schedule(() => this.coinFlip.set(null), BoardAnimationService.COIN_MS);
        break;
      }
      default:
        // No animation mapped for this event type (yet).
        break;
    }
  }

  ngOnDestroy(): void {
    this.timers.forEach(clearTimeout);
    this.timers.clear();
  }

  private playActive(side: AnimSide, anim: ActiveAnim, durationMs: number): void {
    const target = side === 'player' ? this.playerActive : this.opponentActive;
    target.set(anim);
    this.schedule(() => {
      // Clear only if this exact animation is still showing, so a newer one isn't clobbered.
      if (target() === anim) target.set(null);
    }, durationMs);
  }

  private flashScreen(): void {
    this.screenFlash.set(true);
    this.schedule(() => this.screenFlash.set(false), BoardAnimationService.KO_MS);
  }

  private schedule(fn: () => void, ms: number): void {
    const id = setTimeout(() => {
      this.timers.delete(id);
      fn();
    }, ms);
    this.timers.add(id);
  }

  private asNumber(value: unknown): number {
    return typeof value === 'number' ? value : 0;
  }
}
