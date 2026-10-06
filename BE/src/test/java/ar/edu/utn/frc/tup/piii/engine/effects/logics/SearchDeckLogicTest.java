package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.SearchDeckEffect;
import ar.edu.utn.frc.tup.piii.models.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchDeckLogicTest {

    private SearchDeckLogic logic;
    private PlayerField attackerField;
    private AttackContext ctx;

    @BeforeEach
    void setUp() {
        logic = new SearchDeckLogic();

        attackerField = new PlayerField();
        attackerField.setPlayerId(1L);
        attackerField.setTurnFlags(new TurnFlags());
        attackerField.setHand(new ArrayList<>());
        attackerField.setDeck(new ArrayList<>());
        attackerField.getInstanceCardIds().put("fire-inst", "xy1-fire");
        attackerField.getInstanceCardIds().put("sup-inst", "xy1-sup");
        attackerField.getInstanceCardIds().put("poke-inst", "xy1-poke");

        ctx = AttackContext.builder()
                .attackerPokemon(ActivePokemon.builder().cardId("attacker")
                        .attachedEnergies(new ArrayList<>()).build())
                .defenderPokemon(ActivePokemon.builder().cardId("defender").build())
                .attackerField(attackerField)
                .defenderField(new PlayerField())
                .board(new BoardState())
                .cardLookup(id -> {
                    return switch (id) {
                        case "xy1-fire" -> card("Energy", List.of("Basic"), List.of("Fire"));
                        case "xy1-sup"  -> card("Trainer", List.of("Supporter"), null);
                        case "xy1-poke" -> card("Pokémon", List.of("Stage 1"), List.of("Grass"));
                        default -> null;
                    };
                })
                .build();
    }

    private Card card(String supertype, List<String> subtypes, List<String> types) {
        Card c = new Card();
        c.setSupertype(supertype);
        c.setSubtypes(subtypes);
        c.setTypes(types);
        return c;
    }

    private SearchDeckEffect effect(String filter, String destination, int amount) {
        SearchDeckEffect e = new SearchDeckEffect();
        e.setFilter(filter);
        e.setDestination(destination);
        e.setAmount(amount);
        return e;
    }

    @Test
    void isPostDamage_returnsTrue() {
        assertTrue(logic.isPostDamage());
    }

    @Test
    void execute_attachDestination_autoAttachesFirstMatchToActive() {
        attackerField.getDeck().add("fire-inst");
        logic.execute(effect("ENERGY_FIRE", "ATTACH", 1), ctx);
        assertFalse(ctx.getAttackerPokemon().getAttachedEnergies().isEmpty());
        assertTrue(attackerField.getDeck().isEmpty());
        assertEquals(GameEventType.ENERGY_ATTACHED, ctx.getEvents().get(0).getType());
    }

    @Test
    void execute_handDestination_setsPendingSearchDeck() {
        attackerField.getDeck().add("sup-inst");
        logic.execute(effect("SUPPORTER", "HAND", 1), ctx);
        PendingSelection sel = ctx.getBoard().getPendingSelection();
        assertNotNull(sel);
        assertEquals(SelectionType.SEARCH_DECK, sel.getType());
        assertTrue(sel.getValidOptions().contains("sup-inst"));
        assertEquals(1, sel.getSelectionCount());
    }

    @Test
    void execute_noMatchingCards_doesNothing() {
        attackerField.getDeck().add("poke-inst"); // Grass Pokémon, not a Supporter
        logic.execute(effect("SUPPORTER", "HAND", 1), ctx);
        assertNull(ctx.getBoard().getPendingSelection());
        assertTrue(ctx.getEvents().isEmpty());
    }

    @Test
    void execute_emptyDeck_doesNothing() {
        logic.execute(effect("ANY", "HAND", 1), ctx);
        assertNull(ctx.getBoard().getPendingSelection());
    }

    @Test
    void matches_any_alwaysTrue() {
        assertTrue(SearchDeckLogic.matches(null, "ANY"));
        assertTrue(SearchDeckLogic.matches(card("Energy", List.of("Basic"), List.of("Fire")), "ANY"));
    }

    @Test
    void matches_energyFire_matchesFireEnergy() {
        Card fire = card("Energy", List.of("Basic"), List.of("Fire"));
        assertTrue(SearchDeckLogic.matches(fire, "ENERGY_FIRE"));
    }

    @Test
    void matches_energyFire_doesNotMatchWater() {
        Card water = card("Energy", List.of("Basic"), List.of("Water"));
        assertFalse(SearchDeckLogic.matches(water, "ENERGY_FIRE"));
    }

    @Test
    void matches_supporter_matchesSupporterTrainer() {
        Card sup = card("Trainer", List.of("Supporter"), null);
        assertTrue(SearchDeckLogic.matches(sup, "SUPPORTER"));
    }

    @Test
    void matches_pokemonGrass_matchesGrassPokemon() {
        Card poke = card("Pokémon", List.of("Stage 1"), List.of("Grass"));
        assertTrue(SearchDeckLogic.matches(poke, "POKEMON_GRASS"));
    }

    @Test
    void matches_pokemonGrass_doesNotMatchFirePokemon() {
        Card fire = card("Pokémon", List.of("Basic"), List.of("Fire"));
        assertFalse(SearchDeckLogic.matches(fire, "POKEMON_GRASS"));
    }

    @Test
    void execute_revealedCardIdsPopulated() {
        attackerField.getDeck().add("fire-inst");
        logic.execute(effect("ENERGY_FIRE", "HAND", 1), ctx);
        PendingSelection sel = ctx.getBoard().getPendingSelection();
        assertNotNull(sel);
        assertEquals(List.of("xy1-fire"), sel.getRevealedCardIds());
    }
}
