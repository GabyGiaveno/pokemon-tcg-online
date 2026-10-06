package ar.edu.utn.frc.tup.piii.dtos.response;

import java.util.List;

/**
 * DTO representing a single card instance within a game.
 *
 * <p>Every card in a player's hand, deck, prize cards or discard pile
 * is an instance with a unique {@code instanceId} and a global {@code cardId}.
 *
 * <p>The frontend uses {@code instanceId} to reference a specific copy in actions
 * (e.g. playing a Pokémon, attaching an energy), and {@code cardId} to display
 * the card image/name via the public card catalog.
 */
public class CardInstanceDTO {

    private String instanceId;
    private String cardId;
    private String name;
    private List<String> subtypes;
    /** Card category: "Pokémon", "Energy", or "Trainer". Lets the FE classify hand cards. */
    private String supertype;

    public CardInstanceDTO() {}

    public CardInstanceDTO(String instanceId, String cardId, String name) {
        this.instanceId = instanceId;
        this.cardId = cardId;
        this.name = name;
    }

    public CardInstanceDTO(String instanceId, String cardId, String name, List<String> subtypes) {
        this.instanceId = instanceId;
        this.cardId = cardId;
        this.name = name;
        this.subtypes = subtypes;
    }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getSubtypes() { return subtypes; }
    public void setSubtypes(List<String> subtypes) { this.subtypes = subtypes; }
    public String getSupertype() { return supertype; }
    public void setSupertype(String supertype) { this.supertype = supertype; }
}
