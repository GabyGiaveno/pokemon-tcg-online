# Plan — Resumable Attack Effects (opción B)

> Para la próxima sesión. Diseño aprobado el 2026-06-09. Enchufar los efectos de ataque que
> requieren **selección del jugador en medio de la resolución** (`SearchDeck`, `SwitchPokemon`,
> `LookAtDeck`, `DamageToBench`) con un pipeline de ataque **pausable y reanudable**.
>
> Norte explícito del usuario: **arquitectura correcta y patrones consistentes con todo el proyecto.**
> Prerrequisito ya hecho: mecanismo de selección reificada (`player-selection-mechanism`,
> `PendingSelection` + `SelectionType` + `SelectionResolver` + corte/reanudación en `TurnManager`).

## Por qué (b) y no (a)

(a) "la selección cierra el ataque" alcanza para efectos terminales, pero **miente** cuando hay cálculo
posterior a la elección (ej. "descartá una energía —elegí cuál— y este ataque hace 20 más por energía
restante"). (b) reanuda el pipeline **exactamente donde quedó**. Es la arquitectura correcta para un
engine de TCG serio.

## Problema técnico raíz

`AttackResolutionChain.resolve()` construye un `AttackContext` **efímero** y corre 7 handlers que **mutan
el board incrementalmente**. Entre requests HTTP el board se rehidrata desde storage, pero el estado
intermedio del pipeline (`damageModifiers`, `finalDamage`, en qué handler/efecto íbamos, eventos
parciales) **no se persiste**. Reanudar a mitad exige reificar ese estado y saltar los handlers ya
corridos sin re-mutar.

## Decisión arquitectónica central: separar "qué se elige" de "cómo se reanuda"

NO meter el estado del pipeline dentro de `PendingSelection` (rompería SRP: un DTO genérico cargando
internals del motor). En su lugar:

| Responsabilidad | Quién | Patrón |
|---|---|---|
| Qué se le pide al jugador (genérico) | `PendingSelection` (existe) | Reificación / DTO |
| Cómo continúa el ataque (snapshot) | **`AttackResumeState`** (nuevo) | **Memento** |
| Aplicar la elección + continuar | `AttackResolutionChain.resumeFrom()` | **CoR resumible** |
| Despachar al dominio correcto | `SelectionResolver` (existe) | Strategy / OCP |
| Decidir qué se pide | la `EffectLogic` (existe) | Flyweight |

`PendingSelection` queda **intacto y genérico**. El board gana un segundo campo nullable `attackResume`
(el Memento). Conviven: `pendingSelection` (lo que ve el jugador) + `attackResume` (lo que el motor
necesita para seguir).

## Memento: `AttackResumeState`

Captura **solo lo que no vive en el board**:

```java
class AttackResumeState {     // dato plano serializable, en BoardState
    int attackIndex;          // reconstruir el AttackData
    int handlerIndex;         // program counter externo: en qué handler cortamos
    int effectIndex;          // dentro del handler que itera parsedEffects
    int damageModifiers;      // estado mutable del ctx fuera del board
    int finalDamage;
}
```

## Señal de pausa (CoR resumible)

`AttackContext` hoy tiene `normal` y `attackCancelled` (falló). Se agrega un **tercer estado ortogonal:
paused** (esperando input) — NO es cancelar.

```java
// AttackContext gana:
boolean pauseRequested;
PendingSelection requestedSelection;

void requestSelection(PendingSelection sel) { // la logic llama y RETORNA
    this.pauseRequested = true;
    this.requestedSelection = sel;
}
```

Puntos de control:
- `PreAttackHandler` / `PostDamageHandler` (iteran `parsedEffects`): tras cada logic,
  `if (ctx.isPauseRequested()) { guardar effectIndex; break; }`.
- `AttackResolutionChain.resolve()`: tras cada handler,
  `if (ctx.isPauseRequested()) { construir AttackResumeState; board.setAttackResume(...); board.setPendingSelection(...); return events; }`.
- `TurnManager` **ya** corta cuando ve `pendingSelection` — no se toca.

## Contrato que blinda la idempotencia (no doble-mutación)

Una `EffectLogic` con selección **debe ser read-only hasta resolver**: pide la selección y retorna **sin
mutar**. La mutación ocurre en `resumeFrom`, ya con la elección. Así, reconstruir el ctx desde el board
persistido es seguro: los handlers previos ya mutaron (está en el board), el efecto que pausó NO mutó
(lo hará al reanudar).

```java
// SearchDeckLogic deja de ser stub:
public void execute(SearchDeckEffect e, AttackContext ctx) {
    ctx.requestSelection(PendingSelection.builder()
        .type(SelectionType.SEARCH_DECK_CARD)
        .ownerPlayerId(ctx.getAttackerField().getPlayerId())
        .validOptions(/* cardIds del mazo filtrados */)
        .prompt("Elegí una carta del mazo.")
        .build());
    // NO muta. La carta se mueve en el resume.
}
```

## Reanudación

```
RESOLVE_SELECTION → SelectionResolver.resolve()
   case SEARCH_DECK_CARD → attackChain.resumeFrom(board, board.getAttackResume(), choice, lookup)
        ├─ buildContext(board, attackIndex)      // rehidrata inputs
        ├─ ctx.restore(attackResume)             // damageModifiers, finalDamage
        ├─ applyChoice(ctx, choice)              // AHORA sí muta
        └─ continúa pipeline desde handlerIndex/effectIndex+1
   board.setAttackResume(null); board.setPendingSelection(null)
→ TurnManager reanuda (completeTurn por currentPhase, igual que CHOOSE_ACTIVE_ON_KO)
```

`SelectionResolver` queda **OCP**: cada efecto-con-selección es un `case` que delega en la chain; el
`TurnManager` ni se entera.

## Plan de implementación (fase por fase)

- **Fase A — Infra de continuación**: `AttackResumeState` (Memento) + `BoardState.attackResume` + estado
  *paused* en `AttackContext` + detección de pausa en chain/handlers + `resumeFrom()`. Testeable con una
  logic de prueba, sin tocar las 4 reales.
- **Fase B — Wire**: `SelectionType` de ataque + `SelectionResolver` delega en la chain + ruteo.
- **Fase C — Logics reales**, una por una: `DamageToBench` → `SwitchPokemon` → `SearchDeck` → `LookAtDeck`.
- **Fase D — Tests**: unit de `resumeFrom` (idempotencia, no doble-mutación) + integración por logic.

## Archivos que se tocarán (referencia)

- `engine/chain/AttackContext.java` — estado paused + `requestSelection`.
- `engine/chain/AttackResolutionChain.java` — detección de pausa + `resumeFrom`.
- `engine/chain/handlers/PreAttackHandler.java` / `PostDamageHandler.java` — corte por efecto.
- `models/game/AttackResumeState.java` — nuevo (Memento).
- `models/game/BoardState.java` — campo `attackResume`.
- `models/game/SelectionType.java` — nuevos valores.
- `engine/SelectionResolver.java` — cases nuevos (delegan en la chain).
- `engine/effects/logics/{SearchDeck,SwitchPokemon,LookAtDeck,DamageToBench}Logic.java` — dejan de ser stubs.

## Patrones respetados

Chain of Responsibility (pipeline) · Flyweight (logics stateless) · **Memento** (snapshot) ·
Reificación de la continuación (serializable) · Strategy/OCP (`SelectionResolver`) · State (fases).
Consistente con `player-selection-mechanism` y el resto del engine.
