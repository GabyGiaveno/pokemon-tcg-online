import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { DeckApi } from './deck-api.service';
import { DeckResponse } from '../models/deck.model';
import { CreateDeckRequest, UpdateDeckRequest } from '../models/deck-request.model';
import { DeckValidationResponse } from '../models/deck-validation.model';
import { ErrorApi } from '../models/api-error.model';
import { MOCK_DECKS } from '../mocks/mock-decks';
import { MOCK_CARDS } from '../mocks/mock-cards';
import { toDeckCardResponse } from '../utils/deck-builder-mappers';

let nextId = 100;
let decks = [...MOCK_DECKS];

@Injectable({ providedIn: 'root' })
export class MockDeckApiService implements DeckApi {
  getDecks(): Observable<DeckResponse[]> {
    return of([...decks]);
  }

  getDeckById(id: number): Observable<DeckResponse> {
    const deck = decks.find((d) => d.id === id);
    if (!deck) {
      return throwError(() => this.buildError(404, 'Not Found', `Deck with id '${id}' not found`, `/api/decks/${id}`));
    }
    return of({ ...deck });
  }

  createDeck(request: CreateDeckRequest): Observable<DeckResponse> {
    const cardCount = request.cards.reduce((sum, e) => sum + e.quantity, 0);
    const deckCards = request.cards
      .map((entry) => {
        const card = MOCK_CARDS.find((c) => c.id === entry.cardId);
        if (!card) {
          return null;
        }
        return toDeckCardResponse(card, entry.quantity);
      })
      .filter((dc): dc is NonNullable<typeof dc> => dc !== null);

    const valid = cardCount === 60;
    const validationErrors = valid
      ? []
      : [
          {
            code: 'DECK_SIZE_INVALID' as const,
            message: 'Deck must contain exactly 60 cards',
            cardId: null as string | null,
          },
        ];

    const deck: DeckResponse = {
      id: nextId++,
      name: request.name,
      valid,
      cardCount,
      cards: deckCards,
      validationErrors,
      createdAt: new Date().toISOString(),
    };

    decks = [...decks, deck];
    return of({ ...deck });
  }

  updateDeck(id: number, request: UpdateDeckRequest): Observable<DeckResponse> {
    const existing = decks.find((d) => d.id === id);
    if (!existing) {
      return throwError(() => this.buildError(404, 'Not Found', `Deck with id '${id}' not found`, `/api/decks/${id}`));
    }

    const cardCount = request.cards.reduce((sum, e) => sum + e.quantity, 0);
    const deckCards = request.cards
      .map((entry) => {
        const card = MOCK_CARDS.find((c) => c.id === entry.cardId);
        if (!card) {
          return null;
        }
        return toDeckCardResponse(card, entry.quantity);
      })
      .filter((dc): dc is NonNullable<typeof dc> => dc !== null);

    const valid = cardCount === 60;
    const validationErrors = valid
      ? []
      : [
          {
            code: 'DECK_SIZE_INVALID' as const,
            message: 'Deck must contain exactly 60 cards',
            cardId: null as string | null,
          },
        ];

    const updated: DeckResponse = {
      ...existing,
      name: request.name,
      valid,
      cardCount,
      cards: deckCards,
      validationErrors,
      createdAt: existing.createdAt,
    };

    decks = decks.map((d) => (d.id === id ? updated : d));
    return of({ ...updated });
  }

  deleteDeck(id: number): Observable<void> {
    const exists = decks.some((d) => d.id === id);
    if (!exists) {
      return throwError(() => this.buildError(404, 'Not Found', `Deck with id '${id}' not found`, `/api/decks/${id}`));
    }
    decks = decks.filter((d) => d.id !== id);
    return of(void 0);
  }

  validateDeck(id: number): Observable<DeckValidationResponse> {
    const deck = decks.find((d) => d.id === id);
    if (!deck) {
      return throwError(() => this.buildError(404, 'Not Found', `Deck with id '${id}' not found`, `/api/decks/${id}/validate`));
    }

    const cardCount = deck.cardCount;
    const valid = cardCount === 60;
    const errors = valid
      ? []
      : [
          {
            code: 'DECK_SIZE_INVALID' as const,
            message: 'Deck must contain exactly 60 cards',
            cardId: null as string | null,
          },
        ];

    return of({ valid, errors, cardCount });
  }

  private buildError(
    status: number,
    error: string,
    message: string,
    path: string,
  ): ErrorApi {
    return {
      timestamp: new Date().toISOString(),
      status,
      error,
      message,
      path,
    };
  }
}
