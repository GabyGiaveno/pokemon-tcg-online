# OpenSpec — Contexto del proyecto

> Actualizado: 2026-06-11. Almacén de artefactos SDD: **hybrid** (archivos openspec + espejo
> en Engram cuando el MCP está conectado — topic keys `sdd/{change}/{fase}`).
> Punto de entrada para agentes: `/AGENTS.md`. Índice de docs humanas: `docs/README.md`.

## Estructura

```
openspec/
├── project.md            ← este archivo (contexto + convenciones)
├── config.yaml           ← stack, test runners, reglas por fase SDD, strict TDD
├── specs/                ← specs VIGENTES por capability (verdad acumulada)
│   ├── auth-security/    ←   handshake WS autenticado, JWT (del change fix-websocket-auth-lazyinit)
│   ├── effect-parsing/   ←   parser resiliente (del change robust-effect-parser)
│   └── effect-execution/ ←   ejecución de efectos: ataques, trainers y habilidades tier 1
└── changes/
    ├── player-selection-mechanism/  ← ABIERTO: engine ✅, falta Fase 5 (borde DTO, ver tasks §F5)
    └── archive/                     ← changes cerrados (proposal/spec/design/tasks/state completos)
        ├── robust-effect-parser/
        ├── effect-execution-trainers/   ← incluye la continuación Bloque 4 (habilidades tier 1)
        └── fix-websocket-auth-lazyinit/ ← ⚠️ ver POSTMORTEM: el Fix B se perdió en un cherry-pick
                                            y se re-aplicó el 2026-06-11 (commit d3201b1)
```

## Stack (verificado en `BE/pom.xml`)

| Tecnología | Versión |
|-----------|---------|
| Java / Spring Boot | 21 / 4.0.0 |
| Build | Maven wrapper (`mvnw.cmd` en Windows) |
| Tests | JUnit 5 + Mockito, H2 · **los tests están skippeados por defecto → `-Dmaven.test.skip=false`** |
| Frontend | Angular 20.3 (signals, standalone) · Karma/Jasmine |
| Calidad | JaCoCo (verify) · Checkstyle + PMD |

## Comandos

- **Suite BE:** `cd BE && ./mvnw.cmd test -Dmaven.test.skip=false` (217 verdes al 2026-06-11).
- **Cobertura:** `./mvnw.cmd verify` → `BE/target/site/jacoco/index.html` (engine ≥70%, services ≥60%).
- El engine se testea **sin** `@SpringBootTest` (Java puro, `CardLookup` mockeado con lambda).

## Convenciones de trabajo

- Engine (`BE/.../engine/`) = Java puro: sin Spring, sin BD. Única frontera: `CardLookup`.
- Commits convencionales (`feat:`, `fix:`, `docs:`...). **Sin atribución a IA.**
- SDD **interactivo**: preguntar antes de cada bloque de código; el usuario aprueba fase por fase.
- **Apply con Fable** (decisión del usuario 2026-06-11) y **TDD estricto** (rojo→verde por work-unit).
- Un change puede CONTINUARSE (nuevo scope dentro del mismo change con proposal/spec/design/tasks
  extendidos) cuando el dominio es el mismo — precedente: Bloque 4 dentro de
  `effect-execution-trainers`, a pedido del usuario para no fragmentar la documentación.

## Estado de referencia

- Bugs conocidos y verificados: tabla BUG-1..14 en `/AGENTS.md`.
- Roadmap priorizado: `docs/TODO/PROJECT_TODO.md` (A1 persistencia = la decisión bloqueante).
- Estado vivo de efectos: `docs/engine/EFFECT_IMPLEMENTATION_PROGRESS.md`.
- Guía exhaustiva del motor: `docs/engine/ENGINE_GUIDE.md` (§28 = habilidades).
