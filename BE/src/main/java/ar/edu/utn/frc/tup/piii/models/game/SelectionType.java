package ar.edu.utn.frc.tup.piii.models.game;

/**
 * Kind of player selection the engine is waiting on while a resolution is paused.
 *
 * <p>When the engine reaches a decision point it cannot resolve on its own, it records a
 * {@link PendingSelection} on the {@link BoardState} and returns control. The value here tells
 * {@code resolveSelection} how to interpret the player's answer and how to resume.
 *
 * <ul>
 *   <li>{@code CHOOSE_ACTIVE_ON_KO} – the owner must pick which Benched Pokémon is promoted to
 *       Active after their Active was knocked out.</li>
 * </ul>
 */
public enum SelectionType {
    CHOOSE_ACTIVE_ON_KO,

    /**
     * The player must choose the order of cards peeked from the top of their deck.
     * {@code validOptions} contains the instanceIds of the revealed cards (in current deck order).
     * The player's response must supply those same instanceIds in their chosen order via
     * {@link ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest#getOrderedInstanceIds()}.
     */
    REORDER_DECK,

    /**
     * The player must choose which Benched Pokémon to switch with their Active (forced or optional).
     * {@code validOptions} contains the instanceIds of the benched Pokémon.
     * The player's response supplies the bench index via
     * {@link ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest#getBenchIndex()}.
     */
    SWITCH_POKEMON,

    /**
     * The player must choose cards from their deck to take (search effect).
     * {@code validOptions} contains the instanceIds of matching deck cards revealed to the player.
     * {@code selectionCount} is how many must be chosen.
     * The player's response supplies chosen instanceIds via
     * {@link ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest#getOrderedInstanceIds()}.
     */
    SEARCH_DECK,

    /**
     * The player must choose 1 card from revealed deck options to place directly onto their Bench.
     * {@code validOptions} contains the instanceIds of the matching cards (peek window).
     * The player's response supplies the chosen instanceId via
     * {@link ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest#getOrderedInstanceIds()}.
     */
    PLACE_ON_BENCH,

    /**
     * The player must choose 1 Pokémon from their discard pile to place onto their Bench (Max Revive).
     * {@code validOptions} contains the instanceIds of Pokémon in the discard pile.
     * The player's response supplies the chosen instanceId via
     * {@link ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest#getOrderedInstanceIds()}.
     */
    CHOOSE_FROM_DISCARD,

    /**
     * The player must choose 1 Bench Pokémon to shuffle back into their deck along with all attached cards (Cassius).
     * {@code validOptions} contains the instanceIds of all Bench Pokémon.
     * The player's response supplies the chosen instanceId via
     * {@link ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest#getOrderedInstanceIds()}.
     */
    SHUFFLE_POKEMON_TO_DECK
}
