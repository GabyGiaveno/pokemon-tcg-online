ETAPA 0 - Alineación de contratos de API
Objetivo

Esta etapa tiene como objetivo dejar una base común y coherente para el desarrollo de la API del proyecto Pokémon TCG.

Antes de implementar lógica pesada de juego, autenticación completa o WebSocket real, necesitamos alinear los contratos de la API para que backend, frontend y documentación hablen el mismo idioma.

La Etapa 0 NO busca terminar la API completa.

La Etapa 0 busca:

Corregir contratos mal definidos.
Documentar endpoints oficiales.
Definir qué requests y responses se usan.
Evitar que cada integrante trabaje con estructuras distintas.
Preparar el código para que después se pueda implementar Auth/JWT, GameService real y WebSocket sin romper todo.
Alcance de esta etapa
Esta etapa SÍ incluye
Revisar y actualizar docs/API_SPEC.md.
Definir contratos oficiales para Auth, Cards, Decks, Games, Actions y WebSocket.
Crear DTOs faltantes para requests de partidas.
Corregir GameController para que POST /api/games y POST /api/games/{id}/join reciban objetos JSON y no valores sueltos.
Dejar TODOs claros donde todavía haya lógica hardcodeada.
Asegurar que el proyecto compile después de los cambios.
No romper el funcionamiento actual de Cards y Decks.
Esta etapa NO incluye
Implementar JWT completo.
Sacar completamente X-Player-Id del código si todavía no existe autenticación real.
Implementar GameService completo.
Implementar setup real de partida.
Implementar motor completo de acciones.
Implementar WebSocket real.
Cambiar reglas de juego profundas.
Refactorizar todo el engine.
Decisiones oficiales de contrato
1. Endpoints públicos

Los siguientes endpoints serán públicos:

POST /api/auth/register
POST /api/auth/login
GET /api/cards
GET /api/cards/{id}
2. Endpoints privados

Los siguientes endpoints requieren autenticación en la versión final:

