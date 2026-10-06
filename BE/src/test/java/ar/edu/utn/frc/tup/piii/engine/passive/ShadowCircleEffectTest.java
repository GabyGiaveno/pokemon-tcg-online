package ar.edu.utn.frc.tup.piii.engine.passive;

import ar.edu.utn.frc.tup.piii.engine.passive.stadiums.ShadowCircleEffect;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowCircleEffectTest {

    private final ShadowCircleEffect effect = new ShadowCircleEffect();

    @Test
    void suppressesWeaknessWhenDefenderHasDarknessEnergy() {
        ActivePokemon defender = ActivePokemon.builder()
                .cardId("xy1-2")
                .attachedEnergies(List.of(AttachedCard.builder().cardId("dark-energy").build()))
                .build();

        Card darkCard = new Card();
        darkCard.setTypes(List.of("Darkness"));

        assertTrue(effect.suppressesWeakness(defender, id -> darkCard));
    }

    @Test
    void doesNotSuppressWhenNoDarknessEnergy() {
        ActivePokemon defender = ActivePokemon.builder()
                .cardId("xy1-2")
                .attachedEnergies(List.of(AttachedCard.builder().cardId("water-energy").build()))
                .build();

        Card waterCard = new Card();
        waterCard.setTypes(List.of("Water"));

        assertFalse(effect.suppressesWeakness(defender, id -> waterCard));
    }

    @Test
    void doesNotSuppressWhenNullCardLookup() {
        ActivePokemon defender = ActivePokemon.builder()
                .cardId("xy1-2")
                .attachedEnergies(List.of(AttachedCard.builder().cardId("dark-energy").build()))
                .build();

        assertFalse(effect.suppressesWeakness(defender, null));
    }
}
