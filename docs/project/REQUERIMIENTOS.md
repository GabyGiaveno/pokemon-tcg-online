# Requerimientos Funcionales y No Funcionales

Documento de seguimiento del cumplimiento de los requerimientos del TPI.

---

## RF-01 — Reglas del Juego

Define el nucleo del motor de juego. Cubre preparacion de partida, ciclo de turno, resolucion de ataques, knockout, condiciones especiales y condiciones de victoria/derrota. Todo validado y ejecutado exclusivamente en el backend.

### RF-01a) Preparacion de la partida

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-01a.1 | Ambos jugadores barajan sus mazos y roban 7 cartas iniciales | COMPLETO | `GameService.buildPlayerBoardFromDeck()` L585-618 | `Collections.shuffle()` + subList(0,7) |
| RF-01a.2 | Mulligan: si no hay basico, mostrar mano, barajar, robar 7 | PARCIAL | `GameService.buildPlayerBoardFromDeck()` L599-618 | Implementa re-shuffle y re-draw. Falta: mostrar mano al rival (no se notifica al oponente). Limite hardcodeado a 3 reintentos (deberia repetir indefinidamente hasta tener basico). |
| RF-01a.3 | Por cada mulligan, oponente puede robar 1 carta adicional | FALTANTE | — | `buildPlayerBoardFromDeck` no retorna el count de mulligans ni hay logica para dar cartas extra al oponente. Bug conocido en AGENTS.md (BUG-10). |
| RF-01a.4 | Cada jugador coloca Pokemon Activo boca abajo y hasta 5 Basicos en Banca boca abajo | COMPLETO | `SetupPhaseState.handleSetupPlacePokemon()` L57-94 | Valida que sea Basico, max 5 en banca. El estado "boca abajo" es implicito (fase SETUP). |
| RF-01a.5 | Cada jugador toma 6 cartas de su mazo como cartas de Premio boca abajo | COMPLETO | `GameService.buildPlayerBoardFromDeck()` L628-638 | Primeras 6 del mazo restante van a prizeCards. |
| RF-01a.6 | Lanzamiento de moneda para determinar quien comienza | COMPLETO | `GameService.markReady()` L175-188 | `random.nextBoolean()` cuando ambos estan ready. Ganador elige quien arranca via `chooseFirstPlayer()`. |
| RF-01a.7 | Ambos revelan sus Pokemon y comienza la partida | COMPLETO (implicito) | `SetupPhaseState.handleSetupEndTurn()` L98-137 | Transicion SETUP -> ACTIVE revela todo. No hay flag explicito `faceDown/faceUp` en el modelo. |

### Resumen RF-01a

- **Completo:** 5/7 sub-requerimientos
- **Parcial:** 1/7 (mulligan sin limite y sin notificacion al rival)
- **Faltante:** 1/7 (cartas extra al oponente por mulligan)

### Items pendientes para completar RF-01a

1. **Mulligan ilimitado:** Cambiar el limite de 3 reintentos a un loop sin limite (o con un limite alto tipo 100 para evitar loops infinitos en mazos mal armados, aunque la validacion de deck ya exige al menos 1 basico).
2. **Notificar mulligan al oponente:** Publicar evento `MULLIGAN_DECLARED` con la mano del jugador que hizo mulligan.
3. **Cartas extra por mulligan:** Despues de resolver todos los mulligans, dar al oponente la opcion de robar N cartas adicionales (donde N = cantidad de mulligans del rival). Requiere:
   - Que `buildPlayerBoardFromDeck` retorne el mulligan count.
   - Logica post-setup para ofrecer al oponente robar cartas extra.

---

## RF-01b) Estructura del turno

### Fase 1 — Robo obligatorio

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-01b.1 | El jugador roba 1 carta del mazo | COMPLETO | `DrawPhaseState.execute()` L64-66 | `deck.remove(0)` -> `hand.add()` |
| RF-01b.2 | El jugador que empieza NO roba en su primer turno | COMPLETO | `DrawPhaseState.execute()` L40-45 | Condicion: `playerTurnCount == 0 && !firstPlayerHasActed` |
| RF-01b.3 | Si el mazo esta vacio al intentar robar, ese jugador pierde | COMPLETO | `DrawPhaseState.execute()` L52-62 | Setea `FINISHED` + `DECK_OUT` + winnerId al oponente |

### Fase 2 — Acciones (cualquier orden, todas opcionales)

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-01b.4 | Colocar Pokemon Basicos en Banca | COMPLETO | `MainPhaseState.handlePlayBasicPokemon()` L74-91 | Valida basico + bench < 5 via `RuleValidator` |
| RF-01b.5 | Evolucionar Pokemon (no el turno que entro, no primer turno del juego) | COMPLETO | `MainPhaseState.handleEvolvePokemon()` L93-183, `RuleValidator.validateEvolvePokemon()` L68-92 | Chequea `enteredThisTurn` y `isGlobalFirstTurn`. Solo primer jugador del juego no puede evolucionar en su T1 (correcto). |
| RF-01b.6 | Unir 1 carta de Energia por turno a cualquier Pokemon propio | COMPLETO | `MainPhaseState.handleAttachEnergy()` L185-219, `RuleValidator.validateAttachEnergy()` L99-109 | Flag `energyAttachedThisTurn` impide mas de 1. Permite target ACTIVE o BENCH_X. |
| RF-01b.7 | Jugar cartas de Entrenador: Objetos sin limite | COMPLETO | `MainPhaseState.handlePlayItem()` L334-371 | Sin flag de limite por turno. Items se pueden jugar multiples veces. |
| RF-01b.8 | Jugar cartas de Entrenador: 1 Partidario por turno | COMPLETO | `MainPhaseState.handlePlaySupporter()` L374-413, `RuleValidator.validatePlaySupporter()` L192-209 | Flag `supporterPlayedThisTurn`. Tambien maneja Ace Tactician por separado. |
| RF-01b.9 | Jugar cartas de Entrenador: 1 Estadio por turno | FALTANTE | `MainPhaseState.handlePlayStadium()` L415-440 | **No hay flag `stadiumPlayedThisTurn`**. Se puede jugar multiples estadios en un turno. Falta validacion. |
| RF-01b.10 | Retirar el Pokemon Activo pagando costo de retirada (1 vez por turno) | COMPLETO | `MainPhaseState.handleRetreat()` L263-332, `RuleValidator.validateRetreat()` L140-172 | Flag `retreatedThisTurn`. Descarta energias, limpia condiciones, promueve de banca. Tambien valida Paralizado/Dormido no puede retirarse. |
| RF-01b.11 | Usar Habilidades de Pokemon propios | COMPLETO | `MainPhaseState.handleUseAbility()` L442-511 | Flag por habilidad `abilitiesUsedThisTurn` (1 uso por habilidad por turno). Evalua condiciones. |

### Fase 3 — Ataque

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-01b.12 | El jugador puede atacar con su Pokemon Activo | COMPLETO | `TurnManager.handleAttackFlow()` L193-220, `AttackPhaseState.handle()` L55-75 | Delega a `AttackResolutionChain` de 7 pasos. |
| RF-01b.13 | No disponible en el primer turno del jugador que empieza | COMPLETO | `RuleValidator.validateUseAttack()` L243-246 | `!board.isFirstPlayerHasActed() && playerTurnCount == 1` |
| RF-01b.14 | El ataque finaliza el turno automaticamente | COMPLETO | `TurnManager.handleAttackFlow()` L219 | Llama a `completeTurn()` que ejecuta BETWEEN_TURNS -> switch player -> beginTurn. |

### Resumen RF-01b

- **Completo:** 13/14 sub-requerimientos
- **Faltante:** 1/14 (limite de 1 Estadio por turno)

### Items pendientes para completar RF-01b

1. **Limite de 1 Estadio por turno:** Agregar flag `stadiumPlayedThisTurn` en `TurnFlags`, setearlo en `handlePlayStadium()`, y validar en `RuleValidator.validatePlayStadium()` o en el handler antes de permitir jugar un segundo estadio.

## RF-01c) Sistema de ataque — secuencia de resolucion

La resolucion de ataques se implementa como un pipeline Chain of Responsibility de 7 pasos en `AttackResolutionChain`. Cada handler corresponde a un paso del requerimiento.

