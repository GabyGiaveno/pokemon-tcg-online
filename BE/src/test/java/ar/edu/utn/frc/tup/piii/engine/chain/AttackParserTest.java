package ar.edu.utn.frc.tup.piii.engine.chain;

import ar.edu.utn.frc.tup.piii.models.cards.effects.AddDamageEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.ApplyConditionEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.CoinFlipEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.HealEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.UnknownEffect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttackParserTest {

    @Test
    void testParseAndMergeEffects() {
        // 1. Simulamos el JSON crudo que viene de la API para los ataques de la carta
        //    Incluye damage para verificar getBaseDamage()
        String attacksJson = """
                [
                  {
                    "name": "Poison Powder",
                    "damage": "20",
                    "text": "Your opponent's Active Pokémon is now Poisoned."
                  },
                  {
                    "name": "Jungle Hammer",
                    "damage": "40",
                    "text": "Heal 30 damage from this Pokémon."
                  }
                ]
                """;

        // 2. Simulamos el JSON que guardamos nosotros en parsedEffects desde xy1_parsed.json
        String parsedEffectsJson = """
                {
                  "attacks": [
                    {
                      "name": "Poison Powder",
                      "parsedEffects": [
                        {
                          "type": "APPLY_CONDITION",
                          "condition": "POISONED",
                          "target": "DEFENDER"
                        }
                      ]
                    },
                    {
                      "name": "Jungle Hammer",
                      "parsedEffects": [
                        {
                          "type": "HEAL",
                          "amount": 30,
                          "target": "SELF"
                        }
                      ]
                    }
                  ]
                }
                """;

        // 3. Llamamos al parser que se usa en tiempo de ejecución (AttackResolutionChain)
        List<AttackData> attacks = AttackParser.parse(attacksJson, parsedEffectsJson);

        // 4. Verificaciones generales
        assertNotNull(attacks);
        assertEquals(2, attacks.size());

        // ── Primer ataque: Poison Powder ───────────────────────────────────
        AttackData poisonPowder = attacks.get(0);
        assertEquals("Poison Powder", poisonPowder.getName());
        assertEquals(20, poisonPowder.getBaseDamage(), "El daño base debe parsearse del campo damage");
        assertNotNull(poisonPowder.getParsedEffects());
        assertEquals(1, poisonPowder.getParsedEffects().size());

        AttackEffect rawPoisonEffect = poisonPowder.getParsedEffects().get(0);
        assertNotNull(rawPoisonEffect);
        assertTrue(rawPoisonEffect instanceof ApplyConditionEffect,
                "El effect type APPLY_CONDITION debe deserializarse como ApplyConditionEffect");

        ApplyConditionEffect poisonEffect = (ApplyConditionEffect) rawPoisonEffect;
        assertEquals("POISONED", poisonEffect.getCondition(),
                "Debe preservar la condición del parsed effect");
        assertEquals("DEFENDER", poisonEffect.getTarget(),
                "Debe preservar el target del parsed effect");

        // ── Segundo ataque: Jungle Hammer ──────────────────────────────────
        AttackData jungleHammer = attacks.get(1);
        assertEquals("Jungle Hammer", jungleHammer.getName());
        assertEquals(40, jungleHammer.getBaseDamage(), "El daño base debe parsearse del campo damage");
        assertNotNull(jungleHammer.getParsedEffects());
        assertEquals(1, jungleHammer.getParsedEffects().size());

        AttackEffect rawHealEffect = jungleHammer.getParsedEffects().get(0);
        assertNotNull(rawHealEffect);
        assertTrue(rawHealEffect instanceof HealEffect,
                "El effect type HEAL debe deserializarse como HealEffect");

        HealEffect healEffect = (HealEffect) rawHealEffect;
        assertEquals(30, healEffect.getAmount(),
                "Debe preservar el amount del parsed effect");
        assertEquals("SELF", healEffect.getTarget(),
                "Debe preservar el target del parsed effect");

        System.out.println("Test de mergeo en tiempo de ejecución (AttackParser) superado.");
    }
    
    @Test
    void testParseWithoutParsedEffects() {
        String attacksJson = """
                [
                  {
                    "name": "Tackle",
                    "damage": "10",
                    "text": ""
                  }
                ]
                """;

        List<AttackData> attacks = AttackParser.parse(attacksJson, null);

        assertNotNull(attacks);
        assertEquals(1, attacks.size());
        assertEquals("Tackle", attacks.get(0).getName());
        assertEquals(10, attacks.get(0).getBaseDamage());
        assertTrue(attacks.get(0).getParsedEffects() == null || attacks.get(0).getParsedEffects().isEmpty(),
                "Sin parsedEffects, la lista debe estar vacía o ser null");
    }

    // ── Bloque 1: parser resiliente + vocabulario (spec effect-parsing) ──────────

    /** REQ-1.1 — un type desconocido NO descarta los efectos válidos de la carta. */
    @Test
    void unknownTopLevelTypeKeepsValidEffects() {
        String attacksJson = """
                [ { "name": "Mix", "damage": "30", "text": "x" } ]
                """;
        String parsedEffectsJson = """
                {
                  "attacks": [
                    {
                      "name": "Mix",
                      "parsedEffects": [
                        { "type": "ADD_DAMAGE", "amount": 20 },
                        { "type": "MOVE_ENERGY", "amount": 1, "source": "SELF", "destination": "SELF_BENCH" }
                      ]
                    }
                  ]
                }
                """;

        List<AttackData> attacks = AttackParser.parse(attacksJson, parsedEffectsJson);

        assertEquals(1, attacks.size());
        List<AttackEffect> effects = attacks.get(0).getParsedEffects();
        assertEquals(2, effects.size(), "Debe conservar el válido + el desconocido (inerte)");
        assertTrue(effects.stream().anyMatch(e -> e instanceof AddDamageEffect),
                "El ADD_DAMAGE válido debe sobrevivir junto a un type desconocido");
        assertTrue(effects.stream().anyMatch(e -> e instanceof UnknownEffect),
                "El type desconocido debe caer en UnknownEffect, no romper la carta");
    }

    /** REQ-1.2 — cada uno de los 8 tipos hoy huérfanos no rompe el parseo. */
    @ParameterizedTest
    @ValueSource(strings = {
            "MOVE_ENERGY", "DRAW_CARD", "REMOVE_CONDITIONS", "RECYCLE",
            "DISCARD_FROM_DECK", "DISCARD_TOOL", "CHOOSE_RANDOM_FROM_HAND", "ATTACH_ENERGY"
    })
    void orphanTypesDoNotBreakParsing(String orphanType) {
        String attacksJson = """
                [ { "name": "A", "damage": "10", "text": "x" } ]
                """;
        String parsedEffectsJson = """
                {
                  "attacks": [
                    {
                      "name": "A",
                      "parsedEffects": [
                        { "type": "HEAL", "amount": 10, "target": "SELF" },
                        { "type": "%s" }
                      ]
                    }
                  ]
                }
                """.formatted(orphanType);

        List<AttackData> attacks = AttackParser.parse(attacksJson, parsedEffectsJson);

        assertEquals(1, attacks.size());
        List<AttackEffect> effects = attacks.get(0).getParsedEffects();
        assertEquals(2, effects.size());
        assertTrue(effects.stream().anyMatch(e -> e instanceof HealEffect),
                "El efecto válido HEAL debe conservarse junto a " + orphanType);
    }

    /** REQ-2 — una habilidad con campo 'conditions' no modelado no debe lanzar. */
    @Test
    void passiveAbilityWithUnknownConditionsFieldDoesNotThrow() {
        String attacksJson = """
                [ { "name": "Static", "damage": "", "text": "x" } ]
                """;
        String parsedEffectsJson = """
                {
                  "attacks": [
                    {
                      "name": "Static",
                      "parsedEffects": [
                        { "type": "PASSIVE_ABILITY", "trigger": "ON_PLAY",
                          "conditions": [ { "type": "IS_ACTIVE" } ], "stackable": false }
                      ]
                    }
                  ]
                }
                """;

        List<AttackData> attacks = AttackParser.parse(attacksJson, parsedEffectsJson);

        assertEquals(1, attacks.size());
        assertEquals(1, attacks.get(0).getParsedEffects().size(),
                "El PASSIVE_ABILITY debe deserializar pese al campo extra 'conditions'");
    }

    /** REQ-3 — un sub-efecto desconocido dentro de COIN_FLIP no rompe el contenedor. */
    @Test
    void unknownNestedEffectInsideCoinFlipDoesNotBreakContainer() {
        String attacksJson = """
                [ { "name": "Gamble", "damage": "0", "text": "x" } ]
                """;
        String parsedEffectsJson = """
                {
                  "attacks": [
                    {
                      "name": "Gamble",
                      "parsedEffects": [
                        { "type": "COIN_FLIP", "ifHeads": [ { "type": "DESCONOCIDO_X" } ], "ifTails": [] }
                      ]
                    }
                  ]
                }
                """;

        List<AttackData> attacks = AttackParser.parse(attacksJson, parsedEffectsJson);

        assertEquals(1, attacks.size());
        AttackEffect e = attacks.get(0).getParsedEffects().get(0);
        assertTrue(e instanceof CoinFlipEffect, "El COIN_FLIP debe crearse igual");
        CoinFlipEffect cf = (CoinFlipEffect) e;
        assertEquals(1, cf.getIfHeads().size());
        assertTrue(cf.getIfHeads().get(0) instanceof UnknownEffect,
                "El sub-efecto desconocido debe caer en UnknownEffect");
    }
}
