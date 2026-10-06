import { Injectable, signal } from '@angular/core';

const MUSIC_ENABLED_KEY = 'pokemon_tcg_music_enabled';
const MUSIC_SOURCE = '/music/fondo.mp3';
/** Kept low on purpose so the UI sound effects (button/card blips) stay audible over it. */
const DEFAULT_VOLUME = 0.035;

/**
 * Shared background music controller for the lobby-style screens.
 *
 * <p>Plays a single looping audio file from `public/music/fondo.mp3` and keeps the
 * same playback position across navigation. Pages only call `play()` when they mount;
 * the service ignores repeated calls while the track is already active.
 */
@Injectable({ providedIn: 'root' })
export class BackgroundMusicService {
  /** Whether background music is enabled (persisted). */
  readonly enabled = signal<boolean>(this.loadEnabled());

  /** Whether the track is currently playing. */
  readonly playing = signal(false);

  private audio: HTMLAudioElement | null = null;

  toggle(): void {
    this.setEnabled(!this.enabled());
  }

  setEnabled(value: boolean): void {
    this.enabled.set(value);

    try {
      localStorage.setItem(MUSIC_ENABLED_KEY, value ? 'true' : 'false');
    } catch {
      /* localStorage unavailable — keep the in-memory value */
    }

    if (value) {
      void this.play();
    } else {
      this.stop();
    }
  }

  async play(volume = DEFAULT_VOLUME): Promise<void> {
    if (!this.enabled()) {
      return;
    }

    const audio = this.ensureAudio();
    if (!audio) {
      return;
    }

    audio.volume = volume;

    if (!audio.paused) {
      this.playing.set(true);
      return;
    }

    try {
      await audio.play();
      this.playing.set(true);
    } catch {
      this.playing.set(false);
    }
  }

  stop(): void {
    const audio = this.audio;
    if (!audio) {
      this.playing.set(false);
      return;
    }

    audio.pause();
    this.playing.set(false);
  }

  private ensureAudio(): HTMLAudioElement | null {
    if (typeof window === 'undefined') {
      return null;
    }

    if (!this.audio) {
      const audio = new Audio(MUSIC_SOURCE);
      audio.loop = true;
      audio.preload = 'auto';
      audio.volume = DEFAULT_VOLUME;
      audio.addEventListener('ended', () => this.playing.set(false));
      this.audio = audio;
    }

    return this.audio;
  }

  private loadEnabled(): boolean {
    try {
      return localStorage.getItem(MUSIC_ENABLED_KEY) !== 'false';
    } catch {
      return true;
    }
  }
}
