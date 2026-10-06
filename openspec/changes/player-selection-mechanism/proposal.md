# Proposal: Player Selection Mechanism

## Intent

El engine necesita que el jugador **elija algo en medio de resolver un efecto** (qué Pokémon promover tras un KO, qué carta buscar en el mazo, a qué Pokémon de la banca pegarle). Hoy ese mecanismo **no existe**: `ActionType` no tiene acción de selección, `SelectionsHandler` de ataques es un stub, y `KnockoutProcessor.java:96-99` auto-promueve `bench[0]` con un `// TODO: let the owner choose`. Es un mecanismo **transversal** (inventario §6.1) que desbloquea el fix #4 y varios stubs de ataque.

## Scope

### In Scope
- Mecanismo general de selección basado en **estado** (pausar/reanudar), no callback síncrono.
- `pendingSelection` en `BoardState`: qué se pide, a quién, opciones válidas, y la **continuación** (qué falta hacer al recibir la respuesta).
- Nuevos `ActionType` (`RESOLVE_SELECTION`) y entry point público `resolveSelection(playerId, choice)` en `TurnManager`.
- Guarda al tope de `processAction`: con selección pendiente solo se acepta su resolución.
- **Primer consumidor real (fix #4):** `CHOOSE_ACTIVE` al promover tras KO — peor caso (anidado profundo + auto-completar turno después).

### Out of Scope
- Wiring de los stubs de ataque con selección (`SearchDeckLogic`, `SwitchPokemonLogic`, `LookAtDeckLogic`, `DamageToBenchLogic`) — cada uno es per-logic; siguen en bloques posteriores reusando este mecanismo.
- Mecanismo de efectos continuos / pasivos (Bloque 4).
- Capa REST/controller: este bloque es **solo engine**.

## Capabilities

### New Capabilities
- `player-selection`: pausar la resolución para pedir una elección al jugador y reanudar exactamente donde quedó, con opciones válidas y resolución validada.

### Modified Capabilities
- None (los specs de `effect-execution` / ataques se tocan cuando se cableen sus consumidores, en bloques siguientes).

## Approach

Reificar la "continuación" como dato en `pendingSelection` en lugar de depender de la pila de llamadas (Java no tiene continuations). Cuando un punto de resolución necesita input: setea `pendingSelection`, **aborta limpio** el resto del flujo síncrono (sin completar el turno), y devuelve un evento que pide la selección. `resolveSelection` valida la respuesta contra las opciones, ejecuta la continuación, y retoma el flujo (incluido el auto-complete del turno para el caso KO).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `models/game/BoardState.java` | Modified | Campo `pendingSelection` |
| `models/game/PendingSelection.*` | New | Modelo de la selección pendiente + continuación |
| `models/cards/ActionType.java` | Modified | `RESOLVE_SELECTION` |
| `engine/TurnManager.java` | Modified | Guarda + `resolveSelection` + abort/resume del flujo |
| `engine/KnockoutProcessor.java` | Modified | Fix #4: pedir activo en vez de auto-promover `bench[0]` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Abortar el flujo a mitad rompe el auto-complete del turno | High | Continuación reificada + tests del flujo KO end-to-end |
| KO ocurre anidado profundo (peor caso) | Med | Diseñar el punto de corte explícito en design.md antes de codear |
| PR supera presupuesto de 400 líneas | Med | Scope acotado a mecanismo + fix #4; stubs de ataque diferidos |

## Rollback Plan

Cambio aislado en el engine. Revertir el commit del bloque restaura el auto-promote de `bench[0]`; `pendingSelection` es aditivo (nullable) y no afecta flujos existentes si nunca se setea.

## Dependencies

- Ninguna externa. Reusa `KnockoutProcessor` promotion logic (líneas 98-116) y el patrón de campos nullable en `BoardState`.

## Success Criteria

- [ ] Tras un KO con banca no vacía, el engine emite una selección y NO auto-promueve.
- [ ] `resolveSelection` con una opción válida promueve el elegido y completa el turno.
- [ ] `resolveSelection` con opción inválida o jugador equivocado falla sin mutar el board.
- [ ] `./mvnw test` verde (130 actuales + nuevos), sin romper flujos sin selección.
