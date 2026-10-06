# API_SPEC — Contratos REST y WebSocket

Contratos completos de todos los endpoints HTTP y canales WebSocket.
Todos los endpoints (excepto `/api/auth/register`, `/api/auth/login`, `GET /api/cards`, `GET /api/cards/{id}`, `/ping`) requieren `Authorization: Bearer <token>`.

---

## REST API

### Auth

| Método | Ruta | Body | Respuesta exitosa | Errores |
|--------|------|------|------------------|---------|
| `POST` | `/api/auth/register` | `{username, email, password}` | `201 {id, username, token}` (JWT real, no placeholder) | `409` username o email ya existe |
| `POST` | `/api/auth/login` | `{username, password}` | `200 {id, username, token}` (JWT real) | `401` credenciales inválidas |

> Passwords se guardan hasheados con BCrypt. El token JWT contiene `username` como subject y `playerId` como claim.

### Decks

| Método | Ruta | Body | Respuesta exitosa | Errores |
|--------|------|------|------------------|---------|
| `GET` | `/api/decks` | — | `200 [{id, name, isValid, cardCount}]` | `401` |
| `GET` | `/api/decks/{id}` | — | `200 {id, name, isValid, cardCount}` | `401`, `403`, `404` |
| `POST` | `/api/decks` | `{name, cards:[{cardId, quantity}]}` | `201 {id, name, isValid, cardCount, validationErrors[]}` | `400`, `401` |
| `PUT` | `/api/decks/{id}` | `{name, cards:[{cardId, quantity}]}` | `200 {id, name, isValid, cardCount, validationErrors[]}` | `400`, `401`, `403`, `404` |
| `DELETE` | `/api/decks/{id}` | — | `204` | `401`, `403`, `404` |
| `POST` | `/api/decks/{id}/validate` | — | `200 {id, name, isValid, cardCount, validationErrors[]}` | `401`, `403`, `404` |

> `validationErrors` es vacío si el mazo es válido. Los `code` posibles: `WRONG_TOTAL`, `TOO_MANY_COPIES`, `TOO_MANY_ACE_TACTICIAN`, `NO_BASIC_POKEMON`.
> **Ownership**: cada jugador solo puede ver/editar/borrar/validar sus propios mazos. Acceso a mazo ajeno devuelve `403 Forbidden`.
> Ya no se usa `X-Player-Id`. La identidad se resuelve desde el JWT.

### Cards (caché local de pokemontcg.io)

| Método | Ruta | Query params | Respuesta exitosa | Errores |
|--------|------|-------------|------------------|---------|
| `GET` | `/api/cards` | `set`, `name`, `supertype`, `type`, `page=0`, `size=20` | `200 {data:[], total, page, size}` | `400` |
| `GET` | `/api/cards/{id}` | — | `200 CardResponse` (todos los campos) | `404` |
| `POST` | `/api/cards/sync` | `set` (obligatorio) | `202 {message, set, cardsImported}` o `{message, set, error}` | `400`, `401` |

> `GET /api/cards` y `GET /api/cards/{id}` son endpoints públicos (no requieren token).  
> `CardResponse` incluye: id, name, supertype, subtypes, hp, types, cardSetId, cardSetName, imageUrlSmall, imageUrlLarge, aceTactician, evolvesFrom, attacks, weaknesses, resistances, retreatCost.  
> `POST /api/cards/sync` es autenticado. Responde 202. Si falla la API externa devuelve `error` en el body (no 500).

### Game Sessions

| Método | Ruta | Body | Respuesta exitosa | Errores |
|--------|------|------|------------------|---------|
| `POST` | `/api/games` | `{deckId}` | `201 {gameId, status:"WAITING"}` | `400`, `401`, `422` mazo inválido |
| `GET` | `/api/games` | — | `200 [{gameId, status, player1Username, player2Username, createdAt}]` | `401` |
| `POST` | `/api/games/{id}/join` | `{deckId}` | `200 {gameId, status:"ACTIVE"}` | `400`, `401`, `403`, `404`, `409` (no WAITING) |
| `GET` | `/api/games/{id}/state` | — | `200 BoardStateDTO` (filtrado por jugador autenticado) | `401`, `403`, `404` |