POST /api/cards/sync
/api/decks/**
/api/games/**

En la versión final se deberá usar:

Authorization: Bearer <token>

Actualmente puede existir uso temporal de:

X-Player-Id

pero debe quedar documentado como implementación provisoria.

3. No usar X-Player-Id como contrato final

X-Player-Id puede quedar temporalmente en algunos controllers hasta que se implemente JWT, pero no debe figurar como contrato final.

Contrato final esperado:

Authorization: Bearer <jwt>
4. Usar valid, no isValid

Para respuestas de validación de mazos se usará:

{
  "valid": true
}

No usar:

{
  "isValid": true
}

Esto evita inconsistencias entre Java, Jackson y frontend.

5. POST /api/games debe recibir un objeto JSON

No se debe recibir un número suelto como body.

Incorrecto:

10

Correcto:

{
  "deckId": 10
}

Por lo tanto, debe existir un DTO:

CreateGameRequest
6. POST /api/games/{id}/join también debe recibir un objeto JSON

Debe recibir:

{
  "deckId": 15
}

Por lo tanto, debe existir un DTO:

JoinGameRequest
7. Game Actions usará DTO plano

Por ahora, para simplificar el desarrollo, las acciones de juego usarán un DTO plano.

Ejemplo:

{
  "type": "ATTACH_ENERGY",
  "cardId": "xy1-90",
  "targetPosition": "ACTIVE"
}

No se usará por ahora un payload genérico.

Contrato oficial de Auth API
Register
POST /api/auth/register

Request:

{
  "username": "gabriel",
  "email": "gabriel@mail.com",
  "password": "123456"
}

Response esperada:

201 Created
{
  "id": 1,
  "username": "gabriel",
  "token": "jwt-real"
}

Errores posibles:

400 Bad Request
409 Conflict

Notas:

Actualmente puede existir token placeholder.
En una etapa posterior se debe implementar JWT real.
En una etapa posterior se debe hashear password con BCrypt.
Login
POST /api/auth/login

Request:

{
  "username": "gabriel",
  "password": "123456"
}

Response esperada:

200 OK
{
  "id": 1,
  "username": "gabriel",
  "token": "jwt-real"
}

Errores posibles:

401 Unauthorized
Contrato oficial de Cards API
Buscar cartas
GET /api/cards

Query params opcionales:

name
set
supertype
type
page
size
sort

Ejemplo:

GET /api/cards?name=pikachu&set=xy1&page=0&size=20

Response:

{
  "data": [
    {
      "id": "xy1-42",
      "name": "Pikachu",
      "supertype": "Pokémon",
      "subtypes": ["Basic"],
      "hp": 60,
      "types": ["Lightning"],
      "cardSetId": "xy1",
      "cardSetName": "XY",
      "imageUrlSmall": "https://...",
      "imageUrlLarge": "https://...",
      "aceTactician": false
    }
  ],
  "total": 1,
  "page": 0,
  "size": 20
}
Buscar carta por ID
GET /api/cards/{id}

Ejemplo:

GET /api/cards/xy1-42

Response:

{
  "id": "xy1-42",
  "name": "Pikachu",
  "supertype": "Pokémon",
  "subtypes": ["Basic"],
  "hp": 60,
  "types": ["Lightning"],
  "cardSetId": "xy1",
  "cardSetName": "XY",
  "imageUrlSmall": "https://...",
  "imageUrlLarge": "https://...",
  "aceTactician": false
}

Errores posibles:

404 Not Found
Sincronizar cartas
POST /api/cards/sync?set=xy1

Response recomendada para esta versión:

200 OK
{
  "message": "Sync completed",
  "set": "xy1",
  "cardsImported": 146
}

Notas:

En esta etapa no es obligatorio hacerlo asincrónico.
Si ya funciona sincrónico, puede quedar con 200 OK.
Debe quedar documentado que el juego usa cartas cacheadas en base local.
Contrato oficial de Decks API

Todos los endpoints de Decks deberán requerir autenticación en la versión final.

Actualmente puede existir X-Player-Id de forma temporal.

Listar mis mazos
GET /api/decks
Authorization: Bearer <token>

Response:

[
  {
    "id": 1,
    "name": "Mazo Eléctrico",
    "valid": true,
    "cardCount": 60,
    "cards": [],
    "validationErrors": [],
    "createdAt": "2026-05-28T18:30:00"
  }
]

Regla:

Debe devolver solamente los mazos del usuario autenticado.
Ver un mazo propio
GET /api/decks/{id}
Authorization: Bearer <token>

Response:

{
  "id": 1,
  "name": "Mazo Eléctrico",
  "valid": true,
  "cardCount": 60,
  "cards": [
    {
      "cardId": "xy1-42",
      "name": "Pikachu",
      "quantity": 4,
      "imageUrlSmall": "https://..."
    }
  ],
  "validationErrors": [],
  "createdAt": "2026-05-28T18:30:00"
}

Errores posibles:

403 Forbidden
404 Not Found
Crear mazo
POST /api/decks
Authorization: Bearer <token>

Request:

{
  "name": "Mazo Eléctrico",
  "cards": [
    {
      "cardId": "xy1-42",
      "quantity": 4
    },
    {
      "cardId": "xy1-50",
      "quantity": 4
    }
  ]
}

Response:

201 Created
{
  "id": 1,
  "name": "Mazo Eléctrico",
  "valid": false,
  "cardCount": 8,
  "cards": [
    {
      "cardId": "xy1-42",
      "name": "Pikachu",
      "quantity": 4,
      "imageUrlSmall": "https://..."
    }
  ],
  "validationErrors": [
    {
      "code": "WRONG_TOTAL",
      "message": "El mazo debe tener exactamente 60 cartas",
      "cardId": null
    }
  ],
  "createdAt": "2026-05-28T18:30:00"
}

Decisión:

Se permite guardar mazos inválidos.
No se permite usar un mazo inválido para crear o unirse a una partida.
Editar mazo
PUT /api/decks/{id}
Authorization: Bearer <token>

Request:

{
  "name": "Mazo Eléctrico Editado",
  "cards": [
    {
      "cardId": "xy1-42",
      "quantity": 4
    }
  ]
}

Response:

200 OK
{
  "id": 1,
  "name": "Mazo Eléctrico Editado",
  "valid": true,
  "cardCount": 60,
  "cards": [],
  "validationErrors": [],
  "createdAt": "2026-05-28T18:30:00"
}
Eliminar mazo
DELETE /api/decks/{id}
Authorization: Bearer <token>

Response:

204 No Content

Errores posibles:

403 Forbidden
404 Not Found
Validar mazo
POST /api/decks/{id}/validate
Authorization: Bearer <token>

Response:

{
  "valid": false,
  "cardCount": 58,
  "errors": [
    {
      "code": "WRONG_TOTAL",
      "message": "El mazo debe tener exactamente 60 cartas",
      "cardId": null
    }
  ]
}

Códigos posibles:

WRONG_TOTAL
TOO_MANY_COPIES
TOO_MANY_ACE_TACTICIAN
NO_BASIC_POKEMON
UNKNOWN_CARD
Contrato oficial de Game Sessions API

Todos los endpoints de Games deberán requerir autenticación en la versión final.

Actualmente puede haber lógica provisoria, pero no debe quedar como contrato final.

Crear partida
POST /api/games
Authorization: Bearer <token>

Request:

{
  "deckId": 10
}

Response:

201 Created
{
  "gameId": "2e73a6df-6ff7-4cf8-84cf-d71bb2e3c52f",
  "status": "WAITING",
  "player1Username": "gabriel",
  "player2Username": null,
  "createdAt": "2026-05-28T18:30:00",
  "prizeCardsCount": 6
}

Validaciones esperadas:

El deck existe.
El deck pertenece al jugador autenticado.
El deck es válido.
Si el deck es inválido, no se crea partida.

Errores posibles:

404 Not Found
403 Forbidden
422 Unprocessable Entity
Listar partidas
GET /api/games
Authorization: Bearer <token>

Query params opcionales:

status

Ejemplo:

GET /api/games?status=WAITING

Response:

[
  {
    "gameId": "2e73a6df-6ff7-4cf8-84cf-d71bb2e3c52f",
    "status": "WAITING",
    "player1Username": "gabriel",
    "player2Username": null,
    "createdAt": "2026-05-28T18:30:00",
    "prizeCardsCount": 6
  }
]

Para MVP puede devolver solo partidas en estado WAITING.

Unirse a partida
POST /api/games/{id}/join
Authorization: Bearer <token>

Request:

{
  "deckId": 15
}

Response:

200 OK
{
  "gameId": "2e73a6df-6ff7-4cf8-84cf-d71bb2e3c52f",
  "status": "SETUP",
  "player1Username": "gabriel",
  "player2Username": "mateo",
  "createdAt": "2026-05-28T18:30:00",
  "prizeCardsCount": 6
}

Validaciones esperadas:

La partida existe.
La partida está en WAITING.
El jugador no es el creador de la partida.
El deck existe.
El deck pertenece al jugador.
El deck es válido.
Después del join se inicializa el estado de partida.

Errores posibles:

404 Not Found
409 Conflict
403 Forbidden
422 Unprocessable Entity
Obtener estado de partida
GET /api/games/{id}/state
Authorization: Bearer <token>

Response esperada:

{
  "gameId": "2e73a6df-6ff7-4cf8-84cf-d71bb2e3c52f",
  "currentPlayerId": 1,
  "phase": "MAIN",
  "turnNumber": 1,
  "myField": {
    "activePokemon": null,
    "bench": [],
    "hand": ["xy1-42", "xy1-50"],
    "deckSize": 47,
    "prizeCards": ["xy1-10", "xy1-11", "xy1-12", "xy1-13", "xy1-14", "xy1-15"],
    "discardPile": []
  },
  "opponentField": {
    "activePokemon": null,
    "bench": [],
    "handSize": 7,
    "deckSize": 47,
    "prizeCards": [null, null, null, null, null, null],
    "discardPile": []
  }
}

Regla:

El jugador autenticado ve su propia mano.
El jugador autenticado no ve la mano del rival.
El estado debe salir del GameState.stateJson, no de valores hardcodeados.
Contrato oficial de Game Actions API
Ejecutar acción
POST /api/games/{id}/actions
Authorization: Bearer <token>

Request genérica:

{
  "type": "PLAY_BASIC_POKEMON",
  "cardId": "xy1-42",
  "targetPosition": "BENCH_0",
  "attackIndex": null,
  "benchIndex": null,
  "prizeIndex": null
}

Response exitosa:

{
  "success": true,
  "error": null,
  "events": [
    {
      "type": "POKEMON_PLAYED",
      "payload": {
        "playerId": 1,
        "cardId": "xy1-42",
        "targetPosition": "BENCH_0"
      }
    }
  ]
}

Response por acción inválida:

{
  "success": false,
  "error": "No podés adjuntar más de una energía por turno",
  "events": []
}

Decisión:

Para acciones inválidas por reglas de juego, se puede responder 200 OK con success: false.
Para errores reales se deben usar códigos HTTP.

Errores HTTP reales:

401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
500 Internal Server Error
Tipos de acciones oficiales
DRAW_CARD
PLAY_BASIC_POKEMON
EVOLVE_POKEMON
ATTACH_ENERGY
USE_ATTACK
RETREAT
PLAY_ITEM
PLAY_SUPPORTER
PLAY_STADIUM
ATTACH_TOOL
TAKE_PRIZE_CARD
SETUP_PLACE_POKEMON
SETUP_SET_PRIZES
END_TURN
CONCEDE
Ejemplos de acciones
Robar carta
{
  "type": "DRAW_CARD"
}
Jugar Pokémon básico
{
  "type": "PLAY_BASIC_POKEMON",
  "cardId": "xy1-42",
  "targetPosition": "BENCH_0"
}
Colocar Pokémon durante setup
{
  "type": "SETUP_PLACE_POKEMON",
  "cardId": "xy1-42",
  "targetPosition": "ACTIVE"
}
Evolucionar
{
  "type": "EVOLVE_POKEMON",
  "cardId": "xy1-60",
  "targetPosition": "ACTIVE"
}
Adjuntar energía
{
  "type": "ATTACH_ENERGY",
  "cardId": "xy1-90",
  "targetPosition": "BENCH_0"
}
Usar ataque
{
  "type": "USE_ATTACK",
  "attackIndex": 0
}
Retirarse
{
  "type": "RETREAT",
  "benchIndex": 0
}
Tomar premio
{
  "type": "TAKE_PRIZE_CARD",
  "prizeIndex": 2
}
Terminar turno
{
  "type": "END_TURN"
}
Rendirse
{
  "type": "CONCEDE"
}
Contrato oficial de WebSocket

> Actualizado: el contrato final realtime vive en `docs/api/REALTIME_WEBSOCKET.md`.

WebSocket no usa `ActionRequest` ni recibe acciones de juego. Las acciones siguen por REST:

POST /api/games/{gameId}/actions

Conexión WebSocket:

/ws

Servidor publica eventos discretos en:

/topic/games/{gameId}/events

Servidor publica invalidación de estado en:

/topic/games/{gameId}/state-changed

Al recibir `state-changed`, el frontend consulta el estado actualizado por REST con:

GET /api/games/{gameId}/state
Cambios de código concretos para esta etapa
1. Actualizar docs/API_SPEC.md

El archivo debe reflejar los contratos definidos en este documento.

Si el archivo ya existe, actualizarlo.

Si no existe, crearlo.

2. Crear CreateGameRequest

Crear un DTO para crear partidas.

Ubicación sugerida:

src/main/java/.../dtos/CreateGameRequest.java

Contenido esperado:

public class CreateGameRequest {
    private Long deckId;

    public Long getDeckId() {
        return deckId;
    }

    public void setDeckId(Long deckId) {
        this.deckId = deckId;
    }
}

Si el proyecto usa records, puede ser:

public record CreateGameRequest(Long deckId) {}

Usar el estilo que ya tenga el proyecto.

3. Crear JoinGameRequest

Crear un DTO para unirse a partidas.

Ubicación sugerida:

src/main/java/.../dtos/JoinGameRequest.java

Contenido esperado:

public class JoinGameRequest {
    private Long deckId;

    public Long getDeckId() {
        return deckId;
    }

    public void setDeckId(Long deckId) {
        this.deckId = deckId;
    }
}

Si el proyecto usa records, puede ser:

public record JoinGameRequest(Long deckId) {}

Usar el estilo que ya tenga el proyecto.

4. Modificar GameController

Cambiar POST /api/games.

Antes:

@PostMapping
public ResponseEntity<GameSessionResponse> createGame(@RequestBody Long deckId)

Después:

@PostMapping
public ResponseEntity<GameSessionResponse> createGame(@RequestBody CreateGameRequest request)

Y llamar al service usando:

request.getDeckId()

o:

request.deckId()

según si se usa clase o record.

5. Modificar joinGame

Cambiar POST /api/games/{id}/join para que reciba:

@RequestBody JoinGameRequest request

Y llamar al service usando:

request.getDeckId()

o:

request.deckId()
6. Dejar TODOs en GameService

Donde actualmente haya players hardcodeados:

playerRepository.findById(1L)
playerRepository.findById(2L)

Agregar TODOs claros.

Ejemplo:

// TODO Etapa 1: reemplazar player hardcodeado por player autenticado desde JWT.

No es obligatorio resolverlo en esta etapa si todavía no existe JWT.

7. No eliminar todavía X-Player-Id si no hay JWT

Si todavía no está implementado JWT real, no eliminar completamente X-Player-Id en esta etapa.

Solamente debe quedar documentado que:

Es temporal.
No es parte del contrato final.
Será reemplazado por Authorization: Bearer <token> en Etapa 1.
Criterios de aceptación

La Etapa 0 se considera terminada cuando:

docs/API_SPEC.md refleja los contratos oficiales.
Existe CreateGameRequest.
Existe JoinGameRequest.
GameController.createGame recibe { "deckId": 10 }.
GameController.joinGame recibe { "deckId": 15 }.
No se reciben Long sueltos como request body en endpoints de Games.
El proyecto compila.
No se rompieron endpoints existentes de Cards y Decks.
Quedan TODOs claros para JWT, players hardcodeados y X-Player-Id.
Los cambios están en una branch propia de Etapa 0.
La branch puede mergearse a produccion.