| Paso RF | Descripcion | Handler | Estado | Notas |
|---------|-------------|---------|--------|-------|
| RF-01c.1 | Anunciar ataque y verificar Energia requerida | `EnergyValidationHandler` (Handler 1) | COMPLETO | Algoritmo dos pasadas: primero tipos especificos, luego Colorless con cualquier energia sobrante. Si falta energia, cancela con `INSUFFICIENT_ENERGY` y el turno NO se consume (el jugador puede seguir actuando). La energia NO se descarta al atacar (regla XY correcta). |
| RF-01c.2 | Confusion: moneda, cruz = 3 contadores de dano al atacante y turno termina | `ConfusionCheckHandler` (Handler 2) | COMPLETO | Chequea `SpecialCondition.CONFUSED`. Heads = ataque procede. Tails = 30 de autodano + `cancelAttack()`. `PostDamageHandler` tiene `alwaysRun()=true` para detectar auto-KO por confusion. |
| RF-01c.3 | Selecciones del ataque (elegir objetivo, etc.) | `SelectionsHandler` (Handler 3) | PARCIAL | El target por defecto es el Activo del oponente (correcto para la mayoria de ataques). **Falta:** ataques que apuntan a Banca del rival (ej: "Snipe Shot"). Hay un TODO documentado en el codigo. |
| RF-01c.4 | Requisitos previos del ataque (coin flips del texto de la carta, etc.) | `PreAttackHandler` (Handler 4) | COMPLETO | Usa `EffectRegistry` (patron Flyweight) para ejecutar efectos pre-dano parseados de `parsedEffects`. Soporta `AddDamageEffect` (coin flip +dano), `MultiplierDamageEffect`, descartes de energia pre-ataque. |
| RF-01c.5 | Efectos que modifican/cancelan el ataque (efectos del turno anterior del rival, etc.) | `ModifierHandler` (Handler 5) + `DamageApplicationHandler` L45-51 | PARCIAL | Implementa Muscle Band (+20 vs EX/Mega) y proteccion de dano (`damageProtected`). **Falta:** sistema general de "efectos del turno anterior del rival" que modifiquen/cancelen ataques (ej: ataques que dicen "durante el proximo turno del oponente, el dano se reduce en 20"). |
| RF-01c.6 | Calculo y aplicacion del dano | `DamageApplicationHandler` (Handler 6) + `DamageCalculator` | COMPLETO | Secuencia correcta: (1) dano base, (2) modificadores flat (tools, efectos pre-dano), (3) Debilidad x2, (4) Resistencia -20, (5) reduccion por habilidades continuas (Fur Coat), (6) reduccion por tools defensivas (Hard Charm), (7) minimo 0, (8) redondeo a multiplo de 10. Aplica dano restando HP. |
| RF-01c.7 | Efectos posteriores al dano (condiciones, descartes de energia, dano a banca, curacion) | `PostDamageHandler` (Handler 7) | COMPLETO | Ejecuta efectos post-dano via `EffectRegistry`. Resuelve habilidades triggered del defensor (`AbilityTriggerResolver`). Procesa KO del defensor Y del atacante (auto-KO por confusion). |

### Detalle del calculo de dano (RF-01c.6)

Implementado en `DamageCalculator.java`:

| Sub-paso | Descripcion | Estado | Notas |
|----------|-------------|--------|-------|
| 6a | Dano base de la carta | COMPLETO | Parseado de `AttackData.baseDamage` |
| 6b | Modificadores por Entrenadores/efectos activos sobre atacante | COMPLETO | `ModifierHandler` aplica Muscle Band. `DamageApplicationHandler` consulta `PassiveEffectRegistry` para tools del atacante. |
| 6c | Debilidad del defensor (x2) | COMPLETO | `DamageCalculator.applyWeakness()` — soporta formatos "x2" y "+N". Stadium Shadow Circle puede suprimir debilidad. |
| 6d | Resistencia del defensor (-20), minimo 0 | COMPLETO | `DamageCalculator.applyResistance()` — parsea valor del JSON "-20". Resultado final con `Math.max(0, ...)`. |
| 6e | Modificadores por efectos activos sobre defensor | COMPLETO | Habilidades continuas (`ContinuousAbilityQuery.damageReductionFor`), tools defensivas (Hard Charm via `PassiveEffectRegistry`). |
| 6f | 1 contador de dano por cada 10 puntos | COMPLETO (implicito) | El sistema trabaja directamente en puntos de HP, no contadores fisicos. `Math.round(damage / 10.0) * 10` asegura multiplos de 10. Equivalente funcional. |

### Resumen RF-01c

- **Completo:** 5/7 pasos principales
- **Parcial:** 2/7 (seleccion de objetivos en banca, efectos del turno anterior del rival)

### Items pendientes para completar RF-01c

1. **Ataques que apuntan a Banca:** Implementar en `SelectionsHandler` la lectura de `ActionRequest.targetPosition` para ataques tipo "Snipe Shot" que hagan dano a Pokemon de Banca del rival. Requiere un `AttackEffectRegistry` que identifique que ataques necesitan seleccion de banca.
2. **Efectos del turno anterior del rival:** Implementar sistema de "efectos persistentes de turno" donde un ataque puede dejar un efecto que modifica/cancela el proximo ataque del oponente (ej: "durante el proximo turno, el dano de ataques del oponente se reduce en 20"). Requiere almacenar estos efectos en el `BoardState` y consumirlos en `ModifierHandler` o `DamageApplicationHandler`.

## RF-01d) Proceso de Knockout

Implementado en `KnockoutProcessor.java` con victoria decidida por `VictoryConditionChecker.java`.

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-01d.1 | Pokemon queda KO cuando contadores de dano x10 >= HP | COMPLETO | `KnockoutProcessor.processIfKnockedOut()` L50 | Chequea `currentHp <= 0`. El sistema trabaja en HP directos (no contadores), funcionalmente equivalente. |
| RF-01d.2 | El Pokemon y todas las cartas unidas se descartan | COMPLETO | `KnockoutProcessor.discardKnockedOut()` L59-77 | Descarta Pokemon (`discardPile.add`), todas las energias adjuntas (L65-68), y la tool si tiene (L69-72). Setea `activePokemon = null`. Tambien implementado para banca en `processIfKnockedOutOnBench()` L145-176. |
| RF-01d.3 | Oponente toma 1 carta de Premio (2 si es Pokemon-EX) | COMPLETO | `KnockoutProcessor.awardPrizes()` L84-100, `prizesFor()` L103-111 | Chequea subtypes "EX" o "MEGA" para dar 2 premios. Mueve cartas de `prizeCards` a `hand`. |
| RF-01d.4 | Dueno reemplaza Activo con uno de Banca | COMPLETO | `KnockoutProcessor.requestPromotionIfPossible()` L119-132, `promote()` L186-206 | Si hay banca, crea `PendingSelection` tipo `CHOOSE_ACTIVE_ON_KO` — el juego se pausa hasta que el jugador elija. `promote()` transfiere HP, energias, tool y limpia condiciones. |
| RF-01d.5 | Si no tiene Pokemon en Banca, pierde la partida | COMPLETO | `VictoryConditionChecker.checkVictoryConditions()` L37-42, `hasNoPokemon()` L61-63 | Si `activePokemon == null && bench.isEmpty()` -> `FINISHED` con razon `NO_POKEMON_LEFT`. Tambien detecta `ALL_PRIZES_TAKEN` (L45-50). |

### Resumen RF-01d

- **Completo: 5/5 sub-requerimientos**
- **Sin items pendientes.**

Notas adicionales de calidad:
- Buena separacion de responsabilidades: `KnockoutProcessor` solo muta, `VictoryConditionChecker` solo decide victoria, `TurnManager` orquesta ambos.
- Soporta KO en banca (ataques tipo splash damage) con `processIfKnockedOutOnBench()`.
- La promocion es asincrona (pausa el juego con `PendingSelection`), lo cual permite al jugador elegir que Pokemon promover en vez de auto-promover el primero.

## RF-01e) Condiciones especiales

Implementado en `StatusEffectManager.java` (procesamiento entre turnos y aplicacion), modelo en `SpecialCondition.java` enum + flags `isBurned`/`isPoisoned` en `ActivePokemon`.

### Condiciones individuales

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-01e.1 | Dormido: no puede atacar ni retirarse. Entre turnos moneda: cara=despierta | COMPLETO | Ataque/retiro bloqueado en `RuleValidator.validateUseAttack()` L252-254 y `validateRetreat()` L155-157. Entre turnos: `StatusEffectManager.applyBetweenTurnEffects()` L80-91, coin flip heads=NONE. |
| RF-01e.2 | Quemado: entre turnos moneda, cruz=2 contadores de dano (20 HP) | COMPLETO | `StatusEffectManager.applyBetweenTurnEffects()` L64-77. Tails=20 dano, Heads=cura quemadura. |
| RF-01e.3 | Confundido: al atacar moneda, cruz=ataque falla + 3 contadores (30 HP) al atacante | COMPLETO | `ConfusionCheckHandler.handle()` L40-68. Tails=30 autodano + `cancelAttack()`. `PostDamageHandler` con `alwaysRun()` detecta auto-KO. |
| RF-01e.4 | Paralizado: no puede atacar ni retirarse. Se cura automaticamente entre turnos | COMPLETO | Ataque/retiro bloqueado en `RuleValidator` L248-250 y L151-153. Cura automatica: `StatusEffectManager` L95-100. |
| RF-01e.5 | Envenenado: entre turnos 1 contador de dano (10 HP) sin moneda | COMPLETO | `StatusEffectManager.applyBetweenTurnEffects()` L56-61. 10 dano, condicion persiste. |