### Game Actions

`POST /api/games/{id}/actions`

**Body plano — sin `payload` anidado.**

| Campo | Tipo | Obligatorio | Descripción |
|-------|------|-------------|-------------|
| `type` | `ActionType` | ✅ | Tipo de acción |
| `cardInstanceId` | `string` (UUID) | Según acción | ID de la copia concreta de la carta dentro de la partida. Devuelto por `BoardStateDTO.myField.hand[].instanceId`. **Preferido sobre `cardId`.** |
| `cardId` | `string` | Alternativo | ID global de la carta (pokemontcg.io, ej. `"xy1-1"`). Alternativa cuando no se conoce el `instanceId`. |
| `targetPosition` | `string` | Según acción | `"ACTIVE"` o `"BENCH_0"` – `"BENCH_4"` |
| `attackIndex` | `int` | Para `USE_ATTACK` | Índice 0-based del ataque a ejecutar |
| `benchIndex` | `int` | Para `RETREAT` | Slot 0-based de la banca para retirar |
| `prizeIndex` | `int` | Para `TAKE_PRIZE_CARD` | Slot 0-based de la carta premio |

**Campos requeridos por acción:**

| `type` | Campos requeridos |
|--------|-------------------|
| `DRAW_CARD` | — |
| `PLAY_BASIC_POKEMON` | `cardInstanceId` o `cardId`, `targetPosition` |
| `EVOLVE_POKEMON` | `cardInstanceId` o `cardId`, `targetPosition` |
| `ATTACH_ENERGY` | `cardInstanceId` o `cardId`, `targetPosition` |
| `USE_ATTACK` | `attackIndex` |
| `RETREAT` | `benchIndex` |
| `END_TURN` | — |
| `CONCEDE` | — |
| `PLAY_ITEM` / `PLAY_SUPPORTER` / `PLAY_STADIUM` | `cardInstanceId` o `cardId` |
| `ATTACH_TOOL` | `cardInstanceId` o `cardId`, `targetPosition` |

**Ejemplo — PLAY_BASIC_POKEMON con instanceId:**
```json
{
  "type": "PLAY_BASIC_POKEMON",
  "cardInstanceId": "p1-xy1-1-a1b2c3d4",
  "targetPosition": "ACTIVE"
}
```

> **cardInstanceId vs cardId:** `cardInstanceId` es el UUID de una copia concreta de carta en esta partida (ej. `"p1-xy1-1-a1b2c3"`). `cardId` es el ID global de pokemontcg.io (ej. `"xy1-1"`). La API resuelve `cardInstanceId` internamente al ID que el engine necesita. Siempre que sea posible, el frontend debe usar `cardInstanceId`.

Respuesta exitosa (GameActionApiResponse):
```json
{
  "success": true,
  "actionType": "PLAY_BASIC_POKEMON",
  "events": [
    {
      "type": "CARD_PLAYED",
      "payload": { "cardId": "xy1-1", "targetPosition": "ACTIVE" },
      "timestamp": "2026-05-30T12:00:00"
    }
  ]
}
```

Respuesta de error (GameActionApiResponse):
```json
{
  "success": false,
  "actionType": "PLAY_BASIC_POKEMON",
  "events": [],
  "error": "Card not in hand"
}
```

> Las acciones inválidas (fase incorrecta, no es tu turno, carta no encontrada, etc.) devuelven `200` con `success: false`, el `actionType` espejado y un mensaje de error descriptivo en `error`.
> La API no decide si una jugada es válida. Toda validación de reglas la resuelve la fachada/engine.
> El frontend NO debe usar la respuesta de actions para reconstruir el tablero completo. Debe escuchar WebSocket `/topic/games/{gameId}/state-changed` y llamar `GET /api/games/{gameId}/state` con su JWT.
> Los errores del sistema (autenticación, not found, permisos) siguen el esquema estándar `{timestamp, status, error, message}` con códigos HTTP adecuados (401, 403, 404, 409).

