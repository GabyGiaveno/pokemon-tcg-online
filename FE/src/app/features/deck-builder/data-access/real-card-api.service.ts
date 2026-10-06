import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CardApi } from './card-api.service';
import { CardResponse, CardPageResponse, CardSearchParams } from '../models/card.model';

@Injectable()
export class RealCardApiService implements CardApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/cards';

  getCards(params: CardSearchParams): Observable<CardPageResponse> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);

    if (params.name) {
      httpParams = httpParams.set('name', params.name);
    }
    if (params.supertype) {
      httpParams = httpParams.set('supertype', params.supertype);
    }
    if (params.type) {
      httpParams = httpParams.set('type', params.type);
    }
    if (params.set) {
      httpParams = httpParams.set('set', params.set);
    }
    if (params.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }

    return this.http.get<CardPageResponse>(this.baseUrl, { params: httpParams });
  }

  getCardById(id: string): Observable<CardResponse> {
    return this.http.get<CardResponse>(`${this.baseUrl}/${id}`);
  }
}
