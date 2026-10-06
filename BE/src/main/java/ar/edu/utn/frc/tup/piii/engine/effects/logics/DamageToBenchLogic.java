package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.KnockoutProcessor;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.DamageToBenchEffect;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DamageToBenchLogic implements EffectLogic<DamageToBenchEffect> {

    private final KnockoutProcessor knockoutProcessor;

    public DamageToBenchLogic() {
        this.knockoutProcessor = new KnockoutProcessor();
    }

    DamageToBenchLogic(KnockoutProcessor knockoutProcessor) {
        this.knockoutProcessor = knockoutProcessor;
    }

    @Override
    public boolean isPostDamage() {
        return true;
    }

    @Override
    public void execute(DamageToBenchEffect effect, AttackContext ctx) {
        boolean hitsOpponent = !"SELF_BENCH".equalsIgnoreCase(effect.getTarget());
        PlayerField targetField = hitsOpponent ? ctx.getDefenderField() : ctx.getAttackerField();
        PlayerField prizeTakerField = hitsOpponent ? ctx.getAttackerField() : ctx.getDefenderField();

        List<BenchPokemon> bench = targetField.getBench();
        if (bench == null || bench.isEmpty()) return;

        // -1 means all slots; otherwise hit the first N slots.
        int count = effect.getTargetCount() == -1
                ? bench.size()
                : Math.min(effect.getTargetCount(), bench.size());

        List<Integer> targets = new ArrayList<>();
        for (int i = 0; i < count; i++) targets.add(i);

        // Apply damage — no weakness/resistance on bench hits per TCG rules.
        for (int i : targets) {
            BenchPokemon mon = bench.get(i);
            int newHp = Math.max(0, mon.getCurrentHp() - effect.getAmount());
            mon.setCurrentHp(newHp);
            ctx.addEvent(GameEvent.of(
                    GameEventType.DAMAGE_DEALT,
                    effect.getAmount() + " damage dealt to benched " + mon.getCardId()
                            + ". HP remaining: " + newHp + ".",
                    Map.of("defender", mon.getCardId(),
                            "finalDamage", effect.getAmount(),
                            "hpRemaining", newHp)));
        }

        // KO check — reverse iteration so slot removals don't shift unprocessed indices.
        for (int i = count - 1; i >= 0; i--) {
            List<GameEvent> koEvents = knockoutProcessor.processIfKnockedOutOnBench(
                    i, targetField, prizeTakerField, ctx.getBoard(), ctx.getCardLookup());
            koEvents.forEach(ctx::addEvent);
        }
    }
}
