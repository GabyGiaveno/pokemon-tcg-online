# AGENTS.md — Contexto operativo para agentes IA

> Generado del code review integral 2026-06-11 (`Produccion@3242b1e`).
> Para humanos: `docs/review/CODE_REVIEW_2026-06-11.md`. Roadmap: `docs/TODO/PROJECT_TODO.md`.
> Para FE específico ya existe `FE/AGENTS.md` (convenciones Angular — respetarlo).

## Qué es

Pokémon TCG digital 1v1 (reglas XY1), TPI UTN FRC. Monorepo: `BE/` Spring Boot 4 / Java 21 /
Maven wrapper; `FE/` Angular 20.3 standalone+signals; PostgreSQL+Flyway (prod) / H2 (perfil dev);
STOMP sobre SockJS. Specs canónicas en `docs/` (índice: `docs/README.md`).

## Comandos

```bash
cd BE && ./mvnw.cmd test                 # suite completa (193 tests; 1 rojo conocido, ver BUG-6)
cd BE && ./mvnw.cmd spring-boot:run      # requiere .env (DB_URL, JWT_SECRET, MAIL_*, FRONTEND_URL)
cd FE && npm start                       # ng serve en :4200 (proxy.conf.js → :8080)
cd FE && npx ng test --watch=false --browsers=ChromeHeadless   # 1 spec placeholder
```

Convenciones: commits convencionales SIN atribución a IA. Cobertura objetivo: engine ≥70%,
services ≥60% (JaCoCo en `verify`). Checkstyle+PMD corren en el build.

## Invariantes arquitectónicos (ROMPERLOS = revert)

1. **`BE/.../engine/` es Java puro.** Nada de repositorios, HTTP ni `@Transactional` adentro.
   Única frontera con persistencia: `@FunctionalInterface CardLookup` (`engine/CardLookup.java`).
   En tests se mockea con una lambda `id -> testCards.get(id)`.
2. **El FE nunca calcula reglas de juego.** Solo renderiza `BoardStateDTO` y manda acciones.
3. **El rival nunca ve mano/orden del mazo.** `OpponentFieldDTO.handSize`, premios ocultos,
   `CARD_DRAWN` sin cardId en payload.
4. Patrones vigentes: State (`engine/state/*PhaseState`), Chain of Responsibility
   (`engine/chain/` 7 handlers + `engine/effects/trainers/chain/`), Flyweight+Jackson polimórfico
   (`EffectRegistry`/`TrainerEffectRegistry`, datos en `BE/src/main/resources/data/xy1_parsed.json`),
   Facade (`GameEngineFacade`), factories (`engine/factories/`), validador estático (`RuleValidator`).

## Flujo de una acción (web → engine)

`GameController` → `GameService.performGameAction` (carga `GameState.stateJson` →
`GameBoardState`) → `GameEngineFacade.applyAction` → `EngineStateMapper.toDomainState` →
`TurnManager.processAction` (guarda pendingSelection → turno → fase MAIN → CONCEDE/END_TURN/
USE_ATTACK o `MainPhaseState`) → `EngineStateMapper.updateDbState` → persistir + STOMP
(`/topic/games/{id}/events` y `/state-changed`).

Identificadores: el dominio del engine usa ids String opacos; por HTTP las listas llevan
**instanceId** (UUID por copia física) y el `CardLookup` de `GameService.buildCardLookup`
resuelve instanceId→cardId contra el board. Las cartas NO están en DB: solo caché Caffeine
(`CardCacheService`, TTL 24 h) contra pokemontcg.io (`PokemonTCGApiService`), que mergea
`xy1_parsed.json` en `Card.parsedEffects`.

## BUGS CONOCIDOS Y VERIFICADOS (no re-descubrir; citas exactas)

