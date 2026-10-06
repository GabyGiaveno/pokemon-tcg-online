package ar.edu.utn.frc.tup.piii.dtos.response;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO projecting the opponent's field.
 *
 * <p>The opponent's hand, deck contents and prize cards are hidden:
 * only {@code handSize}, {@code deckSize}, and null prize slots are exposed.
 * Discard pile is public and uses {@link CardInstanceDTO}.
 */
public class OpponentFieldDTO {
    private ActivePokemonDTO activePokemon;
    private List<BenchPokemonDTO> bench = new ArrayList<>();
    private int handSize;
    private int deckSize;
    private List<String> prizeCards = new ArrayList<>();
    private List<CardInstanceDTO> discardPile = new ArrayList<>();

    public ActivePokemonDTO getActivePokemon() { return activePokemon; }
    public void setActivePokemon(ActivePokemonDTO activePokemon) { this.activePokemon = activePokemon; }
    public List<BenchPokemonDTO> getBench() { return bench; }
    public void setBench(List<BenchPokemonDTO> bench) { this.bench = bench; }
    public int getHandSize() { return handSize; }
    public void setHandSize(int handSize) { this.handSize = handSize; }
    public int getDeckSize() { return deckSize; }
    public void setDeckSize(int deckSize) { this.deckSize = deckSize; }
    public List<String> getPrizeCards() { return prizeCards; }
    public void setPrizeCards(List<String> prizeCards) { this.prizeCards = prizeCards; }
    public List<CardInstanceDTO> getDiscardPile() { return discardPile; }
    public void setDiscardPile(List<CardInstanceDTO> discardPile) { this.discardPile = discardPile; }
}
