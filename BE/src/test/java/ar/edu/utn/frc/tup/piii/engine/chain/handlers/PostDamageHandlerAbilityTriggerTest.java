package ar.edu.utn.frc.tup.piii.engine.chain.handlers;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.KnockoutProcessor;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackData;
import ar.edu.utn.frc.tup.piii.engine.effects.abilities.AbilityTriggerResolver;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.TurnFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * REQ-E4: triggered abilities fire at the post-damage reaction points.
 * Spiky Shield (ON_ATTACK_RECEIVED) and Destiny Burst (ON_ALLY_KNOCKOUT).
 */
class PostDamageHandlerAbilityTriggerTest {

    private static final String SPIKY_SHIELD_JSON = """
        {"abilities":[{"name":"Spiky Shield","text":"…","parsedEffects":[
          {"type":"PASSIVE_ABILITY","trigger":"ON_ATTACK_RECEIVED",
           "conditions":[{"type":"IS_ACTIVE","target":"SELF","value":"true"}],
           "effect":{"type":"DAMAGE_COUNTERS","amount":3,"target":"ATTACKER"},"stackable":false}]}]}
        """;

    private static final String DESTINY_BURST_JSON = """
        {"abilities":[{"name":"Destiny Burst","text":"…","parsedEffects":[
          {"type":"PASSIVE_ABILITY","trigger":"ON_ALLY_KNOCKOUT",
           "conditions":[{"type":"IS_ACTIVE","target":"SELF","value":"true"}],
           "effect":{"type":"COIN_FLIP_DAMAGE",
                     "headsCondition":{"type":"DAMAGE_COUNTERS","amount":5,"target":"ATTACKER"}},
           "stackable":false}]}]}
        """;

    private PlayerField attackerField;
    private PlayerField defenderField;
    private ActivePokemon attacker;
    private ActivePokemon defender;
    private Card attackerCard;
    private Card defenderCard;
    private BoardState board;
    private CardLookup cardLookup;
    private Random random;

    @BeforeEach
    void setUp() {
        attacker = ActivePokemon.builder().cardId("atk-1").maxHp(100).currentHp(100)
                .attachedEnergies(new ArrayList<>()).build();
        defender = ActivePokemon.builder().cardId("def-1").maxHp(150).currentHp(150)
                .attachedEnergies(new ArrayList<>()).build();

        attackerField = field(1L, attacker);
        defenderField = field(2L, defender);

        attackerCard = new Card();
        attackerCard.setId("atk-1");
        attackerCard.setName("Attacker");

        defenderCard = new Card();
        defenderCard.setId("def-1");
        defenderCard.setName("Defender");

        board = new BoardState();
        board.setPlayer1Field(attackerField);
        board.setPlayer2Field(defenderField);
        board.setCurrentPlayerId(1L);

        cardLookup = Mockito.mock(CardLookup.class);
        random = Mockito.mock(Random.class);
    }

    private PlayerField field(Long playerId, ActivePokemon active) {
        PlayerField f = new PlayerField();
        f.setPlayerId(playerId);
        f.setActivePokemon(active);
        f.setBench(new ArrayList<>());
        f.setHand(new ArrayList<>());
        f.setDeck(new ArrayList<>());
        f.setDiscardPile(new ArrayList<>());
        f.setPrizeCards(new ArrayList<>(List.of("p1", "p2")));
        f.setTurnFlags(new TurnFlags());
        return f;
    }

    private AttackContext context(int finalDamage, boolean cancelled) {
        return AttackContext.builder()
                .board(board)
                .attackerPlayerId(1L)
                .cardLookup(cardLookup)
                .attackerField(attackerField)
                .defenderField(defenderField)
                .attackerPokemon(attacker)
                .defenderPokemon(defender)
                .attackerCard(attackerCard)
                .defenderCard(defenderCard)
                .attackData(new AttackData())
                .attackCancelled(cancelled)
                .finalDamage(finalDamage)
                .build();
    }

    private PostDamageHandler handler() {
        return new PostDamageHandler(new KnockoutProcessor(), new AbilityTriggerResolver(random));
    }

    // ── Spiky Shield ─────────────────────────────────────────────────────────