---

## BoardStateDTO — estructura

```json
{
  "gameId": "uuid",
  "currentPlayerId": 123,
  "phase": "MAIN",
  "turnNumber": 5,
  "myField": PlayerFieldDTO,
  "opponentField": OpponentFieldDTO
}
```

> **Importante:** Inmediatamente después de `joinGame`, `activePokemon` es `null` y `bench` está vacío para ambos jugadores. No hay selección automática de Pokémon activo. El jugador debe enviar `PLAY_BASIC_POKEMON` como primera acción para colocar un Pokémon en `ACTIVE`.

### PlayerFieldDTO (jugador autenticado)

```json
{
  "activePokemon": ActivePokemonDTO,
  "bench": [BenchPokemonDTO],
  "hand": [
    { "instanceId": "p1-xy1-1-a1b2c3", "cardId": "xy1-1", "name": null },
    { "instanceId": "p1-base1-4-d5e6f7", "cardId": "base1-4", "name": null }
  ],
  "deckSize": 35,
  "prizeCards": [
    { "instanceId": "p1-xy7-5-g8h9i0", "cardId": "xy7-5", "name": null }
  ],
  "discardPile": [
    { "instanceId": "p1-base2-3-j1k2l3", "cardId": "base2-3", "name": null }
  ]
}
```

### OpponentFieldDTO (oponente)

```json
{
  "activePokemon": ActivePokemonDTO,
  "bench": [BenchPokemonDTO],
  "handSize": 6,
  "deckSize": 30,
  "prizeCards": [null, null, null, null, null, null],
  "discardPile": [
    { "instanceId": "p2-sm1-2-m4n5o6", "cardId": "sm1-2", "name": null }
  ]
}
```

> La mano y premios del oponente están ocultos: `handSize` es un número, `prizeCards` es un array de `null`s.
> El descarte (`discardPile`) es información pública para ambos jugadores, expuesto como `CardInstanceDTO[]`.
> `name` es opcional y puede ser `null`; el frontend debe usar `cardId` para mostrar la carta.

### CardInstanceDTO

