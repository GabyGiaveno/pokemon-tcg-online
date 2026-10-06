import { Observable } from 'rxjs';
import { CardResponse, CardPageResponse, CardSearchParams } from '../models/card-response';

export abstract class PokedexApi {
  abstract getCards(params: CardSearchParams): Observable<CardPageResponse>;
  abstract getCardById(id: string): Observable<CardResponse>;
  abstract syncSet(setId: string): Observable<void>;
}
