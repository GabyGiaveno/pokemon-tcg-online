package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.HealTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Heals {@code amount} HP from a target Pokémon. Target: "ACTIVE" (own Active). */
public class HealTrainerLogic implements TrainerEffectLogic<HealTrainerEffect> {

    @Override
    public List<GameEvent> execute(HealTrainerEffect effect, TrainerContext ctx) {
        BoardState board = ctx.getBoard();
        Long playerId = ctx.getPlayerId();
        List<GameEvent> events = new ArrayList<>();

        PlayerField self = board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer1Field()
                : board.getPlayer2Field();

        // Currently supports healing own Active only.
        ActivePokemon active = self.getActivePokemon();
        if (active == null) return events;

        int healed = Math.min(effect.getAmount(), active.getMaxHp() - active.getCurrentHp());
        if (healed <= 0) return events;

        active.setCurrentHp(active.getCurrentHp() + healed);
        events.add(GameEvent.of(
                GameEventType.POKEMON_HEALED,
                active.getCardId() + " healed " + healed + " HP. HP now: " + active.getCurrentHp() + ".",
                Map.of("cardId", active.getCardId(), "healed", healed, "currentHp", active.getCurrentHp())));
        return events;
    }
}
