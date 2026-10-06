package ar.edu.utn.frc.tup.piii.engine.effects.abilities;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackData;
import ar.edu.utn.frc.tup.piii.engine.chain.handlers.DamageApplicationHandler;
import ar.edu.utn.frc.tup.piii.engine.effects.logics.ApplyConditionLogic;
import ar.edu.utn.frc.tup.piii.engine.state.MainPhaseState;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.cards.effects.ApplyConditionEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
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
 * REQ-E5: continuous abilities as rule guards — Fur Coat (damage reduction),
 * Forest's Curse (Item lock) and Sweet Veil (condition immunity).
 */
class ContinuousAbilityQueryTest {

    private static final String FUR_COAT_JSON = """
        {"abilities":[{"name":"Fur Coat","text":"…","parsedEffects":[
          {"type":"PASSIVE_ABILITY","trigger":"ON_ATTACK_RECEIVED","conditions":[],
           "effect":{"type":"REDUCE_DAMAGE","amount":20,"target":"SELF"},"stackable":true}]}]}
        """;

    private static final String FORESTS_CURSE_JSON = """
        {"abilities":[{"name":"Forest's Curse","text":"…","parsedEffects":[
          {"type":"PASSIVE_ABILITY","trigger":"ON_PLAY",
           "conditions":[{"type":"IS_ACTIVE","target":"SELF","value":"true"}],
           "effect":{"type":"RESTRICT_ITEMS","target":"OPPONENT"},"stackable":false}]}]}
        """;

    private static final String SWEET_VEIL_JSON = """
        {"abilities":[{"name":"Sweet Veil","text":"…","parsedEffects":[
          {"type":"PASSIVE_ABILITY","trigger":"ON_PLAY",
           "conditions":[{"type":"HAS_ENERGY","target":"SELF","value":"FAIRY"}],
           "effect":{"type":"IMMUNE_TO_CONDITIONS","target":"SELF"},"stackable":false}]}]}
        """;

    private CardLookup cardLookup;

    @BeforeEach
    void setUp() {
        cardLookup = Mockito.mock(CardLookup.class);
    }

    private Card cardWith(String id, String name, String parsedEffects) {
        Card card = new Card();
        card.setId(id);
        card.setName(name);
        card.setParsedEffects(parsedEffects);
        when(cardLookup.findById(id)).thenReturn(card);
        return card;
    }

    private PlayerField field(Long playerId, ActivePokemon active) {
        PlayerField f = new PlayerField();
        f.setPlayerId(playerId);
        f.setActivePokemon(active);
        f.setBench(new ArrayList<>());
        f.setHand(new ArrayList<>());
        f.setDeck(new ArrayList<>());
        f.setDiscardPile(new ArrayList<>());
        f.setPrizeCards(new ArrayList<>());
        f.setTurnFlags(new TurnFlags());
        return f;
    }

    private ActivePokemon pokemon(String cardId, int hp) {
        return ActivePokemon.builder().cardId(cardId).maxHp(hp).currentHp(hp)
                .attachedEnergies(new ArrayList<>()).build();
    }

    // ── Fur Coat: −20 after W&R and modifiers ────────────────────────────────

    @Test
    void furCoat_reducesFinalDamageBy20() {
        cardWith("xy1-114", "Furfrou", FUR_COAT_JSON);
        Card attackerCard = cardWith("atk-1", "Attacker", null);

        ActivePokemon defender = pokemon("xy1-114", 110);
        ActivePokemon attacker = pokemon("atk-1", 100);
        PlayerField defenderField = field(2L, defender);

        AttackData attack = new AttackData();
        attack.setDamage("50");

        AttackContext ctx = AttackContext.builder()
                .cardLookup(cardLookup)
                .attackerField(field(1L, attacker)).defenderField(defenderField)
                .attackerPokemon(attacker).defenderPokemon(defender)
                .attackerCard(attackerCard).defenderCard(cardLookup.findById("xy1-114"))
                .attackData(attack)
                .build();

        new DamageApplicationHandler().handle(ctx);

        assertEquals(30, ctx.getFinalDamage());
        assertEquals(80, defender.getCurrentHp());
    }

    @Test
    void furCoat_neverPushesDamageBelowZero() {
        cardWith("xy1-114", "Furfrou", FUR_COAT_JSON);
        Card attackerCard = cardWith("atk-1", "Attacker", null);

        ActivePokemon defender = pokemon("xy1-114", 110);
        ActivePokemon attacker = pokemon("atk-1", 100);

        AttackData attack = new AttackData();
        attack.setDamage("10");

        AttackContext ctx = AttackContext.builder()
                .cardLookup(cardLookup)
                .attackerField(field(1L, attacker)).defenderField(field(2L, defender))
                .attackerPokemon(attacker).defenderPokemon(defender)
                .attackerCard(attackerCard).defenderCard(cardLookup.findById("xy1-114"))
                .attackData(attack)
                .build();

        new DamageApplicationHandler().handle(ctx);

        assertEquals(0, ctx.getFinalDamage());
        assertEquals(110, defender.getCurrentHp());
    }

    // ── Forest's Curse: Item lock from the opposing Active ──────────────────

