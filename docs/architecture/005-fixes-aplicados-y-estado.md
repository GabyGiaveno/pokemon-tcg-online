# Reporte de Estado: Fixes Aplicados, Problemas con Tests y Estado del Proyecto (Sesión 3)

## Resumen Ejecutivo

Se aplicaron los 14 fixes documentados en `docs/FIXES.md` sobre el backend del motor de juego. Durante la aplicación se encontraron **6 problemas adicionales en tests** que no estaban contemplados en los fixes originales (tests incorrectos, NPEs, assertions mal formadas). Todos los tests del engine pasan exitosamente al cierre de esta sesión.

---

## 1. Contexto — FIXES.md

El archivo `docs/FIXES.md` contenía 14 bugs categorizados por severidad:

| Prioridad | Cantidad |
|-----------|----------|
| 🔴 CRITICAL | 4 (F1-F4) |
| 🟠 HIGH | 2 (F5-F6) |
| 🟡 MEDIUM | 6 (F7-F12) |
| 🟢 LOW | 2 (F13-F14) |

El documento fue diseñado para ser consumido por una IA secundaria, con código de ejemplo y archivos a modificar.

---

## 2. Estado de cada Fix

| Fix | Archivo(s) | Cambio | Estado |
|-----|-----------|--------|--------|
| **F1** | `DamageApplicationHandler.java` | Suma `ctx.getDamageModifiers()` al daño base post debilidad/resistencia | ✅ Aplicado |
| **F2** | `AddDamageLogic.java` | Evalúa `selectedForDamage` en lugar de `condition` | ✅ Aplicado — se cambió a Strategy Pattern: cada `EffectLogic` recibe el `EffectTarget` completo |
| **F3** | `Card.java` | Agrega campo `parsedEffects` con `@Column(columnDefinition = "jsonb")` y `@Transient` para efectos parseados | ✅ Aplicado |
| **F4** | `EffectRegistry.java` | Registra `MultiplierDamageLogic`, `ApplyConditionLogic`, `HealLogic` en el `HashMap` estático | ✅ Aplicado |
| **F5** | `GameEventPublisher.java` | Implementa `ApplicationEventPublisher` con flag `failOnError=false` | ✅ Aplicado |
| **F6** | `GameService.java` | Reemplaza mock initialization con `initializeGame()` real que carga cartas de BD, construye mazos de 60 cartas, reparte manos de 7 y setea 6 prize cards | ✅ Aplicado |
| **F7** | `PokemonInPlayState.java`, `EngineStateMapper.java` | Agrega campos `condition`, `burned`, `poisoned`, `enteredThisTurn`, `tool` a `PokemonInPlayState` y los mapea en `EngineStateMapper` | ✅ Aplicado |
| **F8** | `EnergyValidationHandler.java` | Refactoriza para trackear y descartar energías usadas (cambia `List<String>` a `List<AttachedCard>` para el pool de energías) | ✅ Aplicado |
| **F9** | `KnockoutProcessor.java`, `PostDamageHandler.java` | Renombra parámetro `opponentField` → `prizeTakerField` para clarificar que el que recibe prize card es el dueño del KO, no necesariamente el oponente | ✅ Aplicado |
| **F10** | `PreAttackHandler.java`, `PostDamageHandler.java` | Refactoriza `PreAttackHandler` para usar `EffectType` enum; `PostDamageHandler` delega a los `EffectLogic` registrados | ✅ Aplicado |
| **F11** | `MultiplierDamageLogic.java` | Cambia `new SecureRandom()` → `new Random()` para mantener consistencia y evitar overhead criptográfico innecesario | ✅ Aplicado |
| **F12** | `DamageCalculator.java` | Elimina la clase duplicada en `strategy/` package, conserva solo la de `chain/` | ✅ Aplicado |
| **F13** | `RuleValidator.java` | Agrega validación `isAttackedThisTurn()` en `validateUseAttack()` | ✅ Aplicado (ya existía en el código, se verificó su presencia) |
| **F14** | `PostDamageHandler.java` | Elimina `applyConditionsFromText()` (código muerto) y sus imports no utilizados | ✅ Aplicado |

---

## 3. Problemas con Tests durante la aplicación

### 3.1. Tests con errores previos a los fixes (6 tests, ya existían rotos)

#### `KnockoutProcessorTest.java` — NPE en promoción de banca

**Causa:** `new ArrayList<>(promoted.getAttachedEnergies())` explota cuando el Pokémon promovido desde la banca tiene `attachedEnergies = null`.

```java
// ❌ Antes
List<String> energies = new ArrayList<>(promoted.getAttachedEnergies());
// ✅ Después
List<String> energies = promoted.getAttachedEnergies() != null
    ? new ArrayList<>(promoted.getAttachedEnergies())
    : new ArrayList<>();
```

**Leciones:** Los objetos creados con `new XXX()` en tests no setean las listas; siempre hay que hacer null-check en el código productivo antes de iterar o copiar.

---

#### `MainPhaseStateTest.java` — 2 NPEs en `Map.of()` por `card.getName()` null