### Gestion de incompatibilidades

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-01e.6 | Dormido, Confundido y Paralizado son mutuamente excluyentes (la mas reciente reemplaza) | COMPLETO | `StatusEffectManager.applyCondition()` L114-121. ASLEEP/CONFUSED/PARALYZED usan `pokemon.setCondition()` (un solo campo enum, la nueva reemplaza la anterior). |
| RF-01e.7 | Quemado y Envenenado son independientes, coexisten entre si y con las otras tres | COMPLETO | Modelo: `isBurned` y `isPoisoned` son booleans independientes del campo `condition` (enum). `applyCondition()` L117-118 usa setters separados. Un Pokemon puede estar BURNED + POISONED + PARALYZED simultaneamente. |
| RF-01e.8 | Orden entre turnos: (1) Envenenado, (2) Quemado, (3) Dormido, (4) Paralizado | COMPLETO | `StatusEffectManager.applyBetweenTurnEffects()` L56→L64→L80→L95. Orden exacto del requerimiento. |
| RF-01e.9 | Condiciones se eliminan al retirarse a Banca o evolucionar | COMPLETO | Retiro: `MainPhaseState.handleRetreat()` L299-306 construye `BenchPokemon` sin campos de condition (se pierden implicitamente). Evolucion: `MainPhaseState.handleEvolvePokemon()` L161-163 setea `condition(NONE), isBurned(false), isPoisoned(false)`. Existe `clearAllConditions()` L126-130 como utilidad. |

### Resumen RF-01e

- **Completo: 9/9 sub-requerimientos**
- **Sin items pendientes.**

Nota de calidad: el modelo hibrido (enum para exclusivas + booleans para independientes) es una solucion elegante al problema de incompatibilidades. El orden de procesamiento entre turnos esta hardcodeado en el orden correcto.

## RF-01f) Condiciones de victoria y derrota

Implementado en `VictoryConditionChecker.java`, `DrawPhaseState.java`, y `TurnManager.java`.

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-01f.1 | Victoria por Premios: jugador toma su ultima carta de Premio | COMPLETO | `VictoryConditionChecker.checkVictoryConditions()` L45-50 | Chequea `prizeCards.isEmpty()` para ambos jugadores. Setea `FINISHED` + `ALL_PRIZES_TAKEN`. |
| RF-01f.2 | Victoria por knockout total: oponente sin Pokemon en campo | COMPLETO | `VictoryConditionChecker.checkVictoryConditions()` L37-42, `hasNoPokemon()` L61-63 | Chequea `activePokemon == null && bench.isEmpty()`. Setea `FINISHED` + `NO_POKEMON_LEFT`. |
| RF-01f.3 | Derrota por mazo vacio: al intentar robar, mazo vacio | COMPLETO | `DrawPhaseState.execute()` L52-62 | Chequea `deck.isEmpty()` al inicio del turno. Setea `FINISHED` + `DECK_OUT` + winnerId al oponente. |
| RF-01f.4 | Muerte Subita: si ambos cumplen condicion de victoria simultaneamente, nueva partida con 1 Premio cada uno | COMPLETO | `VictoryConditionChecker.checkVictoryConditions()`, `GameService.startSuddenDeath()`, `BoardStateDTO.suddenDeathRound` | La ronda especial se persiste como `status = SETUP` + `suddenDeathRound > 0`; REST expone `suddenDeathRound` para reconstruir reload/reconnect sin depender del WebSocket. |

### Resumen RF-01f

- **Completo:** 4/4 sub-requerimientos
- **Faltante:** 0/4

### Nota RF-01f.4

Muerte Subita no queda como fase persistida jugable. `SUDDEN_DEATH` puede existir como estado conceptual/transicional, pero la preparacion real de cada mini-partida queda en `SETUP` y se identifica por `suddenDeathRound > 0`.

---

## RF-02 — Tipos de cartas

### RF-02a) Pokemon

Cartas con HP, ataques, debilidad, resistencia y costo de retirada. Pueden ser Basico, Fase 1 o Fase 2. Restricciones de evolucion.

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-02a.1 | Modelar cartas Pokemon con HP, ataques, debilidad, resistencia, costo de retirada | COMPLETO | `Card.java` campos: `hp` (Integer), `attacks` (JSONB), `weaknesses` (JSONB), `resistances` (JSONB), `retreatCost` (List<String>), `types` (List<String>), `evolvesFrom` (String). | Todos los campos del requerimiento presentes. |
| RF-02a.2 | Reconocer subtipos: Basico, Fase 1, Fase 2 | COMPLETO | `CardTypeResolver.resolve()` L32-38 | Resuelve por `subtypes`: "Stage 2" -> `STAGE2`, "Stage 1" -> `STAGE1`, default -> `BASIC_POKEMON`. Tambien reconoce Pokemon-EX (sufijo "-EX") y Mega (prefijo "M "). |
| RF-02a.3 | Fase 1 y Fase 2 son cartas de evolucion | COMPLETO | `RuleValidator.validateEvolvePokemon()` L72-76 | Solo permite evolucionar con cartas tipo `STAGE1`, `STAGE2`, o `MEGA_POKEMON`. Valida linaje via `evolvesFrom` (L77-81). |
| RF-02a.4 | No se puede evolucionar en el primer turno en que el Pokemon entra en juego | COMPLETO | `RuleValidator.validateEvolvePokemon()` L88-91 | Chequea `targetEnteredThisTurn` — flag seteado en `true` por `PokemonFactory` al crear, reseteado a `false` por `TurnManager.resetTurnFlags()` al inicio del siguiente turno. |
| RF-02a.5 | No se puede evolucionar en el primer turno del jugador | COMPLETO | `RuleValidator.validateEvolvePokemon()` L84-87 | Chequea `isGlobalFirstTurn && playerTurnCount <= 1`. Solo el primer jugador del juego no puede evolucionar en su T1 (el segundo jugador si puede — correcto segun reglas TCG). |

### Resumen RF-02a

- **Completo: 5/5 sub-requerimientos**
- **Sin items pendientes.**

### RF-02b) Pokemon-EX

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-02b.1 | Pokemon-EX es un Pokemon Basico (puede colocarse directamente) | COMPLETO | `RuleValidator.validatePlayBasicPokemon()` L43 | Acepta tanto `BASIC_POKEMON` como `POKEMON_EX` para colocar en banca/activo. |
| RF-02b.2 | Mayor HP y ataques mas poderosos | COMPLETO (implicito) | `Card.java` campo `hp` + `attacks` | Los valores vienen de la API pokemontcg.io. El motor no necesita logica especial — los HP y dano de ataque mas altos son inherentes a los datos de la carta. |
| RF-02b.3 | Cuando un Pokemon-EX queda KO, oponente toma 2 Premios en vez de 1 | COMPLETO | `KnockoutProcessor.prizesFor()` L103-111 | Chequea `subtypes` por "EX" o "MEGA" — si match, retorna 2 premios. |
| RF-02b.4 | El sufijo -EX forma parte del nombre del Pokemon | COMPLETO | `CardTypeResolver.resolve()` L29-30 | Detecta `card.getName().endsWith("-EX")` para clasificar como `POKEMON_EX`. El nombre con sufijo viene directamente de pokemontcg.io (ej: "Xerneas-EX"). |

### Resumen RF-02b

- **Completo: 4/4 sub-requerimientos**
- **Sin items pendientes.**

### RF-02c) Energia Basica

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-02c.1 | Sin limite de copias en el mazo | COMPLETO | `CopyLimitValidator.java` L25, L41-45 | Filtra `isBasicEnergy()` antes de contar copias. Energia basica excluida del limite de 4. |
| RF-02c.2 | Se puede unir 1 por turno a cualquier Pokemon en juego | COMPLETO | `MainPhaseState.handleAttachEnergy()` + `RuleValidator.validateAttachEnergy()` L99-109 | Flag `energyAttachedThisTurn` impide mas de 1. Permite target ACTIVE o BENCH_X. |

### Resumen RF-02c: Completo 2/2

### RF-02d) Energia Especial

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-02d.1 | Maximo 4 copias en el mazo | COMPLETO | `CopyLimitValidator.java` L28-29 | No es energia basica -> aplica limite MAX_COPIES=4. |
| RF-02d.2 | Efectos adicionales segun texto de la carta | PARCIAL | `CardTypeResolver.resolve()` L43 clasifica como `SPECIAL_ENERGY` | Se reconoce el tipo pero no se encontro implementacion de efectos especificos de energias especiales (ej: Double Colorless Energy provee 2 energia). El sistema las trata como energia normal de 1 tipo. |

