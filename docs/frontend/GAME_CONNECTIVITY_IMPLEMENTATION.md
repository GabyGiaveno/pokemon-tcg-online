# FE E2E Connectivity — Sesión 11/06/2026

## 🎯 Objetivo

Conectar el frontend con el backend para el flujo completo: lobby → creación/unión de partida → juego con WebSocket → acciones en tiempo real.

---

## ✅ Lo que se implementó

### 1. Tipo `AvailableAction` + `BackendGameActionType`

**Archivos:**
- `FE/src/app/features/game/models/available-action.type.ts` — tipo `AvailableAction`
- `FE/src/app/features/game/models/backend-game-action-type.ts` — enum con `ATTACK`, `RETREAT`, `USE_TRAINER`, `SETUP`, `CONCEDE`

Incluye los actions `SETUP` (iniciar partida como host) y `CONCEDE` (rendirse).

---

### 2. Action services — api + mock

**Archivos:**
- `FE/src/app/features/game/data-access/player-action-api.service.ts` — implementación real HTTP
- `FE/src/app/features/game/data-access/player-action-mock.service.ts` — mock con delays
- `FE/src/app/features/game/data-access/player-action-source.ts` — token de inyección `PLAYER_ACTION_SOURCE`
- `FE/src/app/features/game/data-access/player-action.service.ts` — wrapper que inyecta el source

El service expone `performAction(gameId, action)` que envía la acción al backend. Se switchea entre real/mock desde `app.config.ts`.

---

### 3. `WebsocketStompService`

**Archivo:** `FE/src/app/core/services/websocket-stomp.service.ts`

Servicio real de WebSocket usando STOMP sobre SockJS. Conecta a `/ws`.

> Contrato vigente: [`../api/REALTIME_WEBSOCKET.md`](../api/REALTIME_WEBSOCKET.md).

Features:
- Reconexión automática (máx. 5 intentos)
- Señal `system/reconnected` para recargar el snapshot REST después de reconectar
- Suscripción a tópicos STOMP oficiales
- Publicación de mensajes
- Implementa la interface `WebsocketSource` para poder switchear con mock

**Token de inyección:** `FE/src/app/core/services/websocket-source.ts` → `WebsocketSource`

---

### 4. `app.config.ts` — modo API + WebSocket

**Archivo:** `FE/src/app/app.config.ts`

Cambios:
- `USE_API` ahora es `true` (modo real contra backend)
- `LOBBY_SOURCE` usa `LobbyHttpService` (ya estaba configurable)
- Se agregó provider para `WebsocketSource` usando `WebsocketStompService`
- `PLAYER_ACTION_SOURCE` usa el service real

---

### 5. Pantalla de juego — Board + SETUP + WS subscription

**Archivos:**
- `FE/src/app/features/game/game.routes.ts` — ruta `/game/:gameId` con `GameBoardCmp`
- `FE/src/app/features/game/game-page.ts` — página principal del juego
- `FE/src/app/features/game/components/game-board/game-board.ts` — **board component**
- `FE/src/app/features/game/components/game-board/game-board.html` — template del board
- `FE/src/app/features/game/components/game-board/game-board.css` — estilos del board
- `FE/src/app/features/game/components/game-board/board-state.ts` — tipo `BoardState`

El board:
- Se suscribe a WebSocket al entrar (`/topic/games/{gameId}/events` y `/topic/games/{gameId}/state-changed`)
- Usa `/events` para log/animaciones y `/state-changed` para ejecutar `GET /api/games/{gameId}/state`
- Muestra pantalla de **SETUP** si el estado del juego es `SETUP` y el usuario es el host — botón "Iniciar partida"
- Muestra el board real si `status !== 'SETUP'`

---

### 6. `game-state.mapper.ts` — available actions reales

**Archivo:** `FE/src/app/features/game/mappers/game-state.mapper.ts`

Recibe `GameStateResponse` del backend y mapea `availableActions` del backend a `AvailableAction[]` del frontend.

**Fix aplicado:** Usa notación de bracket para propiedades con camelCase del backend:
```ts
action['actionType']  // en vez de action.actionType
```

---

### 7. Fix: `global is not defined` (sockjs-client)

**Archivo:** `FE/src/index.html`

`SockJS` necesita `global` en el browser. Se agregó polyfill:
```html
<script>window.global = window;</script>
```

---

## ❌ Error detectado y revertido — Confusión de lobbies

**El proyecto TIENE DOS lobbies:**

| Ubicación | Estado |
|-----------|--------|
| `FE/src/app/features/auth/lobby/lobby.ts` | **✅ ORIGINAL — completo, con HTML/CSS/imágenes** |
| `FE/src/app/features/lobby/lobby/lobby.ts` | ❌ Stub vacío, creado para una versión alternativa |

**Lo que pasó:**
1. La ruta `/lobby` en `app.routes.ts` importaba correctamente de `./features/auth/lobby/lobby`
2. Al ver el path, se asumió que era un bug porque también existía `features/lobby/lobby/`
3. Se "fixeó" apuntando al lobby nuevo, pisando el contenido del stub
4. **Resultado:** el usuario veía un lobby diferente al que había construido

