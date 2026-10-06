# Inventario de Implementación de Efectos — Pokémon TCG (XY1)

> ⚠️ **Cómo leer este documento (nota 2026-06-11):** el inventario es el **mapa estructural**.
> Las columnas de *estado* son de la auditoría 2026-06-01 y varias ya evolucionaron — el estado
> VIVO por bloque está en [`EFFECT_IMPLEMENTATION_PROGRESS.md`](EFFECT_IMPLEMENTATION_PROGRESS.md).
> Posteriores a la auditoría: trainers EJECUTAN (Bloque 2), selección de jugador EXISTE (Bloque 3),
> habilidades tier 1 EJECUTAN (Bloque 4 — §3 actualizado; guía completa en `ENGINE_GUIDE.md` §28).

> Fecha: 2026-06-01. Documento vivo. Auditoría **verificada archivo por archivo** del
> sistema de efectos del engine. Reemplaza lo que los `.md` anteriores sobrevendían como
> "Effect system completo".
>
> **Propósito:** para CADA efecto (ataque / habilidad / entrenador) dejar sentado
> **dónde está implementado hoy** y **dónde debería estar**, con su estado real.

---

## Cómo leer este documento

**Patrones reales del engine** (corrige la confusión "Strategy"):

| Patrón | Dónde | Para qué |
|--------|-------|----------|
| **Flyweight** | `EffectRegistry`→`EffectLogic` (ataques) · `TrainerEffectRegistry`→`TrainerEffectLogic` (entrenadores) | Lógica de cada efecto, instancias stateless |
| **Strategy** | `engine/effects/conditions/` (`ConditionStrategy`) | SOLO los `condition` guard de un efecto (ej. `OPPONENT_IS_GRASS`) |
| **Chain of Responsibility** | `engine/chain/` (7 handlers) | Pipeline de resolución de ataque |
| **State** | `engine/state/` (4 fases) | Ciclo de turno |

> ❌ **No existe** carpeta `engine/strategy/`. `AttackEffect` NO está en ningún Strategy:
> es un DTO polimórfico (Jackson `@JsonSubTypes`) ejecutado por un `EffectLogic` (Flyweight).

**Leyenda de estado:**
- ✅ **Real + corre** — lógica implementada y efectivamente invocada.
- 🟠 **Real pero muerto** — lógica implementada pero NUNCA se ejecuta (no marca `isPreDamage`/`isPostDamage`).
- 🔴 **Stub** — solo `System.out.println(...)`.
- ⚫ **Sin lógica** — tiene subtipo/parseo pero no hay `EffectLogic` registrada.
- 🚫 **Sin clase** — el JSON usa el `type` pero no existe subtipo Java → rompe el parseo.

**Mecanismo de ejecución (ataques):** `PreAttackHandler.java:36` corre los efectos con
`isPreDamage()==true`; `PostDamageHandler.java:97` los de `isPostDamage()==true`. El default
de ambos en `EffectLogic.java:25-34` es `false`. **Una logic que no override ninguno NO se ejecuta.**

---

## 1. Efectos de ATAQUE (`AttackEffect`)

Subtipos declarados en `AttackEffect.java:12-29` (16). Logics registradas en `EffectRegistry.java:26-44`.

