#!/usr/bin/env python3
"""
Genera xy1_minimal.json con SOLO id, name, attacks (name, text, parsedEffects),
abilities (name, text, parsedEffects).

El mapping texto→parsedEffects está definido por el LLM (Claude) basado en
comprensión semántica de cada texto de habilidad/ataque según las reglas
de Pokémon TCG y el catálogo de PARSING_METHOD.md + tipos extendidos necesarios.
"""

import json
import re
from pathlib import Path

# ═════════════════════════════════════════════════════════════════════════════
#  MAPEO SEMÁNTICO: cada texto único → sus parsedEffects
#  Definido por Claude (LLM) con comprensión real del texto y reglas TCG
# ═════════════════════════════════════════════════════════════════════════════

TEXT_TO_EFFECTS = {
    # ── GRUPO 1: APPLY_CONDITION puro ──────────────────────────────────
    "Your opponent's Active Pokémon is now Poisoned.": [
        {"type": "APPLY_CONDITION", "condition": "POISONED", "target": "DEFENDER"}
    ],
    "Your opponent's Active Pokémon is now Confused.": [
        {"type": "APPLY_CONDITION", "condition": "CONFUSED", "target": "DEFENDER"}
    ],
    "Your opponent's Active Pokémon is now Asleep.": [
        {"type": "APPLY_CONDITION", "condition": "ASLEEP", "target": "DEFENDER"}
    ],
    "Your opponent's Active Pokémon is now Paralyzed and Poisoned.": [
        {"type": "APPLY_CONDITION", "condition": "PARALYZED", "target": "DEFENDER"},
        {"type": "APPLY_CONDITION", "condition": "POISONED", "target": "DEFENDER"}
    ],
    "Both Active Pokémon are now Confused.": [
        {"type": "APPLY_CONDITION", "condition": "CONFUSED", "target": "DEFENDER"},
        {"type": "APPLY_CONDITION", "condition": "CONFUSED", "target": "SELF"}
    ],

    # ── GRUPO 2: Poisón + Restricción ─────────────────────────────────
    "Your opponent's Active Pokémon is now Poisoned. That Pokémon can't retreat during your opponent's next turn.": [
        {"type": "APPLY_CONDITION", "condition": "POISONED", "target": "DEFENDER"},
        {"type": "RESTRICT", "restriction": "RETREAT", "target": "DEFENDER", "duration": "NEXT_TURN"}
    ],

    # ── GRUPO 3: HEAL ─────────────────────────────────────────────────
    "Heal 10 damage from this Pokémon.": [
        {"type": "HEAL", "amount": 10, "target": "SELF"}
    ],
    "Heal 20 damage from this Pokémon.": [
        {"type": "HEAL", "amount": 20, "target": "SELF"}
    ],
    "Heal 30 damage from this Pokémon.": [
        {"type": "HEAL", "amount": 30, "target": "SELF"}
    ],
    "Heal 30 damage and remove all Special Conditions from this Pokémon.": [
        {"type": "HEAL", "amount": 30, "target": "SELF"},
        {"type": "REMOVE_CONDITIONS", "target": "SELF"}
    ],
    "Heal 60 damage from 1 of your Benched Pokémon.": [
        {"type": "HEAL", "amount": 60, "target": "SELF_BENCH"}
    ],
    "Heal 20 damage from 1 of your Pokémon.": [
        {"type": "HEAL", "amount": 20, "target": "SELF"}
    ],
    "Heal 10 damage from each of your Pokémon.": [
        {"type": "HEAL", "amount": 10, "target": "ALL_SELF"}
    ],

    # ── GRUPO 4: DISCARD_ENERGY ──────────────────────────────────────
    "Discard an Energy attached to this Pokémon.": [
        {"type": "DISCARD_ENERGY", "amount": 1, "target": "SELF"}
    ],
    "Discard an Energy attached to this Pokémon and heal all damage from it.": [
        {"type": "DISCARD_ENERGY", "amount": 1, "target": "SELF"},
        {"type": "HEAL", "amount": -1, "target": "SELF"}
    ],
    "Discard a Fire Energy attached to this Pokémon.": [
        {"type": "DISCARD_ENERGY", "amount": 1, "target": "SELF"}
    ],
    "Discard all Energy attached to this Pokémon.": [
        {"type": "DISCARD_ENERGY", "amount": -1, "target": "SELF"}
    ],
    "Discard all Fire Energy attached to this Pokémon.": [
        {"type": "DISCARD_ENERGY", "amount": -1, "target": "SELF"}
    ],
    "Discard an Energy attached to your opponent's Active Pokémon.": [
        {"type": "DISCARD_ENERGY", "amount": 1, "target": "DEFENDER"}
    ],
    "Discard a Darkness Energy attached to your opponent's Active Pokémon.": [
        {"type": "DISCARD_ENERGY", "amount": 1, "target": "DEFENDER"}
    ],

    # ── GRUPO 5: SELF DAMAGE ──────────────────────────────────────────
    "This Pokémon does 10 damage to itself.": [
        {"type": "ADD_DAMAGE", "amount": 10, "target": "SELF"}
    ],

    # ── GRUPO 6: PREVENT_DAMAGE ──────────────────────────────────────
    "Prevent all damage done to this Pokémon by attacks during your opponent's next turn. This Pokémon can't use King's Shield during your next turn.": [
        {"type": "PREVENT_DAMAGE", "target": "SELF"}
    ],

    # ── GRUPO 7: COIN_FLIP → CONDITION ──────────────────────────────
    "Flip a coin. If heads, your opponent's Active Pokémon is now Paralyzed.": [
        {"type": "COIN_FLIP", "ifHeads": [{"type": "APPLY_CONDITION", "condition": "PARALYZED", "target": "DEFENDER"}], "ifTails": []}
    ],
    "Flip a coin. If heads, your opponent's Active Pokémon is now Paralyzed and discard an Energy attached to that Pokémon.": [
        {"type": "COIN_FLIP", "ifHeads": [
            {"type": "APPLY_CONDITION", "condition": "PARALYZED", "target": "DEFENDER"},
            {"type": "DISCARD_ENERGY", "amount": 1, "target": "DEFENDER"}
        ], "ifTails": []}
    ],
    "Flip a coin. If heads, your opponent's Active Pokémon is now Asleep. If tails, your opponent's Active Pokémon is now Confused.": [
        {"type": "COIN_FLIP",
         "ifHeads": [{"type": "APPLY_CONDITION", "condition": "ASLEEP", "target": "DEFENDER"}],
         "ifTails": [{"type": "APPLY_CONDITION", "condition": "CONFUSED", "target": "DEFENDER"}]}
    ],

    # ── GRUPO 8: COIN_FLIP → ADD_DAMAGE ────────────────────────────
    "Flip a coin. If heads, this attack does 10 more damage.": [
        {"type": "COIN_FLIP", "ifHeads": [{"type": "ADD_DAMAGE", "amount": 10}], "ifTails": []}
    ],
    "Flip a coin. If heads, this attack does 20 more damage.": [
        {"type": "COIN_FLIP", "ifHeads": [{"type": "ADD_DAMAGE", "amount": 20}], "ifTails": []}
    ],
    "Flip a coin. If heads, this attack does 30 more damage.": [
        {"type": "COIN_FLIP", "ifHeads": [{"type": "ADD_DAMAGE", "amount": 30}], "ifTails": []}
    ],
    "Flip a coin. If heads, this attack does 40 more damage and your opponent's Active Pokémon is now Confused.": [
        {"type": "COIN_FLIP", "ifHeads": [
            {"type": "ADD_DAMAGE", "amount": 40},
            {"type": "APPLY_CONDITION", "condition": "CONFUSED", "target": "DEFENDER"}
        ], "ifTails": []}
    ],

    # ── GRUPO 9: COIN_FLIP → DISCARD_ENERGY ────────────────────────
    "Flip a coin. If heads, discard an Energy attached to your opponent's Active Pokémon.": [
        {"type": "COIN_FLIP", "ifHeads": [{"type": "DISCARD_ENERGY", "amount": 1, "target": "DEFENDER"}], "ifTails": []}
    ],
    "Flip a coin. If heads, discard an Energy attached to 1 of your opponent's Pokémon.": [
        {"type": "COIN_FLIP", "ifHeads": [{"type": "DISCARD_ENERGY", "amount": 1, "target": "DEFENDER"}], "ifTails": []}
    ],
    "Flip a coin. If tails, discard 2 Energy attached to this Pokémon.": [
        {"type": "COIN_FLIP", "ifHeads": [], "ifTails": [{"type": "DISCARD_ENERGY", "amount": 2, "target": "SELF"}]}
    ],

    # ── GRUPO 10: COIN_FLIP → PREVENT ──────────────────────────────
    "Flip a coin. If heads, prevent all damage done to this Pokémon by attacks during your opponent's next turn.": [
        {"type": "COIN_FLIP", "ifHeads": [{"type": "PREVENT_DAMAGE", "target": "SELF"}], "ifTails": []}
    ],
    "Flip a coin. If heads, prevent all effects of attacks, including damage, done to this Pokémon during your opponent's next turn.": [
        {"type": "COIN_FLIP", "ifHeads": [{"type": "PREVENT_DAMAGE", "target": "SELF"}], "ifTails": []}
    ],

    # ── GRUPO 11: COIN_FLIP → SWITCH / SEARCH / RESTRICT ───────────
    "Flip a coin. If heads, switch this Pokémon with 1 of your Benched Pokémon.": [
        {"type": "COIN_FLIP", "ifHeads": [{"type": "SWITCH_POKEMON", "target": "SELF", "force": True}], "ifTails": []}
    ],
    "Flip a coin. If heads, search your deck for a Supporter card, reveal it, and put it into your hand. Shuffle your deck afterward.": [
        {"type": "COIN_FLIP", "ifHeads": [{"type": "SEARCH_DECK", "filter": "SUPPORTER", "destination": "HAND", "amount": 1}], "ifTails": []}
    ],
    "Flip a coin. If heads, your opponent can't play any Supporter cards from his or her hand during his or her next turn.": [
        {"type": "COIN_FLIP", "ifHeads": [{"type": "RESTRICT", "restriction": "SUPPORTER", "target": "OPPONENT", "duration": "NEXT_TURN"}], "ifTails": []}
    ],

    # ── GRUPO 12: COIN_FLIP → SELF PENALTY ─────────────────────────
    "Flip a coin. If tails, this Pokémon can't attack during your next turn.": [
        {"type": "COIN_FLIP", "ifHeads": [], "ifTails": [{"type": "RESTRICT", "restriction": "ATTACK", "target": "SELF", "duration": "NEXT_TURN"}]}
    ],
    "Flip a coin. If tails, this Pokémon does 30 damage to itself.": [
        {"type": "COIN_FLIP", "ifHeads": [], "ifTails": [{"type": "ADD_DAMAGE", "amount": 30, "target": "SELF"}]}
    ],
    "Flip a coin. If tails, this attack does nothing.": [
        {"type": "COIN_FLIP", "ifHeads": [], "ifTails": []}
    ],

    # ── GRUPO 13: MULTIPLIER_DAMAGE (coin flips) ────────────────────
    "Flip 4 coins. This attack does 10 damage times the number of heads.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 10, "unitType": "COIN_FLIPS", "flips": 4}
    ],
    "Flip 5 coins. This attack does 30 damage times the number of heads.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 30, "unitType": "COIN_FLIPS", "flips": 5}
    ],
    "Flip 3 coins. This attack does 40 damage times the number of heads. If all of them are heads, prevent all effects of attacks, including damage, done to this Pokémon during your opponent's next turn.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 40, "unitType": "COIN_FLIPS", "flips": 3}
    ],
    "Flip 2 coins. This attack does 20 more damage for each heads.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 20, "unitType": "COIN_FLIPS", "flips": 2}
    ],
    "Flip 2 coins. This attack does 30 damage for each heads.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 30, "unitType": "COIN_FLIPS", "flips": 2}
    ],
    "Flip 2 coins. This attack does 30 damage times the number of heads.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 30, "unitType": "COIN_FLIPS", "flips": 2}
    ],
    "Flip a coin until you get tails. This attack does 20 more damage for each heads.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 20, "unitType": "COIN_FLIPS", "flips": -1}
    ],
    "Flip a coin until you get tails. This attack does 30 damage times the number of heads.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 30, "unitType": "COIN_FLIPS", "flips": -1}
    ],
    "Flip a coin for each Fighting Energy attached to this Pokémon. This attack does 50 damage times the number of heads.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 50, "unitType": "COIN_FLIPS", "flips": -1}
    ],
    "Flip a coin for each damage counter on this Pokémon. This attack does 30 damage times the number of heads.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 30, "unitType": "DAMAGE_COUNTERS_ON_SELF"}
    ],

    # ── GRUPO 14: MULTIPLIER_DAMAGE (energías/contadores) ──────────
    "This attack does 20 more damage for each Fire Energy attached to this Pokémon.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 20, "unitType": "ENERGY_ON_SELF"}
    ],
    "This attack does 20 more damage for each Water Energy attached to this Pokémon.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 20, "unitType": "ENERGY_ON_SELF"}
    ],
    "This attack does 20 damage times the amount of Energy attached to this Pokémon.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 20, "unitType": "ENERGY_ON_SELF"}
    ],
    "This attack does 20 damage times the number of your Benched Pokémon.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 20, "unitType": "BENCH_COUNT"}
    ],
    "This attack does 20 more damage times the amount of Energy attached to both Active Pokémon.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 20, "unitType": "ENERGY_ON_BOTH_ACTIVE"}
    ],
    "This attack does 30 more damage for each different type of basic Energy attached to this Pokémon.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 30, "unitType": "ENERGY_TYPE_DIVERSITY_ON_SELF"}
    ],
    "This attack does 10 more damage for each damage counter on this Pokémon.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 10, "unitType": "DAMAGE_COUNTERS_ON_SELF"}
    ],
    "Does 10 more damage for each damage counter on your opponent's Active Pokémon.": [
        {"type": "MULTIPLIER_DAMAGE", "amountPerUnit": 10, "unitType": "DAMAGE_COUNTERS_ON_DEFENDER"}
    ],

    # ── GRUPO 15: ADD_DAMAGE condicional ────────────────────────────
    # condition: el engine verifica antes de aplicar el efecto
    "If your opponent's Active Pokémon is a Grass Pokémon, this attack does 20 more damage.": [
        {"type": "ADD_DAMAGE", "amount": 20, "condition": "OPPONENT_IS_GRASS"}
    ],
    "If your opponent's Active Pokémon is a Pokémon-EX, this attack does 60 more damage.": [
        {"type": "ADD_DAMAGE", "amount": 60, "condition": "DEFENDER_IS_EX"}
    ],
    "If your opponent's Active Pokémon already has any damage counters on it, this attack does 40 more damage.": [
        {"type": "ADD_DAMAGE", "amount": 40, "condition": "DEFENDER_HAS_DAMAGE_COUNTERS"}
    ],
    "If your opponent's Active Pokémon is affected by a Special Condition, this attack does 60 more damage. Then, remove all Special Conditions from that Pokémon.": [
        {"type": "ADD_DAMAGE", "amount": 60, "condition": "DEFENDER_HAS_SPECIAL_CONDITION"},
        {"type": "REMOVE_CONDITIONS", "target": "DEFENDER", "condition": "DEFENDER_HAS_SPECIAL_CONDITION"}
    ],
    "If this Pokémon has any Psychic Energy attached to it, this attack does 30 more damage.": [
        {"type": "ADD_DAMAGE", "amount": 30, "condition": "SELF_HAS_ENERGY_PSYCHIC"}
    ],
    "If Lunatone is on your Bench, this attack does 30 more damage.": [
        {"type": "ADD_DAMAGE", "amount": 30, "condition": "LUNATONE_ON_BENCH"}
    ],

    # ── GRUPO 16: ADD_DAMAGE con opción y costo ────────────────────
    "You may discard an Energy attached to this Pokémon. If you do, this attack does 30 more damage.": [
        {"type": "DISCARD_ENERGY", "amount": 1, "target": "SELF", "optional": True},
        {"type": "ADD_DAMAGE", "amount": 30}
    ],
    "You may do 20 more damage. If you do, this Pokémon does 20 damage to itself.": [
        {"type": "ADD_DAMAGE", "amount": 20, "optional": True},
        {"type": "ADD_DAMAGE", "amount": 20, "target": "SELF"}
    ],
    "You may do 30 more damage. If you do, this Pokémon is now Asleep.": [
        {"type": "ADD_DAMAGE", "amount": 30, "optional": True},
        {"type": "APPLY_CONDITION", "condition": "ASLEEP", "target": "SELF"}
    ],
    "You may discard the top card of your deck. If that card is a Fire Energy card, this attack does 50 more damage.": [
        {"type": "ADD_DAMAGE", "amount": 50}
    ],

    # ── GRUPO 17: DAMAGE_TO_BENCH ────────────────────────────────────
    "This attack does 10 damage to 1 of your opponent's Benched Pokémon. (Don't apply Weakness and Resistance for Benched Pokémon.)": [
        {"type": "DAMAGE_TO_BENCH", "amount": 10, "targetCount": 1, "target": "OPPONENT_BENCH"}
    ],
    "This attack does 20 damage to 2 of your opponent's Benched Pokémon. (Don't apply Weakness and Resistance for Benched Pokémon.)": [
        {"type": "DAMAGE_TO_BENCH", "amount": 20, "targetCount": 2, "target": "OPPONENT_BENCH"}
    ],
    "This attack does 30 damage to 1 of your opponent's Benched Pokémon. (Don't apply Weakness and Resistance for Benched Pokémon.)": [
        {"type": "DAMAGE_TO_BENCH", "amount": 30, "targetCount": 1, "target": "OPPONENT_BENCH"}
    ],
    "This attack does 30 damage to 2 of your opponent's Benched Pokémon. (Don't apply Weakness and Resistance for Benched Pokémon.)": [
        {"type": "DAMAGE_TO_BENCH", "amount": 30, "targetCount": 2, "target": "OPPONENT_BENCH"}
    ],
    "Does 20 damage to 1 of your opponent's Benched Pokémon. (Don't apply Weakness and Resistance for Benched Pokémon.)": [
        {"type": "DAMAGE_TO_BENCH", "amount": 20, "targetCount": 1, "target": "OPPONENT_BENCH"}
    ],
    "This attack does 10 damage to each of your Benched Pokémon. (Don't apply Weakness and Resistance for Benched Pokémon.)": [
        {"type": "DAMAGE_TO_BENCH", "amount": 10, "targetCount": -1, "target": "SELF_BENCH"}
    ],

    # ── GRUPO 18: DRAW / SEARCH / LOOK ──────────────────────────────
    "Draw a card.": [
        {"type": "DRAW_CARD", "amount": 1}
    ],
    "Draw 2 cards.": [
        {"type": "DRAW_CARD", "amount": 2}
    ],
    "Search your deck for a card and put it into your hand. Shuffle your deck afterward.": [
        {"type": "SEARCH_DECK", "filter": "ANY", "destination": "HAND", "amount": 1}
    ],
    "Search your deck for a Grass Pokémon, reveal it, and put it into your hand. Shuffle your deck afterward.": [
        {"type": "SEARCH_DECK", "filter": "POKEMON_GRASS", "destination": "HAND", "amount": 1}
    ],
    "Search your deck for up to 2 Supporter cards, reveal them, and put them into your hand. Shuffle your deck afterward.": [
        {"type": "SEARCH_DECK", "filter": "SUPPORTER", "destination": "HAND", "amount": 2}
    ],
    "Search your deck for 3 different types of basic Energy cards, reveal them, and put them into your hand. Shuffle your deck afterward.": [
        {"type": "SEARCH_DECK", "filter": "ENERGY_DIFFERENT", "destination": "HAND", "amount": 3}
    ],
    "Search your deck for a basic Energy card and attach it to 1 of your Pokémon. Shuffle your deck afterward.": [
        {"type": "SEARCH_DECK", "filter": "ENERGY_BASIC", "destination": "ATTACH", "amount": 1}
    ],
    "Search your deck for a Fire Energy card and attach it to this Pokémon. Shuffle your deck afterward.": [
        {"type": "SEARCH_DECK", "filter": "ENERGY_FIRE", "destination": "ATTACH", "amount": 1}
    ],
    "Search your deck for a Lightning Energy card and attach it to this Pokémon. Shuffle your deck afterward. If you attached Energy in this way, switch this Pokémon with 1 of your Benched Pokémon.": [
        {"type": "SEARCH_DECK", "filter": "ENERGY_LIGHTNING", "destination": "ATTACH", "amount": 1},
        {"type": "SWITCH_POKEMON", "target": "SELF", "optional": True}
    ],
    "Look at the top 3 cards of your deck and put them back on top of your deck in any order.": [
        {"type": "LOOK_AT_DECK", "amount": 3, "target": "SELF", "reorder": True}
    ],
    "Look at the top card of your opponent's deck. Then, you may have your opponent shuffle his or her deck.": [
        {"type": "LOOK_AT_DECK", "amount": 1, "target": "OPPONENT", "reorder": False}
    ],

    # ── GRUPO 19: SWITCH ──────────────────────────────────────────────
    "Switch 1 of your opponent's Benched Pokémon with your opponent's Active Pokémon.": [
        {"type": "SWITCH_POKEMON", "target": "OPPONENT", "force": True}
    ],
    "Switch this Pokémon with 1 of your Benched Pokémon. Then, your opponent switches his or her Active Pokémon with 1 of his or her Benched Pokémon.": [
        {"type": "SWITCH_POKEMON", "target": "SELF", "force": True},
        {"type": "SWITCH_POKEMON", "target": "OPPONENT", "force": True}
    ],
    "Your opponent switches his or her Active Pokémon with 1 of his or her Benched Pokémon.": [
        {"type": "SWITCH_POKEMON", "target": "OPPONENT", "force": True}
    ],

    # ── GRUPO 20: RESTRICT ────────────────────────────────────────────
    "The Defending Pokémon can't attack during your opponent's next turn.": [
        {"type": "RESTRICT", "restriction": "ATTACK", "target": "DEFENDER", "duration": "NEXT_TURN"}
    ],
    "The Defending Pokémon can't retreat during your opponent's next turn.": [
        {"type": "RESTRICT", "restriction": "RETREAT", "target": "DEFENDER", "duration": "NEXT_TURN"}
    ],
    "The Defending Pokémon has no Abilities until the end of your next turn.": [
        {"type": "RESTRICT", "restriction": "ABILITY", "target": "DEFENDER", "duration": "NEXT_TURN"}
    ],
    "Choose 1 of your opponent's Active Pokémon's attacks. That Pokémon can't use that attack during your opponent's next turn.": [
        {"type": "RESTRICT", "restriction": "ATTACK", "target": "DEFENDER", "duration": "NEXT_TURN"}
    ],
    "This Pokémon can't use X Blast during your next turn.": [
        {"type": "RESTRICT", "restriction": "ATTACK", "target": "SELF", "duration": "NEXT_TURN"}
    ],

    # ── GRUPO 21: MOVE_ENERGY ─────────────────────────────────────────
    "Move an Energy from this Pokémon to 1 of your Benched Pokémon.": [
        {"type": "MOVE_ENERGY", "amount": 1, "source": "SELF", "destination": "SELF_BENCH"}
    ],
    "You may move an Energy attached to your opponent's Active Pokémon to 1 of your opponent's Benched Pokémon.": [
        {"type": "MOVE_ENERGY", "amount": 1, "source": "DEFENDER", "destination": "OPPONENT_BENCH", "optional": True}
    ],

    # ── GRUPO 22: DISCARD_FROM_DECK ──────────────────────────────────
    "Discard the top card of your opponent's deck.": [
        {"type": "DISCARD_FROM_DECK", "amount": 1, "target": "OPPONENT"}
    ],
    "Discard the top card of your deck. If that card is a Fighting Energy, attach it to this Pokémon.": [
        {"type": "DISCARD_FROM_DECK", "amount": 1, "target": "SELF"}
    ],

    # ── GRUPO 23: SHUFFLE_HAND / RECYCLE / ATTACH ──────────────────
    "Your opponent shuffles his or her hand into his or her deck and draws 4 cards.": [
        {"type": "SHUFFLE_HAND", "target": "OPPONENT", "drawAmount": 4}
    ],
    "Put 2 Item cards from your discard pile into your hand.": [
        {"type": "RECYCLE", "filter": "ITEM", "destination": "HAND", "amount": 2}
    ],
    "Put a card from your discard pile on top of your deck.": [
        {"type": "RECYCLE", "filter": "ANY", "destination": "DECK_TOP", "amount": 1}
    ],
    "Attach a Darkness Energy card from your discard pile to 1 of your Benched Pokémon.": [
        {"type": "ATTACH_ENERGY", "source": "DISCARD", "energyType": "DARKNESS", "target": "BENCH"}
    ],

    # ── GRUPO 24: CHOOSE / DAMAGE COUNTERS ──────────────────────────
    "Choose either Asleep or Poisoned. Your opponent's Active Pokémon is now affected by that Special Condition.": [
        {"type": "APPLY_CONDITION", "condition": "ASLEEP", "target": "DEFENDER"}
    ],
    "Choose a random card from your opponent's hand. Your opponent reveals that card and shuffles it into his or her deck.": [
        {"type": "CHOOSE_RANDOM_FROM_HAND", "amount": 1, "target": "OPPONENT"}
    ],
    "Put 2 damage counters each of your opponent's Pokémon.": [
        {"type": "DAMAGE_COUNTERS", "amount": 2, "target": "ALL_OPPONENT"}
    ],
    "Put damage counters on both Active Pokémon until the remaining HP of each Pokémon is 10.": [
        {"type": "DAMAGE_COUNTERS", "amount": -1, "target": "BOTH_ACTIVE"}
    ],
    "Before doing damage, discard all Pokémon Tool cards attached to your opponent's Active Pokémon.": [
        {"type": "DISCARD_TOOL", "target": "DEFENDER", "amount": -1}
    ],
    "Choose 2 of your Benched Pokémon. For each of those Pokémon, search your deck for a Fairy Energy card and attach it to that Pokémon. Shuffle your deck afterward.": [
        {"type": "SEARCH_DECK", "filter": "ENERGY_FAIRY", "destination": "ATTACH", "amount": 2}
    ],

    # ── GRUPO 25: DAÑO con propiedades especiales ────────────────────
    "This attack's damage isn't affected by Resistance.": [
        {"type": "ADD_DAMAGE", "amount": 0}
    ],
    "This attack's damage isn't affected by Weakness or Resistance. This Pokémon can't attack during your next turn.": [
        {"type": "ADD_DAMAGE", "amount": 0},
        {"type": "RESTRICT", "restriction": "ATTACK", "target": "SELF", "duration": "NEXT_TURN"}
    ],
    "This attack's damage isn't affected by Weakness, Resistance, or any other effects on your opponent's Active Pokémon.": [
        {"type": "ADD_DAMAGE", "amount": 0}
    ],

    # ── GRUPO 26: EFECTOS DE DURACIÓN / NEXT TURN ──────────────────
    "During your next turn, this Pokémon's Metal Wallop attack does 40 more damage (before applying Weakness and Resistance).": [
        {"type": "ADD_DAMAGE", "amount": 40}
    ],
    "During your opponent's next turn, any damage done by attacks from the Defending Pokémon is reduced by 20 (before applying Weakness and Resistance).": [
        {"type": "PREVENT_DAMAGE", "target": "SELF"}
    ],
    "During your opponent's next turn, if this Pokémon would be damaged by an attack, prevent that attack's damage done to this Pokémon if that damage is 60 or less.": [
        {"type": "PREVENT_DAMAGE", "target": "SELF"}
    ],

    # ── GRUPO 27: EFECTOS COMPLEJOS (COIN_FLIP, solo estructura) ──
    "Flip 2 coins. If both of them are heads, discard the top card of your opponent's deck for each damage counter on this Pokémon.": [
        {"type": "COIN_FLIP", "ifHeads": [], "ifTails": []}
    ],
    "Flip 3 coins. For each heads, attach a Water Energy card from your discard pile to your Benched Pokémon in any way you like.": [
        {"type": "COIN_FLIP", "ifHeads": [], "ifTails": []}
    ],
    "Your opponent flips 4 coins. For each tails, he or she discards a card from his or her hand.": [
        {"type": "COIN_FLIP", "ifHeads": [], "ifTails": []}
    ],

    # ── GRUPO 28: EFFECTS THAT ARE ESSENTIALLY NO-OP FOR ENGINE ────
    "If the Defending Pokémon tries to attack during your opponent's next turn, your opponent flips a coin. If tails, that attack does nothing.": [
        {"type": "PREVENT_DAMAGE", "target": "SELF"}
    ],
}