### Resumen RF-02d: Parcial 1.5/2

### RF-02e) Entrenador — Objeto (Item)

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-02e.1 | Se puede jugar ilimitadas veces por turno | COMPLETO | `MainPhaseState.handlePlayItem()` L334-371 | Sin flag de limite por turno. Se pueden jugar multiples items. |
| RF-02e.2 | Efecto inmediato y luego se descarta | COMPLETO | `MainPhaseState.handlePlayItem()` L357-360 | Ejecuta efecto via `TrainerResolutionChain`, luego `hand.remove()` + `discardPile.add()`. |

### Resumen RF-02e: Completo 2/2

### RF-02f) Entrenador — AS TACTICO (Ace Tactician)

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-02f.1 | Subtipo especial de Objeto de alto impacto | COMPLETO | `CardTypeResolver.resolve()` L20-22 | Chequea `card.isAceTactician()` primero (prioridad sobre otros tipos). Flag viene de API: subtypes contiene "ACE SPEC". |
| RF-02f.2 | Solo 1 carta AS TACTICO en todo el mazo | COMPLETO | `AceTacticianValidator.java` L20-28 | Cuenta cartas con `isAceTactician()`, rechaza si > 1. |
| RF-02f.3 | Maximo 1 AS TACTICO por turno (en juego) | COMPLETO | `RuleValidator.validatePlaySupporter()` L195-199 + `TurnFlags.aceTacticianPlayedThisTurn` | Flag separado del supporter normal. |

### Resumen RF-02f: Completo 3/3

### RF-02g) Entrenador — Partidario (Supporter)

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-02g.1 | Solo 1 por turno | COMPLETO | `RuleValidator.validatePlaySupporter()` L200-203 + `TurnFlags.supporterPlayedThisTurn` | Flag `supporterPlayedThisTurn`. |
| RF-02g.2 | Efecto inmediato y luego se descarta | COMPLETO | `MainPhaseState.handlePlaySupporter()` L392-396 | `TrainerResolutionChain.resolve()`, luego `hand.remove()` + `discardPile.add()`. |

### Resumen RF-02g: Completo 2/2

### RF-02h) Entrenador — Estadio (Stadium)

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-02h.1 | Solo 1 por turno | FALTANTE | `MainPhaseState.handlePlayStadium()` L415-440 | **No hay flag `stadiumPlayedThisTurn`** en `TurnFlags`. Se puede jugar multiples estadios en un turno. (Mismo gap reportado en RF-01b.9). |
| RF-02h.2 | Permanece en juego (zona compartida) | COMPLETO | `BoardState.activeStadiumCardId` | El estadio se guarda a nivel de board (no de jugador). Persiste entre turnos. |
| RF-02h.3 | Reemplaza al Estadio anterior si lo hubiera | COMPLETO | `MainPhaseState.handlePlayStadium()` L424-425 | Si hay estadio activo, lo descarta antes de poner el nuevo. |

### Resumen RF-02h: Parcial 2/3

### RF-02i) Entrenador — Herramienta Pokemon (Pokemon Tool)

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-02i.1 | Se une a un Pokemon (maximo 1 por Pokemon) | COMPLETO | `RuleValidator.validateAttachTool()` L116-126 | Chequea `targetAlreadyHasTool` — rechaza si ya tiene. |
| RF-02i.2 | Permanece hasta que el Pokemon sea descartado | COMPLETO | `KnockoutProcessor.discardKnockedOut()` L69-72 | Al KO, descarta tool junto con el Pokemon. Tool persiste mientras Pokemon vive (campo `tool` en ActivePokemon/BenchPokemon). |

### Resumen RF-02i: Completo 2/2

### RF-02j) (Opcional) Pokemon Megaevolucion

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-02j.1 | Evolucion de un Pokemon-EX | COMPLETO | `CardTypeResolver.resolve()` L27-28 detecta prefijo "M ". `RuleValidator.validateEvolvePokemon()` L73 acepta `MEGA_POKEMON` como carta de evolucion. | Clasificacion y validacion de linaje OK. |
| RF-02j.2 | Al megaevolucionar, el turno termina inmediatamente | FALTANTE | — | No hay logica en `MainPhaseState.handleEvolvePokemon()` que detecte evolucion a Mega y fuerce fin de turno. El jugador puede seguir actuando despues de megaevolucionar. |
| RF-02j.3 | Al quedar KO, oponente toma 2 Premios | COMPLETO | `KnockoutProcessor.prizesFor()` L107 | Chequea subtype "MEGA" -> retorna 2 premios. |

### Resumen RF-02j: Parcial 2/3 (item faltante: turno termina al megaevolucionar)

---

### Resumen consolidado RF-02

| Seccion | Completo | Total | % |
|---------|----------|-------|---|
| RF-02a) Pokemon | 5 | 5 | 100% |
| RF-02b) Pokemon-EX | 4 | 4 | 100% |
| RF-02c) Energia Basica | 2 | 2 | 100% |
| RF-02d) Energia Especial | 1.5 | 2 | 75% |
| RF-02e) Objeto | 2 | 2 | 100% |
| RF-02f) AS TACTICO | 3 | 3 | 100% |
| RF-02g) Partidario | 2 | 2 | 100% |
| RF-02h) Estadio | 2 | 3 | 67% |
| RF-02i) Herramienta Pokemon | 2 | 2 | 100% |
| RF-02j) Megaevolucion (opcional) | 2 | 3 | 67% |
| **TOTAL RF-02** | **25.5** | **28** | **91%** |

### Items pendientes RF-02

1. **Energia Especial — efectos:** Implementar logica para que energias especiales provean efectos (ej: Double Colorless = 2 energia Colorless). Requiere parseo del texto de la carta o registro de efectos por cardId.
2. **Estadio — limite 1 por turno:** Agregar flag `stadiumPlayedThisTurn` en `TurnFlags` y validar antes de permitir jugar un segundo estadio.
3. **(Opcional) Mega — fin de turno:** En `handleEvolvePokemon()`, detectar si la carta de evolucion es `MEGA_POKEMON` y, si lo es, forzar fin de turno automatico (o retornar un flag que `TurnManager` intercepte para llamar `completeTurn()`).

## RF-03 — Gestion del juego

Ciclo de vida completo de una partida: creacion, matchmaking, gestion de turnos, deteccion de finalizacion. Backend como fuente de verdad.

### RF-03a) Ciclo completo de una partida

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-03a.1 | Creacion de partida | COMPLETO | `GameService.createGame()` L75-103 | Valida jugador, deck valido, crea `GameSession` con status `WAITING`, persiste via `GameSessionRepository`. |
| RF-03a.2 | Matchmaking entre dos jugadores | COMPLETO | `GameService.joinGame()` L105-151 + `getWaitingGames()` | Player 2 busca partidas en `WAITING`, se une con su deck. Valida que no sea el mismo jugador, deck valido. Transicion a `READY_CHECK`. Publica evento WebSocket. |
| RF-03a.3 | Gestion de turnos y su orden | COMPLETO | `TurnManager` completo (L44-356) | Ciclo DRAW->MAIN->ATTACK->BETWEEN_TURNS automatizado. `switchActivePlayer()` alterna turno. Guard de ownership (L169): solo el jugador activo puede actuar. `resetTurnFlags()` limpia flags al inicio de cada turno. |
| RF-03a.4 | Deteccion de la condicion de finalizacion | COMPLETO | `VictoryConditionChecker` + `DrawPhaseState` + `TurnManager.handleConcede()` | 4 condiciones: `ALL_PRIZES_TAKEN`, `NO_POKEMON_LEFT`, `DECK_OUT`, `CONCEDE`. Al detectar: `GameStatus.FINISHED`, `winnerId`, `finishedReason`, `finishedAt`. Actualiza stats de jugador (wins/losses/XP). |

### RF-03b) Estados del juego

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-03b.1 | WAITING — esperando segundo jugador | COMPLETO | `GameStatus.WAITING` + `GameService.createGame()` L97 | Estado inicial al crear partida. `getWaitingGames()` lista partidas en este estado. |
| RF-03b.2 | SETUP — preparacion del tablero (mulligan, colocacion, premios) | COMPLETO | `GameStatus.SETUP` + `SetupPhaseState` + `GameService.buildPlayerBoardFromDeck()` | Mulligan, 7 cartas, 6 premios, colocacion de activo y banca. Nota: existe un estado intermedio extra `READY_CHECK` (coin flip + confirmacion) entre WAITING y SETUP. |
| RF-03b.3 | ACTIVE — partida en curso con ciclo DRAW->MAIN->ATTACK->BETWEEN_TURNS | COMPLETO | `GameStatus.ACTIVE` + `TurnManager` + fases `DrawPhaseState`, `MainPhaseState`, `AttackPhaseState`, `BetweenTurnsState` | Ciclo completo automatizado con alternancia de turnos. |
| RF-03b.4 | FINISHED — partida finalizada con ganador | COMPLETO | `GameStatus.FINISHED` + `VictoryConditionChecker` | Registra `winnerId`, `finishedReason`, `finishedAt`. Actualiza `PlayerStats` (wins/losses/streak). |