| `type` | Freq JSON | Subtipo | Logic (`engine/effects/logics/`) | Flag | Estado |
|--------|:---:|:---:|------|------|--------|
| `ADD_DAMAGE` | 34 | ✓ | `AddDamageLogic` (`:31` pre) | pre | ✅ Real + corre |
| `APPLY_CONDITION` | 27 | ✓ | `ApplyConditionLogic` (`:64` post) | post | ✅ Real + corre |
| `MULTIPLIER_DAMAGE` | 22 | ✓ | `MultiplierDamageLogic` (`:50` pre) | pre | ✅ Real + corre |
| `HEAL` | 10 | ✓ | `HealLogic` (`:35` post) | post | ✅ Real + corre |
| `COIN_FLIP` | **35** | ✓ | `CoinFlipLogic` | — | 🟠 Real pero **muerto** (no flag) |
| `DISCARD_ENERGY` | 18 | ✓ | `DiscardEnergyLogic` | — | 🟠 Real pero **muerto** (no flag) |
| `RESTRICT` | 10 | ✓ | `RestrictLogic` | — | 🔴 Stub |
| `SEARCH_DECK` | 10 | ✓ | `SearchDeckLogic` | — | 🔴 Stub (requiere selección) |
| `SWITCH_POKEMON` | 9 | ✓ | `SwitchPokemonLogic` | — | 🔴 Stub (requiere selección) |
| `PREVENT_DAMAGE` | 7 | ✓ | `PreventDamageLogic` | — | 🔴 Stub (requiere efecto diferido) |
| `DAMAGE_TO_BENCH` | 7 | ✓ | `DamageToBenchLogic` | — | 🔴 Stub (requiere selección) |
| `DAMAGE_COUNTERS` | 4 | ✓ | `DamageCountersLogic` | — | 🔴 Stub |
| `LOOK_AT_DECK` | 2 | ✓ | `LookAtDeckLogic` | — | 🔴 Stub (requiere selección) |
| `DRAW_UNTIL_HAND_SIZE` | 1 | ✓ | `DrawUntilHandSizeLogic` | — | 🔴 Stub |
| `SHUFFLE_HAND` | 1 | ✓ | `ShuffleHandLogic` | — | 🔴 Stub |
| `PASSIVE_ABILITY` | 12 | ✓ | ✅ tier 1 (6/12) | n/a | 🟢 3 familias ejecutan (ver §3) |

> **Resumen brutal:** de 16 tipos, **4 funcionan** (ADD_DAMAGE, APPLY_CONDITION, MULTIPLIER_DAMAGE, HEAL).
> 2 están implementados pero muertos. 9 son stubs. 1 sin lógica.

**Dónde DEBE implementarse cada uno:**
- Subtipo DTO → `models/cards/effects/<Nombre>Effect.java` + alta en `@JsonSubTypes` de `AttackEffect`.
- Lógica → `engine/effects/logics/<Nombre>Logic.java`, **override `isPreDamage()` o `isPostDamage()`**, registrar en `EffectRegistry`.

**Fix inmediato para los 🟠:** `CoinFlipLogic` y `DiscardEnergyLogic` solo necesitan override del flag correcto para revivir (CoinFlip suele ser pre o post según el sub-efecto; DiscardEnergy según el ataque).

---

## 2. Condiciones guard (`ConditionStrategy`)

Único Strategy real. Registradas en `ConditionRegistry.java:33-40`. **Las 6 existen y están completas.**

| Clave JSON | Clase (`engine/effects/conditions/`) | Estado |
|-----------|------|--------|
| `OPPONENT_IS_GRASS` | `OpponentIsGrassCondition` | ✅ |
| `DEFENDER_IS_EX` | `DefenderIsExCondition` | ✅ |
| `DEFENDER_HAS_DAMAGE_COUNTERS` | `DefenderHasDamageCountersCondition` | ✅ |
| `DEFENDER_HAS_SPECIAL_CONDITION` | `DefenderHasSpecialCondition` | ✅ |
| `SELF_HAS_ENERGY_PSYCHIC` | `SelfHasEnergyPsychicCondition` | ✅ |
| `LUNATONE_ON_BENCH` | `LunatoneOnBenchCondition` | ✅ |

> Solo las consume `AddDamageLogic`. Otras logics que necesiten guard deben replicar el patrón
> (`ConditionRegistry.getInstance().getStrategy(key).evaluate(ctx)`).

---

## 3. HABILIDADES (`PASSIVE_ABILITY`) — 12 en el JSON · ✅ tier 1 EJECUTA (2026-06-11)

**Estado: 6 de 12 funcionan** (Bloque 4 tier 1, continuación del change `effect-execution-trainers`).
Los dos problemas históricos quedaron resueltos:

1. **Parseo ✅:** `AbilityParser` (espejo de `TrainerEffectParser`) abre el bloque `"abilities"`;
   `PassiveAbilityEffect` ahora tiene `conditions: List<AbilityCondition>`; subtipos anidados nuevos
   `COIN_FLIP_DAMAGE`, `REDUCE_DAMAGE`, `RESTRICT_ITEMS`, `IMMUNE_TO_CONDITIONS`. Lo que no se
   conoce sigue cayendo a `UnknownEffect` inerte.
