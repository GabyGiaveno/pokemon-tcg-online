export interface PlayerProfileResponse {
  id: number;
  username: string;
  email: string;
  createdAt: string;
  decksCount: number;
  xpPercent: number;
  bio: string;
  level: number;
  favoriteRegion: string;
  favoritePokemon: string;
  totalCards: number;
}

export interface ProfileStats {
  wins: number;
  losses: number;
  streak: number;
  tournamentsWon: number;
  packsOpened: number;
  totalCards: number;
  decksCreated: number;
}

export interface ProfileDeckSummary {
  id: number;
  name: string;
  valid: boolean;
  cardCount: number;
  createdAt: string;
}

export interface ProfileBadge {
  id: string;
  label: string;
  icon: string;
  unlocked: boolean;
  description: string;
  howToUnlock: string;
}

export interface ProfileAchievement {
  id: string;
  name: string;
  description: string;
  icon: string;
  unlocked: boolean;
}

export interface SkinItem {
  id: string;
  name: string;
  category: 'clothes' | 'accessory' | 'pose' | 'background';
  unlocked: boolean;
  previewIcon: string;
  color?: string;
}

export type TrainerCharacter = 'ash' | 'brock' | 'misty';

export interface TrainerSkin {
  id: string;
  name: string;
  character: TrainerCharacter;
  hatColor: string;
  shirtColor: string;
  pantsColor: string;
  skinTone: string;
  equipped?: boolean;
}
