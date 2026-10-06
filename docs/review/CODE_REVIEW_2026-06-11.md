# Code Review Integral — Pokémon TCG Digital (2026-06-11)

> **Audiencia:** desarrolladores del equipo.
> **Alcance:** TODO el repo (BE + FE + docs), rama `Produccion`, commit `3242b1e`.
> **⚠️ ACTUALIZADO el mismo día tras el merge del equipo (`785aba1`, +7.589 líneas de FE):
> ver §8 Addendum post-merge.** La sección de FE original (4.1) quedó reemplazada por §8.2.
> **Método:** lectura archivo por archivo del backend, frontend y documentación + ejecución
> de las suites de test (BE: 193 tests / FE: 1 spec). No se modificó código.
> **Documentos hermanos:** [`AGENTS.md`](../../AGENTS.md) (contexto para agentes IA) y
> [`docs/TODO/PROJECT_TODO.md`](../TODO/PROJECT_TODO.md) (roadmap completo de lo que falta).

---

## 1. Resumen ejecutivo (en criollo)

El proyecto tiene **dos motores en muy buen estado y un cardán roto entre ellos**:

- El **engine** (motor de reglas XY1 en Java puro) está bien diseñado, bien testeado
  (State + Chain of Responsibility + Flyweight + selección de jugador) y verde en sus tests.
- La **capa web** (auth JWT, decks, cartas, recovery por email) está completa y sólida.
- **PERO** la pieza que conecta los dos mundos — `EngineStateMapper` + `GameService.initializeGame` —
  tiene bugs que hacen que **una partida real por HTTP sea injugable hoy**: las cartas que se mueven
  "hacia" una zona desaparecen al persistir, no existe fase de SETUP (la partida termina en el
  turno 1 por victoria automática), y evolucionar es imposible.
- El **frontend de juego es 0%**: tablero, lobby, websocket y pokédex son stubs de 5-9 líneas.
  Auth y Deck Builder sí están completos y conectados al backend real.

**La decisión más importante de la semana** no es escribir más features: es **arreglar la
serialización del estado** (sección 3.1). Todo lo demás depende de eso.

**Suite de tests:** BE `./mvnw test` → **193 tests, 1 rojo** (`WebSocketHandshakeAuthIntegrationTest`,
regresión real, ver 3.6). FE `ng test` → 1 spec placeholder (sin cobertura real).

---

## 2. Mapa del sistema (cómo funciona HOY)

### 2.1 Flujo de una acción de juego

```
FE (stub hoy)
  → POST /api/games/{id}/actions  (GameActionApiRequest, JWT → CurrentPlayerService)
    → GameService.performGameAction
        1. carga GameSession + GameState (stateJson) de la DB
        2. deserializa stateJson → GameBoardState (modelo "DB", instanceIds)
        3. traduce GameActionApiRequest → ActionRequest (cardInstanceId pasa como cardId)
        4. arma CardLookup "inteligente" (cardId directo, o instanceId→cardId vía board)
        5. GameEngineFacade.applyAction
             a. EngineStateMapper.toDomainState  (GameBoardState → BoardState dominio)
             b. TurnManager.processAction        (TODO el juego pasa por acá)
             c. EngineStateMapper.updateDbState  (dominio → GameBoardState)  ← ★ ROTO
        6. persiste stateJson, loguea GameAction, publica eventos por STOMP
  ← GameActionApiResponse { success, events[] }
FE escucha /topic/games/{id}/state-changed → GET /api/games/{id}/state
```

### 2.2 El engine (lo que está BIEN — no romper)

- `TurnManager` orquesta: guarda de `pendingSelection` → ownership de turno → fase MAIN →
  rutea CONCEDE / END_TURN / USE_ATTACK → delega el resto a `MainPhaseState`.
- Fases (State): `DrawPhaseState` (robo automático + deck-out), `MainPhaseState` (8 acciones),
  `AttackPhaseState` (delega en la chain), `BetweenTurnsState` (ticks de condiciones + KO por veneno).
- Ataque (CoR, 7 handlers): EnergyValidation → ConfusionCheck → Selections → PreAttack →
  Modifier → DamageApplication → PostDamage (este último `alwaysRun` para detectar auto-KO).
