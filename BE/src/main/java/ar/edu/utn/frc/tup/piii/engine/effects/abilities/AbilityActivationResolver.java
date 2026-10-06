package ar.edu.utn.frc.tup.piii.engine.effects.abilities;

import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.cards.effects.DrawUntilHandSizeEffect;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executes the nested effect of a player-activated ability ({@code USE_ABILITY}).
 *
 * <p>Tier 1 supports {@code DRAW_UNTIL_HAND_SIZE} (Delphox's Mystical Fire). Effects that
 * require player selection (search, switch, energy transfer…) belong to tier 2 and are
 * rejected explicitly — never silently consumed.
 */
public class AbilityActivationResolver {

    /**
     * @param ownerField the activating player's field (mutated)
     * @param abilityName for event reporting
     * @param effect      the ability's nested effect
     * @return events produced by the activation
     * @throws InvalidActionException {@code ABILITY_NOT_SUPPORTED} for tier-2 effects
     */
    public List<GameEvent> activate(PlayerField ownerField, String abilityName, AttackEffect effect) {
        List<GameEvent> events = new ArrayList<>();
        if (effect instanceof DrawUntilHandSizeEffect draw) {
            int drawn = drawUntilHandSize(ownerField, draw.getAmount());
            events.add(GameEvent.of(GameEventType.ABILITY_USED,
                    abilityName + ": drew " + drawn + " card(s).",
                    Map.of("playerId", ownerField.getPlayerId(),
                            "ability", abilityName,
                            "cardsDrawn", drawn,
                            "handSize", ownerField.getHand().size())));
            return events;
        }
        throw new InvalidActionException("ABILITY_NOT_SUPPORTED",
                "Ability '" + abilityName + "' requires a mechanism not yet supported (tier 2).");
    }

    /** Draws from the top of the deck until the hand reaches {@code targetSize} or the deck runs out. */
    private int drawUntilHandSize(PlayerField field, int targetSize) {
        int drawn = 0;
        while (field.getHand().size() < targetSize && !field.getDeck().isEmpty()) {
            field.getHand().add(field.getDeck().remove(0));
            drawn++;
        }
        return drawn;
    }
}
