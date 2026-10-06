import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CardApi } from './card-api.service';
import { CardPageResponse, CardResponse, CardSearchParams } from '../models/card.model';

@Injectable({ providedIn: 'root' })
export class CardHttpApiService implements CardApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/cards';

  getCards(params: CardSearchParams): Observable<CardPageResponse> {
    let httpParams = new HttpParams();

    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }

    return this.http.get<CardPageResponse>(this.baseUrl, { params: httpParams });
  }

  getCardById(id: string): Observable<CardResponse> {
    return this.http.get<CardResponse>(`${this.baseUrl}/${id}`);
  }
}
