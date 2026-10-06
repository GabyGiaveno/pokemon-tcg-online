# Sudden Death (Muerte Súbita)

> **Estado:** ✅ Completo  
> **Branch:** RamiroBranch  
> **Regla oficial:** XY1 Rulebook — si ambas condiciones de victoria se cumplen simultáneamente, se inicia una nueva mini-partida con 1 sola carta de Premio cada uno. Se repite hasta que haya un ganador.

---

## Contexto y decisiones de diseño

### ¿Cuándo ocurre un empate simultáneo?

En el Pokémon TCG hay exactamente **un** escenario donde ambas condiciones de victoria se pueden cumplir al mismo tiempo:

> El Pokémon Activo del atacante usa un ataque que noquea al Pokémon Activo del defensor, **y** el efecto de recoil (daño a sí mismo) lo noquea también al atacante. Ambos jugadores tomarían su última Prize Card en el mismo momento.

Esto ocurre cuando:
- Ambos activos quedan en 0 HP como resultado de una sola acción (`USE_ATTACK`).
- Ambos jugadores tienen exactamente 1 Prize Card restante.

**No es posible** que ocurra en otros momentos (las Prize Cards se toman de a una, en turnos distintos).

### Flujo de la mini-partida

```
[Partida normal FINISHED con DRAW]
        ↓
[game_session.status = SETUP + suddenDeathRound > 0]
[prize_cards_count = 1 para ambos]
[Re-deal: 4 cartas mano + 1 prize]
[Re-setup: ambos colocan activo]
        ↓
[Mini-partida ACTIVE]
        ↓
    ¿Hay ganador? → SÍ → FINISHED con winnerId
                  → NO (empate de nuevo) → volver arriba
```

### Decisión: misma GameSession, nueva ronda

En lugar de crear una nueva `GameSession`, se reutiliza la misma sesión con un nuevo `BoardState`. Razones:
- El log de acciones queda unificado en la misma sesión.
- El frontend ya está en el board — solo necesita recargar el estado.
- Historial completo de la partida disponible desde un único `game_session_id`.

Se agrega un campo `suddenDeathRound` (int, default 0) a `GameSession` — incrementa en cada ronda de Muerte Súbita. La fuente de verdad persistida para Muerte Súbita es `suddenDeathRound > 0`; `status = SETUP` + `suddenDeathRound > 0` significa setup de una ronda de Muerte Súbita.

---

## Tareas de implementación

### Backend

#### Detección del empate

- [x] **BE-1** ✅ — `VictoryConditionChecker`: agregar detección de empate simultáneo.
  - Evaluar ambos jugadores antes de declarar ganador.
  - Si ambas condiciones son verdaderas al mismo tiempo → retornar `VictoryResult.DRAW`.
  - Si solo una → comportamiento actual sin cambios.
  - Archivo: `engine/VictoryConditionChecker.java`

- [x] **BE-2** ✅ — `VictoryReason`: agregar valor `SIMULTANEOUS_KO`.
  - Archivo: `models/cards/VictoryReason.java`

- [x] **BE-3** ✅ — `GameEventType`: agregar `SUDDEN_DEATH_START`.
  - Archivo: `events/GameEventType.java`

#### Schema / Migración

- [x] **BE-4** ✅ — Migración `V015__add_sudden_death_round.sql`:
  ```sql
  ALTER TABLE game_session ADD COLUMN sudden_death_round INT NOT NULL DEFAULT 0;
  ```
  - Archivo: `db/migration/V015__add_sudden_death_round.sql`

- [x] **BE-5** ✅ — `GameSession` entity: agregar campo `suddenDeathRound`.
  - Archivo: `entities/GameSession.java`

#### Flujo de inicio de Muerte Súbita

- [x] **BE-6** ✅ — `GameStatus`: agregar valor `SUDDEN_DEATH`.
  - Archivo: `models/cards/GameStatus.java`

- [x] **BE-7** ✅ — `GameService.startSuddenDeath(UUID gameId)`:
  - Cargar la `GameSession`.
  - Incrementar `suddenDeathRound`.
  - Setear `status = SETUP`, `prizeCardsCount = 1` y usar `suddenDeathRound > 0` como fuente de verdad.
  - Reinicializar el `BoardState` con `prizeCardsCount = 1` para ambos jugadores (re-deal mano de 4 + 1 prize, re-shuffle mazos).
  - Setear `status = SETUP` en el board para que los jugadores coloquen su Pokémon activo.
  - Persistir y publicar evento `SUDDEN_DEATH_START` por WebSocket.
  - Archivo: `services/GameService.java`