# ═════════════════════════════════════════════════════════════════════════════
#  HABILIDADES (PASSIVE_ABILITY)
# ═════════════════════════════════════════════════════════════════════════════

ABILITY_TO_EFFECTS = {
    "If this Pokémon is your Active Pokémon and is damaged by an opponent's attack (even if this Pokémon is Knocked Out), put 3 damage counters on the Attacking Pokémon.": [
        {"type": "PASSIVE_ABILITY", "trigger": "ON_ATTACK_RECEIVED",
         "conditions": [{"type": "IS_ACTIVE", "target": "SELF", "value": "true"}],
         "effect": {"type": "DAMAGE_COUNTERS", "amount": 3, "target": "ATTACKER"},
         "stackable": False}
    ],
    "Once during your turn (before your attack), you may draw cards until you have 6 cards in your hand.": [
        {"type": "PASSIVE_ABILITY", "trigger": "ON_PLAY",
         "conditions": [],
         "effect": {"type": "DRAW_UNTIL_HAND_SIZE", "amount": 6},
         "stackable": False}
    ],
    "As long as this Pokémon is your Active Pokémon, your opponent can't play any Item cards from his or her hand.": [
        {"type": "PASSIVE_ABILITY", "trigger": "ON_PLAY",
         "conditions": [{"type": "IS_ACTIVE", "target": "SELF", "value": "true"}],
         "effect": {"type": "RESTRICT_ITEMS", "target": "OPPONENT"},
         "stackable": False}
    ],
    "As often as you like during your turn (before your attack), you may move a Fairy Energy attached to 1 of your Pokémon to another of your Pokémon.": [
        {"type": "PASSIVE_ABILITY", "trigger": "ON_PLAY",
         "conditions": [],
         "effect": {"type": "MOVE_FAIRY_ENERGY", "target": "ANY"},
         "stackable": False}
    ],
    "Each of your Pokémon that has any Fairy Energy attached to it can't be affected by any Special Conditions. (Remove any Special Conditions affecting those Pokémon.)": [
        {"type": "PASSIVE_ABILITY", "trigger": "ON_PLAY",
         "conditions": [{"type": "HAS_ENERGY", "target": "SELF", "value": "FAIRY"}],
         "effect": {"type": "IMMUNE_TO_CONDITIONS", "target": "SELF"},
         "stackable": False}
    ],
    "Once during your turn (before your attack), you may have your opponent switch his or her Active Pokémon with 1 of his or her Benched Pokémon.": [
        {"type": "PASSIVE_ABILITY", "trigger": "ON_PLAY",
         "conditions": [],
         "effect": {"type": "FORCE_SWITCH", "target": "OPPONENT"},
         "stackable": False}
    ],
    "Once during your turn (before your attack), you may switch this Pokémon with an Aegislash in your hand. (Any cards attached to this Pokémon, damage counters, Special Conditions, turns in play, and any other effects remain on the new Pokémon.)": [
        {"type": "PASSIVE_ABILITY", "trigger": "ON_PLAY",
         "conditions": [],
         "effect": {"type": "SWITCH_WITH_CARD_IN_HAND", "target": "AEGISLASH"},
         "stackable": False}
    ],
    "Once during your turn, (before your attack), if this Pokémon is Confused, you may search your deck for a card that evolves from this Pokémon and put it onto this Pokémon. (This counts as evolving this Pokémon.) Shuffle your deck afterward.": [
        {"type": "PASSIVE_ABILITY", "trigger": "ON_PLAY",
         "conditions": [{"type": "HAS_CONDITION", "target": "SELF", "value": "CONFUSED"}],
         "effect": {"type": "SEARCH_EVOLUTION", "target": "SELF"},
         "stackable": False}
    ],
    "Once during your turn (before your attack), you may discard a Water Energy card from your hand. If you do, put 3 damage counters on 1 of your opponent's Pokémon.": [
        {"type": "PASSIVE_ABILITY", "trigger": "ON_PLAY",
         "conditions": [],
         "effect": {"type": "DISCARD_FOR_DAMAGE", "discardType": "WATER_ENERGY", "damageAmount": 30, "target": "OPPONENT_ANY"},
         "stackable": False}
    ],
    "If this Pokémon is your Active Pokémon and is Knocked Out by damage from an opponent's attack, flip a coin. If heads, put 5 damage counters on the Attacking Pokémon.": [
        {"type": "PASSIVE_ABILITY", "trigger": "ON_ALLY_KNOCKOUT",
         "conditions": [{"type": "IS_ACTIVE", "target": "SELF", "value": "true"}],
         "effect": {"type": "COIN_FLIP_DAMAGE", "headsCondition": {"type": "DAMAGE_COUNTERS", "amount": 5, "target": "ATTACKER"}},
         "stackable": False}
    ],
    "Any damage done to this Pokémon by attacks is reduced by 20 (after applying Weakness and Resistance).": [
        {"type": "PASSIVE_ABILITY", "trigger": "ON_ATTACK_RECEIVED",
         "conditions": [],
         "effect": {"type": "REDUCE_DAMAGE", "amount": 20, "target": "SELF"},
         "stackable": True}
    ],
}


