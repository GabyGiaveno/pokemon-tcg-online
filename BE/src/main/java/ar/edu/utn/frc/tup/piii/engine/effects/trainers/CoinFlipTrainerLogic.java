package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.CoinFlipTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Roller Skates: flip a coin; run {@code ifHeads} or {@code ifTails} sub-effects via the registry.
 * {@link Random} is injectable for deterministic tests.
 */
public class CoinFlipTrainerLogic implements TrainerEffectLogic<CoinFlipTrainerEffect> {

    private final Random random;

    public CoinFlipTrainerLogic() {
        this.random = new Random();
    }

    public CoinFlipTrainerLogic(Random random) {
        this.random = random;
    }

    @Override
    public List<GameEvent> execute(CoinFlipTrainerEffect effectData, TrainerContext ctx) {
        List<GameEvent> events = new ArrayList<>();
        boolean heads = random.nextBoolean();
        events.add(GameEvent.of(GameEventType.COIN_FLIPPED,
                heads ? "Heads!" : "Tails!",
                Map.of("flip", heads ? "HEADS" : "TAILS")));

        List<TrainerEffect> branch = heads ? effectData.getIfHeads() : effectData.getIfTails();
        if (branch != null) {
            TrainerEffectRegistry registry = TrainerEffectRegistry.getInstance();
            for (TrainerEffect sub : branch) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                TrainerEffectLogic logic = registry.getLogic(sub.getClass());
                if (logic != null) {
                    events.addAll(logic.execute(sub, ctx));
                }
            }
        }
        return events;
    }
}
