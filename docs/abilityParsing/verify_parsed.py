#!/usr/bin/env python3
"""
Verificador de xy1_parsed.json.
- Checkea que todos los ataques con texto tengan parsedEffects
- Lista ataques condicionales y sus condiciones
- Da estadisticas generales
"""

import json
from pathlib import Path

BASE = Path(__file__).parent
PARSED = BASE / "xy1_parsed.json"
RAW = BASE / "xy1.json"

data = json.loads(PARSED.read_text(encoding='utf-8'))

# ── Estadisticas generales ──────────────────────────────────────────
cards = len(data)
total_attacks = sum(len(c.get('attacks', [])) for c in data)
total_abilities = sum(len(c.get('abilities', [])) for c in data)
attacks_with_effects = sum(
    1 for c in data for a in c.get('attacks', [])
    if a.get('parsedEffects')
)
attacks_no_text = sum(
    1 for c in data for a in c.get('attacks', [])
    if not a.get('text', '').strip()
)
missing_effects = [
    (c['id'], a['name'])
    for c in data for a in c.get('attacks', [])
    if a.get('text', '').strip() and not a.get('parsedEffects')
]
abilities_with_effects = sum(
    1 for c in data for ab in c.get('abilities', [])
    if ab.get('parsedEffects')
)
missing_ability_effects = [
    (c['id'], ab['name'])
    for c in data for ab in c.get('abilities', [])
    if not ab.get('parsedEffects')
]

print("=" * 60)
print("📊 VERIFICACION DE xy1_parsed.json")
print("=" * 60)
print(f"  Cartas:                 {cards}")
print(f"  Ataques totales:        {total_attacks}")
print(f"  Con parsedEffects:      {attacks_with_effects}")
print(f"  Danio puro (sin texto): {attacks_no_text}")
print(f"  Habilidades totales:    {total_abilities}")
print(f"  Hab. con parsedEffects: {abilities_with_effects}")

if missing_effects:
    print(f"\n❌ ATAQUES CON TEXTO PERO SIN parsedEffects:")
    for cid, aname in missing_effects:
        print(f"    {cid} / {aname}")

if missing_ability_effects:
    print(f"\n❌ HABILIDADES SIN parsedEffects:")
    for cid, aname in missing_ability_effects:
        print(f"    {cid} / {aname}")

if not missing_effects and not missing_ability_effects:
    print("\n✅ Todos los ataques/habilidades con texto tienen parsedEffects")

# ── Ataques condicionales (solo ADD_DAMAGE, REMOVE_CONDITIONS, etc.) ──
# NOTA: APPLY_CONDITION tiene campo 'condition' pero significa "qué estado
# aplicar", no "bajo qué condición del juego". Se excluye de este listado.
print(f"\n{'=' * 60}")
print("🔍 ATAQUES CON CONDICION DE JUEGO")
print("=" * 60)
for card in data:
    for a in card.get('attacks', []):
        for e in a.get('parsedEffects', []):
            if e.get('condition') and e['type'] not in ('APPLY_CONDITION',):
                print(f"  {card['id']:8s} | {a['name']:20s} | {e['type']:20s} | condition={e['condition']}")

# ── Efectos usados ─────────────────────────────────────────────────
print(f"\n{'=' * 60}")
print("📋 TIPOS DE EFECTO USADOS (frecuencia)")
print("=" * 60)
from collections import Counter
type_counts = Counter()
for card in data:
    for a in card.get('attacks', []):
        for e in a.get('parsedEffects', []):
            type_counts[e['type']] += 1
    for ab in card.get('abilities', []):
        for e in ab.get('parsedEffects', []):
            type_counts[e['type']] += 1

for etype, count in sorted(type_counts.items(), key=lambda x: -x[1]):
    print(f"  {etype:25s} x {count}")
