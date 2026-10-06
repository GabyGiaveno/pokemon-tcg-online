import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthTokenService } from '../../../features/auth/data-access/auth-token.service';
import { BackgroundMusicService } from '../../services/background-music.service';
import { SoundService } from '../../services/sound.service';
import { HoverSoundDirective } from '../../directives/hover-sound.directive';

/**
 * Reusable settings dropdown (gear button → music toggle, sound toggle + logout).
 * Self-contained: handles its own open/close, the audio preferences, and logout.
 * Drop it into any top bar (lobby, Pokédex, profile, decks).
 */
@Component({
  selector: 'app-settings-menu',
  imports: [HoverSoundDirective],
  templateUrl: './settings-menu.html',
  styleUrl: './settings-menu.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsMenu {
  private readonly authToken = inject(AuthTokenService);
  private readonly router = inject(Router);
  private readonly soundService = inject(SoundService);
  private readonly musicService = inject(BackgroundMusicService);

  /** UI sound on/off state (persisted), bound to the toggle. */
  protected readonly soundEnabled = this.soundService.enabled;
  /** Background music on/off state (persisted), bound to the toggle. */
  protected readonly musicEnabled = this.musicService.enabled;

  protected readonly open = signal(false);
  protected readonly showLogoutConfirm = signal(false);
  protected readonly showCredits = signal(false);

  protected toggleMenu(): void {
    this.open.update((v) => !v);
  }

  protected closeMenu(): void {
    this.open.set(false);
  }

  protected toggleSound(): void {
    this.soundService.toggle();
  }

  protected toggleMusic(): void {
    this.musicService.toggle();
  }

  protected openCredits(): void {
    this.open.set(false);
    this.showCredits.set(true);
  }

  protected closeCredits(): void {
    this.showCredits.set(false);
  }

  protected openLogoutConfirm(): void {
    this.open.set(false);
    this.showLogoutConfirm.set(true);
  }

  protected cancelLogout(): void {
    this.showLogoutConfirm.set(false);
  }

  protected logout(): void {
    this.authToken.clear();
    this.showLogoutConfirm.set(false);
    void this.router.navigateByUrl('/auth/login');
  }
}
