# Documentación — Pokémon TCG Digital

`docs/` es el hogar canónico de la documentación. La raíz del repo solo conserva
`README.md` (setup), `ARCHITECTURE.md` (visión arquitectónica), `AGENTS.md` (contexto para
agentes IA) y `CHANGELOG.md`.

> **¿Recién llegás?** Humanos: empezá por el [code review](review/CODE_REVIEW_2026-06-11.md)
> (estado real) y la [guía del engine §0](engine/ENGINE_GUIDE.md). Agentes IA: [`/AGENTS.md`](../AGENTS.md).
> **¿Qué hay que hacer?** → [TODO/PROJECT_TODO.md](TODO/PROJECT_TODO.md) (roadmap priorizado).

## 📍 Estado y planificación (empezar acá)
- [review/CODE_REVIEW_2026-06-11.md](review/CODE_REVIEW_2026-06-11.md) ⭐ — review integral BE+FE: bugs, estado por módulo, qué no tocar.
- [TODO/PROJECT_TODO.md](TODO/PROJECT_TODO.md) ⭐ — roadmap P0/P1/P2 con dependencias y DoD.
- [tracking/NEXT_SESSION.md](tracking/NEXT_SESSION.md) — punto de partida autocontenido de la próxima sesión.
- [/AGENTS.md](../AGENTS.md) — bugs verificados con file:line, invariantes y gotchas para agentes.
- `openspec/` — artefactos SDD por change (specs/design/tasks); estado en cada `state.yaml`.

## 📐 Specs del juego (fuente de verdad de reglas)
- [project/PROJECT_CONSTITUTION.md](project/PROJECT_CONSTITUTION.md) — principios, stack, patrones obligatorios, DoD.
- [engine/GAME_RULES.md](engine/GAME_RULES.md) — reglas del juego (XY1). ⚠️ conflicto abierto con ENGINE_SPEC §1.2 sobre el robo del primer turno (TODO A6).
- [engine/ENGINE_SPEC.md](engine/ENGINE_SPEC.md) — especificación del motor (setup, turnos, ataque).
- [engine/STATUS_EFFECT_SPEC.md](engine/STATUS_EFFECT_SPEC.md) — orden jerárquico de condiciones especiales.
- [project/DATA_SPEC.md](project/DATA_SPEC.md) — ENUMs, entidades JPA, modelos en memoria y DTOs.
- [project/SETUP_SPEC2.md](project/SETUP_SPEC2.md) — configuración y arranque del proyecto.

## ⚙️ Motor (engine)
- [engine/ENGINE_GUIDE.md](engine/ENGINE_GUIDE.md) ⭐⭐ — **LA guía exhaustiva del motor** (§0 onboarding humano → §26 selección → §28 habilidades, con recetas de extensión). Si tocás el engine, vivís acá.
- [engine/EFFECT_IMPLEMENTATION_PROGRESS.md](engine/EFFECT_IMPLEMENTATION_PROGRESS.md) — estado VIVO por bloque (qué efecto funciona hoy).
- [engine/EFFECT_IMPLEMENTATION_INVENTORY.md](engine/EFFECT_IMPLEMENTATION_INVENTORY.md) — mapa estructural de los efectos (dónde vive cada uno).
- [engine/RESUMABLE_ATTACK_EFFECTS_PLAN.md](engine/RESUMABLE_ATTACK_EFFECTS_PLAN.md) — diseño aprobado para efectos con selección (tier 2).
- [engine/ATTACK_EFFECT_DESIGN.md](engine/ATTACK_EFFECT_DESIGN.md) — de texto de carta a efecto tipado.
- [abilityParsing/PARSING_METHOD.md](abilityParsing/PARSING_METHOD.md) — método de los scripts Python que generan `xy1_parsed.json`.

## 🌐 API
- [api/API_SPEC.md](api/API_SPEC.md) — contratos REST y WebSocket.
- [api/REALTIME_WEBSOCKET.md](api/REALTIME_WEBSOCKET.md) — contrato final WebSocket realtime: propósito, seguridad, reconexión y prueba manual.
- [api/ETAPA_0_API_CONTRACT.md](api/ETAPA_0_API_CONTRACT.md) — alineación de contratos.
- [api/ENDPOINTS_CARDS_DECKS.md](api/ENDPOINTS_CARDS_DECKS.md) — endpoints de cartas y mazos.

## 🖥️ Frontend
- [frontend/POKEDEX_DOCS.md](frontend/POKEDEX_DOCS.md) — docs de la Pokédex implementada (mock-first).
- [/SPECS/POKEDEX_SPEC.md](../SPECS/POKEDEX_SPEC.md) — spec funcional de la Pokédex (UC/AC completos).
- [frontend/PROFILE_SCREEN_SPEC.md](frontend/PROFILE_SCREEN_SPEC.md) — pantalla de perfil (fuera de spec original).
- [frontend/FRONTEND_AUTH_UI_SPEC.md](frontend/FRONTEND_AUTH_UI_SPEC.md) — UI de auth.
- [/SPECS/RECUPERACION_CONTRASENA_SPEC.md](../SPECS/RECUPERACION_CONTRASENA_SPEC.md) — flujo de recuperación de contraseña (implementado).
- [frontend/Deck Builder/](frontend/Deck%20Builder/) — arquitectura, scope, contrato API y guidelines del deck builder.
- [/FE/AGENTS.md](../FE/AGENTS.md) — convenciones Angular para agentes (signals, standalone, OnPush).

## 🏛️ Decisiones de arquitectura (ADRs)
- [001 — Factories y validadores](architecture/001-factories-y-validadores.md)
- [002 — Engine Turn Manager](architecture/002-engine-turn-manager.md)
- [003 — Attack Effects (Flyweight)](architecture/003-attack-effects-flyweight.md)
- [004 — Trainer Effects (Flyweight)](architecture/004-trainer-effects-flyweight.md)
- [005 — Facade delegador puro + seam CardLookup](architecture/005-facade-cardlookup-seam.md)
- [006 — Estado y puntos críticos](architecture/006-estado-actual-y-puntos-criticos.md) — ⚠️ histórico; supersedido por el review.

## 👥 Equipo / histórico
- [project/TEAM2.md](project/TEAM2.md) — división de tareas por developer. ⚠️ referencias a `docs/SDD.md` (no existe) — el diseño vivo está en este índice.
- [tracking/FIXES.md](tracking/FIXES.md) — log histórico de fixes (sesiones 2-5).
- [architecture/005-fixes-aplicados-y-estado.md](architecture/005-fixes-aplicados-y-estado.md) — histórico.
- [tracking/FACADE_INTEGRATION_DEBT.md](tracking/FACADE_INTEGRATION_DEBT.md) — diagnóstico del conflicto engine↔mapper (insumo del TODO A1; los tests que cita fueron borrados por el equipo).
