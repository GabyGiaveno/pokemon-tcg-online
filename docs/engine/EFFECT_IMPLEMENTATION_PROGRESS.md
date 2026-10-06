# Progreso de implementación de efectos — Pokémon TCG (XY1)

> Log vivo de avance. Se actualiza a medida que cada bloque atraviesa el ciclo SDD.
> Inventario de referencia: `docs/EFFECT_IMPLEMENTATION_INVENTORY.md`.
> Artefactos SDD: `openspec/changes/<bloque>/`.

## Tablero de bloques

| # | Bloque | Alcance | Estado SDD | Tests |
|---|--------|---------|------------|-------|
| 1 | `robust-effect-parser` | Parser resiliente + vocabulario (catch-all) | ✅ archivado | ✅ verde |
| 2 | `effect-execution-trainers` | Logics autocontenidas + pipeline trainers + CoR de trainers | ✅ hecho (apply+verify) | ✅ 130 verdes |
| 3 | `player-selection-mechanism` | Mecanismo de input + fix #4 (CHOOSE_ACTIVE al KO) | 🟢 completo a nivel engine (borde DTO diferido) | ✅ engine verde |
| 4 | abilities **tier 1** (continuación del change 2) | 6 habilidades, 3 familias: activadas/disparadas/continuas | ✅ hecho (apply+verify 2026-06-11) | ✅ 217 verdes (24 nuevos) |
| 4b | abilities tier 2 + tools/stadiums | Las 6 con selección (Water Shuriken, Stance Change, Fairy Transfer, Drive Off, Upside-Down Evo) + tools data-driven | ⏳ pendiente (requiere resumable/selección) | — |

---

## Bloque 1 — `robust-effect-parser` ✅

**Objetivo:** que un `type` desconocido no haga perder TODOS los efectos de una carta.

**Decisión de diseño:** `defaultImpl = UnknownEffect` (catch-all inerte) en el `@JsonTypeInfo` de
`AttackEffect`, en vez de crear 8 DTOs o re-mapear el JSON. Resuelve los 8 huérfanos y los anidados de
una, sin tocar datos.

**Cambios:**
- `models/cards/effects/UnknownEffect.java` — **nuevo**, catch-all inerte (captura `type` para log).
- `models/cards/effects/AttackEffect.java` — `@JsonTypeInfo(visible=true, defaultImpl=UnknownEffect.class)`
  + `@JsonIgnoreProperties(ignoreUnknown=true)` en la base.
- `engine/chain/AttackParser.java` — `MAPPER` con `FAIL_ON_UNKNOWN_PROPERTIES=false` + log de tipos
  omitidos (helper recursivo `collectUnknownTypes`, cubre anidados de COIN_FLIP y PASSIVE_ABILITY).
- `test/.../AttackParserTest.java` — 5 tests (REQ-1.1, REQ-1.2 ×8, REQ-2, REQ-3).

**Verify:** `cd BE && ./mvnw test` → verde, ningún fallo. (Riesgo descartado: `AttackEffectParsingTest`
con mapper plano + `visible=true` sigue pasando por la herencia de `@JsonIgnoreProperties`.)

**Pendiente operativo:** commit (working tree sin commitear aún).

---

## Bloque 2 — `effect-execution-trainers` 🟢 planificado

Planificación SDD completa (proposal→spec→design→tasks) en `openspec/changes/effect-execution-trainers/`.

**Decisiones de design:** D1 COIN_FLIP pre-damage (flip único, dispatch inline) · D2 scripts `.py` por tipo
+ JSON aplicado directo (Python no corre acá) · D3 5 trainers autocontenidos (Sycamore, Shauna, Red Card,
Team Flare Grunt, Roller Skates) · D4 la cadena ejecuta efectos, el bookkeeping de la carta queda en `MainPhaseState`.

**Apply en 4 work-units (1 commit c/u):** WU-A revivir COIN_FLIP/DISCARD_ENERGY · WU-B stubs autocontenidos ·
WU-C pipeline de datos (parser + carga + scripts + JSON) · WU-D subtipos + CoR + wire.

- ✅ **WU-A** hecho: `CoinFlipLogic.isPreDamage()=true`, `DiscardEnergyLogic.isPostDamage()=true` + tests (`CoinFlipLogicTest`, `DiscardEnergyLogicTest`).
- ✅ **WU-B** hecho: `ShuffleHandLogic`, `DrawUntilHandSizeLogic`, `DamageCountersLogic` (sin elección) implementados + tests.
- `ENGINE_GUIDE.md` §16 + ejemplos de flujo 3 y 4 actualizados (COIN_FLIP pre-daño, DISCARD_ENERGY post-daño).
- ✅ **Tests WU-A/B verdes** (`./mvnw test` → BUILD SUCCESS; 24 tests de las clases nuevas, suite completa sin fallos). Fix menor: los tests deben setear `cardId` (el código usa `Map.of` con `cardId`, null-hostil).
- ✅ **WU-C** hecho + verde: JSON `xy1_parsed.json` con `trainerEffects` en las 5 cartas (Sycamore, Shauna, Red Card, Roller Skates, Team Flare Grunt); `UnknownTrainerEffect`; `TrainerEffect` con `defaultImpl`+`visible`+`@JsonIgnoreProperties`; `TrainerEffectParser`; `PokemonTCGApiService` incluye `trainerEffects`. `.py` ilustrativo en `docs/abilityParsing/build_trainers_parsed.py` (documenta el método). Tests `TrainerEffectParserTest` verdes.
- ✅ **WU-D** hecho + verde: 4 DTOs (`DiscardHandDraw/ShuffleHand/DiscardEnergy/CoinFlip` TrainerEffect) + 4 logics + registro; CoR `engine/effects/trainers/chain/` (`TrainerContext`, `TrainerHandler`, `TrainerResolutionChain`, `TrainerEffectExecutionHandler`); `MainPhaseState` delega en la cadena (loop viejo eliminado). **5 trainers jugables.**
- ✅ **Bloque 2 COMPLETO** — `./mvnw test` → BUILD SUCCESS, **130 tests**.

