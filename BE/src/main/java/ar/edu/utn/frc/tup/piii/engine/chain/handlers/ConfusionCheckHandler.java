package ar.edu.utn.frc.tup.piii.engine.chain.handlers;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackHandler;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;

import java.util.Map;
import java.util.Random;

/**
 * Handler 2 — Confusion check.
 *
 * <p>If the attacking Pokémon is {@link SpecialCondition#CONFUSED}, a coin flip is performed
 * before the attack resolves:
 * <ul>
 *   <li><b>Heads</b> — the Pokémon overcomes confusion and the attack proceeds normally.</li>
 *   <li><b>Tails</b> — the attack fails; the Pokémon deals 30 damage to <em>itself</em>
 *       and {@link AttackContext#cancelAttack()} is called to cancel the remaining pipeline.</li>
 * </ul>
 *
 * <p>Note: when tails, {@link PostDamageHandler} still runs (it overrides {@code alwaysRun()})
 * so that a potential self-KO from the 30 self-damage is detected and processed correctly.
 *
 * <p>A {@link Random} instance is accepted via the constructor to make this handler fully
 * deterministic in unit tests (pass {@code new Random(seed)} or a mock).
 */
public class ConfusionCheckHandler implements AttackHandler {

    private static final int CONFUSION_SELF_DAMAGE = 30;

    private final Random random;

    public ConfusionCheckHandler(Random random) {
        this.random = random;
    }

    @Override
    public void handle(AttackContext ctx) {
        if (ctx.getAttackerPokemon().getCondition() != SpecialCondition.CONFUSED) return;

        boolean heads = random.nextBoolean(); // true = heads, false = tails

        if (heads) {
            ctx.addEvent(GameEvent.of(
                    GameEventType.ATTACK_DECLARED,
                    ctx.getAttackerPokemon().getCardId()
                            + " is Confused but overcame it! (Heads) Attack proceeds.",
                    Map.of("flip", "HEADS",
                            "confused", true,
                            "attacker", ctx.getAttackerPokemon().getCardId())));
        } else {
            // Tails: self-damage + cancel
            int newHp = Math.max(0, ctx.getAttackerPokemon().getCurrentHp() - CONFUSION_SELF_DAMAGE);
            ctx.getAttackerPokemon().setCurrentHp(newHp);

            ctx.addEvent(GameEvent.of(
                    GameEventType.DAMAGE_DEALT,
                    ctx.getAttackerPokemon().getCardId()
                            + " hurt itself in confusion! (Tails) 30 damage. HP: " + newHp,
                    Map.of("flip", "TAILS",
                            "selfDamage", CONFUSION_SELF_DAMAGE,
                            "cardId", ctx.getAttackerPokemon().getCardId(),
                            "hpRemaining", newHp)));

            ctx.cancelAttack();
        }
    }
}