    @Test
    void forestsCurse_blocksPlayItem_whileTrevenantIsActive() {
        cardWith("xy1-55", "Trevenant", FORESTS_CURSE_JSON);
        Card itemCard = cardWith("item-1", "Potion", "{}");
        itemCard.setSupertype("Trainer");
        itemCard.setSubtypes(List.of("Item"));

        BoardState board = new BoardState();
        board.setCurrentPlayerId(1L);
        PlayerField actor = field(1L, null);
        actor.getHand().add("item-1");
        board.setPlayer1Field(actor);
        board.setPlayer2Field(field(2L, pokemon("xy1-55", 110)));

        ActionRequest req = new ActionRequest();
        req.setType(ActionType.PLAY_ITEM);
        req.setCardId("item-1");

        ActionResult result = new MainPhaseState().handle(req, board, 1L, cardLookup);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("ITEMS_LOCKED"));
        assertTrue(actor.getHand().contains("item-1"), "card must stay in hand");
    }

    @Test
    void forestsCurse_doesNotBlock_whenTrevenantIsBenched() {
        cardWith("xy1-55", "Trevenant", FORESTS_CURSE_JSON);
        Card itemCard = cardWith("item-1", "Potion", "{}");
        itemCard.setSupertype("Trainer");
        itemCard.setSubtypes(List.of("Item"));

        BoardState board = new BoardState();
        board.setCurrentPlayerId(1L);
        PlayerField actor = field(1L, null);
        actor.getHand().add("item-1");
        board.setPlayer1Field(actor);
        PlayerField opponent = field(2L, pokemon("other-1", 60));
        cardWith("other-1", "Other", null);
        opponent.getBench().add(BenchPokemon.builder().cardId("xy1-55").maxHp(110)
                .currentHp(110).attachedEnergies(new ArrayList<>()).build());
        board.setPlayer2Field(opponent);

        ActionRequest req = new ActionRequest();
        req.setType(ActionType.PLAY_ITEM);
        req.setCardId("item-1");

        ActionResult result = new MainPhaseState().handle(req, board, 1L, cardLookup);

        assertTrue(result.isSuccess(), () -> "expected success, got: " + result.getError());
    }

    // ── Sweet Veil: condition immunity for Fairy-energized Pokémon ──────────

    @Test
    void sweetVeil_blocksConditionOnFairyEnergizedTarget() {
        cardWith("xy1-95", "Slurpuff", SWEET_VEIL_JSON);
        Card fairyEnergy = cardWith("fairy-energy", "Fairy Energy", null);
        fairyEnergy.setTypes(List.of("Fairy"));

        ActivePokemon defender = pokemon("def-1", 100);
        defender.getAttachedEnergies().add(AttachedCard.builder().cardId("fairy-energy").build());
        cardWith("def-1", "Defender", null);

        PlayerField defenderField = field(2L, defender);
        defenderField.getBench().add(BenchPokemon.builder().cardId("xy1-95").maxHp(80)
                .currentHp(80).attachedEnergies(new ArrayList<>()).build());

        ActivePokemon attacker = pokemon("atk-1", 100);
        AttackContext ctx = AttackContext.builder()
                .cardLookup(cardLookup)
                .attackerField(field(1L, attacker)).defenderField(defenderField)
                .attackerPokemon(attacker).defenderPokemon(defender)
                .attackData(new AttackData())
                .build();

        ApplyConditionEffect poison = new ApplyConditionEffect();
        poison.setTarget("DEFENDER");
        poison.setCondition("POISONED");

        new ApplyConditionLogic().execute(poison, ctx);

        assertFalse(defender.isPoisoned(), "Sweet Veil must block the condition");
    }

    @Test
    void sweetVeil_doesNotProtectTargetsWithoutFairyEnergy() {
        cardWith("xy1-95", "Slurpuff", SWEET_VEIL_JSON);

        ActivePokemon defender = pokemon("def-1", 100); // no fairy energy attached
        cardWith("def-1", "Defender", null);

        PlayerField defenderField = field(2L, defender);
        defenderField.getBench().add(BenchPokemon.builder().cardId("xy1-95").maxHp(80)
                .currentHp(80).attachedEnergies(new ArrayList<>()).build());

        ActivePokemon attacker = pokemon("atk-1", 100);
        AttackContext ctx = AttackContext.builder()
                .cardLookup(cardLookup)
                .attackerField(field(1L, attacker)).defenderField(defenderField)
                .attackerPokemon(attacker).defenderPokemon(defender)
                .attackData(new AttackData())
                .build();

        ApplyConditionEffect poison = new ApplyConditionEffect();
        poison.setTarget("DEFENDER");
        poison.setCondition("POISONED");

        new ApplyConditionLogic().execute(poison, ctx);

        assertTrue(defender.isPoisoned(), "without Fairy energy the condition applies");
    }

    @Test
    void sweetVeil_irrelevantWhenSlurpuffNotInPlay() {
        Card fairyEnergy = cardWith("fairy-energy", "Fairy Energy", null);
        fairyEnergy.setTypes(List.of("Fairy"));

        ActivePokemon defender = pokemon("def-1", 100);
        defender.getAttachedEnergies().add(AttachedCard.builder().cardId("fairy-energy").build());
        cardWith("def-1", "Defender", null);

        ActivePokemon attacker = pokemon("atk-1", 100);
        AttackContext ctx = AttackContext.builder()
                .cardLookup(cardLookup)
                .attackerField(field(1L, attacker)).defenderField(field(2L, defender))
                .attackerPokemon(attacker).defenderPokemon(defender)
                .attackData(new AttackData())
                .build();

        ApplyConditionEffect sleep = new ApplyConditionEffect();
        sleep.setTarget("DEFENDER");
        sleep.setCondition("ASLEEP");

        new ApplyConditionLogic().execute(sleep, ctx);

        assertEquals(SpecialCondition.ASLEEP, defender.getCondition());
    }
}