```json
{
  "instanceId": "p1-xy1-1-a1b2c3",
  "cardId": "xy1-1",
  "name": null
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `instanceId` | `string` | UUID único de esta copia concreta dentro de la partida. Usar este ID en las acciones (`cardInstanceId`). |
| `cardId` | `string` | ID global pokemontcg.io (ej. `"xy1-1"`). Usar para buscar imagen/nombre. |
| `name` | `string?` | Nombre de la carta (opcional, puede ser `null`). |

### ActivePokemonDTO

```json
{
  "instanceId": "p1-xy1-1-a1b2c3",
  "cardId": "xy1-1",
  "hp": 60,
  "attachedEnergies": [
    { "instanceId": "p1-base1-99-z0x9w8", "cardId": "base1-99", "name": null }
  ],
  "toolCard": null,
  "conditions": []
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `instanceId` | `string` | UUID del Pokémon en juego |
| `cardId` | `string` | ID pokemontcg.io |
| `hp` | `int` | HP actual |
| `attachedEnergies` | `CardInstanceDTO[]` | Energías adjuntas |
| `toolCard` | `CardInstanceDTO?` | Herramienta adjunta (nullable) |
| `conditions` | `string[]` | Condiciones especiales (ej. `"ASLEEP"`) |

### BenchPokemonDTO

```json
{
  "instanceId": "p1-xy2-2-v7u8t9",
  "cardId": "xy2-2",
  "hp": 70,
  "attachedEnergies": []
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `instanceId` | `string` | UUID del Pokémon en banca |
| `cardId` | `string` | ID pokemontcg.io |
| `hp` | `int` | HP actual |
| `attachedEnergies` | `CardInstanceDTO[]` | Energías adjuntas |

---

## WebSocket (Real-time Events)

> Fuente de verdad del contrato realtime: [`REALTIME_WEBSOCKET.md`](REALTIME_WEBSOCKET.md).
> Esta sección resume los endpoints y payloads principales.

**Protocolo**: STOMP sobre SockJS
**Endpoint de conexión**: `/ws`
**Broker prefix**: `/topic`
**App prefix**: `/app`

> ⚠️ **Arquitectura**: Toda acción de juego se envía por REST (`POST /api/games/{id}/actions`)
> con JWT en header. El backend publica eventos en tiempo real por STOMP.
> El frontend **no envía acciones por WebSocket** — solo las recibe.

### Conexión

```javascript
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  connectHeaders: {
    Authorization: `Bearer ${jwt}`
  }
});
client.activate();
```

La conexión WebSocket sí requiere autenticación:

- el handshake SockJS puede enviar `?token=<JWT>` porque el browser no puede adjuntar headers custom en todas las requests de SockJS;
- el frame STOMP `CONNECT` envía `Authorization: Bearer <JWT>`;
- el backend valida `CONNECT` y valida cada `SUBSCRIBE` a topics de partida contra los participantes del `gameId`.

La lectura de estado privado completo se realiza vía REST con JWT en `GET /api/games/{gameId}/state`.

### Topics — servidor → cliente

| Topic | DTO | Descripción |
|-------|-----|-------------|
| `/topic/games/{gameId}/events` | `GameEventMessage` | Evento de juego individual (ver abajo). Se publica un mensaje por evento. |
| `/topic/games/{gameId}/state-changed` | `GameStateChangedMessage` | Notificación de que el estado cambió. Cliente debe llamar `GET /api/games/{gameId}/state` con su JWT. |

> No usar `/topic/game/{gameId}` como contrato principal. Es un path legacy y queda fuera del flujo realtime oficial.

### GameEventMessage

```json
{
  "gameId": "uuid",
  "eventType": "TURN_START",
  "payload": { "playerId": 1, "turnNumber": 3 },
  "timestamp": "2025-05-30T12:00:00"
}
```

| `eventType` | `payload` | Descripción |
|-------------|-----------|-------------|
| `KO` | `{pokemonCardId, prizeCardsGranted, attackerPlayerId}` | Pokémon noqueado |
| `PRIZE_TAKEN` | `{playerId, cardId, prizeSlot}` | Carta Premio tomada y revelada |
| `CONDITION_APPLIED` | `{pokemonCardId, condition}` | Condición especial aplicada |
| `TURN_START` | `{playerId, turnNumber}` | Inicio de turno |
| `GAME_OVER` | `{winnerId, reason}` | Partida terminada |
| `CHOOSE_ACTIVE_REQUIRED` | `{playerId}` | Jugador debe elegir nuevo Pokémon Activo |

### GameStateChangedMessage

```json
{
  "gameId": "uuid",
  "actionType": "PLAY_BASIC",
  "status": "ACTIVE",
  "timestamp": "2025-05-30T12:00:00"
}
```

### Ping / Pong (Health Check)

```javascript
client.publish({ destination: '/app/game/ping', body: '{"ping":true}' });
// respuesta en /topic/game/pong
```

| Destino (envío) | Topic (suscripción) | Descripción |
|-----------------|---------------------|-------------|
| `/app/game/ping` | `/topic/game/pong` | Verifica conectividad WebSocket. |

### Reconexión

1. Cliente se reconecta a STOMP endpoint.
2. Se re-subscribe a los topics del gameId correspondiente.
3. Llama `GET /api/games/{id}/state` para reconstruir el tablero completo.
4. El servidor no reenvía eventos perdidos — el snapshot de BD es suficiente.

---

## Flujo práctico para probar la API (end-to-end)

### A. Auth — Registrar y loguear dos jugadores

```bash
# Registrar player 1
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"player1","email":"p1@test.com","password":"pass123"}'
# → 201 {id, username, token}
# Guardar TOKEN1

# Registrar player 2
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"player2","email":"p2@test.com","password":"pass123"}'
# → 201 {id, username, token}
# Guardar TOKEN2

