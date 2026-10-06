# Pokémon TCG Digital — TPI Programación III · UTN FRC

Sistema digital multijugador del juego de cartas **Pokémon TCG** para dos jugadores en tiempo real, desarrollado como Trabajo Práctico Integrador para la cátedra de Programación III de la UTN Facultad Regional Córdoba.

Reglamento implementado: **XY1 (XY Base Set) Rulebook oficial**.

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 4.0.0 · Spring Security · Spring WebSocket (STOMP) |
| Base de datos | H2 (desarrollo local) · PostgreSQL 15 (producción) · Flyway (migraciones) |
| Cache | Caffeine (TTL 24h, warm on startup) |
| Cliente externo | WebFlux `WebClient` → pokemontcg.io API |
| Frontend | Angular 20.3 · TypeScript 5.9 · RxJS 7.8 · Signals |
| WebSocket cliente | `@stomp/stompjs` 7.0 + SockJS |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Calidad de código | Checkstyle · PMD · JaCoCo 0.8.12 |
| Build | Maven Wrapper · Angular CLI 20.3 |

---

## Prerrequisitos

| Herramienta | Versión mínima |
|---|---|
| Java (JDK) | **21** (Spring Boot 4 no soporta versiones anteriores) |
| Node.js | 20+ |
| Angular CLI | 20.3+ (`npm install -g @angular/cli`) |
| Docker Desktop | Cualquier versión reciente (solo para PostgreSQL en producción) |

---

## Setup local (modo recomendado — H2, sin PostgreSQL)

### 1. Backend

Desde la raíz del repositorio, en **PowerShell**:

```powershell
./run-local.ps1
```

El script automatiza todo:
- Crea `.env` desde `.env.example` si no existe
- Crea `BE/src/main/resources/application-local.yml` desde la plantilla si no existe
- Carga las variables de entorno desde `.env`
- Detecta y usa un JDK 21 aunque el `java` por defecto sea otro
- Arranca el backend con perfil `local` → **base H2 en memoria, no requiere PostgreSQL**

URLs disponibles con el backend corriendo:

| Recurso | URL |
|---|---|
| API REST | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Consola H2 | `http://localhost:8080/h2-console` |
| Health check | `http://localhost:8080/ping` |

> Tanto `.env` como `application-local.yml` están en `.gitignore` y nunca se commitean.

### 2. Frontend

```bash
cd FE
npm install
ng serve
```

App disponible en `http://localhost:4200`.

### 3. Mail de recuperación de contraseña (opcional — NO requerido para evaluar)

**El proyecto se levanta y funciona al 100% sin configurar mail.** Cuando el mail no está
configurado, el backend **imprime el correo de recuperación —incluido el link— directamente
en la consola**:

```
=== EMAIL (mail not configured) ===
To: usuario@ejemplo.com
Body: ... https://localhost:4200/reset-password?token=... ...
=== END EMAIL ===
```

Para probar el flujo de recuperación de contraseña basta con copiar ese link desde la consola.
No hace falta ninguna credencial.

#### Envío real por Gmail (opcional)

Si querés que el correo llegue a un inbox real, editá tu `.env` local (que **nunca** se
commitea) y completá:

```env
MAIL_USERNAME=...
MAIL_PASSWORD=...   # App Password de Gmail (16 caracteres), NO la contraseña normal
```

Generá la App Password en `https://myaccount.google.com/apppasswords` (requiere verificación
en 2 pasos activa).

> **Nota de seguridad:** las credenciales de mail nunca se publican en este repositorio.
> Un App Password en GitHub queda en el historial de git de forma permanente y es revocado
> automáticamente al detectarse filtrado. Los integrantes del grupo comparten las credenciales
> de la cuenta del proyecto por su canal interno; la cátedra las recibe en el informe de entrega.

---

## Setup con PostgreSQL (perfil producción)

> **Requisito:** Docker Desktop corriendo antes de ejecutar el comando.

Desde la raíz del repositorio, en **PowerShell**:

```powershell
.\run-local.ps1 -Profile prod
```

El script automatiza todo:

1. Levanta el container `pokemon-tcg-postgres` (PostgreSQL 15-alpine, puerto 5432) con `docker compose up -d`
2. Crea `.env` desde `.env.example` si no existe y carga las variables de entorno
3. Detecta y usa un JDK 21 aunque el `java` por defecto sea otro
4. Arranca el backend con perfil `prod` → apunta a `localhost:5432/pokemon_tcg` con credenciales del compose (`pokemon_user` / `pokemon_pass`)
5. Flyway ejecuta las migraciones automáticamente al iniciar

