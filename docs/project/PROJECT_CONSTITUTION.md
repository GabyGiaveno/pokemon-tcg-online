# PROJECT_CONSTITUTION — Pokémon TCG Digital

Fuente de verdad para principios, arquitectura, stack y convenciones.
Cualquier código que viole estas reglas se revierte.

---

## Visión

Implementar un juego digital de cartas Pokémon TCG para dos jugadores en tiempo real,
siguiendo el **Reglamento XY1 oficial** como única fuente de reglas.

---

## Principios arquitectónicos (no negociables)

### 1. El backend es la única fuente de verdad

El estado del juego vive en la base de datos. El frontend recibe snapshots — nunca calcula ni modifica estado local.

Prohibido en Angular:
- Decidir si una acción es válida
- Calcular daño, aplicar condiciones especiales o verificar victorias
- El frontend envía intenciones (`POST /api/games/{id}/actions`) y espera el resultado del backend

### 2. El Game Engine es Java puro

El paquete `engine/` no tiene dependencias de Spring Framework.

Prohibido dentro de `engine/`:
- `@Component`, `@Service`, `@Autowired`, `@Inject`
- `@Transactional`
- Cualquier `Repository` o acceso a BD
- `ApplicationContext` o cualquier bean de Spring

Excepción única: las implementaciones de `GamePhaseState` pueden ser `@Component` stateless (el estado vive en `BoardState`, no en el componente).

Razón: el engine debe testearse en JUnit sin levantar Spring.

### 3. GameAction es inmutable

Ninguna fila de `game_action` se actualiza. Solo INSERT.
Todos los campos de `GameAction.java` tienen `@Column(updatable=false)`.

### 4. La mano del oponente nunca se transmite completa

`BoardStateDTOMapper` filtra el estado antes de serializar. El cliente recibe `handSize: int`, nunca `hand: List<String>`.

### 5. El orden del mazo nunca es visible

La lista interna del mazo existe para mantener el orden de robo. Los DTOs exponen solo `deckSize: int`.

---

## Arquitectura de capas

### Backend

```
Controller → Service → GameEngineFacade (aislado) → Repository → ExternalServices
```

| Capa | Responsabilidad |
|------|----------------|
| `Controller` | Recibe HTTP/WS, valida formato, delega al Service |
| `Service` | Orquesta casos de uso, gestiona `@Transactional` |
| `GameEngineFacade` | Única interfaz pública del motor — oculta sub-servicios |
| `Repository` | Acceso a datos vía Spring Data JPA |
| `ExternalServices` | Integración con pokemontcg.io (solo carga/sincronización) |

Sub-servicios internos del engine (todos sin Spring):

| Sub-servicio | Responsabilidad |
|-------------|----------------|
| `TurnManager` | Control de fases y flags por turno |
| `AttackResolutionChain` | Pipeline de 7 pasos (Chain of Responsibility) |
| `CardEffectService` | Strategy de efectos de ataques y cartas Entrenador |
| `BoardStateDTOMapper` | Genera DTOs filtrados por jugador |
| `VictoryConditionChecker` | Evalúa las 3 condiciones de victoria |
| `KnockoutProcessor` | Secuencia de KO: descarte, Prize Cards, solicitud de CHOOSE_ACTIVE |
| `StatusEffectManager` | Resolución de condiciones especiales entre turnos |
| `DamageCalculator` | Fórmula de daño XY1 en orden estricto |

### Frontend

```
Components → Services (HTTP + WS) → State (Signals + RxJS)
```

| Capa | Responsabilidad |
|------|----------------|
| `Components` | Presentación pura; emite eventos, nunca calcula resultados |
| `Services` | HTTP (REST) + WebSocket (STOMP); traducen protocolo a/desde state |
| `State` | Angular Signals + RxJS; proyecta estado recibido del backend |

---

## Stack tecnológico y versiones

