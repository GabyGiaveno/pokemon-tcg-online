package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.DiscardEnergyTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;
import ar.edu.utn.frc.tup.piii.models.game.AttachedCard;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Team Flare Grunt: discard {@code amount} energy from the opponent's Active Pokémon. */
public class DiscardEnergyTrainerLogic implements TrainerEffectLogic<DiscardEnergyTrainerEffect> {

    @Override
    public List<GameEvent> execute(DiscardEnergyTrainerEffect effectData, TrainerContext ctx) {
        BoardState board = ctx.getBoard();
        Long playerId = ctx.getPlayerId();
        List<GameEvent> events = new ArrayList<>();
        PlayerField self = board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer1Field()
                : board.getPlayer2Field();
        PlayerField opponent = self == board.getPlayer1Field()
                ? board.getPlayer2Field()
                : board.getPlayer1Field();

        boolean targetsSelf = "SELF_ACTIVE".equalsIgnoreCase(effectData.getTarget());
        ActivePokemon active = targetsSelf
                ? self.getActivePokemon()
                : opponent.getActivePokemon();
        PlayerField activeOwner = targetsSelf ? self : opponent;
        if (active == null || active.getAttachedEnergies() == null || active.getAttachedEnergies().isEmpty()) {
            return events;
        }

        List<AttachedCard> energies = active.getAttachedEnergies();
        int toDiscard = effectData.getAmount() < 0
                ? energies.size()
                : Math.min(effectData.getAmount(), energies.size());

        for (int i = 0; i < toDiscard; i++) {
            AttachedCard removed = energies.remove(energies.size() - 1);
            if (removed != null && removed.getCardId() != null) {
                activeOwner.getDiscardPile().add(removed.getCardId());
            }
        }

        events.add(GameEvent.of(GameEventType.ENERGY_DISCARDED,
                "Discarded " + toDiscard + " energy from " + active.getCardId() + ".",
                Map.of("cardId", active.getCardId(), "amount", toDiscard)));
        return events;
    }
}
