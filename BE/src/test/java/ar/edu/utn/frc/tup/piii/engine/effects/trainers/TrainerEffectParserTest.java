package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.DrawCardsTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.UnknownTrainerEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerEffectParserTest {

    @Test
    void parsesKnownTrainerEffectFromParsedEffects() {
        String parsedEffects = """
                { "attacks": [], "abilities": [],
                  "trainerEffects": [ { "type": "DRAW_CARDS", "amount": 2 } ] }
                """;

        List<TrainerEffect> effects = TrainerEffectParser.parse(parsedEffects);

        assertEquals(1, effects.size());
        assertTrue(effects.get(0) instanceof DrawCardsTrainerEffect);
        assertEquals(2, ((DrawCardsTrainerEffect) effects.get(0)).getAmount());
    }

    /** REQ-C1: un tipo desconocido cae en UnknownTrainerEffect y no descarta los válidos. */
    @Test
    void unknownTypeDegradesToUnknownTrainerEffect() {
        String parsedEffects = """
                { "trainerEffects": [
                    { "type": "DRAW_CARDS", "amount": 1 },
                    { "type": "FAKE_UNSUPPORTED_TYPE", "foo": "bar" }
                ] }
                """;

        List<TrainerEffect> effects = TrainerEffectParser.parse(parsedEffects);

        assertEquals(2, effects.size());
        assertTrue(effects.stream().anyMatch(e -> e instanceof DrawCardsTrainerEffect));
        assertTrue(effects.stream().anyMatch(e -> e instanceof UnknownTrainerEffect));
    }

    @Test
    void noTrainerEffectsBlock_returnsEmpty() {
        assertTrue(TrainerEffectParser.parse("{ \"attacks\": [], \"abilities\": [] }").isEmpty());
    }

    @Test
    void nullOrBlank_returnsEmpty() {
        assertTrue(TrainerEffectParser.parse(null).isEmpty());
        assertTrue(TrainerEffectParser.parse("   ").isEmpty());
    }
}
