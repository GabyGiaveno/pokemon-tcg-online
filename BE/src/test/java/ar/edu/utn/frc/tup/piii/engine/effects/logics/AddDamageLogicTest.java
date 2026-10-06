package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.CardLookup;
import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.models.cards.effects.AddDamageEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class AddDamageLogicTest {

    private AddDamageLogic logic;
    private AttackContext ctx;
    private CardLookup cardLookup;

    @BeforeEach
    void setUp() {
        logic = new AddDamageLogic();
        cardLookup = Mockito.mock(CardLookup.class);
        ctx = new AttackContext();
        ctx.setCardLookup(cardLookup);
        ctx.setDamageModifiers(0);
        
        ActivePokemon defender = new ActivePokemon();
        defender.setMaxHp(100);
        defender.setCurrentHp(100);
        ctx.setDefenderPokemon(defender);
    }

    @Test
    void execute_noCondition_addsDamage() {
        AddDamageEffect effect = new AddDamageEffect();
        effect.setAmount(20);
        
        logic.execute(effect, ctx);
        
        assertEquals(20, ctx.getDamageModifiers());
    }

    @Test
    void execute_opponentIsGrassCondition_met_addsDamage() {
        AddDamageEffect effect = new AddDamageEffect();
        effect.setAmount(30);
        effect.setCondition("OPPONENT_IS_GRASS");

        Card defenderCard = new Card();
        defenderCard.setTypes(List.of("Grass", "Poison"));
        ctx.setDefenderCard(defenderCard);

        logic.execute(effect, ctx);
        
        assertEquals(30, ctx.getDamageModifiers());
    }

    @Test
    void execute_opponentIsGrassCondition_notMet_doesNotAddDamage() {
        AddDamageEffect effect = new AddDamageEffect();
        effect.setAmount(30);
        effect.setCondition("OPPONENT_IS_GRASS");

        Card defenderCard = new Card();
        defenderCard.setTypes(List.of("Fire"));
        ctx.setDefenderCard(defenderCard);

        logic.execute(effect, ctx);
        
        assertEquals(0, ctx.getDamageModifiers());
    }

    @Test
    void execute_defenderIsExCondition_met_addsDamage() {
        AddDamageEffect effect = new AddDamageEffect();
        effect.setAmount(50);
        effect.setCondition("DEFENDER_IS_EX");

        Card defenderCard = new Card();
        defenderCard.setSubtypes(List.of("Basic", "EX"));
        ctx.setDefenderCard(defenderCard);

        logic.execute(effect, ctx);
        
        assertEquals(50, ctx.getDamageModifiers());
    }
}
