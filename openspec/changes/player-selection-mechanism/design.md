# Design: Player Selection Mechanism

## Technical Approach

Selección por **estado**: cuando un punto de resolución necesita input, **setea `pendingSelection` en `BoardState` y NO actúa** (no promueve, no completa el turno). El orquestador (`TurnManager`) detecta `pendingSelection != null` y **corta limpio**, devolviendo los eventos acumulados. Un nuevo entry point `resolveSelection(playerId, choice)` valida, ejecuta la acción elegida y **reanuda** el flujo. No hay nuevo `GamePhaseState`; la pausa es un campo aditivo + guarda en el orquestador.

## Architecture Decisions

### Decision: Reificación de la continuación

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| `Runnable`/lambda guardada en el board | NO sobrevive a la serialización: `resolveSelection` llega en un request HTTP posterior; el board se reconstruye desde storage entre requests → la lambda se pierde | ❌ |
| Command object con referencias a objetos del engine | Mismo problema de serialización + acopla el board a clases del engine | ❌ |
| **`SelectionType` (enum) + datos planos** (ownerPlayerId, opciones como IDs) | Serializable, testeable, el `switch` de resume vive en `TurnManager`/handler, no en el board | ✅ |

El board es **datos**, no comportamiento. La continuación se reconstruye desde el `SelectionType` + el estado del board al resolver.

### Decision: Punto de corte (abort) y resume

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| Excepción de control para desenrollar la pila | Anti-patrón; oscurece el flujo; rompe el contrato `List<GameEvent>` | ❌ |
| Flag chequeado en cada handler de la cadena | Toca ~7 archivos; invasivo | ❌ |
| **Cooperativo en el orquestador**: el sitio profundo solo setea `pendingSelection`; `TurnManager` chequea y corta | Mínimo toque; `KnockoutProcessor` deja de promover y setea la selección; la promoción se mueve a `resolveSelection` | ✅ |

**Resume sin puntero explícito:** el `currentPhase` del board ya codifica dónde estábamos. KO en ataque → `phase == ATTACK` → resume = `completeTurn(...)`. KO entre turnos → `phase == BETWEEN_TURNS` → resume = resto de `completeTurn` (switch player + beginTurn). El `resolveSelection` ramifica por `currentPhase`.

### Decision: la victoria la decide el orquestador, no el KO (SRP)

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| `KnockoutProcessor` setea `FINISHED`/`winnerId` (estado actual) | Mezcla mutación con decisión de fin de juego; duplica la lógica que YA vive en `VictoryConditionChecker` (que hoy está **huérfano**, nunca invocado); además colisiona con la pausa por selección (¿pausar o terminar?) | ❌ |
| **`processIfKnockedOut` solo muta y reporta; el orquestador llama `VictoryConditionChecker` post-KO** | El KO *habilita* la victoria, no la decide. Una sola fuente de verdad (el checker). El orquestador resuelve la precedencia: **victoria gana sobre selección** (si ganaste por premios, se limpia el `pendingSelection`) | ✅ |

**Consecuencias:**
- `KnockoutProcessor` deja de setear `FINISHED`/`winnerId`/emitir `GAME_FINISHED` (en ambos casos: premios agotados y banca vacía). Solo descarta, reparte premios y setea `pendingSelection` (o nada si no hay banca).
- `VictoryConditionChecker` (hoy código muerto) se **conecta** en los dos puntos post-KO del orquestador: tras `attackPhase.handle` (en `handleAttackFlow`) y tras `betweenTurnsPhase.execute` (en `completeTurn`).
- El **evento `GAME_FINISHED`** lo emite ahora el orquestador cuando `checkVictoryConditions(board) == true` (el checker decide, el orquestador narra).
- **Orden post-KO** en cada punto: (1) checker decide victoria; (2) si `FINISHED` → limpiar `pendingSelection` + emitir `GAME_FINISHED` + cortar; (3) si hay `pendingSelection` → pausar; (4) si no → continuar.

## Data Flow

