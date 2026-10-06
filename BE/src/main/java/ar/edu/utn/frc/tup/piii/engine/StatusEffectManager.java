package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.events.GameEvent;
import ar.edu.utn.frc.tup.piii.events.GameEventType;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.game.ActivePokemon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Processes special conditions on the Active Pokémon at the end of each turn (BETWEEN_TURNS phase).
 *
 * <p>Execution order (ENGINE_SPEC §4):
 * <ol>
 *   <li>POISONED – 10 damage (1 counter), no flip.</li>
 *   <li>BURNED – flip coin; heads = burn cured, tails = 20 damage (2 counters).</li>
 *   <li>ASLEEP – flip coin; heads = wake up.</li>
 *   <li>PARALYZED – cured automatically.</li>
 * </ol>
 *
 * <p>CONFUSED is handled before an attack (inside the attack-resolution pipeline), not here.
 */
public class StatusEffectManager {

    private final Random random;

    /** Default constructor: uses a standard {@link Random}. */
    public StatusEffectManager() {
        this.random = new Random();
    }

    /** Constructor for testing: inject a deterministic {@link Random} or mock. */
    public StatusEffectManager(Random random) {
        this.random = random;
    }

    // -------------------------------------------------------------------------
    // Between-turns processing
    // -------------------------------------------------------------------------

    /**
     * Applies between-turn status-effect ticks to the given Active Pokémon.
     * Delegates to {@link #applyBetweenTurnEffects(ActivePokemon, boolean)} with
     * {@code isOwnerTurn = true} (backward-compatible default).
     */
    public List<GameEvent> applyBetweenTurnEffects(ActivePokemon pokemon) {
        return applyBetweenTurnEffects(pokemon, true);
    }

    /**
     * Applies between-turn status-effect ticks to the given Active Pokémon.
     *
     * <p>TCG rules distinguish two categories:
     * <ul>
     *   <li><b>Always apply</b> (every turn boundary): POISONED tick, BURNED flip.</li>
     *   <li><b>Owner's-turn only</b>: ASLEEP wake check, PARALYZED auto-cure.
     *       These only resolve at the end of the <em>afflicted</em> Pokémon's
     *       controller's turn, not at the end of the opponent's turn.</li>
     * </ul>
     *
     * @param pokemon      the Active Pokémon to process (null-safe – returns empty list)
     * @param isOwnerTurn  {@code true} when this is the afflicted Pokémon's controller's
     *                     turn ending; {@code false} when it is the opponent's turn ending
     * @return ordered list of events produced
     */
    public List<GameEvent> applyBetweenTurnEffects(ActivePokemon pokemon, boolean isOwnerTurn) {
        List<GameEvent> events = new ArrayList<>();
        if (pokemon == null) return events;

        // 1. POISONED: 10 damage every turn boundary
        if (pokemon.isPoisoned()) {
            applyDamage(pokemon, 10);
            events.add(GameEvent.of(GameEventType.DAMAGE_DEALT,
                    "Poison dealt 10 damage.",
                    Map.of("cardId", pokemon.getCardId(), "damage", 10, "source", "POISON")));
        }

        // 2. BURNED: flip coin every turn boundary; heads = cured, tails = 20 damage
        if (pokemon.isBurned()) {
            boolean heads = random.nextBoolean();
            if (heads) {
                pokemon.setBurned(false);
                events.add(GameEvent.of(GameEventType.STATUS_EFFECT_CLEARED,
                        "Burn healed (heads).",
                        Map.of("cardId", pokemon.getCardId(), "condition", "BURNED")));
            } else {
                applyDamage(pokemon, 20);
                events.add(GameEvent.of(GameEventType.DAMAGE_DEALT,
                        "Burn dealt 20 damage (tails).",
                        Map.of("cardId", pokemon.getCardId(), "damage", 20, "source", "BURN")));
            }
        }

        // 3 & 4 only apply at the end of the afflicted Pokémon's controller's turn
        if (!isOwnerTurn) return events;

        // 3. ASLEEP: flip coin at end of owner's turn; heads = wake up
        if (pokemon.getCondition() == SpecialCondition.ASLEEP) {
            boolean heads = random.nextBoolean();
            if (heads) {
                pokemon.setCondition(SpecialCondition.NONE);
                events.add(GameEvent.of(GameEventType.STATUS_EFFECT_CLEARED,
                        "Woke up (heads).",
                        Map.of("cardId", pokemon.getCardId(), "condition", "ASLEEP")));
            } else {
                events.add(GameEvent.of(GameEventType.STATUS_EFFECT_APPLIED,
                        "Still Asleep (tails).",
                        Map.of("cardId", pokemon.getCardId(), "condition", "ASLEEP")));
            }
        }

        // 4. PARALYZED: cures automatically at end of owner's turn
        if (pokemon.getCondition() == SpecialCondition.PARALYZED) {
            pokemon.setCondition(SpecialCondition.NONE);
            events.add(GameEvent.of(GameEventType.STATUS_EFFECT_CLEARED,
                    "Paralysis cleared.",
                    Map.of("cardId", pokemon.getCardId(), "condition", "PARALYZED")));
        }

        return events;
    }

    // -------------------------------------------------------------------------
    // Condition application (called by attack effects / trainer effects)
    // -------------------------------------------------------------------------

    /**
     * Applies a condition respecting exclusivity rules.
     * ASLEEP/CONFUSED/PARALYZED are mutually exclusive (newest wins).
     * BURNED/POISONED are independent booleans.
     */
    public void applyCondition(ActivePokemon pokemon, SpecialCondition condition) {
        switch (condition) {
            case ASLEEP, CONFUSED, PARALYZED -> pokemon.setCondition(condition);
            case BURNED   -> pokemon.setBurned(true);
            case POISONED -> pokemon.setPoisoned(true);
            case NONE     -> pokemon.setCondition(SpecialCondition.NONE);
        }
    }

    /**
     * Clears ALL conditions. Called on evolve or retreat.
     */
    public void clearAllConditions(ActivePokemon pokemon) {
        pokemon.setCondition(SpecialCondition.NONE);
        pokemon.setBurned(false);
        pokemon.setPoisoned(false);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void applyDamage(ActivePokemon pokemon, int damage) {
        pokemon.setCurrentHp(Math.max(0, pokemon.getCurrentHp() - damage));
    }
}
