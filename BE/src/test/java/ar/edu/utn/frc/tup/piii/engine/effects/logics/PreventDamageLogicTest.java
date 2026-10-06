package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackData;
import ar.edu.utn.frc.tup.piii.engine.chain.handlers.DamageApplicationHandler;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.PreventDamageEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreventDamageLogicTest {

    private PreventDamageLogic logic;
    private ActivePokemon attacker;
    private ActivePokemon defender;
    private AttackContext ctx;

    @BeforeEach
    void setUp() {
        logic = new PreventDamageLogic();

        attacker = ActivePokemon.builder().cardId("xy1-46").currentHp(80).maxHp(80).build();
        defender = ActivePokemon.builder().cardId("xy1-1").currentHp(100).maxHp(100).build();

        ctx = AttackContext.builder()
                .attackerPokemon(attacker)
                .defenderPokemon(defender)
                .build();
    }

    private PreventDamageEffect effect() {
        PreventDamageEffect e = new PreventDamageEffect();
        e.setTarget("self");
        return e;
    }

    @Test
    void isPostDamage_returnsTrue() {
        assertTrue(logic.isPostDamage());
    }

    @Test
    void isPreDamage_returnsFalse() {
        assertFalse(logic.isPreDamage());
    }

    @Test
    void execute_setsDamageProtectedOnAttacker() {
        logic.execute(effect(), ctx);
        assertTrue(attacker.isDamageProtected());
    }

    @Test
    void execute_doesNotAffectDefender() {
        logic.execute(effect(), ctx);
        assertFalse(defender.isDamageProtected());
    }

    @Test
    void execute_emitsDamagePreventedEvent() {
        logic.execute(effect(), ctx);
        assertEquals(1, ctx.getEvents().size());
        assertEquals(GameEventType.DAMAGE_PREVENTED, ctx.getEvents().get(0).getType());
    }

    @Test
    void damageApplicationHandler_blocksHitWhenProtected() {
        // Attacker used Harden last turn → is now protected
        attacker.setDamageProtected(true);

        // Next turn: attacker is now the defender being hit
        ActivePokemon nowDefender = attacker;
        AttackData attackData = new AttackData("Tackle", null, 1, "30", null, null);
        AttackContext hitCtx = AttackContext.builder()
                .attackerPokemon(defender)
                .defenderPokemon(nowDefender)
                .attackData(attackData)
                .attackerCard(null)
                .defenderCard(null)
                .build();

        new DamageApplicationHandler().handle(hitCtx);

        assertEquals(0, hitCtx.getFinalDamage());
        assertEquals(80, nowDefender.getCurrentHp(), "HP must be unchanged when protected");
        assertFalse(nowDefender.isDamageProtected(), "Protection consumed after absorbing one hit");
        assertEquals(1, hitCtx.getEvents().size());
        assertEquals(GameEventType.DAMAGE_PREVENTED, hitCtx.getEvents().get(0).getType());
    }

    @Test
    void damageApplicationHandler_protectionConsumedAfterOneHit() {
        attacker.setDamageProtected(true);
        AttackData attackData = new AttackData("Tackle", null, 1, "30", null, null);
        AttackContext hitCtx = AttackContext.builder()
                .attackerPokemon(defender)
                .defenderPokemon(attacker)
                .attackData(attackData)
                .attackerCard(null)
                .defenderCard(null)
                .build();

        new DamageApplicationHandler().handle(hitCtx);
        assertFalse(attacker.isDamageProtected());

        // Second hit — no longer protected
        attacker.setCurrentHp(80);
        new DamageApplicationHandler().handle(hitCtx);
        assertTrue(attacker.getCurrentHp() < 80, "Second hit should apply damage");
    }
}
