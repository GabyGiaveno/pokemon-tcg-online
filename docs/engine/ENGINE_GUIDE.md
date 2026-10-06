# Guía Completa del Motor de Juego — Pokémon TCG Simulator
> Sesiones 2–8 · Branch: `Produccion`
> Última actualización: 2026-06-11 (habilidades tier 1 + player-selection completo)
>
> **Si es tu primer día en este repo, no arranques por la sección 1.** Leé primero la
> [Sección 0 — Bienvenida](#0-bienvenida-empezá-por-acá) abajo. Te da el modelo mental completo
> en lenguaje humano antes de meterte en el detalle técnico. La sección 1 en adelante asume que
> ya entendés "de qué va" el motor.

---

## Tabla de contenidos

0. [Bienvenida — empezá por acá](#0-bienvenida-empezá-por-acá) ⭐ *para nuevas incorporaciones*
1. [Arquitectura general](#1-arquitectura-general)
2. [Modelos de estado del juego](#2-modelos-de-estado-del-juego)
3. [Patrones de diseño aplicados](#3-patrones-de-diseño-aplicados)
4. [Flujo completo de un turno](#4-flujo-completo-de-un-turno)
5. [TurnManager — el orquestador](#5-turnmanager--el-orquestador)
6. [GamePhaseState — patrón State](#6-gamephasestate--patrón-state)
7. [RuleValidator — validador estático](#7-rulevalidator--validador-estático)
8. [StatusEffectManager — condiciones especiales](#8-statuseffectmanager--condiciones-especiales)
9. [KnockoutProcessor — procesamiento de KO](#9-knockoutprocessor--procesamiento-de-ko)
10. [VictoryConditionChecker — condiciones de victoria](#10-victoryconditionchecker--condiciones-de-victoria)
11. [CardLookup — bridge hacia Spring](#11-cardlookup--bridge-hacia-spring)
12. [GameEngineFacade — punto de entrada Spring](#12-gameenginefacade--punto-de-entrada-spring)
13. [AttackResolutionChain — cadena de responsabilidad](#13-attackresolutionchain--cadena-de-responsabilidad)
14. [Los 7 handlers del pipeline de ataque](#14-los-7-handlers-del-pipeline-de-ataque)
15. [DamageCalculator — fórmula de daño](#15-damagecalculator--fórmula-de-daño)
16. [Sistema de efectos — Flyweight pattern](#16-sistema-de-efectos--flyweight-pattern)
17. [AttackTextParser — ELIMINADO](#17-attacktextparser--eliminado)
18. [AttackData / AttackParser / AttackContext](#18-attackdata--attackparser--attackcontext)
19. [DTOs: ActionRequest y ActionResult](#19-dtos-actionrequest-y-actionresult)
20. [Eventos: GameEvent y GameEventType](#20-eventos-gameevent-y-gameeventtype)
21. [TODOs conocidos y próximos pasos](#21-todos-conocidos-y-próximos-pasos)
22. [Flujos de resolución — ejemplos reales (XY1)](#22-flujos-de-resolución--ejemplos-reales-xy1)
23. [ConditionStrategy — el patrón Strategy real del engine](#23-conditionstrategy--el-patrón-strategy-real-del-engine)
24. [Pipeline de Trainers — efectos de cartas de entrenador](#24-pipeline-de-trainers--efectos-de-cartas-de-entrenador)
25. [EngineStateMapper — puente de persistencia (dominio ↔ BD)](#25-enginestatemapper--puente-de-persistencia-dominio--bd)
26. [Player Selection Mechanism — ✅ completo a nivel engine](#26-player-selection-mechanism--en-curso)
27. [Glosario y mapa mental rápido](#27-glosario-y-mapa-mental-rápido)
28. [Sistema de Habilidades (abilities)](#28-sistema-de-habilidades-abilities) ⭐ *nuevo 2026-06-11*

---

## 0. Bienvenida — empezá por acá

> Esta sección es para que **cualquiera** del equipo entienda el motor sin haber escrito una sola
> línea de él. Nada de jerga gratis: primero la intuición, después el nombre técnico.

### ¿Qué es "el engine"?

Imaginá un **árbitro de Pokémon TCG**. No es el tablero, no es la pantalla, no es la base de datos.
Es la persona que conoce las reglas de memoria y dice: *"sí, podés hacer eso"*, *"no, todavía no
podés atacar"*, *"ese Pokémon quedó fuera de combate, tomá una carta de premio"*. El **engine es ese
árbitro, escrito en Java puro**.

El frontend (la UI) y la base de datos son **el mundo de afuera**. El engine no les habla
directamente. Recibe una **acción** (`"jugador 3 quiere atacar con el ataque 0"`), la valida contra
las reglas, muta el estado del juego, y devuelve una **lista de cosas que pasaron** (eventos:
`ATTACK_DECLARED`, `DAMAGE_DEALT`, `POKEMON_KNOCKED_OUT`...). El que pintó eso en pantalla o lo guardó
en disco es problema de otra capa.

```
   [ Frontend / REST ]                [ Base de datos ]
           │  ActionRequest                   ▲  (estado persistido)
           ▼                                  │
   ┌─────────────────────────────────────────────────────┐
   │  GameEngineFacade  (única clase con @Service Spring)  │
   │      │  delega todo a ...                             │
   │      ▼                                                │
   │  TurnManager  ── orquesta el ciclo del turno ──►      │
   │      ├─ valida con RuleValidator                      │
   │      ├─ delega cada fase a un GamePhaseState          │
   │      └─ resuelve ataques con AttackResolutionChain    │
   │                                                       │
   │  TODO ESTO ES JAVA PURO — sin Spring, 100% testeable  │
   └─────────────────────────────────────────────────────┘
```

### Las 3 reglas de oro del diseño

Si te llevás solo tres ideas de toda la guía, que sean estas:

| # | Regla | Por qué importa |
|---|-------|-----------------|
| 1 | **El engine no sabe que Spring existe.** | No hay `@Autowired` adentro. La única conexión con el mundo es `CardLookup` (una lambda que busca cartas). Resultado: podés testear TODO el motor con un `Map` en memoria, sin levantar el servidor. |
| 2 | **El estado es DATOS, no comportamiento.** | `BoardState` y compañía son POJOs con `@Builder`. No tienen lógica. La lógica vive en clases sin estado (validators, handlers, logics). Esto los hace serializables y predecibles. |
| 3 | **Una acción del jugador devuelve TODOS los eventos que disparó.** | Un solo `USE_ATTACK` puede producir: ataque declarado → daño → KO → premio tomado → carta robada del rival. El cliente recibe la película completa en una respuesta y la reproduce en orden. |

### El recorrido de una acción (en cámara lenta, sin código)

1. Llega un `ActionRequest` ("quiero atacar con el ataque índice 0").
2. `GameEngineFacade` busca la partida en su registry en memoria y se la pasa a `TurnManager`.
3. `TurnManager` chequea lo obvio: *¿es tu turno? ¿el juego sigue vivo? ¿estás en la fase correcta?*
4. Si la acción es un ataque, delega al **pipeline de ataque** (`AttackResolutionChain`): una fila de
   7 "inspectores" (handlers) que se pasan el ataque uno tras otro — ¿tenés energía? ¿estás
   confundido? ¿a quién apuntás? aplicar efectos previos, modificadores, el daño, y por último los KO.
5. Cuando el ataque termina, el turno se **auto-completa**: condiciones especiales entre turnos
   (veneno, quemadura, sueño...), cambio de jugador, y arranca el turno del rival.
6. Toda esa cascada se devuelve como una lista de `GameEvent`.

### Los patrones que vas a ver una y otra vez

No te asustes con los nombres. Cada patrón resuelve UN problema concreto:

| Patrón | Traducción humana | Dónde |
|--------|-------------------|-------|
| **State** | "Cada fase del turno sabe qué hacer consigo misma; nadie hace `switch(fase)` gigante" | `GamePhaseState` + 4 fases |
| **Chain of Responsibility** | "Una fila de inspectores; cada uno hace su parte y pasa el testigo" | `AttackResolutionChain`, `TrainerResolutionChain` |
| **Flyweight** | "Una sola instancia compartida de cada lógica de efecto, sin estado" | `EffectRegistry`, `TrainerEffectRegistry` |
| **Strategy** | "Distintas formas de responder una pregunta sí/no, intercambiables" | `ConditionStrategy` (ver §23) |
| **Facade** | "Una puerta de entrada simple que esconde toda la complejidad" | `GameEngineFacade` |
| **Factory / Builder** | "Construir objetos bien armados sin constructores de 10 parámetros" | `PokemonFactory`, Lombok `@Builder` |

### Estado del proyecto, en una línea

El **flujo de turnos, validaciones, ataques con daño, condiciones especiales y trainers básicos
funcionan y están testeados** (130+ tests verdes en sesión 6). Lo que está **a mitad de camino** es
el **mecanismo de selección del jugador** (elegir qué Pokémon promover tras un KO, qué carta buscar,
etc.) — ver [§26](#26-player-selection-mechanism--en-curso), que documenta honestamente qué está
hecho y qué no. Muchos efectos de cartas están **registrados pero todavía son stubs** — la fuente de
verdad de qué efecto funciona de verdad es `docs/engine/EFFECT_IMPLEMENTATION_INVENTORY.md`.

### Cómo leer el resto de esta guía

- **Sección 1–4**: la foto grande (arquitectura, modelos de estado, patrones, flujo del turno). Leelas en orden.
- **Secciones 5–15**: cada componente en detalle. Consultá la que necesites.
- **Sección 16 + 23–24**: el sistema de efectos (ataques, condiciones dinámicas, trainers).
- **Sección 22**: ejemplos reales de cartas XY1 resueltas paso a paso — **el mejor lugar para "ver" el motor en acción**.
- **Sección 26**: lo que estamos construyendo ahora mismo.
- **Sección 27**: glosario, por si te perdés con un término.

---

## 1. Arquitectura general

### Paquetes del engine

> **Leyenda:** ✅ implementado y testeado · 🟡 implementado parcial / con TODO · 🔴 stub (solo registrado, no hace lógica real) · 🚧 en construcción ahora mismo

```
engine/
├── GameEngineFacade.java          ← @Service Spring — facade con in-memory game registry ✅
├── TurnManager.java               ← orquestador principal ✅ (🚧 le falta el wiring de selección — §26)
├── RuleValidator.java             ← validaciones estáticas puras ✅
├── StatusEffectManager.java       ← condiciones especiales entre turnos ✅
├── KnockoutProcessor.java         ← procesamiento de KO ✅ EX/MEGA = 2 prizes (🚧 ahora setea pendingSelection — §26)
├── VictoryConditionChecker.java   ← condiciones de victoria ✅ (🚧 pasando de "safety net" a fuente única de decisión — §26)
├── CardLookup.java                ← @FunctionalInterface hacia Spring ✅
├── state/
│   ├── GamePhaseState.java        ← interfaz (patrón State) ✅
│   ├── DrawPhaseState.java        ← fase DRAW (automática) ✅
│   ├── MainPhaseState.java        ← fase MAIN (interactiva, 8 acciones) ✅ (trainers ya cableados — §24)
│   ├── AttackPhaseState.java      ← fase ATTACK (delega a la cadena) ✅
│   └── BetweenTurnsState.java     ← fase BETWEEN_TURNS (automática) ✅
├── chain/                          ← pipeline de ATAQUES
│   ├── AttackHandler.java          ← interfaz base (Chain of Responsibility) ✅
│   ├── AttackResolutionChain.java  ← orquestador de los 7 handlers ✅
│   ├── AttackContext.java          ← contexto mutable del pipeline ✅
│   ├── AttackData.java             ← POJO de un ataque parseado desde JSONB ✅
│   ├── AttackParser.java           ← deserialización Jackson de attacks + merge de parsedEffects ✅
│   ├── DamageCalculator.java       ← base → weakness → resistance → round-to-10 ✅ (TODO: defender_mods)
│   └── handlers/
│       ├── EnergyValidationHandler.java   ← handler 1 ✅
│       ├── ConfusionCheckHandler.java     ← handler 2 ✅
│       ├── SelectionsHandler.java         ← handler 3 ✅ (TODO: bench targeting → §26)
│       ├── PreAttackHandler.java          ← handler 4 ✅ ejecuta efectos isPreDamage()
│       ├── ModifierHandler.java           ← handler 5 ✅ Muscle Band
│       ├── DamageApplicationHandler.java  ← handler 6 ✅
│       └── PostDamageHandler.java         ← handler 7 alwaysRun ✅
├── effects/                        ← sistema de EFECTOS (Flyweight)
│   ├── EffectLogic.java                   ← interfaz Flyweight de lógica ✅
│   ├── EffectRegistry.java                ← singleton clase→lógica ✅ (15 registradas; varias 🔴 stub — ver §16 + inventario)
│   ├── logics/                            ← 15 lógicas de ataque (estado real en EFFECT_IMPLEMENTATION_INVENTORY.md)
│   │   ├── AddDamageLogic ✅ · ApplyConditionLogic ✅ · HealLogic ✅ · MultiplierDamageLogic ✅
│   │   ├── CoinFlipLogic ✅ · DiscardEnergyLogic ✅
│   │   ├── DamageCountersLogic 🟡 · ShuffleHandLogic 🟡 · DrawUntilHandSizeLogic 🟡
│   │   └── DamageToBenchLogic 🔴 · SearchDeckLogic 🔴 · SwitchPokemonLogic 🔴 ·
│   │       LookAtDeckLogic 🔴 · PreventDamageLogic 🔴 · RestrictLogic 🔴   (requieren selección/efectos diferidos)
│   ├── conditions/                        ← patrón Strategy real del engine (§23) ✅
│   │   ├── ConditionStrategy.java         ← interfaz Strategy
│   │   ├── ConditionRegistry.java         ← singleton key→strategy (6 registradas)
│   │   └── OpponentIsGrass / DefenderIsEx / DefenderHasDamageCounters /
│   │       DefenderHasSpecialCondition / SelfHasEnergyPsychic / LunatoneOnBench
│   └── trainers/                          ← sistema de TRAINERS (§24) ✅ cableado
│       ├── TrainerEffectLogic.java        ← interfaz Flyweight trainers
│       ├── TrainerEffectRegistry.java     ← singleton clase→lógica (6 registradas)
│       ├── TrainerEffectParser.java       ← parsea parsedEffects de trainers
│       ├── DrawCardsTrainerLogic ✅ · HealTrainerLogic ✅ · DiscardHandDrawTrainerLogic ✅
│       ├── ShuffleHandTrainerLogic ✅ · DiscardEnergyTrainerLogic ✅ · CoinFlipTrainerLogic ✅
│       └── chain/                         ← pipeline de trainers (espejo del de ataques)
│           ├── TrainerResolutionChain.java     ← orquestador
│           ├── TrainerHandler.java             ← interfaz base
│           ├── TrainerContext.java             ← contexto mutable
│           └── TrainerEffectExecutionHandler.java ← único handler del Bloque 2
├── factories/
│   ├── CardTypeResolver.java              ← Card entity → CardType enum ✅
│   ├── PokemonFactory.java                ← Card → ActivePokemon/BenchPokemon ✅
│   └── PlayerFieldBuilder.java            ← construye campo inicial ✅
└── mappers/
    └── EngineStateMapper.java            ← puente dominio BoardState ↔ entidad de BD (§25) ✅

models/game/                          ← (fuera de engine/) modelos de estado del juego
├── BoardState, PlayerField, ActivePokemon, BenchPokemon, TurnFlags, AttachedCard  ✅
├── PendingSelection.java             ← 🚧 selección pendiente (player-selection — §26)
└── SelectionType.java                ← 🚧 enum (CHOOSE_ACTIVE_ON_KO) (§26)
```

> **Nota sesión 4:** se eliminó `engine/strategy/` (interfaces vacías sin importadores — dead code).
>
> **Nota sesión 5:** se eliminó `AttackTextParser.java` (dead code). El flujo real usa
> `AttackParser.parse(attacks, parsedEffects)` con datos pre-cargados desde `xy1_parsed.json`.
>
> **Nota sesión 6 + actual:** se agregaron el subsistema `conditions/` (Strategy, §23), el pipeline
> completo de `trainers/chain/` (§24, ya cableado en `MainPhaseState`), el `EngineStateMapper` de
> persistencia (§25), y los modelos `PendingSelection`/`SelectionType` del mecanismo de selección
> (§26, **en curso**). `EffectRegistry` pasó de 6 a 15 lógicas registradas — pero *registrada ≠
> funcional*: varias son stubs (ver §16).

### Principio de aislamiento

El engine es **puro Java — sin Spring**. No hay `@Component`, `@Service`, ni `@Autowired` en ninguna clase del engine. La única conexión con Spring es a través de `CardLookup`, que es una lambda (`cardCacheService::findById`) inyectada desde fuera. Esto permite testear todo el engine sin levantar un `ApplicationContext`.

---

## 2. Modelos de estado del juego

### `BoardState`

```
BoardState
├── matchState: GameStatus        WAITING | SETUP | ACTIVE | FINISHED
├── currentPhase: TurnPhase       DRAW | MAIN | ATTACK | BETWEEN_TURNS
├── currentPlayerId: Long
├── turnNumber: int               global; incrementa en cada DRAW phase
├── firstPlayerHasActed: boolean  true cuando el jugador 1 completó su primer turno
├── winnerId: Long                null mientras el juego sigue
├── player1Field: PlayerField
└── player2Field: PlayerField
```

### `PlayerField`

```
PlayerField
├── playerId: Long
├── activePokemon: ActivePokemon
├── bench: List<BenchPokemon>     máx. 5
├── hand: List<String>            cardIds en mano
├── deck: List<String>            cardIds en mazo (nunca expuesto al rival)
├── prizeCards: List<String>      6 al inicio; se reducen al tomar prizes
├── discardPile: List<String>     visible para ambos jugadores
├── turnFlags: TurnFlags          flags booleanos por turno
└── playerTurnCount: int          cuántas fases DRAW completó este jugador
```

**`playerTurnCount == 1`** significa que es el primer turno del jugador. En ese estado no puede evolucionar ni atacar.

### `TurnFlags`

Flags booleanos reseteados al inicio de cada fase DRAW:

| Flag | Restricción |
|------|-------------|
| `energyAttachedThisTurn` | máx. 1 energía por turno |
| `retreatedThisTurn` | máx. 1 retirada por turno |
| `supporterPlayedThisTurn` | máx. 1 Supporter/AS Táctico por turno |
| `attackedThisTurn` | si es true, el turno debe terminar |

### `ActivePokemon`

```
ActivePokemon
├── cardId: String
├── maxHp: int
├── currentHp: int
├── attachedEnergies: List<AttachedCard>
├── tool: AttachedCard            null si no tiene tool
├── condition: SpecialCondition   NONE | ASLEEP | CONFUSED | PARALYZED
├── isBurned: boolean             independiente de condition
├── isPoisoned: boolean           independiente de condition
└── enteredThisTurn: boolean      true si entró a juego este turno
```

**¡Importante!** `condition` NUNCA debe ser BURNED ni POISONED — esas condiciones son independientes y se manejan como booleanos. El enum `SpecialCondition` los incluye por completitud pero en `ActivePokemon` siempre se usan los campos `isBurned`/`isPoisoned`.

### `BenchPokemon`

```
BenchPokemon
├── cardId: String
├── maxHp: int
├── currentHp: int
├── attachedEnergies: List<AttachedCard>
├── tool: AttachedCard
└── enteredThisTurn: boolean
```

`BenchPokemon` no tiene `condition` porque las condiciones solo afectan al Active.

---

## 3. Patrones de diseño aplicados

| Patrón | Dónde | Por qué |
|--------|-------|---------|
| **State** | `GamePhaseState` + 4 implementaciones | Cada fase encapsula su propia lógica; `TurnManager` delega sin `switch(phase)` |
| **Chain of Responsibility** | `AttackHandler` + 7 handlers | El pipeline de ataque está dividido en pasos independientes y reemplazables |
| **Strategy** | `CardLookup` (`@FunctionalInterface`) | El engine no depende de Spring; la "estrategia" de búsqueda es inyectable |
| **Factory** | `PokemonFactory` | Crea `ActivePokemon`/`BenchPokemon` con todos los campos inicializados correctamente |
| **Builder** | Todos los modelos (`@Builder` de Lombok) | Construcción legible con valores opcionales sin constructores telescópicos |
| **Template Method** | `GamePhaseState.handle()` / `execute()` | Cada fase define solo sus métodos relevantes; los demás lanzan `UnsupportedOperationException` |
| **Facade** | `GameEngineFacade` | Oculta la complejidad del engine tras una API simple; gestiona el registry de partidas activas (`Map<Long, BoardState>`) |
| **Observer** (pendiente) | `GameEventPublisher` | Retransmisión de eventos al cliente por WebSocket |
| **Static Validator** | `RuleValidator` | Sin estado, sin Spring, testeable con solo `assertThrows` |

---

## 4. Flujo completo de un turno

```
beginTurn(board, cardLookup)
  │
  ├─ resetTurnFlags()
  │    └─ limpia TurnFlags + enteredThisTurn en todos los pokémon
  │
  ├─ DrawPhaseState.execute(board, cardLookup)
  │    ├─ playerTurnCount++
  │    ├─ Si primer turno del jugador 1 (playerTurnCount==1 && !firstPlayerHasActed)
  │    │    └─ skip draw (regla oficial)
  │    ├─ Si deck vacío → FINISHED + winnerId = rival (deck-out)
  │    └─ Mover top card del deck a la mano
  │
  └─ setCurrentPhase(MAIN) → esperar input del jugador

processAction(action, board, playerId, cardLookup)
  │
  ├─ Validar que es el turno del jugador
  ├─ CONCEDE → FINISHED + winnerId = rival
  ├─ END_TURN → completeTurn()
  ├─ USE_ATTACK → handleAttackFlow()
  └─ Resto (8 acciones) → MainPhaseState.handle()

handleAttackFlow()
  ├─ RuleValidator.validateUseAttack()
  ├─ setCurrentPhase(ATTACK)
  ├─ AttackPhaseState.handle()  ← 7-step pipeline
  └─ completeTurn()

completeTurn()
  ├─ BetweenTurnsState.execute()
  │    ├─ StatusEffectManager.applyBetweenTurnEffects(activePokemon)
  │    │    order: POISON(10dmg) → BURN(flip:cara=cura,cruz=20dmg) → ASLEEP(flip:cara=despierta) → PARALYZED(auto-cura)
  │    ├─ KnockoutProcessor.processIfKnockedOut() si HP≤0
  │    └─ Si primer turno del jugador 1 → firstPlayerHasActed = true
  │
  ├─ switchActivePlayer()
  └─ beginTurn() ← el rival empieza su turno
```

### Regla del primer turno del jugador 1

```
DrawPhaseState: si (playerTurnCount == 1 && !board.firstPlayerHasActed)
  → no roba carta + no puede atacar (playerTurnCount == 1 en RuleValidator)

BetweenTurnsState: si (actorField.playerTurnCount == 1 && !board.firstPlayerHasActed)
  → board.firstPlayerHasActed = true

Consecuencia: cuando le toca el turno al jugador 2 por primera vez,
  firstPlayerHasActed == true → SÍ roba carta.
```

---

## 5. TurnManager — el orquestador

**Clase:** `engine/TurnManager.java`

### `beginTurn(board, cardLookup)`

1. `resetTurnFlags(board)` — resetea `TurnFlags` a `false` + pone `enteredThisTurn = false` en todos los pokémon del jugador activo
2. Ejecuta `DrawPhaseState` automáticamente
3. Si el juego termina por deck-out → `return` (no continuar)
4. Cambia `currentPhase` a MAIN

### `processAction(action, board, playerId, cardLookup)`

- Valida que `playerId == board.currentPlayerId` → `InvalidActionException("NOT_YOUR_TURN", ...)`
- Si el juego ya terminó → `InvalidActionException("GAME_ALREADY_FINISHED", ...)`
- Switch por `action.type`:
  - `CONCEDE` → marca FINISHED
  - `END_TURN` → `handleEndTurnFlow()`
  - `USE_ATTACK` → `handleAttackFlow()`
  - Todo lo demás → `MainPhaseState.handle()`

### `handleAttackFlow(board, action, playerId, cardLookup)`

1. `RuleValidator.validateUseAttack()` — lanza excepción si el jugador no puede atacar
2. Cambia `currentPhase` a ATTACK
3. `AttackPhaseState.handle()` → ejecuta la cadena de 7 handlers
4. `completeTurn()`

### `completeTurn(board, cardLookup)`

1. `BetweenTurnsState.execute()`
2. `switchActivePlayer(board)` — cambia `currentPlayerId`
3. `beginTurn(board, cardLookup)` — empieza el turno del rival

### `resetTurnFlags(board)`

- Resetea `TurnFlags` del jugador activo (todos a `false`)
- Itera sobre el `activePokemon` y todos los `bench` del jugador activo
- Pone `enteredThisTurn = false` en cada uno

### `switchActivePlayer(board)`

```java
if (board.currentPlayerId.equals(board.player1Field.playerId))
    board.currentPlayerId = board.player2Field.playerId;
else
    board.currentPlayerId = board.player1Field.playerId;
```

---

## 6. GamePhaseState — patrón State

**Interfaz:** `engine/state/GamePhaseState.java`

```java
TurnPhase getPhase();

// Para fases interactivas (MAIN, ATTACK)
default ActionResult handle(ActionRequest action, BoardState board, Long playerId, CardLookup cardLookup) {
    throw new UnsupportedOperationException("Phase " + getPhase() + " no acepta acciones del jugador.");
}

// Para fases automáticas (DRAW, BETWEEN_TURNS)
default List<GameEvent> execute(BoardState board, CardLookup cardLookup) {
    throw new UnsupportedOperationException("Phase " + getPhase() + " no es automática.");
}
```

### `DrawPhaseState.execute()`

| Paso | Lógica |
|------|--------|
| Obtener campo del jugador activo | `getActiveField(board)` |
| Incrementar contadores | `actorField.playerTurnCount++` + `board.turnNumber++` |
| Regla primer turno | Si `playerTurnCount == 1 && !board.firstPlayerHasActed` → skip draw |
| Deck vacío | Si `actorField.deck.isEmpty()` → FINISHED + winnerId = rival + `DECK_OUT` event |
| Robar carta | `deck.remove(0)` → `hand.add(cardId)` |

### `MainPhaseState.handle()` — 8 acciones

| `ActionType` | Validación principal | Efecto |
|---|---|---|
| `PLAY_BASIC_POKEMON` | `validatePlayBasicPokemon` (tipo básico + banca no llena) | Mueve carta de mano a banca como `BenchPokemon` |
| `EVOLVE_POKEMON` | `validateEvolvePokemon` (evolvesFrom, no primer turno, no enteredThisTurn) | Reemplaza la carta en banca/activo; preserva daño; limpia condiciones |
| `ATTACH_ENERGY` | `validateAttachEnergy` (tipo energía, no duplicado ese turno) | Agrega a `attachedEnergies`; setea `energyAttachedThisTurn = true` |
| `ATTACH_TOOL` | `validateAttachTool` (tipo tool, pokémon sin tool) | Setea `tool`; quita carta de mano |
| `RETREAT` | `validateRetreat` (no retreatedThisTurn, no paralizado/dormido, energía suficiente) | Descarta energías de retirada; mueve activo a banca; promueve bench[benchIndex] |
| `PLAY_ITEM` | `validatePlayItem` (tipo ítem) | TODO: `TrainerEffectRegistry` — no ejecuta efecto aún |
| `PLAY_SUPPORTER` | `validatePlaySupporter` (tipo supporter, no duplicado ese turno) | TODO: `TrainerEffectRegistry` — setea `supporterPlayedThisTurn = true` |
| `PLAY_STADIUM` | `validatePlayStadium` (tipo estadio) | TODO: `TrainerEffectRegistry` — no ejecuta efecto aún |

**Detalle importante de EVOLVE:**

```java
// Preservar el daño al evolucionar
int damageTaken = target.maxHp - target.currentHp;
int newHp = Math.max(0, newMaxHp - damageTaken);
// Limpiar condiciones
StatusEffectManager.clearAllConditions(newActive);
newActive.enteredThisTurn = false;
```

**Detalle importante de RETREAT:**

```java
// Descartar exactamente retreatCostSize energías (del final de la lista)
for (int i = 0; i < retreatCostSize; i++) {
    int lastIdx = active.attachedEnergies.size() - 1;
    ownerField.discardPile.add(active.attachedEnergies.remove(lastIdx).cardId);
}
// Mover activo a banca
BenchPokemon newBench = BenchPokemon.builder()... (con enteredThisTurn = false, condiciones limpias)
ownerField.bench.add(newBench);
// Promover del banca
BenchPokemon promoted = ownerField.bench.remove(benchIndex);
ActivePokemon newActive = ActivePokemon.builder()... (enteredThisTurn = false)
```

### `AttackPhaseState.handle()`

Solo acepta `USE_ATTACK`. Setea `attackedThisTurn = true` y delega a `AttackResolutionChain.resolve()`. Devuelve `ActionResult.success(events)`.

### `BetweenTurnsState.execute()`

1. Obtiene el campo del jugador activo
2. Si `activePokemon != null` → `StatusEffectManager.applyBetweenTurnEffects(activePokemon)`
3. Si `activePokemon.currentHp <= 0` → `KnockoutProcessor.processIfKnockedOut()`
4. Si `actorField.playerTurnCount == 1 && !board.firstPlayerHasActed` → `board.firstPlayerHasActed = true`

---

## 7. RuleValidator — validador estático

**Clase:** `engine/RuleValidator.java`

Todos los métodos son `public static void`. Lanzan `InvalidActionException(errorCode, mensaje)` cuando la validación falla. No tienen estado, no dependen de Spring.

### Método `requireCardInHand(cardId, field)`

```
Precondición: cardId está en field.hand
Error: CARD_NOT_IN_HAND
```

### `validatePlayBasicPokemon(card, field)`

```
card.subtypes.contains("Basic")                   → NOT_BASIC_POKEMON
field.bench.size() < 5                            → BENCH_FULL
```

### `validateEvolvePokemon(evolutionCard, targetCard, targetEnteredThisTurn, actorField)`

```
evolutionCard.subtypes.contains("Stage 1") || "Stage 2"   → NOT_EVOLUTION_CARD
evolutionCard.evolvesFrom.equals(targetCard.name)         → WRONG_EVOLUTION_TARGET
actorField.playerTurnCount > 1                            → CANNOT_EVOLVE_FIRST_TURN
!targetEnteredThisTurn                                    → POKEMON_JUST_ENTERED
```

### `validateAttachEnergy(card, field)`

```
card.supertype.equals("Energy")                           → NOT_ENERGY_CARD
!field.turnFlags.energyAttachedThisTurn                   → ENERGY_ALREADY_ATTACHED
```

### `validateAttachTool(card, targetAlreadyHasTool)`

```
card.subtypes.contains("Pokémon Tool")                    → NOT_TOOL_CARD
!targetAlreadyHasTool                                     → POKEMON_ALREADY_HAS_TOOL
```

### `validateRetreat(active, retreatCostSize, benchIndex, field)`

```
!field.turnFlags.retreatedThisTurn                        → ALREADY_RETREATED
active != null                                            → NO_ACTIVE_POKEMON
active.condition != PARALYZED                             → PARALYZED_CANNOT_RETREAT
active.condition != ASLEEP                                → ASLEEP_CANNOT_RETREAT
benchIndex in [0, field.bench.size()-1]                   → INVALID_BENCH_INDEX
active.attachedEnergies.size() >= retreatCostSize         → NOT_ENOUGH_ENERGY_TO_RETREAT
```

### `validateUseAttack(board, actorField)`

```
actorField.activePokemon != null                          → NO_ACTIVE_POKEMON
actorField.playerTurnCount > 1                            → CANNOT_ATTACK_FIRST_TURN
active.condition != PARALYZED                             → PARALYZED_CANNOT_ATTACK
active.condition != ASLEEP                                → ASLEEP_CANNOT_ATTACK
```

---

## 8. StatusEffectManager — condiciones especiales

**Clase:** `engine/StatusEffectManager.java`

Constructor recibe `Random` inyectable para hacer los tests deterministas sin Mockito:

```java
StatusEffectManager manager = new StatusEffectManager(new Random(42)); // seed fijo → flip predecible
```

### `applyBetweenTurnEffects(activePokemon)`

Orden **estricto** según las reglas del juego:

| Paso | Condición | Efecto |
|------|-----------|--------|
| 1 | `isPoisoned` | 10 daño fijo (sin modificadores) |
| 2 | `isBurned` | flip: cara = cura quemadura; cruz = 20 daño |
| 3 | `condition == ASLEEP` | flip: cara = despierta (condition = NONE) |
| 4 | `condition == PARALYZED` | auto-cura (condition = NONE) |

> **¿Por qué no está esto en DamageCalculator?**
> `DamageCalculator` existe para el pipeline de ataque (base → weakness → resistance).
> El daño de estado es fijo (sin modificadores), ocurre en una fase completamente distinta,
> y no tiene relación con los ataques. Mezclarlos violaría el SRP.

### `applyCondition(pokemon, condition)`

Aplica respetando la exclusividad:
- Si la nueva condición es ASLEEP/CONFUSED/PARALYZED → setea `condition` (sobreescribe la anterior exclusiva)
- Si es BURNED → setea `isBurned = true` (no afecta `condition`)
- Si es POISONED → setea `isPoisoned = true` (no afecta `condition`)

### `clearAllConditions(pokemon)`

Limpia todo: `condition = NONE`, `isBurned = false`, `isPoisoned = false`.
Se llama al evolucionar o retirarse.

---

## 9. KnockoutProcessor — procesamiento de KO

**Clase:** `engine/KnockoutProcessor.java`

> **⚠️ Cambió en el trabajo actual (player-selection-mechanism).** Esta sección describe el
> comportamiento **nuevo** ya commiteado en `KnockoutProcessor`. La contraparte en `TurnManager`
> (que consume el `pendingSelection` y declara la victoria) **todavía no está cableada** — ver
> [§26](#26-player-selection-mechanism--en-curso) para el estado honesto de la migración.

### Nueva responsabilidad: solo MUTAR y REPORTAR (no decide el juego)

La idea de fondo (SRP): **el KO no decide quién gana.** El KO solo *habilita* una victoria. Quién
gana lo decide `VictoryConditionChecker`, invocado por `TurnManager` después del KO. Antes
`KnockoutProcessor` seteaba `FINISHED`/`winnerId` él mismo — eso se quitó.

### `processIfKnockedOut(pokemon, ownerField, prizeTakerField, board, cardLookup)`

> Ojo con la firma: el tercer parámetro se llama ahora `prizeTakerField` (el que toma los premios,
> normalmente el atacante), no `opponentField`.

**Guard:** si `pokemon == null || currentHp > 0` → devuelve lista vacía.

Secuencia cuando hay KO:

```
1. Evento POKEMON_KNOCKED_OUT
2. Descartar el pokémon + energías adjuntas + tool → ownerField.discardPile
3. ownerField.activePokemon = null
4. Determinar prizes vía koCard.subtypes:
   → contiene "EX" o "MEGA" (case-insensitive) → 2 prizes; resto → 1 prize
   → transferir de prizeTakerField.prizeCards a prizeTakerField.hand
   → emitir PRIZE_TAKEN (NO decide victoria aunque queden 0 premios)
5. Si la banca NO está vacía:
   → board.pendingSelection = PendingSelection(CHOOSE_ACTIVE_ON_KO, owner, opciones=cardIds banca)
   → y NO promueve a nadie (espera la elección del jugador)
6. Si la banca está vacía:
   → no hace nada (NO setea FINISHED). El orquestador declarará la derrota por "sin Pokémon".
```

### `promote(ownerField, benchIndex)` — nuevo método extraído

Mueve el Pokémon de `bench[benchIndex]` al slot activo, preservando HP/energías/tool y limpiando
condiciones (`enteredThisTurn = false` porque ya estaba en juego). Devuelve un evento `PHASE_CHANGED`.
Lo llama `TurnManager.resolveSelection()` cuando el jugador elige (ver §26). El caller garantiza que
el índice es válido.

**Notas de implementación:**
- Los prizes se determinan leyendo `koCard.getSubtypes()` directamente (no vía `CardTypeResolver`).
- Si `koCard == null` o no tiene subtypes → default 1 prize.

**Lo que se quitó vs. el comportamiento viejo (sesión 5):**
- ❌ ya **no** setea `FINISHED`/`winnerId` ni emite `GAME_FINISHED` (ni por premios agotados ni por banca vacía).
- ❌ ya **no** auto-promueve `bench[0]`: ahora pide la elección al jugador.
- Se quitó el `import GameStatus`.

---

## 10. VictoryConditionChecker — condiciones de victoria

**Clase:** `engine/VictoryConditionChecker.java`

Safety net que cubre las 3 condiciones de victoria del reglamento:

| Condición | Quién gana |
|-----------|-----------|
| Todas las Prize Cards tomadas | El que las tomó |
| Sin Pokémon en juego | El rival |
| Deck vacío al inicio del turno | El rival |

> La condición de deck-out se verifica en `DrawPhaseState`.
> Las condiciones de KO se verifican en `KnockoutProcessor`.
> `VictoryConditionChecker` es el safety net que las agrupa para verificaciones adicionales.

---

## 11. CardLookup — bridge hacia Spring

**Interfaz:** `engine/CardLookup.java`

```java
@FunctionalInterface
public interface CardLookup {
    Card findById(String cardId);
}
```

**Uso en producción** (dentro de `GameEngineFacade`, que sí es un Spring bean):
```java
CardLookup lookup = cardId -> cardRepository.findById(cardId).orElse(null);
```

**Uso en tests** (sin Spring):
```java
Map<String, Card> testCards = Map.of("xy1-1", buildCard("xy1-1"), ...);
CardLookup lookup = testCards::get;
```

Por eso el engine es testeable de forma completamente aislada.

---

## 12. GameEngineFacade — punto de entrada Spring

**Clase:** `engine/GameEngineFacade.java` · `@Service`

Único componente del engine con dependencias Spring. Wirea la capa de persistencia con el engine puro-Java.

### Responsabilidades

```
1. Construye el CardLookup como lambda desde CardRepository
2. Mantiene el registry in-memory: Map<Long, BoardState> activeGames
3. Delega toda lógica a TurnManager
```

### API pública

| Método | Qué hace |
|--------|----------|
| `startGame(gameId, p1Field, p2Field)` | Crea `BoardState(ACTIVE)`, lo guarda, llama `beginTurn()` para P1 |
| `processAction(gameId, action, playerId)` | Busca el board, delega a `TurnManager.processAction()` |
| `getState(gameId)` | Devuelve `Optional<BoardState>` — vacío si el juego no existe |
| `endGame(gameId)` | Elimina la partida del registry |

### Construcción del `CardLookup`

```java
private CardLookup buildCardLookup() {
    return cardId -> cardRepository.findById(cardId).orElse(null);
}
```

El método es privado y devuelve una nueva lambda en cada llamada — stateless, sin caché propia. Si en el futuro se agrega un `CardCacheService`, este es el único lugar a cambiar.

### Estado de las partidas

Las partidas viven en un `ConcurrentHashMap<Long, BoardState>` in-memory. No hay persistencia del estado a la BD todavía.

**TODO:** serializar `BoardState` a la entidad `GameState` para recuperar partidas tras reinicio del servidor.

---

## 13. AttackResolutionChain — cadena de responsabilidad

**Clase:** `engine/chain/AttackResolutionChain.java`

### Constructor

```java
// Producción — usa new Random() y new KnockoutProcessor()
new AttackResolutionChain()

// Tests — inyección de dependencias para determinismo
new AttackResolutionChain(new Random(42), mockKnockoutProcessor)
```

### `resolve(action, board, attackerPlayerId, cardLookup)`

1. Llama a `buildContext()` → retorna `null` si hay error de contexto
2. Si `buildContext()` retorna `null` → devuelve evento de error
3. Construye el pipeline de 7 handlers (`buildPipeline()`)
4. Itera los handlers: si `ctx.attackCancelled && !handler.alwaysRun()` → **skip** ese handler
5. Retorna `Collections.unmodifiableList(ctx.events)`

### `buildContext(action, board, attackerPlayerId, cardLookup)`

Retorna `null` en los siguientes casos:
- `attackerPokemon == null` o `defenderPokemon == null`
- `attackerCard == null` (card no encontrada en el lookup)
- `attackIndex` fuera de rango para la lista de ataques de la carta

Si todo es válido, construye el `AttackContext` con:
```
board, attackerPlayerId, cardLookup,
attackerField, defenderField,
attackerPokemon, defenderPokemon,
attackerCard, defenderCard,
attackData = attacks.get(attackIndex)
```

### `buildPipeline()`

Devuelve `List.of(...)` con los 7 handlers en orden. Se llama en cada `resolve()` para mantener cada ejecución stateless (los handlers no deben tener estado entre llamadas).

### Regla de cancelación

```java
for (AttackHandler handler : buildPipeline()) {
    if (ctx.isAttackCancelled() && !handler.alwaysRun()) continue;
    handler.handle(ctx);
}
```

El único handler con `alwaysRun() == true` es `PostDamageHandler`, para que el KO check siempre ocurra incluso después de una cancelación por confusión.

---

## 14. Los 7 handlers del pipeline de ataque

### Handler 1: `EnergyValidationHandler`

**Pregunta:** ¿El atacante tiene suficiente energía adjunta para pagar el coste del ataque?

**Algoritmo (two-pass):**

```
Paso 1: para cada energía requerida que NO sea "Colorless":
  → buscar en el pool de energías adjuntas una que coincida (case-insensitive)
  → si encuentra → remover del pool
  → si no encuentra → cancelAttack() + evento INSUFFICIENT_ENERGY

Paso 2: contar los "Colorless" requeridos
  → verificar que quedan >= energías restantes en el pool
  → cualquier tipo de energía satisface Colorless
```

**¿Por qué así?** "Colorless" es genérico — puede ser cualquier energía. Pero los tipos específicos (Fire, Water, etc.) deben matchear exactamente. Si primero sacamos los específicos, el resto del pool queda disponible para los Colorless.

**Tipo de energía:** se obtiene lookupando la carta de energía via `CardLookup` y leyendo `card.types.get(0)`. Si la carta no existe o no tiene tipos, se asume "Colorless".

**Evento de error:** `ATTACK_DECLARED` con `errorCode = "INSUFFICIENT_ENERGY"` y el tipo que falta.

---

### Handler 2: `ConfusionCheckHandler`

**Pregunta:** ¿El atacante está confundido? Si es así, ¿puede atacar?

**Lógica:**

```
Si condition != CONFUSED → handler no hace nada, el ataque sigue

Si condition == CONFUSED:
  flip = random.nextBoolean()   (true = Heads, false = Tails)

  HEADS: evento "atacó a través de la confusión" → continúa el pipeline

  TAILS: aplicar 30 de auto-daño al ATACANTE
         → activePokemon.currentHp = max(0, currentHp - 30)
         → evento DAMAGE_DEALT (con selfDamage = 30)
         → cancelAttack()
```

**¿Por qué el `PostDamageHandler` igualmente corre?**
Porque el atacante podría quedar con HP = 0 por el auto-daño. Sin el `alwaysRun()` del `PostDamageHandler`, ese KO nunca se procesaría.

**Inyección de `Random`:** el constructor acepta un `Random`, lo que permite tests deterministas pasando `new Random(seed)`.

---

### Handler 3: `SelectionsHandler`

**Pregunta:** ¿A quién apunta este ataque?

**Estado actual:** el target por defecto es siempre el `ActivePokemon` del rival, que ya viene pre-seteado en el `AttackContext` por `buildContext()`. Este handler solo emite el evento de anuncio del ataque.

**Evento:** `ATTACK_DECLARED` con attacker, attack name y defender.

**TODO futuro (ataques de snipe):** cuando se implementen ataques que golpean a pokémon de la banca (e.g. "Snipe Shot"), este handler leerá `action.targetPosition` y actualizará `ctx.defenderPokemon`.

---

### Handler 4: `PreAttackHandler`

Ejecuta efectos que deben resolverse **antes** de que el daño se aplique. Itera `attackData.getParsedEffects()` y llama a la lógica Flyweight de los efectos que retornan `isPreDamage() == true`.

**Efectos pre-daño típicos:**
- `DiscardEnergyEffect` (e.g. "Discard 2 Fire Energy attached to this Pokémon")
- `CoinFlipEffect` con bonus de daño (e.g. "Flip a coin; if heads, +30 damage")

**¿Cómo sabe si un efecto es pre-daño?** La interfaz `EffectLogic` tiene `default boolean isPreDamage() { return false; }`. Cada lógica override este método si necesita ejecutarse antes del daño.

---

### Handler 5: `ModifierHandler`

Modificadores planos al daño por herramientas y estadios. Se aplican **antes** de Weakness/Resistance.

**Implementado:** Muscle Band — si el atacante lleva Muscle Band adjunto Y el defensor es Pokémon-EX o Mega Pokémon → `ctx.damageModifiers += 20`.

```
1. Verificar ctx.attackerPokemon.tool != null
2. Lookup de la carta tool via ctx.cardLookup
3. Si tool.name == "Muscle Band":
      CardType defType = CardTypeResolver.resolve(defenderCard)
      Si defType == POKEMON_EX || MEGA_POKEMON → damageModifiers += 20
```

**Pendiente:** Hard Charm (−20 al defensor), inmunidades por habilidades, modificadores de estadio.

---

### Handler 6: `DamageApplicationHandler`

**Hace el daño real.**

```
base = attackData.getBaseDamage()
si base == 0 → ctx.finalDamage = 0; return  (ataque de efecto puro, no hay evento de daño)

finalDamage = DamageCalculator.calculate(base, attackerCard, defenderCard)
ctx.finalDamage = finalDamage

defenderPokemon.currentHp = max(0, defenderPokemon.currentHp - finalDamage)
```

Emite evento `DAMAGE_DEALT` con `baseDamage`, `finalDamage`, `hpRemaining`.

---

### Handler 7: `PostDamageHandler` ⭐ `alwaysRun() = true`

**Tres responsabilidades:**

1. **Aplicar condiciones** del texto del ataque (solo si el ataque NO fue cancelado)
2. **KO check del defensor**
3. **KO check del atacante** (puede haber muerto por auto-daño de confusión)

**Detección de condiciones por keywords** (substring matching sobre `attackData.text.toLowerCase()`):

| Keyword | Condición aplicada | Campo |
|---|---|---|
| "defending pokémon is now asleep" | ASLEEP | `condition` |
| "defending pokémon is now paralyzed" | PARALYZED | `condition` |
| "defending pokémon is now confused" | CONFUSED | `condition` |
| "defending pokémon is now burned" | BURNED | `isBurned = true` |
| "defending pokémon is now poisoned" | POISONED | `isPoisoned = true` |

**Nota sobre exclusividad:** ASLEEP/CONFUSED/PARALYZED son mutuamente excluyentes (usan `if/else if`). BURNED y POISONED son independientes y van en sus propios `if` separados.

**¿Por qué `alwaysRun() = true`?** → Ver sección anterior (Handler 2).

---

## 15. DamageCalculator — fórmula de daño

**Clase:** `engine/chain/DamageCalculator.java`

### `calculate(baseDamage, attackerCard, defenderCard)`

Fórmula XY era en orden estricto:

```
1. base damage (de attackData.getBaseDamage())
2. Weakness: si tipo del atacante matchea weakness del defensor → damage × 2
3. Resistance: si tipo del atacante matchea resistance del defensor → damage - 20
4. return max(0, damage)
```

**Tipos del atacante:** `attackerCard.getTypes().get(0)` (el primer tipo).

**Formato del JSONB:**

```json
// weaknesses en Card.weaknesses
[{"type": "Water", "value": "×2"}]

// resistances en Card.resistances
[{"type": "Fighting", "value": "-20"}]
```

Estos strings se parsean con Jackson en cada llamada. El `ObjectMapper` es `static final` (thread-safe, una sola instancia).

**Inner class `TypeModifier`:**
```java
@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
static class TypeModifier {
    private String type;
    private String value;
}
```

**Formato de weakness:** puede ser `"×2"`, `"x2"`, `"X2"` (manejo case-insensitive). Fallback: si no se puede parsear el multiplicador → ×2 por defecto. También maneja el antiguo formato `"+N"` de sets más viejos.

**Formato de resistance:** siempre `"-N"` en la era XY.

**Estado de la fórmula completa (spec PROJECT_CONSTITUTION — 6 pasos):**

| Paso | Estado |
|------|--------|
| base damage | ✅ |
| attacker mods (`ctx.damageModifiers`) | ✅ sumado antes de llamar a `calculate()` |
| ×2 weakness | ✅ |
| −20 resistance | ✅ |
| defender mods | ❌ no implementado — reducción por habilidades/tools del defensor (e.g. Hard Charm) |
| round to 10 | ✅ `(int)(Math.round(damage / 10.0) * 10)` al final del cálculo |

---

## 16. Sistema de efectos — Flyweight pattern

El sistema de efectos resuelve el problema de que la API devuelve los efectos como texto libre en inglés. El engine necesita objetos tipados para ejecutar lógica real.

### Arquitectura en dos capas

```
Capa 1 — DATOS   (models/cards/effects/)
AttackEffect (abstract, Jackson @JsonTypeInfo/@JsonSubTypes)
├── ApplyConditionEffect  { condition, target }       ← EffectLogic ✅
├── HealEffect            { amount, target }           ← EffectLogic ✅
├── AddDamageEffect       { amount, condition, target }← EffectLogic ✅
├── MultiplierDamageEffect                             ← EffectLogic ✅
├── DiscardEnergyEffect   { amount, target }           ← EffectLogic ✅ (post-daño, revivido sesión 6)
├── DamageToBenchEffect   { amount }                   ← stub (requiere selección → Bloque 3)
├── CoinFlipEffect        { ifHeads, ifTails }         ← EffectLogic ✅ (pre-daño, flip único + dispatch inline, sesión 6)
├── ShuffleHandEffect     { target, drawAmount }       ← EffectLogic ✅ (sesión 6)
├── DrawUntilHandSizeEffect { amount }                 ← EffectLogic ✅ (sesión 6)
├── DamageCountersEffect  { amount, target }           ← EffectLogic ✅ (variantes sin elección, sesión 6)
├── PreventDamageEffect / RestrictEffect               ← stub (efectos diferidos → Bloque 4)
├── SwitchPokemonEffect / SearchDeckEffect / LookAtDeckEffect ← stub (requieren selección → Bloque 3)
├── PassiveAbilityEffect                               ← sin lógica (efectos continuos → Bloque 4)
├── UnknownEffect                                      ← catch-all INERTE (defaultImpl, sesión 6 / Bloque 1)
└── ... (16 subtipos + UnknownEffect)

Capa 2 — COMPORTAMIENTO   (engine/effects/)
EffectRegistry (singleton Flyweight) — 15 lógicas registradas hoy:
  ✅ funcionales:
     ApplyConditionEffect → ApplyConditionLogic      HealEffect → HealLogic
     AddDamageEffect → AddDamageLogic                MultiplierDamageEffect → MultiplierDamageLogic
     CoinFlipEffect → CoinFlipLogic                  DiscardEnergyEffect → DiscardEnergyLogic
  🔴 registradas pero STUB (entran al registry, pero su execute() aún no hace lógica real / depende de selección):
     SearchDeckEffect → SearchDeckLogic              SwitchPokemonEffect → SwitchPokemonLogic
     RestrictEffect → RestrictLogic                  PreventDamageEffect → PreventDamageLogic
     DamageToBenchEffect → DamageToBenchLogic        DamageCountersEffect → DamageCountersLogic
     LookAtDeckEffect → LookAtDeckLogic              DrawUntilHandSizeEffect → DrawUntilHandSizeLogic
     ShuffleHandEffect → ShuffleHandLogic
  // UnknownEffect NO se registra a propósito → getLogic()==null → se omite (inerte)
```

> **⚠️ Registrada ≠ funcional.** Que una lógica esté en `EffectRegistry` solo significa que el motor
> *sabe a quién llamar*. Varias `*Logic` están registradas pero su `execute()` todavía es un stub
> (no muta nada, o espera el mecanismo de selección del §26, o son efectos diferidos del Bloque 4).
> **La fuente de verdad de qué efecto funciona de verdad es `docs/engine/EFFECT_IMPLEMENTATION_INVENTORY.md`
> y `EFFECT_IMPLEMENTATION_PROGRESS.md`** — esta guía da el mapa, el inventario da el estado fino.

### Estado real de ejecución (sesión 6)

Polimorfismo Jackson resiliente: `AttackEffect` usa `@JsonTypeInfo(visible=true, defaultImpl=UnknownEffect.class)`
+ `@JsonIgnoreProperties(ignoreUnknown=true)`. Un `type` desconocido cae en `UnknownEffect` (inerte) y
**no descarta** los efectos válidos de la carta (Bloque 1).

| Efecto | Timing | Estado |
|--------|--------|--------|
| ADD_DAMAGE, MULTIPLIER_DAMAGE | pre | ✅ |
| HEAL, APPLY_CONDITION | post | ✅ |
| COIN_FLIP | pre (flip único, dispatch inline) | ✅ revivido sesión 6 |
| DISCARD_ENERGY | post | ✅ revivido sesión 6 |
| SHUFFLE_HAND, DRAW_UNTIL_HAND_SIZE, DAMAGE_COUNTERS (sin elección) | post | ✅ sesión 6 |
| SEARCH_DECK, SWITCH_POKEMON, LOOK_AT_DECK, DAMAGE_TO_BENCH | — | 🔴 stub → requieren selección (Bloque 3) |
| PREVENT_DAMAGE, RESTRICT | — | 🔴 stub → efectos diferidos (Bloque 4) |
| PASSIVE_ABILITY | — | ⚫ sin lógica → efectos continuos (Bloque 4) |

### Cómo se ejecuta en el pipeline

```java
// PostDamageHandler — efectos post-daño
if (attackData.hasParsedEffects()) {
    for (AttackEffect effect : attackData.getParsedEffects()) {
        EffectLogic logic = registry.getLogic(effect.getClass());
        if (logic != null) logic.execute(effect, ctx);
        // null = efecto declarado pero sin lógica aún → silencioso
    }
}
```

```java
// PreAttackHandler — efectos pre-daño (discards, coin flips de bonus)
if (attackData.hasParsedEffects()) {
    for (AttackEffect effect : attackData.getParsedEffects()) {
        if (isPreDamage(effect)) {
            EffectLogic logic = registry.getLogic(effect.getClass());
            if (logic != null) logic.execute(effect, ctx);
        }
    }
}
```

### Cómo llegan los `parsedEffects` al engine

El JSON crudo de la API no tiene efectos tipados — solo `text` humano. La cadena completa es:

```
Startup:
  PokemonTCGApiService()
    → loadParsedEffects()          // lee /resources/data/xy1_parsed.json UNA VEZ
    → parsedEffectsCache: Map<cardId, {attacks, abilities}>

fetchSet("xy1"):
  → HTTP pokemontcg.io → Card entities
  → para cada carta: card.parsedEffects = parsedEffectsCache.get(card.id) serializado
  → Caffeine cache (TTL 24h)

USE_ATTACK:
  AttackResolutionChain.buildContext()
    → AttackParser.parse(card.attacks, card.parsedEffects)
         // attacks JSONB → List<AttackData>
         // parsedEffects JSON → merge por nombre de ataque → AttackData.parsedEffects poblado
    → pipeline de 7 handlers con efectos tipados listos
```

**El JSON de referencia:** `docs/abilityParsing/xy1_parsed.json` + `build_parsed_json.py` (script Python que generó el JSON con ayuda de LLM). Cubre las 147 cartas de XY1, incluyendo ataques con texto complejo (CoinFlip, condicionales, search deck, etc.).

**Para agregar soporte a nuevos sets:** copiar el JSON del set a `/resources/data/<setId>_parsed.json` y actualizar `loadParsedEffects()` en `PokemonTCGApiService`.

---

## 17. AttackTextParser — ~~ELIMINADO~~

> **Sesión 5 (2026-06-01):** `AttackTextParser.java` fue eliminado por ser dead code. El flujo de parseo via `xy1_parsed.json` (descrito arriba) reemplaza completamente el enfoque de regex. `AttackResolutionChain` nunca llamó a `AttackTextParser`.

---

## 18. AttackData / AttackParser / AttackContext

### `AttackData`

POJO Jackson que mapea un ataque del campo `card.attacks` (JSONB). Campos:

| Campo | Tipo | Ejemplo |
|-------|------|---------|
| `name` | String | "Flamethrower" |
| `cost` | `List<String>` | ["Fire", "Colorless", "Colorless"] |
| `convertedEnergyCost` | int | 3 |
| `damage` | String | "120", "10×", "30+", "" |
| `text` | String | "Discard an Energy attached to this Pokémon." |

**Métodos derivados:**

- `getBaseDamage()` → extrae solo los dígitos iniciales: "120"→120, "10×"→10, "30+"→30, ""→0
- `hasEffect()` → `text != null && !text.isBlank()`
- `hasDamageModifier()` → `damage` contiene "×", "+", o "-"
- `getCost()` → null-safe, retorna `Collections.emptyList()` si es null

### `AttackParser`

Utilidad estática con dos sobrecargas:

```java
// 1-arg: solo parsea el JSONB de ataques
List<AttackData> attacks = AttackParser.parse(card.getAttacks());

// 2-arg: parsea Y enriquece con parsedEffects (flujo normal del engine)
List<AttackData> attacks = AttackParser.parse(card.getAttacks(), card.getParsedEffects());
```

El overload de 2 argumentos lee el JSON de `card.parsedEffects` (pre-cargado desde `xy1_parsed.json`), matchea por nombre de ataque, y popula `AttackData.parsedEffects` con los efectos tipados desserializados via Jackson polimórfico (`@JsonSubTypes`).

Retorna `Collections.emptyList()` en caso de null, blank, o error de parseo. El `ObjectMapper` es `static final` (thread-safe).

### `AttackContext`

Contexto mutable del pipeline. Tiene dos secciones:

**Inmutables (seteados por `buildContext`):**
- `board`, `attackerPlayerId`, `cardLookup`
- `attackerField`, `defenderField`
- `attackerPokemon`, `defenderPokemon`
- `attackerCard`, `defenderCard`
- `attackData`

**Mutables (modificados por los handlers):**
- `attackCancelled` → true cuando el ataque no va a resolverse
- `finalDamage` → escrito por `DamageApplicationHandler`
- `events` → lista acumulada de eventos

**Métodos de conveniencia:**
- `cancelAttack()` → setea `attackCancelled = true`
- `addEvent(event)` → agrega al list

**`@Builder.Default`** en `events` → garantiza que no sea null aunque se use `.builder().build()` sin pasar `events`.

---

## 19. DTOs: ActionRequest y ActionResult

### `ActionRequest`

Cubre todas las acciones posibles con 6 campos:

| Campo | Tipo | Usado por |
|-------|------|-----------|
| `type` | `ActionType` | todos |
| `cardId` | String | play, evolve, attach |
| `targetPosition` | String | "ACTIVE" o "BENCH_0..4" |
| `attackIndex` | Integer | USE_ATTACK |
| `benchIndex` | Integer | RETREAT |
| `prizeIndex` | Integer | TAKE_PRIZE_CARD (TODO) |

**¿Por qué `targetPosition` y no `targetCardId`?**
Si hay dos Charizard en la banca (mismo cardId), `targetCardId` es ambiguo. Con `"BENCH_0"` siempre sabemos exactamente qué slot se apunta.

### `ActionResult`

```java
// Éxito
ActionResult.success(events)   // boolean success = true, lista de eventos

// Fallo
ActionResult.failure(errorMsg) // boolean success = false, error = mensaje, events vacía
```

Los eventos acumulan **todo lo que pasó en esa acción más todas las fases automáticas**. Un solo `USE_ATTACK` puede devolver: `ATTACK_DECLARED` + `DAMAGE_DEALT` + `STATUS_EFFECT_APPLIED` + `POKEMON_KNOCKED_OUT` + `PRIZE_TAKEN` + `CARD_DRAWN` (del draw del rival). El cliente recibe todo en una sola respuesta y los procesa en orden.

---

## 20. Eventos: GameEvent y GameEventType

### `GameEvent`

```java
GameEvent
├── type: GameEventType
├── description: String   (mensaje legible para humanos)
├── data: Map<String, Object>  (datos machine-readable para el frontend)
└── timestamp: long

// Factory methods
GameEvent.of(type, description)
GameEvent.of(type, description, Map.of("key", value, ...))
```

### `GameEventType` (enum completo)

| Evento | Cuándo se emite |
|--------|----------------|
| `PHASE_CHANGED` | Transición entre fases del turno |
| `TURN_ENDED` | Fin de turno |
| `CARD_DRAWN` | Carta robada del mazo |
| `DECK_OUT` | Mazo vacío → derrota |
| `POKEMON_PLAYED_TO_BENCH` | Pokémon básico a la banca |
| `POKEMON_EVOLVED` | Evolución de un pokémon |
| `ENERGY_ATTACHED` | Energía adjuntada |
| `ENERGY_DISCARDED` | Energía descartada (efecto post-ataque como Flamethrower) |
| `TOOL_ATTACHED` | Tool adjuntada |
| `ITEM_PLAYED` | Ítem jugado (trainer) |
| `SUPPORTER_PLAYED` | Supporter jugado (trainer) |
| `STADIUM_PLAYED` | Estadio jugado (trainer) |
| `POKEMON_RETREATED` | Pokémon retirado |
| `ATTACK_DECLARED` | Ataque declarado (o fallido) |
| `DAMAGE_DEALT` | Daño aplicado al defensor o auto-daño |
| `STATUS_EFFECT_APPLIED` | Condición especial aplicada |
| `STATUS_EFFECT_CLEARED` | Condición especial curada |
| `POKEMON_KNOCKED_OUT` | Pokémon derrotado |
| `PRIZE_TAKEN` | Prize Card tomada |
| `GAME_FINISHED` | Fin del juego con winnerId |
| `PLAYER_CONCEDED` | Jugador rindió |

---

## 21. TODOs conocidos y próximos pasos

### Avances posteriores a sesión 5 (a 2026-06-09)

| Tema | Estado |
|------|--------|
| **Trainers ejecutan efectos reales** (PLAY_ITEM/SUPPORTER/STADIUM vía `TrainerResolutionChain`) | ✅ Hecho (§24) |
| **`EffectRegistry`** ampliado de 6 → 15 lógicas registradas (varias aún stub) | ✅ Registradas / 🟡 lógica parcial (§16) |
| **Subsistema `ConditionStrategy`** (6 strategies + registry) | ✅ Existe (§23); 🟡 falta cablear limpio en `AddDamageLogic` |
| **Persistencia de estado** vía `EngineStateMapper` (dominio ↔ BD) | ✅ Hecho (§25) |
| **Mecanismo de selección del jugador** (KO → elegir promovido; fix #4) | 🚧 EN CURSO — modelos + `KnockoutProcessor` hechos; `TurnManager` + tests pendientes (§26) |
| **`KnockoutProcessor`**: ya no decide victoria, setea `pendingSelection`, método `promote()` | ✅ Commiteado (§9, §26) |
| **`VictoryConditionChecker`**: ascender de huérfano a fuente única de victoria | 🚧 Pendiente de cableado en `TurnManager` (§26) |

> Próximos pasos inmediatos: cerrar las tareas 2.3–2.8 + Fase 3/4 de
> `openspec/changes/player-selection-mechanism/tasks.md` (wiring de `TurnManager` + tests + verde
> `./mvnw test`). Recién ahí el flujo de KO con selección queda end-to-end.

### Estado al cierre de sesión 5 (2026-06-01)

| Archivo | Problema | Estado |
|---------|---------|--------|
| `GameEngineFacade` | Clase vacía — sin este componente el engine no es invocable desde Spring | ✅ Resuelto |
| `AttackData.parsedEffects` | Siempre null — nadie lo populaba desde la API | ✅ Resuelto (xy1_parsed.json → `AttackParser.parse` 2-arg) |
| `AttackParser` | Bug: `pa.get("effects")` → siempre null, efectos nunca se cargaban | ✅ Corregido (sesión 5: clave correcta `"parsedEffects"`) |
| `PokemonTCGApiService` | `loadParsedEffects()` se re-ejecutaba en cada `fetchSet()` call | ✅ Corregido (sesión 5: cacheado en campo `final` al construir el bean) |
| `AttackTextParser` | Dead code — regex parser nunca invocado por el engine real | ✅ Eliminado (sesión 5) |
| `engine/strategy/` | Dead code — interfaces vacías sin importadores | ✅ Eliminado |
| `PassiveAbilityCondition` | Dead code — sin importadores; compilación rota | ✅ Eliminado |
| `ModifierHandler` | Stub vacío — tools como Muscle Band no aplicaban | ✅ Implementado (Muscle Band) |
| `KnockoutProcessor` | Siempre 1 prize — EX/MEGA debería dar 2 | ✅ Implementado |
| `MultiplierDamageLogic` | `ENERGY_ON_SELF` hardcodeado a 1 | ✅ Implementado |
| `DamageCalculator` | Faltaba `round-to-10` | ✅ Implementado |
| `DiscardEnergyEffect` / `DamageToBenchEffect` | Sin lógica de ejecución | ✅ Implementado |
| `HealTrainerLogic` | `BENCH_X` y `ALL` targets no implementados | ✅ Implementado (sesión 5) |
| `MainPhaseState` | Stadium anterior no se descartaba al jugar uno nuevo | ✅ Implementado (sesión 5: `BoardState.activeStadiumCardId`) |
| Tests faltantes | `TurnManagerTest`, `DamageCalculatorTest`, `StatusEffectManagerTest`, `VictoryConditionCheckerTest` | ✅ Agregados (sesión 5: 41 tests nuevos) |
| `AddDamageLogic` | Condición (tipo del defensor) siempre evalúa true | 🟡 Pendiente |
| `DamageCalculator` | Falta `defender_mods` (Hard Charm, etc.) | 🟡 Pendiente |
| `Card.parsedTrainerEffects` | `@Transient`, nunca populado — trainers no ejecutan via Flyweight | 🟡 Pendiente |
| `SelectionsHandler` | Solo ataca al Active — bench targeting no implementado | 🟠 Pendiente |
| `KnockoutProcessor` | El jugador debería elegir qué Pokémon de banca promover (ahora índice 0) | 🟠 UX pendiente |
| `MainPhaseState` | Stack de evolución incompleto — solo descarta el stage anterior, no toda la cadena | 🟠 Pendiente |
| `BetweenTurnsState` | KO de EX por veneno/quemadura da 1 prize (no pasa CardLookup) | 🟠 Pendiente |

### Componentes pendientes

| Componente | Prioridad | Descripción |
|------------|-----------|-------------|
| `GameEventPublisher` | Alta | Observer — retransmite `GameEvent`s por WebSocket al controller |
| Tests del engine | Media | Cobertura JaCoCo ≥ 70%; faltan `AttackResolutionChain` handlers individuales |
| Trainer effects completos | Media | PLAY_ITEM/SUPPORTER/STADIUM ejecutan efectos reales via `TrainerEffectRegistry` |
| Persistencia de `BoardState` | Baja | Serializar estado a `GameState` entity para recuperar partidas tras reinicio |

### Estructura de tests recomendada

```
test/engine/
├── state/
│   ├── DrawPhaseStateTest.java
│   ├── MainPhaseStateTest.java
│   └── BetweenTurnsStateTest.java
├── chain/
│   ├── EnergyValidationHandlerTest.java
│   ├── ConfusionCheckHandlerTest.java
│   ├── DamageApplicationHandlerTest.java
│   ├── PostDamageHandlerTest.java
│   └── AttackResolutionChainTest.java
├── RuleValidatorTest.java
├── StatusEffectManagerTest.java
├── KnockoutProcessorTest.java
└── TurnManagerIntegrationTest.java
```

**Patrón para tests del engine:**

```java
// No Spring, no Mockito para CardLookup — usar lambda
Map<String, Card> cards = Map.of("xy1-1", buildPikachuCard());
CardLookup lookup = cards::get;

// Para ConfusionCheckHandler determinista
Random fixedRandom = new Random(0); // seed 0 → primero tails, luego heads (depende de la JVM)
// O mejor:
Random alwaysHeads = mock(Random.class);
when(alwaysHeads.nextBoolean()).thenReturn(true);

// Para StatusEffectManager determinista
StatusEffectManager manager = new StatusEffectManager(alwaysHeads);
```

---

## Diagrama de flujo completo (ASCII)

```
                    ┌─────────────────────────────────────────────┐
                    │              TurnManager                     │
                    │                                             │
 beginTurn() ──────►│  resetTurnFlags()                           │
                    │       ↓                                     │
                    │  DrawPhaseState.execute()                   │
                    │    ├─ playerTurnCount++                     │
                    │    ├─ [skip draw si primer turno P1]        │
                    │    ├─ [deck-out → FINISHED]                 │
                    │    └─ hand.add(deck.remove(0))              │
                    │       ↓                                     │
                    │  currentPhase = MAIN ◄── espera input       │
                    └───────────────┬─────────────────────────────┘
                                    │
              processAction(action) │
                                    ▼
                    ┌───────────────────────────────┐
                    │      ¿Qué acción?              │
                    └──┬─────────────┬──────────────┘
                       │             │
              MAIN ────┘    USE_ATTACK│      END_TURN
              actions                ▼
                       ┌─────────────────────────┐
                       │  RuleValidator.validate  │
                       │         ↓                │
                       │  AttackPhaseState        │
                       │         ↓                │
                       │  AttackResolutionChain   │
                       │    1. EnergyValidation   │
                       │    2. ConfusionCheck     │
                       │    3. Selections         │
                       │    4. PreAttack          │
                       │    5. Modifier           │
                       │    6. DamageApplication  │
                       │    7. PostDamage (KO)    │
                       └──────────┬──────────────┘
                                  │
                                  ▼
                    ┌─────────────────────────────────┐
                    │      completeTurn()              │
                    │         ↓                       │
                    │  BetweenTurnsState.execute()    │
                    │    POISON→BURN→SLEEP→PARALYZE   │
                    │    KO check                     │
                    │    firstPlayerHasActed flag     │
                    │         ↓                       │
                    │  switchActivePlayer()           │
                    │         ↓                       │
                    │  beginTurn() [rival]  ◄─────────┘
                    └─────────────────────────────────┘
```

---

## 22. Flujos de resolución — ejemplos reales (XY1)

Cada ejemplo muestra qué ocurre paso a paso cuando un jugador ejecuta USE_ATTACK con una carta específica de XY1. El punto de entrada siempre es `AttackResolutionChain.resolve()`.

---

### Ejemplo 1 — Efecto directo: Venusaur-EX "Poison Powder"

**Carta:** `xy1-1` · Ataque sin daño base, texto: *"Your opponent's Active Pokémon is now Poisoned."*

**parsedEffects en xy1_parsed.json:**
```json
[{ "type": "APPLY_CONDITION", "condition": "POISONED", "target": "DEFENDER" }]
```

**Pipeline:**
```
Handler 1 EnergyValidation   → costo cubierto ✅
Handler 2 ConfusionCheck     → no confundido → sigue
Handler 3 SelectionsHandler  → emite ATTACK_DECLARED
Handler 4 PreAttackHandler   → ApplyConditionEffect.isPreDamage() = false → skip
Handler 5 ModifierHandler    → sin modificadores
Handler 6 DamageApplication  → baseDamage = 0 (damage="") → no emite DAMAGE_DEALT
Handler 7 PostDamageHandler  → itera parsedEffects:
                                  ApplyConditionEffect {POISONED, DEFENDER}
                                  → ApplyConditionLogic.execute()
                                  → defenderPokemon.setPoisoned(true)
                                  → emite STATUS_EFFECT_APPLIED
```

**Eventos resultantes:** `[ATTACK_DECLARED, STATUS_EFFECT_APPLIED{condition:POISONED}]`

---

### Ejemplo 2 — Coin flip → condición: Spewpa "Stun Spore"

**Carta:** `xy1-16` · Ataque sin daño base, texto: *"Flip a coin. If heads, your opponent's Active Pokémon is now Paralyzed."*

**parsedEffects:**
```json
[{
  "type": "COIN_FLIP",
  "ifHeads": [{ "type": "APPLY_CONDITION", "condition": "PARALYZED", "target": "DEFENDER" }],
  "ifTails": []
}]
```

**Pipeline:**
```
Handler 1–5  → igual al ejemplo anterior
Handler 6    → baseDamage = 0 → skip
Handler 7    → CoinFlipEffect encontrado
                → CoinFlipLogic.execute():
                     flip = random.nextBoolean()
                     emite COIN_FLIPPED("HEADS" o "TAILS")

                     si HEADS:
                       ApplyConditionEffect{PARALYZED, DEFENDER}
                       → ApplyConditionLogic → defenderPokemon.setCondition(PARALYZED)
                       → emite STATUS_EFFECT_APPLIED

                     si TAILS:
                       ifTails = [] → no hace nada
```

**Eventos (HEADS):** `[ATTACK_DECLARED, COIN_FLIPPED{HEADS}, STATUS_EFFECT_APPLIED{PARALYZED}]`
**Eventos (TAILS):** `[ATTACK_DECLARED, COIN_FLIPPED{TAILS}]`

---

### Ejemplo 3 — Coin flip → daño adicional: Illumise "Quick Attack"

**Carta:** `xy1-9` · `damage: "10"`, texto: *"Flip a coin. If heads, this attack does 20 more damage."*

**parsedEffects:**
```json
[{
  "type": "COIN_FLIP",
  "ifHeads": [{ "type": "ADD_DAMAGE", "amount": 20 }],
  "ifTails": []
}]
```

**Pipeline:**
```
Handler 4 PreAttackHandler  → CoinFlipEffect.isPreDamage() = true ✅ (sesión 6)
                               CoinFlipLogic.execute(): flip = random.nextBoolean()
                               emite COIN_FLIPPED
                               si HEADS: AddDamageEffect{20} → AddDamageLogic
                                          → ctx.damageModifiers += 20
                               si TAILS: ifTails = [] → nada

Handler 6 DamageApplication → baseDamage = 10; aplica weakness/resistance;
                               suma ctx.damageModifiers (20 si fue heads) → finalDamage
                               → defenderPokemon.currentHp -= finalDamage
                               → emite DAMAGE_DEALT
```

**Nota:** el bonus de +20 se acumula en `damageModifiers` **antes** de aplicar el daño (COIN_FLIP es
pre-daño desde sesión 6), no como una resta de HP separada post-daño.

---

### Ejemplo 4 — Descarte de energía + daño base: Slugma "Flamethrower"

**Carta:** `xy1-20` · `damage: "60"`, texto: *"Discard an Energy attached to this Pokémon."*

**parsedEffects:**
```json
[{ "type": "DISCARD_ENERGY", "amount": 1, "target": "SELF" }]
```

**Pipeline:**
```
Handler 6 DamageApplication → baseDamage = 60
                               → DamageCalculator.calculate(60, attacker, defender)
                               → emite DAMAGE_DEALT

Handler 7 PostDamageHandler → DiscardEnergyEffect.isPostDamage() = true ✅ (sesión 6)
                               → DiscardEnergyLogic.execute():
                                    remueve 1 energía de attackerPokemon.attachedEnergies
                                    emite ENERGY_DISCARDED
                               → KO check del defensor
```

**Eventos:** `[ATTACK_DECLARED, DAMAGE_DEALT{60}, ENERGY_DISCARDED, (POKEMON_KNOCKED_OUT?)]`

**¿Por qué post-daño (sesión 6)?** `DiscardEnergyLogic.isPostDamage()` retorna `true`: así los ataques cuyo
daño escala con la energía adjunta calculan el daño **antes** de descartar. (Mover la carta a `discardPile`
explícitamente es una mejora pendiente; hoy la energía se remueve de la lista del Pokémon.)

---

### Ejemplo 5 — Daño a la banca: Ledian "Mach Punch"

**Carta:** `xy1-7` · `damage: "30"`, texto: *"This attack does 10 damage to 1 of your opponent's Benched Pokémon."*

**parsedEffects:**
```json
[{ "type": "DAMAGE_TO_BENCH", "amount": 10, "targetCount": 1, "target": "OPPONENT_BENCH" }]
```

**Pipeline:**
```
Handler 6 DamageApplication → baseDamage = 30 → aplica al Active rival
Handler 7 PostDamageHandler → DamageToBenchEffect{10, OPPONENT_BENCH}
                               → DamageToBenchLogic.execute():
                                    elige bench[0] del rival (TODO: player choice)
                                    benchPokemon.currentHp -= 10
                                    emite DAMAGE_DEALT{target:bench, amount:10}
                               → KO check Active
                               → KO check banca afectada
```

**Regla TCG:** el daño a la banca NO aplica weakness/resistance. `DamageToBenchLogic` aplica el valor fijo directamente, sin pasar por `DamageCalculator`.

---

### Ejemplo 6 — Multiplicador por monedas: Beedrill "Flash Needle"

**Carta:** `xy1-5` · `damage: "0"`, texto: *"Flip 3 coins. This attack does 40 damage times the number of heads."*

**parsedEffects:**
```json
[{ "type": "MULTIPLIER_DAMAGE", "amountPerUnit": 40, "unitType": "COIN_FLIPS", "flips": 3 }]
```

**Pipeline:**
```
Handler 4 PreAttackHandler  → MultiplierDamageEffect.isPreDamage() = true ✅
                               → MultiplierDamageLogic.execute():
                                    tira 3 monedas: heads = random.nextBoolean() × 3
                                    ctx.damageModifiers += (heads_count × 40)
                                    emite COIN_FLIPPED × 3

Handler 6 DamageApplication → baseDamage = 0
                               total = 0 + ctx.damageModifiers  (0, 40, 80, o 120)
                               → DamageCalculator.calculate(total, attacker, defender)
                               → emite DAMAGE_DEALT

Handler 7 PostDamageHandler → KO check
```

**Resultado posible:** 0 (3 tails), 40, 80, o 120 de daño. La debilidad/resistencia aplica sobre ese total.

---

## 23. ConditionStrategy — el patrón Strategy real del engine

**Paquete:** `engine/effects/conditions/`

### El problema que resuelve

Muchos ataques tienen daño **condicional**: *"this attack does 20 more damage **if the Defending
Pokémon is a Grass type**"*, o *"...**if this Pokémon has any Psychic Energy attached**"*. Esa
condición es una **pregunta sí/no** que depende del estado del tablero en el momento del ataque.

Necesitamos una forma de:
1. Representar esa pregunta como un dato en el JSON (`"condition": "OPPONENT_IS_GRASS"`).
2. Mapearla a un objeto Java que sepa responderla mirando el `AttackContext`.
3. Poder agregar preguntas nuevas sin tocar las viejas.

Eso es exactamente el **patrón Strategy**. Y es —ojo con esto— el **único Strategy real del engine**.
`CardLookup` se documenta a veces como "Strategy" pero en rigor es un `@FunctionalInterface` de
inyección. El Strategy genuino vive acá.

### La interfaz

```java
public interface ConditionStrategy {
    boolean evaluate(AttackContext ctx);   // ¿se cumple la condición, dado el estado actual?
}
```

Una línea. Una pregunta sí/no sobre el contexto del ataque. Cada implementación responde una
condición concreta.

### El registry (key → strategy)

`ConditionRegistry` es un singleton que mapea la **string del JSON** a la **instancia de strategy**:

```java
register("OPPONENT_IS_GRASS",               new OpponentIsGrassCondition());
register("DEFENDER_IS_EX",                  new DefenderIsExCondition());
register("DEFENDER_HAS_DAMAGE_COUNTERS",    new DefenderHasDamageCountersCondition());
register("DEFENDER_HAS_SPECIAL_CONDITION",  new DefenderHasSpecialCondition());
register("SELF_HAS_ENERGY_PSYCHIC",         new SelfHasEnergyPsychicCondition());
register("LUNATONE_ON_BENCH",               new LunatoneOnBenchCondition());
```

| Key del JSON | Pregunta que responde |
|--------------|-----------------------|
| `OPPONENT_IS_GRASS` | ¿El Pokémon defensor es de tipo Grass? |
| `DEFENDER_IS_EX` | ¿El defensor es Pokémon-EX? |
| `DEFENDER_HAS_DAMAGE_COUNTERS` | ¿El defensor ya tiene daño encima? |
| `DEFENDER_HAS_SPECIAL_CONDITION` | ¿El defensor está dormido/confundido/etc.? |
| `SELF_HAS_ENERGY_PSYCHIC` | ¿El atacante tiene energía Psychic adjunta? |
| `LUNATONE_ON_BENCH` | ¿Hay un Lunatone en la banca propia? (combo específico de XY1) |

### Cómo se consume

Las lógicas de efecto condicional (típicamente `AddDamageLogic`) leen el campo `condition` del
efecto, piden la strategy al registry y la evalúan:

```java
ConditionStrategy strategy = ConditionRegistry.getInstance().getStrategy(effect.getCondition());
if (strategy == null || strategy.evaluate(ctx)) {
    // condición ausente o cumplida → aplicar el bonus
}
```

> **TODO conocido (§21):** `AddDamageLogic` todavía no consume esto de forma robusta — la condición
> puede evaluar siempre `true`. El subsistema de strategies **existe y está listo**, pero falta
> cablearlo limpio en `AddDamageLogic`. Agregar una condición nueva = una clase de 5 líneas + una
> línea en `ConditionRegistry`.

---

## 24. Pipeline de Trainers — efectos de cartas de entrenador

**Paquetes:** `engine/effects/trainers/` y `engine/effects/trainers/chain/`

> **Novedad post-sesión 5:** en la guía vieja, jugar un Item/Supporter/Stadium movía la carta pero
> **no ejecutaba su efecto** (era un TODO). **Ahora sí se ejecuta**, a través de un pipeline espejo
> del de ataques.

### La idea: mismo molde que los ataques

Los trainers reusan exactamente la misma arquitectura de dos capas que los ataques (§16):

```
Capa 1 — DATOS         models/cards/effects/trainer/
  TrainerEffect (abstract, Jackson polimórfico)
  ├── DrawCardsTrainerEffect       ├── HealTrainerEffect
  ├── DiscardHandDrawTrainerEffect ├── ShuffleHandTrainerEffect
  ├── DiscardEnergyTrainerEffect   └── CoinFlipTrainerEffect

Capa 2 — COMPORTAMIENTO  engine/effects/trainers/   (Flyweight, stateless)
  TrainerEffectRegistry — 6 lógicas registradas:
    DrawCardsTrainerEffect       → DrawCardsTrainerLogic        ✅
    HealTrainerEffect            → HealTrainerLogic             ✅ (ACTIVE / BENCH_X / ALL)
    DiscardHandDrawTrainerEffect → DiscardHandDrawTrainerLogic  ✅
    ShuffleHandTrainerEffect     → ShuffleHandTrainerLogic      ✅
    DiscardEnergyTrainerEffect   → DiscardEnergyTrainerLogic    ✅
    CoinFlipTrainerEffect        → CoinFlipTrainerLogic         ✅
```

### El pipeline (`TrainerResolutionChain`)

Es un Chain of Responsibility, igual que `AttackResolutionChain`, pero hoy con **un solo handler**:

```java
public List<GameEvent> resolve(Card card, BoardState board, Long playerId, CardLookup cardLookup) {
    TrainerContext ctx = TrainerContext.builder()
        .board(board).playerId(playerId).card(card).cardLookup(cardLookup)
        .parsedEffects(TrainerEffectParser.parse(card.getParsedEffects()))   // JSON → efectos tipados
        .build();

    for (TrainerHandler handler : buildPipeline()) {        // hoy: [TrainerEffectExecutionHandler]
        if (ctx.isCancelled() && !handler.alwaysRun()) continue;
        handler.handle(ctx);
    }
    return ctx.getEvents();
}
```

**¿Por qué una cadena si hay un solo handler?** Por **extensibilidad planificada**: el diseño deja
huecos explícitos para insertar, sin tocar lo existente:
- **Bloque 3:** un `TrainerSelectionHandler` (cuando un trainer pida elegir, ej. buscar carta) — reusará el mecanismo del §26.
- **Bloque 4:** handlers de validación / post-efecto.

### Dónde se engancha (`MainPhaseState`)

`MainPhaseState` tiene un `private final TrainerResolutionChain trainerChain = new TrainerResolutionChain();`
y lo invoca en las 3 acciones de trainer:

| Acción | Qué hace el handler | Movimiento físico de la carta |
|--------|---------------------|-------------------------------|
| `PLAY_ITEM` | `trainerChain.resolve(...)` ejecuta el efecto | mano → discard |
| `PLAY_SUPPORTER` | idem + setea `supporterPlayedThisTurn` | mano → discard |
| `PLAY_STADIUM` | idem; setea `board.activeStadiumCardId` (descarta el estadio anterior) | sale de la mano |

> **División de responsabilidades clave:** la cadena **solo ejecuta los efectos parseados**. El
> movimiento físico de la carta (descartar, poner el estadio, etc.) y los `turnFlags` se quedan en
> `MainPhaseState`. La cadena no toca la mano ni el descarte directamente.

---

## 25. EngineStateMapper — puente de persistencia (dominio ↔ BD)

**Clase:** `engine/mappers/EngineStateMapper.java`

> **Novedad post-sesión 5:** la guía vieja marcaba "persistencia de `BoardState`" como TODO. Ya
> existe este mapper que traduce entre el **modelo de dominio del engine** (`BoardState`, que vive en
> memoria mientras se juega) y las **entidades persistidas en la BD** (`GameBoardState`,
> `PlayerBoardState`, `PokemonInPlayState`, `CardInstanceState`, en `models/game/state/`).

### Por qué hacen falta dos modelos distintos

| | Modelo de dominio (engine) | Modelo de BD (state) |
|--|---------------------------|----------------------|
| Clase raíz | `BoardState` | `GameBoardState` |
| Identidad de carta | `cardId` (String) — "qué carta es" | `instanceId` (UUID) — "qué copia física concreta" |
| Para qué | reglas, mutación rápida en memoria | guardar/recuperar la partida entre requests HTTP |

El engine razona con `cardId` (le alcanza saber "es un Charizard"). La BD necesita **instancias
únicas** (`instanceId`): si tenés dos Charizard, hay que distinguir *cuál* está en la banca y *cuál*
en el descarte. El mapper traduce entre ambos vocabularios.

### Los dos métodos

```java
// BD → dominio: reconstruye el BoardState que el engine va a manipular
static BoardState toDomainState(GameBoardState dbState);

// dominio → BD: vuelca de vuelta el estado mutado por el engine a la entidad persistida
static void updateDbState(BoardState domainState, GameBoardState dbState);
```

**Detalles que importan al leer el código:**
- `matchState` se mapea como `null` al traer de la BD con el comentario *"Engine calculates this"* —
  el estado de la partida (ACTIVE/FINISHED) lo recalcula el engine, no se confía al snapshot.
- `firstPlayerHasActed` se deriva de `turnNumber > 1`.
- En `updateDbState`, al volcar la banca **se preserva el `instanceId` existente** si la carta en esa
  posición no cambió (evita regenerar UUIDs en cada update y romper referencias).
- Regla TCG respetada en ambas direcciones: **máximo 1 tool por Pokémon** (`attachedTools` se trata
  como lista pero se usa solo el primer elemento).

> Este mapper es el lugar donde "el árbitro" (engine) y "el acta del partido" (BD) se sincronizan.
> Si agregás un campo nuevo al estado del juego que tenga que persistir, **este es el archivo a tocar
> en ambas direcciones**.

---

## 26. Player Selection Mechanism — ✅ completo a nivel engine

> **Estado 2026-06-11: COMPLETO y verde a nivel engine** (sesión 7). Lo único pendiente es el
> **borde DTO** (Fase 5): exponer `pendingSelection` por `GET /state` y que sobreviva la
> persistencia (`EngineStateMapper` no lo mapea — ver TODO A1/A5 de `docs/TODO/PROJECT_TODO.md`).
> Hasta eso, el mecanismo funciona dentro del engine y sus tests, pero NO end-to-end por HTTP.
> Artefactos SDD: `openspec/changes/player-selection-mechanism/`.

### El problema

El engine necesita, a veces, **pausar a mitad de resolver un efecto y pedirle algo al jugador**:
- ¿Qué Pokémon de la banca promover tras un KO? (el caso que estamos resolviendo)
- ¿Qué carta buscar en el mazo? ¿A qué Pokémon de la banca pegarle? (casos futuros)

Hoy ese mecanismo **no existía**: `KnockoutProcessor` auto-promovía `bench[0]` con un `// TODO`. Es
un mecanismo **transversal** que desbloquea varios stubs de efectos.

### Por qué NO se resuelve con un callback

Java no tiene continuations, y —más importante— `resolveSelection` llega en un **request HTTP
posterior**: entre el "pedir" y el "responder", el `BoardState` se reconstruye desde la BD (§25). Una
lambda/`Runnable` guardada en el board **no sobrevive a la serialización**. 

**La decisión de diseño:** reificar la continuación como **datos planos serializables**. El board es
datos, no comportamiento. La continuación se reconstruye desde un enum + el estado del board.

### Las piezas

```java
enum SelectionType { CHOOSE_ACTIVE_ON_KO }   // extensible

class PendingSelection {          // se guarda en BoardState.pendingSelection (nullable)
    SelectionType type;
    Long ownerPlayerId;           // quién debe elegir (¡puede NO ser el jugador del turno!)
    List<String> validOptions;    // cardIds candidatos (la banca del dueño)
    String prompt;                // texto para la UI
}
// + nuevo ActionType.RESOLVE_SELECTION
```

### El flujo objetivo (diseño)

```
USE_ATTACK → handleAttackFlow → chain → PostDamageHandler
                                            │ KO + banca≠∅ → set pendingSelection (NO promueve)
        corta: return events ◀──────────────┘
            ╎ ... request HTTP posterior ...
RESOLVE_SELECTION → resolveSelection(playerId, choice)
            → valida (owner correcto + opción válida)
            → promote(elegido) + clear pendingSelection
            → resume según currentPhase: ATTACK→completeTurn · BETWEEN_TURNS→switch+beginTurn
```

**Resume sin puntero explícito:** el `currentPhase` del board ya codifica dónde estábamos. KO en
ataque → `phase == ATTACK` → resume = `completeTurn`. KO entre turnos → `phase == BETWEEN_TURNS` →
resume = switch + beginTurn.

### Estado final (sesión 7 — todo ✅)

| Pieza | Estado |
|-------|--------|
| `SelectionType`, `PendingSelection`, `BoardState.pendingSelection`, `ActionType.RESOLVE_SELECTION` | ✅ |
| `KnockoutProcessor`: setea `pendingSelection`, no auto-promueve, no decide victoria, método `promote()` | ✅ |
| `TurnManager`: guarda de `pendingSelection` en `processAction` (antes que la guarda de turno) | ✅ |
| `TurnManager`: corte tras `attackPhase.handle` + dentro de `completeTurn` | ✅ |
| `TurnManager.resolveSelection(...)` → delega validación/aplicación en `SelectionResolver` (SRP/OCP) | ✅ |
| `VictoryConditionChecker` como fuente única de victoria (firma `Optional<VictoryReason>`) | ✅ |
| Tests: `KnockoutProcessorTest`, `SelectionResolverTest`, `VictoryConditionCheckerTest`, `TurnManagerTest`, `TurnManagerSelectionIntegrationTest` | ✅ verdes |

**Sutileza clave:** en un KO por ataque, quien elige es el **DEFENSOR** (no el jugador del turno) —
la guarda valida contra `pending.ownerPlayerId`, no contra `currentPlayerId`.

**🚧 Lo único pendiente (borde, NO engine):** Fase 5 — `EngineStateMapper` no mapea
`pendingSelection` ↔ `GameBoardState`, así que la selección **no sobrevive entre requests HTTP**
y `GET /state` no la expone. Es el ítem A5 del `PROJECT_TODO` (depende de la decisión de
persistencia A1).

### El cambio de rol de `VictoryConditionChecker`

Parte de este trabajo es **ascender** a `VictoryConditionChecker`: de "red de seguridad" huérfana
(nunca invocada) a **fuente única de decisión de victoria**, llamada por `TurnManager` en los dos
puntos post-KO. Precedencia acordada: **la victoria gana sobre la selección** (si ganaste por
premios, se limpia el `pendingSelection`).

---

## 27. Glosario y mapa mental rápido

Para cuando un término te frena. Ordenado por "qué tan seguido aparece".

| Término | En una frase |
|---------|--------------|
| **Engine** | El árbitro en Java puro: valida reglas, muta estado, devuelve eventos. Sin Spring. |
| **`BoardState`** | La foto completa del tablero: fase, jugador de turno, ambos `PlayerField`. Datos puros. |
| **`PlayerField`** | Todo lo de un jugador: activo, banca, mano, mazo, premios, descarte, flags. |
| **`CardLookup`** | La única lambda que conecta el engine con la persistencia (`cardId → Card`). |
| **`TurnManager`** | El orquestador del ciclo del turno. Punto de entrada de cada acción. |
| **`GamePhaseState`** | Patrón State: cada fase (DRAW/MAIN/ATTACK/BETWEEN_TURNS) sabe qué hacer. |
| **Handler** | Un "inspector" del pipeline (Chain of Responsibility). Hace su parte y pasa el testigo. |
| **`AttackContext`** | La "mochila" mutable que viaja por los 7 handlers del ataque. |
| **`AttackData`** | Un ataque parseado del JSON: nombre, costo, daño, texto, efectos tipados. |
| **`parsedEffects`** | Efectos tipados pre-cargados desde `xy1_parsed.json` (la API solo da texto en inglés). |
| **Flyweight** | Una sola instancia compartida y sin estado de cada `*Logic`. Vive en un `*Registry`. |
| **`EffectLogic` / `*Logic`** | La lógica que ejecuta un efecto de ataque. Stateless. |
| **`ConditionStrategy`** | Pregunta sí/no sobre el contexto (ej. "¿el defensor es Grass?"). El Strategy real (§23). |
| **`GameEvent`** | "Algo que pasó" (daño, KO, premio...). Lo que el engine devuelve al cliente. |
| **`pendingSelection`** | La elección que el engine le está pidiendo al jugador (§26). ✅ engine / 🚧 borde DTO. |
| **EX / MEGA** | Pokémon que valen **2 premios** al ser noqueados (el resto vale 1). |
| **`enteredThisTurn`** | Flag: un Pokémon recién jugado no puede evolucionar ni atacar ese turno. |
| **`turnFlags`** | Límites por turno: 1 energía, 1 retirada, 1 supporter, ya atacó. |

### Mapa mental de "¿dónde toco si quiero...?"

| Quiero... | Archivo |
|-----------|---------|
| ...agregar/cambiar una **validación de regla** | `RuleValidator` |
| ...cambiar el **flujo del turno** | `TurnManager` + `state/*` |
| ...implementar un **efecto de ataque** nuevo | `effects/logics/` + registrarlo en `EffectRegistry` |
| ...implementar un **efecto de trainer** | `effects/trainers/` + `TrainerEffectRegistry` |
| ...implementar/extender una **habilidad** | `effects/abilities/` — receta paso a paso en §28.5 |
| ...agregar una **condición de daño** ("if defender is X") | `effects/conditions/` + `ConditionRegistry` |
| ...tocar **cómo se calcula el daño** | `chain/DamageCalculator` + `ModifierHandler` |
| ...cambiar **qué se persiste** | `mappers/EngineStateMapper` (ambas direcciones) |
| ...exponer algo a **Spring/REST** | `GameEngineFacade` (la única puerta) |

---

## 28. Sistema de Habilidades (abilities)

> **Nuevo 2026-06-11** — Bloque 4 tier 1, continuación del change `effect-execution-trainers`.
> 6 de las 12 habilidades del set XY1 funcionan; las otras 6 requieren selección (tier 2).
> Paquete: `engine/effects/abilities/`. Spec: Grupo E del change. Design: D5-D9.

### 28.1 De dónde vienen los datos (no es magia, es configuración)

Las habilidades NO se parsean del texto en el engine. Vienen **pre-parseadas** en
`xy1_parsed.json` (generado por los scripts Python de `docs/abilityParsing/` — cortesía de
bigpickle, son documentación del método). `PokemonTCGApiService.loadParsedEffects()` empaqueta
los tres bloques `{attacks, abilities, trainerEffects}` en `Card.parsedEffects` al sincronizar
el set. Cada familia tiene SU parser que abre SU bloque:

```
Card.parsedEffects (JSON String)
 ├─ "attacks"        → AttackParser        (§18) — efectos de ataque
 ├─ "trainerEffects" → TrainerEffectParser (§24) — efectos de trainers
 └─ "abilities"      → AbilityParser       (§28) — habilidades  ← ESTO antes no lo leía NADIE
```

Forma de una habilidad en el JSON (ejemplo real, Spiky Shield):

```json
{ "name": "Spiky Shield", "text": "If this Pokémon is your Active…",
  "parsedEffects": [{
    "type": "PASSIVE_ABILITY",
    "trigger": "ON_ATTACK_RECEIVED",
    "conditions": [{"type": "IS_ACTIVE", "target": "SELF", "value": "true"}],
    "effect": {"type": "DAMAGE_COUNTERS", "amount": 3, "target": "ATTACKER"},
    "stackable": false }] }
```

⚠️ **El JSON etiqueta TODO como `PASSIVE_ABILITY` y casi todo con trigger `ON_PLAY`** — es un
misnomer de los scripts. La familia REAL se deduce de trigger + texto (28.3).

### 28.2 Las piezas (todas en `engine/effects/abilities/`, Java puro)

| Clase | Rol |
|-------|-----|
| `AbilityParser` | Abre el bloque `"abilities"` → `List<AbilityData>`. Espejo de `TrainerEffectParser`: tolerante, tipo desconocido → `UnknownEffect` inerte, nunca rompe. |
| `AbilityData` | `{name, text, parsedEffects}` + helper `passiveEffects()`. |
| `AbilityCondition` | `{type, target, value}` — el modelo que `PassiveAbilityEffect` antes DESCARTABA al deserializar. |
| `AbilityConditionEvaluator` | Evalúa `IS_ACTIVE` / `HAS_ENERGY` (color vía `CardLookup`) / `HAS_CONDITION`. **Fail-closed**: condición desconocida = false (una habilidad que no entendemos NO se dispara). |
| `AbilityActivationResolver` | Ejecuta el efecto de una habilidad ACTIVADA (tier 1: `DRAW_UNTIL_HAND_SIZE`). Tier 2 → `ABILITY_NOT_SUPPORTED` (nunca consume en silencio). |
| `AbilityTriggerResolver` | Dispara `ON_ATTACK_RECEIVED` / `ON_ALLY_KNOCKOUT` del defensor. `Random` inyectable (lo provee la chain). |
| `ContinuousAbilityQuery` | Responde "¿hay una habilidad continua que modifica esta regla?" — la consultan los subsistemas alterados. |

Subtipos anidados nuevos en `AttackEffect` (modelos): `COIN_FLIP_DAMAGE {headsCondition}`,
`REDUCE_DAMAGE {amount}`, `RESTRICT_ITEMS {target}`, `IMMUNE_TO_CONDITIONS {target}`.

### 28.3 Las TRES familias y dónde se ejecuta cada una

**Decisión de diseño (D5):** se evaluó State y se DESCARTÓ — las habilidades coexisten (no son
modos excluyentes), no transicionan sino que se inyectan en subsistemas ajenos, y el engine ya
usa Flyweight para las otras dos familias de efectos. State participa solo como puerta de
entrada (el `MainPhaseState` rutea `USE_ABILITY`).

| Familia | Cómo entra | Quién ejecuta | Tier 1 |
|---------|-----------|---------------|--------|
| **Activadas** ("Once during your turn, you may…") | `ActionType.USE_ABILITY` (con `targetPosition` + `abilityIndex`) → `MainPhaseState.handleUseAbility` | `AbilityActivationResolver` | Mystical Fire (Delphox: robá hasta 6) |
| **Disparadas** (reaccionan a un evento) | Ganchos en `PostDamageHandler`, DESPUÉS de los efectos del ataque y ANTES del doble KO check | `AbilityTriggerResolver.resolveDefenderTriggers(ctx)` | Spiky Shield (retroceso 30, "even if KO"), Destiny Burst (flip → 50 al atacante) |
| **Continuas** (mientras esté en juego) | Guards de consulta en el subsistema alterado | `ContinuousAbilityQuery` | Forest's Curse (`ITEMS_LOCKED` en `handlePlayItem`), Fur Coat (−20 post-W&R en `DamageApplicationHandler`), Sweet Veil (inmunidad en `ApplyConditionLogic`) |

### 28.4 Detalles de timing que importan

- **Spiky Shield "even if Knocked Out"**: el trigger corre ANTES de `processKo(defensor)` — el
  defensor sigue en su slot cuando dispara. Y el retroceso que mata al atacante lo procesa el
  `processKo(atacante)` que ya existía: cero código nuevo de KO.
- **Fur Coat es "after applying Weakness and Resistance"**: la resta va en
  `DamageApplicationHandler` DESPUÉS de `DamageCalculator` y de los `damageModifiers`, con floor 0.
- **Once-per-turn**: `TurnFlags.abilitiesUsedThisTurn` (clave `posición#nombre`), se limpia solo
  en `resetTurnFlags` (beginTurn). ⚠️ No se persiste por `EngineStateMapper` (deuda A1).
- **Sweet Veil protege desde la banca**: `ContinuousAbilityQuery.isImmuneToConditions` escanea
  activo + banca del dueño; la condition `HAS_ENERGY FAIRY` se evalúa sobre el Pokémon PROTEGIDO.

### 28.5 Cómo agregar una habilidad nueva (receta)

1. ¿Su `effect.type` ya tiene subtipo en `AttackEffect`? Si no: crear el DTO + registrarlo en
   `@JsonSubTypes` (modelos) — test en `AbilityParserTest`.
2. Identificar la familia:
   - **Activada** → case nuevo en `AbilityActivationResolver.activate(...)`.
   - **Disparada** → case en `AbilityTriggerResolver.applyTriggeredEffect(...)` (y si el trigger
     es nuevo, el `switch` de `resolveDefenderTriggers`).
   - **Continua** → método de consulta nuevo en `ContinuousAbilityQuery` + guard en el subsistema
     que altera (validador / daño / condiciones / retiro).
3. ¿Requiere que el jugador ELIJA algo? → es tier 2: necesita el plan resumable
   (`RESUMABLE_ATTACK_EFFECTS_PLAN.md`) + un `SelectionType` nuevo (§26). NO lo improvises con
   un efecto "determinístico": rechazalo con `ABILITY_NOT_SUPPORTED` hasta tener el mecanismo.
4. Test por familia (modelos de referencia): `MainPhaseStateUseAbilityTest`,
   `PostDamageHandlerAbilityTriggerTest`, `ContinuousAbilityQueryTest`.

### 28.6 Tier 2 — las 6 que faltan y por qué

Water Shuriken (descartar energía de mano + elegir blanco), Upside-Down Evolution (buscar en
mazo), Stance Change ×2 (intercambiar con mano), Fairy Transfer (mover energía a elección,
"as often as you like"), Drive Off (el RIVAL elige qué banca sube). Todas piden **selección del
jugador** → dependen del plan resumable / `SelectionType` nuevos. Drive Off es la más barata
(reutiliza el `pendingSelection` de §26 casi directo).

---

---

*Actualizado: 2026-06-11 — cubre sesiones 2–8: engine completo + player-selection (§26) + habilidades tier 1 (§28).*
*Para el estado fino de cada efecto, ver `docs/engine/EFFECT_IMPLEMENTATION_INVENTORY.md` y `EFFECT_IMPLEMENTATION_PROGRESS.md`.*
*Ver ADRs: `docs/architecture/001` (factories), `002` (turn manager), `003` (attack effects flyweight), `004` (trainer effects flyweight).*
