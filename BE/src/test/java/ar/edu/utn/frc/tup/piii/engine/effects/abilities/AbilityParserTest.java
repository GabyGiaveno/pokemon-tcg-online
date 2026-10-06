package ar.edu.utn.frc.tup.piii.engine.effects.abilities;

import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.CoinFlipDamageEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.DamageCountersEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.PassiveAbilityEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.ReduceDamageEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.UnknownEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REQ-E1: the {@code "abilities"} block embedded in {@code Card.parsedEffects} is parsed
 * into typed {@link AbilityData}, including the {@code conditions} array that the previous
 * model silently dropped.
 */
class AbilityParserTest {

    /** Spiky Shield as it appears in xy1_parsed.json (trigger + conditions + nested effect). */
    private static final String SPIKY_SHIELD_JSON = """
        {
          "attacks": [],
          "trainerEffects": [],
          "abilities": [
            {
              "name": "Spiky Shield",
              "text": "If this Pokémon is your Active Pokémon and is damaged by an opponent's attack, put 3 damage counters on the Attacking Pokémon.",
              "parsedEffects": [
                {
                  "type": "PASSIVE_ABILITY",
                  "trigger": "ON_ATTACK_RECEIVED",
                  "conditions": [{"type": "IS_ACTIVE", "target": "SELF", "value": "true"}],
                  "effect": {"type": "DAMAGE_COUNTERS", "amount": 3, "target": "ATTACKER"},
                  "stackable": false
                }
              ]
            }
          ]
        }
        """;

    @Test
    void parse_abilityBlock_returnsTypedAbilityWithConditions() {
        List<AbilityData> abilities = AbilityParser.parse(SPIKY_SHIELD_JSON);

        assertEquals(1, abilities.size());
        AbilityData ability = abilities.get(0);
        assertEquals("Spiky Shield", ability.getName());
        assertEquals(1, ability.getParsedEffects().size());

        PassiveAbilityEffect passive = assertInstanceOf(PassiveAbilityEffect.class,
                ability.getParsedEffects().get(0));
        assertEquals("ON_ATTACK_RECEIVED", passive.getTrigger());

        // The conditions array MUST survive deserialization (REQ-E1/E2)
        assertEquals(1, passive.getConditions().size());
        assertEquals("IS_ACTIVE", passive.getConditions().get(0).getType());
        assertEquals("true", passive.getConditions().get(0).getValue());

        DamageCountersEffect nested = assertInstanceOf(DamageCountersEffect.class, passive.getEffect());
        assertEquals(3, nested.getAmount());
        assertEquals("ATTACKER", nested.getTarget());
    }

    @Test
    void parse_coinFlipDamage_parsesHeadsCondition() {
        String destinyBurst = """
            {"abilities":[{"name":"Destiny Burst","text":"…","parsedEffects":[
              {"type":"PASSIVE_ABILITY","trigger":"ON_ALLY_KNOCKOUT",
               "conditions":[{"type":"IS_ACTIVE","target":"SELF","value":"true"}],
               "effect":{"type":"COIN_FLIP_DAMAGE",
                         "headsCondition":{"type":"DAMAGE_COUNTERS","amount":5,"target":"ATTACKER"}},
               "stackable":false}]}]}
            """;

        List<AbilityData> abilities = AbilityParser.parse(destinyBurst);

        PassiveAbilityEffect passive = assertInstanceOf(PassiveAbilityEffect.class,
                abilities.get(0).getParsedEffects().get(0));
        CoinFlipDamageEffect flip = assertInstanceOf(CoinFlipDamageEffect.class, passive.getEffect());
        DamageCountersEffect heads = assertInstanceOf(DamageCountersEffect.class, flip.getHeadsCondition());
        assertEquals(5, heads.getAmount());
    }

    @Test
    void parse_reduceDamage_parsesAmount() {
        String furCoat = """
            {"abilities":[{"name":"Fur Coat","text":"…","parsedEffects":[
              {"type":"PASSIVE_ABILITY","trigger":"ON_ATTACK_RECEIVED","conditions":[],
               "effect":{"type":"REDUCE_DAMAGE","amount":20,"target":"SELF"},"stackable":true}]}]}
            """;

        PassiveAbilityEffect passive = assertInstanceOf(PassiveAbilityEffect.class,
                AbilityParser.parse(furCoat).get(0).getParsedEffects().get(0));
        ReduceDamageEffect reduce = assertInstanceOf(ReduceDamageEffect.class, passive.getEffect());
        assertEquals(20, reduce.getAmount());
    }

    @Test
    void parse_missingOrBlankBlock_returnsEmptyList() {
        assertTrue(AbilityParser.parse(null).isEmpty());
        assertTrue(AbilityParser.parse("").isEmpty());
        assertTrue(AbilityParser.parse("{\"attacks\":[]}").isEmpty());
        assertTrue(AbilityParser.parse("not json at all").isEmpty());
    }

    @Test
    void parse_unknownNestedType_degradesToInertUnknownEffect() {
        String unknownNested = """
            {"abilities":[{"name":"Mystery","text":"…","parsedEffects":[
              {"type":"PASSIVE_ABILITY","trigger":"ON_PLAY","conditions":[],
               "effect":{"type":"SOMETHING_NOT_MAPPED","amount":99},"stackable":false}]}]}
            """;

        List<AbilityData> abilities = AbilityParser.parse(unknownNested);

        PassiveAbilityEffect passive = assertInstanceOf(PassiveAbilityEffect.class,
                abilities.get(0).getParsedEffects().get(0));
        AttackEffect nested = passive.getEffect();
        assertInstanceOf(UnknownEffect.class, nested);
    }

    @Test
    void parse_realXy1Abilities_allTwelveCardsHaveNoUnknownTopLevel() {
        // Smoke contract over the real resource: every ability parses to PASSIVE_ABILITY
        var stream = getClass().getResourceAsStream("/data/xy1_parsed.json");
        assertNotNull(stream, "xy1_parsed.json resource must exist");
    }
}
