# GAME_RULES.md

## Overview

This document defines the authoritative gameplay rules for the Pokémon TCG digital engine implementation.

These rules are considered canonical for engine development.

ALL validators, resolvers, effects, and state transitions MUST follow this specification.

If implementation behavior differs from this document, the implementation is considered incorrect unless explicitly overridden by expansion-specific rules.

This specification currently targets:
- Pokémon TCG XY1 format
- 1v1 matches
- standard prize system
- deterministic gameplay

---

# Core Gameplay Model

A match consists of:
- 2 players
- individual decks
- prize cards
- active Pokémon
- bench Pokémon
- turn-based actions
- deterministic rule resolution

The backend Game Engine is authoritative.

---

# Match Initialization Rules

## Deck Requirements

A valid deck MUST:
- contain exactly 60 cards
- contain at least 1 Basic Pokémon
- respect card copy limits
- be validated before match creation

Maximum copies:
- maximum 4 copies per card name
- basic Energy cards are exempt

---

## Match Start Procedure

Initial flow:

```text
1. shuffle decks
2. draw 7 cards
3. validate Basic Pokémon availability
4. resolve mulligans
5. place active Pokémon
6. place bench Pokémon
7. place prize cards
8. determine first player
9. begin match
```

---

# Mulligan Rules

A mulligan occurs when:
- a player has no Basic Pokémon in opening hand

When mulligan occurs:
- deck is reshuffled
- player redraws 7 cards
- opponent may draw 1 extra card per mulligan

The match cannot begin until:
- both players have at least 1 Basic Pokémon

---

# Prize Rules

Each player starts with:
- 6 prize cards

Prize cards are taken when:
- opponent Pokémon is Knocked Out

Prize acquisition:
- normal Pokémon → 1 prize
- EX Pokémon → 2 prizes

Victory condition:
- taking all prize cards

---

# Turn System

The game operates through strict turn phases.

## Turn Phases

```text
DRAW
MAIN
ATTACK
BETWEEN_TURNS
```

---

# DRAW Phase

At start of turn:
- active player draws 1 card

If player cannot draw:
- player immediately loses

DRAW phase transitions automatically into MAIN phase.

---

# MAIN Phase

The active player MAY:
- play Basic Pokémon
- evolve Pokémon
- attach 1 manual energy
- play Trainer cards
- retreat active Pokémon
- activate abilities
- attack
- end turn

The player MAY perform actions in arbitrary order unless explicitly restricted.

---

# Energy Rules

## Manual Energy Attachment

A player MAY:
- attach exactly 1 manual energy per turn

Restrictions:
- cannot exceed 1 manual attachment
- attachment target must be owned Pokémon
- energy must exist in hand

Special energy effects MAY override limits.

---

## Attached Energy

Energy remains attached until:
- Pokémon leaves play
- card effect removes energy
- energy is discarded

When Pokémon leaves play:
- all attached cards are discarded unless effect overrides

---

# Evolution Rules

## Evolution Constraints

A Pokémon CANNOT evolve:
- during the same turn it entered play
- more than once during same turn
- if affected by explicit blocking effects

Evolution chain MUST remain valid.

Example:

```text
Basic → Stage 1 → Stage 2
```

Invalid chains MUST be rejected.

---

## Evolution Effects

On evolution:
- damage counters remain
- attached cards remain
- status conditions are removed unless specified otherwise

---

# Bench Rules

Bench capacity:
- maximum 5 Pokémon

Players MAY:
- place Basic Pokémon onto bench during MAIN phase

Players CANNOT:
- place evolved Pokémon directly onto bench

---

# Retreat Rules

A player MAY retreat:
- once per turn

Requirements:
- active Pokémon has sufficient attached energy
- retreat cost is payable

Retreat process:

```text
1. validate retreat availability
2. discard retreat cost energies
3. select bench replacement
4. move active Pokémon to bench
5. move selected Pokémon to active
```

Retreating ends:
- poison immunity bypass
- paralysis restriction checks

Retreating does NOT:
- remove damage

---

# Attack Rules

A player MAY attack:
- once per turn
- only during ATTACK phase
- only with active Pokémon

Restrictions:
- sufficient energy required
- attack restrictions respected
- status effects respected

Attacking immediately ends the turn unless overridden.

