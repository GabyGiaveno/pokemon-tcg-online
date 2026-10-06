import { DeckValidationResponse } from '../models/deck-validation.model';

export const MOCK_VALID_VALIDATION: DeckValidationResponse = {
  valid: true,
  errors: [],
  cardCount: 60,
};

export const MOCK_INVALID_VALIDATION: DeckValidationResponse = {
  valid: false,
  errors: [
    { code: 'DECK_SIZE_INVALID', message: 'Deck must contain exactly 60 cards', cardId: null },
    { code: 'TOO_MANY_COPIES', message: 'Only 4 copies of the same card are allowed', cardId: 'xy1-42' },
  ],
  cardCount: 38,
};

export const MOCK_EMPTY_VALIDATION: DeckValidationResponse = {
  valid: false,
  errors: [
    { code: 'EMPTY_DECK', message: 'Deck must contain at least 1 card', cardId: null },
  ],
  cardCount: 0,
};