```
USE_ATTACK ─→ handleAttackFlow ─→ attackPhase.handle ─→ chain ─→ PostDamageHandler
                     │                                                   │
                     │                                    KO + bench≠∅ → set pendingSelection
                     ▼ (pendingSelection != null?)                       │ (NO promueve)
            corta: return events  ◀───────────────────────────────────────┘
                     ╎  ... request posterior ...
resolveSelection(playerId, choice) ─→ valida ─→ promueve elegido ─→ clear pendingSelection
                     │
                     └─→ resume por currentPhase: ATTACK→completeTurn · BETWEEN_TURNS→switch+beginTurn
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `models/game/PendingSelection.java` | Create | Record/POJO: `SelectionType type`, `Long ownerPlayerId`, `List<String> validOptions` (cardIds de banca), prompt |
| `models/game/SelectionType.java` | Create | Enum: `CHOOSE_ACTIVE_ON_KO` (extensible) |
| `models/game/BoardState.java` | Modify | Campo nullable `pendingSelection` |
| `models/cards/ActionType.java` | Modify | `RESOLVE_SELECTION` |
| `engine/KnockoutProcessor.java` | Modify | Banca≠∅ → setear `pendingSelection` en vez de auto-promover; extraer `promote(BenchPokemon)` reusable; **quitar la decisión de victoria** (no setea `FINISHED`/`winnerId`) |
| `engine/TurnManager.java` | Modify | Guarda en `processAction` (solo `RESOLVE_SELECTION` si hay pendiente); corte tras `attackPhase.handle` y dentro de `completeTurn`; nuevo `resolveSelection`; **invocar `VictoryConditionChecker` post-KO** y emitir `GAME_FINISHED` |
| `engine/VictoryConditionChecker.java` | (Conectar) | Pasa de huérfano a fuente única de decisión de victoria; actualizar javadoc (ya no "red de seguridad de `KnockoutProcessor`") |
| `engine/SelectionResolver.java` | Create | **SRP**: valida + aplica la selección (dispatch por `SelectionType`). El `TurnManager` solo reanuda. Prepara Open/Closed para los próximos `SelectionType` (search/switch/look/damage-to-bench) sin tocar el orquestador |
| `models/game/VictoryReason.java` | Create | Enum del motivo de victoria + `describe(winnerId)` (la narración vive en el dominio, no en el orquestador) |
| `dtos/request/ActionRequest.java` | Modify (verificar) | Llevar el `choice` (cardId elegido) |

## Interfaces / Contracts

```java
public final class PendingSelection {
    SelectionType type;          // CHOOSE_ACTIVE_ON_KO
    Long ownerPlayerId;          // quién debe elegir
    List<String> validOptions;   // cardIds candidatos (banca del dueño)
    String prompt;               // texto opcional para UI
}
// TurnManager
ActionResult resolveSelection(BoardState board, Long playerId, String choice, CardLookup cardLookup);
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | `KnockoutProcessor`: banca≠∅ setea pendingSelection y NO promueve; banca=∅ derrota | JUnit puro, sin Spring. **Actualizar `KnockoutProcessorTest`** (hoy espera auto-promote de `bench[0]`) |
| Unit | `TurnManager.resolveSelection`: válida promueve+reanuda; inválida/jugador erróneo falla sin mutar; guarda bloquea otras acciones | Board armado a mano |
| Integration | KO por ataque y KO por veneno (between-turns) → ambos pausan y reanudan completo | Flujo end-to-end con board real |
| Regresión | Sin selección, los 130 tests verdes siguen igual | `./mvnw test` |

## Migration / Rollout

Aditivo. `pendingSelection` nullable; sin setearlo, todo igual. **Breaking interno:** los asserts de auto-promote en `KnockoutProcessorTest` cambian de comportamiento — se actualizan en el mismo commit.

## Open Questions

- [ ] ¿`ActionRequest` ya tiene un campo libre para el `choice` (cardId), o hay que agregarlo? (verificar en apply)
- [ ] KO entre-turnos: confirmar a quién pertenece el activo KO para fijar `ownerPlayerId` (es el jugador del turno saliente).
