import { DeckBuilderCard } from '../models/deck-builder-state.model';
import { CreateDeckRequest, UpdateDeckRequest } from '../models/deck-request.model';
import { DeckCardEntry } from '../models/deck-request.model';
import { CardResponse } from '../models/card.model';
import { DeckCardResponse } from '../models/deck.model';

export function toCreateDeckRequest(
  name: string,
  deckCards: DeckBuilderCard[],
): CreateDeckRequest {
  return {
    name,
    cards: deckCards.map((item) => ({
      cardId: item.card.id,
      quantity: item.quantity,
    })),
  };
}

export function toDeckCardEntry(cardId: string, quantity: number): DeckCardEntry {
  return { cardId, quantity };
}

export function toDeckCardResponse(
  card: CardResponse,
  quantity: number,
): DeckCardResponse {
  return {
    cardId: card.id,
    cardName: card.name,
    quantity,
    supertype: card.supertype,
    types: card.types,
    subtypes: card.subtypes,
    imageUrlSmall: card.imageUrlSmall,
  };
}

/**
 * Converts a {@link DeckCardResponse} (returned by GET /api/decks/:id) into a
 * partial {@link CardResponse} with sensible defaults for fields the deck
 * endpoint does not return.
 */
export function deckCardResponseToCardResponse(dcr: DeckCardResponse): CardResponse {
  return {
    id: dcr.cardId,
    name: dcr.cardName,
    supertype: dcr.supertype,
    subtypes: dcr.subtypes,
    hp: null,
    types: dcr.types,
    cardSetId: '',
    cardSetName: '',
    imageUrlSmall: dcr.imageUrlSmall,
    imageUrlLarge: '',
    aceTactician: false,
    evolvesFrom: null,
    attacks: null,
    weaknesses: null,
    resistances: null,
    retreatCost: [],
  };
}

/**
 * Maps a {@link DeckCardResponse} array from the API into the internal
 * {@link DeckBuilderCard} format the deck builder page works with.
 */
export function deckCardsToBuilderCards(cards: DeckCardResponse[]): DeckBuilderCard[] {
  return cards.map((dcr) => ({
    card: deckCardResponseToCardResponse(dcr),
    quantity: dcr.quantity,
  }));
}

/**
 * Convenience alias — {@link UpdateDeckRequest} has the same shape as
 * {@link CreateDeckRequest}.
 */
export function toUpdateDeckRequest(
  name: string,
  deckCards: DeckBuilderCard[],
): UpdateDeckRequest {
  return {
    name,
    cards: deckCards.map((item) => ({
      cardId: item.card.id,
      quantity: item.quantity,
    })),
  };
}
