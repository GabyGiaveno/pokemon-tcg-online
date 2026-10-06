package ar.edu.utn.frc.tup.piii.engine.chain;

import ar.edu.utn.frc.tup.piii.entities.Card;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageCalculatorTest {

    // ── Helper builders ───────────────────────────────────────────────────────

    private Card cardWithType(String type) {
        return Card.builder().types(List.of(type)).build();
    }

    private Card cardWithWeakness(String weakType) {
        return Card.builder()
                .weaknesses("[{\"type\":\"" + weakType + "\",\"value\":\"×2\"}]")
                .build();
    }

    private Card cardWithResistance(String resistType) {
        return Card.builder()
                .resistances("[{\"type\":\"" + resistType + "\",\"value\":\"-20\"}]")
                .build();
    }

    private Card emptyCard() {
        return Card.builder().build();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void calculate_zeroDamage_returnsZero() {
        assertEquals(0, DamageCalculator.calculate(0, cardWithType("Fire"), emptyCard()));
    }

    @Test
    void calculate_noModifiers_returnsBaseDamageRoundedToTen() {
        assertEquals(30, DamageCalculator.calculate(30, cardWithType("Fire"), emptyCard()));
    }

    @Test
    void calculate_withMatchingWeakness_doublesDamage() {
        Card attacker = cardWithType("Fire");
        Card defender = cardWithWeakness("Fire");

        assertEquals(60, DamageCalculator.calculate(30, attacker, defender));
    }

    @Test
    void calculate_withNonMatchingWeakness_doesNotDouble() {
        Card attacker = cardWithType("Water");
        Card defender = cardWithWeakness("Fire");

        assertEquals(30, DamageCalculator.calculate(30, attacker, defender));
    }

    @Test
    void calculate_withMatchingResistance_subtractsTwenty() {
        Card attacker = cardWithType("Psychic");
        Card defender = cardWithResistance("Psychic");

        assertEquals(60, DamageCalculator.calculate(80, attacker, defender));
    }

    @Test
    void calculate_resistanceCannotMakeDamageNegative_returnsZero() {
        Card attacker = cardWithType("Psychic");
        Card defender = cardWithResistance("Psychic");

        assertEquals(0, DamageCalculator.calculate(10, attacker, defender));
    }

    @Test
    void calculate_weaknessThenResistance_appliesInOrder() {
        // 30 * 2 (weakness) = 60, then 60 - 20 (resistance) = 40
        Card attacker = cardWithType("Fire");
        Card defender = Card.builder()
                .weaknesses("[{\"type\":\"Fire\",\"value\":\"×2\"}]")
                .resistances("[{\"type\":\"Fire\",\"value\":\"-20\"}]")
                .build();

        assertEquals(40, DamageCalculator.calculate(30, attacker, defender));
    }

    @Test
    void calculate_roundsToNearestTen() {
        // 35 has no modifiers — rounds to 40
        assertEquals(40, DamageCalculator.calculate(35, cardWithType("Fire"), emptyCard()));
    }

    @Test
    void calculate_attackerWithNullTypes_treatedAsNoType() {
        Card attacker = emptyCard(); // null types
        Card defender = cardWithWeakness("Fire");

        // No matching attacker type → no weakness applied
        assertEquals(30, DamageCalculator.calculate(30, attacker, defender));
    }

    @Test
    void calculate_nullAttackerCard_treatedAsNoType() {
        assertEquals(30, DamageCalculator.calculate(30, null, emptyCard()));
    }

    @Test
    void calculate_invalidWeaknessJson_ignoresAndReturnBase() {
        Card defender = Card.builder().weaknesses("NOT_JSON").build();
        Card attacker = cardWithType("Fire");

        assertEquals(30, DamageCalculator.calculate(30, attacker, defender));
    }

    @Test
    void calculate_plusFormatWeakness_addsValue() {
        // Some older sets use "+30" format instead of "×2"
        Card attacker = cardWithType("Fire");
        Card defender = Card.builder()
                .weaknesses("[{\"type\":\"Fire\",\"value\":\"+30\"}]")
                .build();

        assertEquals(60, DamageCalculator.calculate(30, attacker, defender));
    }

    @Test
    void calculate_asciiXWeakness_multipliesByParsedAmount() {
        Card attacker = cardWithType("Fire");
        Card defender = Card.builder()
                .weaknesses("[{\"type\":\"Fire\",\"value\":\"x3\"}]")
                .build();

        assertEquals(90, DamageCalculator.calculate(30, attacker, defender));
    }

    @Test
    void calculate_suppressWeakness_doesNotDoubleDamage() {
        Card attacker = cardWithType("Fire");
        Card defender = cardWithWeakness("Fire");

        assertEquals(30, DamageCalculator.calculate(30, attacker, defender, true));
    }

    @Test
    void calculate_unknownWeaknessFormat_defaultsToDoubleDamage() {
        Card attacker = cardWithType("Fire");
        Card defender = Card.builder()
                .weaknesses("[{\"type\":\"Fire\",\"value\":\"double\"}]")
                .build();

        assertEquals(60, DamageCalculator.calculate(30, attacker, defender));
    }

    @Test
    void calculate_withNonMatchingResistance_doesNotSubtract() {
        Card attacker = cardWithType("Fire");
        Card defender = cardWithResistance("Psychic");

        assertEquals(80, DamageCalculator.calculate(80, attacker, defender));
    }

    @Test
    void calculate_matchingResistanceWithNonMinusFormat_isIgnored() {
        Card attacker = cardWithType("Fire");
        Card defender = Card.builder()
                .resistances("[{\"type\":\"Fire\",\"value\":\"+20\"}]")
                .build();

        assertEquals(80, DamageCalculator.calculate(80, attacker, defender));
    }

    @Test
    void calculate_attackerWithBlankType_treatedAsNoType() {
        Card attacker = cardWithType(" ");
        Card defender = Card.builder()
                .weaknesses("[{\"type\":\"Fire\",\"value\":\"×2\"}]")
                .resistances("[{\"type\":\"Fire\",\"value\":\"-20\"}]")
                .build();

        assertEquals(30, DamageCalculator.calculate(30, attacker, defender));
    }

    @Test
    void calculate_blankModifierStrings_areIgnored() {
        Card attacker = cardWithType("Fire");
        Card defender = Card.builder()
                .weaknesses(" ")
                .resistances(" ")
                .build();

        assertEquals(30, DamageCalculator.calculate(30, attacker, defender));
    }
}
