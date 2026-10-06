package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.models.cards.effects.ApplyConditionEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.HealEffect;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttackEffectParsingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testPolymorphicDeserialization() throws Exception {
        String json = "[\n" +
                "  {\n" +
                "    \"name\": \"Poison Powder\",\n" +
                "    \"text\": \"Your opponent's Active Pokémon is now Poisoned.\",\n" +
                "    \"parsedEffects\": [\n" +
                "      {\n" +
                "        \"type\": \"APPLY_CONDITION\",\n" +
                "        \"condition\": \"POISONED\",\n" +
                "        \"target\": \"DEFENDER\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"name\": \"Jungle Hammer\",\n" +
                "    \"text\": \"Heal 30 damage from this Pokémon.\",\n" +
                "    \"parsedEffects\": [\n" +
                "      {\n" +
                "        \"type\": \"HEAL\",\n" +
                "        \"amount\": 30,\n" +
                "        \"target\": \"SELF\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";

        List<AttackData> attacks = mapper.readValue(json, new TypeReference<>() {});

        assertEquals(2, attacks.size());

        // First attack
        AttackData attack1 = attacks.get(0);
        assertEquals("Poison Powder", attack1.getName());
        assertTrue(attack1.hasParsedEffects());
        AttackEffect effect1 = attack1.getParsedEffects().get(0);
        assertTrue(effect1 instanceof ApplyConditionEffect);
        ApplyConditionEffect applyCond = (ApplyConditionEffect) effect1;
        assertEquals("POISONED", applyCond.getCondition());
        assertEquals("DEFENDER", applyCond.getTarget());

        // Second attack
        AttackData attack2 = attacks.get(1);
        assertEquals("Jungle Hammer", attack2.getName());
        assertTrue(attack2.hasParsedEffects());
        AttackEffect effect2 = attack2.getParsedEffects().get(0);
        assertTrue(effect2 instanceof HealEffect);
        HealEffect heal = (HealEffect) effect2;
        assertEquals(30, heal.getAmount());
        assertEquals("SELF", heal.getTarget());
    }
}
