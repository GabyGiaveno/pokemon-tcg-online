import { Injectable } from '@angular/core';
import { Observable, of, delay, throwError } from 'rxjs';
import { PokedexApi } from './pokedex-api.service';
import { CardResponse, CardPageResponse, CardSearchParams } from '../models/card-response';
import { MOCK_POKEDEX_CARDS } from '../mocks/pokedex-mock-cards';
import { PokedexCard } from '../../domain/models/pokedex-card';

function toCardResponse(card: PokedexCard): CardResponse {
  return {
    id: card.id,
    name: card.name,
    supertype: card.supertype,
    subtypes: card.subtypes,
    hp: card.hp,
    types: card.types,
    cardSetId: card.cardSetId,
    cardSetName: card.cardSetName,
    imageUrlSmall: card.imageUrlSmall,
    imageUrlLarge: card.imageUrlLarge,
    evolvesFrom: card.evolvesFrom,
    retreatCost: card.retreatCost,
    aceTactician: card.aceTactician,
    attacks: card.attacks ? JSON.stringify(card.attacks) : null,
    weaknesses: card.weaknesses ? JSON.stringify(card.weaknesses) : null,
    resistances: card.resistances ? JSON.stringify(card.resistances) : null,
  };
}

const MOCK_CARD_RESPONSES: CardResponse[] = MOCK_POKEDEX_CARDS.map(toCardResponse);

@Injectable({ providedIn: 'root' })
export class MockPokedexApiService implements PokedexApi {
  getCards(params: CardSearchParams): Observable<CardPageResponse> {
    let filtered = [...MOCK_CARD_RESPONSES];

    if (params.name) {
      const term = params.name.toLowerCase();
      filtered = filtered.filter(c => c.name.toLowerCase().includes(term));
    }

    if (params.type) {
      filtered = filtered.filter(c =>
        c.types.some(t => t.toLowerCase() === params.type!.toLowerCase()),
      );
    }

    if (params.supertype) {
      filtered = filtered.filter(
        c => c.supertype.toLowerCase() === params.supertype!.toLowerCase(),
      );
    }

    if (params.set) {
      filtered = filtered.filter(c => c.cardSetId === params.set);
    }

    const total = filtered.length;
    const page = params.page ?? 0;
    const size = params.size ?? 20;
    const start = page * size;
    const paged = filtered.slice(start, start + size);

    return of({ data: paged, total, page, size }).pipe(delay(300));
  }

  getCardById(id: string): Observable<CardResponse> {
    const card = MOCK_CARD_RESPONSES.find(c => c.id === id);
    if (!card) {
      return throwError(() => new Error(`Card with id '${id}' not found`));
    }
    return of(card).pipe(delay(150));
  }

  syncSet(_setId: string): Observable<void> {
    return of(undefined).pipe(delay(800));
  }
}
