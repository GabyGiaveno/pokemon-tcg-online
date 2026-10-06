import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthTokenService } from '../../../auth/data-access/auth-token.service';
import { ProfileApiService } from '../../data-access/profile-api.service';
import { PlayerProfileResponse, ProfileAchievement, ProfileBadge, ProfileStats, TrainerSkin } from '../../data-access/profile-api.types';
import { TrainerCard } from '../../components/trainer-card/trainer-card';
import { TrainerStats } from '../../components/trainer-stats/trainer-stats';
import { TrainerBadges } from '../../components/trainer-badges/trainer-badges';
import { TrainerAchievements } from '../../components/trainer-achievements/trainer-achievements';
import { SettingsMenu } from '../../../../shared/components/settings-menu/settings-menu';
import { BackgroundMusicService } from '../../../../shared/services/background-music.service';

@Component({
  selector: 'app-profile-page',
  imports: [RouterLink, TrainerCard, TrainerStats, TrainerBadges, TrainerAchievements, SettingsMenu],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage {
  private readonly api = inject(ProfileApiService);
  private readonly authToken = inject(AuthTokenService);
  private readonly router = inject(Router);
  private readonly backgroundMusic = inject(BackgroundMusicService);

  protected readonly profile = signal<PlayerProfileResponse | null>(null);
  protected readonly stats = signal<ProfileStats | null>(null);
  protected readonly badges = signal<ProfileBadge[]>([]);
  protected readonly achievements = signal<ProfileAchievement[]>([]);
  protected readonly skins = signal<TrainerSkin[]>([]);
  protected readonly loading = signal(true);
  protected readonly profileError = signal<string | null>(null);
  protected readonly equiping = signal(false);

  protected readonly currentSkinIndex = signal(0);
  protected readonly currentSkin = computed(() => {
    const list = this.skins();
    return list.length > 0 ? list[this.currentSkinIndex()] : this.fallbackSkin;
  });

  private readonly fallbackSkin: TrainerSkin = {
    id: 'default',
    name: 'Ash',
    character: 'ash',
    hatColor: '#e74c3c',
    shirtColor: '#2980b9',
    pantsColor: '#2c3e50',
    skinTone: '#f5d0a9',
  };

  constructor() {
    this.loadProfile();
    this.backgroundMusic.play();
  }

  private loadProfile(): void {
    this.loading.set(true);
    this.profileError.set(null);

    this.api.getProfile().subscribe({
      next: (profile) => this.profile.set(profile),
      error: () => this.profileError.set('No se pudo cargar el perfil.'),
      complete: () => this.loading.set(false),
    });

    this.api.getStats().subscribe({ next: (stats) => this.stats.set(stats) });
    this.api.getBadges().subscribe({ next: (badges) => this.badges.set(badges) });
    this.api.getAchievements().subscribe({ next: (achievements) => this.achievements.set(achievements) });
    this.api.getSkins().subscribe({
      next: (skins) => {
        this.skins.set(skins);
        this.currentSkinIndex.set(this.findEquippedSkinIndex(skins));
      },
      error: () => this.skins.set([]),
    });
  }

  private findEquippedSkinIndex(skins: TrainerSkin[]): number {
    const equippedIndex = skins.findIndex((skin) => skin.equipped);
    return equippedIndex >= 0 ? equippedIndex : 0;
  }

  protected prevSkin(): void {
    this.currentSkinIndex.update((i) => Math.max(0, i - 1));
  }

  protected nextSkin(): void {
    this.currentSkinIndex.update((i) => Math.min(this.skins().length - 1, i + 1));
  }

  protected confirmSkin(): void {
    const skin = this.currentSkin();
    if (skin.equipped || this.equiping()) return;

    this.equiping.set(true);
    this.api.equipSkin(skin.id).subscribe({
      next: () => {
        this.skins.update((list) =>
          list.map((s) => ({ ...s, equipped: s.id === skin.id }))
        );
        this.equiping.set(false);
      },
      error: () => {
        this.equiping.set(false);
      },
    });
  }

  protected logout(): void {
    this.authToken.clear();
    void this.router.navigateByUrl('/auth/login');
  }

}