# Login player 1 (si ya existe)
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"player1","password":"pass123"}'
# → 200 {id, username, token}

# Login player 2
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"player2","password":"pass123"}'
# → 200 {id, username, token}
```

### B. Cards — Ver cartas disponibles

```bash
# Ver cartas disponibles (público, sin token)
curl http://localhost:8080/api/cards?page=0&size=5

# Buscar por nombre
curl "http://localhost:8080/api/cards?name=Pikachu&page=0&size=10"

# Obtener carta por ID
curl http://localhost:8080/api/cards/xy1-1
```

### C. Decks — Crear mazos válidos

Cada jugador necesita un mazo con al menos 1 Pokémon Basic para jugar.
El mazo debe tener exactamente 60 cartas.

```bash
# Player 1 crea un mazo (usando cartas reales de la BD)
curl -s -X POST http://localhost:8080/api/decks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN1" \
  -d '{"name":"Mi Mazo","cards":[{"cardId":"xy1-1","quantity":4},{"cardId":"base1-1","quantity":56}]}'
# → 201 {id, name, isValid: true, cardCount: 60, validationErrors: []}
# Guardar DECK1_ID

# Player 2 crea un mazo
curl -s -X POST http://localhost:8080/api/decks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN2" \
  -d '{"name":"Otro Mazo","cards":[{"cardId":"xy1-1","quantity":4},{"cardId":"base1-1","quantity":56}]}'
# Guardar DECK2_ID
```

### D. Game — Flujo de partida

```bash
# 1. Player 1 crea partida
curl -s -X POST http://localhost:8080/api/games \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN1" \
  -d "{\"deckId\":$DECK1_ID}"
# → 201 {gameId: "uuid", status: "WAITING"}
# Guardar GAME_ID

# 2. Player 2 se une
curl -s -X POST http://localhost:8080/api/games/$GAME_ID/join \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN2" \
  -d "{\"deckId\":$DECK2_ID}"
# → 200 {gameId, status: "ACTIVE"}

# 3. Player 1 consulta estado
curl -s http://localhost:8080/api/games/$GAME_ID/state \
  -H "Authorization: Bearer $TOKEN1"
# → 200 BoardStateDTO (con myField.hand visible, opponentField.handSize)

# 4. Player 2 consulta estado (ve su propia mano)
curl -s http://localhost:8080/api/games/$GAME_ID/state \
  -H "Authorization: Bearer $TOKEN2"
# → 200 BoardStateDTO (mano de player2 visible, mano de player1 oculta)

# 5. Player 2 intenta actuar fuera de turno → error
curl -s -X POST http://localhost:8080/api/games/$GAME_ID/actions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN2" \
  -d '{"type":"DRAW_CARD","payload":{}}'
# → 200 {success: false, error: {code: "NOT_YOUR_TURN", message: "..."}}
```

### E. Acciones disponibles (MVP)

Todas las acciones usan body plano (sin `payload` anidado).

| Acción | Fase | Campos del body | Descripción |
|--------|------|-----------------|-------------|
| `DRAW_CARD` | `DRAW` | — | Robar una carta del mazo. Avanza a MAIN. |
| `PLAY_BASIC_POKEMON` | `MAIN` | `cardInstanceId` (o `cardId`), `targetPosition` | Juega Pokémon Basic. Posición: `ACTIVE` o `BENCH_0`–`BENCH_4`. |
| `ATTACH_ENERGY` | `MAIN` | `cardInstanceId` (o `cardId`), `targetPosition` | Adjunta energía a un Pokémon. Una vez por turno. |
| `USE_ATTACK` | `MAIN` | `attackIndex` | Ataca con el Pokémon Activo. `attackIndex` según lista de ataques. |
| `END_TURN` | `MAIN` | — | Termina el turno. Cambia `currentPlayerId`. Vuelve a DRAW. |
| `CONCEDE` | cualquiera | — | Rendirse. Termina la partida. |

**Ejemplo completo:**
```json
{
  "type": "PLAY_BASIC_POKEMON",
  "cardInstanceId": "p1-xy1-1-a1b2c3",
  "targetPosition": "ACTIVE"
}
```

### F. WebSocket — Suscripción a eventos

```javascript
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
});

