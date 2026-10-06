package ar.edu.utn.frc.tup.piii.engine.passive;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;

public interface StadiumEffect {
    default boolean suppressesWeakness(ActivePokemon defender, CardLookup cardLookup) { return false; }
    default int modifyRetreatCost(int cost, ActivePokemon active, CardLookup cardLookup) { return cost; }
}