- [x] **BE-8** ✅ — Conectar `PlayerFieldBuilder.withSuddenDeath()` en `buildPlayerBoardFromDeck`:
  - Actualmente el prize count está hardcodeado a 6. Leer `session.getPrizeCardsCount()`.
  - Archivo: `services/GameService.java`

- [x] **BE-9** ✅ — Llamar a `startSuddenDeath()` desde el lugar donde se detecta el empate:
  - En `TurnManager` (o `GameService`), cuando `VictoryConditionChecker` retorna `DRAW`, en lugar de llamar a `finish()`, llamar a `startSuddenDeath()`.
  - Archivo: `engine/TurnManager.java` y/o `services/GameService.java`

#### Tests

- [x] **BE-10** ✅ — `VictoryConditionCheckerTest`: caso de empate simultáneo.
- [x] **BE-11** ✅ — `GameServiceTest`: test de `startSuddenDeath()` — verifica que `prizeCardsCount=1`, `suddenDeathRound` incrementa, board reiniciado y REST expone `suddenDeathRound` para reload/reconnect.
- [ ] **BE-12** — Test E2E (opcional): flujo completo de detección de DRAW → SUDDEN_DEATH → nueva ronda.

---

### Frontend

- [x] **FE-1** ✅ — `GameEventType` / feed service: manejar evento `SUDDEN_DEATH_START`.
  - Mostrar mensaje en el feed: "¡Muerte Súbita! Ronda X — 1 carta de Premio cada uno."
  - Archivo: `features/game/services/game-feed.service.ts`

- [x] **FE-2** ✅ — Overlay de transición "Muerte Súbita":
  - Al recibir `SUDDEN_DEATH_START`, mostrar overlay con animación antes de recargar el board.
  - Mensaje: "¡MUERTE SÚBITA! Ambos jugadores quedaron sin Pokémon al mismo tiempo."
  - Auto-dismiss después de 3 segundos.
  - Archivo: inline en `board-container`.

- [x] **FE-3** ✅ — Recarga del estado del board post-Muerte Súbita:
  - Tras el overlay (3s), recarga el board via `loadGameState`. El `STATE_CHANGED` WS event también recarga si llega primero.
  - El board ya soporta SETUP — no hay cambios en la lógica de colocación de Pokémon.
  - Archivo: `features/game/board-container/board-container.ts`

- [x] **FE-4** ✅ — Indicador de ronda en la UI:
  - Si `suddenDeathRound > 0`, muestra badge "Muerte Súbita — Ronda X" centrado en el header.
  - Archivo: `board-container` template + CSS.

---

## Criterios de aceptación

1. **Empate detectado**: si ambos activos quedan en 0 HP con 1 Prize Card restante cada uno, el juego no declara ganador — emite `SUDDEN_DEATH_START`.
2. **Board reiniciado**: ambos jugadores reciben nueva mano de 4 cartas + 1 Prize Card.
3. **Setup obligatorio**: el juego vuelve a fase SETUP — ambos jugadores deben colocar su Pokémon activo antes de continuar.
4. **Loop**: si vuelve a haber empate simultáneo en la mini-partida, se inicia otra ronda de Muerte Súbita.
5. **Log completo**: cada acción de la mini-partida queda registrada en el mismo `game_action` log de la sesión original.
6. **Frontend informado**: el jugador ve el overlay de transición si recibe WebSocket, y el indicador de ronda se reconstruye desde REST con `suddenDeathRound`.

---

## Archivos clave para orientarse

| Archivo | Rol |
|---|---|
| `engine/VictoryConditionChecker.java` | Detecta condiciones de victoria — acá va la detección del empate |
| `engine/TurnManager.java` | Llama al checker después de cada ataque y entre turnos |
| `engine/state/SetupPhaseState.java` | Lógica del SETUP — ya funciona, no necesita cambios |
| `services/GameService.java` | Orquesta el ciclo de vida de la partida — acá va `startSuddenDeath()` |
| `models/cards/GameStatus.java` | Enum de estados — agregar `SUDDEN_DEATH` |
| `models/cards/VictoryReason.java` | Enum de razones de victoria — agregar `SIMULTANEOUS_KO` |
| `events/GameEventType.java` | Tipos de eventos WebSocket — agregar `SUDDEN_DEATH_START` |
| `entities/GameSession.java` | Entidad JPA — agregar `suddenDeathRound` |
| `engine/PlayerFieldBuilder.java` | `withSuddenDeath()` ya existe — solo hay que llamarlo |
| `features/game/services/game-feed.service.ts` | Maneja eventos del feed en el frontend |
| `features/game/board-container/board-container.ts` | Componente principal del board |
