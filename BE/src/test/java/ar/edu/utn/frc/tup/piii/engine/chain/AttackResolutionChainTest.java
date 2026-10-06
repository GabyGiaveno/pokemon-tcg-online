package ar.edu.utn.frc.tup.piii.engine.chain;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.cards.CardType;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttackResolutionChainTest {

        private AttackResolutionChain chain;

        @BeforeEach
        void setUp() {
                chain = new AttackResolutionChain();
        }

        @Test
        void resolve_BasicDamage_WithWeakness() {
                // Arrange: Pikachu (Lightning) attacks Squirtle (Water) which has Lightning
                // Weakness.
                Long p1Id = 1L;
                Long p2Id = 2L;

                Card energyCard = Card.builder()
                                .id("lightning-energy")
                                .supertype("Energy")
                                .types(List.of("Lightning"))
                                .subtypes(List.of("Basic"))
                                .name("Lightning Energy")
                                .build();

                Card attackerCard = Card.builder()
                                .id("pikachu1")
                                .types(List.of("Lightning"))
                                // Requires 1 Lightning energy, does 20 base damage
                                .attacks("[{\"name\":\"Quick Attack\", \"cost\":[\"Lightning\"], \"convertedEnergyCost\":1, \"damage\":\"20\", \"text\":\"\"}]")
                                .build();

                Card defenderCard = Card.builder()
                                .id("squirtle1")
                                .types(List.of("Water"))
                                // Weak to Lightning x2
                                .weaknesses("[{\"type\":\"Lightning\", \"value\":\"×2\"}]")
                                .build();

                CardLookup cardLookup = id -> {
                        if ("pikachu1".equals(id))
                                return attackerCard;
                        if ("squirtle1".equals(id))
                                return defenderCard;
                        if ("lightning-energy".equals(id))
                                return energyCard;
                        return null;
                };

                // Attach energy so validation passes
                AttachedCard attachedEnergy = AttachedCard.builder()
                                .instanceId("energy-1")
                                .cardId("lightning-energy")
                                .type(CardType.BASIC_ENERGY)
                                .build();
                List<AttachedCard> energies = new ArrayList<>();
                energies.add(attachedEnergy);

                ActivePokemon attacker = ActivePokemon.builder()
                                .cardId("pikachu1")
                                .currentHp(60)
                                .attachedEnergies(energies)
                                .condition(SpecialCondition.NONE)
                                .build();

                ActivePokemon defender = ActivePokemon.builder()
                                .cardId("squirtle1")
                                .currentHp(60)
                                .attachedEnergies(new ArrayList<>())
                                .condition(SpecialCondition.NONE)
                                .build();

                PlayerField p1Field = PlayerField.builder()
                                .playerId(p1Id)
                                .activePokemon(attacker)
                                .turnFlags(new TurnFlags())
                                .build();

                PlayerField p2Field = PlayerField.builder()
                                .playerId(p2Id)
                                .activePokemon(defender)
                                .prizeCards(new ArrayList<>(List.of("prize1")))
                                .discardPile(new ArrayList<>())
                                .bench(new ArrayList<>())
                                .turnFlags(new TurnFlags())
                                .build();

                BoardState board = BoardState.builder()
                                .player1Field(p1Field)
                                .player2Field(p2Field)
                                .currentPlayerId(p1Id)
                                .build();

                ActionRequest action = new ActionRequest();
                action.setType(ActionType.USE_ATTACK);
                action.setAttackIndex(0); // Quick Attack

                // Act
                List<GameEvent> events = chain.resolve(action, board, p1Id, cardLookup);

                // Assert
                // Base damage 20 * 2 (weakness) = 40 damage.
                assertEquals(20, defender.getCurrentHp(),
                                "Defender should have taken 40 damage (20 * 2 weakness). Original HP was 60.");

                // Assert that the pipeline successfully created events
                assertFalse(events.isEmpty(), "Chain should produce events.");
        }

        @Test
        void resolve_InsufficientEnergy_CancelsAttack() {
                Long p1Id = 1L;
                Long p2Id = 2L;

                Card attackerCard = Card.builder()
                                .id("charmander1")
                                .types(List.of("Fire"))
                                .attacks("[{\"name\":\"Ember\", \"cost\":[\"Fire\", \"Colorless\"], \"convertedEnergyCost\":2, \"damage\":\"30\", \"text\":\"\"}]")
                                .build();

                Card defenderCard = Card.builder()
                                .id("bulbasaur1")
                                .types(List.of("Grass"))
                                .build();

                CardLookup cardLookup = id -> {
                        if ("charmander1".equals(id))
                                return attackerCard;
                        if ("bulbasaur1".equals(id))
                                return defenderCard;
                        return null; // Missing energy card lookup will default to Colorless, but we won't attach any
                };

                ActivePokemon attacker = ActivePokemon.builder()
                                .cardId("charmander1")
                                .currentHp(60)
                                .attachedEnergies(new ArrayList<>()) // NO ENERGIES ATTACHED
                                .build();

                ActivePokemon defender = ActivePokemon.builder()
                                .cardId("bulbasaur1")
                                .currentHp(60)
                                .build();

                BoardState board = BoardState.builder()
                                .player1Field(PlayerField.builder().playerId(p1Id).activePokemon(attacker)
                                                .turnFlags(new TurnFlags()).build())
                                .player2Field(PlayerField.builder().playerId(p2Id).activePokemon(defender)
                                                .turnFlags(new TurnFlags()).build())
                                .currentPlayerId(p1Id)
                                .build();

                ActionRequest action = new ActionRequest();
                action.setType(ActionType.USE_ATTACK);
                action.setAttackIndex(0);

                List<GameEvent> events = chain.resolve(action, board, p1Id, cardLookup);

                assertEquals(60, defender.getCurrentHp(),
                                "Defender should take 0 damage because attack was cancelled.");
                assertTrue(events.stream().anyMatch(
                                e -> e.getDescription() != null && e.getDescription().contains("Not enough energy")),
                                "Should produce insufficient energy event.");
        }

        @Test
        void resolve_Knockout_ProcessesPrizes_WithoutDecidingVictory() {
                Long p1Id = 1L;
                Long p2Id = 2L;

                Card attackerCard = Card.builder()
                                .id("mewtwo1")
                                .types(List.of("Psychic"))
                                .attacks("[{\"name\":\"Psystrike\", \"cost\":[], \"convertedEnergyCost\":0, \"damage\":\"100\", \"text\":\"\"}]")
                                .build();

                Card defenderCard = Card.builder()
                                .id("rattata1")
                                .types(List.of("Colorless"))
                                .build();

                CardLookup cardLookup = id -> {
                        if ("mewtwo1".equals(id))
                                return attackerCard;
                        if ("rattata1".equals(id))
                                return defenderCard;
                        return null;
                };

                ActivePokemon attacker = ActivePokemon.builder()
                                .cardId("mewtwo1")
                                .currentHp(120)
                                .attachedEnergies(new ArrayList<>()) // cost is []
                                .build();

                ActivePokemon defender = ActivePokemon.builder()
                                .cardId("rattata1")
                                .currentHp(40)
                                .attachedEnergies(new ArrayList<>())
                                .build();

                PlayerField p1Field = PlayerField.builder()
                                .playerId(p1Id)
                                .activePokemon(attacker)
                                .hand(new ArrayList<>()) // Empty hand initially
                                .prizeCards(new ArrayList<>(List.of("prize-card-1"))) // Prizes for p1 to take
                                .turnFlags(new TurnFlags())
                                .build();

                PlayerField p2Field = PlayerField.builder()
                                .playerId(p2Id)
                                .activePokemon(defender)
                                .bench(new ArrayList<>()) // Empty bench, meaning if active is KO'd, game is over
                                .discardPile(new ArrayList<>())
                                .turnFlags(new TurnFlags())
                                .build();

                BoardState board = BoardState.builder()
                                .player1Field(p1Field)
                                .player2Field(p2Field)
                                .currentPlayerId(p1Id)
                                .build();

                ActionRequest action = new ActionRequest();
                action.setType(ActionType.USE_ATTACK);
                action.setAttackIndex(0);

                List<GameEvent> events = chain.resolve(action, board, p1Id, cardLookup);

                assertNull(p2Field.getActivePokemon(),
                                "Defender active pokemon should be moved to discard pile (null active).");
                assertEquals(1, p2Field.getDiscardPile().size(), "Discard pile should contain KO'd Rattata.");
                assertEquals("rattata1", p2Field.getDiscardPile().get(0));

                assertEquals(1, p1Field.getHand().size(), "Attacker should have taken 1 prize card into their hand.");
                assertEquals("prize-card-1", p1Field.getHand().get(0));

                // The chain no longer decides the game: the KO only mutates and reports. The
                // "no Pokémon left" victory is declared by the orchestrator (TurnManager +
                // VictoryConditionChecker), so the chain must NOT set a winner here.
                assertNull(board.getWinnerId(), "The chain must not declare a winner.");
        }

        // ── Bug #3: Take Down recoil — ADD_DAMAGE{target:SELF} must hurt attacker, not defender ──

        @Test
        void resolve_TakeDown_appliesRecoilToAttackerNotDefender() {
                Long p1Id = 1L;
                Long p2Id = 2L;

                // Take Down: 30 base damage + 10 recoil to self
                String parsedEffects = """
                        {"attacks":[{"name":"Take Down","parsedEffects":[{"type":"ADD_DAMAGE","amount":10,"target":"SELF"}]}]}
                        """;
                Card attackerCard = Card.builder()
                        .id("tauros1")
                        .types(List.of("Colorless"))
                        .attacks("[{\"name\":\"Take Down\",\"cost\":[\"Colorless\",\"Colorless\"],\"convertedEnergyCost\":2,\"damage\":\"30\",\"text\":\"This Pokémon does 10 damage to itself.\"}]")
                        .parsedEffects(parsedEffects)
                        .build();

                Card defenderCard = Card.builder()
                        .id("defender1")
                        .types(List.of("Colorless"))
                        .build();

                Card energyCard = Card.builder()
                        .id("colorless-energy")
                        .supertype("Energy")
                        .types(List.of("Colorless"))
                        .subtypes(List.of("Basic"))
                        .name("Double Colorless Energy")
                        .build();

                CardLookup lookup = id -> switch (id) {
                    case "tauros1" -> attackerCard;
                    case "defender1" -> defenderCard;
                    default -> energyCard;
                };

                AttachedCard e1 = AttachedCard.builder().instanceId("e1").cardId("colorless-energy")
                        .type(ar.edu.utn.frc.tup.piii.models.cards.CardType.BASIC_ENERGY).build();
                AttachedCard e2 = AttachedCard.builder().instanceId("e2").cardId("colorless-energy")
                        .type(ar.edu.utn.frc.tup.piii.models.cards.CardType.BASIC_ENERGY).build();

                ActivePokemon attacker = ActivePokemon.builder()
                        .cardId("tauros1").currentHp(90).maxHp(90)
                        .attachedEnergies(new ArrayList<>(List.of(e1, e2)))
                        .condition(SpecialCondition.NONE).build();

                ActivePokemon defender = ActivePokemon.builder()
                        .cardId("defender1").currentHp(100).maxHp(100)
                        .attachedEnergies(new ArrayList<>())
                        .condition(SpecialCondition.NONE).build();

                PlayerField p1Field = PlayerField.builder()
                        .playerId(p1Id).activePokemon(attacker)
                        .bench(new ArrayList<>()).discardPile(new ArrayList<>())
                        .prizeCards(new ArrayList<>()).hand(new ArrayList<>())
                        .turnFlags(new TurnFlags()).build();

                PlayerField p2Field = PlayerField.builder()
                        .playerId(p2Id).activePokemon(defender)
                        .bench(new ArrayList<>()).discardPile(new ArrayList<>())
                        .prizeCards(new ArrayList<>(List.of("prize1"))).hand(new ArrayList<>())
                        .turnFlags(new TurnFlags()).build();

                BoardState board = BoardState.builder()
                        .player1Field(p1Field).player2Field(p2Field)
                        .currentPlayerId(p1Id).build();

                ActionRequest action = new ActionRequest();
                action.setType(ActionType.USE_ATTACK);
                action.setAttackIndex(0);

                chain.resolve(action, board, p1Id, lookup);

                assertEquals(70, defender.getCurrentHp(),
                        "Defender should take exactly 30 damage (base), NOT 40. Recoil must not boost defender damage.");
                assertEquals(80, attacker.getCurrentHp(),
                        "Attacker (Tauros) must take 10 recoil damage from Take Down.");
        }
}
