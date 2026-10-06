import { Observable } from 'rxjs';
import { CardResponse, CardPageResponse, CardSearchParams } from '../models/card.model';

export abstract class CardApi {
  abstract getCards(params: CardSearchParams): Observable<CardPageResponse>;
  abstract getCardById(id: string): Observable<CardResponse>;
}
