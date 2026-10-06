package ar.edu.utn.frc.tup.piii.engine.effects.logics;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.effects.EffectLogic;
import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.effects.SwitchPokemonEffect;
import ar.edu.utn.frc.tup.piii.models.game.PendingSelection;
import ar.edu.utn.frc.tup.piii.models.game.PlayerField;
import ar.edu.utn.frc.tup.piii.models.game.SelectionType;

import java.util.List;
import java.util.Map;

public class SwitchPokemonLogic implements EffectLogic<SwitchPokemonEffect> {

    @Override
    public boolean isPostDamage() {
        return true;
    }

    @Override
    public void execute(SwitchPokemonEffect effect, AttackContext ctx) {
        boolean targetsSelf = "SELF".equalsIgnoreCase(effect.getTarget());
        PlayerField targetField = targetsSelf ? ctx.getAttackerField() : ctx.getDefenderField();

        if (targetField == null || targetField.getBench() == null || targetField.getBench().isEmpty()) {
            return; // no bench to switch into — no-op
        }

        List<String> benchInstanceIds = targetField.getBench().stream()
                .map(b -> b.getInstanceId() != null ? b.getInstanceId() : b.getCardId())
                .toList();

        // Only one PendingSelection can exist at a time; if one is already set (e.g. Rapid Spin's
        // dual-switch), skip this second one — the first switch takes priority.
        if (ctx.getBoard().getPendingSelection() != null) {
            return;
        }

        ctx.getBoard().setPendingSelection(PendingSelection.builder()
                .type(SelectionType.SWITCH_POKEMON)
                .ownerPlayerId(targetField.getPlayerId())
                .validOptions(benchInstanceIds)
                .prompt("Choose a Benched Pokémon to switch with your Active Pokémon.")
                .build());

        ctx.addEvent(GameEvent.of(
                GameEventType.PHASE_CHANGED,
                "Player " + targetField.getPlayerId() + " must switch their Active Pokémon.",
                Map.of("playerId", targetField.getPlayerId())));
    }
}
