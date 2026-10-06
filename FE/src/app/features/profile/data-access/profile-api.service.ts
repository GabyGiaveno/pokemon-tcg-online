import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PlayerProfileResponse, ProfileAchievement, ProfileBadge, ProfileDeckSummary, ProfileStats, SkinItem, TrainerSkin } from './profile-api.types';
import { PROFILE_SOURCE } from './profile-source';

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly source = inject(PROFILE_SOURCE);

  getProfile(): Observable<PlayerProfileResponse> {
    return this.source.getProfile();
  }

  getDecks(): Observable<ProfileDeckSummary[]> {
    return this.source.getDecks();
  }

  getStats(): Observable<ProfileStats> {
    return this.source.getStats();
  }

  getBadges(): Observable<ProfileBadge[]> {
    return this.source.getBadges();
  }

  getAchievements(): Observable<ProfileAchievement[]> {
    return this.source.getAchievements();
  }

  getSkins(): Observable<TrainerSkin[]> {
    return this.source.getSkins();
  }

  equipSkin(skinId: string): Observable<void> {
    return this.source.equipSkin(skinId);
  }

  getCustomizationItems(): Observable<SkinItem[]> {
    return this.source.getCustomizationItems();
  }
}
