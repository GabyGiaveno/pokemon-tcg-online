# DATA_SPEC — Especificación de datos

Fuente de verdad para ENUMs, entidades JPA, modelos en memoria y DTOs.
Toda discrepancia entre este documento y el código es un bug en el código.

Fuente de verdad para el schema SQL: `BE/src/main/resources/db/migration/V1__initial_schema.sql`.

---

## ENUMs de dominio

Existen en Java (`models/cards/` o `models/game/`) y en PostgreSQL (`V1__initial_schema.sql`).

### GameStatus

| Valor | Significado |
|-------|------------|
| `WAITING` | Creada por player1, esperando oponente |
| `SETUP` | Player2 se unió; ambos colocan Pokémon Activo y Prize Cards |
| `ACTIVE` | Partida en curso, turnos alternados |
| `FINISHED` | Partida terminada, hay un ganador |

### TurnPhase

| Valor | Quién actúa | Qué puede pasar |
|-------|-------------|-----------------|
| `DRAW` | Jugador activo | Roba 1 carta obligatoriamente |
| `MAIN` | Jugador activo | Juega cartas, adjunta energía, se retira |
| `ATTACK` | Jugador activo | Ejecuta exactamente 1 ataque (opcional — puede saltearse con END_TURN) |
| `BETWEEN_TURNS` | Sistema | Resuelve condiciones especiales, cambia turno |

### ActionType

| Valor | Fase válida | Descripción |
|-------|-------------|-------------|
| `DRAW_CARD` | DRAW | Robar carta del mazo |
| `PLAY_BASIC_POKEMON` | MAIN, SETUP | Colocar Pokémon Básico en la Banca |
| `EVOLVE_POKEMON` | MAIN | Evolucionar un Pokémon de Banca o Activo |
| `ATTACH_ENERGY` | MAIN | Adjuntar 1 carta de energía (límite: 1 por turno) |
| `USE_ATTACK` | ATTACK | Ejecutar un ataque del Pokémon Activo |
| `RETREAT` | MAIN | Retirar el Pokémon Activo a la Banca (límite: 1 por turno) |
| `PLAY_ITEM` | MAIN | Jugar carta tipo ITEM (sin límite por turno) |
| `PLAY_SUPPORTER` | MAIN | Jugar carta SUPPORTER o AS TÁCTICO (límite: 1 por turno compartido) |
| `PLAY_STADIUM` | MAIN | Jugar carta STADIUM (reemplaza el anterior) |
| `ATTACH_TOOL` | MAIN | Adjuntar POKEMON_TOOL a un Pokémon (1 por Pokémon) |
| `TAKE_PRIZE_CARD` | POST-KO | Tomar una Prize Card tras noquear al oponente |
| `SETUP_PLACE_POKEMON` | SETUP | Colocar Pokémon inicial durante el setup |
| `SETUP_SET_PRIZES` | SETUP | Colocar las 6 Prize Cards boca abajo |
| `END_TURN` | MAIN, ATTACK | Terminar el turno sin atacar |
| `CONCEDE` | Cualquiera | Rendirse |

### SpecialCondition

| Valor | Cuándo se resuelve | Efecto | Notas |
|-------|-------------------|--------|-------|
| `NONE` | — | — | Estado por defecto |
| `ASLEEP` | BETWEEN_TURNS | Flip de moneda: cara = despierta | Excluyente con CONFUSED, PARALYZED |
| `BURNED` | BETWEEN_TURNS | 20 de daño + flip: cara = se cura | Independiente, coexiste con todo |
| `CONFUSED` | Al declarar ataque (antes del pipeline) | Flip: cara = ataca normal, cruz = 30 de daño a sí mismo, ataque no se ejecuta | Excluyente con ASLEEP, PARALYZED |
| `PARALYZED` | BETWEEN_TURNS | No puede atacar ni retirarse ese turno; se cura automáticamente | Excluyente con ASLEEP, CONFUSED |
| `POISONED` | BETWEEN_TURNS | 10 de daño | Independiente, coexiste con todo |

### EnergyType

`GRASS · FIRE · WATER · LIGHTNING · PSYCHIC · FIGHTING · DARKNESS · METAL · FAIRY · DRAGON · COLORLESS`

