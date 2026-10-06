# Action Play Trainer Specification

## Purpose

Define the trainer-card play flow (Item/Supporter/Stadium): routing to the correct action type by subtype and dispatching a complete payload without unnecessary modals for non-targeted trainers.

## Requirements

### Requirement: Trainer Subtype Routing

The system MUST determine the trainer card's subtype (Item, Supporter, or Stadium) and dispatch the corresponding action type: `PLAY_ITEM`, `PLAY_SUPPORTER`, or `PLAY_STADIUM`.

#### Scenario: Player plays an Item card

- GIVEN the player has a trainer card with subtype Item in hand
- WHEN the player clicks the card to play it
- THEN the system dispatches `{ type: "PLAY_ITEM", cardInstanceId: "<card id>" }`

#### Scenario: Player plays a Supporter card

- GIVEN the player has a trainer card with subtype Supporter in hand
- WHEN the player clicks the card to play it
- THEN the system dispatches `{ type: "PLAY_SUPPORTER", cardInstanceId: "<card id>" }`

#### Scenario: Player plays a Stadium card

- GIVEN the player has a trainer card with subtype Stadium in hand
- WHEN the player clicks the card to play it
- THEN the system dispatches `{ type: "PLAY_STADIUM", cardInstanceId: "<card id>" }`

### Requirement: Immediate Dispatch for Non-Targeted Trainers

The system MUST dispatch the trainer action immediately upon click, without opening a confirmation or targeting modal, for trainer cards that do not require target selection.

#### Scenario: Player plays a trainer with no target requirement

- GIVEN a trainer card in hand requires no target selection
- WHEN the player clicks the card
- THEN the system dispatches the corresponding `PLAY_*` action immediately
- AND no modal is opened

### Requirement: Trainer Play Feedback

The system MUST show a success toast naming the trainer card after a successful dispatch, and MUST refresh board state from the backend response.

#### Scenario: Trainer play succeeds

- GIVEN the player dispatched a valid PLAY_ITEM, PLAY_SUPPORTER, or PLAY_STADIUM action
- WHEN the backend confirms the action
- THEN a toast displays "✓ {Trainer Name} played"
- AND the board state updates (card removed from hand, effects applied)

### Requirement: Payload Contract Compliance

The system MUST send only `cardInstanceId` in the payload for PLAY_ITEM, PLAY_SUPPORTER, and PLAY_STADIUM action types, with no additional fields.

#### Scenario: Trainer payload omits extra fields

- GIVEN a trainer card play is dispatched
- WHEN the payload is constructed
- THEN it contains exactly `type` and `cardInstanceId`
- AND it does not contain `targetPosition` or `benchIndex`
