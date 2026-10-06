import { Injectable, inject } from '@angular/core';
import { Observable, delay, of } from 'rxjs';
import { AuthTokenService } from '../../auth/data-access/auth-token.service';
import { PlayerProfileResponse, ProfileAchievement, ProfileBadge, ProfileDeckSummary, ProfileStats, SkinItem, TrainerSkin } from './profile-api.types';
import { ProfileSource } from './profile-source';

type ProfileMockVariant = 'ash' | 'misty' | 'red' | 'late' | 'empty';

interface MockProfileBundle {
  profile: PlayerProfileResponse;
  stats: ProfileStats;
  decks: ProfileDeckSummary[];
  badges: ProfileBadge[];
  achievements: ProfileAchievement[];
  skins: TrainerSkin[];
  customization: SkinItem[];
}

const PROFILE_VARIANT_KEY = 'profile_mock_variant';

@Injectable({ providedIn: 'root' })
export class ProfileMockService implements ProfileSource {
  private readonly authToken = inject(AuthTokenService);

  getProfile(): Observable<PlayerProfileResponse> {
    return of(this.bundle().profile);
  }

  getDecks(): Observable<ProfileDeckSummary[]> {
    return of(this.bundle().decks);
  }

  getStats(): Observable<ProfileStats> {
    return of(this.bundle().stats);
  }

  getBadges(): Observable<ProfileBadge[]> {
    return of(this.bundle().badges);
  }

  getAchievements(): Observable<ProfileAchievement[]> {
    return of(this.bundle().achievements);
  }

  getSkins(): Observable<TrainerSkin[]> {
    return of(this.bundle().skins);
  }

  equipSkin(_skinId: string): Observable<void> {
    return of(undefined).pipe(delay(400));
  }

  getCustomizationItems(): Observable<SkinItem[]> {
    return of(this.bundle().customization);
  }

  private bundle(): MockProfileBundle {
    switch (this.readVariant()) {
      case 'misty': return this.buildMistyBundle();
      case 'red': return this.buildRedBundle();
      case 'late': return this.buildLateBundle();
      case 'empty': return this.buildEmptyBundle();
      default: return this.buildAshBundle();
    }
  }

  private readVariant(): ProfileMockVariant {
    try {
      const value = localStorage.getItem(PROFILE_VARIANT_KEY);
      if (value === 'misty' || value === 'red' || value === 'late' || value === 'empty' || value === 'ash') {
        return value;
      }
    } catch {
      // ignore
    }
    return 'ash';
  }

  private baseProfile(overrides: Partial<PlayerProfileResponse> = {}): PlayerProfileResponse {
    return {
      id: this.authToken.playerIdValue ?? 1,
      username: this.authToken.usernameValue ?? 'Ash',
      email: 'ash@pallet.town',
      createdAt: '2026-01-15 10:30:00',
      decksCount: 3,
      xpPercent: 65,
      bio: 'Entrenador Pokémon en busca de nuevos desafíos. ¡Atrapo sueños y colecciono victorias!',
      level: 12,
      favoriteRegion: 'Kanto',
      favoritePokemon: 'Pikachu',
      totalCards: 156,
      ...overrides,
    };
  }

  private baseStats(overrides: Partial<ProfileStats> = {}): ProfileStats {
    return {
      wins: 47,
      losses: 42,
      streak: 3,
      tournamentsWon: 1,
      packsOpened: 23,
      totalCards: 156,
      decksCreated: 3,
      ...overrides,
    };
  }

  private baseDecks(): ProfileDeckSummary[] {
    return [
      { id: 1, name: 'Mazo Fuego', valid: true, cardCount: 60, createdAt: '2026-02-10 18:42:00' },
      { id: 2, name: 'Mazo Agua', valid: false, cardCount: 54, createdAt: '2026-03-01 09:15:00' },
      { id: 3, name: 'Mazo Eléctrico', valid: true, cardCount: 60, createdAt: '2026-04-22 21:05:00' },
    ];
  }