Implementado en `GameStatus.java` enum: `WAITING, READY_CHECK, SETUP, ACTIVE, FINISHED` (5 estados — incluye `READY_CHECK` como extra no exigido por el requerimiento).

### RF-03c) Backend como fuente de verdad

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-03c.1 | Backend es fuente de verdad exclusiva del estado | COMPLETO | `GameService.performGameAction()` L342-431 | Toda accion pasa por validacion en backend (`RuleValidator`). Frontend envia via REST, backend valida, muta, persiste, notifica. En fallo: no muta ni publica. |
| RF-03c.2 | Frontend es unicamente capa de presentacion | COMPLETO | Arquitectura REST + WebSocket | Frontend envia acciones via `POST /api/games/{id}/actions`. Recibe estado filtrado via `GET /api/games/{id}/state`. No puede mutar estado directamente. `toOpponentFieldDTO()` oculta mano rival y premios. |

### RF-03d) Log completo de eventos y acciones

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-03d.1 | Log de todas las acciones (turno, jugador, accion, resultado) | COMPLETO | `GameAction` entity + `GameService.logAction()` | Registra: `actionType`, `payload` (JSON request), `result` (JSON response), turno, jugador. Inmutable — cada accion se registra independientemente de exito/fallo. |
| RF-03d.2 | Utilizable para auditoria y revision | COMPLETO | `GameActionRepository` | Historial completo consultable por `gameSessionId`. Cada entrada tiene datos suficientes para reconstruir la secuencia de juego. |
| RF-03d.3 | Utilizable para reconexion | PARCIAL | `GameState.stateJson` permite reconstruir estado actual | El estado completo se persiste tras cada accion, permitiendo que un jugador desconectado se reconecte y obtenga el estado via `GET /api/games/{id}/state`. **Falta:** no hay logica explicita de deteccion de desconexion, timeout, ni flujo de reconexion automatica (ej: re-suscripcion WebSocket). El jugador puede refetchear estado, pero no se notifica al oponente de la desconexion. |

### RF-03e) Persistencia automatica del estado

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-03e.1 | Persistencia automatica tras cada accion relevante | COMPLETO | `GameService.performGameAction()` dentro de `@Transactional` | Tras cada accion exitosa: serializa `GameBoardState` -> JSON, escribe a `GameState.stateJson`, `gameStateRepository.save()`. ACID garantizado por transaccion. |
| RF-03e.2 | Suficiente para reconstruir la partida ante desconexion | COMPLETO | `GameState.stateJson` (JSONB) contiene estado completo | Incluye: campo completo de ambos jugadores (mano, deck, banca, activo, premios, descarte), fase actual, turno, jugador activo, condiciones especiales, selecciones pendientes. |

### RF-03f) Datos de cartas desde cache local

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-03f.1 | Cartas se obtienen del cache local (no API externa durante partida) | COMPLETO | `CardCacheService.findById()` + `CardLookup` interface | El engine usa `CardLookup` (implementado por `CardCacheService`) que busca en cache en memoria / BD local. `PokemonTCGApiService` solo se invoca durante sincronizacion de sets (fuera de partida). El motor nunca llama a pokemontcg.io durante juego. |

### RF-03g) Opcionales

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-03g.1 | (Opcional) Chat entre jugadores durante partida | FALTANTE | — | No hay endpoint ni logica de chat. `GameWebSocketController` solo tiene `/game/ping`. No hay modelo de mensaje ni topic de chat. |
| RF-03g.2 | (Opcional) Ranking o historial de partidas por jugador | PARCIAL | `PlayerStats` entity (wins, losses, streak) + `PlayerService.getProfile()` | Stats basicas existen: wins, losses, streak, tournamentsWon. Se actualizan al finalizar partida. `ProfileResponse` expone stats. **Falta:** no hay endpoint de ranking/leaderboard (comparacion entre jugadores). No hay endpoint de historial de partidas (lista de partidas jugadas con resultados). |

### Resumen RF-03 actualizado

| Seccion | Completo | Total | % |
|---------|----------|-------|---|
| RF-03a) Ciclo completo | 4 | 4 | 100% |
| RF-03b) Estados del juego | 4 | 4 | 100% |
| RF-03c) Backend fuente de verdad | 2 | 2 | 100% |
| RF-03d) Log de eventos y acciones | 2.5 | 3 | 83% |
| RF-03e) Persistencia automatica | 2 | 2 | 100% |
| RF-03f) Cache local de cartas | 1 | 1 | 100% |
| RF-03g) Opcionales | 0.5 | 2 | 25% |
| **TOTAL RF-03 (sin opcionales)** | **15.5** | **16** | **97%** |
| **TOTAL RF-03 (con opcionales)** | **16** | **18** | **89%** |

### Items pendientes RF-03

1. **Reconexion explicita:** El estado se persiste correctamente, pero falta un flujo de reconexion: detectar desconexion WebSocket, notificar al oponente, y re-suscribir al jugador automaticamente al reconectarse.
2. **(Opcional) Chat:** No implementado. Requiere modelo de mensaje, topic WebSocket `/topic/games/{id}/chat`, y endpoint para enviar mensajes.
3. **(Opcional) Ranking/Historial:** Stats basicas existen (`PlayerStats`), pero falta endpoint de leaderboard (top jugadores) y endpoint de historial (partidas jugadas por jugador con resultados).

## RF-04 — Construccion de mazos (Deck Builder)

### RF-04a) Restriccion de expansiones

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-04a.1 | Set base obligatorio: XY (xy1, 146 cartas) | COMPLETO | Backend: `CardCacheService` warmup carga xy1 en `@PostConstruct`. `PokemonTCGApiService` filtra por `set.id:xy1`. Frontend: `deck-builder-page.ts` L407 hardcodea `set: 'xy1'`. Badge en UI: "Set base: XY". |
| RF-04a.2 | (Opcional) Expansiones adicionales con puntaje extra | NO IMPLEMENTADO | — | Solo soporta xy1. No hay selector de sets adicionales. |

### RF-04b) Deck Builder — busqueda e integracion API

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-04b.1 | Buscar cartas de pokemontcg.io v2 filtrando por xy1 | COMPLETO | `PokemonTCGApiService.fetchSet()` L64-68 con RestClient. Filtro `set.id:xy1`. Timeouts: 10s connect, 60s read. | Sync asincrono via `POST /api/cards/sync?set=xy1`. |
| RF-04b.2 | Agregar cartas al mazo | COMPLETO | Frontend: `deck-builder-page.ts` `onAddCard()` L251-264, `onIncreaseQuantity()` L266-275 | Agrega nueva carta o incrementa cantidad. |
| RF-04b.3 | Gestionar mazos guardados del jugador | COMPLETO | `DeckService` CRUD completo + `DeckController`. Frontend: `deck-list-page.ts` lista/elimina/valida mazos. `deck-builder-page.ts` crea/edita. | Ownership enforced: solo ves tus mazos. |

### RF-04c) Validacion del mazo

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-04c.1 | Exactamente 60 cartas | COMPLETO | `ExactSizeValidator` — error `WRONG_TOTAL` | Mensaje en español: "El mazo debe tener exactamente 60 cartas..." |
| RF-04c.2 | Maximo 4 copias del mismo nombre (excepto Energia Basica) | COMPLETO | `CopyLimitValidator` L24-26 — error `TOO_MANY_COPIES` | Filtra `isBasicEnergy()` antes de contar. Energia Basica ilimitada. |
| RF-04c.3 | Maximo 1 carta AS TACTICO en todo el mazo | COMPLETO | `AceTacticianValidator` L20-24 — error `TOO_MANY_ACE_TACTICIAN` | Cuenta por flag `isAceTactician`. |
| RF-04c.4 | Al menos 1 Pokemon Basico | COMPLETO | `BasicPokemonValidator` — error `NO_BASIC_POKEMON` | Chequea supertype "Pokemon" + subtype "Basic". |

Pipeline: `DeckService.validateDeck()` L57-61 ejecuta todos los validators via stream/flatMap.

### RF-04d) Persistencia de mazos

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-04d.1 | Crear mazos | COMPLETO | `DeckService.createDeck()` L91-96 | Valida, persiste, incrementa `PlayerStats.decksCreated`. |
| RF-04d.2 | Editar mazos | COMPLETO | `DeckService.updateDeck()` L152-175 | Reemplaza nombre y cartas via `replaceDeckCards()`. Re-valida. |
| RF-04d.3 | Eliminar mazos | COMPLETO | `DeckService.deleteDeck()` L181-187 | Ownership enforced. Cascade delete via `orphanRemoval=true`. |
| RF-04d.4 | Listar mazos por jugador | COMPLETO | `DeckService.getDecksByPlayer()` L67-74 | DISTINCT LEFT JOIN FETCH para eager loading de cartas. |

