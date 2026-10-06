package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.models.cards.effects.DiscardEnergyEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscardEnergyLogicTest {

    @Test
    void isPostDamage_true() {
        assertTrue(new DiscardEnergyLogic().isPostDamage());
    }

    @Test
    void discardsRequestedEnergyFromSelf() {
        ActivePokemon attacker = new ActivePokemon();
        attacker.setCardId("attacker-1");
        List<AttachedCard> energies = new ArrayList<>();
        energies.add(AttachedCard.builder().instanceId("e1").cardId("e1").build());
        energies.add(AttachedCard.builder().instanceId("e2").cardId("e2").build());
        attacker.setAttachedEnergies(energies);

        DiscardEnergyEffect effect = new DiscardEnergyEffect();
        effect.setAmount(1);
        effect.setTarget("SELF");

        AttackContext ctx = AttackContext.builder().attackerPokemon(attacker).build();
        new DiscardEnergyLogic().execute(effect, ctx);

        assertEquals(1, attacker.getAttachedEnergies().size(), "Debe quedar 1 energía tras descartar 1");
    }
}