> **Nota:** si tenés PostgreSQL instalado localmente en tu máquina (no Docker), el puerto 5432 va a estar ocupado y la conexión va a fallar. Pará el servicio local antes de correr el script:
>
> ```powershell
> # PowerShell como Administrador:
> Get-Service *postgres*              # ver el nombre del servicio
> Stop-Service postgresql-x64-XX      # reemplazar XX por tu versión
> ```

### Detener el container

```powershell
cd BE
docker compose down      # detiene y preserva datos
docker compose down -v   # detiene y borra datos (reset completo)
```

### Variables de entorno (deploy real)

| Variable | Descripción |
|---|---|
| `DB_URL` | JDBC URL de PostgreSQL |
| `DB_USER` / `DB_PASSWORD` | Credenciales de la base |
| `JWT_SECRET` | Secreto de firma JWT (mínimo 32 caracteres) |
| `FRONTEND_URL` | Base del link de recuperación (ej. `https://miapp.com`) |
| `MAIL_HOST` / `MAIL_PORT` | Servidor SMTP |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Credenciales SMTP |
| `POKEMON_TCG_API_KEY` | API key de pokemontcg.io (opcional) |

---

## Correr tests y cobertura

```bash
cd BE
./mvnw.cmd verify
```

Esto ejecuta todos los tests y genera el reporte JaCoCo.
El build **falla** si la cobertura total cae por debajo del **85%**.

Reporte HTML: `BE/target/site/jacoco/index.html`

---

## Estructura del repositorio

```
tpi-pokemon-2w1-03/
├── BE/                          # Spring Boot backend (Java 21)
│   ├── src/main/java/ar/edu/utn/frc/tup/piii/
│   │   ├── controllers/         # REST controllers (Auth, Card, Deck, Game, Player)
│   │   ├── services/            # Lógica de negocio
│   │   ├── engine/              # Motor de juego TCG (turnos, ataques, efectos)
│   │   ├── entities/            # JPA entities (22 tablas)
│   │   ├── dtos/                # Request / Response DTOs
│   │   ├── repositories/        # Spring Data JPA
│   │   ├── configs/             # CORS, JWT, Security, WebSocket, SpringDoc
│   │   ├── security/            # JWT filter, CurrentPlayerService
│   │   └── events/              # Sistema de eventos (35 tipos)
│   ├── src/main/resources/
│   │   ├── application.yml                  # Config base (lee vars de entorno)
│   │   ├── application-local.example.yml    # Plantilla perfil local (copiar y renombrar)
│   │   ├── application-dev.yml              # Perfil dev con H2
│   │   ├── application-prod.yml             # Perfil producción con PostgreSQL
│   │   ├── data/xy1_parsed.json             # Dataset de cartas XY1 parseado
│   │   └── db/migration/                    # Flyway V1–V013
│   └── pom.xml
├── FE/                          # Angular 20.3 frontend
│   └── src/app/
│       ├── core/                # Guards, interceptors, WebSocket service
│       ├── shared/              # Componentes reutilizables, directivas, pipes
│       └── features/
│           ├── auth/            # Login, register, recuperación de contraseña
│           ├── deck-builder/    # Constructor de mazos
│           ├── game/            # Board en tiempo real, acciones de juego
│           ├── pokedex/         # Explorador de cartas
│           ├── profile/         # Perfil de jugador, badges, achievements
│           └── rules/           # Página de reglas
├── docs/                        # Documentación canónica (ver docs/README.md)
│   ├── api/                     # Especificaciones de API REST y WebSocket
│   ├── architecture/            # ADRs (Architecture Decision Records)
│   ├── engine/                  # Especificación y guía del motor de juego
│   ├── frontend/                # Specs de UI y conectividad
│   └── project/                 # Requerimientos, constitución del proyecto
├── ARCHITECTURE.md              # Visión general de arquitectura
├── CHANGELOG.md                 # Historial de versiones
├── run-local.ps1                # Script de arranque local (un comando)
└── .env.example                 # Plantilla de variables de entorno
```

---

## Funcionalidad backend

### Autenticación y seguridad (`/api/auth`)

- **Registro** de jugadores con validación de username/email únicos y hash bcrypt de contraseñas
- **Login** con generación de JWT (firmado HS256, expira en 24h)
- **Recuperación de contraseña** por email: genera token de un uso con TTL, envía link por SMTP
- **Reset de contraseña** con validación del token (caducidad + uso único)
- Todas las rutas protegidas usan `Authorization: Bearer <token>` via `JwtAuthenticationFilter`
- `CurrentPlayerService` extrae el ID del jugador del contexto de seguridad en cualquier controller