| # | Severidad | Qué | Dónde |
|---|---|---|---|
| BUG-1 | CRÍTICO | `syncCardList` solo borra: robar/tomar premio pierde la carta; `attachedEnergies` sin write-back; `discardPile` nunca se sincroniza; `playerTurnCount` siempre 0 | `engine/mappers/EngineStateMapper.java:147-151`, `:63`, `updatePlayerBoardState` |
| BUG-2 | CRÍTICO | No hay SETUP: board nace `MAIN` sin activos; `SETUP_PLACE_POKEMON`/`SETUP_SET_PRIZES` sin handler; `GameStatus.SETUP` sin uso → P1 gana `NO_POKEMON_LEFT` en su primer END_TURN | `GameService.initializeGame`, `MainPhaseState.handle`, `VictoryConditionChecker.hasNoPokemon` |
| BUG-3 | CRÍTICO | Evolución imposible por HTTP (`playerTurnCount<=1` siempre, por BUG-1); regla "P1 no ataca turno 1" nunca aplica | `RuleValidator.java:79`, `:234` |
| BUG-4 | CRÍTICO | `pendingSelection` no se persiste/expone → KO con banca traba la partida por HTTP (Fase 5 diferida) | `EngineStateMapper.updateDbState`, `openspec/changes/player-selection-mechanism/tasks.md` §F5 |
| BUG-5 | CRÍTICO | Atacar descarta las energías del costo (regla violada: la energía queda adjunta) | `engine/chain/handlers/EnergyValidationHandler.java:87` |
| BUG-6 | ✅ RESUELTO 2026-06-11 | Regresión WS 401: el filtro ahora acepta `?token=` SOLO en `/ws/**` (fallback header; más restrictivo que RamiroBranch a propósito — no filtrar tokens a los logs). Test handshake VERDE, suite 193 verde. PENDIENTE [P1]: `JwtChannelInterceptor` para el CONNECT STOMP (no existe en ninguna rama — topics sin auth) | `security/JwtAuthenticationFilter.java` (doFilterInternal), `configs/WebSocketConfig.java` (interceptor pendiente) |
| BUG-7 | ALTO | `findById(instanceId-UUID)` deduce "set" del primer segmento del UUID → fetch HTTP real a pokemontcg.io por set inexistente, por cada instanceId | `services/CardCacheService.java:92-93` + `GameService.buildCardLookup` |
| BUG-8 | ALTO | Modificadores de daño se suman DESPUÉS de debilidad (deben ir antes para el atacante); Muscle Band hardcodeado por nombre y solo EX/MEGA | `DamageApplicationHandler.java:37-40`, `ModifierHandler` |
| BUG-9 | ALTO | Ticks de veneno/quemadura/sueño solo para el jugador que terminó el turno (media frecuencia) | `engine/state/BetweenTurnsState.java:56-69` |
| BUG-10 | ALTO | Mulligan: 3 reintentos y arranca igual sin básico; sin draw extra del rival | `GameService.buildPlayerBoardFromDeck:419-435` |
| BUG-11 | MEDIO | `retreatedThisTurn`/`attackedThisTurn` no persisten → multi-retiro por turno vía HTTP | `EngineStateMapper.toPlayerField` (TurnFlags parciales) |
| BUG-12 | MEDIO | NPE→500: tercero consulta partida WAITING (`player2` null); CONCEDE en WAITING → `IllegalStateException` | `GameService.getGameState:150`, `performGameAction:211,222-230` |
| BUG-13 | MEDIO | Premios PROPIOS expuestos con cardId (deberían estar boca abajo también para el dueño) | `GameService.toPlayerFieldDTO:540` |
| BUG-14 | MEDIO | Trainers sin datos (todos salvo 5) se consumen sin efecto y en silencio; stadium reemplazado va al descarte del jugador equivocado | `MainPhaseState.handlePlayItem/handlePlayStadium` |