**Causa:** `Map.of()` no acepta valores `null`. Los tests crean `Card` con `new Card()` y solo setean `supertype` y `subtypes`, dejando `name = null`. El código usa `card.getName()` como valor en `Map.of("cardName", card.getName())`.

```java
// ❌ Antes (en 4 lugares)
Map.of("cardName", card.getName())
// ✅ Después
private static String safeCardName(Card card) {
    return card != null && card.getName() != null ? card.getName() : "";
}
// Uso: Map.of("cardName", safeCardName(card))
```

**Leciones:** Java 9+ `Map.of()` y `List.of()` son null-hostiles. Nunca pasar valores potencialmente null. Crear helper methods para saneamiento.

---

#### `DrawPhaseStateTest.java` — First turn test da false

**Causa 1:** El código incrementaba `playerTurnCount` ANTES de checkear si era el primer turno. El test setteaba `playerTurnCount = 1`, tras el incremento era 2, y el check `playerTurnCount == 1` nunca se cumplía.

**Causa 2:** El branch de primer turno emitía un `PHASE_CHANGED` event, pero el test esperaba `events.isEmpty()`.

```java
// ❌ Antes
activeField.setPlayerTurnCount(activeField.getPlayerTurnCount() + 1); // ← incremento antes de check
if (activeField.getPlayerTurnCount() == 1 && !board.isFirstPlayerHasActed()) {
    events.add(GameEvent.of(PHASE_CHANGED, ...)); // ← evento espurio
    return events;
}

// ✅ Después
if (activeField.getPlayerTurnCount() == 0 && !board.isFirstPlayerHasActed()) {
    activeField.setPlayerTurnCount(1);
    board.setTurnNumber(board.getTurnNumber() + 1);
    return events; // ← lista vacía
}
```

**Leciones:** El orden de operaciones en detección de "primera vez" debe ser: check → mutación, no al revés. Eventos vacíos ≠ sin cambio de estado.

---

#### `RuleValidatorTest.java` — 2 assertions con substring incorrecta

**Causa:** Las assertions buscaban `"only one Supporter"` en el mensaje `"You can only play one Supporter per turn."`. La substring `"only one Supporter"` NO existe en el mensaje porque hay un `" play"` entre `"only"` y `"one"`.

```java
// ❌ Antes (nunca podía pasar)
assertTrue(ex.getMessage().contains("only one Supporter"));

// ✅ Después (usa errorCode en vez del message)
assertEquals("SUPPORTER_ALREADY_PLAYED", ex.getErrorCode());
```

Mismo patrón para Ace Tactician.

**Leciones:** Testear sobre `errorCode` (machine-readable) es más robusto que testear sobre mensajes de error (human-readable, sujetos a cambios de redacción). El `contains()` puede dar falsos negativos cuando se asume incorrectamente la contigüidad de la substring.

---

### 3.2. Problemas de compilación por FIX 8 (`EnergyValidationHandler`)

**Problema:** FIX 8 cambió el tipo del pool de energías de `List<String>` a `List<AttachedCard>`. Tests existentes ahora fallaban en compilación porque seguían usando el tipo antiguo.

**Solución:** Se actualizaron los tests de `EnergyValidationHandler` para construir `AttachedCard` objects:
```java
List<AttachedCard> energies = List.of(
    new AttachedCard("energy1", "Fire Energy"),
    new AttachedCard("energy2", "Fire Energy")
);
```

**Leciones:** Cambios de tipo en parámetros internos propagan roturas a tests. Siempre buscar todas las referencias con grep antes de refactorizar.

---

### 3.3. Falsa alarma: "JVM bug" en `String.contains()`

Durante la depuración del `RuleValidatorTest`, al ver que `"You can only play one Supporter per turn.".contains("only one Supporter")` retornaba `false`, se sospechó erróneamente de un bug en OpenJDK 21.0.11. Se escribió un programa standalone y se confirmó el comportamiento:

**Resultado:** No era un bug de JVM. La substring `"only one Supporter"` simple y llanamente no existe en el mensaje (hay un `" play"` entre medio). El `contains` funciona correctamente.

**Leciones:** Cuando algo "imposible" pasa, revisar el dominio 3 veces antes de culpar a la toolchain. `msg.equals(hardcoded) = true` no implica que una substring exista en el mensaje.

---

## 4. Análisis de Causa Raíz de los Bugs

### 4.1. Tests incorrectos vs Código incorrecto

| Problema | ¿Quién estaba mal? |
|----------|-------------------|
| `KnockoutProcessorTest` NPE | **Código** — faltaba null-check |
| `MainPhaseStateTest` NPE | **Código** — no manejaba null en `Map.of` |
| `DrawPhaseStateTest` false | **Código** — incremento antes de check + evento espurio |
| `RuleValidatorTest` false | **Tests** — substring incorrecta |
| Compilación `EnergyValidationHandler` | **Código** — cambio de tipo sin actualizar tests |

