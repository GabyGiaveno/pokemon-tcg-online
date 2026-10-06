# Changelog

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto se adhiere a [Semantic Versioning](https://semver.org/lang/es/).

## [Unreleased]

## [1.1.1] - 2026-06-28

### Fixed
- **Rastro residual de Ace Spec:** removido el texto "Máximo 1 carta Ace Tactician" de la UI del deck-builder (era visible al usuario y contradecía que la regla no existe en XY1) y la mención "Ace Spec" de la descripción Swagger de `POST /decks/{id}/validate`. (Nota: el campo `aceTactician` —siempre `false`— persiste en modelos y mocks del frontend como deuda cosmética sin impacto funcional.)

## [1.1.0] - 2026-06-27

### Added
- **Muerte Súbita (Sudden Death):** detección de empate simultáneo (doble KO con 1 carta de Premio restante cada uno) en `VictoryConditionChecker`. Al detectarse, en lugar de declarar ganador se inicia una nueva ronda con 1 sola carta de Premio por jugador, re-deal de mano (4 cartas) y re-setup. Se repite hasta que haya un ganador. Nuevos valores `VictoryReason.SIMULTANEOUS_KO`, `GameStatus.SUDDEN_DEATH` y evento `GameEventType.SUDDEN_DEATH_START`.
- **Muerte Súbita — persistencia:** migración `V015__add_sudden_death_round.sql` agrega la columna `sudden_death_round`. La fuente de verdad de una ronda de Muerte Súbita es `suddenDeathRound > 0` (status `SETUP`), expuesta en `BoardStateDTO` para reconexión y reconstrucción del estado.
- **Muerte Súbita — frontend:** manejo del evento `SUDDEN_DEATH_START` en el feed, overlay de transición con auto-dismiss e indicador de ronda en el header del board.
- **Cobertura de tests:** suite ampliada a 896 tests (coverage de instrucciones 85.9%). Nuevos tests de seguridad (`JwtService`, `CurrentPlayerService`), mail (`EmailService`), gamificación (`AchievementUnlockService`, `BadgeUnlockService`) e integración del ciclo de vida de partida (`GameLifecycleIntegrationTest`).
- **Documentación:** spec de Muerte Súbita con checklist de tareas en `openspec/changes/sudden-death/SPEC.md`. README aclara que el mail es opcional (fallback a consola con el link de recuperación) y documenta la política de credenciales.

### Fixed
- **Regla Ace Spec:** removida la lógica de "As Táctico" del engine — no existe en el reglamento oficial XY1. Eliminado el chequeo `ACE_TACTICIAN` y su límite por turno en `RuleValidator`. (Nota: queda pendiente eliminar `AceTacticianValidator` y el resto del rastro residual del modelo de cartas.)
- **Timing de cura de parálisis:** la parálisis ahora se cura en el momento correcto del ciclo entre turnos.
- **Selección al pasar turno:** corregido el paso de turno cuando hay una selección pendiente.
- **Recuperación de contraseña con PostgreSQL:** integración del flujo de reset de contraseña con el perfil de producción.

### Changed
- `PlayerPrivateDTO` marcado como DTO legacy — el estado privado se obtiene vía `GET /api/games/{id}/state`, no por WebSocket.

## [1.0.0] - 2026-06-27

### Added
- **Infraestructura — PostgreSQL:** `docker-compose.yml` con PostgreSQL 15-alpine y healthcheck. Perfil `prod` (`application-prod.yml`) con Flyway habilitado y `ddl-auto: validate`. Instrucciones completas en README.
- **Migraciones Flyway:** V011 — recrea tabla `card_set` eliminada en V004 pero referenciada por la entidad JPA. V012 — corrige tipo de `deck_card.quantity` de `SMALLINT` a `INTEGER`. V013 — agrega columnas faltantes en `game_session` (`player1_ready`, `player2_ready`, `coin_flip_winner_id`) y corrige tipo de `prize_cards_count`.
- **Seed data (V014):** Jugadores de ejemplo (`ash`, `misty`, `brock`), dos mazos válidos de 60 cartas y una partida finalizada con log de acciones representativo.
- **Log de acciones — completitud:** `GameActionLogService` con `@Transactional(REQUIRES_NEW)` — las acciones fallidas ahora se persisten en un commit independiente al del business transaction (sobreviven rollback). Antes solo se logueaban las acciones exitosas.
- **Log de acciones — resultado real:** El campo `result` ahora serializa datos reales: `{"status":"SUCCESS","events":[...]}` en éxito, `{"status":"FAILURE","message":"..."}` en fallo. Antes era siempre `{"status":"SUCCESS"}` hardcodeado.
- **API — lectura del log:** Nuevo endpoint `GET /api/games/{id}/actions` (autenticado, solo participantes). Devuelve el log ordenado por `turnNumber ASC, id ASC`. `GameActionRepository` con finder `findByGameSession_IdOrderByTurnNumberAscIdAsc`.
- **DTO:** `GameActionDto` con todos los campos del log para la respuesta del endpoint.
- **Swagger / OpenAPI:** Anotaciones `@Operation`, `@ApiResponse`, `@Tag`, `@SecurityRequirement` en todos los controllers. `SpringDocConfig` actualizado.

### Fixed
- **Spring Boot 4.0 — Flyway:** `FlywayAutoConfiguration` fue extraída del `spring-boot-autoconfigure` a un módulo separado `spring-boot-flyway` en Spring Boot 4. Agregada la dependencia faltante en `pom.xml`.
- **Tests con H2:** Flyway deshabilitado en contexto de test (`spring.flyway.enabled=false`) para que H2 siga usando `ddl-auto=create-drop` sin conflicto.
- **Bug #3 — Daño de recoil (Take Down):** El daño de recoil se aplicaba al defensor en lugar del atacante. Corregido en `PostDamageHandler` y `AddDamageLogic`.
- **Bug #4 — Setup targetPosition:** `SETUP_PLACE_POKEMON` ignoraba el campo `targetPosition`. Ahora coloca correctamente en `ACTIVE` o `BENCH` según lo indicado, y bloquea colocar en `BENCH` antes de tener un Pokémon Activo.

### Changed
- `application.properties` (main): eliminadas propiedades H2 y JPA hardcodeadas que pertenecen al perfil de test/local.
- `GameService`: `logAction()` privado reemplazado por inyección de `GameActionLogService`. El log es ahora un side-effect explícito con transacción propia.

## [0.4.0] - 2026-06-27

### Added
- **Engine - Trainer Cards (Group A):** Implementación completa de 5 Trainers con selección asíncrona del jugador: Great Ball (`SEARCH_DECK` top 7, filtro Pokémon → mano), Professor's Letter (`SEARCH_DECK` mazo completo, filtro Energía Básica × 2 → mano), Evosoda (`SEARCH_DECK` top 8, filtro Evolución → banca), Max Revive (`RECYCLE` desde descarte, costo 2 cartas, destino banca con HP/2), Cassius (`SHUFFLE_POKEMON_TO_DECK` con cartas adjuntas).
- **Engine - Trainer Cards (Group B — Passive):** Implementación de 4 cartas pasivas via `PassiveEffectRegistry` (Singleton): Muscle Band (+20 daño saliente pre-debilidad), Hard Charm (−20 daño entrante post-debilidad), Fairy Garden (costo de retirada → 0 si el Pokémon activo tiene energía Fairy adjunta), Shadow Circle (suprime debilidad si el defensor tiene energía Darkness adjunta).
- **Engine - Arquitectura:** `TrainerContext` value object (espejo de `AttackContext`); `TrainerResolutionChain` refactorizado para operar sobre `TrainerContext`; `TrainerEffectLogic<T>` con firma primaria `execute(T, TrainerContext)` y adaptador legacy. Interfaces `ToolEffect` y `StadiumEffect` con defaults no-op. `PassiveEffectRegistry` con mapas separados para tools y stadiums.
- **Engine - SelectionResolver:** Soporte para 3 nuevos tipos — `PLACE_ON_BENCH` (crea `BenchPokemon` con HP completo vía `CardLookup`), `CHOOSE_FROM_DISCARD` (revive con `max(1, maxHp/2)`), `SHUFFLE_POKEMON_TO_DECK` (remueve Pokémon + adjuntos de banca, agrega al mazo y lo mezcla). `CardLookup` agregado como 4.° parámetro con overload backward-compatible.
- **Engine - DamageApplicationHandler:** Pipeline de daño extendido con hooks pasivos: Muscle Band (paso 1b, pre-debilidad), Shadow Circle (paso 2, supresión de debilidad), Hard Charm (paso 3b, post-debilidad). Null-guard en `ctx.getBoard()` para tests sin board.
- **Engine - MainPhaseState:** `handleRetreat` consulta `PassiveEffectRegistry` para aplicar `modifyRetreatCost` de Fairy Garden antes de validar el costo.
- **Engine - Eventos:** Nuevos `GameEventType`: `DECK_SEARCHED`, `CARD_DISCARDED`, `POKEMON_PLACED`, `POKEMON_SHUFFLED_INTO_DECK`.
- **Engine - Data:** Efectos parseados para xy1-115 (Cassius), xy1-116 (Evosoda), xy1-118 (Great Ball), xy1-120 (Max Revive), xy1-123 (Professor's Letter) en `xy1_parsed.json`.
- **Frontend - Card Picker Overlay:** UI estilo Hearthstone para selecciones `SEARCH_DECK`, `PLACE_ON_BENCH` y `CHOOSE_FROM_DISCARD` — backdrop blur, cartas con animación `cardReveal` escalonada (80ms por carta), selección múltiple con glow dorado, botón Confirmar / Pasar. Unificado en `myCardPickerSelection` computed.
- **Frontend - Shuffle Pokémon Overlay:** Overlay dedicado para `SHUFFLE_POKEMON_TO_DECK` (Cassius) mostrando las opciones de banca disponibles.
- **Frontend - Tool Targeting:** Nuevo modo de interacción `'tool-target'` — al seleccionar un Pokémon Tool de la mano, el Active y los slots de banca del jugador pulsan en dorado (`tool-target-pulse` keyframe 1.1s infinito, cursor crosshair). Click en destino despacha `ATTACH_TOOL` con `targetPosition`.
- **Frontend - Game Feed:** Handlers para `DECK_SEARCHED`, `POKEMON_PLACED`, `POKEMON_SHUFFLED_INTO_DECK`, `CARD_DISCARDED`. Eliminación del case `TRAINER_PLAYED` duplicado.
- **Frontend - DTOs:** `revealedCardIds` y `selectionCount` en `BackendPendingSelectionDto` y `PendingSelectionDto`; mapeados en `board-state.mapper.ts`.
- **Frontend - Clasificador:** `isTool()` en `card-classify.ts` para detectar subtipo `'Pokémon Tool'`.
- **Frontend - UI/UX:** Banner hint al activar tool-targeting con botón cancelar. Página 404 (`NotFoundPage`) con estética de auth shell.

### Fixed
- `TurnManagerTest`: firma del mock de `SelectionResolver` actualizada a 4 parámetros tras incorporar `CardLookup`.
- Case `TRAINER_PLAYED` duplicado en `game-feed.service.ts` (línea 87 vs 112).

### Changed
- `SelectionType`: añadidos `PLACE_ON_BENCH`, `CHOOSE_FROM_DISCARD`, `SHUFFLE_POKEMON_TO_DECK`.
- `TrainerEffect` `@JsonSubTypes`: registrados `SearchDeckTrainerEffect`, `ShufflePokemonIntoDeckTrainerEffect`, `RecycleTrainerEffect`.
- `AttackResolutionChain`: pasa `PassiveEffectRegistry.getInstance()` al construir `DamageApplicationHandler`.

## [0.2.0-alpha.1] - 2026-06-12

### Added
- **Engine - Habilidades (Bloque 4 tier 1):** Parseo del bloque `abilities` con conditions y subtipos anidados (`COIN_FLIP_DAMAGE`, `REDUCE_DAMAGE`, `RESTRICT_ITEMS`, `IMMUNE_TO_CONDITIONS`). Habilidades activadas vía `USE_ABILITY` (Mystical Fire). Habilidades disparadas `ON_ATTACK_RECEIVED` / `ON_ALLY_KNOCKOUT` (Spiky Shield, Destiny Burst). Habilidades continuas como guards (Fur Coat, Forest's Curse, Sweet Veil). Evaluador de condiciones (`AbilityConditionEvaluator`) con `IS_ACTIVE`, `HAS_ENERGY`, `HAS_CONDITION`.
- **Engine - Selección de jugador:** Mecanismo de selección (`pendingSelection`) + `CHOOSE_ACTIVE_ON_KO` para reemplazo post-KO.
- **Engine - Persistencia:** Cierre de huecos de persistencia en el estado del juego (Fase 5 ampliada).
- **API:** Exposición de `pendingSelection`, `status`, `winner` y `finishedReason` en el estado de la partida (Fase 6.2). Fix en `publishStateChanged` y notificación de estado `WAITING` en `joinGame`.
- **Frontend - Juego E2E:** Camino crítico de la demo — atacar, selección post-KO y fin de partida. Componente de waiting screen para estado `WAITING`. WebSocket STOMP real (`WebsocketStompService`), game board con zonas y SETUP screen. Tipo `AvailableAction`, `BackendGameActionType`, servicios de acción (api + mock), `game-state.mapper` con available actions reales.
- **Frontend - Auth y Lobby:** Conexión frontend-backend para auth, cartas, mazos, proxy y guards. Diseño completo del lobby con panel de partidas, estadísticas y navegación. Aceptación de JWT vía query param `?token=` en handshake WebSocket.
- **Frontend - UI/UX:** Detalles visuales en deck builder, lobby, board y perfil. Fix de carga de mazos y edición.
- **Docs y SDD:** Reorganización y compresión de documentación del proyecto. Cierre de Bloque 4 tier 1 en openspec. Code review integral. Documentación del engine, player-selection y deuda de facade.

### Fixed
- Partida E2E real jugable: bugs encontrados por smoke test contra el backend vivo.
- Status `WAITING` en `joinGame` y notificación `state-changed`.
- 401 en `/ws/info` causado por lazy initialization de colecciones de Player.
- JWT filter salta para catálogo público de cartas.
- Recursión JPA al guardar mazos.
- Regresión de handshake WebSocket: filtro acepta `?token=` solo en `/ws/**`.

### Changed
- Refactor de mocks a servicios reales HTTP en frontend (modo `api`).
- Actualización de estado SDD y openspec para effect-execution-trainers.

## [0.1.0-alpha.1] - 2026-06-08

### Added
- **Frontend:** Integración inicial de la interfaz visual elaborada por el equipo.
- **Testing:** Cobertura inicial y tests extensivos de la lógica del GameEngine (KnockoutProcessor, DrawPhaseState, RuleValidator, MainPhaseState, Efectos).
- **Engine Core:** Estabilización del motor del juego (Fases 4 y 5 del MVP).
- **WebSockets:** Eventos de juego en tiempo real (GameEventPublisher usando Spring ApplicationEventPublisher).
- **Lógicas de Cartas:** Stubs y handlers funcionales para ataques usando el patrón Flyweight (EffectRegistry, CoinFlipLogic, DiscardEnergyLogic, etc).
- **Validaciones:** Lógicas independientes para límites de cartas por turno (Ej. Ace Tactician y Supporter).

### Changed
- Refactorización profunda de manejo de estados e inyección de dependencias (`@Transactional` en GameService para evitar LazyInitializationException de Hibernate).

### Fixed
- Error de resolución del tipo de evento en curaciones (`HP_RESTORED` a `POKEMON_HEALED`).
- Condiciones de victoria: Derrota automática por Deck Out en la fase de robo.
- Doble premio (Prize Cards) al debilitar cartas Pokémon EX y MEGA.