Estado de efectos (actualizado 2026-06-11 post-Bloque 4): **9 logics de ataque reales** con flag
pre/post y **6 stubs** (SearchDeck, SwitchPokemon, LookAtDeck, DamageToBench, PreventDamage,
Restrict) sin flag → nunca invocados; requieren selección/resumable.
**HABILIDADES: tier 1 EJECUTA (6/12)** — paquete `engine/effects/abilities/`: `AbilityParser`
(abre el bloque "abilities"), `USE_ABILITY`+`AbilityActivationResolver` (Mystical Fire),
`AbilityTriggerResolver` en PostDamageHandler (Spiky Shield, Destiny Burst),
`ContinuousAbilityQuery` como guards (Forest's Curse → ITEMS_LOCKED, Fur Coat → −20 post-W&R,
Sweet Veil → inmunidad). Tier 2 (6 con selección) se rechaza con `ABILITY_NOT_SUPPORTED`.
**Guía exhaustiva del engine: `docs/engine/ENGINE_GUIDE.md`** (§28 habilidades, §28.5 receta
para agregar una). `ModifierHandler`: Muscle Band sigue hardcodeado y con regla incorrecta
(BUG-8). Estado vivo por bloque: `docs/engine/EFFECT_IMPLEMENTATION_PROGRESS.md`; plan stubs
con selección: `docs/engine/RESUMABLE_ATTACK_EFFECTS_PLAN.md`.

Estado FE (post-merge `785aba1`): arquitectura por tokens `*_SOURCE` (mock/http) conmutados por
`APP_DATA_MODE` en `app.config.ts` — **HOY 'mock' global: TODA la app (incluido login) corre
simulada**. Construido: lobby (`features/auth/lobby` — ubicación rara), game board+zonas+
`GameStateService` (382 líneas) con mapper del `BoardStateDTO` real, pokédex completa (contra
mock), profile (fuera de spec). Rutas: `/lobby`, `/perfil`, `/game` (lazy), `/decks` —
`/lobby` y `/perfil` SIN authGuard. **WebSocket sin implementación real**: `WEBSOCKET_SOURCE`
inyecta siempre `WebsocketMockService`; `@stomp/stompjs`/`sockjs-client` están en package.json
pero nadie los importa. **Bug de contrato**: `game-actions-api.service.ts` mapea
`'attack'→'ATTACK'` (el BE espera `USE_ATTACK`), faltan `PLAY_BASIC_POKEMON`/`PLAY_ITEM`/
`ATTACH_TOOL`/`CONCEDE`/`RESOLVE_SELECTION`; URLs hardcodeadas `http://localhost:8080`.

## Gotchas para no perder tiempo

- Los errores de reglas NO son HTTP errors: `POST /actions` devuelve 200 con
  `{success:false, errorMessage}`.
- En tests de logics/efectos setear **`cardId`** en `ActivePokemon`/`BenchPokemon` (código
  null-hostil con `Map.of`); el builder usa `isPoisoned`/`isBurned`.
- COIN_FLIP = pre-daño; DISCARD_ENERGY = post-daño (flags `isPreDamage`/`isPostDamage` en la logic —
  una logic sin flag NO se ejecuta nunca).
- `CardInstanceDTO.name` viene null casi siempre; resolver nombres por `/api/cards/{id}`.
- `GameEngineFacadeTest` fue BORRADO a propósito por el equipo ("deleted bad tests") — no
  resucitarlo; la deuda real está descrita en `docs/tracking/FACADE_INTEGRATION_DEBT.md`.
- `docs/SDD.md` NO existe aunque README/TEAM2 lo citen.
- SDD/openspec activo en `openspec/changes/` (player-selection-mechanism abierto, Fase 5
  pendiente); artefactos espejo en engram cuando hay MCP.
- El equipo trabaja en la rama `Produccion` (no `main`). Preguntar antes de bloques de código
  grandes; el usuario aprueba fase por fase.

## Orden de ataque recomendado (detalle en docs/TODO/PROJECT_TODO.md)

A: persistencia (BUG-1) → SETUP (BUG-2) → energía (BUG-5) → WS (BUG-6) → Fase 5 (BUG-4)
⇒ partida E2E por Swagger. B: FE websocket → lobby → board → acciones → pokédex.
C: reglas finas (BUG-8/9/10) + stubs de efectos. D: hardening (BUG-12/13, test E2E, CORS).
