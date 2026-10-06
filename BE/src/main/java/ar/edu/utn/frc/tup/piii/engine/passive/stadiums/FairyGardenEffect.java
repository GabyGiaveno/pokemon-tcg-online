package ar.edu.utn.frc.tup.piii.engine.passive.stadiums;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.passive.StadiumEffect;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;

public class FairyGardenEffect implements StadiumEffect {

    @Override
    public int modifyRetreatCost(int cost, ActivePokemon active, CardLookup cardLookup) {
        if (active == null || active.getAttachedEnergies() == null || cardLookup == null) return cost;
        boolean hasFairyEnergy = active.getAttachedEnergies().stream()
                .anyMatch(e -> hasType(e.getCardId(), "Fairy", cardLookup));
        return hasFairyEnergy ? 0 : cost;
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
