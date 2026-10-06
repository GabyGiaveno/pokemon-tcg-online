import { DeckValidationError } from './deck-validation.model';

export interface DeckCardResponse {
  cardId: string;
  cardName: string;
  quantity: number;
  supertype: string;
  types: string[];
  subtypes: string[];
  imageUrlSmall: string;
}

export interface DeckResponse {
  id: number;
  name: string;
  valid: boolean;
  cardCount: number;
  cards: DeckCardResponse[];
  validationErrors: DeckValidationError[];
  createdAt: string;
}

export type DeckListResponse = DeckResponse[];