client.onConnect = () => {
  // Escuchar eventos de juego
  client.subscribe('/topic/games/{gameId}/events', msg => {
    const event = JSON.parse(msg.body);
    console.log(event.eventType, event.payload);
  });

  // Escuchar notificación de cambio de estado
  client.subscribe('/topic/games/{gameId}/state-changed', msg => {
    const notif = JSON.parse(msg.body);
    // → llamar GET /api/games/{gameId}/state con JWT
    fetch(`/api/games/${notif.gameId}/state`, {
      headers: { Authorization: `Bearer ${token}` }
    }).then(r => r.json()).then(state => {
      // actualizar UI con el estado filtrado
    });
  });
};

client.activate();
```

> ⚠️ Las acciones de juego se envían por REST (`POST /api/games/{id}/actions`),
> NO por WebSocket. El WebSocket solo recibe notificaciones.

---
## Limitaciones actuales del MVP

Este proyecto es una implementación mínima viable del TCG de Pokémon.
Las siguientes reglas NO están implementadas y quedan como trabajo futuro:

| Funcionalidad | Estado |
|---------------|--------|
| **Evolución** (`EVOLVE_POKEMON`) | No implementada |
| **Retirada** (`RETREAT`) | No implementada |
| **Cartas Trainer** (Item, Supporter, Stadium, Tool) | No implementadas |
| **Selección manual de Pokémon Activo** (`CHOOSE_ACTIVE_REQUIRED`) | No implementada. Promoción automática del primer Pokémon de la banca. |
| **Costos de energía complejos** | El engine acepta cualquier energía como válida |
| **Estados especiales** (Dormido, Confuso, Paralizado, Envenenado, Quemado) | Parcial: `CONDITION_APPLIED` existe como evento, sin lógica de juego |
| **WebSocket con datos privados** | No se envía información privada por WebSocket. Todo estado privado se obtiene por REST con JWT. |
| **Reconexión con replay de eventos** | No se reenvían eventos perdidos. El snapshot de BD es suficiente. |
| **Cola de acciones / sincronización** | Sin cola. El turno se pasa explícitamente con `END_TURN`. |
| **Mulligan / mano inicial** | Sin lógica de mulligan. La mano inicial se asigna en `joinGame`. |

---

## Errores HTTP estándar

Todos los errores del sistema (no de reglas de juego) tienen este formato:

```json
{
  "timestamp": "2026-05-29T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Deck not found: 99",
  "path": "/api/decks/99"
}
```

| Excepción Java | HTTP Status | Cuándo ocurre |
|----------------|-------------|---------------|
| `InvalidActionException` | `400 Bad Request` | Acción inválida, payload incorrecto |
| `IllegalArgumentException` | `400 Bad Request` | Player not found, parámetros inválidos |
| `GameNotFoundException` | `404 Not Found` | Partida o estado no encontrado |
| `ResourceNotFoundException` | `404 Not Found` | Recurso no encontrado |
| `NoSuchElementException` | `404 Not Found` | Deck/carta no encontrado |
| `DeckValidationException` | `422 Unprocessable Entity` | Mazo inválido al crear/unirse a partida |
| `ForbiddenException` | `403 Forbidden` | Jugador no pertenece a la partida, deck ajeno, unirse a propia partida |
| `SecurityException` | `403 Forbidden` | Credenciales inválidas / acceso denegado |
| `DuplicateResourceException` | `409 Conflict` | Username/email duplicado |
| `IllegalStateException` | `409 Conflict` | Partida no está WAITING al hacer join |
| Token ausente o inválido | `401 Unauthorized` | Sin autenticación o token expirado |

> Los errores de **reglas de juego** (fase incorrecta, no es tu turno, etc.) devuelven `200 OK` con `success: false` y un `error.code` descriptivo.
