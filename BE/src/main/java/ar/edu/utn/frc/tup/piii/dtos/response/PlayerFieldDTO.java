package ar.edu.utn.frc.tup.piii.dtos.response;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO projecting the authenticated player's field.
 *
 * <p>All card references use {@link CardInstanceDTO} so the frontend always
 * has both {@code instanceId} (to reference in actions) and {@code cardId}
 * (to display card details).
 */
public class PlayerFieldDTO {
    private ActivePokemonDTO activePokemon;
    private List<BenchPokemonDTO> bench = new ArrayList<>();
    private List<CardInstanceDTO> hand = new ArrayList<>();
    private int deckSize;
    private List<CardInstanceDTO> prizeCards = new ArrayList<>();
    private List<CardInstanceDTO> discardPile = new ArrayList<>();

    public ActivePokemonDTO getActivePokemon() { return activePokemon; }
    public void setActivePokemon(ActivePokemonDTO activePokemon) { this.activePokemon = activePokemon; }
    public List<BenchPokemonDTO> getBench() { return bench; }
    public void setBench(List<BenchPokemonDTO> bench) { this.bench = bench; }
    public List<CardInstanceDTO> getHand() { return hand; }
    public void setHand(List<CardInstanceDTO> hand) { this.hand = hand; }
    public int getDeckSize() { return deckSize; }
    public void setDeckSize(int deckSize) { this.deckSize = deckSize; }
    public List<CardInstanceDTO> getPrizeCards() { return prizeCards; }
    public void setPrizeCards(List<CardInstanceDTO> prizeCards) { this.prizeCards = prizeCards; }
    public List<CardInstanceDTO> getDiscardPile() { return discardPile; }
    public void setDiscardPile(List<CardInstanceDTO> discardPile) { this.discardPile = discardPile; }
}