    @Test
    void spikyShield_attackerTakes30Recoil() {
        defenderCard.setParsedEffects(SPIKY_SHIELD_JSON);
        defender.setCurrentHp(110); // already damaged by DamageApplicationHandler
        AttackContext ctx = context(40, false);

        handler().handle(ctx);

        assertEquals(70, attacker.getCurrentHp());
        assertTrue(ctx.getEvents().stream().anyMatch(e -> e.getType() == GameEventType.DAMAGE_DEALT));
    }

    @Test
    void spikyShield_firesEvenIfDefenderKnockedOut_andRecoilKoIsProcessed() {
        defenderCard.setParsedEffects(SPIKY_SHIELD_JSON);
        defender.setCurrentHp(0);   // KO'd by this attack
        attacker.setCurrentHp(30);  // recoil will KO the attacker too
        AttackContext ctx = context(60, false);

        handler().handle(ctx);

        // Recoil applied even though the defender is KO'd…
        assertNull(attackerField.getActivePokemon(), "attacker should be KO'd by the recoil");
        // …and both KOs were processed by the existing double KO check
        assertNull(defenderField.getActivePokemon());
        assertTrue(attackerField.getDiscardPile().contains("atk-1"));
        assertTrue(defenderField.getDiscardPile().contains("def-1"));
    }

    @Test
    void spikyShield_doubleKnockout_processesBothDiscardPiles() {
        defenderCard.setParsedEffects(SPIKY_SHIELD_JSON);
        defender.setCurrentHp(0);
        attacker.setCurrentHp(20);
        AttackContext ctx = context(50, false);

        handler().handle(ctx);

        assertNull(attackerField.getActivePokemon());
        assertNull(defenderField.getActivePokemon());
        assertTrue(attackerField.getDiscardPile().contains("atk-1"));
        assertTrue(defenderField.getDiscardPile().contains("def-1"));
    }

    @Test
    void spikyShield_doesNotFire_whenSourceIsNotTheActive() {
        defenderCard.setParsedEffects(SPIKY_SHIELD_JSON);
        // IS_ACTIVE fails: the defender field's Active slot holds a different Pokémon
        defenderField.setActivePokemon(ActivePokemon.builder().cardId("other").maxHp(60)
                .currentHp(60).attachedEnergies(new ArrayList<>()).build());
        AttackContext ctx = context(40, false);

        handler().handle(ctx);

        assertEquals(100, attacker.getCurrentHp());
    }

    @Test
    void spikyShield_doesNotFire_onCancelledAttack() {
        defenderCard.setParsedEffects(SPIKY_SHIELD_JSON);
        AttackContext ctx = context(0, true);

        handler().handle(ctx);

        assertEquals(100, attacker.getCurrentHp());
    }

    // ── Destiny Burst ────────────────────────────────────────────────────────

    @Test
    void destinyBurst_heads_deals50ToAttacker() {
        defenderCard.setParsedEffects(DESTINY_BURST_JSON);
        when(random.nextBoolean()).thenReturn(true);
        defender.setCurrentHp(0);
        AttackContext ctx = context(60, false);

        handler().handle(ctx);

        assertEquals(50, attacker.getCurrentHp());
        assertTrue(ctx.getEvents().stream().anyMatch(e -> e.getType() == GameEventType.COIN_FLIPPED));
    }

    @Test
    void destinyBurst_tails_doesNothing() {
        defenderCard.setParsedEffects(DESTINY_BURST_JSON);
        when(random.nextBoolean()).thenReturn(false);
        defender.setCurrentHp(0);
        AttackContext ctx = context(60, false);

        handler().handle(ctx);

        assertEquals(100, attacker.getCurrentHp());
    }

    @Test
    void destinyBurst_doesNotFire_ifDefenderSurvives() {
        defenderCard.setParsedEffects(DESTINY_BURST_JSON);
        when(random.nextBoolean()).thenReturn(true);
        defender.setCurrentHp(10); // damaged but alive
        AttackContext ctx = context(60, false);

        handler().handle(ctx);

        assertEquals(100, attacker.getCurrentHp());
        assertTrue(ctx.getEvents().stream().noneMatch(e -> e.getType() == GameEventType.COIN_FLIPPED));
    }
}
