package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.RestrictEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.Map;

public class RestrictLogic implements EffectLogic<RestrictEffect> {

    @Override
    public boolean isPostDamage() {
        return true;
    }

    @Override
    public void execute(RestrictEffect effect, AttackContext ctx) {
        String restriction = effect.getRestriction();
        if (restriction == null) return;

        String target = effect.getTarget() != null ? effect.getTarget().toUpperCase() : "DEFENDER";

        // OPPONENT target → player-level restriction (e.g. Zoroark Corner — SUPPORTER lock)
        if ("OPPONENT".equals(target)) {
            PlayerField opponentField = ctx.getDefenderField();
            opponentField.getPlayerRestrictions().add(restriction);
            ctx.addEvent(GameEvent.of(
                    GameEventType.POKEMON_RESTRICTED,
                    "Player " + opponentField.getPlayerId() + " cannot use " + restriction + " next turn.",
                    Map.of("playerId", opponentField.getPlayerId(), "restriction", restriction)));
            return;
        }

        // SELF or DEFENDER → Pokémon-level restriction
        ActivePokemon targetPokemon = "SELF".equals(target)
                ? ctx.getAttackerPokemon()
                : ctx.getDefenderPokemon();

        if (targetPokemon == null) return;
        targetPokemon.getRestrictions().add(restriction);
        ctx.addEvent(GameEvent.of(
                GameEventType.POKEMON_RESTRICTED,
                targetPokemon.getCardId() + " cannot use " + restriction + " next turn.",
                Map.of("cardId", targetPokemon.getCardId(), "restriction", restriction)));
    }
}
