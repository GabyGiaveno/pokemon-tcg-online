# Spec — Capability `effect-execution` (vigente)

> Promovida desde el change `effect-execution-trainers` (archivado 2026-06-11) que cubrió
> Bloque 2 (autocontenidos + trainers, grupos A-D) y Bloque 4 tier 1 (habilidades, grupo E).
> Tier 2 de habilidades y efectos con selección → change futuro sobre el plan resumable.

---

## Grupo A — Revivir logics de ataque ya escritas

### REQ-A0 (MODIFIED 2026-06-12) — Pagar el costo de un ataque NO descarta energía
- **GIVEN** un atacante con 2 energías adjuntas y un ataque de costo 2
- **WHEN** el ataque se valida y resuelve con éxito
- **THEN** las 2 energías SIGUEN adjuntas (regla XY: la energía solo se descarta por efecto
  explícito — p.ej. `DISCARD_ENERGY` —, retiro, o al salir de juego; `GAME_RULES.md` §Energy).
- *Fix de la implementación original de `EnergyValidationHandler`, que consumía el pool al validar.*

### REQ-A1 (ADDED) — `DISCARD_ENERGY` se ejecuta en ataques
- **GIVEN** un ataque con `{type:"DISCARD_ENERGY", amount:1, target:"SELF"}` y el atacante con 2 energías
- **WHEN** se resuelve el ataque
- **THEN** el atacante queda con 1 energía (se ejecutó la logic, ya no está muerta).

### REQ-A2 (ADDED) — `COIN_FLIP` se ejecuta y resuelve sus sub-efectos
- **GIVEN** un ataque con `{type:"COIN_FLIP", ifHeads:[{type:"ADD_DAMAGE",amount:20}], ifTails:[]}` y `Random` fijado a cara
- **WHEN** se resuelve el ataque
- **THEN** el daño se incrementa en 20.
- **AND** con `Random` fijado a cruz, no se aplica el sub-efecto.
- *Nota:* el timing (pre/post) de los sub-efectos lo resuelve `design`.

---

## Grupo B — Stubs autocontenidos (con lógica real)

### REQ-B1 (ADDED) — `SHUFFLE_HAND` (ataque)
- **GIVEN** un efecto `SHUFFLE_HAND` con `target` y `drawAmount`
- **WHEN** se ejecuta
- **THEN** la mano del objetivo se incorpora al mazo y se roban `drawAmount` cartas.

### REQ-B2 (ADDED) — `DRAW_UNTIL_HAND_SIZE`
- **GIVEN** un efecto `DRAW_UNTIL_HAND_SIZE` con `amount:6` y una mano de 4
- **WHEN** se ejecuta
- **THEN** se roban cartas hasta tener 6 (o hasta agotar el mazo).

### REQ-B3 (ADDED) — `DAMAGE_COUNTERS` sin elección
- **GIVEN** un efecto `DAMAGE_COUNTERS` con `target` sin selección (ej. `ALL_OPPONENT`)
- **WHEN** se ejecuta
- **THEN** se aplican los contadores a todos los objetivos indicados.
- *Out:* variantes que requieren elegir objetivo → Bloque 3.

---

## Grupo C — Pipeline de trainers (espejo de ataques)

### REQ-C1 (ADDED) — `TrainerEffectParser` resiliente
Hereda el contrato de la spec `effect-parsing`: un `type` desconocido cae en `UnknownTrainerEffect`
(`defaultImpl`) sin romper.
- **GIVEN** `trainerEffects` con un tipo válido + uno desconocido
- **WHEN** se parsea
- **THEN** el válido se conserva y el desconocido es inerte.

### REQ-C2 (ADDED) — `MainPhaseState` ejecuta los `trainerEffects` parseados
- **GIVEN** un `Card` de test con `parsedEffects` que incluye `trainerEffects:[{type:"DRAW_CARDS",amount:2}]`
- **WHEN** el jugador juega esa carta en MAIN
- **THEN** el board refleja el efecto (roba 2) y se emiten los eventos.

### REQ-C3 (ADDED) — La carga incluye `trainerEffects`
- **GIVEN** una carta con bloque `trainerEffects` en el JSON parseado
- **WHEN** `PokemonTCGApiService` la carga
- **THEN** `card.parsedEffects` contiene ese bloque (viaja en el mismo String que attacks/abilities).

### REQ-C4 (ADDED) — Trainers autocontenidos de XY1 funcionan
Cada uno, con su `Card` de test y board:
- **Professor Sycamore** (`DISCARD_HAND_DRAW`, 7): descarta la mano y roba 7.
- **Shauna** (`SHUFFLE_HAND` SELF, 5): baraja la mano al mazo y roba 5.
- **Red Card** (`SHUFFLE_HAND` OPPONENT, 4): el rival baraja su mano y roba 4.
- **Team Flare Grunt** (`DISCARD_ENERGY` al activo rival): descarta 1 energía del activo del oponente.
- **Roller Skates** (coin flip → robar 3): con cara roba 3, con cruz nada.

---

## Grupo D — Chain of Responsibility para jugar trainer/item/stadium