Entidades: `Deck` (id, name, player FK, isValid, createdAt) + `DeckCard` (deck FK, cardId, quantity).

### RF-04e) Interfaz visual del Deck Builder

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-04e.1 | Contador de cartas (0/60) | COMPLETO | `deck-builder-page.html` L114-116: `{{ totalCards() }}/60`. `deck-validation-panel.html` L10-18: barra de progreso con clases CSS `count--empty`, `count--valid`, `count--exact`, `count--over`. |
| RF-04e.2 | Limites de copias por carta | COMPLETO | `deck-current-list.html` L31-32: muestra cantidad por carta. Validacion server-side con `TOO_MANY_COPIES` se muestra en panel de errores. |
| RF-04e.3 | Errores de validacion con mensajes descriptivos y accionables | COMPLETO | `deck-validation-panel.html` L34-51: muestra estado (valido/invalido), lista de errores con `error-code` + `error-message` + `error-card-id`. Todos los mensajes en español. Estado pendiente: "Guarda el mazo para validarlo". |

Layout de 4 columnas: filtros, cartas disponibles, mazo actual, info/acciones.

### Resumen RF-04

| Seccion | Completo | Total | % |
|---------|----------|-------|---|
| RF-04a) Restriccion expansiones | 1 | 1 | 100% |
| RF-04b) Busqueda e integracion API | 3 | 3 | 100% |
| RF-04c) Validacion del mazo | 4 | 4 | 100% |
| RF-04d) Persistencia de mazos | 4 | 4 | 100% |
| RF-04e) Interfaz visual | 3 | 3 | 100% |
| **TOTAL RF-04 (sin opcional)** | **15** | **15** | **100%** |

- **Sin items pendientes (obligatorios).**
- Opcional no implementado: expansiones adicionales.

## RF-05 — Guardado y persistencia del estado

### RF-05a) Estado persistido despues de cada accion

El estado se persiste como JSONB en `GameState.stateJson` (entity `GameState.java`), serializado desde `GameBoardState` + `PlayerBoardState` + `PokemonInPlayState`.

| Dato requerido | Estado | Campo en el modelo | Notas |
|---------------|--------|-------------------|-------|
| Pokemon Activo de cada jugador | COMPLETO | `PlayerBoardState.activePokemon` (PokemonInPlayState) | Incluye cardId, instanceId, name, currentHp, maxHp. |
| Banca de cada jugador | COMPLETO | `PlayerBoardState.bench` (List<PokemonInPlayState>) | Hasta 5 Pokemon con mismos campos que activo. |
| Cartas unidas (energias) | COMPLETO | `PokemonInPlayState.attachedEnergies` (List<CardInstanceState>) | Cada energia con instanceId y cardId. |
| Cartas unidas (tools) | COMPLETO | `PokemonInPlayState.attachedTools` (List<CardInstanceState>) | Herramientas Pokemon adjuntas. |
| Manos de ambos jugadores | COMPLETO | `PlayerBoardState.hand` (List<CardInstanceState>) | Cada carta con instanceId unico. |
| Mazos (orden incluido) | COMPLETO | `PlayerBoardState.deck` (List<CardInstanceState>) | Lista ordenada — el orden del array es el orden del mazo. |
| Pilas de descarte | COMPLETO | `PlayerBoardState.discardPile` (List<CardInstanceState>) | Historial de descartes. |
| Cartas de Premio | COMPLETO | `PlayerBoardState.prizeCards` (List<CardInstanceState>) | 6 iniciales, decrementan al tomar premios. |
| Contadores de dano | COMPLETO | `PokemonInPlayState.currentHp` + `maxHp` | Dano = maxHp - currentHp. Persistido por Pokemon. |
| Condiciones especiales activas | COMPLETO | `PokemonInPlayState.condition` (String: "ASLEEP"/"CONFUSED"/"PARALYZED"/"NONE") + `burned` (bool) + `poisoned` (bool) | Modelo hibrido: enum exclusivo + booleans independientes. |
| Fase actual del turno | COMPLETO | `GameBoardState.phase` (TurnPhase enum) + `GameState.turnPhase` (denormalizado) | DRAW, MAIN, ATTACK, BETWEEN_TURNS. |
| Flag: ya unio Energia | COMPLETO | `PlayerBoardState.hasAttachedEnergyThisTurn` | Boolean persistido. |
| Flag: ya retiro | COMPLETO | `PlayerBoardState.retreatedThisTurn` | Boolean persistido. |
| Flag: ya jugo Partidario | COMPLETO | `PlayerBoardState.hasPlayedSupporterThisTurn` | Boolean persistido. |
| Flag: ya jugo AS TACTICO | COMPLETO | `PlayerBoardState.hasPlayedAceTacticianThisTurn` | Boolean persistido. |
| Flag: ya ataco | COMPLETO | `PlayerBoardState.attackedThisTurn` | Boolean persistido. |
| Flag: habilidades usadas | COMPLETO | `PlayerBoardState.abilitiesUsedThisTurn` (Set<String>) | Clave: "posicion#nombreHabilidad". |
| Flag: entro este turno | COMPLETO | `PokemonInPlayState.enteredThisTurn` | Por Pokemon, para restriccion de evolucion. |
| Turno actual | COMPLETO | `GameBoardState.turnNumber` + `GameState.turnNumber` (denormalizado) | Entero incremental. |
| Jugador activo | COMPLETO | `GameBoardState.currentPlayerId` | Long. |
| Estadio activo | COMPLETO | `GameBoardState.activeStadiumCardId` | String nullable (zona compartida). |
| Seleccion pendiente | COMPLETO | `GameBoardState.pendingSelection` (PendingSelection) | Tipo, owner, opciones validas, prompt. |
| Estado de partida | COMPLETO | `GameBoardState.matchState` (GameStatus) | WAITING/READY_CHECK/SETUP/ACTIVE/FINISHED. |
| Ganador y razon | COMPLETO | `GameBoardState.winnerPlayerId` + `finishedReason` | Setados al terminar. |

### RF-05b) Reconstruccion completa de la partida

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-05b.1 | Estado suficiente para reconstruir sin perdida de informacion | COMPLETO | `GameState.stateJson` (JSONB completo) | Todos los campos listados arriba se serializan/deserializan via Jackson. El engine reconstruye `BoardState` desde `GameBoardState` via `EngineStateMapper`. Round-trip lossless: estado -> JSON -> estado. |
| RF-05b.2 | Persistencia tras cada accion relevante | COMPLETO | `GameService.performGameAction()` dentro de `@Transactional` | Tras cada accion exitosa: `gameStateRepository.save()` con JSON actualizado + campos denormalizados (`turnNumber`, `turnPhase`, `updatedAt`). |

### RF-05c) Registro de acciones (log)

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-05c.1 | Log completo e inmutable | COMPLETO | `GameAction` entity — todos los campos `updatable = false` | JPA enforcea inmutabilidad: `turnNumber`, `playerId`, `actionType`, `payload`, `result`, `timestamp` — ninguno actualizable post-insert. |
| RF-05c.2 | Cada entrada indica turno | COMPLETO | `GameAction.turnNumber` (int, not null) | Numero de turno al momento de la accion. |
| RF-05c.3 | Cada entrada indica jugador | COMPLETO | `GameAction.playerId` (Long, not null) | ID del jugador que ejecuto la accion. |
| RF-05c.4 | Cada entrada indica tipo de accion | COMPLETO | `GameAction.actionType` (ActionType enum, STRING) | Enum persistido como string (ej: "USE_ATTACK", "PLAY_BASIC_POKEMON", "CONCEDE"). |
| RF-05c.5 | Cada entrada indica resultado | COMPLETO | `GameAction.result` (JSON, not null) | JSON con exito/fallo, eventos producidos, errores si aplica. |
| RF-05c.6 | Fuente para auditoria y revision | COMPLETO | `GameActionRepository` consultable por `gameSessionId` | Historial completo ordenable por timestamp/turno. |
| RF-05c.7 | Fuente para reconexion | COMPLETO (a nivel de datos) | `GameState.stateJson` para estado actual + `GameAction` para historial | Estado actual reconstruible desde JSONB. Historial de acciones disponible para replay. Nota: no hay flujo automatico de reconexion en el frontend, pero los datos estan. |

### Resumen RF-05

