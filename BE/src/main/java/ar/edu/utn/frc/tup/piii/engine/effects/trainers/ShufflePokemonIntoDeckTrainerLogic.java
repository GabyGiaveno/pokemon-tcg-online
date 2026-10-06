package ar.edu.utn.frc.tup.piii.engine.effects.trainers;

import ar.edu.utn.frc.tup.piii.engine.effects.trainers.chain.TrainerContext;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.ShufflePokemonIntoDeckTrainerEffect;
import ar.edu.utn.frc.tup.piii.models.game.BenchPokemon;
import ar.edu.utn.frc.tup.piii.models.game.BoardState;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;

import java.util.List;
import java.util.Map;

public class ShufflePokemonIntoDeckTrainerLogic implements TrainerEffectLogic<ShufflePokemonIntoDeckTrainerEffect> {

    @Override
    public List<GameEvent> execute(ShufflePokemonIntoDeckTrainerEffect effectData, TrainerContext ctx) {
        BoardState board = ctx.getBoard();
        Long playerId = ctx.getPlayerId();

        PlayerField actorField = board.getPlayer1Field().getPlayerId().equals(playerId)
                ? board.getPlayer1Field()
                : board.getPlayer2Field();

        List<BenchPokemon> bench = actorField.getBench();
        if (bench == null || bench.isEmpty()) {
            return List.of();
        }

        List<String> benchInstanceIds = bench.stream()
                .map(BenchPokemon::getInstanceId)
                .toList();

        board.setPendingSelection(PendingSelection.builder()
                .type(SelectionType.SHUFFLE_POKEMON_TO_DECK)
                .ownerPlayerId(playerId)
                .validOptions(benchInstanceIds)
                .prompt("Choose a Bench Pokémon to shuffle into your deck")
                .selectionCount(1)
                .build());

        return List.of(GameEvent.of(
                GameEventType.DECK_SEARCHED,
                "Player " + playerId + " must choose a Bench Pokémon to shuffle into their deck.",
                Map.of("playerId", playerId)));
    }
}
