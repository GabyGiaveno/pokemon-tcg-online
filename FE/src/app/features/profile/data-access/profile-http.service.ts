import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of } from 'rxjs';
import { PlayerProfileResponse, ProfileAchievement, ProfileBadge, ProfileDeckSummary, ProfileStats, SkinItem, TrainerSkin } from './profile-api.types';
import { ProfileSource } from './profile-source';

interface DeckCardResponseDto {
  cardId: string;
  quantity: number;
}

interface DeckResponseDto {
  id: number;
  name: string;
  valid: boolean;
  cardCount: number;
  cards: DeckCardResponseDto[];
  createdAt: string;
}

interface ProfileWithStatsDto extends PlayerProfileResponse {
  wins: number;
  losses: number;
  streak: number;
  decksCreated: number;
}

@Injectable({ providedIn: 'root' })
export class ProfileHttpService implements ProfileSource {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api';

  private readonly placeholderBadges: ProfileBadge[] = [
    { id: 'fire', label: 'Insignia Llama', icon: '🔥', unlocked: false, description: '', howToUnlock: '' },
    { id: 'water', label: 'Insignia Cascada', icon: '💧', unlocked: false, description: '', howToUnlock: '' },
    { id: 'grass', label: 'Insignia Bosque', icon: '🌿', unlocked: false, description: '', howToUnlock: '' },
    { id: 'electric', label: 'Insignia Trueno', icon: '⚡', unlocked: false, description: '', howToUnlock: '' },
    { id: 'psychic', label: 'Insignia Mente', icon: '🔮', unlocked: false, description: '', howToUnlock: '' },
  ];

  getProfile(): Observable<PlayerProfileResponse> {
    return this.http.get<PlayerProfileResponse>(`${this.baseUrl}/players/me`);
  }

  getDecks(): Observable<ProfileDeckSummary[]> {
    return this.http.get<DeckResponseDto[]>(`${this.baseUrl}/decks`).pipe(
      map((decks) => decks.map((deck) => ({
        id: deck.id,
        name: deck.name,
        valid: deck.valid,
        cardCount: deck.cardCount,
        createdAt: deck.createdAt,
      })))
    );
  }

  getStats(): Observable<ProfileStats> {
    return this.http.get<ProfileWithStatsDto>(`${this.baseUrl}/players/me`).pipe(
      map((profile) => ({
        wins: profile.wins ?? 0,
        losses: profile.losses ?? 0,
        streak: profile.streak ?? 0,
        tournamentsWon: 0,
        packsOpened: 0,
        totalCards: profile.totalCards,
        decksCreated: profile.decksCreated ?? profile.decksCount,
      }))
    );
  }

  getBadges(): Observable<ProfileBadge[]> {
    return of(this.placeholderBadges);
  }

  getAchievements(): Observable<ProfileAchievement[]> {
    return of([]);
  }

  getSkins(): Observable<TrainerSkin[]> {
    return this.http.get<TrainerSkin[]>(`${this.baseUrl}/players/me/skins`);
  }

  equipSkin(skinId: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/players/me/skin`, { skinId });
  }

  getCustomizationItems(): Observable<SkinItem[]> {
    return of([]);
  }
}