### Cartas (`/api/cards`)

- Búsqueda paginada en cache local con filtros: nombre, set, supertype, tipo
- Cache Caffeine con TTL de 24 horas, warm on startup desde `data/xy1_parsed.json`
- Sincronización bajo demanda desde `pokemontcg.io/v2` (endpoint de administración)
- El diseño de "cache first" evita dependencia de disponibilidad del API externo en tiempo de juego

### Mazos (`/api/decks`)

- CRUD completo de mazos para el jugador autenticado
- Validación en 4 capas (pipeline Chain of Responsibility):
  1. `ExactSizeValidator` — exactamente 60 cartas
  2. `CopyLimitValidator` — máximo 4 copias de la misma carta (excepto Energías básicas)
  3. `AceTacticianValidator` — regla de soporte Ace Spec: máximo 1 por mazo
  4. `BasicPokemonValidator` — al menos 1 Pokémon básico
- Un mazo de inicio por defecto se provisiona automáticamente al registrarse

### Juego en tiempo real (`/api/games` + WebSocket)

- Creación de sala y espera en lobby (`WAITING`)
- Join de sala con mazo propio
- Setup: ambos jugadores marcan "ready" → se ejecuta flip de moneda → ganador elige quién va primero
- Acciones de juego via HTTP POST, notificaciones de cambio via WebSocket
- El estado del board es **filtrado por jugador**: cada uno ve su mano completa pero del rival solo ve la mano oculta (cantidad de cartas)
- Log de acciones inmutable (`game_action`) para auditoría y replay

### Perfil del jugador (`/api/players`)

- Perfil con estadísticas: victorias, derrotas, racha, torneos ganados
- Sistema de badges y achievements (desbloqueables)
- Skins de entrenador (color de sombrero, camisa, pantalón, tono de piel)
- Items de customización por categoría: ropa, accesorios, poses, fondos

### Motor de juego (engine)

El motor implementa el reglamento XY1 completo. Arquitectura de capas:

```
GameEngineFacade
    └── TurnManager           (orquesta fases: DRAW → MAIN → ATTACK → BETWEEN_TURNS)
         ├── RuleValidator     (valida cada acción según fase y estado)
         ├── AttackResolutionChain  (7 handlers encadenados)
         │    ├── EnergyValidationHandler
         │    ├── ConfusionCheckHandler
         │    ├── SelectionsHandler
         │    ├── PreAttackHandler
         │    ├── ModifierHandler
         │    ├── DamageApplicationHandler   (aplica debilidad ×2, resistencia −20)
         │    └── PostDamageHandler
         ├── KnockoutProcessor (KO, premios —2 por EX/MEGA—, promoción)
         ├── VictoryConditionChecker
         └── StatusEffectManager (entre turnos: quemadura, sueño, parálisis, veneno)
```

**Sistema de efectos (Flyweight):** `EffectRegistry` mapea claves de efecto de ataque → implementaciones `EffectLogic`. Los efectos no tienen estado propio — se aplican sobre el `BoardState` recibido. Eso permite que la misma instancia de efecto procese miles de ataques sin crear objetos.

**Efectos de entrenadores:** `TrainerResolutionChain` con 9 logics específicas para soporters, items y estadios.

**Efectos pasivos:** `PassiveEffectRegistry` con stadiums (`FairyGarden`, `ShadowCircle`) y tools (`HardCharm`, `MuscleBand`).

**Persistencia del estado:** el `BoardState` completo se serializa como JSONB en `game_state`. Una sola row por partida, actualizada atómicamente en cada acción.

### WebSocket (STOMP)

Endpoint: `ws://localhost:8080/ws` (con fallback SockJS)

Autenticación: JWT en el header `Authorization: Bearer <token>` del frame `CONNECT`, o como query param `?token=`.

| Suscripción | Descripción |
|---|---|
| `/topic/games/{id}/events` | Eventos de juego (35 tipos: `ATTACK_DECLARED`, `POKEMON_KNOCKED_OUT`, `PRIZE_TAKEN`, `PHASE_CHANGED`, `GAME_FINISHED`, etc.) |
| `/topic/games/{id}/state-changed` | Notificación de cambio de estado — el cliente debe hacer `GET /api/games/{id}/state` |
| `/topic/game/pong` | Respuesta al ping de conectividad |

---

## Funcionalidad frontend

### Arquitectura Angular

- **Signals + RxJS**: estado local con signals Angular, streams reactivos para WebSocket y HTTP
- **Mock/Real pattern**: cada feature tiene un `*HttpService` (real) y un `*MockService` (desarrollo sin backend), intercambiables via token de inyección `APP_DATA_MODE`
- **Interceptores**: `JwtInterceptor` (agrega Bearer token), `HttpErrorInterceptor` (manejo centralizado)
- **Guard**: `authGuard` protege todas las rutas que requieren sesión activa

