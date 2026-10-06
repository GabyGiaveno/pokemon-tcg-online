package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatusEffectManagerTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ActivePokemon pokemon(int hp) {
        ActivePokemon p = new ActivePokemon();
        p.setCardId("test-card");
        p.setMaxHp(100);
        p.setCurrentHp(hp);
        p.setCondition(SpecialCondition.NONE);
        return p;
    }

    // ── applyBetweenTurnEffects ───────────────────────────────────────────────

    @Test
    void applyBetweenTurnEffects_nullPokemon_returnsEmptyList() {
        StatusEffectManager manager = new StatusEffectManager();
        assertTrue(manager.applyBetweenTurnEffects(null).isEmpty());
    }

    @Test
    void applyBetweenTurnEffects_noConditions_returnsEmptyList() {
        StatusEffectManager manager = new StatusEffectManager();
        ActivePokemon p = pokemon(80);

        assertTrue(manager.applyBetweenTurnEffects(p).isEmpty());
        assertEquals(80, p.getCurrentHp());
    }

    @Test
    void applyBetweenTurnEffects_poisoned_dealsTenDamage() {
        StatusEffectManager manager = new StatusEffectManager();
        ActivePokemon p = pokemon(50);
        p.setPoisoned(true);

        List<GameEvent> events = manager.applyBetweenTurnEffects(p);

        assertEquals(1, events.size());
        assertEquals(GameEventType.DAMAGE_DEALT, events.get(0).getType());
        assertEquals(40, p.getCurrentHp());
        assertTrue(p.isPoisoned()); // condition persists
    }

    @Test
    void applyBetweenTurnEffects_burnedHeads_curesBurn() {
        Random alwaysHeads = mock(Random.class);
        when(alwaysHeads.nextBoolean()).thenReturn(true);
        StatusEffectManager manager = new StatusEffectManager(alwaysHeads);

        ActivePokemon p = pokemon(80);
        p.setBurned(true);

        List<GameEvent> events = manager.applyBetweenTurnEffects(p);

        assertEquals(1, events.size());
        assertEquals(GameEventType.STATUS_EFFECT_CLEARED, events.get(0).getType());
        assertFalse(p.isBurned());
        assertEquals(80, p.getCurrentHp()); // no damage on heads
    }

    @Test
    void applyBetweenTurnEffects_burnedTails_dealsTwentyDamage() {
        Random alwaysTails = mock(Random.class);
        when(alwaysTails.nextBoolean()).thenReturn(false);
        StatusEffectManager manager = new StatusEffectManager(alwaysTails);

        ActivePokemon p = pokemon(80);
        p.setBurned(true);

        List<GameEvent> events = manager.applyBetweenTurnEffects(p);

        assertEquals(1, events.size());
        assertEquals(GameEventType.DAMAGE_DEALT, events.get(0).getType());
        assertTrue(p.isBurned()); // burn persists on tails
        assertEquals(60, p.getCurrentHp());
    }

    @Test
    void applyBetweenTurnEffects_asleepHeads_wakesUp() {
        Random alwaysHeads = mock(Random.class);
        when(alwaysHeads.nextBoolean()).thenReturn(true);
        StatusEffectManager manager = new StatusEffectManager(alwaysHeads);

        ActivePokemon p = pokemon(80);
        p.setCondition(SpecialCondition.ASLEEP);

        List<GameEvent> events = manager.applyBetweenTurnEffects(p);

        assertEquals(1, events.size());
        assertEquals(GameEventType.STATUS_EFFECT_CLEARED, events.get(0).getType());
        assertEquals(SpecialCondition.NONE, p.getCondition());
    }

    @Test
    void applyBetweenTurnEffects_asleepTails_staysAsleep() {
        Random alwaysTails = mock(Random.class);
        when(alwaysTails.nextBoolean()).thenReturn(false);
        StatusEffectManager manager = new StatusEffectManager(alwaysTails);

        ActivePokemon p = pokemon(80);
        p.setCondition(SpecialCondition.ASLEEP);

        List<GameEvent> events = manager.applyBetweenTurnEffects(p);

        assertEquals(1, events.size());
        assertEquals(GameEventType.STATUS_EFFECT_APPLIED, events.get(0).getType());
        assertEquals(SpecialCondition.ASLEEP, p.getCondition());
    }

    @Test
    void applyBetweenTurnEffects_paralyzed_curesAutomatically() {
        StatusEffectManager manager = new StatusEffectManager();
        ActivePokemon p = pokemon(80);
        p.setCondition(SpecialCondition.PARALYZED);

        List<GameEvent> events = manager.applyBetweenTurnEffects(p);

        assertEquals(1, events.size());
        assertEquals(GameEventType.STATUS_EFFECT_CLEARED, events.get(0).getType());
        assertEquals(SpecialCondition.NONE, p.getCondition());
    }

    @Test
    void applyBetweenTurnEffects_poisonedAndBurned_bothApply() {
        Random alwaysTails = mock(Random.class);
        when(alwaysTails.nextBoolean()).thenReturn(false);
        StatusEffectManager manager = new StatusEffectManager(alwaysTails);

        ActivePokemon p = pokemon(80);
        p.setPoisoned(true);
        p.setBurned(true);

        List<GameEvent> events = manager.applyBetweenTurnEffects(p);

        // 10 (poison) + 20 (burn tails) = 30 total damage
        assertEquals(2, events.size());
        assertEquals(50, p.getCurrentHp());
    }

    @Test
    void applyBetweenTurnEffects_hpCannotGoBelowZero() {
        StatusEffectManager manager = new StatusEffectManager();
        ActivePokemon p = pokemon(5);
        p.setPoisoned(true); // would deal 10, only 5 HP left

        manager.applyBetweenTurnEffects(p);

        assertEquals(0, p.getCurrentHp());
    }

    // ── applyCondition ────────────────────────────────────────────────────────

    @Test
    void applyCondition_asleepReplacesConfused() {
        StatusEffectManager manager = new StatusEffectManager();
        ActivePokemon p = pokemon(80);
        p.setCondition(SpecialCondition.CONFUSED);

        manager.applyCondition(p, SpecialCondition.ASLEEP);

        assertEquals(SpecialCondition.ASLEEP, p.getCondition());
    }

    @Test
    void applyCondition_burnedAndPoisonedAreIndependentOfCondition() {
        StatusEffectManager manager = new StatusEffectManager();
        ActivePokemon p = pokemon(80);
        p.setCondition(SpecialCondition.PARALYZED);

        manager.applyCondition(p, SpecialCondition.BURNED);
        manager.applyCondition(p, SpecialCondition.POISONED);

        assertEquals(SpecialCondition.PARALYZED, p.getCondition()); // exclusive unchanged
        assertTrue(p.isBurned());
        assertTrue(p.isPoisoned());
    }

    @Test
    void applyCondition_noneClearsExclusiveConditionOnly() {
        StatusEffectManager manager = new StatusEffectManager();
        ActivePokemon p = pokemon(80);
        p.setCondition(SpecialCondition.CONFUSED);
        p.setBurned(true);
        p.setPoisoned(true);

        manager.applyCondition(p, SpecialCondition.NONE);

        assertEquals(SpecialCondition.NONE, p.getCondition());
        assertTrue(p.isBurned());
        assertTrue(p.isPoisoned());
    }

    // ── clearAllConditions ────────────────────────────────────────────────────

    @Test
    void clearAllConditions_clearsEverything() {
        StatusEffectManager manager = new StatusEffectManager();
        ActivePokemon p = pokemon(80);
        p.setCondition(SpecialCondition.ASLEEP);
        p.setBurned(true);
        p.setPoisoned(true);

        manager.clearAllConditions(p);

        assertEquals(SpecialCondition.NONE, p.getCondition());
        assertFalse(p.isBurned());
        assertFalse(p.isPoisoned());
    }
}