| Tecnología | Versión | Notas |
|-----------|---------|-------|
| Java | 21 | Usar records, sealed classes y pattern matching donde aplique |
| Spring Boot | 4.0.0 | Parent del pom.xml |
| Angular | 20.3.x | Standalone components, signals, OnPush |
| PostgreSQL | 15+ | Base de datos de producción |
| H2 | — | Solo para tests y perfil `dev` (sin Flyway) |
| Maven | 3.9+ | Wrapper incluido (`mvnw`) |
| Node.js | 20+ | Runtime del frontend |
| Angular CDK | 20.0.x | Para drag & drop en deck builder — solo llegó a 20.0.4 en serie 20 |
| jjwt | 0.12.6 | Librería JWT (HS256) |
| Flyway | (gestionado por Spring Boot) | Solo corre en perfil prod (PostgreSQL) |
| JaCoCo | 0.8.12 | Cobertura — corre en fase `verify` |
| WebClient (webflux) | (gestionado por Spring Boot) | Para llamadas a pokemontcg.io; requiere `spring.main.web-application-type=servlet` |

---

## Patrones de diseño aplicados

| Patrón | Componente | Razón |
|--------|------------|-------|
| **State** | `GameSession` (WAITING/SETUP/ACTIVE/FINISHED) + `TurnPhase` (DRAW/MAIN/ATTACK/BETWEEN_TURNS) | Ciclo de vida discreto — previene transiciones inválidas |
| **Strategy** | Efectos de ataques y cartas Entrenador | Cada carta tiene comportamiento único — elimina el switch gigante |
| **Chain of Responsibility** | Pipeline de resolución de ataque — 7 pasos | El ataque recorre validaciones y cálculos en orden fijo e interrumpible |
| **Observer** | Eventos → `ApplicationEventPublisher` → WebSocket STOMP → clientes | El engine publica eventos sin acoplamiento al transporte |
| **Repository** | Spring Data JPA para todas las entidades | Abstrae SQL, facilita tests con H2 |
| **Facade** | `GameEngineFacade` — único punto de entrada al motor | Oculta los 7 sub-servicios internos |

---

## Seguridad

### Autenticación

| Regla | Implementación |
|-------|---------------|
| Algoritmo | JWT stateless, HS256 |
| Secreto | Variable de entorno `JWT_SECRET` (mínimo 32 chars) |
| Expiración | 24 horas |
| Endpoints protegidos | Todos excepto `POST /api/auth/**` requieren `Authorization: Bearer <token>` |
| WebSocket | Token validado en frame STOMP CONNECT por `JwtChannelInterceptor` |

### Autorización

- Un jugador solo puede acceder a sus propios mazos y a las sesiones donde participa.
- El backend verifica ownership antes de cualquier escritura → 403 si falla.
- El frontend no implementa lógica de autorización — confía en los 403 del backend.

### Datos sensibles

| Dato | Regla |
|------|-------|
| Mano del oponente | Nunca el contenido — solo `handSize: int` |
| Orden del mazo | Nunca visible — solo `deckSize: int` |
| Prize Cards | Ocultas hasta que se toman; se revelan en evento `PRIZE_TAKEN` |
| Contraseñas | BCrypt, nunca texto plano |
| Secreto JWT | Variable de entorno, nunca en el código |

---

## Performance

| Operación | Límite |
|-----------|--------|
| Acciones de juego (POST /api/games/{id}/actions) | < 200ms |
| Búsqueda de cartas (GET /api/cards) | < 500ms |
| Sincronización pokemontcg.io | Asíncrona — responde 202 inmediatamente |

El motor de juego **no llama** a pokemontcg.io en runtime — opera exclusivamente sobre el caché local.

---

## Reglas de código

### Java

- Usar Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) en entidades JPA.
- Preferir Java records para modelos inmutables de dominio (`BoardState`, `PlayerField`, DTOs).
- Usar `sealed classes` e `instanceof` con pattern matching (Java 21) donde aplique.
- Sin `@SuppressWarnings("unchecked")` sin comentario que explique el motivo.
- Checkstyle y PMD corren en el build — no ignorarlos.

### Angular

- **Standalone components**: nunca `NgModule`. `standalone: true` es el default (no escribirlo explícitamente).
- **Signals**: `signal()` para estado local, `computed()` para derivado, `effect()` con cuidado.
- **`ChangeDetectionStrategy.OnPush`** en todos los componentes.
- **`inject()`** en lugar de constructor injection.
- **Control flow nativo**: `@if`, `@for`, `@switch` — nunca `*ngIf`, `*ngFor`.
- **`input()` y `output()`** en lugar de `@Input()` y `@Output()`.
- Sin `ngClass` — usar class bindings. Sin `ngStyle` — usar style bindings.

---

## Testing

### Cobertura (JaCoCo — corre en `mvn verify`)

