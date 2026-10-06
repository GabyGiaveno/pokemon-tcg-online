# TODO Completo — Terminar Pokémon TCG Digital en 1 semana

> Generado por el code review integral del 2026-06-11
> (`docs/review/CODE_REVIEW_2026-06-11.md` — leer PRIMERO para el porqué de cada ítem).
> Formato: `[prioridad] tarea — archivos involucrados — criterio de aceptación`.
> P0 = sin esto no hay juego. P1 = entrega digna. P2 = puntos extra / pulido.

---

## BLOQUE A — Backend jugable E2E (días 1-2) 🔴

### A1. [P0] Decidir y arreglar la persistencia del estado (el bug madre)
**Problema:** `EngineStateMapper.syncCardList` solo borra; energías/descarte sin write-back;
`playerTurnCount` siempre 0; `pendingSelection` y `finishedReason` no se persisten.
**Opción recomendada (A):** persistir el `BoardState` de dominio como JSON en `GameState.stateJson`
y matar la reconciliación bidireccional:
- [ ] Decidir id canónico de las listas del dominio: **instanceId** (recomendado — distingue copias).
      El engine ya es agnóstico: compara strings (`hand.contains(cardId)` funciona con cualquier id).
- [ ] Agregar al `BoardState`/`PlayerField` lo que falte para render: mapa `instanceId→cardId`
      (o `CardRef {instanceId, cardId}` en las listas).
- [ ] `GameService`: serializar/deserializar `BoardState` directo (Jackson). Eliminar
      `updateDbState`/`toDomainState` o reducirlos a adaptadores de borde.
- [ ] Reescribir `toPlayerFieldDTO`/`toOpponentFieldDTO` leyendo del dominio.
- [ ] Migrar `AttachedCard.cardId` de energías para guardar instanceId + cardId real.
**Opción B (si no quieren tocar `GameBoardState`):** completar el mapper — agregar instancias
nuevas en hand/prizes/discard buscando por instanceId en las demás zonas, write-back de
`attachedEnergies`/tools en active+bench, sync de `discardPile`, persistir `playerTurnCount`,
`retreatedThisTurn`, `attackedThisTurn`, `firstPlayerHasActed`, `pendingSelection`, `finishedReason`.
**Aceptación:** test de integración que roba 3 turnos seguidos y verifica mano/deck/descarte
consistentes tras cada round-trip de persistencia; adjuntar energía 2 turnos y atacar.

### A2. [P0] Implementar la fase de SETUP
- [ ] `joinGame`: `WAITING → SETUP` (no ACTIVE). `initializeGame` con `phase=null` o `SETUP`.
- [ ] Handler para `SETUP_PLACE_POKEMON` (elige básico de mano → activo la primera vez, banca
      después) + acción de confirmación (`SETUP_READY` nuevo o reuso de `END_TURN` en SETUP).
- [ ] Cuando ambos confirman: `SETUP → ACTIVE`, premios ya repartidos, `TurnManager.beginTurn`
      para el primer turno de P1 (hoy nadie lo llama → P1 nunca roba).
- [ ] `VictoryConditionChecker` NO debe correr hasta status ACTIVE (o hasta que ambos confirmaron).
- [ ] Mulligan correcto: si la mano no tiene básico → revelar, reshuffle, redraw, rival puede
      robar 1 extra (GAME_RULES.md §Mulligan). Eliminar el "use whatever we have".
**Aceptación:** partida nueva NO termina en el primer END_TURN; ambos jugadores arrancan con
activo seteado por ellos; P1 roba al inicio de su turno (o no, según decisión de specs A6).

### A3. [P0] No descartar energía al atacar
- [ ] `EnergyValidationHandler`: borrar `ctx.getAttackerPokemon().setAttachedEnergies(remainingEnergies)`
      (la validación por pool queda igual).
