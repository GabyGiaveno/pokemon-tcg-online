export interface DeckCardEntry {
  cardId: string;
  quantity: number;
}

export interface CreateDeckRequest {
  name: string;
  cards: DeckCardEntry[];
}

export interface UpdateDeckRequest {
  name: string;
  cards: DeckCardEntry[];
}