| Seccion | Completo | Total | % |
|---------|----------|-------|---|
| RF-05a) Estado persistido | 22 | 22 | 100% |
| RF-05b) Reconstruccion completa | 2 | 2 | 100% |
| RF-05c) Registro de acciones | 7 | 7 | 100% |
| **TOTAL RF-05** | **31** | **31** | **100%** |

- **Sin items pendientes.**

Nota de calidad: el modelo de persistencia es exhaustivo. Cada campo relevante del estado de juego se serializa a JSONB, incluyendo flags de turno granulares y condiciones especiales con su modelo hibrido. El log de acciones es verdaderamente inmutable (JPA `updatable = false` en todos los campos). La unica observacion menor es que la reconexion automatica del frontend no esta implementada, pero los datos para soportarla estan completos en el backend.

## RF-06 — Comunicacion en tiempo real

### RF-06a) WebSockets bidireccionales

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-06a.1 | Comunicacion bidireccional en tiempo real via WebSockets | COMPLETO | Backend: `WebSocketConfig.java` — endpoint `/ws`, STOMP sobre SockJS, broker `/topic`, app prefix `/app`. Frontend: `websocket-stomp.service.ts` — STOMP client con SockJS transport. | Autenticacion JWT en STOMP CONNECT (header `Authorization: Bearer` o query param `?token=`). |
| RF-06a.2 | Investigacion e implementacion propia del equipo | COMPLETO | — | Implementacion completa con autenticacion, autorizacion de subscripciones, DTOs especificos, y mock service para testing. |

### RF-06b) Sincronizacion de estado

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-06b.1 | Sincronizar estado entre ambos jugadores despues de cada accion valida | COMPLETO | Backend: `GameEventPublisher.publishStateChanged()` -> `/topic/games/{id}/state-changed`. Frontend: `board-container.ts` L351-356 — al recibir `state-changed`, ejecuta `loadGameState()` via REST. | Patron: WS notifica, REST refetch. Garantiza que el frontend siempre tiene estado autorizado y filtrado. |
| RF-06b.2 | Enviar actualizaciones a ambos clientes | COMPLETO | `GameEventPublisher.publishGameEvents()` + `publishStateChanged()` — publicacion a topic del game, ambos clientes suscritos reciben. `JwtChannelInterceptor` L104-134 valida que solo participantes se suscriban. | Autorizacion: verifica player es player1 o player2 del game en SUBSCRIBE. |

### RF-06c) Notificacion de eventos relevantes

| Evento requerido | Estado | GameEventType | Notas |
|-----------------|--------|---------------|-------|
| Inicio de turno | COMPLETO | `PHASE_CHANGED` | Publicado al inicio de cada MAIN phase con playerId. |
| Knockout | COMPLETO | `POKEMON_KNOCKED_OUT` | Con cardId y playerId del KO'd. Toast en frontend. |
| Toma de carta de Premio | COMPLETO | `PRIZE_TAKEN` | Con playerId, prizesTaken, prizesLeft. Toast en frontend. |
| Condiciones especiales aplicadas | COMPLETO | `STATUS_EFFECT_APPLIED` / `STATUS_EFFECT_CLEARED` | Con cardId y condicion. |
| Fin de partida | COMPLETO | `GAME_FINISHED` / `PLAYER_CONCEDED` / `DECK_OUT` | Con winnerId y reason. Toast en frontend. |

35 tipos de evento en total en `GameEventType.java`. Frontend: `GameFeedService` convierte a feed entries + toasts transientes (max 3, auto-fade 4s).

### RF-06d) Manejo de reconexiones

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-06d.1 | Reconectarse tras desconexion | PARCIAL | Frontend: `websocket-stomp.service.ts` L165-186 — max 5 intentos con backoff exponencial (`1000 * attempt` ms). Status: `disconnected` -> `reconnecting` -> `connected` o `error`. | Reconexion WS automatica con reintentos. |
| RF-06d.2 | Recibir estado actualizado al reconectarse | COMPLETO | `board-container.ts` L346-349 — al recibir evento `system/reconnected`, ejecuta `loadGameState()` para refetch completo. | Estado completo disponible via `GET /api/games/{id}/state` en cualquier momento. |
| RF-06d.3 | Deteccion de desconexion del oponente | FALTANTE | — | No hay notificacion al oponente cuando un jugador se desconecta. No hay timeout por inactividad ni heartbeat de presencia. |

### RF-06e) Backend como fuente de verdad

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-06e.1 | Backend es unica fuente de verdad | COMPLETO | Acciones via REST `POST /api/games/{id}/actions` -> validacion backend -> persistencia -> WS notify. Frontend nunca muta estado localmente. | Ya evaluado en RF-03c, confirmado en la capa WS. |
| RF-06e.2 | Frontend solo presenta lo que el servidor indica | COMPLETO | `state-changed` -> `loadGameState()` -> render. Eventos WS no incluyen datos privados del oponente. `toOpponentFieldDTO()` oculta mano/premios. | Informacion filtrada por jugador. Fallback: REST poll si WS no disponible (100ms delay post-accion). |

### Resumen RF-06

| Seccion | Completo | Total | % |
|---------|----------|-------|---|
| RF-06a) WebSockets bidireccionales | 2 | 2 | 100% |
| RF-06b) Sincronizacion de estado | 2 | 2 | 100% |
| RF-06c) Notificacion de eventos | 5 | 5 | 100% |
| RF-06d) Manejo de reconexiones | 2 | 3 | 67% |
| RF-06e) Backend fuente de verdad | 2 | 2 | 100% |
| **TOTAL RF-06** | **13** | **14** | **93%** |

### Items pendientes RF-06

1. **Deteccion de desconexion del oponente (RF-06d.3):** Implementar heartbeat/presencia para detectar cuando un jugador se desconecta y notificar al oponente (ej: "Tu oponente se desconecto, esperando reconexion..."). Podria usar STOMP heartbeat o un sistema de ping/timeout custom.

## RF-07 — Interfaz de usuario

### RF-07a) Lobby

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-07a.1 | Crear partidas nuevas | COMPLETO | `lobby.html` L153 — boton "Crear partida" con seleccion de deck previa (step 1: elegir deck, step 2: crear). |
| RF-07a.2 | Unirse a partidas disponibles | COMPLETO | `lobby.html` L156-168 — lista de partidas en WAITING con boton "Unirse" por fila. `joinGame(game.gameId)` handler. |

### RF-07b) Pantalla de juego — tablero interactivo

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-07b.1 | Zona del oponente (Activo, Banca, mazo, Premios, descarte) | COMPLETO | `board-container.html` L303-311: `<app-player-zone perspective="opponent">`. `player-zone.html`: activo (L16-54), banca 5 slots (L56-88), premios 6 slots (L4-13), deck/descarte (L92-106). Zona rotada 180°. |
| RF-07b.2 | Zona del jugador (idem) | COMPLETO | `board-container.html` L330-348: `<app-player-zone perspective="player">`. Mismos componentes, orientacion normal. |
| RF-07b.3 | Zona compartida para Estadio activo | PARCIAL | `board-container.css` L208-238 — CSS layout `.stadium-zone` existe. **No se renderiza en el template HTML** — el estadio no es visible en la UI. |
| RF-07b.4 | Mano del jugador | COMPLETO | `player-zone.html` L161-185 — cartas en abanico (fanned display). |

### RF-07c) Drag & drop

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-07c.1 | Colocar Pokemon Basicos en Banca | COMPLETO | `card-drag.directive.ts` + `drag-drop.service.ts` — tipo 'basic', drop zones con `data-drop="BENCH_n"`. Ghost card sigue el puntero. |
| RF-07c.2 | Unir Energias a un Pokemon especifico | COMPLETO | Tipo 'energy', drop en ACTIVE o BENCH_n. |
| RF-07c.3 | Equipar Herramientas | COMPLETO | Tipo 'trainer' (tools son subtipo de trainer), drop en target. |
| RF-07c.4 | Jugar cartas de Entrenador con seleccion de target | COMPLETO | Tipo 'trainer', dispatch en `board-container.ts` L592-625 segun clasificacion de carta. |