- [ ] Ajustar los tests que asuman el descarte.
**Aceptación:** atacar dos turnos seguidos con las mismas 2 energías funciona.

### A4. [P0] Restaurar Fix B del WebSocket — ✅ RESUELTO 2026-06-11
- [x] Portado a `Produccion`: el filtro acepta `?token=` **solo en `/ws/**`** (más restrictivo
      que RamiroBranch, que lo aceptaba en toda la API — tokens en query params terminan en
      logs), con fallback al header `Bearer`. `shouldNotFilter` y `/error` permitAll intactos.
      `WebSocketHandshakeAuthIntegrationTest` VERDE; suite completa 193 verde (de paso se
      estabilizó el flake documentado de `GameServiceTest.joinGame_setsStatusActive` con
      `lenient()`).
- [x] [P1] Registrar `JwtChannelInterceptor` en `WebSocketConfig`: valida JWT en `CONNECT`
      STOMP y valida `SUBSCRIBE` contra los participantes del `gameId`. Test focalizado:
      `JwtChannelInterceptorTest` 14/14 OK.

### A5. [P0] Cerrar la Fase 5 de player-selection (borde DTO)
- [ ] Persistir `pendingSelection` (viene gratis con A1 opción A).
- [ ] Exponerla en `BoardStateDTO` (`pendingSelection: {type, ownerPlayerId, validOptions, prompt}`).
- [ ] `finishedReason` real (mapear `VictoryReason` + CONCEDE + DECK_OUT) en vez de "ENGINE_FINISHED";
      exponer `winner` en `GameSessionResponse`/`BoardStateDTO`.
**Aceptación:** KO con banca por HTTP → `GET /state` muestra la selección → `RESOLVE_SELECTION`
del defensor la resuelve → el turno continúa. (El test de integración del engine ya existe;
agregar uno por capa web.)

### A6. [P1] Resolver el conflicto de specs del primer turno
GAME_RULES.md dice "siempre se roba"; ENGINE_SPEC §1.2 y `DrawPhaseState` saltean el robo de P1.
El rulebook XY real: P1 roba pero no ataca. → Elegir, documentar en GAME_RULES.md, alinear
`DrawPhaseState` + `RuleValidator.validateUseAttack` + tests. Con A1 arreglado,
`playerTurnCount` vuelve a ser confiable y el bloqueo de ataque del primer turno funciona.

### A7. [P1] Errores 500 evitables en GameService
- [ ] Participant-check null-safe (`player2 == null` → 403, no NPE) en `getGameState` y
      `performGameAction`.
- [ ] `CONCEDE`/acciones sobre partida WAITING → fail limpio ("Game has not started").
- [ ] `CardCacheService.findById`: no tratar UUIDs como cardIds (regex simple) — evita el
      fetch HTTP basura a pokemontcg.io por cada instanceId (y resolver instanceId→cardId
      ANTES del lookup si A1-A elimina el problema de raíz).

---

## BLOQUE B — Frontend de juego (días 3-5) 🔴 — ACTUALIZADO post-merge 785aba1

> El equipo ya construyó la base con arquitectura `*_SOURCE` mock/http conmutada por
> `APP_DATA_MODE` (`app.config.ts`). **HOY todo corre en `'mock'`** (incluido el login).
> El trabajo cambió de "construir desde cero" a "conmutar a `api` y cerrar huecos".
> Ver detalle en CODE_REVIEW §8.2.

### B1. [P0] WebSocketService real — ✅ CERRADO para realtime MVP
- [x] ~~Agregar deps~~ — `@stomp/stompjs` + `sockjs-client` YA están en package.json (nadie los importa).
- [x] Implementar `WebsocketStompService` que cumple `WebsocketSource` y se usa en modo API.
- [x] Conectar a `/ws` con `?token=` y header STOMP `Authorization: Bearer <JWT>`.
- [x] Auto-reconexión con estado `reconnecting`; `reconnectAttempts` se resetea solo en conexión exitosa.
- [x] Emitir `system/reconnected` y recargar `GameStateService.loadGameState(gameId)`.
- [x] Suscribir `/topic/games/{id}/events` y `/topic/games/{id}/state-changed`.
- [x] Documentar que `/topic/game/{id}` singular es legacy y no forma parte del contrato realtime oficial.

