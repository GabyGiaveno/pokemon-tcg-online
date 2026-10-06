# Player Selection Specification

## Purpose

Permitir que el engine **pause la resolución** de un efecto cuando necesita que un jugador elija algo, y **reanude** exactamente donde quedó al recibir la respuesta. Mecanismo transversal, basado en estado (no callback síncrono). Primer consumidor: elegir Pokémon activo al promover tras un KO (fix #4).

## Requirements

### Requirement: Pending Selection State

El `BoardState` MUST poder representar una selección pendiente que describa qué se pide, a qué jugador, las opciones válidas, y la continuación a ejecutar al resolverse. Mientras no haya selección pendiente, el campo MUST ser nulo y los flujos existentes MUST comportarse igual que sin el mecanismo.

#### Scenario: No hay selección pendiente

- GIVEN un board sin selección pendiente
- WHEN se procesa cualquier acción normal de turno
- THEN el flujo se resuelve igual que hoy
- AND el campo de selección pendiente permanece nulo

#### Scenario: Selección pendiente describe el pedido

- GIVEN un efecto que requiere elección del jugador
- WHEN el engine emite la selección
- THEN la selección pendiente registra playerId, opciones válidas y la continuación
- AND el engine devuelve un evento que comunica el pedido

### Requirement: Action Guard While Selection Pending

Mientras exista una selección pendiente, `processAction` MUST aceptar únicamente la resolución de esa selección. Cualquier otra acción MUST ser rechazada SIN mutar el board.

#### Scenario: Acción normal bloqueada

- GIVEN un board con selección pendiente
- WHEN un jugador envía una acción que no es la resolución
- THEN la acción es rechazada con un mensaje de error
- AND el board no se modifica

### Requirement: Resolve Selection

El engine MUST exponer `resolveSelection(playerId, choice)`. La elección MUST validarse contra las opciones válidas y contra el `playerId` esperado. Una elección inválida o de un jugador incorrecto MUST fallar SIN mutar el board ni limpiar la selección pendiente. Una elección válida MUST ejecutar la continuación, limpiar la selección pendiente, y reanudar el flujo que estaba en curso.

#### Scenario: Resolución válida

- GIVEN un board con selección pendiente para el jugador A
- WHEN A resuelve con una opción válida
- THEN se ejecuta la continuación asociada
- AND la selección pendiente queda nula
- AND el flujo interrumpido se reanuda hasta completarse

#### Scenario: Opción inválida

- GIVEN un board con selección pendiente
- WHEN se resuelve con una opción fuera de las válidas
- THEN la resolución falla con error
- AND el board no se modifica
- AND la selección pendiente sigue pendiente

#### Scenario: Jugador equivocado

- GIVEN una selección pendiente para el jugador A
- WHEN el jugador B intenta resolverla
- THEN la resolución falla con error
- AND el board no se modifica

### Requirement: Choose Active On Knockout (Fix #4)

Cuando un Pokémon activo es KO y su dueño tiene Pokémon en banca, el engine MUST pedir al dueño qué Pokémon promover en lugar de auto-promover `bench[0]`. Las opciones válidas MUST ser los Pokémon de la banca del dueño. Al resolverse, el elegido MUST promoverse a activo y el flujo de fin de turno MUST reanudarse. Si la banca está vacía, NO se pide selección y se mantiene el flujo de derrota actual.

#### Scenario: KO con banca no vacía pide selección

- GIVEN el activo del jugador queda KO y su banca tiene ≥1 Pokémon
- WHEN se procesa el KO
- THEN el engine emite una selección con la banca como opciones
- AND no se promueve ningún Pokémon automáticamente
- AND el turno no se completa todavía

#### Scenario: Resolución promueve el elegido y completa el turno

- GIVEN una selección pendiente de activo tras KO
- WHEN el dueño elige un Pokémon válido de la banca
- THEN ese Pokémon pasa a activo y se quita de la banca
- AND el auto-complete del turno (between-turns, cambio de jugador, draw) se reanuda

#### Scenario: KO con banca vacía no pide selección

- GIVEN el activo queda KO y la banca está vacía
- WHEN se procesa el KO
- THEN no se crea selección pendiente
- AND se aplica la condición de derrota existente
