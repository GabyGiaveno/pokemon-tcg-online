# Reporte de Implementación: Motor de Turnos (Sesión 2)

## Resumen Ejecutivo

Se implementó el núcleo del motor de juego: el ciclo completo de un turno (DRAW → MAIN → ATTACK → BETWEEN_TURNS), incluyendo todas las acciones del jugador en la fase MAIN, el procesamiento de condiciones especiales entre turnos, la detección de KO y las condiciones de victoria. El engine queda completamente aislado de Spring gracias al patrón `CardLookup`.

---

## Nuevos Componentes y Fundamentos Técnicos

### 1. Ciclo de Turno — `TurnManager` (Orquestador)

- **Qué hicimos**: Un orquestador que maneja la secuencia completa de un turno.
- **Flujo**:
  ```
  beginTurn() → resetFlags → DRAW (auto) → MAIN (espera input)
  processAction(USE_ATTACK) → ATTACK → BETWEEN_TURNS (auto) → switch player → beginTurn()
  processAction(END_TURN)   →           BETWEEN_TURNS (auto) → switch player → beginTurn()
  ```
- **Por qué**: Centralizar las transiciones de fase evita que los estados se "llamen entre sí" (lo que generaría acoplamiento circular). `TurnManager` es el único que conoce el orden del ciclo.

### 2. Patrón State — `GamePhaseState` + 4 implementaciones

- **Qué hicimos**: Interfaz con dos métodos por defecto que lanzan `UnsupportedOperationException`: `handle()` (fases interactivas) y `execute()` (fases automáticas).
  - `DrawPhaseState` — automática: roba carta, detecta deck-out, aplica regla del primer turno.
  - `MainPhaseState` — interactiva: maneja las 8 acciones del jugador.
  - `AttackPhaseState` — interactiva: stub que delega a `AttackResolutionChain` (pendiente).
  - `BetweenTurnsState` — automática: efectos de estado → KO check → `firstPlayerHasActed`.
- **Por qué**: Cada fase encapsula su propia lógica. `TurnManager` no necesita un `switch(phase)` gigante para saber qué hacer; simplemente delega al estado correcto.

### 3. `RuleValidator` — Validador Estático Puro

- **Qué hicimos**: Clase de métodos estáticos que valida pre-condiciones para cada acción. Lanza `InvalidActionException(errorCode, message)` ante cualquier violación de regla.
- **Acciones cubiertas**: PLAY_BASIC_POKEMON, EVOLVE_POKEMON, ATTACH_ENERGY, ATTACH_TOOL, RETREAT, PLAY_ITEM, PLAY_SUPPORTER, PLAY_STADIUM, USE_ATTACK.
- **Por qué**: Sin estado propio, sin dependencias de Spring, testeable de forma aislada con un simple `assertThrows`. Los error codes en mayúsculas (`"CANNOT_EVOLVE_FIRST_TURN"`) son machine-readable y facilitan el manejo en el frontend.

### 4. `CardLookup` — Bridge hacia Spring (Patrón Strategy)

- **Qué hicimos**: `@FunctionalInterface` que actúa como la única conexión entre el engine puro y la capa de persistencia.
  ```java
  CardLookup lookup = cardCacheService::findById; // en GameEngineFacade
  CardLookup lookup = id -> testCards.get(id);    // en tests unitarios
  ```
- **Por qué**: Mantiene el engine completamente independiente de Spring. Los tests no necesitan levantar un `ApplicationContext`; se pasa un map de cartas de prueba como lambda.

### 5. `StatusEffectManager` — Procesador de Condiciones

- **Qué hicimos**: Procesa los efectos entre turnos en el orden exacto de la spec: veneno (10 daño fijo) → quemadura (flip: cara = cura, cruz = 20 daño) → sueño (flip: cara = despierta) → parálisis (cura automática).
- **Constructor con `Random` inyectable**: La clase acepta un `Random` en el constructor para permitir tests deterministas sin mocks de framework.
- **Por qué el daño de veneno NO está en `DamageCalculator`**: El `DamageCalculator` existe para el pipeline de ataque (debilidad, resistencia, modificadores). El daño de estado es fijo, sin modificadores, y ocurre en una fase completamente distinta. Mezclarlos violaría SRP.

### 6. `KnockoutProcessor` — Procesador de KO

- **Qué hicimos**: Dado un `ActivePokemon` con HP ≤ 0, procesa: descarte del pokemon y sus adjuntos, entrega de Prize Card al oponente, detección de victoria por prizes, promoción automática desde la banca.
- **TODOs documentados**:
  - EX/MEGA knockouts deberían otorgar 2 Prize Cards (pendiente).
  - El jugador debería elegir qué Prize Card tomar (por ahora se toma índice 0).
  - El jugador debería elegir qué Pokémon de banca promover (por ahora se promueve índice 0).

### 7. Modelos Actualizados

| Clase | Campo agregado | Motivo |
|---|---|---|
| `BoardState` | `matchState`, `winnerId` | El engine necesita señalar FINISHED y al ganador |
| `PlayerField` | `playerTurnCount` | Para detectar "primer turno del jugador" sin ambigüedad |
| `ActivePokemon` | `enteredThisTurn` | Previene evolucionar en el mismo turno que entró al juego |
| `BenchPokemon` | `enteredThisTurn` | Ídem — se aplica tanto al activo como a la banca |

### 8. `ActionRequest` / `ActionResult` — DTOs Completos

- `ActionRequest`: 6 campos cubre todas las acciones (`cardId`, `targetPosition`, `attackIndex`, `benchIndex`, `prizeIndex`). `targetPosition` usa el formato `"ACTIVE"` / `"BENCH_0..4"` para evitar ambigüedad con copias de la misma carta.
- `ActionResult`: Factory methods `success(events)` y `failure(error)`. Los eventos acumulan **todo** lo que pasó en esa acción más las fases automáticas posteriores (un solo USE_ATTACK devuelve: evento de ataque + efectos de estado + KO + draw del rival).

---

## Decisiones de Diseño

### Turno completo en una sola respuesta
Cuando el jugador envía `USE_ATTACK` o `END_TURN`, el `TurnManager` encadena automáticamente BETWEEN_TURNS + switch de jugador + DRAW del siguiente. El `ActionResult` devuelve **todos** los eventos en orden. La Facade los retransmite por WebSocket en secuencia. Esto simplifica el estado en el cliente: no hay que esperar confirmaciones intermedias de fases automáticas.

### Promoción automática de Pokémon tras KO
Cuando un Pokémon es KO'd, el sistema promueve automáticamente el primero de la banca. Esto es una simplificación consciente documentada como TODO. La implementación completa requeriría un estado "esperando input del jugador" que interrumpe el flujo del turno — se diseñará junto con el sistema de acciones opcionales (TAKE_PRIZE_CARD, CHOOSE_ACTIVE).

### `enteredThisTurn` vs `playerTurnCount`
Dos restricciones distintas, dos flags distintos:
- `enteredThisTurn` en el modelo de Pokémon → "este Pokémon específico no puede evolucionar aún"
- `playerTurnCount` en `PlayerField` → "este jugador no puede evolucionar ni atacar en su primer turno"

---

## Pendiente (Próxima Sesión)

| Componente | Estado |
|---|---|
| `AttackResolutionChain` + 7 handlers | Stub — estructura definida, sin lógica |
| `DamageCalculator` | Stub — vacío |
| `TrainerEffectRegistry` + efectos XY1 | No iniciado |
| `GameEngineFacade` | Stub — vacío |
| Tests unitarios del engine | No iniciados |
