# Bugs encontrados durante testing de gameplay

Bugs detectados jugando una partida completa vía peticiones HTTP directas al backend (sin frontend).  
Partida: `testbot` vs `rivalbot`, Game ID `82a222ee-6709-42f4-84f8-f1a2bdd81759`.

---

## Bug 1: `RESOLVE_SELECTION` — campo `benchIndex` no documentado en el contrato público

**Área:** Backend — `ActionRequest.java` / `PendingSelectionDTO.java`

**Descripción:**  
Cuando el juego queda en estado `pendingSelection` (por ejemplo, el jugador cuyo Pokemon fue KO'd debe elegir cuál del banco promover como activo), el endpoint espera el campo `benchIndex` en el body del request. Sin embargo, ese campo **no está documentado en ningún DTO de respuesta** y la única pista sobre su existencia está en un comentario interno de `ActionType.java`. El frontend no tiene forma de saber que debe enviar `benchIndex` sin leer el código fuente del backend.

**Impacto:**  
El frontend no puede implementar la resolución de selección correctamente sin acceder al código interno. Los `validOptions` sí se devuelven en `PendingSelectionDTO`, pero no se indica cómo mapearlos al índice que hay que enviar.

**Cómo solucionarlo:**  
Agregar `benchIndex` explícitamente en `PendingSelectionDTO` como campo de instrucción, o crear un DTO de request dedicado para `RESOLVE_SELECTION` que documente qué campos acepta. Idealmente, la respuesta del estado debería indicar claramente: "para resolver esta selección, enviá `{type: RESOLVE_SELECTION, benchIndex: N}`".

---

## Bug 2: Estado devuelve instancias duplicadas / mezcladas en `hand`

**Área:** Backend — `GameStateMapper.java` (o quien construye el DTO de estado)

**Descripción:**  
Al consultar `GET /api/games/{id}/state`, la lista `hand` del jugador incluye `instanceId`s de cartas que ya fueron jugadas — energías que ya están adjuntadas a un Pokemon, trainers ya descartados. Esas instancias deberían estar en `attachedEnergies` o en `discardPile`, pero aparecen también en `hand`.

**Impacto:**  
El frontend mostraría cartas fantasma en la mano del jugador. Si el frontend usa esa lista para habilitar acciones (adjuntar energía, jugar trainer), podría intentar jugar cartas que ya no existen en la mano real, generando errores o comportamientos inesperados.

**Cómo solucionarlo:**  
Revisar el método que construye el campo `hand` en el mapper de estado. Debe usar la lista `PlayerField.hand` del `BoardState` en memoria (la fuente de verdad del motor), no una lista reconstruida desde el historial de eventos o desde la base de datos. La lista en `BoardState` se actualiza correctamente cuando se juegan cartas; el mapper debe leerla directamente.

---

## Bug 3: "Take Down" no aplica recoil damage al atacante

**Área:** Backend — `AttackResolutionChain` / handlers de efectos de ataque

**Descripción:**  
Tauros (`xy1-100`) usó el ataque "Take Down" (que en el TCG real hace 30 de daño al defensor + 10 de daño de recoil al propio Tauros), pero Tauros no recibió ningún daño. Solo se aplicó el daño al defensor. El efecto de recoil fue ignorado completamente.

**Impacto:**  
Pokemon con ataques de recoil son significativamente más fuertes de lo que deberían ser, rompiendo el balance del juego. Ataques que tienen un costo de autolesión se vuelven gratuitos.

**Cómo solucionarlo:**  
Verificar el `parsedEffects` de la carta Take Down — debería existir un efecto de tipo `SELF_DAMAGE` (o similar) con valor `10`. Luego revisar si `AttackResolutionChain` tiene un handler que procese ese tipo de efecto. Si el handler no existe, hay que implementarlo. Si el efecto no está en los datos de la carta, revisar el proceso de parseo/importación de cartas desde la PokeAPI.

---

## Bug 4: `targetPosition` ignorado en `SETUP_PLACE_POKEMON` — primer Pokemon siempre va a ACTIVE

**Área:** Backend — handler de acción `SETUP_PLACE_POKEMON`

**Descripción:**  
Durante la fase de setup inicial, al colocar el primer Pokemon con `"targetPosition": "BENCH"`, ese Pokemon termina en la posición ACTIVE independientemente del valor enviado. El campo `targetPosition` no es respetado para la primera carta colocada.

**Impacto:**  
El jugador no puede controlar qué Pokemon quiere como activo durante la fase de setup. En el TCG real, el jugador elige libremente cuál va al activo y cuáles al banco durante el setup.

**Cómo solucionarlo:**  
Revisar el handler de `SETUP_PLACE_POKEMON`. Probablemente tiene lógica del tipo "si no hay Pokemon activo, el primero va a ACTIVE" sin considerar el `targetPosition` enviado. Se debería respetar ese campo o, si las reglas del juego exigen colocar primero el activo, retornar un error descriptivo cuando se intenta colocar en BENCH sin activo previo.

---

## Bug 5: Objeto huérfano en `discardPile` — `cardId` con formato UUID en vez de ID de carta

**Área:** Backend — motor de juego / lógica de creación de instancias de carta

**Descripción:**  
En el `discardPile` de rivalbot apareció este objeto anómalo:
```json
{
  "cardId": "eaf2d718-7b1d-4972-b67b-7c94235e6c6b",
  "instanceId": "eaf2d718-7b1d-4972-b67b-7c94235e6c6b"
}
```
El `cardId` es un UUID generado automáticamente (debería ser algo como `xy1-133`), no un ID real de carta. Además, `cardId` e `instanceId` tienen el mismo valor, lo que indica que la instancia fue creada sin asociarle una carta base.

**Impacto:**  
El frontend no puede renderizar esa carta en el descarte (no tiene nombre, imagen ni tipo). Si intenta llamar a la API de cartas con ese `cardId`, va a obtener un 404. Además, si esta carta huérfana tiene energía adjunta, el sistema de validación de costos de ataque podría fallar o comportarse incorrectamente.

**Cómo solucionarlo:**  
Buscar el punto donde se crean instancias de cartas (probablemente en el servicio de setup del mazo o en el `DeckBuilderService`). Asegurarse de que toda instancia creada tenga un `cardId` que apunte a una carta real. El `instanceId` debe ser un UUID único generado en runtime; el `cardId` debe venir de los datos del mazo, nunca generarse como UUID.

### Análisis de causa raíz y plan de solución paso a paso

**Causa raíz (dónde se rompe):**  
`EngineStateMapper.java` línea 267 — cuando reconstruye una zona y no encuentra el `instanceId` en el mapa `existing`, usa el UUID como fallback de `cardId`. Ese fallback nunca debería existir porque indica que hay una instancia "fantasma" que no fue indexada.

**¿Por qué llega a ese `else`?** Porque el mapa `existing` se construye leyendo todas las zonas conocidas del estado anterior. Si una carta se mueve a una zona mediante `SearchDeckLogic` o `SelectionResolver` y esa lógica crea o mueve instancias sin pasar por el mapeador correctamente, quedan "huérfanas" fuera del índice.

### Qué habría que tocar

**1. `EngineStateMapper.java` — el fix más defensivo:**  
En el bloque `else` del `rebuildZone`, en vez de crear una instancia con `cardId = instanceId`, loguear el error y saltear la carta. Así el estado corrupto no se propaga al frontend. Esto es un fix de contención, no de causa raíz.

**2. `SearchDeckLogic.java` línea 41:**  
Hay un `.getOrDefault(instanceId, instanceId)` que es exactamente el mismo patrón: si no encuentra el `cardId` real en el mapa, devuelve el `instanceId` como fallback. Esa línea debería fallar ruidosamente o saltear la carta en lugar de usar el UUID.

**3. `PlayerField.getInstanceCardIds()`:**  
Este mapa `instanceId → cardId` es la fuente de verdad del engine. Habría que asegurarse de que toda operación que crea o mueve instancias (`SearchDeck`, `RecycleTrainer`, `SelectionResolver` al promover del banco, etc.) actualice este mapa correctamente. Si el mapa está completo y correcto, el `else` de `rebuildZone` nunca se ejecuta.

### Orden de ataque recomendado

1. **Fix inmediato en `EngineStateMapper`** — eliminar el fallback corrupto para que el bug sea visible (carta no aparece) en vez de silencioso (carta con UUID como `cardId`).
2. **Fix de raíz en `PlayerField`** — auditar todos los lugares que agregan instancias al board y verificar que también actualicen `instanceCardIds`.
3. **Fix en `SearchDeckLogic`** — eliminar el `.getOrDefault(instanceId, instanceId)` que propaga el problema.

---

## Bug 6: Mulligan incompleto — puede iniciar sin Pokemon básico y no da robo extra al rival

**Área:** Backend — `GameService.initializeGame` / construcción inicial del board

**Descripción:**
La lógica de mulligan reintenta una cantidad limitada de veces y, si no logra una mano con Pokemon básico, puede continuar con la mano disponible. Además, no implementa el robo extra opcional del rival por cada mulligan, regla documentada en `docs/engine/GAME_RULES.md`.

**Impacto:**
Una partida puede empezar en un estado inválido, sin Pokemon básico para uno de los jugadores. Eso puede derivar en derrota inmediata, setup roto o una partida que no representa las reglas esperadas.

**Cómo solucionarlo:**
Implementar mulligan completo: revelar mano sin básico, reshuffle/redraw hasta obtener básico o declarar error claro si el deck es inválido para iniciar; contabilizar mulligans y exponer/aplicar el robo extra opcional del rival según la regla elegida.

---

## Bug 7: Orden de daño incorrecto — modificadores después de debilidad/resistencia

**Área:** Backend / Engine — `DamageApplicationHandler` / `DamageCalculator`

**Descripción:**
El cálculo actual aplica debilidad/resistencia antes de sumar modificadores de daño. La regla esperada es: daño base → modificadores → debilidad/resistencia.

**Impacto:**
El daño final puede ser incorrecto en ataques con herramientas, habilidades o efectos que modifiquen daño. Esto rompe balance y resultados de KO.

**Cómo solucionarlo:**
Reordenar el cálculo para que `DamageApplicationHandler` acumule modificadores antes de invocar la etapa de debilidad/resistencia, o separar explícitamente las fases dentro de `DamageCalculator`.

---

## Bug 8: Modificadores como `Muscle Band` están hardcodeados y no son data-driven

**Área:** Backend / Engine — `ModifierHandler`

**Descripción:**
La lógica de modificadores depende de condiciones hardcodeadas, como nombre/tipo de carta, en vez de procesarse desde datos/efectos parseados de forma uniforme.

**Impacto:**
Las herramientas y efectos similares pueden comportarse distinto a la carta real o fallar cuando cambia el texto, subtipo o fuente de datos.

**Cómo solucionarlo:**
Mover los modificadores a una lógica data-driven basada en `parsedEffects`/registry, con tests para herramientas que alteran daño.

---

## Bug 9: Condiciones especiales entre turnos se procesan para un solo activo

**Área:** Backend / Engine — `BetweenTurnsState`

**Descripción:**
El procesamiento de condiciones especiales entre turnos parece aplicarse solo al campo activo del jugador que terminó el turno, en vez de procesar ambos Pokemon activos.

**Impacto:**
Veneno, quemadura, sueño u otras condiciones pueden resolverse a media frecuencia o solo para un jugador, generando partidas injustas e inconsistentes.

**Cómo solucionarlo:**
Procesar condiciones de ambos campos activos durante `BETWEEN_TURNS`, con tests donde ambos Pokemon estén afectados por condiciones distintas.

---

## Bug 10: Evolución demasiado restrictiva por `playerTurnCount <= 1`

**Área:** Backend / Reglas — `RuleValidator.validateEvolve`

**Descripción:**
La validación bloquea evolución cuando `playerTurnCount <= 1`. Esto puede bloquear también al segundo jugador en su primer turno propio, aunque la restricción debería alinearse con la regla elegida para el primer turno del juego.

**Impacto:**
Los jugadores pueden quedar impedidos de evolucionar más tiempo del debido, afectando ritmo y estrategia.

**Cómo solucionarlo:**
Revisar la regla final en `GAME_RULES.md`/`ENGINE_SPEC` y ajustar la validación para distinguir primer turno global, turno propio del Pokemon y entrada al juego.

---

## Bug 11: Selecciones pendientes incompletas fuera de `CHOOSE_ACTIVE_ON_KO`

**Área:** Backend / Engine — `SelectionResolver`, `TurnManager`, selección resumible

**Descripción:**
El mecanismo de `pendingSelection` existe, pero la resolución general está cubierta principalmente para elegir activo tras KO. Otros efectos que requieren selección (`SearchDeck`, `SwitchPokemon`, daño a banca, etc.) siguen incompletos o dependen de planes futuros.

**Impacto:**
Ataques, habilidades o trainers que necesitan selección del jugador pueden trabar la partida o no ejecutarse correctamente.

**Cómo solucionarlo:**
Completar el mecanismo resumible según `docs/engine/RESUMABLE_ATTACK_EFFECTS_PLAN.md`: tipos de selección, DTOs, payload esperado, persistencia y tests por cada tipo.

---

## Bug 12: Premios propios expuestos con `cardId`

**Área:** Backend — `GameService.toPlayerFieldDTO`

**Descripción:**
El DTO del jugador expone identidad de cartas de premio propias mediante `cardId`. En TCG, los premios están boca abajo; el jugador no debería conocer qué cartas son.

**Impacto:**
El frontend podría mostrar información oculta y darle ventaja indebida al jugador.

**Cómo solucionarlo:**
Exponer premios propios como conteo o instancias sin identidad visible, salvo que una regla específica revele una carta.

---

## Bug 13: Errores 500 evitables en partidas incompletas o no iniciadas

**Área:** Backend — `GameService.getGameState` / `performGameAction`

**Descripción:**
Consultas o acciones sobre partidas en `WAITING`/estados incompletos pueden llegar a rutas donde `player2` es `null` o donde la partida todavía no tiene board inicializado.

**Impacto:**
El backend puede responder 500 en vez de un error de dominio claro (`403`, `409`, `422`, etc.), complicando el manejo desde frontend y tests.

**Cómo solucionarlo:**
Agregar guards null-safe y validaciones de estado antes de acceder a `player2` o al board; devolver mensajes explícitos como “Game has not started”.

---

## Bug 14: Trainers/effects sin implementación pueden consumirse sin efecto claro

**Área:** Backend / Engine — `MainPhaseState` + registries de efectos

**Descripción:**
Cartas trainer o efectos sin implementación pueden aceptarse, moverse al descarte y no producir efecto observable ni error explícito.

**Impacto:**
El jugador pierde cartas por acciones que no hacen nada, y el frontend no puede explicar qué ocurrió.

**Cómo solucionarlo:**
Antes de consumir la carta, verificar si existe lógica registrada para el efecto. Si no existe, rechazar la acción con error claro o documentar la carta como no soportada.

---

## Bug 15: `SETUP_SET_PRIZES` existe en contrato pero no tiene handler real

**Área:** Backend / Contrato de acciones — `ActionType`, `SetupPhaseState`, `GameService.initializeGame`

**Descripción:**
`SETUP_SET_PRIZES` aparece como acción disponible/posible en el contrato, pero los premios se reparten automáticamente durante la inicialización y no hay un handler público claro para esta acción.

**Impacto:**
El contrato confunde al frontend: parece que debe enviar una acción que en realidad no corresponde o no está soportada.

**Cómo solucionarlo:**
Eliminar `SETUP_SET_PRIZES` del contrato público si los premios son automáticos, o implementar/documentar su handler si realmente debe ser una acción del setup.

---

## Bug 16: Case duplicado `TRAINER_PLAYED` en `GameFeedService` — segundo handler nunca se ejecuta

**Área:** Frontend — `game-feed.service.ts`

**Descripción:**  
En el `switch` de `buildEntry()`, el case `'TRAINER_PLAYED'` aparece dos veces: primero en la línea 87 (devuelve `"A Trainer card was played."`) y después en la línea 112, donde está agrupado con `'ITEM_PLAYED'` para devolver `"An Item card was played."`. En JavaScript/TypeScript, cuando un `switch` tiene dos `case` con el mismo valor, solo el **primero** se ejecuta; el segundo es código muerto que nunca se alcanza. El compilador lo advierte con el warning `[duplicate-case]`.

El resultado es que cuando el backend emite un evento `TRAINER_PLAYED`, el feed siempre muestra el mensaje genérico `"A Trainer card was played."` (línea 88) en vez del mensaje más específico `"An Item card was played."` (línea 114) que era la intención del segundo bloque.

**Impacto:**  
Funcional bajo — el feed de juego no crashea, pero el mensaje mostrado es incorrecto para el caso que se intentaba cubrir. El jugador no puede distinguir si se jugó un Item, un Supporter o un Stadium cuando todos dicen lo mismo. Además, el warning de compilación ensucia la consola de build y puede ocultar advertencias más graves. A nivel de código, genera confusión sobre cuál de los dos bloques es el correcto.

**Cómo solucionarlo:**  
Eliminar el primer `case 'TRAINER_PLAYED'` (línea 87–88) ya que es el genérico y redundante. El segundo bloque (línea 112–114) junto con los cases de `ITEM_PLAYED`, `SUPPORTER_PLAYED` y `STADIUM_PLAYED` ya cubren todos los subtipos de trainer de forma más específica. Si se necesita un fallback genérico para `TRAINER_PLAYED`, moverlo **después** de los cases específicos y hacer que caiga al final como último caso de la sección de trainers.

---

## Bug 17: Selección `SWITCH_POKEMON` no soportada en el frontend — partida se traba

**Área:** Frontend — `board-container.ts` / `board-container.html`

**Estado:** ✅ SOLUCIONADO

**Descripción:**  
Cuando un ataque o efecto de carta obliga a un jugador a intercambiar su Pokémon activo por uno de la banca (ej: "Luring Glow" de Slugma, "Knock Back" de Pangoro, "Rapid Spin" de Donphan, o la carta trainer "Switch"), el backend emite un `pendingSelection` con tipo `SWITCH_POKEMON`. El frontend tenía una lista de tipos de selección soportados en `board-container.ts` (línea 257) que **no incluía** `SWITCH_POKEMON`. Al no estar en la lista, el frontend mostraba un banner rojo diciendo `Tipo de selección "SWITCH_POKEMON" no soportado aún.` y la partida quedaba bloqueada — el backend esperaba una respuesta del jugador que nunca llegaba.

El bug solo aparece avanzada la partida porque `SWITCH_POKEMON` se dispara por efectos de ataques específicos o trainers que requieren intercambio. No es algo que ocurra en el setup ni al inicio.

**Impacto:**  
Crítico durante la partida — cualquier ataque con efecto de swap (hay al menos 5 cartas en el set XY Base que lo usan) causaba que la partida se trabara sin posibilidad de continuar. El jugador no tenía forma de resolver la selección y la partida quedaba en estado muerto.

**Cómo se solucionó:**  
Se implementaron tres cambios, todos en el frontend (el backend ya tenía todo el soporte necesario):

1. **`board-container.ts` — Lista de tipos soportados (línea 257):** Se agregó `'SWITCH_POKEMON'` al array `supported` dentro del computed `pendingSelectionUnsupported`, para que este tipo de selección deje de caer en el banner de error.

2. **`board-container.ts` — Computed `mySwitchPokemonSelection` (líneas 252–264):** Se creó un computed que filtra los Pokémon de la banca del jugador cuyo `instanceId` esté en `validOptions` del `pendingSelection`. Devuelve el prompt y las opciones con su `instanceId`, `cardId` y `benchIndex` (que es lo que el backend espera recibir).

3. **`board-container.html` — Panel de selección visual (líneas 208–225):** Se agregó una sección `@else if (mySwitchPokemonSelection(); as switchSel)` que renderiza un overlay con las cartas de la banca disponibles como botones clickeables. Al hacer click en una, se llama a `resolveSelection(opt.benchIndex)` que envía `{ action: 'resolveSelection', payload: { benchIndex } }` al backend, resolviendo la selección y continuando la partida.

Se reutilizó el mismo estilo visual `board-container__card-picker-overlay` que ya usan otras selecciones (`SHUFFLE_POKEMON_TO_DECK`, `SEARCH_DECK`, etc.) para mantener consistencia visual.

**Verificación:**  
El backend (`SelectionResolver.java:118–129`) ya tenía la lógica para procesar `SWITCH_POKEMON` recibiendo `benchIndex`, validando el índice, ejecutando el intercambio activo↔banca, y limpiando el `pendingSelection`. Los tests del backend (`SwitchPokemonLogicTest.java`) confirman que el efecto genera correctamente el `pendingSelection` tanto para `SELF` como para `OPPONENT`. El flujo completo es: efecto dispara selección → frontend muestra opciones → jugador elige → frontend envía `benchIndex` → backend ejecuta swap → partida continúa.

---

## Resumen

| # | Bug | Área | Severidad |
|---|-----|------|-----------|
| 1 | `benchIndex` no documentado en contrato público | Backend (API contract) | Media |
| 2 | Hand devuelve instancias de cartas ya jugadas | Backend (estado del juego) | Alta |
| 3 | Take Down no aplica recoil al atacante | Backend (motor de juego) | Media |
| 4 | `targetPosition` ignorado en SETUP | Backend (motor de juego) | Baja |
| 5 | Carta huérfana con UUID como `cardId` en discard | Backend (motor de juego) | Media |
| 6 | Mulligan incompleto | Backend (setup/reglas) | Alta |
| 7 | Orden de daño incorrecto | Backend (engine/reglas) | Alta |
| 8 | Modificadores hardcodeados | Backend (engine/reglas) | Media |
| 9 | Condiciones entre turnos procesan un solo activo | Backend (engine/reglas) | Alta |
| 10 | Evolución demasiado restrictiva | Backend (reglas) | Media |
| 11 | Selecciones pendientes incompletas | Backend (engine/selecciones) | Alta |
| 12 | Premios propios expuestos | Backend (DTO/estado) | Media |
| 13 | Errores 500 evitables en partidas incompletas | Backend (servicio/API) | Media |
| 14 | Trainers sin implementación consumidos sin efecto | Backend (engine/effects) | Media |
| 15 | `SETUP_SET_PRIZES` confuso/no soportado | Backend (contrato/setup) | Baja |
| 16 | Case duplicado `TRAINER_PLAYED` en feed service | Frontend (game-feed) | Baja |
| 17 | Selección `SWITCH_POKEMON` no soportada en frontend | Frontend (board-container) | Alta — ✅ Solucionado |
