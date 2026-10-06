package ar.edu.utn.frc.tup.piii.engine.chain;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttackContextTest {

    @Test
    void builder_populatesAttackParticipantsAndStartsWithEmptyEvents() {
        BoardState board = new BoardState();
        PlayerField attackerField = PlayerField.builder().playerId(1L).build();
        PlayerField defenderField = PlayerField.builder().playerId(2L).build();
        ActivePokemon attacker = ActivePokemon.builder().cardId("attacker").build();
        ActivePokemon defender = ActivePokemon.builder().cardId("defender").build();
        Card attackerCard = card("attacker", "Froakie");
        Card defenderCard = card("defender", "Pikachu");
        AttackData attackData = new AttackData("Pound", List.of("Colorless"), 1, "20", "", null);

        AttackContext context = AttackContext.builder()
                .board(board)
                .attackerPlayerId(1L)
                .cardLookup(id -> attackerCard)
                .attackerField(attackerField)
                .defenderField(defenderField)
                .attackerPokemon(attacker)
                .defenderPokemon(defender)
                .attackerCard(attackerCard)
                .defenderCard(defenderCard)
                .attackData(attackData)
                .build();

        assertSame(board, context.getBoard());
        assertEquals(1L, context.getAttackerPlayerId());
        assertSame(attackerField, context.getAttackerField());
        assertSame(defenderField, context.getDefenderField());
        assertSame(attacker, context.getAttackerPokemon());
        assertSame(defender, context.getDefenderPokemon());
        assertSame(attackerCard, context.getAttackerCard());
        assertSame(defenderCard, context.getDefenderCard());
        assertSame(attackData, context.getAttackData());
        assertNotNull(context.getEvents());
        assertTrue(context.getEvents().isEmpty());
    }

    @Test
    void cancelAttackAndFlags_mutatePipelineState() {
        AttackContext context = new AttackContext();

        context.cancelAttack();
        context.setDamageBlocked(true);
        context.setDamageModifiers(20);
        context.setFinalDamage(70);

        assertTrue(context.isAttackCancelled());
        assertTrue(context.isDamageBlocked());
        assertEquals(20, context.getDamageModifiers());
        assertEquals(70, context.getFinalDamage());
    }

    @Test
    void addEvent_appendsToExistingEventList() {
        GameEvent first = GameEvent.of(GameEventType.ATTACK_DECLARED, "Attack used.");
        AttackContext context = AttackContext.builder()
                .events(new ArrayList<>(List.of(first)))
                .build();
        GameEvent second = GameEvent.of(GameEventType.DAMAGE_DEALT, "Damage dealt.");

        context.addEvent(second);

        assertEquals(List.of(first, second), context.getEvents());
    }

    private Card card(String id, String name) {
        Card card = new Card();
        card.setId(id);
        card.setName(name);
        return card;
    }
}
