package ar.edu.utn.frc.tup.piii.engine.chain.handlers;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackHandler;
import ar.edu.utn.frc.tup.piii.engine.factories.CardTypeResolver;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.CardType;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;

/**
 * Handler 5 — Flat damage modifiers from Pokémon Tools.
 *
 * <p>Implemented for XY1: Muscle Band adds +20 damage to Pokémon-EX and Mega Pokémon.
 * All other tools are ignored (not yet in the registry).
 *
 * <p>Modifiers accumulated here are applied in {@link DamageApplicationHandler} before
 * Weakness/Resistance.
 */
public class ModifierHandler implements AttackHandler {

    private static final String MUSCLE_BAND = "Muscle Band";
    private static final int MUSCLE_BAND_BONUS = 20;

    @Override
    public void handle(AttackContext ctx) {
        applyMuscleBand(ctx);
    }

    private void applyMuscleBand(AttackContext ctx) {
        AttachedCard tool = ctx.getAttackerPokemon().getTool();
        if (tool == null) return;

        Card toolCard = ctx.getCardLookup().findById(tool.getCardId());
        if (toolCard == null || !MUSCLE_BAND.equalsIgnoreCase(toolCard.getName())) return;

        Card defenderCard = ctx.getDefenderCard();
        if (defenderCard == null) return;

        CardType defenderType = CardTypeResolver.resolve(defenderCard);
        if (defenderType == CardType.POKEMON_EX || defenderType == CardType.MEGA_POKEMON) {
            ctx.setDamageModifiers(ctx.getDamageModifiers() + MUSCLE_BAND_BONUS);
        }
    }
}
