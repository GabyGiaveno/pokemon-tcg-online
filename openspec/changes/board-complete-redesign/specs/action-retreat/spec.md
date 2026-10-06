# Action Retreat Specification

## Purpose

Define the RETREAT action flow: selecting a bench Pokémon to become the new active Pokémon, with energy-cost validation and a complete payload dispatch.

## Requirements

### Requirement: Retreat Target Selection

The system MUST open a retreat-selector modal listing the player's bench Pokémon when the player initiates a retreat action, showing each bench Pokémon's retreat cost versus its currently attached energy.

#### Scenario: Player opens the retreat selector

- GIVEN the player has an active Pokémon and at least one bench Pokémon
- WHEN the player clicks the "Retreat" action
- THEN a retreat-selector modal opens listing all bench Pokémon
- AND each entry displays its retreat cost and currently attached energy count

### Requirement: Valid vs Invalid Retreat Targets

The system MUST mark a bench Pokémon as a valid retreat target only if its attached energy count is greater than or equal to its retreat cost, and MUST disable selection of invalid targets.

#### Scenario: Bench Pokémon has insufficient energy

- GIVEN a bench Pokémon's retreat cost is 2 and it has 1 attached energy
- WHEN the retreat-selector modal renders
- THEN that bench Pokémon entry is shown as disabled
- AND the player cannot select it

#### Scenario: Bench Pokémon has sufficient energy

- GIVEN a bench Pokémon's retreat cost is 1 and it has 2 attached energy
- WHEN the retreat-selector modal renders
- THEN that bench Pokémon entry is selectable

### Requirement: Retreat Dispatch Payload

The system MUST dispatch a `RETREAT` action containing only the `benchIndex` of the chosen Pokémon, matching `GameActionApiRequestDto`.

#### Scenario: Player confirms a valid retreat target

- GIVEN the retreat-selector modal is open with valid targets
- WHEN the player clicks a valid bench Pokémon entry
- THEN the system dispatches `{ type: "RETREAT", benchIndex: <selected index> }`
- AND the payload does not include `cardInstanceId` or `targetPosition`

### Requirement: Retreat Feedback

The system MUST show a success toast naming the retreated Pokémon after a successful retreat dispatch, and MUST refresh board state from the backend response.

#### Scenario: Retreat succeeds

- GIVEN the player dispatched a valid RETREAT action
- WHEN the backend confirms the action
- THEN a toast displays "✓ {Pokémon Name} retreated"
- AND the board state updates to reflect the new active Pokémon

### Requirement: Retreat Preconditions

The system MUST NOT allow opening the retreat-selector modal when there is no active Pokémon or when the bench is empty.

#### Scenario: Bench is empty

- GIVEN the player's bench has no Pokémon
- WHEN the player clicks the "Retreat" action
- THEN the retreat-selector modal does not open
- AND no RETREAT action is dispatched
