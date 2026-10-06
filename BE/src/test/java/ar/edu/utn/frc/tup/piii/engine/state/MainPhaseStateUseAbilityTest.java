package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * REQ-E3: player-activated abilities via USE_ABILITY (tier 1: Delphox's Mystical Fire).
 * PlayerField.hand/deck are List&lt;String&gt; of card IDs.
 */
class MainPhaseStateUseAbilityTest {

    private static final String MYSTICAL_FIRE_JSON = """
        {"abilities":[{"name":"Mystical Fire",
          "text":"Once during your turn (before your attack), you may draw cards until you have 6 cards in your hand.",
          "parsedEffects":[{"type":"PASSIVE_ABILITY","trigger":"ON_PLAY","conditions":[],
            "effect":{"type":"DRAW_UNTIL_HAND_SIZE","amount":6},"stackable":false}]}]}
        """;

    private MainPhaseState mainPhaseState;
    private BoardState board;
    private PlayerField ownerField;
    private CardLookup cardLookup;

    @BeforeEach
    void setUp() {
        mainPhaseState = new MainPhaseState();
        board = new BoardState();
        board.setCurrentPlayerId(1L);

        ownerField = new PlayerField();
        ownerField.setPlayerId(1L);
        ownerField.setHand(new ArrayList<>(List.of("h1", "h2", "h3")));
        ownerField.setDeck(new ArrayList<>(List.of("d1", "d2", "d3", "d4", "d5")));
        ownerField.setDiscardPile(new ArrayList<>());
        ownerField.setBench(new ArrayList<>());
        ownerField.setTurnFlags(new TurnFlags());
        ownerField.setActivePokemon(ActivePokemon.builder()
                .cardId("xy1-26")
                .maxHp(130).currentHp(130)
                .attachedEnergies(new ArrayList<>())
                .build());

        board.setPlayer1Field(ownerField);
        cardLookup = Mockito.mock(CardLookup.class);

        Card delphox = new Card();
        delphox.setId("xy1-26");
        delphox.setName("Delphox");
        delphox.setParsedEffects(MYSTICAL_FIRE_JSON);
        when(cardLookup.findById("xy1-26")).thenReturn(delphox);
    }

    private ActionRequest useAbilityRequest() {
        ActionRequest req = new ActionRequest();
        req.setType(ActionType.USE_ABILITY);
        req.setTargetPosition("ACTIVE");
        return req;
    }

    @Test
    void useAbility_mysticalFire_drawsUntilHandSizeSix() {
        ActionResult result = mainPhaseState.handle(useAbilityRequest(), board, 1L, cardLookup);

        assertTrue(result.isSuccess(), () -> "expected success, got: " + result.getError());
        assertEquals(6, ownerField.getHand().size());
        assertEquals(2, ownerField.getDeck().size());
        assertTrue(result.getEvents().stream()
                .anyMatch(e -> e.getType() == GameEventType.ABILITY_USED));
    }

    @Test
    void useAbility_sameAbilityTwiceInTurn_fails() {
        assertTrue(mainPhaseState.handle(useAbilityRequest(), board, 1L, cardLookup).isSuccess());

        ActionResult second = mainPhaseState.handle(useAbilityRequest(), board, 1L, cardLookup);

        assertFalse(second.isSuccess());
        assertTrue(second.getError().contains("ABILITY_ALREADY_USED"));
        assertEquals(6, ownerField.getHand().size()); // no double draw
    }

    @Test
    void useAbility_pokemonWithoutAbility_fails() {
        Card plain = new Card();
        plain.setId("xy1-1");
        plain.setName("Venusaur");
        when(cardLookup.findById("xy1-1")).thenReturn(plain);
        ownerField.getActivePokemon().setCardId("xy1-1");

        ActionResult result = mainPhaseState.handle(useAbilityRequest(), board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("NO_ABILITY"));
    }

    @Test
    void useAbility_handAlreadyAtSize_drawsNothingButCountsAsUsed() {
        ownerField.setHand(new ArrayList<>(List.of("h1", "h2", "h3", "h4", "h5", "h6")));

        ActionResult result = mainPhaseState.handle(useAbilityRequest(), board, 1L, cardLookup);

        assertTrue(result.isSuccess());
        assertEquals(6, ownerField.getHand().size());
        assertTrue(ownerField.getTurnFlags().getAbilitiesUsedThisTurn()
                .contains("ACTIVE#Mystical Fire"));
    }
}
