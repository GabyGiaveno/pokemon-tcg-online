#!/usr/bin/env python3
"""
Agrega el bloque `trainerEffects` a las cartas de Entrenador (Trainer/Item/Supporter/Stadium)
en xy1_parsed.json, siguiendo el MISMO método que build_parsed_json.py (mapeo semántico
texto/carta -> efectos atómicos, consumidos por el motor Java vía polimorfismo Jackson).

NOTA: este script es documentación del método. El parseo efectivo ya fue aplicado al JSON
siguiendo este template (el entorno de dev no tiene Python). Es idempotente: hace merge por id,
solo agrega `trainerEffects` a las cartas mapeadas, sin tocar el resto.

Vocabulario de efectos de trainer (subtipos TrainerEffect en Java):
  DRAW_CARDS         { amount }
  HEAL               { amount, target }
  DISCARD_HAND_DRAW  { amount }                       (descarta la mano y roba `amount`)
  SHUFFLE_HAND       { target: SELF|OPPONENT, drawAmount }
  DISCARD_ENERGY     { amount, target: OPPONENT_ACTIVE }
  COIN_FLIP          { ifHeads: [TrainerEffect], ifTails: [TrainerEffect] }
Tipos no soportados aún por el engine caen en UnknownTrainerEffect (inerte) hasta su bloque.
"""

import json
from pathlib import Path

# Mapeo por NOMBRE de carta -> trainerEffects (los trainers no tienen "text" por ataque)
TRAINER_NAME_TO_EFFECTS = {
    # ── Supporters ──────────────────────────────────────────────────────────
    "Professor Sycamore": [
        {"type": "DISCARD_HAND_DRAW", "amount": 7}
    ],
    "Shauna": [
        {"type": "SHUFFLE_HAND", "target": "SELF", "drawAmount": 5}
    ],
    "Team Flare Grunt": [
        {"type": "DISCARD_ENERGY", "amount": 1, "target": "OPPONENT_ACTIVE"}
    ],
    # ── Items ───────────────────────────────────────────────────────────────
    "Red Card": [
        {"type": "SHUFFLE_HAND", "target": "OPPONENT", "drawAmount": 4}
    ],
    "Roller Skates": [
        {"type": "COIN_FLIP",
         "ifHeads": [{"type": "DRAW_CARDS", "amount": 3}],
         "ifTails": []}
    ],
    # ── Pendientes (requieren selección / pasivos) → bloques posteriores ──────
    # "Super Potion": HEAL + discard energy (selección)        → Bloque 3
    # "Great Ball" / "Professor's Letter" / "Evosoda": SEARCH  → Bloque 3
    # "Max Revive": RECYCLE                                     → Bloque 3
    # "Cassius": SHUFFLE_POKEMON_INTO_DECK                      → Bloque 3
    # "Hard Charm" / "Muscle Band": PASSIVE_TOOL               → Bloque 4
    # "Fairy Garden" / "Shadow Circle": PASSIVE_STADIUM        → Bloque 4
}


def main():
    path = Path(__file__).resolve().parents[2] / "BE/src/main/resources/data/xy1_parsed.json"
    cards = json.loads(path.read_text(encoding="utf-8"))

    patched = 0
    for card in cards:
        effects = TRAINER_NAME_TO_EFFECTS.get(card.get("name"))
        if effects is not None:
            card["trainerEffects"] = effects  # merge idempotente por id/nombre
            patched += 1

    path.write_text(json.dumps(cards, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"trainerEffects aplicados a {patched} cartas en {path.name}")


if __name__ == "__main__":
    main()