| Scope | Mínimo |
|-------|--------|
| Engine (`GameEngine`, `DamageCalculator`, `StatusEffectManager`, `RuleValidator`) | ≥ 70% |
| Services | ≥ 60% |

### Tipos de tests requeridos

| Tipo | Herramienta | Scope obligatorio |
|------|-------------|------------------|
| Unitarios | JUnit 5 + Mockito | Lógica aislada — engine sin Spring |
| Integración | `@DataJpaTest` + H2 o Testcontainers | Repositorios; flujo completo de partida, mulligan, evolución, KO, victoria |
| E2E Frontend | Angular Testing | Crear mazo, unirse a partida, ejecutar 1 turno |

Regla: el engine se testea sin Spring. Si un test del engine necesita `@SpringBootTest`, algo está mal.

---

## Calidad de build

Orden de ejecución en Maven:

1. **Checkstyle** (`maven-checkstyle-plugin`) — configuración en `.code_quality/checkstyle_rules.xml`
2. **PMD** (`maven-pmd-plugin`) — configuración en `.code_quality/pmd_rules.xml`
3. **Tests** (`mvn test`) — JUnit 5 + Mockito
4. **JaCoCo report** (`mvn verify`) — cobertura generada en `BE/target/site/jacoco/index.html`

Si cualquiera falla, el build falla. No se hace merge con el build roto.

---

## Flujo de trabajo Git

```
main
  └── feature/dev1-foundation
  └── feature/dev2-auth
  └── feature/dev3-cards-decks
  └── feature/dev4-engine
  └── feature/dev5-game-session-ws
  └── feature/dev6-frontend
```

- Una rama por developer. Si necesitás trabajar sobre algo de otra rama, coordinás.
- PR a `main` cuando la feature está completa y los tests pasan. Mínimo 1 review.
- No hacer force push a `main`.

### Convención de commits

```
feat: implementar validación de mazo (DeckService)
fix: corregir cálculo de debilidad cuando hay resistencia
test: agregar tests de TurnManager para primer turno
refactor: extraer BoardStateDTOMapper de GameService
docs: actualizar ENGINE_SPEC con reglas de KO
chore: actualizar dependencias Maven
```

---

## Decisiones técnicas registradas

| Decisión | Alternativa descartada | Motivo |
|----------|----------------------|--------|
| H2 en dev, PostgreSQL en prod | PostgreSQL en todos los ambientes | Setup local sin Docker obligatorio |
| JSONB guardado como `String` + Jackson | Hibernate Types (hypersistence-utils) | Menos dependencias, alcance del TPI |
| JWT stateless | HTTP Sessions | No requiere estado en servidor |
| WebClient (webflux) para pokemontcg.io | RestTemplate | RestTemplate deprecado en Spring 6+ |
| `@angular/cdk@^20.0.0` | `^20.3.0` | CDK solo llegó a 20.0.4 en la serie Angular 20 |
| `spring-boot-starter-webflux` agregado solo para WebClient | — | No reemplaza el stack servlet — requiere `spring.main.web-application-type=servlet` |
| `BoardStateDTOMapper` centraliza el filtrado | Filtrar en cada controller | Un solo punto de falla para la regla de privacidad |
| Engram (memoria persistente) para SDD artifacts | Archivos en openspec/ | Solo dev, sin archivos comiteados adicionales |

---

## Seed data requerido

La migración debe incluir datos de seed:
- Al menos **1 mazo temático completo y válido** según set XY1 (60 cartas, reglas cumplidas).
- El seed permite verificar manualmente el deck builder y la sala de espera sin importar cartas.

---

## TODOs pendientes

- [ ] **Rate limiting pokemontcg.io**: definir estrategia de backoff si la API devuelve 429.
- [x] **Trigger Muerte Súbita**: KO simultáneo de ambos Pokémon Activos en el mismo turno → ver `ENGINE_SPEC.md §Muerte Súbita`.
- [x] **Mecanismo primer jugador**: coin flip — el ganador elige quién empieza → ver `ENGINE_SPEC.md §Setup`.

---

## Bugs conocidos en V1__initial_schema.sql

| Bug | Descripción | Fix requerido |
|-----|-------------|---------------|
| Columna `password_hash` ausente en tabla `player` | La entidad JPA `Player` tiene `passwordHash` pero el SQL no tiene la columna | Agregar `password_hash VARCHAR(255) NOT NULL` a la tabla `player` |