### REQ-D1 (ADDED) — La acción se resuelve por una cadena de handlers
- **GIVEN** la acción de jugar un trainer con efectos parseados
- **WHEN** se procesa
- **THEN** se resuelve vía `TrainerResolutionChain` (handlers ordenados, contexto mutable, cancelable),
  cuyo `TrainerEffectExecutionHandler` ejecuta los `TrainerEffect` vía `TrainerEffectRegistry`.

### REQ-D2 (ADDED) — Extensible sin romper
La cadena DEBE permitir insertar handlers posteriores (`TrainerSelectionHandler` en Bloque 3,
validación/post en Bloque 4) sin modificar los handlers existentes.

---

## Preservados

### REQ-P1 (PRESERVED) — Trainers sin `trainerEffects` siguen jugándose
- **GIVEN** un trainer sin bloque `trainerEffects` (cáscara `{id,name}`)
- **WHEN** se juega
- **THEN** la carta se mueve (descarte/estadio) y NO se lanza error (comportamiento actual).

---

---

# CONTINUACIÓN — Grupo E: Habilidades (Bloque 4 tier 1, 2026-06-11)

### REQ-E1 (ADDED) — El bloque `"abilities"` se parsea
- **GIVEN** un `Card` cuyo `parsedEffects` incluye el bloque `abilities` de `xy1_parsed.json`
- **WHEN** `AbilityParser.parse(card.getParsedEffects())` se invoca
- **THEN** devuelve la lista de `AbilityData {name, text, parsedEffects}` con los
  `PassiveAbilityEffect` tipados, **incluyendo el array `conditions`** (hoy se descarta).
- **AND** un `type` anidado desconocido cae en `UnknownEffect` inerte sin romper (hereda el
  contrato de `effect-parsing`).

### REQ-E2 (ADDED) — Las `conditions` del JSON se evalúan
El sistema MUST evaluar las condiciones del vocabulario de bigpickle contra el estado real:
- `IS_ACTIVE` — el Pokémon fuente es el Activo de su dueño.
- `HAS_ENERGY {value}` — el Pokémon protegido tiene energía del tipo indicado adjunta.
- `HAS_CONDITION {value}` — el Pokémon fuente tiene esa condición especial.
Una habilidad cuyas conditions no se cumplen MUST NOT ejecutarse.

### REQ-E3 (ADDED) — Habilidades activadas vía `USE_ABILITY`
- **GIVEN** Delphox en juego con *Mystical Fire* y una mano de 3 cartas
- **WHEN** el jugador envía `USE_ABILITY` con `targetPosition` apuntando a Delphox en fase MAIN
- **THEN** roba hasta tener 6 en mano y se emite el evento de habilidad usada.
- **AND** un segundo `USE_ABILITY` de la misma habilidad en el mismo turno MUST fallar
  (`ABILITY_ALREADY_USED`); el registro se resetea en `resetTurnFlags`.
- **AND** `USE_ABILITY` sobre un Pokémon sin habilidades activables MUST fallar (`NO_ABILITY`).

### REQ-E4 (ADDED) — Habilidades disparadas en la attack chain
- **Spiky Shield**: **GIVEN** Chesnaught activo defensor **WHEN** recibe daño > 0 de un ataque
  rival **THEN** el atacante recibe 30 de daño (3 counters), incluso si Chesnaught quedó KO,
  y un auto-KO del atacante por ese retroceso se procesa.
- **Destiny Burst**: **GIVEN** Voltorb activo KO por daño de ataque y `Random` fijado a cara
  **THEN** el atacante recibe 50 (5 counters); con cruz, nada. (Nuevo subtipo `COIN_FLIP_DAMAGE`
  con `headsCondition`.)

### REQ-E5 (ADDED) — Habilidades continuas como guards
- **Fur Coat**: **GIVEN** Furfrou defensor **WHEN** se calcula el daño final **THEN** se restan
  20 DESPUÉS de debilidad/resistencia y modificadores (floor 0).
- **Forest's Curse**: **GIVEN** Trevenant ACTIVO del rival **WHEN** el jugador intenta
  `PLAY_ITEM` **THEN** la acción es rechazada (`ITEMS_LOCKED`); si Trevenant está en banca,
  MUST NOT bloquear (condition `IS_ACTIVE`).
- **Sweet Veil**: **GIVEN** Slurpuff en juego del defensor y el defensor activo con energía
  Fairy adjunta **WHEN** un ataque intenta aplicar una condición especial **THEN** la condición
  NO se aplica; sin energía Fairy en el objetivo, se aplica normal.

### REQ-E6 (PRESERVED) — Cartas sin habilidades / abilities desconocidas
- Un `Card` sin bloque `abilities` parsea a lista vacía; el flujo de ataque/trainer existente
  no cambia para cartas sin habilidades (suite previa sigue verde).

## Out of scope (recordatorio)
- Selección del jugador → Bloque 3 (hecho a nivel engine). Tier 2 de abilities (Water Shuriken,
  Stance Change, Fairy Transfer, Drive Off, Upside-Down Evolution) → requiere selección/resumable.
- Tools (Hard Charm/Muscle Band data-driven) y stadiums pasivos siguen fuera (resto del Bloque 4).
