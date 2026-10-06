import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PokedexApi } from './pokedex-api.service';
import { CardResponse, CardPageResponse, CardSearchParams } from '../models/card-response';

/**
 * HTTP implementation of PokedexApi that calls the real backend.
 * Uses the Angular proxy (/api/* → localhost:8080).
 */
@Injectable({ providedIn: 'root' })
export class HttpPokedexApiService implements PokedexApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/cards';

  getCards(params: CardSearchParams): Observable<CardPageResponse> {
    let httpParams = new HttpParams();

    if (params.name) httpParams = httpParams.set('name', params.name);
    if (params.type) httpParams = httpParams.set('type', params.type);
    if (params.supertype) httpParams = httpParams.set('supertype', params.supertype);
    if (params.set) httpParams = httpParams.set('set', params.set);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params.sort) httpParams = httpParams.set('sort', params.sort);

    return this.http.get<CardPageResponse>(this.baseUrl, { params: httpParams });
  }

  getCardById(id: string): Observable<CardResponse> {
    return this.http.get<CardResponse>(`${this.baseUrl}/${id}`);
  }

  syncSet(setId: string): Observable<void> {
    const params = new HttpParams().set('set', setId);
    return this.http.post(`${this.baseUrl}/sync`, null, { params }).pipe(map(() => undefined));
  }
}
