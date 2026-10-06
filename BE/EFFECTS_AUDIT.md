# Effects System Audit — XY1

> Última actualización: 2026-06-26
> Metodología: revisión de `engine/effects/`, `engine/chain/handlers/`, `xy1_parsed.json` y registries.

Legend: ✅ Resuelto · 🔴 Stub puro · 🟡 Incompleto · 🗑️ Dead code eliminado

---

## Dead Code — Eliminado

| Clase / Artefacto | Motivo |
|---|---|
| 🗑️ `DrawUntilHandSizeLogic` (attack) | `DRAW_UNTIL_HAND_SIZE` no aparece en ningún ataque de xy1; solo existe en abilities (Delphox), que usa `AbilityActivationResolver` |
| 🗑️ `HealTrainerLogic` | Ningún trainer de xy1 emite tipo `HEAL` trainer effect |

---

## Stubs Puros — Ataques completamente rotos

### ✅ ~~`PreventDamageLogic` — 4 ataques afectados~~
**Ataques:** Kakuna (Harden), Lunatone (Moonblast), Malamar (Mental Panic), Aegislash (King's Shield)
**Solución:** `ActivePokemon.damageProtected` (+ `PokemonInPlayState`) persiste el flag cross-turno. `DamageApplicationHandler` lo lee al inicio de `handle()`, bloquea el daño y lo consume (un hit). `PreventDamageLogic.isPostDamage()=true` escribe el flag en el atacante. Tests: 8 casos cubriendo set, consumo en primer hit, daño normal en segundo hit.

---

### ✅ ~~`DamageToBenchLogic` — 7 ataques afectados~~
**Ataques:** Ledian, M Blastoise-EX (×2), Trevenant, Dugtrio (Earthquake — banca propia), Stoutland, Xerneas-EX
**Solución:** `isPostDamage()=true`. Itera los primeros `targetCount` slots de banca (sin debilidad/resistencia por regla TCG); `targetCount=-1` golpea toda la banca propia (Dugtrio). KO en banca: nuevo `KnockoutProcessor.processIfKnockedOutOnBench()` que descarta y otorga prize cards sin requerir selección de promoción. Iteración en reversa para evitar shift de índices al remover. Tests: 10 casos.

---

### ✅ ~~`RestrictLogic` — 8 ataques afectados~~
**Ataques:** Simisage (Torment), Arbok (Gastro Acid), Scolipede (Poison Ring), Rhyperior (Rock Wrecker), Krookodile, Zoroark (Corner), Wigglytuff, Xerneas-EX (X Blast ×2)
**Solución:** `ActivePokemon.restrictions` (Set&lt;String&gt;) + `PlayerField.playerRestrictions` para restricciones de nivel jugador (SUPPORTER). Persistido en `PokemonInPlayState.restrictions`. `TurnManager.resetTurnFlags()` limpia ambos conjuntos. Checks en `RuleValidator` (ATTACK, RETREAT) y `MainPhaseState` (SUPPORTER, ABILITY). Tests: 7 casos.

---

### ✅ ~~`SwitchPokemonLogic` — 8 ataques afectados~~
**Ataques:** Volbeat, Blastoise-EX (Rapid Spin), Krookodile (Knock Back), Emolga-EX (Energy Glide), +4 más
**Solución:** `isPostDamage()=true`. Crea `PendingSelection(SWITCH_POKEMON)` con `validOptions`=instanceIds del banco del jugador objetivo. `SelectionResolver.switchActive()` mueve el Active actual al banco y promueve el elegido (condiciones limpias). Limitación conocida: Rapid Spin (dual-switch) solo ejecuta el primer SWITCH — el segundo se ignora cuando ya hay `pendingSelection`. Tests: 5 casos.

---

### ✅ ~~`SearchDeckLogic` — 9 ataques afectados~~
**Ataques:** varios (incluyendo Yanmega, Goodra, etc.)
**Solución:** `isPostDamage()=true`. Dos caminos por `destination`: `ATTACH` → adjunta automáticamente la primera carta matching al Active (sin selección); `HAND` → crea `PendingSelection(SEARCH_DECK)` con `validOptions`=instanceIds matching y `selectionCount`. `SelectionResolver` caso `SEARCH_DECK`: valida los elegidos contra `validOptions`, los mueve de mazo a mano. Filtros soportados: `POKEMON_<TYPE>`, `ENERGY_<TYPE>`, `ENERGY_BASIC`, `SUPPORTER`, `ANY`. Tests: 12 casos.

---

## Incompletas

### 🟡 `DamageCountersLogic`
- El branch default para selección de targets no hace nada (`/* requires target selection → Block 3 */`).
- `counters <= 0` retorna sin evento — el jugador no recibe feedback.

### 🟡 `MultiplierDamageLogic`
- Solo maneja `COIN_FLIPS` y `ENERGY_ON_SELF`. Otros `unitType` producen 0 daño extra silenciosamente.

### 🟡 `AddDamageLogic`
- Condiciones desconocidas aplican el daño bonus por defecto (`// If strategy is unknown, default to true`). Shortcut explícito de MVP.

### 🟡 `AbilityActivationResolver`
- Solo soporta `DRAW_UNTIL_HAND_SIZE` (Delphox). Cualquier otra habilidad activada lanza `ABILITY_NOT_SUPPORTED`.

### 🟡 `SelectionsHandler`
- Ataques que targetean banca siempre usan el Pokémon Activo del oponente como target. Bench-targeting no implementado.

### 🟡 `ModifierHandler`
- Solo Muscle Band implementado. Hard Charm, Rocky Helmet y otros tools ignorados silenciosamente.

---

## Tipos en xy1_parsed.json Sin Lógica Registrada

Se deserializan a `UnknownEffect` y se ignoran silenciosamente:

| Tipo | Cartas afectadas | Descripción |
|---|---|---|
| `ATTACH_ENERGY` | 1 (Yveltal Oblivion Wing) | Atar energía del mazo al atacar |
| `CHOOSE_RANDOM_FROM_HAND` | 1 (Phantump Astonish) | Descartar carta aleatoria de la mano rival |
| `DISCARD_FROM_DECK` | 2 (Rhyhorn, Gurdurr) | Descartar X cartas del mazo |
| `DISCARD_TOOL` | 2 (Skarmory-EX Joust) | Descartar Tool del Pokémon objetivo |
| `DRAW_CARD` | 3 (Lunatone, Sableye, Fletchling) | Robar X cartas |
| `MOVE_ENERGY` | 3 (Grumpig, Yveltal-EX ×2) | Mover energía entre Pokémon |
| `RECYCLE` | 2 (Simipour, Diggersby) | Poner carta del descarte en mazo |
| `REMOVE_CONDITIONS` | 2 (Corsola, Conkeldurr) | Curar condiciones especiales |