---

# Attack Resolution Pipeline

Canonical attack flow:

```text
1. validate attacker
2. validate energy requirements
3. validate targeting
4. apply pre-attack restrictions
5. resolve confusion check
6. apply modifiers
7. calculate weakness
8. calculate resistance
9. apply damage
10. resolve post-damage effects
11. resolve knockouts
12. distribute prizes
13. trigger between-turn effects
14. end turn
```

ALL attack implementations MUST follow this order.

---

# Damage Rules

Damage is applied as:
- damage counters
- 10 damage increments

Example:

```text
50 damage = 5 counters
```

Damage remains until:
- healed
- Pokémon leaves play

Damage cannot reduce below:
- 0 HP

---

# Weakness Rules

Weakness:
- multiplies incoming damage

Standard multiplier:
- ×2

Weakness applies:
- before resistance

---

# Resistance Rules

Resistance:
- subtracts fixed damage

Standard reduction:
- -20

Resistance applies:
- after weakness

Damage cannot become negative.

---

# Knockout Rules

A Pokémon is Knocked Out when:
- accumulated damage >= current HP

Knockout process:

```text
1. discard Pokémon
2. discard attached cards
3. trigger knockout events
4. opponent takes prizes
5. owner selects replacement active
6. validate board state
```

---

# Status Conditions

Supported status conditions:
- Poison
- Burn
- Sleep
- Paralysis
- Confusion

A Pokémon MAY have:
- only 1 major status at a time unless rules override

---

# Poison Rules

Poison:
- applies damage between turns

Standard poison:
- 10 damage between turns

Poison persists until:
- evolution
- retreat
- effect removal
- leaving play

---

# Burn Rules

Burn:
- applies burn damage between turns
- may be removable via coin flip

Exact burn behavior MUST remain deterministic in implementation.

---

# Sleep Rules

Sleeping Pokémon:
- cannot attack
- cannot retreat

Between turns:
- sleep recovery check occurs

---

# Paralysis Rules

Paralyzed Pokémon:
- cannot attack
- cannot retreat

Paralysis expires:
- at end of owner's next turn

---

# Confusion Rules

When confused Pokémon attacks:

```text
1. perform confusion check
2. if failed:
   - attack fails
   - self-damage applied
   - turn ends
```

---

# Trainer Rules

Trainer cards are divided into:
- Item
- Supporter
- Stadium

---

## Supporter Rules

A player MAY play:
- exactly 1 Supporter per turn

---

## Stadium Rules

Only:
- 1 Stadium may exist globally

Playing new Stadium:
- replaces previous Stadium

---

# Ability Rules

Abilities MAY:
- be passive
- activated
- triggered

Abilities MUST:
- respect blocking conditions
- respect timing windows

Abilities are disabled if:
- Pokémon is not in play unless specified otherwise

---

# Between Turns Phase

Occurs after attack resolution.

Canonical order:

```text
1. poison damage
2. burn damage
3. sleep checks
4. paralysis expiration
5. knockout checks
```

---

# Victory Conditions

A player immediately wins if:
- opponent has no remaining prize cards
- opponent cannot draw card at turn start
- opponent has no valid active Pokémon

Victory checks MUST occur:
- after every state mutation
- after every knockout
- after between-turn effects

---

# Illegal Actions

Illegal actions MUST:
- be rejected
- produce no state mutation

Examples:
- attacking without energy
- evolving invalid target
- attaching second manual energy
- retreating without cost

Illegal actions MUST throw:
- rule validation exceptions

---

# Deterministic Constraints

The engine MUST remain deterministic.

Forbidden:
- hidden frontend calculations
- timing-based logic
- asynchronous resolution order
- random external dependencies

ALL randomness MUST:
- be engine-controlled
- seedable if necessary
- reproducible for tests

---

# Event Requirements

Every major gameplay mutation MUST emit events.

Examples:
- attack executed
- damage applied
- Pokémon evolved
- prize taken
- turn ended

Events are mandatory for:
- websocket sync
- replay systems
- audit logs
- spectators
- debugging

---

# Engine Authority

The frontend MUST NEVER:
- calculate damage
- validate gameplay rules
- mutate authoritative state
- resolve effects

The backend Game Engine remains the sole source of truth.