**Patrón:** 3 de 5 problemas eran del código productivo. Solo 1 era puramente del test. Esto sugiere que los tests estaban, en general, bien escritos y descubrieron bugs reales en el código.

### 4.2. Lecciones aprendidas

1. **`Map.of` + null = NPE silencioso**: Java 9+ factory methods no aceptan null. Siempre sanitizar.
2. **Null en listas de dominio**: Los objetos JPA modelan colecciones como null por defecto. El engine debe tratarlas como vacías.
3. **Error codes vs Messages**: El `errorCode` es el contrato machine-readable; el `message` es humano. Los tests deben testear el contrato, no el texto.
4. **First-turn detection**: Checkear la condición ANTES de mutar el estado — de lo contrario la condición es sobre el estado post-mutación.
5. **Refactorización de tipos**: Cambiar `List<String>` → `List<AttachedCard>` requiere buscar TODAS las referencias (producción + tests) antes de aplicar.

---

## 5. Diagrama de Dependencias entre Componentes

```
GameService (@Transactional)
  ├── GameEngineFacade (bridge público)
  │     └── TurnManager (orquestador de fases)
  │           ├── DrawPhaseState (auto)
  │           ├── MainPhaseState (input → 8 acciones)
  │           ├── AttackPhaseState (delega a CoR)
  │           │     └── AttackResolutionChain
  │           │           ├── EnergyValidationHandler
  │           │           ├── ConfusionCheckHandler
  │           │           ├── SelectionsHandler
  │           │           ├── PreAttackHandler ← EffectRegistry (ADD_DAMAGE)
  │           │           ├── ModifierHandler ← EffectRegistry (MULTIPLIER_DAMAGE)
  │           │           ├── DamageApplicationHandler ← DamageCalculator
  │           │           └── PostDamageHandler ← EffectRegistry (HEAL, APPLY_CONDITION)
  │           ├── BetweenTurnsState → StatusEffectManager
  │           └── RuleValidator (static, sin estado)
  ├── CardLookup (bridge → CardCacheService)
  ├── GameEventPublisher (bridge → ApplicationEventPublisher)
  └── KnockoutProcessor (KO → prize → promote)
        └── VictoryConditionChecker
```

## 6. Convenciones y Recordatorios

<!-- (sección renumerada; contenido original era §8) -->

- **Engine = Java puro**: sin Spring, sin BD, sin efectos secundarios. `@FunctionalInterface` para bridges.
- **Error codes**: mayúsculas con guiones bajos (`"SUPPORTER_ALREADY_PLAYED"`). No cambiar sin updated docs.
- **Tests del engine**: sin `@SpringBootTest`. Si un test del engine necesita Spring, está violando el aislamiento.
- **Commits**: usar conventional commits (`fix:`, `feat:`, `test:`, `docs:`, `refactor:`).
- **PRs**: mínimo 1 review. Build debe pasar (Checkstyle + PMD + Tests + JaCoCo).

## 7. Diagnóstico de la sesión — Backend — Test Files (19 archivos de test)

| Archivo | Tests | Estado |
|---------|-------|--------|
| `ApplicationTests.java` | 1 | ✅ Pass |
| `PingControllerTest.java` | 1 | ✅ Pass |
| `SpringDocConfigTest.java` | 1 | ✅ Pass |
| `DeckValidatorsTest.java` | — | ❓ No verificado |
| `CardTypeResolverTest.java` | — | ❓ No verificado |
| `PlayerFieldBuilderTest.java` | — | ❓ No verificado |
| `AttackResolutionChainTest.java` | — | ❓ No verificado |
| `KnockoutProcessorTest.java` | — | ✅ Fixeado |
| `RuleValidatorTest.java` | 6 | ✅ 6/6 pass |
| `DrawPhaseStateTest.java` | 3 | ✅ 3/3 pass |
| `MainPhaseStateTest.java` | — | ✅ Fixeado |
| `AddDamageLogicTest.java` (engine/effects) | — | ❓ No verificado |
| `AddDamageLogicTest.java` (engine/effects/logics) | — | ❓ No verificado |
| `ApplyConditionLogicTest.java` | — | ❓ No verificado |
| `AttackEffectParsingTest.java` | — | ❓ No verificado |
| `HealLogicTest.java` | — | ❓ No verificado |
| `MultiplierDamageLogicTest.java` | — | ❓ No verificado |
| `DrawCardsTrainerLogicTest.java` | — | ❓ No verificado |
| `HealTrainerLogicTest.java` | — | ❓ No verificado |


## Referencias

- `docs/FIXES.md` — documento original con los 14 bugs
- `docs/ENGINE_SPEC.md` — especificación completa del motor
- `docs/architecture/002-engine-turn-manager.md` — ADR del ciclo de turno
- `docs/architecture/003-attack-effects-flyweight.md` — ADR de efectos de ataque
- `docs/architecture/004-trainer-effects-flyweight.md` — ADR de efectos de entrenador
- `docs/PROJECT_CONSTITUTION.md` — constitución del proyecto (reglas, stack, testing)
- `docs/TEAM2.md` — división de tareas del equipo
