import { CardInstanceDto } from '../models/board-state.dto';

/**
 * Card classification helpers based on the PTCG `supertype` + `subtypes`.
 *
 * `supertype` ("Pokémon" / "Energy" / "Trainer") is required to tell a Basic
 * ENERGY apart from a Basic POKÉMON — both carry the "Basic" subtype, so the
 * subtype alone is ambiguous.
 */

export function isPokemon(card: CardInstanceDto): boolean {
  return card.supertype === 'Pokémon' || card.supertype === 'Pokemon';
}

export function isEnergy(card: CardInstanceDto): boolean {
  return card.supertype === 'Energy';
}

export function isTrainer(card: CardInstanceDto): boolean {
  return card.supertype === 'Trainer';
}

/** A Basic Pokémon (playable directly to the bench / active in SETUP). */
export function isBasicPokemon(card: CardInstanceDto): boolean {
  return isPokemon(card) && (card.subtypes?.includes('Basic') ?? false);
}

/** An evolution card (Stage 1 / Stage 2). */
export function isEvolution(card: CardInstanceDto): boolean {
  return isPokemon(card) && (card.subtypes?.some((s) => s.startsWith('Stage')) ?? false);
}

/** A Pokémon Tool card (subtype "Pokémon Tool"). Attach with ATTACH_TOOL, not PLAY_ITEM. */
export function isTool(card: CardInstanceDto): boolean {
  return card.subtypes?.includes('Pokémon Tool') ?? false;
}

/** Maps a Trainer card to its dispatch action by subtype (Item / Supporter / Stadium). */
export function trainerActionType(card: CardInstanceDto): 'playItem' | 'playSupporter' | 'playStadium' {
  const subtypes = card.subtypes ?? [];
  if (subtypes.includes('Item')) return 'playItem';
  if (subtypes.includes('Stadium')) return 'playStadium';
  return 'playSupporter';
}
