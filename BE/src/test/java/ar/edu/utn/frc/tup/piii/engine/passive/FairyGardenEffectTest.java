package ar.edu.utn.frc.tup.piii.engine.passive;

import ar.edu.utn.frc.tup.piii.engine.passive.stadiums.FairyGardenEffect;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FairyGardenEffectTest {

    private final FairyGardenEffect effect = new FairyGardenEffect();

    @Test
    void costIsZeroWhenActiveHasFairyEnergy() {
        ActivePokemon active = ActivePokemon.builder()
                .cardId("xy1-1")
                .attachedEnergies(List.of(AttachedCard.builder().cardId("fairy-energy").build()))
                .build();

        Card fairyCard = new Card();
        fairyCard.setTypes(List.of("Fairy"));

        int result = effect.modifyRetreatCost(3, active, id -> fairyCard);

        assertEquals(0, result);
    }

    @Test
    void costUnchangedWhenNoFairyEnergy() {
        ActivePokemon active = ActivePokemon.builder()
                .cardId("xy1-1")
                .attachedEnergies(List.of(AttachedCard.builder().cardId("fire-energy").build()))
                .build();

        Card fireCard = new Card();
        fireCard.setTypes(List.of("Fire"));

        int result = effect.modifyRetreatCost(2, active, id -> fireCard);

        assertEquals(2, result);
    }

    @Test
    void costUnchangedWhenNullCardLookup() {
        ActivePokemon active = ActivePokemon.builder()
                .cardId("xy1-1")
                .attachedEnergies(List.of(AttachedCard.builder().cardId("some-energy").build()))
                .build();

        int result = effect.modifyRetreatCost(2, active, null);

        assertEquals(2, result);
    }
}