- Efectos (Flyweight): `EffectRegistry` → `EffectLogic` (ataques), `TrainerEffectRegistry` →
  `TrainerEffectLogic` (trainers, con su propia mini-chain). Datos: `xy1_parsed.json` embebido
  que `PokemonTCGApiService` mergea en `Card.parsedEffects` al sincronizar el set.
- KO/victoria con SRP correcto: `KnockoutProcessor` solo muta (descarta, premios, setea
  `pendingSelection` CHOOSE_ACTIVE_ON_KO); `VictoryConditionChecker` decide; `SelectionResolver`
  aplica la elección del jugador; `TurnManager` reanuda el flujo.
- **Invariante sagrado:** `engine/` es Java puro; su única frontera con Spring/DB es
  `@FunctionalInterface CardLookup`. Se respeta en todo el código actual.

### 2.3 Cartas: ya NO hay persistencia en DB

`CardCacheService` (Caffeine, TTL 24 h) + `PokemonTCGApiService` (RestClient → pokemontcg.io).
**No existe `CardRepository`** — las entidades `Card`/`CardSet` viven solo en memoria.
Consecuencia: el backend necesita internet en el primer acceso a un set, y `Card` es @Entity
solo de nombre. `Deck`/`DeckCard` guardan `cardId` String y resuelven contra el caché.

### 2.4 Estado real por módulo

| Módulo | Estado | Evidencia |
|---|---|---|
| Auth (register/login JWT) | ✅ completo | `PlayerService`, filtro, `CurrentPlayerService` (CR-4 histórico resuelto) |
| Recuperación de contraseña | ✅ completo BE+FE | `RecoveryService` + `RecoveryToken` + mail + pantallas FE |
| Cartas (lista/detalle/sync) | ✅ BE (sin rarity/abilities/sets) | `CardController`, gaps en `SPECS/POKEDEX_SPEC.md` §14 |
| Decks CRUD + validación | ✅ completo | `DeckService` + 4 validators (60 cartas, 4 copias, ace, básico) |
| Engine (reglas, en sus tests) | ✅ con gaps de reglas (3.3-3.5) | suites engine verdes |
| Integración GameService↔engine | 🔴 ROTA | sección 3.1/3.2 |
| WebSocket STOMP | 🟡 publica, pero handshake 401 y sin auth STOMP | sección 3.6 |
| FE auth + deck builder | ✅ completos, API real | `features/auth`, `features/deck-builder` |
| FE lobby/board/pokedex/profile | 🟡 construidos post-merge, pero **TODO en modo mock** (ver §8.2) | `APP_DATA_MODE='mock'` en `app.config.ts` |
| FE websocket | 🔴 solo simulador (sin STOMP real) | `WEBSOCKET_SOURCE` → siempre `WebsocketMockService` |
| Efectos de ataque | 🟡 6 reales / 9 stubs / pasivas 0 | `docs/engine/EFFECT_IMPLEMENTATION_INVENTORY.md` |
| Trainers | 🟡 5 cartas jugables | Sycamore, Shauna, Red Card, Roller Skates, Team Flare Grunt |

---

## 3. Hallazgos CRÍTICOS (bloquean jugar de verdad)

> Orden = prioridad de arreglo. Los tres primeros son **la misma enfermedad**: el modelo de
> estado "DB" (`GameBoardState`, con `instanceId`) y el modelo de dominio (`BoardState`, con
> listas de ids planos) se reconcilian a mano en `EngineStateMapper` y la reconciliación es
> **con pérdida**.

### 3.1 ★ `EngineStateMapper.syncCardList` solo BORRA — las cartas que entran a una zona se pierden

`engine/mappers/EngineStateMapper.java:147-151`:

```java
private static void syncCardList(List<String> domainIds, List<CardInstanceState> dbList) {
    if (dbList != null && domainIds != null) {
        dbList.removeIf(c -> !domainIds.contains(c.getInstanceId()));   // borra, NUNCA agrega
    }
}
```

El engine mueve ids entre listas del dominio (deck→hand al robar, prizes→hand al tomar premio,
hand→discard al jugar trainer). Al persistir (`updateDbState`):

- **Robar carta:** se borra del `deck` de la DB, pero **nunca se agrega** a `hand` → la carta
  desaparece del juego para siempre. Cada turno el jugador pierde 1 carta en el limbo.
