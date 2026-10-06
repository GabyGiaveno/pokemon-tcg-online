import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { PlayerProfileResponse, ProfileAchievement, ProfileBadge, ProfileDeckSummary, ProfileStats, SkinItem, TrainerSkin } from './profile-api.types';

export interface ProfileSource {
  getProfile(): Observable<PlayerProfileResponse>;
  getDecks(): Observable<ProfileDeckSummary[]>;
  getStats(): Observable<ProfileStats>;
  getBadges(): Observable<ProfileBadge[]>;
  getAchievements(): Observable<ProfileAchievement[]>;
  getSkins(): Observable<TrainerSkin[]>;
  equipSkin(skinId: string): Observable<void>;
  getCustomizationItems(): Observable<SkinItem[]>;
}

export const PROFILE_SOURCE = new InjectionToken<ProfileSource>('PROFILE_SOURCE');