### B2. [P0] Conmutar a API real + arreglar contrato de acciones
- [ ] **Bug bloqueante** `game-actions-api.service.ts`: `'attack'→'ATTACK'` pero el backend
      espera **`USE_ATTACK`**; faltan `PLAY_BASIC_POKEMON`, `PLAY_ITEM`, `ATTACH_TOOL`,
      `CONCEDE`, `RESOLVE_SELECTION`; `'playTrainer'` mapea todo a `PLAY_SUPPORTER`.
      Alinear 1:1 con `models/cards/ActionType` del BE (+ payload `cardInstanceId`,
      `targetPosition`, `attackIndex`, `benchIndex`).
- [ ] `APP_DATA_MODE` → `'api'` (o por environment); URLs hardcodeadas
      `http://localhost:8080` en lobby-http/game-state-api/game-actions-api/profile-http →
      centralizar en token/environment y usar `proxy.conf.js`.
- [ ] `authGuard` en `/lobby`, `/perfil` (y decidir `/pokedex`); redirect post-login → `/lobby`.
- [ ] Mover `LobbyPage` de `features/auth/lobby` a `features/lobby` y borrar los stubs muertos
      (`features/lobby` viejo, `shared/card-display`, wrapper `websocket.service.ts`).

### B3. [P0] Cerrar los huecos del tablero (la base ya existe: board 82 líneas + mapper real)
- [ ] **Pantalla de SETUP** (depende de A2): elegir activo/banca y confirmar — los mocks ya
      contemplan `phase: 'SETUP'`, falta la UI real contra el contrato nuevo.
- [ ] **Modal de selección pendiente** (depende de A5): cuando
      `pendingSelection.ownerPlayerId == yo`, elegir banca → `RESOLVE_SELECTION {benchIndex}`.
      Hoy NO hay rastro de RESOLVE_SELECTION en el FE.
- [ ] Mostrar `GameActionApiResponse.success=false` con su mensaje (errores de reglas vienen
      200 con fail, no como HTTP error).
- [ ] `card-display`/arte: el FE resuelve arte con `card-art.ts` local; conectar imágenes
      reales por cardId vía `/api/cards/{id}` con caché (recordar `CardInstanceDTO.name` null).

### B4. [P1] Pokédex: conmutar a API real
- [x] ~~Componentes según spec §13~~ — YA construidos (filter-bar, card-grid, card-preview,
      pagination, mappers, domain) pero contra `mock-pokedex-api.service`.
- [ ] Implementar `pokedex-http.service` contra `GET /api/cards` (+detalle) y conmutar por modo.
- [ ] Verificar parseo JSON-string de attacks/weaknesses/resistances (BR-09) con datos reales.

### B5. [P2] Profile (`/perfil`) — feature fuera de spec (scope creep)
- [ ] Decidir si entra en la semana. Si entra: el BE no tiene endpoint de perfil — definir
      contrato mínimo o dejarla en mock y excluirla de la demo.

---

## BLOQUE C — Reglas finas y efectos (día 6) 🟡

- [ ] **C1 [P1]** Ticks de condiciones para AMBOS activos en between-turns (hoy solo el jugador
      que terminó: veneno a media frecuencia). `BetweenTurnsState`.
- [ ] **C2 [P1]** Orden de daño: modificadores del atacante ANTES de debilidad
      (`DamageApplicationHandler`/`DamageCalculator`); Muscle Band por id de carta y para todos
      los Pokémon (sacar el hardcode por nombre y la restricción EX/MEGA de `ModifierHandler`).