  private commonBadges(): ProfileBadge[] {
    return [
      { id: 'fire', label: 'Insignia Llama', icon: '🔥', unlocked: true, description: 'Otorgada por el líder del Gimnasio Fuego', howToUnlock: 'Gana una partida con un mazo de tipo Fuego' },
      { id: 'water', label: 'Insignia Cascada', icon: '💧', unlocked: true, description: 'Otorgada por el líder del Gimnasio Agua', howToUnlock: 'Gana una partida con un mazo de tipo Agua' },
      { id: 'grass', label: 'Insignia Bosque', icon: '🌿', unlocked: true, description: 'Otorgada por el líder del Gimnasio Planta', howToUnlock: 'Gana una partida con un mazo de tipo Planta' },
      { id: 'electric', label: 'Insignia Trueno', icon: '⚡', unlocked: false, description: 'Otorgada por el líder del Gimnasio Eléctrico', howToUnlock: 'Gana una partida con un mazo de tipo Eléctrico' },
      { id: 'psychic', label: 'Insignia Mente', icon: '🔮', unlocked: false, description: 'Otorgada por el líder del Gimnasio Psíquico', howToUnlock: 'Gana una partida con un mazo de tipo Psíquico' },
      { id: 'fighting', label: 'Insignia Lucha', icon: '🥊', unlocked: false, description: 'Otorgada por el líder del Gimnasio Lucha', howToUnlock: 'Gana una partida con un mazo de tipo Lucha' },
      { id: 'dark', label: 'Insignia Oscuridad', icon: '🌑', unlocked: false, description: 'Otorgada por el líder del Gimnasio Siniestro', howToUnlock: 'Gana una partida con un mazo de tipo Siniestro' },
      { id: 'dragon', label: 'Insignia Dragón', icon: '🐉', unlocked: false, description: 'Otorgada por el líder del Gimnasio Dragón', howToUnlock: 'Gana 10 partidas consecutivas' },
    ];
  }

  private commonAchievements(): ProfileAchievement[] {
    return [
      { id: 'first-win', name: 'Campeón Novato', description: 'Obtén tu primera victoria.', icon: '🏆', unlocked: true },
      { id: 'collector-100', name: 'Coleccionista de Cartas', description: 'Consigue 100 cartas.', icon: '📚', unlocked: true },
      { id: 'deck-master', name: 'Maestro de Mazos', description: 'Crea 10 mazos.', icon: '🃏', unlocked: false },
      { id: 'veteran', name: 'Veterano Pokémon', description: 'Juega 100 partidas.', icon: '⭐', unlocked: false },
      { id: 'lucky', name: 'Golpe de Suerte', description: 'Gana una partida con una carta decisiva.', icon: '🍀', unlocked: true },
    ];
  }

  private customizationBase(): SkinItem[] {
    return [
      { id: 'shirt-blue', name: 'Camisa Azul', category: 'clothes', unlocked: true, previewIcon: '👕', color: '#105189' },
      { id: 'shirt-red', name: 'Camisa Roja', category: 'clothes', unlocked: true, previewIcon: '👕', color: '#c0392b' },
      { id: 'shirt-black', name: 'Camisa Negra', category: 'clothes', unlocked: false, previewIcon: '👕', color: '#2c3e50' },
      { id: 'hat-cap', name: 'Gorra Clásica', category: 'accessory', unlocked: true, previewIcon: '🧢', color: '#DE940E' },
      { id: 'hat-beanie', name: 'Gorro Invernal', category: 'accessory', unlocked: false, previewIcon: '🧶', color: '#3498db' },
      { id: 'glasses', name: 'Gafas Oscuras', category: 'accessory', unlocked: true, previewIcon: '🕶️' },
      { id: 'pose-1', name: 'Firme', category: 'pose', unlocked: true, previewIcon: '🧍' },
      { id: 'pose-2', name: 'Con Puño', category: 'pose', unlocked: true, previewIcon: '✊' },
      { id: 'pose-3', name: 'Saludando', category: 'pose', unlocked: false, previewIcon: '👋' },
      { id: 'bg-stadium', name: 'Estadio', category: 'background', unlocked: true, previewIcon: '🏟️' },
      { id: 'bg-beach', name: 'Playa', category: 'background', unlocked: false, previewIcon: '🏖️' },
      { id: 'bg-mountain', name: 'Montaña', category: 'background', unlocked: false, previewIcon: '⛰️' },
    ];
  }

