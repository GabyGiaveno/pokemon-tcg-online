import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DeckApi } from './deck-api.service';
import { DeckResponse } from '../models/deck.model';
import { CreateDeckRequest, UpdateDeckRequest } from '../models/deck-request.model';
import { DeckValidationResponse } from '../models/deck-validation.model';

@Injectable({ providedIn: 'root' })
export class DeckHttpApiService implements DeckApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/decks';

  getDecks(): Observable<DeckResponse[]> {
    return this.http.get<DeckResponse[]>(this.baseUrl);
  }

  getDeckById(id: number): Observable<DeckResponse> {
    return this.http.get<DeckResponse>(`${this.baseUrl}/${id}`);
  }

  createDeck(request: CreateDeckRequest): Observable<DeckResponse> {
    return this.http.post<DeckResponse>(this.baseUrl, request);
  }

  updateDeck(id: number, request: UpdateDeckRequest): Observable<DeckResponse> {
    return this.http.put<DeckResponse>(`${this.baseUrl}/${id}`, request);
  }

  deleteDeck(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  validateDeck(id: number): Observable<DeckValidationResponse> {
    return this.http.get<DeckValidationResponse>(`${this.baseUrl}/${id}/validate`);
  }
}