# ═════════════════════════════════════════════════════════════════════════════
#  GENERACIÓN DEL JSON MINIMALISTA
# ═════════════════════════════════════════════════════════════════════════════

def main():
    input_path = Path("/mnt/c/Users/Ramiro/Desktop/programacion/proyectos/TPI/tpi-pokemon-2w1-03/docs/abilityParsing/xy1.json")
    output_path = input_path.parent / "xy1_parsed.json"

    print(f"📂 Leyendo {input_path} ...")
    with open(input_path, "r", encoding="utf-8") as f:
        raw_cards = json.load(f)

    output = []
    stats = {"total_attacks": 0, "parsed_attacks": 0, "empty_text_attacks": 0,
             "total_abilities": 0, "parsed_abilities": 0}

    for card in raw_cards:
        minimal_card = {
            "id": card["id"],
            "name": card.get("name", ""),
        }

        # ── Ataques ──────────────────────────────────────────────────
        if "attacks" in card:
            minimal_attacks = []
            for atk in card["attacks"]:
                stats["total_attacks"] += 1
                text = atk.get("text", "").strip()

                entry = {
                    "name": atk.get("name", ""),
                    "text": text,
                }

                if not text:
                    # Daño puro, sin efectos → parsedEffects vacío
                    entry["parsedEffects"] = []
                    stats["empty_text_attacks"] += 1
                elif text in TEXT_TO_EFFECTS:
                    entry["parsedEffects"] = list(TEXT_TO_EFFECTS[text])
                    stats["parsed_attacks"] += 1
                else:
                    # No debería pasar, pero por si las moscas
                    print(f"⚠️  TEXTO NO MAPEADO: [{card['id']}] {atk.get('name')}: {text[:60]}")
                    entry["parsedEffects"] = []

                minimal_attacks.append(entry)

            if minimal_attacks:
                minimal_card["attacks"] = minimal_attacks

        # ── Habilidades ──────────────────────────────────────────────
        if "abilities" in card:
            minimal_abilities = []
            for ab in card["abilities"]:
                stats["total_abilities"] += 1
                text = ab.get("text", "").strip()

                entry = {
                    "name": ab.get("name", ""),
                    "text": text,
                }

                if text in ABILITY_TO_EFFECTS:
                    entry["parsedEffects"] = list(ABILITY_TO_EFFECTS[text])
                    stats["parsed_abilities"] += 1
                else:
                    print(f"⚠️  ABILITY NO MAPEADA: [{card['id']}] {ab.get('name')}")
                    entry["parsedEffects"] = []

                minimal_abilities.append(entry)

            if minimal_abilities:
                minimal_card["abilities"] = minimal_abilities

        output.append(minimal_card)

    # ── Estadísticas ─────────────────────────────────────────────────
    print(f"\n📊 Estadísticas:")
    print(f"   Cartas:               {len(output)}")
    print(f"   Ataques totales:      {stats['total_attacks']}")
    print(f"   Con parsedEffects:    {stats['parsed_attacks']}")
    print(f"   Daño puro (sin texto): {stats['empty_text_attacks']}")
    print(f"   Habilidades totales:    {stats['total_abilities']}")
    print(f"   Habilidades parseadas:  {stats['parsed_abilities']}")

    # ── Escribir ────────────────────────────────────────────────────
    print(f"\n📝 Escribiendo {output_path} ...")
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(output, f, indent=2, ensure_ascii=False)

    print(f"✅ ¡Listo! {output_path.name} generado ({output_path.stat().st_size / 1024:.0f} KB)")


if __name__ == "__main__":
    main()
