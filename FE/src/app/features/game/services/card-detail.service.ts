import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, shareReplay, tap } from 'rxjs/operators';
import { CardResponse } from '../../pokedex/data-access/models/card-response';
import { AttackDetailDto, CardDetailDto, TypedValue } from '../models/card-detail.dto';
import { XY1_ES } from '../data/xy1-es';

/**
 * Resolves static card metadata (attacks, costs, damage) by `cardId`.
 *
 * Backend: `GET /api/cards/{cardId}` returns a `CardResponse` whose `attacks` field
 * is a JSON-stringified array (see `CardResponse.attacks` in the pokedex feature).
 * Results are cached in-memory for the lifetime of the app to avoid repeated lookups.
 */
@Injectable({ providedIn: 'root' })
export class CardDetailService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/cards';

  private readonly cache = new Map<string, Observable<CardDetailDto>>();

  /** Returns card detail (cached) for the given cardId, including parsed attacks. */
  getCardDetail(cardId: string): Observable<CardDetailDto> {
    const cached = this.cache.get(cardId);
    if (cached) {
      return cached;
    }

    const request$ = this.http.get<CardResponse>(`${this.baseUrl}/${cardId}`).pipe(
      map((response) => this.toCardDetail(response)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    this.cache.set(cardId, request$);
    return request$;
  }

  private toCardDetail(response: CardResponse): CardDetailDto {
    return {
      cardId: response.id,
      name: response.name,
      supertype: response.supertype,
      subtypes: response.subtypes ?? [],
      hp: response.hp,
      types: response.types ?? [],
      evolvesFrom: response.evolvesFrom,
      setName: response.cardSetName,
      attacks: this.parseAttacks(response.attacks, response.id),
      weaknesses: this.parseTypedValues(response.weaknesses),
      resistances: this.parseTypedValues(response.resistances),
      retreatCost: response.retreatCost ?? [],
    };
  }

  /** Parses a JSON-stringified `[{ type, value }]` list (weaknesses / resistances). */
  private parseTypedValues(raw: string | null): TypedValue[] {
    if (!raw) {
      return [];
    }
    try {
      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) {
        return [];
      }
      return parsed
        .filter((entry) => entry && typeof entry.type === 'string')
        .map((entry) => ({ type: entry.type as string, value: typeof entry.value === 'string' ? entry.value : '' }));
    } catch {
      return [];
    }
  }

  private parseAttacks(raw: string | null, cardId: string): AttackDetailDto[] {
    if (!raw) {
      return [];
    }

    try {
      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) {
        return [];
      }

      const translations = XY1_ES[cardId]?.attacks;
      return parsed.map((attack, index) => {
        const englishName = typeof attack?.name === 'string' ? attack.name : `Attack ${index + 1}`;
        const englishText = typeof attack?.text === 'string' ? attack.text : '';
        const es = translations?.[englishName];
        return {
          index,
          name: es?.name || englishName,
          cost: Array.isArray(attack?.cost) ? (attack.cost as string[]) : [],
          damage: this.parseDamage(attack?.damage),
          text: es ? es.text : englishText,
        };
      });
    } catch {
      return [];
    }
  }

  /** Parses damage strings like "20", "20+", "20x", "" into a numeric value (0 if non-numeric). */
  private parseDamage(value: unknown): number {
    if (typeof value === 'number') {
      return value;
    }

    if (typeof value === 'string') {
      const match = value.match(/^\d+/);
      return match ? Number(match[0]) : 0;
    }

    return 0;
  }
}

/** Test helper: build a CardDetailDto without going through the HTTP/cache layer. */
export function buildCardDetailFixture(overrides: Partial<CardDetailDto> = {}): Observable<CardDetailDto> {
  return of({
    cardId: 'fixture-card',
    name: 'Fixture Card',
    supertype: 'Pokémon',
    subtypes: [],
    hp: null,
    types: [],
    evolvesFrom: null,
    setName: '',
    attacks: [],
    weaknesses: [],
    resistances: [],
    retreatCost: [],
    ...overrides,
  });
}
