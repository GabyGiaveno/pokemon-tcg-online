package ar.edu.utn.frc.tup.piii.models.game;

import ar.edu.utn.frc.tup.piii.models.cards.CardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** In-memory model of a card attached to a Pokemon (energy or tool). */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttachedCard {
    private String instanceId;
    private String cardId;
    private CardType type; // BASIC_ENERGY, SPECIAL_ENERGY, o POKEMON_TOOL
}
