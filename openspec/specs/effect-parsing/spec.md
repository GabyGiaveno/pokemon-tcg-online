# Spec canónica — `effect-parsing`

> Baseline establecida por la change `robust-effect-parser` (archivada 2026-06-01).
> Esta es la spec vigente de la capability de parseo de efectos.

## Requisitos vigentes

- **R1** — El parser de efectos procesa cada efecto de forma independiente: un `type` desconocido se
  resuelve a `UnknownEffect` (inerte) y NO descarta los efectos válidos de la misma carta.
- **R2** — El parser tolera propiedades JSON no modeladas (ej. `conditions` en `PASSIVE_ABILITY`).
- **R3** — La resiliencia aplica también a efectos anidados (`COIN_FLIP.ifHeads/ifTails`,
  `PASSIVE_ABILITY.effect`).
- **R4** — La fuente única del vocabulario es el `@JsonSubTypes` de `AttackEffect`; `UnknownEffect` es el
  borde controlado (`defaultImpl`). Los tipos omitidos se loguean.
- **R5** — Sin regresión para tipos ya soportados.

## Implementación de referencia
- `models/cards/effects/AttackEffect.java` (`@JsonTypeInfo` con `defaultImpl=UnknownEffect`, `visible=true`).
- `models/cards/effects/UnknownEffect.java`.
- `engine/chain/AttackParser.java`.

## Nota de extensión
- `TrainerEffect` debe recibir el mismo patrón (`defaultImpl=UnknownTrainerEffect`) cuando se cree
  `TrainerEffectParser` (Bloque 2).
