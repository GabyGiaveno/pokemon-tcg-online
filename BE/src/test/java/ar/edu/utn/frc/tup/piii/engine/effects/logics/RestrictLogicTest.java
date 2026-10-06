package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.RestrictEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class RestrictLogicTest {

    private RestrictLogic logic;
    private ActivePokemon attacker;
    private ActivePokemon defender;
    private PlayerField attackerField;
    private PlayerField defenderField;
    private AttackContext ctx;

    @BeforeEach
    void setUp() {
        logic = new RestrictLogic();

        attacker = ActivePokemon.builder().cardId("xy1-46").build();
        defender = ActivePokemon.builder().cardId("xy1-1").build();

        attackerField = new PlayerField();
        attackerField.setPlayerId(1L);
        attackerField.setTurnFlags(new TurnFlags());
        attackerField.setBench(new ArrayList<>());

        defenderField = new PlayerField();
        defenderField.setPlayerId(2L);
        defenderField.setTurnFlags(new TurnFlags());
        defenderField.setBench(new ArrayList<>());

        ctx = AttackContext.builder()
                .attackerPokemon(attacker)
                .defenderPokemon(defender)
                .attackerField(attackerField)
                .defenderField(defenderField)
                .board(new BoardState())
                .cardLookup(id -> null)
                .build();
    }

    private RestrictEffect effect(String restriction, String target) {
        RestrictEffect e = new RestrictEffect();
        e.setRestriction(restriction);
        e.setTarget(target);
        e.setDuration("NEXT_TURN");
        return e;
    }

    @Test
    void isPostDamage_returnsTrue() {
        assertTrue(logic.isPostDamage());
    }

    @Test
    void execute_attackRestriction_addedToDefender() {
        logic.execute(effect("ATTACK", "DEFENDER"), ctx);
        assertTrue(defender.getRestrictions().contains("ATTACK"));
    }

    @Test
    void execute_retreatRestriction_addedToDefender() {
        logic.execute(effect("RETREAT", "DEFENDER"), ctx);
        assertTrue(defender.getRestrictions().contains("RETREAT"));
    }

    @Test
    void execute_attackRestriction_onSelf_addedToAttacker() {
        logic.execute(effect("ATTACK", "SELF"), ctx);
        assertTrue(attacker.getRestrictions().contains("ATTACK"));
        assertFalse(defender.getRestrictions().contains("ATTACK"));
    }

    @Test
    void execute_supporterRestriction_addedToOpponentPlayerField() {
        logic.execute(effect("SUPPORTER", "OPPONENT"), ctx);
        assertTrue(defenderField.getPlayerRestrictions().contains("SUPPORTER"));
        assertFalse(attackerField.getPlayerRestrictions().contains("SUPPORTER"));
    }

    @Test
    void execute_emitsRestrictedEvent() {
        logic.execute(effect("ATTACK", "DEFENDER"), ctx);
        assertEquals(1, ctx.getEvents().size());
        assertEquals(GameEventType.POKEMON_RESTRICTED, ctx.getEvents().get(0).getType());
    }

    @Test
    void execute_nullRestriction_doesNothing() {
        logic.execute(effect(null, "DEFENDER"), ctx);
        assertTrue(ctx.getEvents().isEmpty());
        assertTrue(defender.getRestrictions().isEmpty());
    }

    @Test
    void restrictions_clearedAtStartOfTurn() {
        defender.getRestrictions().add("ATTACK");
        defender.getRestrictions().clear();
        assertTrue(defender.getRestrictions().isEmpty());
    }
}
