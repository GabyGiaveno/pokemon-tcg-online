# Action Evolve Specification

## Purpose

Define the EVOLVE_POKEMON action flow: selecting an evolution card from hand and a valid evolution target on the field, confirming, and dispatching a complete payload.

## Requirements

### Requirement: Evolution Card Selection Highlights Valid Targets

The system MUST, when the player clicks an evolution card in hand, highlight field Pokémon (active and bench) that match the evolution's required previous stage and were not played this turn as valid targets, and MUST gray out (disable) Pokémon that do not match the stage or entered play this turn.

#### Scenario: Player selects a valid evolution card

- GIVEN the player has an evolution card in hand matching the stage of their active Pokémon
- AND the active Pokémon did not enter play this turn
- WHEN the player clicks the evolution card
- THEN the active Pokémon is highlighted as a valid target

#### Scenario: Target entered play this turn

- GIVEN a bench Pokémon matches the evolution's required stage
- AND that bench Pokémon was played this turn
- WHEN the player selects the evolution card
- THEN that bench Pokémon is shown grayed out and is not selectable

### Requirement: Evolve Confirm Modal

The system MUST open an evolve-confirm modal when the player clicks a valid evolution target, showing the current Pokémon's name and the evolution's name, with a CONFIRM control.

#### Scenario: Player clicks a valid evolution target

- GIVEN an evolution card is selected and a valid target is highlighted
- WHEN the player clicks the highlighted target
- THEN an evolve-confirm modal opens showing "Evolve {Current} into {Evolution}?"
- AND a CONFIRM button is present

### Requirement: Evolve Dispatch Payload

The system MUST dispatch an `EVOLVE_POKEMON` action containing `cardInstanceId` (the evolution card) and `targetPosition` (`"ACTIVE"` or `"BENCH_<index>"`), matching `GameActionApiRequestDto`.

#### Scenario: Player confirms evolving the active Pokémon

- GIVEN the evolve-confirm modal is open with the active Pokémon as target
- WHEN the player clicks CONFIRM
- THEN the system dispatches `{ type: "EVOLVE_POKEMON", cardInstanceId: "<evolution card id>", targetPosition: "ACTIVE" }`

#### Scenario: Player confirms evolving a bench Pokémon

- GIVEN the evolve-confirm modal is open with bench slot 2 as target
- WHEN the player clicks CONFIRM
- THEN the system dispatches `{ type: "EVOLVE_POKEMON", cardInstanceId: "<evolution card id>", targetPosition: "BENCH_2" }`

### Requirement: Evolve Feedback

The system MUST show a success toast naming the resulting evolution after a successful dispatch, and MUST refresh board state from the backend response.

#### Scenario: Evolve succeeds

- GIVEN the player confirmed a valid EVOLVE_POKEMON action
- WHEN the backend confirms the action
- THEN a toast displays "✓ {Pokémon} evolved into {Evolution}"
- AND the board state updates to show the evolved Pokémon in place of the previous stage