- **Tomar premio tras KO:** se borra de `prizeCards`, no llega a `hand` → premio evaporado.
- **`discardPile`: ni siquiera se llama `syncCardList`** → el descarte de la DB nunca crece.
- **`attachedEnergies`: no hay write-back en absoluto** (ni en active ni en bench) → adjuntar
  energía la saca de la mano y la tira al vacío. **Nunca podés juntar energía para atacar.**
- `playerTurnCount` se reconstruye **siempre en 0** (`toPlayerField(...).playerTurnCount(0)`) y
  `firstPlayerHasActed` se infiere de `turnNumber > 1` → ver 3.3.
- `pendingSelection` no se mapea en ninguna dirección → ver 3.4.
- `finishedReason` es siempre `"ENGINE_FINISHED"` (la Fase 5 diferida de player-selection).

**Por qué pasó (lo humano):** el mapper se escribió pensando "las listas solo se achican desde
afuera", que era cierto cuando el engine estaba huérfano. Al integrar `GameService` →
`GameEngineFacade` (¡bien hecho, era el CR-1!) nadie volvió a auditar el contrato del mapper.
Los tests del engine no lo ven porque testean el dominio puro sin round-trip de persistencia
(exactamente el conflicto que ya describía `docs/tracking/FACADE_INTEGRATION_DEBT.md`).

**Recomendación fuerte (decisión de arquitectura):** dejar de reconciliar a mano. Persistir el
`BoardState` de dominio **directo como JSON** (con un mapa `instanceId→cardId` aparte, o
adoptando instanceId como id canónico de las listas del dominio) y reducir `EngineStateMapper`
a mapear **solo hacia los DTOs de salida**. Sincronizar dos modelos mutables campo por campo es
una fábrica de este tipo de bugs. Si prefieren conservar `GameBoardState`, el mapper necesita:
agregar instancias nuevas (buscándolas en las otras zonas de la DB por instanceId), write-back de
energías/herramientas, sync del descarte, y persistir `playerTurnCount`, flags de retiro/ataque,
`pendingSelection` y `finishedReason`. Es mucho más código que la opción A.

### 3.2 ★ No existe SETUP → la partida termina "ganada" en el primer END_TURN

- `GameService.initializeGame` arranca el board en `phase=MAIN, turnNumber=0`, **sin Pokémon
  activos** (solo mano/deck/premios). `joinGame` pasa `WAITING → ACTIVE` directo; el estado
  `GameStatus.SETUP` existe pero **nunca se usa**.
- `ActionType.SETUP_PLACE_POKEMON` y `SETUP_SET_PRIZES` existen pero **ningún handler los
  procesa** (`MainPhaseState` los rechaza con "not valid in MAIN phase").
- `MainPhaseState.handlePlayBasicPokemon` manda **siempre a la banca** — no hay forma de poner
  un activo al inicio (solo retreat o promoción post-KO ponen activos).
- `VictoryConditionChecker.hasNoPokemon` = activo null + banca vacía. En el primer
  `END_TURN` de P1, `completeTurn → finishedByVictory` ve a P2 (que aún no jugó nada) sin
  Pokémon → **P1 gana `NO_POKEMON_LEFT` en el turno 1**. Game over.
- Bonus: el robo inicial de P1 nunca ocurre (nadie llama `beginTurn` al crear la partida) y el
  `TurnManager.beginTurn` documenta que el Facade lo llama "when a new game starts" — no es cierto.

**Falta diseñar el flujo de SETUP completo** (reglas: ambos colocan activo + banca opcional,
premios, recién ahí arranca el turno 1 con robo). Propuesta concreta en el TODO.

### 3.3 ★ Evolucionar es imposible por HTTP (y la regla del primer turno no aplica)

`RuleValidator.validateEvolvePokemon` exige `playerTurnCount > 1`, pero el mapper siempre
reconstruye `playerTurnCount = 0` (3.1) → `CANNOT_EVOLVE_FIRST_TURN` **eternamente** en partidas
reales. Y `validateUseAttack` bloquea el ataque del primer jugador con
`!firstPlayerHasActed && playerTurnCount == 1` → con count siempre 0, **nunca bloquea**: el
primer jugador puede atacar en su primer turno, contra el spec.