### Páginas principales

| Ruta | Descripción |
|---|---|
| `/auth/login` | Login con validación inline |
| `/auth/register` | Registro con indicador de fortaleza de contraseña |
| `/auth/forgot-password` | Solicitud de recuperación |
| `/auth/reset-password` | Reset con token (llegado por mail) |
| `/lobby` | Lista de partidas esperando jugadores |
| `/decks` | Lista de mazos del jugador |
| `/decks/new` y `/decks/:id/edit` | Constructor de mazos (drag & drop, validación en tiempo real) |
| `/pokedex` | Explorador de cartas con filtros, paginación, preview holográfico 3D |
| `/game/**` | Board de juego en tiempo real |
| `/perfil` | Perfil, badges, achievements, customización del entrenador |
| `/reglas` | Página de reglas del juego |

### Board de juego

- Zonas: Activo, Banca (5 slots), Mazo, Descarte, Premios, Estadio, zona de herramienta
- Efecto holográfico 3D en cartas con `vanilla-tilt` y `HoloPointerDirective`
- `BoardAnimationService`: lunge de ataque, flash de KO, números de daño flotantes
- `GameFeedService`: buffer de 500 eventos con 35 tipos para el historial de la partida
- `WebSocketStompService`: reconexión automática con backoff exponencial (5 intentos, `1000ms × intento`)

---

## API REST — Documentación técnica

La documentación interactiva completa está disponible en **Swagger UI** con el backend corriendo:

```
http://localhost:8080/swagger-ui.html
```

### Decisiones de diseño justificadas

**1. HTTP para acciones de juego, WebSocket solo para notificaciones**

Las acciones de juego (`POST /api/games/{id}/actions`) van por HTTP. El WebSocket solo notifica que el estado cambió (`state-changed`). El cliente entonces llama `GET /api/games/{id}/state` para obtener el estado filtrado.

*Justificación:* Las acciones tienen semántica request/response clara (éxito/fallo con validación de reglas). HTTP garantiza entrega, tiene retry automático del browser, y el estado filtrado por jugador es más fácil de calcular server-side en el momento del GET que de trackear en la sesión WebSocket.

**2. Estado del board como JSONB**

El `BoardState` completo se guarda como una sola columna JSONB en `game_state`, una row por partida.

*Justificación:* El estado del TCG es un grafo de objetos interdependientes (mazos, manos, zonas, contadores de daño, efectos de estado). Normalizarlo a tablas requeriría docenas de JOINs para reconstruirlo en cada acción — O(n) queries vs O(1). JSONB en PostgreSQL permite queries sobre campos internos si fuera necesario para reporting, sin sacrificar la simplicidad de lectura/escritura.

**3. Cache Caffeine "cache first" para cartas**

Las cartas se cargan de `xy1_parsed.json` al startup y se mantienen en Caffeine con TTL 24h. El endpoint de sync de pokemontcg.io existe pero no se invoca en el flujo de juego.

*Justificación:* Las cartas del set XY1 son inmutables (no cambian con el tiempo). Una dependencia de red en el path de búsqueda de cartas crearía latencia variable y riesgo de disponibilidad en partidas en curso. El warm-on-startup garantiza que el primer usuario no paga el costo de carga.

**4. JWT stateless vs sesiones**

*Justificación:* El sistema no necesita invalidación server-side en el flujo normal (logout es solo borrar el token en el cliente). JWT stateless escala horizontalmente sin shared session store. El `JWT_SECRET` se configura externamente para rotación sin redeploy.

**5. Flyway para migraciones**

*Justificación:* Las migraciones versionadas permiten que distintos environments (local H2, dev, prod PostgreSQL) partan del mismo schema. Rollback controlado por convención de naming (`V001__`, `V002__`, etc.). La integración con Spring Boot hace que las migraciones corran automáticamente al iniciar.

**6. Chain of Responsibility para validación de mazos y resolución de ataques**

*Justificación:* Las reglas de TCG son additive — el reglamento XY1 puede agregar capas de validación sin modificar las existentes. Cada handler en la chain tiene responsabilidad única, es testeable en aislamiento, y el orden de ejecución es explícito. Alternativa rechazada: mega-service con múltiples métodos — difícil de extender y testear.

**7. Flyweight para efectos de ataques**

