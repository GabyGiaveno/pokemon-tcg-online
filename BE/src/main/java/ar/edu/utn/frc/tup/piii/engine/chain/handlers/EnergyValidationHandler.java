package ar.edu.utn.frc.tup.piii.engine.chain.handlers;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackHandler;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handler 1 — Energy Validation & Discard.
 *
 * <p>Verifies that the attacking Pokémon has sufficient attached energy to pay
 * the cost declared in {@link ar.edu.utn.frc.tup.piii.engine.chain.AttackData#getCost()}.
 * Energy is NOT discarded by paying the cost (XY rules) - only explicit effects,
 * retreat, or leaving play remove attached energy (spec effect-execution REQ-A0).
 *
 * <h3>Matching algorithm (two-pass)</h3>
 * <ol>
 *   <li>First pass — specific types: each required energy type (e.g. "Fire") is matched
 *       against the pool of attached energies. A matching energy is consumed from the pool.</li>
 *   <li>Second pass — Colorless costs: any remaining energies in the pool satisfy Colorless
 *       requirements one-for-one, regardless of type.</li>
 * </ol>
 *
 * <p>If validation fails, {@link AttackContext#cancelAttack()} is called and one
 * {@link GameEventType#ATTACK_DECLARED} event is appended with {@code errorCode = "INSUFFICIENT_ENERGY"}.
 */
public class EnergyValidationHandler implements AttackHandler {

    @Override
    public void handle(AttackContext ctx) {
        List<String> cost = ctx.getAttackData().getCost();
        if (cost.isEmpty()) return; // free attack — no energy required

        // Build a mutable pool of attached energy card references
        List<AttachedCard> remainingEnergies = new ArrayList<>(ctx.getAttackerPokemon().getAttachedEnergies());
        if (remainingEnergies == null || remainingEnergies.isEmpty()) {
            cancelWithReason(ctx, cost.get(0));
            return;
        }

        List<String> colorlessCosts = new ArrayList<>();
        String missingType = null;

        // First pass: satisfy specific-type requirements
        for (String required : cost) {
            if (required.equalsIgnoreCase("Colorless")) {
                colorlessCosts.add(required);
            } else {
                boolean satisfied = false;
                for (int i = 0; i < remainingEnergies.size(); i++) {
                    String type = resolveEnergyType(remainingEnergies.get(i), ctx);
                    if (type.equalsIgnoreCase(required)) {
                        remainingEnergies.remove(i);
                        satisfied = true;
                        break;
                    }
                }
                if (!satisfied) {
                    missingType = required;
                    break;
                }
            }
        }

        if (missingType != null) {
            cancelWithReason(ctx, missingType);
            return;
        }

        // Second pass: Colorless can be satisfied by any remaining energy
        if (colorlessCosts.size() > remainingEnergies.size()) {
            cancelWithReason(ctx, "Colorless");
            return;
        }

        // Remove the Colorless-matched energies from the pool
        for (int i = 0; i < colorlessCosts.size(); i++) {
            remainingEnergies.remove(0);
        }

        // BUG-5 FIX: Energy is NOT discarded from the Pokémon after attacking.
        // In TCG rules, energy cards stay attached unless a specific effect discards them.
        // The pool 'remainingEnergies' is intentionally not written back.
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Resolves the energy type of an attached card by looking it up via the CardLookup.
     * Falls back to "Colorless" if the card cannot be found or has no type information.
     */
    private String resolveEnergyType(AttachedCard attached, AttackContext ctx) {
        Card energyCard;
        try {
            energyCard = ctx.getCardLookup().findById(attached.getCardId());
        } catch (RuntimeException e) {
            return "Colorless";
        }
        if (energyCard == null) {
            return "Colorless";
        }
        // Some cards expose a Pokémon-style `types` array.
        if (energyCard.getTypes() != null && !energyCard.getTypes().isEmpty()) {
            return energyCard.getTypes().get(0);
        }
        // Basic Energy cards have NO `types` field (that's a Pokémon-only attribute),
        // so the element must be derived from the card name, which follows the
        // "{Type} Energy" convention (e.g. "Fire Energy" → "Fire",
        // "Lightning Energy" → "Lightning"). Without this, every basic energy
        // resolved to "Colorless" and typed attacks were wrongly rejected.
        String name = energyCard.getName();
        if (name != null) {
            String trimmed = name.trim();
            int idx = trimmed.toLowerCase().lastIndexOf(" energy");
            if (idx > 0) {
                return trimmed.substring(0, idx).trim();
            }
        }
        return "Colorless";
    }

    private void cancelWithReason(AttackContext ctx, String missingType) {
        ctx.cancelAttack();
        ctx.addEvent(GameEvent.of(
                GameEventType.ATTACK_DECLARED,
                "Not enough energy to use " + ctx.getAttackData().getName()
                        + ". Missing: " + missingType + ".",
                Map.of("errorCode", "INSUFFICIENT_ENERGY",
                        "attack", ctx.getAttackData().getName(),
                        "missingType", missingType)));
    }
}
