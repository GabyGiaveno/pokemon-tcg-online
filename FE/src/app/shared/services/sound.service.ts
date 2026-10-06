import { Injectable, signal } from '@angular/core';

const SOUND_ENABLED_KEY = 'pokemon_tcg_sound_enabled';

/**
 * Lightweight UI sound effects synthesized with the Web Audio API (no audio assets).
 * Currently exposes a short, "techy" hover blip. The on/off preference is persisted
 * to localStorage and exposed as a signal so the settings UI can bind to it.
 */
@Injectable({ providedIn: 'root' })
export class SoundService {
  /** Whether UI sounds are enabled (persisted). Bind this in the settings toggle. */
  readonly enabled = signal<boolean>(this.loadEnabled());

  private audioCtx: AudioContext | null = null;
  /** Timestamp of the last blip — throttles rapid hovers so the sound doesn't stack. */
  private lastPlay = 0;

  /** Plays a short, machine-like "piii" blip. No-op when sound is disabled. */
  playHover(): void {
    if (!this.enabled()) return;

    const now = Date.now();
    if (now - this.lastPlay < 60) return; // throttle fast cursor sweeps
    this.lastPlay = now;

    const ctx = this.ensureContext();
    if (!ctx) return;

    const t = ctx.currentTime;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    // Square wave + a slight upward pitch slide → a crisp, digital/machine beep.
    osc.type = 'square';
    osc.frequency.setValueAtTime(720, t);
    osc.frequency.exponentialRampToValueAtTime(1180, t + 0.05);

    // Fast attack, quick decay so it's a soft blip, not a sustained tone.
    // Kept a bit hotter than the music bed so it cuts through clearly.
    gain.gain.setValueAtTime(0.0001, t);
    gain.gain.exponentialRampToValueAtTime(0.08, t + 0.008);
    gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.085);

    osc.connect(gain).connect(ctx.destination);
    osc.start(t);
    osc.stop(t + 0.09);
  }

  /** Plays a softer, lower "boop" — used when hovering cards (distinct from the button blip). */
  playCardHover(): void {
    if (!this.enabled()) return;

    const now = Date.now();
    if (now - this.lastPlay < 45) return;
    this.lastPlay = now;

    const ctx = this.ensureContext();
    if (!ctx) return;

    const t = ctx.currentTime;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    // Sine + downward glide → a soft, rounded "boop", clearly different from the button beep.
    osc.type = 'sine';
    osc.frequency.setValueAtTime(520, t);
    osc.frequency.exponentialRampToValueAtTime(340, t + 0.06);

    gain.gain.setValueAtTime(0.0001, t);
    gain.gain.exponentialRampToValueAtTime(0.055, t + 0.01);
    gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.07);

    osc.connect(gain).connect(ctx.destination);
    osc.start(t);
    osc.stop(t + 0.08);
  }

  /** Toggles UI sounds on/off and persists the choice. */
  toggle(): void {
    this.setEnabled(!this.enabled());
  }

  setEnabled(value: boolean): void {
    this.enabled.set(value);
    try {
      localStorage.setItem(SOUND_ENABLED_KEY, value ? 'true' : 'false');
    } catch {
      /* localStorage unavailable — keep the in-memory value */
    }
  }

  /** Lazily creates (and resumes) the shared AudioContext after a user gesture. */
  private ensureContext(): AudioContext | null {
    const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!Ctor) return null;

    if (!this.audioCtx) {
      this.audioCtx = new Ctor();
    }
    if (this.audioCtx.state === 'suspended') {
      void this.audioCtx.resume();
    }
    return this.audioCtx;
  }

  private loadEnabled(): boolean {
    try {
      // Default ON; only an explicit 'false' disables it.
      return localStorage.getItem(SOUND_ENABLED_KEY) !== 'false';
    } catch {
      return true;
    }
  }
}
