import type { PokedexCard } from '../../features/pokedex/domain/models/pokedex-card';

export type CardFinish = 'normal' | 'holo' | 'reverse' | 'ultra' | 'rainbow' | 'gold' | 'secret';

/**
 * Conservative visual-only finish heuristic.
 *
 * PokedexCard currently does not expose official rarity, so this function avoids
 * over-classifying normal cards as holographic.
 */
export function getCardFinish(card: PokedexCard): CardFinish {
  const name = normalize(card.name);
  const supertype = normalize(card.supertype);
  const subtypes = (card.subtypes ?? []).map(normalize);
  const searchable = [name, ...subtypes].join(' ');

  if (card.aceTactician === true) {
    return 'gold';
  }

  if (containsToken(searchable, 'rainbow')) {
    return 'rainbow';
  }

  if (containsToken(searchable, 'secret')) {
    return 'secret';
  }

  if (containsToken(searchable, 'gold')) {
    return 'gold';
  }

  if (containsToken(searchable, 'reverse')) {
    return 'reverse';
  }

  if (subtypes.some(isUltraSubtype)) {
    return 'ultra';
  }

  if ((supertype === 'pokemon' || supertype === 'pokémon') && subtypes.some(isHoloSubtype)) {
    return 'holo';
  }

  return 'normal';
}

function normalize(value: string | null | undefined): string {
  return (value ?? '').trim().toLowerCase();
}

function containsToken(value: string, token: string): boolean {
  return new RegExp(`\\b${token}\\b`, 'i').test(value);
}

function isUltraSubtype(subtype: string): boolean {
  return ['ex', 'gx', 'v', 'vmax', 'vstar', 'break', 'mega'].includes(subtype);
}

function isHoloSubtype(subtype: string): boolean {
  return ['legend', 'prime', 'restored'].includes(subtype);
}
