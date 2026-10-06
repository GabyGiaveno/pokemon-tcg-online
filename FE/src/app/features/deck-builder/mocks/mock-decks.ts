import { DeckResponse } from '../models/deck.model';

export const MOCK_VALID_DECK: DeckResponse = {
  id: 1,
  name: 'Mazo Eléctrico',
  valid: true,
  cardCount: 60,
  cards: [
    { cardId: 'xy1-6', cardName: 'Pikachu', quantity: 4, supertype: 'Pokémon', types: ['Lightning'], subtypes: ['Basic'], imageUrlSmall: 'https://images.pokemontcg.io/xy1/6.png' },
    { cardId: 'xy1-7', cardName: 'Raichu', quantity: 3, supertype: 'Pokémon', types: ['Lightning'], subtypes: ['Stage 1'], imageUrlSmall: 'https://images.pokemontcg.io/xy1/7.png' },
    { cardId: 'xy1-17', cardName: 'Lightning Energy', quantity: 20, supertype: 'Energy', types: ['Lightning'], subtypes: ['Basic'], imageUrlSmall: 'https://images.pokemontcg.io/xy1/17.png' },
    { cardId: 'xy1-11', cardName: "Professor's Letter", quantity: 4, supertype: 'Trainer', types: [], subtypes: ['Item'], imageUrlSmall: 'https://images.pokemontcg.io/xy1/11.png' },
    { cardId: 'xy1-13', cardName: 'Professor Sycamore', quantity: 4, supertype: 'Trainer', types: [], subtypes: ['Supporter'], imageUrlSmall: 'https://images.pokemontcg.io/xy1/13.png' },
  ],
  validationErrors: [],
  createdAt: '2026-06-08T21:30:00',
};

export const MOCK_INVALID_DECK: DeckResponse = {
  id: 2,
  name: 'Mazo Incompleto',
  valid: false,
  cardCount: 38,
  cards: [
    { cardId: 'xy1-1', cardName: 'Venusaur-EX', quantity: 1, supertype: 'Pokémon', types: ['Grass'], subtypes: ['Basic', 'EX'], imageUrlSmall: 'https://images.pokemontcg.io/xy1/1.png' },
    { cardId: 'xy1-6', cardName: 'Pikachu', quantity: 2, supertype: 'Pokémon', types: ['Lightning'], subtypes: ['Basic'], imageUrlSmall: 'https://images.pokemontcg.io/xy1/6.png' },
    { cardId: 'xy1-14', cardName: 'Grass Energy', quantity: 10, supertype: 'Energy', types: ['Grass'], subtypes: ['Basic'], imageUrlSmall: 'https://images.pokemontcg.io/xy1/14.png' },
    { cardId: 'xy1-17', cardName: 'Lightning Energy', quantity: 10, supertype: 'Energy', types: ['Lightning'], subtypes: ['Basic'], imageUrlSmall: 'https://images.pokemontcg.io/xy1/17.png' },
  ],
  validationErrors: [
    { code: 'DECK_SIZE_INVALID', message: 'Deck must contain exactly 60 cards', cardId: null },
    { code: 'TOO_MANY_TYPES', message: 'Deck should focus on a single type', cardId: null },
  ],
  createdAt: '2026-06-08T21:35:00',
};

export const MOCK_DECKS: DeckResponse[] = [MOCK_VALID_DECK, MOCK_INVALID_DECK];
