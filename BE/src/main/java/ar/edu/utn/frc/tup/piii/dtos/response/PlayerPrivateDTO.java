package ar.edu.utn.frc.tup.piii.dtos.response;

import java.util.List;

/**
 * Legacy DTO from the old private WebSocket topic design.
 * <p>
 * The current realtime contract does not send private state through WebSocket.
 * Clients receive only game events/state invalidations over STOMP and fetch the
 * filtered private board through {@code GET /api/games/{gameId}/state} with JWT.
 */
public class PlayerPrivateDTO {
    private List<String> hand;
    private List<String> prizeCards;
    private int deckSize;

    public List<String> getHand() { return hand; }
    public void setHand(List<String> hand) { this.hand = hand; }
    public List<String> getPrizeCards() { return prizeCards; }
    public void setPrizeCards(List<String> prizeCards) { this.prizeCards = prizeCards; }
    public int getDeckSize() { return deckSize; }
    public void setDeckSize(int deckSize) { this.deckSize = deckSize; }
}
