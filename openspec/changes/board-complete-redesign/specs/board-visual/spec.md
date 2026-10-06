# Board Visual Specification

## Purpose

Define the visual layout, 3D perspective, animations, and responsive behavior of the game board, replacing the previous board and removing the game-log UI.

## Requirements

### Requirement: Hearthstone-Style Table Layout

The system MUST render the opponent's field at the top of the board (read-only) and the player's field at the bottom (interactive), with a 3D table perspective applied via CSS (`rotateX` + `perspective`).

#### Scenario: Board renders with opponent on top and player on bottom

- GIVEN an active game session loaded on the board
- WHEN the board view renders
- THEN the opponent's active/bench/prizes appear in the top half
- AND the player's active/bench/hand/prizes appear in the bottom half
- AND the field container applies a 3D perspective transform

### Requirement: Collapsible Action Drawer

The system MUST provide a right-side drawer containing action controls, toggleable via a button in the top-right corner.

#### Scenario: Player toggles the action drawer

- GIVEN the board is rendered with the drawer collapsed
- WHEN the player clicks the drawer toggle button
- THEN the drawer expands showing available actions
- AND clicking the toggle again collapses the drawer

### Requirement: Fan-Arranged Hand

The system MUST arrange the player's hand cards in a fan/arc layout using per-card transforms based on card position.

#### Scenario: Hand cards render in a fan arrangement

- GIVEN the player has 5 cards in hand
- WHEN the hand zone renders
- THEN each card is rotated/offset according to its index to form an arc
- AND cards visually overlap without obscuring their identifying art

### Requirement: Card Hover Animations

The system MUST apply hover animations to all rendered cards (hand and field zones): scale up to 1.05x, a vertical lift, a 3D tilt effect, and a dynamic glow that follows the cursor position.

#### Scenario: Player hovers over a card

- GIVEN a card is rendered in the hand or a field zone
- WHEN the player moves the mouse over the card
- THEN the card scales to 1.05x and lifts vertically
- AND a 3D tilt effect is applied based on cursor position
- AND a radial glow follows the cursor position over the card
- AND the transition completes within 200ms

#### Scenario: Player moves mouse away from card

- GIVEN a card is in its hovered/animated state
- WHEN the cursor leaves the card boundary
- THEN the card returns to its resting scale, position, and tilt
- AND the glow effect is removed

### Requirement: Game Log Removal

The system MUST NOT render any game-log UI component on the board.

#### Scenario: Board renders without a game log

- GIVEN the board view is loaded
- WHEN the page renders
- THEN no game-log element is present in the DOM
- AND no game-log component is referenced by board-container

### Requirement: Responsive Layout

The board SHOULD render usable layouts at desktop (1920px), tablet (1024px), and mobile (480px) viewport widths.

#### Scenario: Board renders at tablet width

- GIVEN the viewport width is 1024px
- WHEN the board renders
- THEN all zones (opponent field, player field, hand, drawer) remain visible and non-overlapping
- AND interactive elements remain clickable

#### Scenario: Board renders at mobile width

- GIVEN the viewport width is 480px
- WHEN the board renders
- THEN the layout adapts (e.g., stacked or scaled zones) without horizontal overflow
- AND core actions remain accessible
