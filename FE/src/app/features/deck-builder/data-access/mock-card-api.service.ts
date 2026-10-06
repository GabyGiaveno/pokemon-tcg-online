import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { CardApi } from './card-api.service';
import { CardResponse, CardPageResponse, CardSearchParams } from '../models/card.model';
import { ErrorApi } from '../models/api-error.model';
import { MOCK_CARDS } from '../mocks/mock-cards';

@Injectable({ providedIn: 'root' })
export class MockCardApiService implements CardApi {
  getCards(params: CardSearchParams): Observable<CardPageResponse> {
    let filtered = [...MOCK_CARDS];

    if (params.name) {
      const term = params.name.toLowerCase();
      filtered = filtered.filter((c) => c.name.toLowerCase().includes(term));
    }

    if (params.supertype) {
      filtered = filtered.filter(
        (c) => c.supertype.toLowerCase() === params.supertype!.toLowerCase(),
      );
    }

    if (params.type) {
      filtered = filtered.filter((c) =>
        c.types.some((t) => t.toLowerCase() === params.type!.toLowerCase()),
      );
    }

    const total = filtered.length;
    const page = params.page ?? 0;
    const size = params.size ?? 20;
    const start = page * size;
    const paged = filtered.slice(start, start + size);

    return of({
      data: paged,
      total,
      page,
      size,
    });
  }

  getCardById(id: string): Observable<CardResponse> {
    const card = MOCK_CARDS.find((c) => c.id === id);
    if (!card) {
      const error: ErrorApi = {
        timestamp: new Date().toISOString(),
        status: 404,
        error: 'Not Found',
        message: `Card with id '${id}' not found`,
        path: `/api/cards/${id}`,
      };
      return throwError(() => error);
    }
    return of(card);
  }
}