### RF-07d) Visualizacion en tiempo real

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-07d.1 | HP actual vs HP maximo | COMPLETO | `card.html` L43-52 — texto "current/max" + barra de vida con porcentaje. `card.css` L193-229 — colores: verde (100%), amarillo (60-30%), rojo (<30%). |
| RF-07d.2 | Contadores de dano | COMPLETO | `player-zone.html` L47-53 — numeros de dano flotantes (`-X`) animados sobre Pokemon activo. |
| RF-07d.3 | Energias unidas | COMPLETO | `card.html` L40 — badge con icono rayo + cantidad. |
| RF-07d.4 | Herramienta equipada | FALTANTE | `board-state.dto.ts` L19 tiene `toolCard: CardInstanceDto` en el modelo, pero **no hay visualizacion** en el componente card (sin icono/badge de tool). |
| RF-07d.5 | Condicion especial activa — rotacion para Dormido/Confundido/Paralizado | PARCIAL | `card.html` L59-65 — badges de colores (ZZZ purpura, PAR amarillo, CFZ rosa, BRN naranja, PSN verde). **No hay rotacion de carta** para Dormido/Confundido/Paralizado — solo badges textuales. |
| RF-07d.6 | Condicion especial — marcador para Quemado/Envenenado | COMPLETO | `card.html` L59-65 — badges BRN (naranja) y PSN (verde) como marcadores visuales. |
| RF-07d.7 | Cartas de Premio restantes de cada jugador | COMPLETO | `player-zone.html` L4-13 — 6 slots face-down + count badge. `prizeCount()` computed signal. |
| RF-07d.8 | Cantidad de cartas en mano del oponente (sin revelar) | COMPLETO | `player-zone.html` L163-169 — cartas face-down en abanico + count badge. Sin datos revelados. |

### RF-07e) Panel de acciones

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-07e.1 | Botones: Evolucionar, Unir Energia, Jugar Entrenador, Retirar, Atacar, Finalizar Turno | COMPLETO | `action-panel.html` + `player-zone.html` L109-159 — 7 botones: Atacar, Retirar, Energia, Entrenador, Evolucionar, Fin de Turno, Conceder. |
| RF-07e.2 | Habilitados/deshabilitados segun fase y acciones ya realizadas | COMPLETO | `action-availability.util.ts` (120 lineas) — computa disponibilidad por fase (SETUP/DRAW/MAIN/ATTACK/BETWEEN_TURNS). Razones de deshabilitacion en español como tooltips. |

### RF-07f) Log de acciones

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-07f.1 | Log visible durante la partida con historial cronologico | COMPLETO | `log-ribbon/` componente. `game-feed.service.ts` — 15+ tipos de evento, buffer de 500 entradas, nuevos al inicio. Categorias: combat, game, card, status, system. |

### RF-07g) Notificaciones visuales

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-07g.1 | Inicio de turno | COMPLETO | `TURN_STARTED` -> toast (game-feed.service.ts L91-94). |
| RF-07g.2 | Ataques resueltos | COMPLETO | `ATTACK_DECLARED` -> feed entry (L69-71). |
| RF-07g.3 | Knockouts | COMPLETO | `POKEMON_KNOCKED_OUT` -> toast (L78-79). |
| RF-07g.4 | Toma de cartas de Premio | COMPLETO | `PRIZE_TAKEN` -> toast (L80-84). |
| RF-07g.5 | Condiciones especiales aplicadas | COMPLETO | `STATUS_EFFECT_APPLIED` / `CLEARED` -> feed (L87-90). |
| RF-07g.6 | Fin de partida | COMPLETO | Pantalla de fin integrada en `board-container.html` L83-93. |

### RF-07h) Habilidades de Pokemon (Abilities)

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-07h.1 | Visualizar habilidades desde el tablero | FALTANTE | `card-inspection-overlay.ts` L14-17 tiene `InspectionAbility` interface y input (L75), pero **no se renderiza en el HTML** del overlay. Backend no envia abilities en `CardDetailDto`. |
| RF-07h.2 | Permitir el uso de habilidades desde el tablero | FALTANTE | Backend tiene `USE_ABILITY` action en `MainPhaseState` (completo), pero **no hay boton ni interaccion en el frontend** para activar habilidades. |

### RF-07i) Animaciones (Opcional)

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RF-07i.1 | Animacion de ataques | COMPLETO | `board-animation.service.ts` — lunge atacante (600ms), hit defensor (800ms), floating damage. |
| RF-07i.2 | Animacion de knockouts | COMPLETO | `board-animation.service.ts` — fade + flash blanco full-screen (1000ms). |
| RF-07i.3 | Animacion de coin flip | COMPLETO | `coin-flip/` — spin 3D CSS (1900ms). |
| RF-07i.4 | Animacion de evoluciones | NO IMPLEMENTADO | Carta aparece instantaneamente sin transicion. |

### Resumen RF-07

| Seccion | Completo | Total | % |
|---------|----------|-------|---|
| RF-07a) Lobby | 2 | 2 | 100% |
| RF-07b) Tablero interactivo | 3.5 | 4 | 88% |
| RF-07c) Drag & drop | 4 | 4 | 100% |
| RF-07d) Visualizacion tiempo real | 6.5 | 8 | 81% |
| RF-07e) Panel de acciones | 2 | 2 | 100% |
| RF-07f) Log de acciones | 1 | 1 | 100% |
| RF-07g) Notificaciones | 6 | 6 | 100% |
| RF-07h) Habilidades UI | 0 | 2 | 0% |
| RF-07i) Animaciones (opcional) | 3 | 4 | 75% |
| **TOTAL RF-07 (sin opcionales)** | **25** | **29** | **86%** |
| **TOTAL RF-07 (con opcionales)** | **28** | **33** | **85%** |

### Items pendientes RF-07

1. **Zona de Estadio visible (RF-07b.3):** El CSS layout existe pero no se renderiza en el template. Agregar componente de estadio en `board-container.html`.
2. **Herramienta equipada visible (RF-07d.4):** El dato llega en el DTO pero no se muestra en la carta. Agregar icono/badge de tool en `card.html`.
3. **Rotacion de carta por condicion (RF-07d.5):** Implementar rotacion CSS (90° para Dormido, giro para Confundido, etc.) ademas de los badges existentes.
4. **Habilidades en UI (RF-07h):** Conectar el backend (que ya tiene `USE_ABILITY` completo) con el frontend. Requiere: (a) enviar abilities en `CardDetailDto`, (b) renderizar en el inspection overlay, (c) agregar boton/interaccion para activar desde el tablero.

---

# Requerimientos No Funcionales

## RNF-01 — Rendimiento y optimizacion

| Sub-req | Descripcion | Estado | Donde se implementa | Notas |
|---------|-------------|--------|---------------------|-------|
| RNF-01.1 | Acciones de juego con tiempo de respuesta promedio < 200ms | CUMPLE (por diseno) | `GameService.performGameAction()` — logica in-memory via `TurnManager`, sin llamadas externas durante partida. Cache Caffeine para card lookups. `@Transactional` para persistencia atomica. | El engine opera sobre objetos en memoria (deserializa JSON -> muta -> serializa). No hay I/O externo durante acciones. La unica latencia es BD local (read/write JSONB). No hay medicion explicita (no hay metricas/timers configurados), pero la arquitectura lo garantiza por diseno. |
| RNF-01.2 | Busqueda de cartas en API o cache local < 500ms | CUMPLE (por diseno) | `CardCacheService` con Caffeine `LoadingCache` (TTL 24h, refresh asincrono). `@PostConstruct warmCache()` precarga xy1 al startup. Busqueda: filtrado in-memory sobre lista cacheada. | Primera carga depende de pokemontcg.io (~1-5s), pero warmup ocurre al arrancar. Busquedas posteriores son filtrado in-memory (<10ms). Si API falla, retorna cache stale (no bloquea). |
| RNF-01.3 | Frontend renderiza tablero con tiempos de respuesta acordes | CUMPLE (por diseno) | Angular signals + `ChangeDetectionStrategy.OnPush` en multiples componentes. Pattern: WS notify -> REST refetch -> signal update -> render. | `OnPush` en: `app.ts`, `reset-password`, `waiting-screen`, `register`, `deck-list-page`, y otros. Signals para estado reactivo (`signal()`, `computed()`). Debounce 300ms en busqueda de cartas. |
| RNF-01.4 | Optimizar carga de imagenes de cartas | PARCIAL | Imagenes: `imageUrlSmall` (PNG de pokemontcg.io, ~100-200KB). `loading="lazy"` en `deck-list-page.html` L103 y `lobby.html` L125. | **Implementado:** lazy loading nativo en deck list y lobby. Usa `imageUrlSmall` (thumbnails) en lugar de `imageUrlLarge` donde corresponde. **Falta:** no hay uso de `ngOptimizedImage`, no hay conversion a WebP, no hay `srcset` para responsive, no hay CDN propio ni proxy de imagenes. Las imagenes vienen directamente de pokemontcg.io (tercero). |

### Resumen RNF-01

| Completo | Total | % |
|----------|-------|---|
| 3.5 | 4 | 88% |

### Observaciones RNF-01

- No hay **metricas de rendimiento** configuradas (no Micrometer, no Spring Actuator metrics, no timers custom). El cumplimiento de los tiempos se infiere por la arquitectura (in-memory + cache) pero no se mide explicitamente.
- La optimizacion de imagenes es basica (lazy loading + thumbnails). Para un juego de cartas donde se cargan muchas imagenes simultaneamente, se podria mejorar con `ngOptimizedImage`, sprites, o cache local de imagenes.

<!-- Agregar RNF-02 en adelante a medida que se revisen -->