  private buildAshBundle(): MockProfileBundle {
    return {
      profile: this.baseProfile(),
      stats: this.baseStats(),
      decks: this.baseDecks(),
      badges: this.commonBadges(),
      achievements: this.commonAchievements(),
      skins: [
        { id: 'default',  name: 'Ash',       character: 'ash',   hatColor: '#e74c3c', shirtColor: '#2980b9', pantsColor: '#2c3e50', skinTone: '#f5d0a9', equipped: true },
        { id: 'fire',     name: 'Fuego',      character: 'ash',   hatColor: '#e74c3c', shirtColor: '#c0392b', pantsColor: '#8e44ad', skinTone: '#f5d0a9', equipped: false },
        { id: 'water',    name: 'Agua',       character: 'ash',   hatColor: '#3498db', shirtColor: '#2980b9', pantsColor: '#1a5276', skinTone: '#f5d0a9', equipped: false },
        { id: 'electric', name: 'Eléctrico',  character: 'ash',   hatColor: '#f1c40f', shirtColor: '#2c3e50', pantsColor: '#7f8c8d', skinTone: '#f5d0a9', equipped: false },
        { id: 'dark',     name: 'Oscuro',     character: 'ash',   hatColor: '#2c3e50', shirtColor: '#1a1a2e', pantsColor: '#16213e', skinTone: '#d4a574', equipped: false },
        { id: 'brock',    name: 'Brock',      character: 'brock', hatColor: '#2a1a0e', shirtColor: '#4a7c3f', pantsColor: '#3d2b1f', skinTone: '#c8a165', equipped: false },
        { id: 'misty',    name: 'Misty',      character: 'misty', hatColor: '#ff6600', shirtColor: '#e63946', pantsColor: '#20b2aa', skinTone: '#f5d0a9', equipped: false },
      ],
      customization: this.customizationBase(),
    };
  }

  private buildMistyBundle(): MockProfileBundle {
    return {
      profile: this.baseProfile({ username: 'Misty', email: 'misty@cerulean.city', bio: 'Líder de gimnasio y especialista en estrategias de agua.', favoriteRegion: 'Kanto', favoritePokemon: 'Staryu', level: 16, xpPercent: 42, totalCards: 228, decksCount: 5 }),
      stats: this.baseStats({ wins: 63, losses: 21, streak: 8, decksCreated: 5, totalCards: 228 }),
      decks: [
        { id: 11, name: 'Azul Profundo', valid: true, cardCount: 60, createdAt: '2026-05-02 12:20:00' },
        { id: 12, name: 'Oleaje', valid: true, cardCount: 60, createdAt: '2026-05-14 20:10:00' },
      ],
      badges: this.commonBadges().map((badge) => ({ ...badge, unlocked: ['water', 'grass'].includes(badge.id) })),
      achievements: this.commonAchievements().map((achievement) => ({ ...achievement, unlocked: ['first-win', 'collector-100'].includes(achievement.id) })),
      skins: [
        { id: 'default', name: 'Ash',   character: 'ash' as const, hatColor: '#e74c3c', shirtColor: '#2980b9', pantsColor: '#2c3e50', skinTone: '#f5d0a9', equipped: false },
        { id: 'water',   name: 'Agua',  character: 'ash' as const, hatColor: '#3498db', shirtColor: '#2980b9', pantsColor: '#1a5276', skinTone: '#f5d0a9', equipped: true },
        { id: 'misty',   name: 'Misty', character: 'misty' as const, hatColor: '#ff6600', shirtColor: '#e63946', pantsColor: '#20b2aa', skinTone: '#f5d0a9', equipped: false },
      ],
      customization: this.customizationBase().map((item) => ({ ...item, unlocked: item.id !== 'shirt-black' && item.id !== 'bg-beach' })),
    };
  }