- [ ] **C3 [P1]** Trainers sin efecto implementado: rechazar la acción con error claro en vez de
      consumir la carta en silencio (`MainPhaseState.handlePlayItem/Supporter` + registry check).
- [ ] **C4 [P2]** Los 4 stubs de ataque con selección (`SearchDeck`, `SwitchPokemon`,
      `LookAtDeck`, `DamageToBench`) usando el plan aprobado
      `docs/engine/RESUMABLE_ATTACK_EFFECTS_PLAN.md` (Fase A: `AttackResumeState` + paused +
      `resumeFrom`). Nota: estos stubs NO declaran `isPreDamage`/`isPostDamage` → registrados
      pero nunca invocados. Si no entra en la semana: declarar el recorte en el README de entrega.
- [x] **C5-a ✅ (2026-06-11)** HABILIDADES tier 1 — 6/12 ejecutan en 3 familias (activadas/
      disparadas/continuas). Ver `ENGINE_GUIDE.md` §28 y openspec `effect-execution-trainers`
      (continuación Bloque 4). Suite 217 verde.
- [ ] **C5-b [P2]** Habilidades tier 2 (Water Shuriken, Stance Change ×2, Fairy Transfer,
      Drive Off, Upside-Down Evolution — requieren selección; Drive Off primero, reutiliza
      `pendingSelection`), `PreventDamage`/`Restrict` (estado de efecto persistente entre
      turnos) y tools/stadiums data-driven.
- [ ] **C6 [P2]** Stadium: descarte al dueño correcto al reemplazar; evolución: trackear stack
      para descartar todas las etapas al KO; bench identity por instanceId en vez de
      posición+cardId (se resuelve con A1-A).

---

## BLOQUE D — Hardening y entrega (día 7) 🟢

- [ ] **D1 [P1]** Premios propios ocultos (conteo o instancias sin cardId) en `toPlayerFieldDTO`.
- [ ] **D2 [P1]** Test de integración E2E backend: crear→unir→setup→3 turnos→KO→selección→victoria
      (este test habría detectado TODOS los bugs del bloque A).
- [ ] **D3 [P1]** CORS restringido a `http://localhost:4200`; revisar `allowedHeaders`.
- [ ] **D4 [P2]** `toDomainEvents`: loggear tipos de evento desconocidos en vez de mapear a
      PHASE_CHANGED silencioso.
- [ ] **D5 [P2]** `requestRecovery` async (anti-enumeración por timing).
- [ ] **D6 [P2]** Tests FE de servicios (auth, deck-builder, websocket) — hoy hay 1 spec total.
- [ ] **D7 [P2]** Docs: crear o des-referenciar `docs/SDD.md` (README y TEAM2 lo citan);
      actualizar ADR 006 (CR-1/CR-4 ya resueltos); pom.xml name/description.
- [ ] **D8 [P2]** Unificar topics WS (`/topic/games/...` plural) y borrar el `@EventListener`
      legacy o alinearlo.

---

## Dependencias (qué bloquea a qué)

```
A1 (persistencia) ──► A2 (setup) ──► B3/B4 (tablero/acciones)
A1 ──► A5 (pendingSelection DTO) ──► B3 (modal selección)
A1 ──► A6 (primer turno) y C-todo (reglas finas confiables)
A4 (fix WS) ──► B1 (websocket FE) ──► B3/B4
A3 (energía) ──► demo jugable
B1+B2 ──► B3 ──► B4
```

## Definition of Done de la semana

1. `./mvnw test` 100% verde (incluido el WS test) + test E2E nuevo (D2).
2. Partida completa jugable desde dos browsers: login → mazo → lobby → setup → turnos con
   robo/energía/evolución/ataque/trainer → KO con selección → victoria con motivo real.
3. Pokédex navegable según spec (criterios AC-01..AC-20 al menos los P0).
4. Recorte de alcance explícito y documentado para lo que no entró (C4/C5).
