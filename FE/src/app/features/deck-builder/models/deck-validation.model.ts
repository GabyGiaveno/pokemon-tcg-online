export interface DeckValidationError {
  code: string;
  message: string;
  cardId: string | null;
}

export interface DeckValidationResponse {
  valid: boolean;
  errors: DeckValidationError[];
  cardCount: number;
}