Detalle extra de specs: `DrawPhaseState` **saltea el robo** del primer jugador en su primer turno
(cita ENGINE_SPEC §1.2), pero `docs/engine/GAME_RULES.md` dice "At start of turn: active player
draws 1 card" sin excepción, y el rulebook XY real tampoco saltea el robo (lo que prohíbe es
atacar). **Decidan cuál es la regla canónica y alineen specs + código + tests.**

### 3.4 ★ `pendingSelection` no sobrevive entre requests → KO con banca = partida trabada

A nivel engine el mecanismo es perfecto (sesión 7). Pero el campo no se persiste
(`GameBoardState` no lo tiene / el mapper no lo mapea, Fase 5 diferida en
`openspec/changes/player-selection-mechanism/tasks.md`). Por HTTP:

1. Ataque con KO y banca → engine setea `pendingSelection`, corta el flujo, responde OK.
2. Al persistir, la selección **se pierde**. El defensor queda con activo null.
3. El `RESOLVE_SELECTION` del request siguiente encuentra `pendingSelection == null` →
   "There is no selection to resolve". No hay forma de promover. Juego muerto.

`GET /api/games/{id}/state` tampoco expone la selección pendiente (el FE no podría ni enterarse).

### 3.5 ★ Atacar DESCARTA las energías del costo — violación directa de reglas

