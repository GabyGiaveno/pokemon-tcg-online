package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Spring Data JPA repository for Deck; provides findByPlayer and findByPlayerId. */
public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByPlayer(Player player);
    List<Deck> findByPlayerId(Long playerId);

    /**
     * Fetches decks by player ID with the cards collection eagerly loaded
     * in a single JPQL query, preventing LazyInitializationException.
     */
    @Query("SELECT DISTINCT d FROM Deck d LEFT JOIN FETCH d.cards WHERE d.player.id = :playerId")
    List<Deck> findByPlayerIdWithCards(@Param("playerId") Long playerId);

    /**
     * Fetches a single deck by ID with the cards collection eagerly loaded,
     * preventing LazyInitializationException on the lazy @OneToMany.
     */
    @Query("SELECT d FROM Deck d LEFT JOIN FETCH d.cards WHERE d.id = :id")
    Optional<Deck> findByIdWithCards(@Param("id") Long id);

    /**
     * Returns the set of non-null default keys for decks owned by the given player.
     * Used by provisioning service to determine which starter decks already exist.
     */
    @Query("SELECT d.defaultKey FROM Deck d WHERE d.player.id = :playerId AND d.defaultKey IS NOT NULL")
    Set<String> findDefaultKeysByPlayerId(@Param("playerId") Long playerId);
}
