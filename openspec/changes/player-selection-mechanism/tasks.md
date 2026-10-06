# Tasks: Player Selection Mechanism

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~330–410 (prod ~180, tests ~180) |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR; fallback: PR1 = Fase 1+2 (mecanismo), PR2 = Fase 3+4 (tests/integración) si supera 400 |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Mecanismo + fix #4 + tests, todo verde | PR 1 | Cohesivo; medias mitades no son shippeables por separado (KO setearía pendingSelection sin nada que lo resuelva) |

## Phase 1: Foundation (tipos + estado)

- [x] 1.1 Crear `models/game/SelectionType.java` — enum con `CHOOSE_ACTIVE_ON_KO`.
- [x] 1.2 Crear `models/game/PendingSelection.java` — `@Data @Builder`: `SelectionType type`, `Long ownerPlayerId`, `List<String> validOptions` (cardIds banca), `String prompt`.
- [x] 1.3 `models/game/BoardState.java` — agregar campo nullable `PendingSelection pendingSelection`.
- [x] 1.4 `models/cards/ActionType.java` — agregar `RESOLVE_SELECTION` (+ actualizar javadoc enum).

## Phase 2: Core (corte + resume)

- [x] 2.1 `KnockoutProcessor.java` — extraer `promote(BenchPokemon, PlayerField)` (reusa builder líneas 100-116).
- [x] 2.2 `KnockoutProcessor.java` — banca≠∅ → setear `board.pendingSelection` (owner=ownerField.playerId, validOptions=cardIds banca) y **NO** promover; banca=∅ → no hace nada (sin auto-promover).
- [x] 2.2b `KnockoutProcessor.java` — **SRP victoria**: quitar AMBOS seteos de `FINISHED`/`winnerId`/`GAME_FINISHED` (premios agotados y banca vacía). El KO solo muta y reporta; quitar import `GameStatus`.
- [x] 2.3 `TurnManager.processAction` — guarda: si `pendingSelection != null`, solo aceptar `RESOLVE_SELECTION` (de su `ownerPlayerId`, que puede NO ser el jugador del turno); resto → `ActionResult.failure` sin mutar. La guarda va **antes** que la de turno/fase.
- [x] 2.4 `TurnManager.handleAttackFlow` — tras `attackPhase.handle`: (1) `finishedByVictory` (checker → si victoria limpiar `pendingSelection` + emitir `GAME_FINISHED` + return); (2) si `pendingSelection != null` → return eventos SIN `completeTurn`; (3) si no → `completeTurn`.
- [x] 2.5 `TurnManager.completeTurn` — tras `betweenTurnsPhase.execute`: mismo orden (`finishedByVictory`; pendiente → pausa; si no → switch+beginTurn).
- [x] 2.6 `TurnManager.resolveSelection(board, playerId, request, cardLookup)` — validar owner + `benchIndex` dentro de validOptions; inválido → failure sin mutar; válido → `promote`, clear `pendingSelection`, resume por `currentPhase` (ATTACK→`completeTurn`; BETWEEN_TURNS→switch+beginTurn).
- [x] 2.7 Rutear `RESOLVE_SELECTION` hacia `resolveSelection` (resuelto **dentro** de `processAction`; `GameEngineFacade` no requiere cambios).
- [x] 2.8 `VictoryConditionChecker` — firma `boolean`→`Optional<VictoryReason>` + nuevo enum `models/game/VictoryReason.java`; javadoc: pasa de "red de seguridad" huérfana a fuente única de decisión, invocada por `TurnManager` post-KO. `VictoryConditionCheckerTest` actualizado.
- [x] 2.9 **Refactor SRP** (antes de tests): extraer `engine/SelectionResolver.java` (valida + aplica la selección, dispatch por `SelectionType`; prepara O/C para los próximos tipos). `TurnManager.resolveSelection` queda solo con el resume. Menores: `victoryMessage`→`VictoryReason.describe(winnerId)`; extract-method en `KnockoutProcessor.processIfKnockedOut` (`discardKnockedOut`/`awardPrizes`/`requestPromotionIfPossible`).