`EnergyValidationHandler.handle` termina con
`ctx.getAttackerPokemon().setAttachedEnergies(remainingEnergies)` — "paga" el costo descartando.
En el TCG la energía **queda adjunta** al atacar (lo dice el propio `GAME_RULES.md`: "Energy
remains attached until: Pokémon leaves play / card effect removes energy"). Hoy un Pokémon con
2 energías ataca una vez y queda en cero. Encima ni siquiera van al descarte — se pierden del
contexto. Hay que eliminar el descarte (la validación con pool de dos pasadas — específicas
primero, Colorless después — está bien y se conserva).

### 3.6 ★ Regresión: handshake WebSocket devuelve 401 otra vez (1 test rojo)

`WebSocketHandshakeAuthIntegrationTest.wsInfo_withValidToken_isNotUnauthorized` está **rojo** en
`Produccion`. Causa: el fix archivado `fix-websocket-auth-lazyinit` tenía dos partes y el
cherry-pick **solo conservó el Fix A** (los `@ToString.Exclude`/`@EqualsAndHashCode.Exclude` en
`Player`). El **Fix B se perdió**: el `JwtAuthenticationFilter` actual solo lee el header
`Authorization: Bearer` — no soporta `?token=` por query param (que SockJS necesita, porque el
browser no puede mandar headers en el handshake) — y `SecurityConfig` no tiene `/ws/**` en
`permitAll`. Resultado: `/ws/info?token=<válido>` → anónimo → 401. **El FE no va a poder
conectar el WebSocket autenticado hasta restaurar esto.**

Además `WebSocketConfig` no registra **ningún** interceptor JWT para el frame STOMP CONNECT
(el `JwtChannelInterceptor` del diseño original nunca se escribió): cualquiera que conozca un
gameId puede suscribirse a `/topic/games/{id}/events`. Los eventos no llevan info privada
(CARD_DRAWN omite el cardId a propósito, bien ahí), así que es severidad media, pero para un
TPI con "seguridad" en la rúbrica conviene cerrarlo.

---

## 4. Hallazgos ALTOS

### 4.1 Frontend de juego: 0%

`features/game/**` (board, 5 zonas, action-panel, game-log, notifications, game-state.service,
game-actions.service), `features/lobby/**`, `core/services/websocket.service.ts` y
`shared/components/card-display` son **stubs de 5-9 líneas**. No existen rutas `/lobby` ni
`/game/:id` en `app.routes.ts`. La Pokédex tiene spec completa (`SPECS/POKEDEX_SPEC.md`) y solo
un placeholder de página. El plan detallado de qué construir está en TEAM2.md §Developer 6 y en
el TODO.

### 4.2 `CardCacheService.findById(instanceId)` dispara llamadas HTTP basura a pokemontcg.io

`findById` hace `cardId.split("-")[0]` para deducir el set. El `CardLookup` de
`GameService.buildCardLookup` le pasa **instanceIds UUID** primero: el "set" deducido es el
primer segmento del UUID (`"3f8a2c1b"`), Caffeine no lo tiene → **fetch HTTP real** a la API
externa por un set inexistente → lista vacía → `IllegalArgumentException` → recién ahí cae al
fallback por board. Cada instanceId nuevo = 1 request externo perdido + 1 entrada basura en el
caché. Fix barato: detectar formato UUID (o probar primero contra el board) antes de ir al caché;
fix correcto: resolver instanceId→cardId ANTES de llamar al lookup.

### 4.3 Orden de modificadores de daño incorrecto

`DamageApplicationHandler`: `finalDamage = DamageCalculator.calculate(base) + ctx.getDamageModifiers()`.
Los modificadores del ATACANTE (Muscle Band, AddDamage pre-daño) deben sumarse **antes** de
aplicar debilidad ×2 (TEAM2.md §Dev4 lo especifica: "base + modificadores del atacante → ×2 →
−20 → modificadores del defensor"). Hoy un Muscle Band contra debilidad suma 20 en vez de 40.
Además `ModifierHandler` tiene a Muscle Band **hardcodeado por nombre** y con la regla mal (suma
solo a EX/MEGA; la carta real suma a todos los ataques de quien la porta).

### 4.4 Ticks de condiciones a media frecuencia

`BetweenTurnsState` solo tickea veneno/quemadura/sueño/parálisis del **jugador que terminó su
turno**. En el TCG el "Pokémon Checkup" corre entre CADA turno para **ambos** activos: un
defensor envenenado debería recibir 10 entre cada medio-turno (2 ticks por ronda), hoy recibe 1.
Parálisis sí coincide de casualidad con la regla ("se cura al final del turno del dueño").

### 4.5 Mulligan a medias

`GameService.buildPlayerBoardFromDeck`: hasta 3 reshuffles buscando un básico y si no aparece
"use whatever we have" → puede arrancar una partida **sin básicos en mano y sin activo posible**.
Falta: garantía dura (el deck validado exige ≥1 básico, pero puede estar en premios), revelado
de mulligan al rival y la carta extra opcional por mulligan (GAME_RULES.md §Mulligan).

### 4.6 Flags de turno que no persisten

Solo `energyAttached`, `supporterPlayed` y `aceTacticianPlayed` viajan a la DB.
`retreatedThisTurn`/`attackedThisTurn` se pierden entre requests → un cliente puede **retirarse
N veces por turno** mandando N requests (el ataque corta turno, así que ese flag importa menos).

### 4.7 Premios visibles para su dueño

`GameService.toPlayerFieldDTO` manda `prizeCards` propios **con cardId**. En el TCG los premios
están boca abajo también para el dueño — saber cuáles son cambia decisiones estratégicas.
Deberían viajar como conteo o instancias sin cardId (como ya se hace con el rival).

### 4.8 Errores 500 evitables en `GameService`

- Tercero consulta una partida `WAITING` (`player2 == null`): el check de participante hace
  `session.getPlayer2().getId()` → **NPE/500** en vez de 403. Mismo patrón en `performGameAction`.
- `CONCEDE` sobre partida `WAITING`: pasa la guarda (solo excluye FINISHED) y revienta con
  `IllegalStateException("Game state not found")` porque el board se crea recién en el join.

---

## 5. Hallazgos MEDIOS / BAJOS (lista de limpieza)

1. **Efectos**: 9 de 15 `EffectLogic` son stubs (`SearchDeck`, `SwitchPokemon`, `LookAtDeck`,
   `PreventDamage`, `Restrict`, etc.); `PASSIVE_ABILITY` parseada (12 abilities) pero sin logic →
   no hace nada. Fuente de verdad: `docs/engine/EFFECT_IMPLEMENTATION_INVENTORY.md` + plan
   aprobado `docs/engine/RESUMABLE_ATTACK_EFFECTS_PLAN.md` (Memento + CoR reanudable).
2. **Trainers**: solo 5 cartas con `trainerEffects` en `xy1_parsed.json`. Jugar cualquier otro
   Item/Supporter **consume la carta sin efecto, en silencio**. Mínimo: rechazar trainers sin
   efectos conocidos (o whitelistear), para no comerle cartas al jugador.
3. **Stadium**: el reemplazado va al descarte del **jugador actual** aunque el dueño fuera el
   rival; el nuevo queda solo como `activeStadiumCardId` (nunca llega al descarte de nadie al
   reemplazarse el suyo propio).
4. **Evolución**: `field.getDiscardPile().add(targetCardId)` mete un **cardId** en una lista que
   (por HTTP) contiene **instanceIds** — vocabulario mezclado; además el TODO de trackear el
   stack de evolución para el KO sigue abierto.
5. **Bench identity en el mapper**: preserva instancia por posición+cardId; con dos copias del
   mismo Pokémon en banca y una promoción, las energías pueden "saltar" de una copia a la otra.
   `existing.getCardId()` null → NPE potencial.
6. **`toDomainEvents`**: tipo de evento desconocido cae a `PHASE_CHANGED` silencioso — enmascara
   bugs de mapeo de eventos.
7. **CORS** `allowedOrigins("*")` + headers `*`. Para la entrega, restringir a
   `http://localhost:4200` (config y doc del TEAM2 lo piden).
8. **Recovery**: `requestRecovery` manda el mail en línea → la latencia delata si el email
   existe (enumeración por timing). `@Async` o cola lo resuelve. Menor para TPI.
9. **`docs/SDD.md` no existe** pero `README.md` y `TEAM2.md` lo referencian como lectura
   obligatoria. O se restaura o se corrigen las referencias.
10. **FE sin tests** (1 spec placeholder). Los servicios de deck-builder/auth son testeables ya.
11. **`CardInstanceDTO.name`** viaja casi siempre null (nadie setea `CardInstanceState.name`) —
    el FE va a tener que resolver nombres por `cardId` contra `/api/cards/{id}`; documentarlo.
12. **`GameSessionResponse`** no expone `winner` ni `finishedAt` — el lobby/fin de partida los
    va a necesitar.
13. **Maven**: `pom.xml` aún con `name`/`description` del template (TODO en ADR 006).
14. Doble `GameEventPublisher` path: el `@EventListener` legacy publica a `/topic/game/{id}`
    (singular) y el directo a `/topic/games/{id}/...` (plural) — unificar antes de cablear el FE.

---

## 6. Lo que está BIEN (para no tocarlo "de pasada")

- **Aislamiento del engine** impecable: cero Spring en la lógica (las anotaciones `@Service` en
  `TurnManager`/`GameEngineFacade` son solo wiring), `CardLookup` como única frontera, tests con
  lambdas. Es el activo arquitectónico más valioso del proyecto.
- **SRP del subsistema KO/victoria/selección** (sesión 7): KnockoutProcessor muta,
  VictoryConditionChecker decide, SelectionResolver aplica, TurnManager reanuda. Extensible a
  los próximos `SelectionType` sin tocar el orquestador.
- **Parser resiliente** (`UnknownEffect` defaultImpl + `UnknownTrainerEffect`): un type
  desconocido ya no borra los efectos de la carta (bloque robust-effect-parser).
- **DeckService + validators**: reglas de mazo completas, merge de entradas duplicadas,
  ownership en cada operación, respuesta de validación con códigos.
- **Auth/recovery**: BCrypt, token único de un solo uso con expiración, respuesta genérica
  anti-enumeración, filtro JWT con `shouldNotFilter` para públicos.
- **DTOs con information hiding**: mano del rival como `handSize`, premios del rival ocultos,
  `CARD_DRAWN` sin cardId en el payload.
- **FE deck-builder**: arquitectura limpia (data-access abstracto con providers mock/real,
  mappers, signals, OnPush).

---

## 7. Estrategia recomendada para la semana

> El detalle tarea por tarea, con estimaciones y orden de dependencias, está en
> [`docs/TODO/PROJECT_TODO.md`](../TODO/PROJECT_TODO.md). Acá va la lógica.

1. **Días 1-2 — Hacer jugable el backend E2E** (sin esto, el FE de juego no tiene contra qué
   desarrollar): decisión de persistencia (3.1), SETUP (3.2), no descartar energía (3.5),
   restaurar Fix B WebSocket (3.6), Fase 5 (`pendingSelection` + `finishedReason` por `GET /state`).
   Criterio de salida: **una partida completa por Swagger/HTTP** (crear → unir → setup → turnos
   con robo/energía/ataque → KO → selección → victoria por premios).
2. **Días 3-5 — Frontend de juego**: websocket real → lobby → board read-only → acciones.
   En paralelo (otro dev): Pokédex según spec.
3. **Día 6 — Reglas finas y efectos**: ticks dobles, mulligan, orden de modificadores, y si hay
   aire los 4 stubs con selección (plan resumable ya aprobado — si no llegan, recortar alcance
   EXPLÍCITAMENTE y documentarlo).
4. **Día 7 — Hardening**: los 500 evitables, premios ocultos, tests de integración del flujo
   completo, demo end-to-end ensayada.

---

## 8. ADDENDUM post-merge (2026-06-11, tras `785aba1`)

> Re-verificación sobre el `Produccion` actual después del merge del equipo
> (PR #12 + lobby/pokédex/board/profile). El merge tocó **casi nada de backend**
> (CardSet, DeckRepository, DeckService) — todos los bugs de §3 siguen vigentes.

### 8.1 WebSocket: ~~SIGUE ROTO~~ → ✅ RESUELTO el mismo día (commit `d3201b1`)

> **Status update:** el Fix B se portó a `Produccion` combinando lo mejor de ambas ramas
> (`?token=` SOLO en `/ws/**` + `shouldNotFilter`/`/error` intactos). Test handshake verde,
> suite completa verde. Queda P1: `JwtChannelInterceptor` para el CONNECT STOMP.
> El análisis original se conserva abajo como registro del diagnóstico.

Re-ejecutado hoy contra el HEAD post-merge:
`WebSocketHandshakeAuthIntegrationTest.wsInfo_withValidToken_isNotUnauthorized` → **ROJO (401)**.

La situación exacta (verificada con diff entre ramas):

| Pieza del fix | `Produccion` | `origin/RamiroBranch` |
|---|---|---|
| Filtro lee `?token=` (Fix B, lo que SockJS necesita) | ❌ | ✅ (`request.getParameter("token")` con fallback al header) |
| `/error` en permitAll (no enmascarar errores como 401) | ✅ | ❌ |
| `shouldNotFilter` para rutas públicas | ✅ | ❌ |
| `JwtChannelInterceptor` (auth del CONNECT STOMP) | ❌ | ❌ (no existe en ninguna rama) |

**Cada rama tiene media solución.** El arreglo es portar a `Produccion` el bloque de
`doFilterInternal` de RamiroBranch (~10 líneas: token query param primero, header como
fallback) SIN pisar el `shouldNotFilter` ni el `/error` que Produccion ya tiene. Hasta
entonces, el FE no puede conectar SockJS autenticado.

### 8.2 Frontend: estado REAL post-merge (reemplaza §4.1)

El equipo avanzó mucho — pero **toda la app corre contra mocks**:

- **Arquitectura nueva (buena):** cada feature expone un token `*_SOURCE`
  (`AUTH_SOURCE`, `LOBBY_SOURCE`, `GAME_STATE_SOURCE`, `GAME_ACTIONS_SOURCE`,
  `WEBSOCKET_SOURCE`, `PROFILE_SOURCE`) con implementación mock y http, conmutadas por
  `APP_DATA_MODE` en `app.config.ts`. **Hoy `APP_DATA_MODE = 'mock'` global** → login,
  lobby, juego y perfil son simulados aunque el backend esté corriendo.
- **Game** (`features/game`, 51 archivos): board real (82 líneas) + zonas, `GameStateService`
  (382 líneas, signals) + `game-state.mapper` que YA mapea el `BoardStateDTO` real del
  backend, `GameStateApiService`/`GameActionsApiService` (HTTP real) listos detrás del token.
- **🐛 Bug de contrato bloqueante** (`game-actions-api.service.ts:25-35`): mapea
  `'attack' → 'ATTACK'` pero el enum del backend es **`USE_ATTACK`**; `'playTrainer' →
  PLAY_SUPPORTER` (Items quedan sin acción); **faltan** `PLAY_BASIC_POKEMON`, `PLAY_ITEM`,
  `ATTACH_TOOL`, `CONCEDE` y `RESOLVE_SELECTION`. En modo api, atacar daría 400 por enum
  inválido. No hay manejo de `pendingSelection` ni de SETUP fuera de los mocks.
- **WebSocket: sin implementación real.** `WEBSOCKET_SOURCE` inyecta **siempre**
  `WebsocketMockService` (sin ternario por modo); `core/services/websocket.service.ts` es un
  simulador documentado ("never opens a real socket"). `@stomp/stompjs` y `sockjs-client` ya
  están en `package.json` pero **ningún archivo los importa**.
- **Lobby real** en `features/auth/lobby` (`LobbyPage`, ruta `/lobby`) + `lobby-http.service`
  alineado con `GET/POST /api/games` y `/join`. Ubicación rara (dentro de auth) — mover a
  `features/lobby` y borrar el stub viejo. **`/lobby`, `/perfil` y `/pokedex` están SIN
  `authGuard`** (con API real, el lobby necesita JWT).
- **Pokédex**: implementada con componentes según la spec (filter-bar, card-grid,
  card-preview, pagination, mappers, domain models) pero contra `mock-pokedex-api.service`.
  El mapper de `CardResponse` real ya existe — falta el servicio http y conmutar.
- **Profile** (`/perfil`): feature nueva fuera de toda spec (mock + http listos). Scope creep
  consciente o no — decidir si entra en la semana.
- **URLs hardcodeadas** `http://localhost:8080` en todos los servicios http nuevos (bypassean
  `proxy.conf.js`; centralizar en un `environment`/token de base URL).
- Stubs muertos que quedaron: `features/lobby` viejo, `shared/components/card-display`,
  `core/services/websocket.service.ts` (wrapper del mock).

**Consecuencia para el plan:** el Bloque B del TODO cambió de "construir desde cero" a
"**conmutar a modo api y cerrar los huecos**": websocket STOMP real, mapeo de ActionType
correcto, SETUP + selección en UI, guards, y URLs por environment. Sigue dependiendo 100%
del Bloque A (con los bugs de §3, el modo api se estrella al tercer turno).

### 8.3 Efectos y habilidades: precisión post-auditoría (corrige §5.1)

> **Status update (mismo día):** las HABILIDADES tier 1 se implementaron tras esta auditoría —
> 6 de 12 ejecutan (3 familias), suite 217 verde. Ver `ENGINE_GUIDE.md` §28 y
> `EFFECT_IMPLEMENTATION_PROGRESS.md` Bloque 4. El análisis de abajo describe el estado PREVIO.

Verificado archivo por archivo en `engine/effects/logics/` + `EffectRegistry`:

- **9 logics REALES** (con flag pre/post → se ejecutan): ApplyCondition, Heal, AddDamage,
  MultiplierDamage, CoinFlip, DiscardEnergy, DamageCounters, DrawUntilHandSize, ShuffleHand.
- **6 logics STUB** (12 líneas, `println`): SearchDeck, SwitchPokemon, LookAtDeck,
  DamageToBench, PreventDamage, Restrict. Detalle clave: **ninguna declara
  `isPreDamage`/`isPostDamage`, así que los handlers NUNCA las invocan** — están registradas
  pero muertas. Las 4 primeras requieren el mecanismo de selección (plan
  `RESUMABLE_ATTACK_EFFECTS_PLAN.md`); PreventDamage/Restrict requieren estado de efecto
  persistente entre turnos (no existe).
- **HABILIDADES (abilities): 0%.** `PassiveAbilityEffect` se parsea (12 abilities en el set)
  pero NO está registrada en `EffectRegistry` (el TODO de la línea 43 lo admite) y no hay
  ganchos pasivos en `DamageCalculator`/`RuleValidator`/retiro. Ninguna habilidad del set
  hace nada — era el "Bloque 4 (continuos/pasivos)" que nunca se empezó.
- `ModifierHandler`: confirmado que Muscle Band está hardcodeado por nombre y aplica +20
  **solo si el DEFENSOR es EX/MEGA** — la carta real suma +20 a todos los ataques del
  portador, sin mirar al defensor.

---

*Review generado el 2026-06-11 sobre `Produccion@3242b1e`; addendum §8 re-verificado el
mismo día sobre el HEAD post-merge (`785aba1` + rebase). Tests ejecutados: BE 193 (1 rojo,
ver 3.6/8.1 — re-confirmado post-merge), FE 1 (placeholder). Ningún archivo de código fue
modificado.*
