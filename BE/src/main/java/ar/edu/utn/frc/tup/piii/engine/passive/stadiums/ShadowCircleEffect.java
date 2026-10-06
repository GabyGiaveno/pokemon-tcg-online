package ar.edu.utn.frc.tup.piii.engine.passive.stadiums;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.passive.StadiumEffect;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;

public class ShadowCircleEffect implements StadiumEffect {

    @Override
    public boolean suppressesWeakness(ActivePokemon defender, CardLookup cardLookup) {
        if (defender == null || defender.getAttachedEnergies() == null || cardLookup == null) return false;
        return defender.getAttachedEnergies().stream()
                .anyMatch(e -> hasType(e.getCardId(), "Darkness", cardLookup));
    }

    private boolean hasType(String cardId, String type, CardLookup cardLookup) {
        if (cardId == null) return false;
        try {
            Card card = cardLookup.findById(cardId);
            return card != null && card.getTypes() != null && card.getTypes().contains(type);
        } catch (Exception e) {
            return false;
        }
    }
}