## Phase 3: Tests

- [x] 3.1 `KnockoutProcessorTest` — **actualizado**: banca≠∅ ahora setea `pendingSelection` (`CHOOSE_ACTIVE_ON_KO`, owner, validOptions) y `activePokemon==null`; banca=∅ → `activePokemon==null` **sin** `FINISHED`; premios agotados → toma premios **sin** `FINISHED`.
- [ ] 3.1b `VictoryConditionCheckerTest` / `TurnManager*Test` — cubrir que el orquestador declara victoria post-KO (premios agotados → gana prizeTaker; banca vacía → gana oponente) y emite `GAME_FINISHED`.
- [x] 3.2 `SelectionResolverTest` (nuevo) — validación aislada de la selección: resolve OK (promueve + limpia pendiente), sin pendiente, jugador equivocado, índice fuera de rango, índice null → failure sin mutar.
- [x] 3.3 `TurnManagerTest` — guarda bloquea acción normal con pendiente; `RESOLVE_SELECTION` ruteado a `resolveSelection` (resume completeTurn en phase ATTACK); validación falla → no reanuda.
- [x] 3.4 `TurnManagerTest` — `setUp` con mocks del checker + resolver (constructor de 6 args, DIP); `endTurn_victoryInBetweenTurns` reescrito (checker declara victoria, `GAME_FINISHED` con motivo); `endTurn_runsBetweenTurns` verde con checker mockeado.
- [x] 3.5 Integración KO por ataque (`TurnManagerSelectionIntegrationTest`, fases reales): pausa para que elija el DEFENSOR (no el del turno), resolve, turno completa y cambia jugador.
- [x] 3.6 Integración KO por veneno (between-turns): pausa, resolve, reanuda switch+beginTurn.
- [x] 3.7 `AttackResolutionChainTest` — ajustado: la chain procesa el KO **sin** declarar victoria (`assertNull(winnerId)`).
- Nota baseline: la suite trae deuda **pre-existente** ajena a este cambio — `GameEngineFacadeTest` + `GameServiceTest` (UnnecessaryStubbing), por el conflicto semántico engine↔fachada (ver `NEXT_SESSION.md`).
- [x] 3.8 `GameEngineFacadeTest.useAttack_benchPromotionAfterKo` → reescrito como `useAttack_koWithBench_awaitsSelection_doesNotAutoPromote`: era un test de la **implementación vieja** (auto-promote bench[0]). Ahora verifica el contrato nuevo (KO descarta, NO auto-promueve, conserva banca). Fix de fixture: el atacante necesitaba 1 energía para que el ataque no se cancelara. **Verificado verde aislado.**

## Fase 5 — borde engine↔DTO (ampliada 2026-06-11: completar la persistencia opción B)

> 5.1/5.2 las cerró el EQUIPO en el merge `3f62a5e` (mapper con `rebuildZone` + write-back de
> energías + `playerTurnCount` + flags + `pendingSelection`/`matchState` + SETUP). Decisión de
> sesión (usuario): se mantiene la opción B del equipo — NO se reescribe a opción A. Esta fase
> ampliada cierra los huecos restantes verificados contra código.

- [x] 5.1 ✅ (equipo) `pendingSelection` dominio→DTO en `updateDbState` + campo en `GameBoardState`.
- [x] 5.2 ✅ (equipo) `pendingSelection` DTO→dominio en `toDomainState` — sobrevive entre requests.
- [x] 5.3 `finishedReason` por motivo: campo en dominio `BoardState`, seteado en
      `TurnManager.finishedByVictory` (VictoryReason.name()), `handleConcede` ("CONCEDE") y
      deck-out de `DrawPhaseState` ("DECK_OUT"); `EngineStateMapper` lo persiste tal cual.
