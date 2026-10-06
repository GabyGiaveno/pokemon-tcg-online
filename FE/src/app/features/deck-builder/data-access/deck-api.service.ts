import { Observable } from 'rxjs';
import { DeckResponse } from '../models/deck.model';
import { CreateDeckRequest, UpdateDeckRequest } from '../models/deck-request.model';
import { DeckValidationResponse } from '../models/deck-validation.model';

export abstract class DeckApi {
  abstract getDecks(): Observable<DeckResponse[]>;
  abstract getDeckById(id: number): Observable<DeckResponse>;
  abstract createDeck(request: CreateDeckRequest): Observable<DeckResponse>;
  abstract updateDeck(id: number, request: UpdateDeckRequest): Observable<DeckResponse>;
  abstract deleteDeck(id: number): Observable<void>;
  abstract validateDeck(id: number): Observable<DeckValidationResponse>;
}