  private buildRedBundle(): MockProfileBundle {
    return {
      profile: this.baseProfile({ username: 'Red', email: 'red@mountain.labs', bio: 'Siempre busco el combate más duro.', favoriteRegion: 'Kanto', favoritePokemon: 'Charizard', level: 22, xpPercent: 86, totalCards: 302, decksCount: 8 }),
      stats: this.baseStats({ wins: 102, losses: 37, streak: 14, decksCreated: 8, totalCards: 302 }),
      decks: [
        { id: 21, name: 'Inferno', valid: true, cardCount: 60, createdAt: '2026-04-04 19:05:00' },
        { id: 22, name: 'Rivalidad', valid: true, cardCount: 60, createdAt: '2026-06-01 13:50:00' },
      ],
      badges: this.commonBadges().map((badge) => ({ ...badge, unlocked: ['fire', 'electric', 'dragon'].includes(badge.id) })),
      achievements: this.commonAchievements().map((achievement) => ({ ...achievement, unlocked: ['first-win', 'deck-master', 'lucky'].includes(achievement.id) })),
      skins: [
        { id: 'default', name: 'Ash',    character: 'ash' as const, hatColor: '#e74c3c', shirtColor: '#2980b9', pantsColor: '#2c3e50', skinTone: '#f5d0a9', equipped: false },
        { id: 'fire',    name: 'Fuego',  character: 'ash' as const, hatColor: '#e74c3c', shirtColor: '#c0392b', pantsColor: '#8e44ad', skinTone: '#f5d0a9', equipped: true },
        { id: 'dark',    name: 'Oscuro', character: 'ash' as const, hatColor: '#2c3e50', shirtColor: '#1a1a2e', pantsColor: '#16213e', skinTone: '#d4a574', equipped: false },
        { id: 'brock',   name: 'Brock',  character: 'brock' as const, hatColor: '#2a1a0e', shirtColor: '#4a7c3f', pantsColor: '#3d2b1f', skinTone: '#c8a165', equipped: false },
      ],
      customization: this.customizationBase().map((item) => ({ ...item, unlocked: true })),
    };
  }

  private buildLateBundle(): MockProfileBundle {
    return {
      profile: this.baseProfile({ username: 'Late Game', email: 'late@battle.room', bio: 'Una mano, un premio, una victoria.', favoriteRegion: 'Johto', favoritePokemon: 'Umbreon', level: 29, xpPercent: 12, totalCards: 411, decksCount: 12 }),
      stats: this.baseStats({ wins: 141, losses: 88, streak: 6, decksCreated: 12, totalCards: 411 }),
      decks: [
        { id: 31, name: 'Control', valid: true, cardCount: 60, createdAt: '2026-03-19 08:10:00' },
        { id: 32, name: 'Final Push', valid: true, cardCount: 60, createdAt: '2026-05-28 22:33:00' },
        { id: 33, name: 'Mirror', valid: false, cardCount: 58, createdAt: '2026-06-05 17:05:00' },
      ],
      badges: this.commonBadges().map((badge) => ({ ...badge, unlocked: badge.id !== 'dragon' })),
      achievements: this.commonAchievements().map((achievement) => ({ ...achievement, unlocked: achievement.id !== 'veteran' })),
      skins: [
        { id: 'default',  name: 'Ash',       character: 'ash' as const, hatColor: '#e74c3c', shirtColor: '#2980b9', pantsColor: '#2c3e50', skinTone: '#f5d0a9', equipped: false },
        { id: 'shadow',   name: 'Sombra',    character: 'ash' as const, hatColor: '#1a1a2e', shirtColor: '#16213e', pantsColor: '#0f172a', skinTone: '#d4a574', equipped: true },
        { id: 'electric', name: 'Eléctrico', character: 'ash' as const, hatColor: '#f1c40f', shirtColor: '#2c3e50', pantsColor: '#7f8c8d', skinTone: '#f5d0a9', equipped: false },
      ],
      customization: this.customizationBase().map((item) => ({ ...item, unlocked: item.category !== 'background' || item.id === 'bg-stadium' })),
    };
  }

  private buildEmptyBundle(): MockProfileBundle {
    return {
      profile: this.baseProfile({ username: this.authToken.usernameValue ?? 'Entrenador', email: 'trainer@empty.zone', bio: 'Todavía no se cargó ningún dato de progreso.', favoriteRegion: 'Sin definir', favoritePokemon: 'Sin definir', level: 1, xpPercent: 0, totalCards: 0, decksCount: 0 }),
      stats: this.baseStats({ wins: 0, losses: 0, streak: 0, decksCreated: 0, totalCards: 0 }),
      decks: [],
      badges: this.commonBadges().map((badge) => ({ ...badge, unlocked: false })),
      achievements: this.commonAchievements().map((achievement) => ({ ...achievement, unlocked: false })),
      skins: [
        { id: 'default', name: 'Ash', character: 'ash' as const, hatColor: '#e74c3c', shirtColor: '#2980b9', pantsColor: '#2c3e50', skinTone: '#f5d0a9', equipped: true },
      ],
      customization: this.customizationBase().map((item) => ({ ...item, unlocked: item.id === 'shirt-blue' || item.id === 'hat-cap' })),
    };
  }
}
