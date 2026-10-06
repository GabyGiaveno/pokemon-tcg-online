package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.DamageCountersEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.Map;

/**
 * Places damage counters (1 counter = 10 HP) on a fixed set of targets.
 *
 * <p>Block 2 supports only the non-selection targets: {@code DEFENDER}, {@code ATTACKER}/{@code SELF},
 * {@code ALL_OPPONENT}, {@code BOTH_ACTIVE}. Targets that require the player to pick a specific
 * Pokémon (e.g. "1 of your opponent's Pokémon") are deferred to the selection mechanism (Block 3).
 * {@code amount <= 0} (e.g. "until remaining HP is 10") is also out of scope here.
 */
public class DamageCountersLogic implements EffectLogic<DamageCountersEffect> {

    @Override
    public void execute(DamageCountersEffect effectData, AttackContext ctx) {
        int counters = effectData.getAmount();
        if (counters <= 0) return; // special / not in Block 2 scope
        int damage = counters * 10;

        String target = effectData.getTarget() == null ? "" : effectData.getTarget().toUpperCase();
        switch (target) {
            case "DEFENDER" -> hitActive(ctx.getDefenderPokemon(), damage, ctx);
            case "ATTACKER", "SELF" -> hitActive(ctx.getAttackerPokemon(), damage, ctx);
            case "ALL_OPPONENT" -> {
                hitActive(ctx.getDefenderPokemon(), damage, ctx);
                hitBench(ctx.getDefenderField(), damage, ctx);
            }
            case "BOTH_ACTIVE" -> {
                hitActive(ctx.getAttackerPokemon(), damage, ctx);
                hitActive(ctx.getDefenderPokemon(), damage, ctx);
            }
            default -> { /* requires target selection → Block 3 */ }
        }
    }

    private void hitActive(ActivePokemon p, int damage, AttackContext ctx) {
        if (p == null) return;
        p.setCurrentHp(Math.max(0, p.getCurrentHp() - damage));
        ctx.addEvent(GameEvent.of(GameEventType.DAMAGE_DEALT,
                damage + " damage placed on " + p.getCardId() + ".",
                Map.of("cardId", p.getCardId(), "damage", damage, "source", "DAMAGE_COUNTERS")));
    }

    private void hitBench(PlayerField field, int damage, AttackContext ctx) {
        if (field == null || field.getBench() == null) return;
        for (BenchPokemon b : field.getBench()) {
            b.setCurrentHp(Math.max(0, b.getCurrentHp() - damage));
            ctx.addEvent(GameEvent.of(GameEventType.DAMAGE_DEALT,
                    damage + " damage placed on benched " + b.getCardId() + ".",
                    Map.of("cardId", b.getCardId(), "damage", damage, "source", "DAMAGE_COUNTERS")));
        }
    }

    @Override
    public boolean isPostDamage() {
        return true;
    }
}