2. **Ejecución ✅ (3 familias):** paquete `engine/effects/abilities/` —
   - Activadas → `ActionType.USE_ABILITY` + `AbilityActivationResolver` (Mystical Fire).
   - Disparadas → `AbilityTriggerResolver` en `PostDamageHandler` (Spiky Shield, Destiny Burst).
   - Continuas → `ContinuousAbilityQuery` como guards (Forest's Curse, Fur Coat, Sweet Veil).

**Tier 2 pendiente (6/12, requieren selección/resumable):** Water Shuriken, Upside-Down Evolution,
Stance Change ×2, Fairy Transfer, Drive Off. Se rechazan con `ABILITY_NOT_SUPPORTED` — no se
consumen en silencio. Receta para agregar habilidades: `ENGINE_GUIDE.md` §28.5.

---

## 4. ENTRENADORES / ITEMS / STADIUMS (`TrainerEffect`) — 15 cartas XY1

Subtipos en `TrainerEffect.java:17-20` (hoy solo 2). Logics en `engine/effects/trainers/`.
Ejecución en `MainPhaseState.java:411-423` (`executeTrainerEffects`), que lee `card.getParsedTrainerEffects()`
**que hoy nadie puebla** → ningún trainer hace nada.

| Carta | Subtype | `type` propuesto | Logic | Estado | Categoría |
|-------|---------|------------------|-------|--------|-----------|
| (base) | `DRAW_CARDS` | — | `DrawCardsTrainerLogic` | ✅ Real (no usada) | autocontenida |
| (base) | `HEAL` | — | `HealTrainerLogic` | ✅ Real (no usada) | autocontenida |
| Professor Sycamore | Supporter | `DISCARD_HAND_DRAW` | 🆕 | falta | autocontenida |
| Shauna | Supporter | `SHUFFLE_HAND` (self) | 🆕 | falta | autocontenida |
| Red Card | Item | `SHUFFLE_HAND` (opponent) | 🆕 | falta | autocontenida |
| Team Flare Grunt | Supporter | `DISCARD_ENERGY` (defender) | 🆕 | falta | autocontenida |
| Roller Skates | Item | `COIN_FLIP`→`DRAW_CARDS` | 🆕 | falta | autocontenida |
| Super Potion | Item | `HEAL` + `discardEnergy` | extiende HEAL | falta | requiere selección (cuál Pokémon/energía) |
| Cassius | Supporter | `SHUFFLE_POKEMON_INTO_DECK` | 🆕 | falta | requiere selección |
| Evosoda | Item | `SEARCH_AND_EVOLVE` | 🆕 | falta | requiere selección |
| Great Ball | Item | `SEARCH_DECK` (top 7, Pokémon) | 🆕 | falta | requiere selección |
| Professor's Letter | Item | `SEARCH_DECK` (2 energía básica) | 🆕 | falta | requiere selección |
| Max Revive | Item | `RECYCLE` (discard→deck top) | 🆕 | falta | requiere selección |
| Hard Charm | Pokémon Tool | `PASSIVE_TOOL` (−20 daño recibido) | 🆕 | falta | efecto continuo |
| Muscle Band | Pokémon Tool | `PASSIVE_TOOL` (+20 daño ataque) | 🆕 | falta | efecto continuo (hoy hardcodeado en `ModifierHandler.java:21-43`, regla incorrecta) |
| Fairy Garden | Stadium | `PASSIVE_STADIUM` (sin retiro si Fairy) | 🆕 | falta | efecto continuo |
| Shadow Circle | Stadium | `PASSIVE_STADIUM` (sin debilidad si Darkness) | 🆕 | falta | efecto continuo |

**Dónde DEBE implementarse:**
- Subtipo DTO → `models/cards/effects/trainer/<Nombre>Effect.java` + alta en `@JsonSubTypes` de `TrainerEffect`.
- Lógica → `engine/effects/trainers/<Nombre>TrainerLogic.java`, registrar en `TrainerEffectRegistry`.
- Pipeline de carga (falta): `TrainerEffectParser` + bloque `trainerEffects` en el JSON parseado (ver `session6-working-agreement`).

---

## 5. Tipos HUÉRFANOS del JSON (rompen el parseo) 🚫

Estos `type` aparecen en `xy1_parsed.json` pero **no existen como subtipo** de `AttackEffect`.
Jackson tira excepción → `AttackParser.java:72` la captura → la carta pierde **todos** sus efectos.

| `type` | Freq | Aparece en |
|--------|:---:|-----------|
| `MOVE_ENERGY` | 3 | ataques |
| `DRAW_CARD` | 3 | ataques (¡ojo: el subtipo se llama `DRAW_UNTIL_HAND_SIZE`, no `DRAW_CARD`!) |
| `REMOVE_CONDITIONS` | 2 | ataques |
| `RECYCLE` | 2 | ataques |
| `DISCARD_FROM_DECK` | 2 | ataques |
| `DISCARD_TOOL` | 2 | ataques |
| `CHOOSE_RANDOM_FROM_HAND` | 1 | ataques |
| `ATTACH_ENERGY` | 1 | ataques |

**Fix:** o se crean los subtipos+logics, o se re-mapea el JSON al vocabulario soportado, **y** se hace
robusto el parser (ignorar un efecto desconocido sin perder la carta entera).

---

## 6. Mecanismos TRANSVERSALES que faltan

Sin estos dos, una parte de las cartas NO puede implementarse "correctamente":

### 6.1 Mecanismo de SELECCIÓN del jugador ✅ (implementado — `player-selection-mechanism`)
**HECHO** para `CHOOSE_ACTIVE_ON_KO`. Existe `ActionType.RESOLVE_SELECTION`, `models/game/PendingSelection`
+ `SelectionType` + campo `BoardState.pendingSelection`, `engine/SelectionResolver` (valida + aplica,
dispatch por tipo), y el `TurnManager` pausa/reanuda (guarda + 2 cortes + `resolveSelection`). El KO
elige activo al promover (**fix #4 hecho**). Engine verde.

**Pendiente para los efectos de ataque con selección** (search/switch/look/damage-to-bench): el
pipeline de ataque debe poder **pausar en medio y reanudar** (Memento + CoR resumible). Diseño aprobado:
`docs/engine/RESUMABLE_ATTACK_EFFECTS_PLAN.md`. Cada efecto = nuevo `SelectionType` + case en
`SelectionResolver` (OCP), sin tocar el `TurnManager`.

**Pendiente de borde (otro dev):** mapear `pendingSelection` a `GET /state` en `EngineStateMapper` para
que sobreviva entre requests HTTP (`player-selection-mechanism/tasks.md §Fase 5`).

### 6.2 Mecanismo de EFECTOS CONTINUOS (no existe)
Para `PASSIVE_ABILITY`, tools y stadiums. Hoy el único caso es Muscle Band **hardcodeado por nombre**
en `ModifierHandler.java` (y con regla incorrecta).

**Dónde debería estar:**
- Registro de efectos continuos activos en `BoardState` (tools adjuntas + stadium + abilities en juego).
- Consulta en los puntos de decisión: `DamageCalculator` (modificadores ±), cálculo de retiro
  (`MainPhaseState.handleRetreat`), `RuleValidator` (bloqueos), `KnockoutProcessor` (prizes).

---

## 7. Otros pendientes de correctness ya conocidos

- **Fix #4 — Elegir Pokémon activo al promover:** ✅ HECHO. `KnockoutProcessor` ya NO auto-promueve:
  setea `pendingSelection` (`CHOOSE_ACTIVE_ON_KO`) y el dueño elige vía `RESOLVE_SELECTION` (§6.1).
- **`ModifierHandler` Muscle Band:** suma +20 solo a EX/MEGA; la carta real suma a TODOS. A migrar al
  mecanismo de efectos continuos (§6.2).

---

## 8. Bloques YA ESCRITOS y reutilizables (NO reescribir) ⭐

Barrido archivo-por-archivo. Antes de crear una logic nueva, **conectar/reusar** esto:

| Bloque existente | Archivo:línea | Qué hace | Reusar para |
|------------------|---------------|----------|-------------|
| `StatusEffectManager.applyCondition()` | `engine/StatusEffectManager.java:114` | Aplica condición **respetando exclusividad** ASLEEP/CONFUSED/PARALYZED + BURNED/POISONED | `APPLY_CONDITION` (ataque y trainer) |
| `StatusEffectManager.clearAllConditions()` | `:126` | Limpia todas las condiciones | `REMOVE_CONDITIONS` |
| `StatusEffectManager.applyBetweenTurnEffects()` | `:51` | Veneno/quemadura/sueño/parálisis entre turnos | (ya conectado en `BetweenTurnsState`) |
| `HealLogic` / `HealTrainerLogic` | `logics/HealLogic.java` · `trainers/HealTrainerLogic.java` | Curan (ACTIVE/SELF) / (ACTIVE/BENCH/ALL) | `HEAL` — Super Potion reusa `HealTrainerLogic` |
| `DiscardEnergyLogic` | `logics/DiscardEnergyLogic.java` | Descarta N energías de SELF/DEFENDER (-1 = todas) | `DISCARD_ENERGY` — Team Flare Grunt. **Real pero muerto → revivir con flag** |
| `CoinFlipLogic` | `logics/CoinFlipLogic.java` | Flip + dispatch recursivo `ifHeads`/`ifTails` | `COIN_FLIP` — Roller Skates. **Real pero muerto → revivir con flag** |
| Robar 1 del mazo | `DrawPhaseState.java:63-65` | `deck.remove(0)` → `hand` | `DRAW_CARDS`, `DRAW_UNTIL_HAND_SIZE` |
| `DamageCalculator.calculate()` | `chain/DamageCalculator.java:46` | Debilidad ×2 / resistencia −20 / redondeo a 10 | cualquier daño |
| Promoción de banca | `KnockoutProcessor.java:98-116` | Construye `ActivePokemon` desde `BenchPokemon` y lo promueve | `SWITCH_POKEMON`, `CHOOSE_ACTIVE` |
| Swap activo↔banca | `MainPhaseState.handleRetreat (:254-313)` | Mueve activo a banca + promueve elegido | `SWITCH_POKEMON` |
| `ConditionRegistry` + 6 strategies | `effects/conditions/` | Guards (`OPPONENT_IS_GRASS`, `DEFENDER_IS_EX`, …) | `condition` de cualquier efecto |
| `CardTypeResolver` / `PokemonFactory` / `PlayerFieldBuilder` | `engine/factories/` | Clasificar carta · construir Pokémon · armar field | construcción/clasificación |

### Duplicaciones a evitar / consolidar
- **Condiciones:** `ApplyConditionLogic.java:22-43` **reimplementa** el switch de condiciones, duplicando
  `StatusEffectManager.applyCondition()` y **sin respetar la exclusividad**. Debería **delegar** en el manager.
- **Heal:** existe en `HealLogic` (ataque) y `HealTrainerLogic` (trainer) por separado (contratos distintos:
  `AttackContext` vs `board+playerId`). Aceptable, pero el cálculo `min(hp+amount, maxHp)` podría extraerse a un helper.
- **Resolución de field por playerId:** el patrón `player1Field.getPlayerId().equals(...) ? ... : ...`
  está copiado en ~6 clases. Candidato a helper en `BoardState`.

---

## 9. Orden de ataque recomendado (por dependencias)

1. **Vocabulario + parser robusto** (§5): que ningún `type` rompa la carta; alinear los 8 huérfanos.
2. **Categoría autocontenida** (§1 🟠 + stubs sin selección, §4 trainers on-play): revivir flags + reemplazar stubs + `TrainerEffectParser`.
3. **Mecanismo de selección** (§6.1): desbloquea search/switch/look + fix #4.
4. **Mecanismo de efectos continuos** (§6.2): desbloquea abilities + tools + stadiums.
5. **Datos** del parsed JSON (trainers/items/stadiums) con el vocabulario ya soportado.
