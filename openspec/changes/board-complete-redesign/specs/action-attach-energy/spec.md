# Action Attach Energy Specification

## Purpose

Define the ATTACH_ENERGY action flow: selecting an energy card from hand and a target Pokémon on the field, confirming, and dispatching a complete payload.

## Requirements

### Requirement: Energy Card Selection Highlights Valid Targets

The system MUST, when the player clicks an energy card in hand, apply a glow/selection effect to that card and highlight all valid attach targets (active and bench Pokémon) on the player's field.

#### Scenario: Player selects an energy card

- GIVEN the player has an energy card in hand and an active Pokémon plus bench Pokémon on the field
- WHEN the player clicks the energy card
- THEN the energy card displays a selection glow
- AND the active Pokémon and each bench Pokémon are highlighted as valid targets

### Requirement: Energy Confirm Modal

The system MUST open an energy-confirm modal when the player clicks a highlighted target Pokémon, showing the energy card name and the target Pokémon name, with CONFIRM and CANCEL controls.

#### Scenario: Player clicks a highlighted target

- GIVEN an energy card is selected and targets are highlighted
- WHEN the player clicks a highlighted Pokémon (active or bench)
- THEN an energy-confirm modal opens showing "Attach {Energy} to {Target}?"
- AND CONFIRM and CANCEL buttons are present

#### Scenario: Player cancels the energy attach

- GIVEN the energy-confirm modal is open
- WHEN the player clicks CANCEL
- THEN the modal closes
- AND no action is dispatched
- AND the energy card selection and target highlights are cleared

### Requirement: Attach Energy Dispatch Payload

The system MUST dispatch an `ATTACH_ENERGY` action containing `cardInstanceId` (the energy card) and `targetPosition` (`"ACTIVE"` or `"BENCH_<index>"`), matching `GameActionApiRequestDto`.

#### Scenario: Player confirms attaching energy to the active Pokémon

- GIVEN the energy-confirm modal is open with the active Pokémon as target
- WHEN the player clicks CONFIRM
- THEN the system dispatches `{ type: "ATTACH_ENERGY", cardInstanceId: "<energy card id>", targetPosition: "ACTIVE" }`

#### Scenario: Player confirms attaching energy to a bench Pokémon

- GIVEN the energy-confirm modal is open with bench slot 0 as target
- WHEN the player clicks CONFIRM
- THEN the system dispatches `{ type: "ATTACH_ENERGY", cardInstanceId: "<energy card id>", targetPosition: "BENCH_0" }`

### Requirement: Attach Energy Feedback

The system MUST show a success toast naming the attached energy and target Pokémon after a successful dispatch, and MUST refresh board state from the backend response.

#### Scenario: Attach energy succeeds

- GIVEN the player confirmed a valid ATTACH_ENERGY action
- WHEN the backend confirms the action
- THEN a toast displays "✓ {Energy Name} attached to {Pokémon}"
- AND the board state updates to show the new attached energy on the target