**Ya está revertido:**
- ✅ `app.routes.ts` importa de `./features/auth/lobby/lobby` otra vez
- ✅ `features/lobby/lobby/lobby.ts` volvió al stub original: `export class Lobby {}`
- ✅ Archivos muertos eliminados (`game-lobby.service.ts`, `lobby.html`, `lobby.css`)

**Lección:** siempre verificar AMBOS extremos de un import antes de declararlo roto.

---

## 🧩 El lobby ORIGINAL (features/auth/lobby/)

- Componente: `LobbyPage`
- Template: header con avatar/XP, estadísticas, botón JUGAR con panel de partidas, navegación a Colección/Perfil/Mi mazo
- Pide **Deck ID manual** (input numérico)
- Usa `LobbyApiService` → `LOBBY_SOURCE` → `LobbyHttpService`
- Crea partida con `POST /api/game-sessions` y redirige a `/game/{gameId}`
- Se une a partida con `POST /api/game-sessions/{gameId}/join` y redirige a `/game/{gameId}`

**Archivos:**
- `FE/src/app/features/auth/lobby/lobby.ts`
- `FE/src/app/features/auth/lobby/lobby.html`
- `FE/src/app/features/auth/lobby/lobby.css`
- `FE/src/app/features/auth/lobby/imagen-A.png`
- `FE/src/app/features/auth/lobby/imagen-B.png`
- `FE/src/app/features/auth/data-access/lobby-api.service.ts`
- `FE/src/app/features/auth/data-access/lobby-api.types.ts`
- `FE/src/app/features/auth/data-access/lobby-http.service.ts`
- `FE/src/app/features/auth/data-access/lobby-mock.service.ts`
- `FE/src/app/features/auth/data-access/lobby-source.ts`

---

## 🔧 Estado actual del flujo completo

```
Login → /lobby (auth/lobby) → Deck ID manual → Crear/Unirse → /game/{id}
                                                              ↓
                                              WebSocket suscribe a /topic/game/{id}
                                                              ↓
                                              Si status=SETUP y soy host → botón "Iniciar"
                                                              ↓
                                              Board con acciones disponibles
```

---

## 📋 Pendiente / A mejorar

### Mazos predeterminados al registrar
El backend **NO crea mazos al registrar**. El usuario arranca con 0 mazos.
- `PlayerService.register()` solo crea el `Player`
- No hay seed data, ni `@PostConstruct`, ni migraciones con INSERTs
- Para tener mazos por defecto, hay que implementarlo desde cero

**Posibles approaches:**
1. En `PlayerService.register()`: crear automáticamente N mazos con cartas predefinidas
2. Endpoint separado que el frontend llame post-registro
3. Seed data en migración Flyway

### Selector de mazos en lugar de input manual
El lobby actual pide un Deck ID numérico. Sería mejor un dropdown con los mazos del usuario.

### Pantalla de juego — refinamientos
- Mostrar cartas reales del jugador
- Animaciones de acciones
- Manejo de errores de WebSocket (reconexión)

---

## 📁 Archivos relevantes creados/modificados

| Archivo | Acción |
|---------|--------|
| `FE/src/app/features/game/models/available-action.type.ts` | ✨ Creado |
| `FE/src/app/features/game/models/backend-game-action-type.ts` | ✨ Creado |
| `FE/src/app/features/game/data-access/player-action-api.service.ts` | ✨ Creado |
| `FE/src/app/features/game/data-access/player-action-mock.service.ts` | ✨ Creado |
| `FE/src/app/features/game/data-access/player-action-source.ts` | ✨ Creado |
| `FE/src/app/features/game/data-access/player-action.service.ts` | ✨ Creado |
| `FE/src/app/core/services/websocket-stomp.service.ts` | ✨ Creado |
| `FE/src/app/core/services/websocket-source.ts` | ✨ Creado |
| `FE/src/app/features/game/game.routes.ts` | ✨ Creado |
| `FE/src/app/features/game/game-page.ts` | ✨ Creado |
| `FE/src/app/features/game/components/game-board/game-board.ts` | ✨ Creado |
| `FE/src/app/features/game/components/game-board/game-board.html` | ✨ Creado |
| `FE/src/app/features/game/components/game-board/game-board.css` | ✨ Creado |
| `FE/src/app/features/game/components/game-board/board-state.ts` | ✨ Creado |
| `FE/src/app/features/game/mappers/game-state.mapper.ts` | ✨ Creado |
| `FE/src/app/app.config.ts` | ✏️ Modificado (modo API, WS provider) |
| `FE/src/index.html` | ✏️ Modificado (polyfill `global`) |
| `FE/src/app/app.routes.ts` | ✏️ Modificado (authGuard en lobby) → revertido import |
| `FE/src/app/features/game/data-access/player-action-api.service.ts` | ✏️ Modificado (url endpoint) |