## Bloque 3 — `player-selection-mechanism` 🟡 en curso

Planificación SDD completa en `openspec/changes/player-selection-mechanism/` (proposal→spec→design→tasks).

**Decisiones de design (cerradas):**
- D1 Reificar la continuación como `SelectionType` (enum) + datos planos en `PendingSelection` — **NO** lambda/`Runnable`: el `resolveSelection` llega en un request posterior y el board se reconstruye desde storage; una lambda no sobrevive a la serialización.
- D2 Corte **cooperativo en el orquestador**: el sitio profundo (`KnockoutProcessor`) solo setea `pendingSelection` y no actúa; `TurnManager` detecta y corta. Resume **sin puntero explícito**: ramifica por `board.getCurrentPhase()` (ATTACK→`completeTurn`; BETWEEN_TURNS→switch+beginTurn).
- Hallazgo: `processIfKnockedOut` se llama desde **DOS** sitios (`PostDamageHandler:84` en ataque y `BetweenTurnsState:67` por veneno/quemadura). Ambos pausan; el mecanismo es agnóstico al sitio.
- `RESOLVE_SELECTION` reutiliza `ActionRequest.benchIndex` (no se agrega campo).

**Apply (completo a nivel engine):**
- ✅ **Fase 1**: `SelectionType`, `PendingSelection`, campo en `BoardState`, `RESOLVE_SELECTION`.
- ✅ **Fase 2 (core)**: `KnockoutProcessor.promote()` extraído; el KO setea `pendingSelection` en vez de auto-promover; guarda + 2 cortes en `TurnManager`; `resolveSelection`. **SRP victoria**: el KO ya no decide el juego — lo hace el orquestador vía `VictoryConditionChecker` (que pasó de huérfano a fuente única), con `VictoryReason` (motivo).
- ✅ **Refactor SRP**: `SelectionResolver` (valida + aplica la selección, dispatch por `SelectionType`; prepara O/C para los próximos tipos). DIP: `TurnManager` recibe checker + resolver por constructor.
- ✅ **Fase 3 (tests)**: `KnockoutProcessorTest`, `VictoryConditionCheckerTest`, `SelectionResolverTest` (nuevo), `TurnManagerTest` (mocks del checker/resolver), `TurnManagerSelectionIntegrationTest` (nuevo, integración real KO ataque + veneno), `AttackResolutionChainTest` ajustado. **Engine 100% verde.**
- ⏳ **Fase 5 (borde DTO) — DIFERIDA a otro desarrollador**: exponer `pendingSelection` en `GET /state` + round-trip en `EngineStateMapper` + `finishedReason` por motivo. Cruza la frontera engine↔DTO y toca contrato de API. Ver `openspec/changes/player-selection-mechanism/tasks.md` §Fase 5.

**Deuda pre-existente (ajena a este bloque):** `GameEngineFacadeTest` + `GameServiceTest` fallan por el conflicto semántico engine↔fachada (fixtures sin energía + round-trip del mapper). Documentado en `NEXT_SESSION.md`.

## Bloque 4 tier 1 — Habilidades ✅ (2026-06-11, continuación del change `effect-execution-trainers`)

**Arquitectura (design D5-D9):** Flyweight + 3 mecanismos por familia (State descartado con
rationale — ver proposal). `AbilityParser` abre por fin el bloque `"abilities"` de
`Card.parsedEffects` (nadie lo leía); `PassiveAbilityEffect` ya no pierde las `conditions`;
4 subtipos anidados nuevos (`COIN_FLIP_DAMAGE`, `REDUCE_DAMAGE`, `RESTRICT_ITEMS`,
`IMMUNE_TO_CONDITIONS`); `AbilityConditionEvaluator` fail-closed.

- **Activadas** — `ActionType.USE_ABILITY` → `MainPhaseState.handleUseAbility` →
  `AbilityActivationResolver`. Once-per-turn en `TurnFlags.abilitiesUsedThisTurn`.
  Jugable: **Mystical Fire** (Delphox). Tier 2 se rechaza con `ABILITY_NOT_SUPPORTED`.
- **Disparadas** — `AbilityTriggerResolver` en `PostDamageHandler` (antes del doble KO check):
  **Spiky Shield** (retroceso 30, "even if KO"), **Destiny Burst** (flip → 50 al atacante).
- **Continuas** — `ContinuousAbilityQuery` como guards: **Forest's Curse** (`ITEMS_LOCKED` en
  `handlePlayItem`), **Fur Coat** (−20 post-W&R en `DamageApplicationHandler`),
  **Sweet Veil** (inmunidad a condiciones en `ApplyConditionLogic`).

**Verify:** suite completa **217 tests BUILD SUCCESS** (4 clases de test nuevas, 24 tests).
**Limitación conocida:** `abilitiesUsedThisTurn` no se persiste por `EngineStateMapper`
(misma deuda BUG-11/A1 del review — se arregla con la decisión de persistencia del TODO A1).

## Próxima sesión
Opciones: **(a)** cerrar el borde DTO (Fase 5) + persistencia (TODO A1 del review — desbloquea
todo lo E2E); **(b)** abilities tier 2 + los 4 stubs de ataque con selección vía plan resumable;
**(c)** tools/stadiums data-driven (resto del Bloque 4).
