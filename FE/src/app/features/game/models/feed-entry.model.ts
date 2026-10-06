export type FeedEntryKind =
  | 'card_drawn' | 'attack_declared' | 'damage_dealt' | 'pokemon_knocked_out'
  | 'prize_taken' | 'energy_attached' | 'trainer_played' | 'status_effect_applied'
  | 'status_effect_cleared' | 'turn_started' | 'turn_ended' | 'phase_changed'
  | 'game_finished' | 'coin_flipped' | 'setup_completed' | 'player_ready';

export type FeedEntryCategory = 'combat' | 'game' | 'card' | 'status' | 'system';

export interface FeedEntry {
  id: string;
  kind: FeedEntryKind;
  category: FeedEntryCategory;
  message: string;
  timestamp: Date;
  isToast: boolean;
}