`COLORLESS` es comodín: se satisface con cualquier tipo de energía.

### CardType

Clasificación en memoria (no en BD). Determina reglas de juego y Prize Cards al KO.

| Categoría | Tipos |
|-----------|-------|
| Pokémon | `BASIC_POKEMON`, `STAGE1`, `STAGE2`, `POKEMON_EX`, `MEGA_POKEMON` |
| Energía | `BASIC_ENERGY`, `SPECIAL_ENERGY` |
| Entrenador | `ITEM`, `ACE_TACTICIAN`, `SUPPORTER`, `STADIUM`, `POKEMON_TOOL` |

Detección desde datos de la API:
- `POKEMON_EX`: nombre contiene sufijo `-EX` (e.g. `Venusaur-EX`)
- `MEGA_POKEMON`: nombre contiene prefijo `M ` (e.g. `M Venusaur-EX`)
- `ACE_TACTICIAN`: campo `isAceTactician = true` en la entidad `Card`
- Los demás tipos se derivan de `supertype` + `subtypes` de la API

---

## Entidades JPA

Package: `ar.edu.utn.frc.tup.piii.entities`
Anotaciones Lombok en todas: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`

### Player

```
Tabla: player
```

| Campo Java | Tipo Java | Columna SQL | Tipo SQL | Restricciones | JPA |
|-----------|-----------|-------------|----------|---------------|-----|
| `id` | `Long` | `id` | `BIGSERIAL` | PK | `@Id @GeneratedValue(IDENTITY)` |
| `username` | `String` | `username` | `VARCHAR(50)` | NOT NULL, UNIQUE | `@Column(unique=true, nullable=false)` |
| `email` | `String` | `email` | `VARCHAR(255)` | NOT NULL, UNIQUE | `@Column(unique=true, nullable=false)` |
| `passwordHash` | `String` | `password_hash` | `VARCHAR(255)` | NOT NULL | `@Column(nullable=false)` |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | `@Column(nullable=false)` |

> **BUG CONOCIDO**: la columna `password_hash` está ausente en `V1__initial_schema.sql`. Debe agregarse.

Relaciones:
- `@OneToMany(mappedBy="player") List<Deck> decks`
- `@OneToMany(mappedBy="player1") List<GameSession> sessionsAsPlayer1`

---

### CardSet

```
Tabla: card_set
```

| Campo Java | Tipo Java | Columna SQL | Tipo SQL | Restricciones | JPA |
|-----------|-----------|-------------|----------|---------------|-----|
| `id` | `String` | `id` | `VARCHAR(20)` | PK — asignado desde API | `@Id` (sin generación) |
| `name` | `String` | `name` | `VARCHAR(200)` | NOT NULL | `@Column(nullable=false)` |
| `series` | `String` | `series` | `VARCHAR(100)` | nullable | |
| `printedTotal` | `int` | `printed_total` | `SMALLINT` | nullable | |
| `releaseDate` | `LocalDate` | `release_date` | `DATE` | nullable | |

---

### Card

```
Tabla: card
```

| Campo Java | Tipo Java | Columna SQL | Tipo SQL | Restricciones | JPA |
|-----------|-----------|-------------|----------|---------------|-----|
| `id` | `String` | `id` | `VARCHAR(20)` | PK — asignado desde API | `@Id` (sin generación) |
| `name` | `String` | `name` | `VARCHAR(200)` | NOT NULL, INDEX | `@Column(nullable=false)` |
| `supertype` | `String` | `supertype` | `VARCHAR(20)` | NOT NULL | |
| `subtypes` | `List<String>` | `subtypes` | `TEXT[]` | NOT NULL DEFAULT '{}' | `@Convert(StringListConverter)` |
| `hp` | `Integer` | `hp` | `SMALLINT` | nullable | |
| `types` | `List<String>` | `types` | `TEXT[]` | NOT NULL DEFAULT '{}' | `@Convert(StringListConverter)` |
| `attacks` | `String` | `attacks` | `JSONB` | NOT NULL DEFAULT '[]' | `@Column(columnDefinition="jsonb")` |
| `weaknesses` | `String` | `weaknesses` | `JSONB` | NOT NULL DEFAULT '[]' | `@Column(columnDefinition="jsonb")` |
| `resistances` | `String` | `resistances` | `JSONB` | NOT NULL DEFAULT '[]' | `@Column(columnDefinition="jsonb")` |
| `retreatCost` | `List<String>` | `retreat_cost` | `TEXT[]` | NOT NULL DEFAULT '{}' | `@Convert(StringListConverter)` |
| `evolvesFrom` | `String` | `evolves_from` | `VARCHAR(200)` | nullable | |
| `cardSet` | `CardSet` | `set_id` | `VARCHAR(20) FK` | NOT NULL, INDEX | `@ManyToOne @JoinColumn(name="set_id")` |
| `imageUrlLarge` | `String` | `image_url_large` | `TEXT` | nullable | |
| `imageUrlSmall` | `String` | `image_url_small` | `TEXT` | nullable | |
| `isAceTactician` | `boolean` | `is_ace_tactician` | `BOOLEAN` | NOT NULL DEFAULT false | |

`StringListConverter`: `@Converter` que convierte `List<String>` ↔ `String` separado por comas.
Los campos JSONB se guardan como `String` de JSON y se deserializan con Jackson cuando el engine los usa.

---

### Deck

```
Tabla: deck
```

| Campo Java | Tipo Java | Columna SQL | Tipo SQL | Restricciones | JPA |
|-----------|-----------|-------------|----------|---------------|-----|
| `id` | `Long` | `id` | `BIGSERIAL` | PK | `@Id @GeneratedValue(IDENTITY)` |
| `name` | `String` | `name` | `VARCHAR(100)` | NOT NULL | |
| `player` | `Player` | `player_id` | `BIGINT FK` | NOT NULL | `@ManyToOne @JoinColumn(name="player_id")` |
| `isValid` | `boolean` | `is_valid` | `BOOLEAN` | NOT NULL DEFAULT false | |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL DEFAULT NOW() | |

Relaciones:
- `@OneToMany(mappedBy="deck", cascade=ALL, orphanRemoval=true) List<DeckCard> cards`

Reglas de validación (enforced en `DeckService`, no en DB):

| Código de error | Regla |
|----------------|-------|
| `WRONG_TOTAL` | Exactamente 60 cartas en total |
| `TOO_MANY_COPIES` | Máximo 4 copias de una carta con el mismo nombre (excepto BASIC_ENERGY: sin límite) |
| `TOO_MANY_ACE_TACTICIAN` | Máximo 1 carta con `isAceTactician = true` |
| `NO_BASIC_POKEMON` | Mínimo 1 Pokémon con subtipo `Basic` |

---

### DeckCard

```
Tabla: deck_card
```

| Campo Java | Tipo Java | Columna SQL | Tipo SQL | Restricciones | JPA |
|-----------|-----------|-------------|----------|---------------|-----|
| `id` | `Long` | `id` | `BIGSERIAL` | PK | `@Id @GeneratedValue(IDENTITY)` |
| `deck` | `Deck` | `deck_id` | `BIGINT FK` | NOT NULL | `@ManyToOne @JoinColumn(name="deck_id")` |
| `card` | `Card` | `card_id` | `VARCHAR(20) FK` | NOT NULL | `@ManyToOne @JoinColumn(name="card_id")` |
| `quantity` | `int` | `quantity` | `SMALLINT` | NOT NULL, CHECK >= 1 | `@Column(nullable=false)` |

UNIQUE constraint en `(deck_id, card_id)`: una carta solo aparece una vez por mazo con su cantidad.

---

### GameSession

```
Tabla: game_session
```

| Campo Java | Tipo Java | Columna SQL | Tipo SQL | Restricciones | JPA |
|-----------|-----------|-------------|----------|---------------|-----|
| `id` | `UUID` | `id` | `UUID` | PK, DEFAULT gen_random_uuid() | `@Id @GeneratedValue(strategy=UUID)` |
| `player1` | `Player` | `player1_id` | `BIGINT FK` | NOT NULL | `@ManyToOne @JoinColumn(name="player1_id")` |
| `player2` | `Player` | `player2_id` | `BIGINT FK` | nullable | `@ManyToOne @JoinColumn(name="player2_id")` |
| `status` | `GameStatus` | `status` | `game_status` | NOT NULL DEFAULT WAITING, INDEX | `@Enumerated(STRING)` |
| `currentPlayerId` | `Long` | `current_player_id` | `BIGINT` | nullable | |
| `prizeCardsCount` | `int` | `prize_cards_count` | `SMALLINT` | NOT NULL DEFAULT 6, CHECK IN (1,6) | |
| `winner` | `Player` | `winner_id` | `BIGINT FK` | nullable | `@ManyToOne @JoinColumn(name="winner_id")` |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL DEFAULT NOW() | |
| `finishedAt` | `LocalDateTime` | `finished_at` | `TIMESTAMP` | nullable | |

Relaciones:
- `@OneToOne(mappedBy="gameSession") GameState gameState`
- `@OneToMany(mappedBy="gameSession") List<GameAction> actions`

---

### GameState

```
Tabla: game_state
```

| Campo Java | Tipo Java | Columna SQL | Tipo SQL | Restricciones | JPA |
|-----------|-----------|-------------|----------|---------------|-----|
| `id` | `Long` | `id` | `BIGSERIAL` | PK | `@Id @GeneratedValue(IDENTITY)` |
| `gameSession` | `GameSession` | `game_session_id` | `UUID FK` | NOT NULL, UNIQUE | `@OneToOne @JoinColumn(name="game_session_id")` |
| `stateJson` | `String` | `state_json` | `JSONB` | NOT NULL | `@Column(columnDefinition="jsonb", nullable=false)` |
| `turnNumber` | `int` | `turn_number` | `INT` | NOT NULL DEFAULT 0 | |
| `phase` | `TurnPhase` | `phase` | `turn_phase` | NOT NULL DEFAULT DRAW | `@Enumerated(STRING)` |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL DEFAULT NOW() | |

`stateJson` contiene un `BoardState` serializado con Jackson. Es el snapshot completo del tablero.
Se sobreescribe tras cada acción válida.

---

### GameAction

```
Tabla: game_action
```

| Campo Java | Tipo Java | Columna SQL | Tipo SQL | Restricciones | JPA |
|-----------|-----------|-------------|----------|---------------|-----|
| `id` | `Long` | `id` | `BIGSERIAL` | PK | `@Id @GeneratedValue(IDENTITY)` |
| `gameSession` | `GameSession` | `game_session_id` | `UUID FK` | NOT NULL, INDEX | `@ManyToOne @JoinColumn` `@Column(updatable=false)` |
| `turnNumber` | `int` | `turn_number` | `INT` | NOT NULL | `@Column(updatable=false)` |
| `playerId` | `Long` | `player_id` | `BIGINT FK` | NOT NULL | `@Column(updatable=false)` |
| `actionType` | `ActionType` | `action_type` | `action_type` | NOT NULL | `@Enumerated(STRING)` `@Column(updatable=false)` |
| `payload` | `String` | `payload` | `JSONB` | NOT NULL DEFAULT '{}' | `@Column(columnDefinition="jsonb", updatable=false)` |
| `result` | `String` | `result` | `JSONB` | NOT NULL DEFAULT '{}' | `@Column(columnDefinition="jsonb", updatable=false)` |
| `timestamp` | `LocalDateTime` | `timestamp` | `TIMESTAMP` | NOT NULL DEFAULT NOW() | `@Column(updatable=false)` |

**Todos los campos tienen `@Column(updatable=false)`**. Solo INSERT — nunca UPDATE. Si alguien llama `save()` sobre una instancia existente, JPA no puede modificar nada.

---

## Modelos en memoria (no JPA)

Java records o clases inmutables. Package: `models/game/`. Sin `@Entity`, sin acceso a BD.

### BoardState

Estado completo del tablero en un momento dado.

```
BoardState {
  TurnPhase         currentPhase
  Long              currentPlayerId        // ID del jugador activo (Long, igual que Player.id)
  int               turnNumber
  boolean           firstPlayerHasActed   // false hasta que el primer jugador termina su primer turno
  PlayerField       player1Field
  PlayerField       player2Field
}
```

### PlayerField

El lado del tablero de un jugador.

```
PlayerField {
  Long              playerId               // Long, igual que Player.id
  ActivePokemon     activePokemon          // null durante el setup
  List<BenchPokemon> bench                 // máx. 5
  List<String>      hand                   // IDs de cartas en mano
  List<String>      deck                   // IDs de cartas del mazo (orden interno — nunca se expone)
  List<String>      prizeCards             // IDs de cartas Prize (ocultas hasta tomarse)
  List<String>      discardPile            // IDs de cartas descartadas (visible para ambos)
  TurnFlags         turnFlags
}
```

### ActivePokemon

```
ActivePokemon {
  String              cardId
  int                 maxHp
  int                 currentHp
  List<AttachedCard>  attachedEnergies
  AttachedCard        tool                 // null si no tiene herramienta
  SpecialCondition    condition            // NONE por defecto (excluyente: ASLEEP/CONFUSED/PARALYZED)
  boolean             isBurned             // independiente de condition
  boolean             isPoisoned           // independiente de condition
}
```

### BenchPokemon

```
BenchPokemon {
  String              cardId
  int                 maxHp
  int                 currentHp
  List<AttachedCard>  attachedEnergies
  AttachedCard        tool
}
```

> Los Pokémon en Banca no tienen `SpecialCondition` — las condiciones solo aplican al Pokémon Activo.

### TurnFlags

Flags que se reinician al inicio de cada fase DRAW.

```
TurnFlags {
  boolean energyAttachedThisTurn    // máx. 1 energía por turno
  boolean retreatedThisTurn         // máx. 1 retiro por turno
  boolean supporterPlayedThisTurn   // máx. 1 Supporter/AS TÁCTICO por turno (límite compartido)
  boolean attackedThisTurn          // si es true, el turno debe terminar
}
```

> `firstPlayerHasActed` vive en `BoardState`, no en `TurnFlags`. Es una flag de partida, no de turno.

### AttachedCard

```
AttachedCard {
  String    cardId
  CardType  type      // BASIC_ENERGY, SPECIAL_ENERGY, o POKEMON_TOOL
}
```

---

## DTOs

### Request DTOs — `dtos/request/`

| Clase | Campos | Validaciones |
|-------|--------|-------------|
| `RegisterRequest` | `username`, `email`, `password` | `@NotBlank`, `@Email`, `@Size(min=8)` para password, `@Size(min=3, max=50)` para username |
| `LoginRequest` | `username`, `password` | `@NotBlank` |
| `CreateDeckRequest` | `name`, `List<DeckCardEntry(cardId, quantity)>` | `@NotBlank`, `@NotEmpty`, `@Min(1)` en quantity |
| `JoinGameRequest` | `deckId` | `@NotNull` |
| `ActionRequest` | `type` (ActionType), `payload` (Object) | `@NotNull` en type |

### Response DTOs — `dtos/response/`

| Clase | Campos | Notas |
|-------|--------|-------|
| `PlayerResponse` | `id`, `username`, `token` | |
| `DeckResponse` | `id`, `name`, `isValid`, `cardCount`, `validationErrors[]` | `validationErrors` vacío si es válido |
| `DeckValidationError` | `code`, `message`, `cardId` (nullable) | Códigos: `WRONG_TOTAL`, `TOO_MANY_COPIES`, `TOO_MANY_ACE_TACTICIAN`, `NO_BASIC_POKEMON` |
| `CardResponse` | `id`, `name`, `supertype`, `subtypes[]`, `hp`, `types[]`, `imageUrlSmall`, `imageUrlLarge`, `isAceTactician` | |
| `GameSessionResponse` | `gameId`, `status`, `player1Username`, `createdAt` | |
| `BoardStateDTO` | `gameId`, `myField`, `opponentField`, `currentPlayerId`, `phase`, `turnNumber` | Mano del oponente como `handSize: int`; mazo como `deckSize: int`; Prize Cards ocultas |
| `ActionResult` | `success`, `newState` (BoardStateDTO), `events[]`, `error` (nullable) | |
| `GameEventDTO` | `type`, `payload`, `timestamp` | |
| `PlayerPrivateDTO` | `hand[]` (completa), `prizeCards[]` (propias, visibles), `deckSize` | Legacy/no usado por el contrato realtime actual. El estado privado se obtiene por `GET /api/games/{gameId}/state` con JWT. |
