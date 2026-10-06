package ar.edu.utn.frc.tup.piii.engine.chain.handlers;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackData;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * REQ-A0 (spec effect-execution): paying an attack's cost does NOT discard the
 * attached energies — XY rule: energy stays attached unless an explicit effect,
 * retreat, or leaving play removes it. The two-pass pool validation (specific
 * types first, Colorless with anything left) is preserved.
 */
class EnergyValidationHandlerTest {

    private CardLookup cardLookup;
    private ActivePokemon attacker;

    @BeforeEach
    void setUp() {
        cardLookup = Mockito.mock(CardLookup.class);
        attacker = ActivePokemon.builder()
                .cardId("atk-1").maxHp(100).currentHp(100)
                .attachedEnergies(new ArrayList<>())
                .build();
    }

    private void attachEnergy(String cardId, String color) {
        attacker.getAttachedEnergies().add(AttachedCard.builder().cardId(cardId).build());
        Card energy = new Card();
        energy.setId(cardId);
        energy.setTypes(List.of(color));
        when(cardLookup.findById(cardId)).thenReturn(energy);
    }

    private AttackContext context(List<String> cost) {
        AttackData attack = new AttackData();
        attack.setName("Test Attack");
        attack.setCost(cost);
        return AttackContext.builder()
                .cardLookup(cardLookup)
                .attackerPokemon(attacker)
                .attackData(attack)
                .build();
    }

    @Test
    void validAttack_keepsEnergiesAttached() {
        attachEnergy("e1", "Fire");
        attachEnergy("e2", "Colorless");
        AttackContext ctx = context(List.of("Fire", "Colorless"));

        new EnergyValidationHandler().handle(ctx);

        assertFalse(ctx.isAttackCancelled());
        assertEquals(2, attacker.getAttachedEnergies().size(),
                "paying the attack cost must NOT discard attached energy (XY rules)");
    }

    @Test
    void validAttack_colorlessSatisfiedByAnyType_keepsEnergies() {
        attachEnergy("e1", "Water");
        attachEnergy("e2", "Water");
        AttackContext ctx = context(List.of("Colorless", "Colorless"));

        new EnergyValidationHandler().handle(ctx);

        assertFalse(ctx.isAttackCancelled());
        assertEquals(2, attacker.getAttachedEnergies().size());
    }

    @Test
    void insufficientEnergy_cancelsWithoutMutating() {
        attachEnergy("e1", "Water");
        AttackContext ctx = context(List.of("Fire", "Colorless"));

        new EnergyValidationHandler().handle(ctx);

        assertTrue(ctx.isAttackCancelled());
        assertEquals(1, attacker.getAttachedEnergies().size());
    }

    @Test
    void freeAttack_noCost_passesAndKeepsEnergies() {
        attachEnergy("e1", "Psychic");
        AttackContext ctx = context(List.of());

        new EnergyValidationHandler().handle(ctx);

        assertFalse(ctx.isAttackCancelled());
        assertEquals(1, attacker.getAttachedEnergies().size());
    }
}
