package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.models.cards.effects.ShuffleHandEffect;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShuffleHandLogicTest {

    @Test
    void self_shufflesHandIntoDeckAndDraws() {
        PlayerField field = PlayerField.builder()
                .playerId(1L)
                .hand(new ArrayList<>(List.of("a", "b")))
                .deck(new ArrayList<>(List.of("c", "d", "e")))
                .build();

        ShuffleHandEffect effect = new ShuffleHandEffect();
        effect.setTarget("SELF");
        effect.setDrawAmount(2);

        AttackContext ctx = AttackContext.builder().attackerField(field).build();
        new ShuffleHandLogic().execute(effect, ctx);

        assertEquals(2, field.getHand().size(), "Roba drawAmount=2");
        assertEquals(3, field.getDeck().size(), "5 cartas en mazo - 2 robadas");
    }

    @Test
    void opponent_targetsDefenderField() {
        PlayerField defender = PlayerField.builder()
                .playerId(2L)
                .hand(new ArrayList<>(List.of("x", "y", "z")))
                .deck(new ArrayList<>(List.of("p", "q")))
                .build();

        ShuffleHandEffect effect = new ShuffleHandEffect();
        effect.setTarget("OPPONENT");
        effect.setDrawAmount(4);

        AttackContext ctx = AttackContext.builder().defenderField(defender).build();
        new ShuffleHandLogic().execute(effect, ctx);

        // 3 (mano) + 2 (mazo) = 5 total; roba min(4,5) = 4
        assertEquals(4, defender.getHand().size());
        assertEquals(1, defender.getDeck().size());
    }
}