- [x] 5.5 **Energías/tools adjuntas resolubles tras round-trip**: `MainPhaseState.handleAttachEnergy/
      handleAttachTool` guardan `card.getId()` (cardId REAL) en `AttachedCard.cardId` en vez del
      id del request (instanceId por HTTP); `handlePlayStadium` igual para `activeStadiumCardId`.
      Sin esto, atacar con energías tras persistir no puede resolver el color (lookup no escanea
      zonas adjuntas) → falla.
- [x] 5.6 Persistir `firstPlayerHasActed` (campo en `GameBoardState` + mapeo bidireccional;
      eliminar la derivación frágil `turnNumber > 1`).
- [x] 5.7 Persistir `TurnFlags.abilitiesUsedThisTurn` (campo en `PlayerBoardState` + mapeo) —
      el once-per-turn de habilidades (Bloque 4) hoy no sobrevive requests.
- [x] 5.8 `CardCacheService.findById`: guard de formato UUID → `IllegalArgumentException`
      inmediata SIN tocar el caché (hoy cada instanceId dispara un fetch HTTP real a
      pokemontcg.io por un set inexistente — BUG-7/A7 del review).
- [ ] 5.4 (sin cambios) Conflicto amplio engine↔fachada — diagnóstico en
      `docs/tracking/FACADE_INTEGRATION_DEBT.md`; los tests citados fueron borrados por el equipo.

## Fase 6 — Demo crítica (2026-06-12): partida jugable E2E sin mocks

> Objetivo: demo al cliente. Camino crítico BE acordado con el usuario. La 6.1 toca la
> capability `effect-execution` (spec vigente actualizada con el requirement corregido).

- [x] 6.1 **BUG-5/A3** (fix aplicado por el EQUIPO en 3f62a5e; esta sesión agrega el test de regresión REQ-A0 y corrige el javadoc) — `EnergyValidationHandler` NO descarta las energías del costo al validar
      (regla XY: la energía queda adjunta; `GAME_RULES.md` §Energy). La validación por pool de
      dos pasadas (específicas → Colorless) se conserva intacta. Test:
      `EnergyValidationHandlerTest` (válida y conserva / insuficiente cancela sin mutar).
- [x] 6.3 **Bugs encontrados por el smoke E2E real** (scripts/smoke-e2e.py, partida completa
      contra el BE vivo): (a) `GameAction.result` es columna JSON pero `logAction` escribia
      el string plano "SUCCESS" -> DataIntegrityViolation 409 en la PRIMERA accion de toda
      partida; ahora `{"status":"SUCCESS"}`. (b) `BoardState.setupCompletedPlayers` (campo
      del equipo) no se persistia -> partida atrapada en SETUP para siempre; agregado a
      GameBoardState + mapper con test de round-trip. (c) El 409 generico decia "mazo" en
      cualquier conflicto -> mensaje genericizado.
- [x] 6.4 Smoke E2E verificado VERDE: partida completa por HTTP (setup -> turnos -> KO ->
      seleccion -> victoria NO_POKEMON_LEFT) con energias acumulandose (REQ-A0 en vivo).
- [x] 6.2 **A5 salida** — `BoardStateDTO` expone `status` (matchState), `winnerId`,
      `finishedReason` y `pendingSelection {type, ownerPlayerId, validOptions, prompt}`;
      mapeo en `GameService.getGameState`. Sin esto el FE no puede mostrar la selección
      post-KO ni el fin de partida. Test en `GameServiceTest`.

## Phase 4: Verify + docs

- [ ] 4.1 `cd BE && ./mvnw test` → verde (130 previos + nuevos).
- [ ] 4.2 Actualizar `docs/EFFECT_IMPLEMENTATION_INVENTORY.md` §6.1/§7 (fix #4 hecho) y `EFFECT_IMPLEMENTATION_PROGRESS.md` (Bloque 3).
- [ ] 4.3 Commit convencional `feat(engine): player selection mechanism + choose active on KO` (sin atribución IA).
