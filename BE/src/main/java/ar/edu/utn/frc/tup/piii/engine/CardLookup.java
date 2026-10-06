package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.entities.Card;

/**
 * Bridge between the pure-Java engine and the Spring persistence layer.
 *
 * <p>
 * Engine components never import Spring beans directly; instead they receive a
 * {@code CardLookup} lambda injected by {@code GameEngineFacade}, which wraps
 * {@code CardCacheService.findById()} behind this interface.
 *
 * <pre>
 * CardLookup lookup = cardCacheService::findById;
 * turnManager.beginTurn(board, lookup);
 * </pre>
 */
@FunctionalInterface
public interface CardLookup {
    /**
     * Resolves a {@link Card} entity by its pokemontcg.io ID.
     *
     * @param cardId the card ID (e.g. {@code "xy1-1"})
     * @return the matching {@link Card}
     * @throws IllegalArgumentException if the card is not found in the local cache
     */
    Card findById(String cardId);
}
