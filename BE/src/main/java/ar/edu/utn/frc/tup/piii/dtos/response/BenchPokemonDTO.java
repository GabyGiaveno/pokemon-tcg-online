package ar.edu.utn.frc.tup.piii.dtos.response;

import java.util.List;

/**
 * DTO projecting a benched Pokémon.
 *
 * <p>Energies are exposed as {@link CardInstanceDTO} so the frontend has both
 * {@code instanceId} and {@code cardId} for each attached energy.
 */
public class BenchPokemonDTO {
    private String instanceId;
    private String cardId;
    private int hp;
    private int maxHp;
    private List<CardInstanceDTO> attachedEnergies;

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public List<CardInstanceDTO> getAttachedEnergies() { return attachedEnergies; }
    public void setAttachedEnergies(List<CardInstanceDTO> attachedEnergies) { this.attachedEnergies = attachedEnergies; }
}
