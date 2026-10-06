package ar.edu.utn.frc.tup.piii.engine.chain.handlers;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackHandler;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;

import java.util.Map;

/**
 * Handler 3 — Target selection.
 *
 * <p>Resolves which Pokémon the attack will hit. In the standard case the target is always
 * the opponent's Active Pokémon, which is already pre-populated in {@link AttackContext}
 * by {@link ar.edu.utn.frc.tup.piii.engine.chain.AttackResolutionChain#buildContext}.
 *
 * <p>This handler emits the {@link GameEventType#ATTACK_DECLARED} event that announces
 * the attack name — the "flavour" event visible to both players.
 *
 * <p><b>TODO (bench-targeting attacks):</b> Some attacks hit a specific Bench Pokémon
 * (e.g., "Snipe Shot", "Rainstorm"). When those are implemented, this handler will read
 * {@code ActionRequest.targetPosition} and update {@code ctx.defenderPokemon} accordingly.
 * An {@code AttackEffectRegistry} keyed on attack name will be needed to know which attacks
 * require bench targeting input.
 */
public class SelectionsHandler implements AttackHandler {

    @Override
    public void handle(AttackContext ctx) {
        // Default: target is already set to the opponent's Active Pokémon.
        // No selection logic required for standard attacks.
        ctx.addEvent(GameEvent.of(
                GameEventType.ATTACK_DECLARED,
                ctx.getAttackerPokemon().getCardId()
                        + " uses " + ctx.getAttackData().getName()
                        + " against " + ctx.getDefenderPokemon().getCardId() + "!",
                Map.of("attacker", ctx.getAttackerPokemon().getCardId(),
                        "attack", ctx.getAttackData().getName(),
                        "defender", ctx.getDefenderPokemon().getCardId())));
    }
}