*Justificación:* Hay cientos de cartas con efectos pero solo ~13 tipos de lógica distintos. Instanciar una clase por carta sería innecesario. `EffectRegistry` mapea claves string → instancias singleton de `EffectLogic`. El estado del efecto vive en el `BoardState` del juego, no en la lógica del efecto.

### Resumen de endpoints

#### `POST /api/auth/register`
Registra un nuevo jugador. Provisiona automáticamente un mazo de inicio.

**Request body:**
```json
{ "username": "ash", "email": "ash@pokemon.com", "password": "Pikachu123!" }
```
**Response 201:**
```json
{ "id": 1, "username": "ash", "token": "eyJhbGc..." }
```

---

#### `POST /api/auth/login`
Login de jugador existente.

**Request body:**
```json
{ "username": "ash", "password": "Pikachu123!" }
```
**Response 200:**
```json
{ "id": 1, "username": "ash", "token": "eyJhbGc..." }
```

---

#### `POST /api/auth/forgot-password`
Solicita un email de recuperación. Siempre responde `200` (evita enumeración de emails).

#### `POST /api/auth/reset-password`
Restablece la contraseña con token de un solo uso.

---

#### `GET /api/cards?name=&set=&supertype=&type=&page=0&size=20`
Búsqueda paginada de cartas en el cache local.

**Response 200:**
```json
{
  "data": [ { "id": "xy1-1", "name": "Venusaur-EX", "supertype": "Pokémon", ... } ],
  "total": 146, "page": 0, "size": 20
}
```

---

#### `GET /api/decks` · `POST /api/decks` · `PUT /api/decks/{id}` · `DELETE /api/decks/{id}`
CRUD de mazos del jugador autenticado. Requiere `Authorization: Bearer <token>`.

#### `POST /api/decks/{id}/validate`
Valida un mazo contra las reglas. Devuelve `valid: true/false` con lista de errores detallados.

---

#### `POST /api/games`
Crea una sala de juego en estado `WAITING`.

**Request body:**
```json
{ "deckId": 3 }
```

#### `GET /api/games`
Lista partidas en estado `WAITING` disponibles para unirse.

#### `POST /api/games/{id}/join`
El segundo jugador se une a la partida.

#### `POST /api/games/{id}/ready`
Marca al jugador como listo. Cuando ambos están listos se ejecuta el coin flip.

#### `POST /api/games/{id}/choose-first`
El ganador del coin flip elige quién toma el primer turno.

#### `GET /api/games/{id}/state`
Devuelve el estado del board filtrado para el jugador autenticado (su mano completa, mano del rival oculta).

#### `POST /api/games/{id}/actions`
Ejecuta una acción de juego. Los errores de reglas retornan `success: false`, no errores HTTP.

**Request body:**
```json
{
  "actionType": "ATTACK",
  "attackIndex": 0,
  "selections": []
}
```

**Response 200:**
```json
{
  "success": true,
  "events": [
    { "type": "ATTACK_DECLARED", "payload": { ... } },
    { "type": "DAMAGE_APPLIED", "payload": { "damage": 90 } }
  ]
}
```

---

## Cobertura de tests

Los tests se ejecutan con `./mvnw.cmd verify`. JaCoCo genera el reporte en `BE/target/site/jacoco/index.html`.

El build **falla automáticamente** si la cobertura de instrucciones del proyecto cae por debajo del **85%**.

Cobertura por área:

| Área | Descripción |
|---|---|
| Engine | TurnManager, KnockoutProcessor, VictoryConditionChecker, StatusEffectManager, AttackResolutionChain, todos los EffectLogics y TrainerLogics |
| Services | PlayerService, DeckService, GameService, CardCacheService, RecoveryService |
| Controllers | AuthController, CardController, DeckController, GameController, PlayerController |
| Validators | ExactSizeValidator, CopyLimitValidator, AceTacticianValidator, BasicPokemonValidator |
| Integración | AuthIntegrationTest, WebSocketHandshakeAuthIntegrationTest |

---

## Documentación adicional

| Documento | Descripción |
|---|---|
| [`docs/README.md`](docs/README.md) | Índice completo de documentación |
| [`docs/engine/ENGINE_GUIDE.md`](docs/engine/ENGINE_GUIDE.md) | Guía del motor de juego |
| [`docs/api/REALTIME_WEBSOCKET.md`](docs/api/REALTIME_WEBSOCKET.md) | Protocolo WebSocket completo |
| [`docs/architecture/`](docs/architecture/) | ADRs de decisiones de arquitectura |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Visión general de arquitectura |
| [`AGENTS.md`](AGENTS.md) | Contexto operativo para agentes IA |

---

## Equipo

Trabajo Práctico Integrador — Programación III · UTN FRC · 2W1 Grupo 03
