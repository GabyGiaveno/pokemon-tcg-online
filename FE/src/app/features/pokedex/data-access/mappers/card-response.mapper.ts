import { CardResponse, CardPageResponse } from '../models/card-response';
import { PokedexCard, Attack, WeaknessResistance } from '../../domain/models/pokedex-card';

function safeJsonParse<T>(json: string | null): T | null {
  if (json === null || json === undefined) return null;
  try {
    const parsed = JSON.parse(json);
    return Array.isArray(parsed) ? (parsed as T) : null;
  } catch {
    return null;
  }
}

export function mapCardResponseToPokedexCard(response: CardResponse): PokedexCard {
  const { attacks, weaknesses, resistances, ...rest } = response;
  return {
    ...rest,
    attacks: safeJsonParse<Attack[]>(attacks),
    weaknesses: safeJsonParse<WeaknessResistance[]>(weaknesses),
    resistances: safeJsonParse<WeaknessResistance[]>(resistances),
  };
}

export function mapCardPageResponse(response: CardPageResponse): {
  cards: PokedexCard[];
  total: number;
  page: number;
  size: number;
} {
  return {
    cards: response.data.map(mapCardResponseToPokedexCard),
    total: response.total,
    page: response.page,
    size: response.size,
  };
}
