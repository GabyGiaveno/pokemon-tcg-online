# SETUP_SPEC — Especificación de configuración y setup

Todo lo necesario para levantar el proyecto desde cero.  
Si algo no funciona siguiendo este documento, el documento está incompleto — arreglarlo.

---

## Prerequisitos

| Herramienta | Versión mínima | Cómo verificar |
|------------|---------------|----------------|
| Java | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` (o usar `./mvnw`) |
| Node.js | 20+ | `node -version` |
| npm | 10+ | `npm -version` |
| Angular CLI | 20.3+ | `ng version` |
| Docker | Cualquier reciente | `docker -version` (solo si usás Docker para la BD) |
| PostgreSQL | 15+ | Solo si no usás Docker |

> El proyecto incluye Maven Wrapper (`BE/mvnw`). Si no tenés Maven instalado, podés usar `./mvnw` en lugar de `mvn`.

---

## Variables de entorno

Crear un archivo `.env` en la raíz del repositorio copiando `.env.example` y completando los valores:

```bash
cp .env.example .env
```

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/pokemon_tcg` |
| `DB_USER` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de la base de datos | `postgres` |
| `JWT_SECRET` | Secreto de firma JWT — mínimo 32 caracteres, aleatorio | Ver nota abajo |
| `POKEMON_TCG_API_KEY` | Clave de la API pokemontcg.io — gratis en el sitio | — |

**Generar un JWT_SECRET seguro**:
```bash
# Linux/Mac
openssl rand -hex 32

# Windows PowerShell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

> En el perfil dev (H2 en memoria), `DB_URL`, `DB_USER` y `DB_PASSWORD` no se usan. Solo necesitás `JWT_SECRET` y `POKEMON_TCG_API_KEY` para desarrollo local básico.

---

## Base de datos

### Opción A — Docker (recomendado)

```bash
docker run \
  --name pokemon-db \
  -e POSTGRES_DB=pokemon_tcg \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:15
```

Para que arranque automáticamente con Docker:
```bash
docker start pokemon-db
```

### Opción B — PostgreSQL local

1. Instalar PostgreSQL 15+.
2. Crear la base de datos:
```sql
CREATE DATABASE pokemon_tcg;
```
3. Configurar las variables de entorno con el usuario y contraseña correctos.

### Migración con Flyway

Flyway corre automáticamente al iniciar el backend con el perfil por defecto (no dev).  
El script de migración está en `BE/src/main/resources/db/migration/V1__initial_schema.sql`.

Para verificar que la migración corre correctamente:
```bash
cd BE
./mvnw flyway:info -Dspring.profiles.active=prod
```

---

## Perfiles de Spring Boot

El proyecto tiene dos perfiles. Se activan con la variable de sistema `spring.profiles.active`.

### Perfil `dev` (default para desarrollo)

- Base de datos: H2 en memoria (no necesita PostgreSQL)
- Flyway: desactivado
- JPA: `ddl-auto=create-drop` (recrea el schema en cada arranque)
- Logs: nivel DEBUG para el código del proyecto y Spring Security
- Consola H2 disponible en `http://localhost:8080/h2-console`

### Perfil por defecto / `prod`

- Base de datos: PostgreSQL (requiere `DB_URL`, `DB_USER`, `DB_PASSWORD`)
- Flyway: activado
- JPA: `ddl-auto=validate` (no modifica el schema — confía en Flyway)
- Logs: nivel WARN para todo, INFO para código del proyecto

---

## Backend (Spring Boot)

### Arrancar en modo dev (H2, sin PostgreSQL)

```bash
cd BE
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Arrancar con PostgreSQL (requiere `.env` configurado)

```bash
cd BE
export $(cat ../.env | xargs)   # Linux/Mac
# En Windows: cargar las variables manualmente o usar una herramienta como dotenv

./mvnw spring-boot:run
```

### URLs disponibles

| URL | Descripción |
|-----|-------------|
| `http://localhost:8080` | API REST |
| `http://localhost:8080/swagger-ui.html` | Swagger UI — explorar todos los endpoints |
| `http://localhost:8080/actuator/health` | Health check |
| `http://localhost:8080/h2-console` | Consola H2 (solo perfil dev) |

### Ejecutar tests

```bash
cd BE
./mvnw test              # Solo tests unitarios
./mvnw verify            # Tests + JaCoCo report
```

El reporte de JaCoCo se genera en `BE/target/site/jacoco/index.html`.

### Verificar calidad de código

```bash
cd BE
./mvnw checkstyle:check  # Estilo de código
./mvnw pmd:check         # Análisis estático
```

---

## Frontend (Angular)

### Instalar dependencias

```bash
cd FE
npm install
```


### Arrancar el servidor de desarrollo

```bash
cd FE
ng serve
```

App disponible en `http://localhost:4200`.  
Hot reload activo — los cambios se reflejan sin reiniciar.

### Ejecutar tests del frontend

```bash
cd FE
ng test              # Modo watch (para desarrollo)
ng test --watch=false  # Una sola ejecución (para CI)
ng test --code-coverage  # Con reporte de cobertura
```

### Build de producción

```bash
cd FE
ng build
```

El output está en `FE/dist/`. Servir con cualquier servidor estático (nginx, Apache, etc.).

---

## Configuración de la integración WebSocket

> Contrato realtime actualizado: ver [`../api/REALTIME_WEBSOCKET.md`](../api/REALTIME_WEBSOCKET.md).
> WebSocket no transporta acciones de juego; solo notifica eventos e invalidaciones.

El frontend se conecta al backend por WebSocket en desarrollo así:

```
Backend:  http://localhost:8080/ws  (SockJS + STOMP)
Frontend: http://localhost:4200
```

CORS está configurado en `CorsConfig.java` para permitir `http://localhost:4200`.

### Topics del WebSocket

| Topic | Quién lo recibe | Contenido |
|-------|----------------|-----------|
| `/topic/games/{gameId}/events` | Participantes de la partida | Eventos discretos para log/animaciones. |
| `/topic/games/{gameId}/state-changed` | Participantes de la partida | Señal para hacer `GET /api/games/{gameId}/state`. |

`/topic/game/{gameId}` y `/topic/game/{gameId}/player/{playerId}` son referencias legacy y no forman parte del contrato realtime actual.

### Enviar acciones

Las acciones de juego **no** se envían por WebSocket. El cliente usa REST:

```text
POST /api/games/{gameId}/actions
```

El servidor valida reglas, persiste estado y publica por WebSocket:

- `/topic/games/{gameId}/events`
- `/topic/games/{gameId}/state-changed`

### Autenticación en WebSocket

El token JWT se envía en el header del frame STOMP CONNECT:

```javascript
// Ejemplo con @stomp/stompjs
const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  connectHeaders: {
    Authorization: `Bearer ${localStorage.getItem('token')}`
  }
});
```

Además, SockJS puede enviar el token como query param `?token=<JWT>` durante el handshake. El backend valida:

- `CONNECT`: JWT válido;
- `SUBSCRIBE`: el usuario autenticado participa en la partida del `gameId`.

---

## Integración con pokemontcg.io

1. Registrarse en [pokemontcg.io](https://pokemontcg.io) para obtener una API key gratuita.
2. Agregar la key en `.env` como `POKEMON_TCG_API_KEY`.
3. Sincronizar el set deseado (ejemplo: set XY1):

```bash
curl -X POST "http://localhost:8080/api/cards/sync?set=xy1" \
  -H "Authorization: Bearer <tu-token>"
```

La sincronización es asíncrona — el endpoint responde 202 inmediatamente y el proceso corre en background.  
Una vez completada, las cartas del set están disponibles en `GET /api/cards?set=xy1`.

**Nota**: durante el juego activo, el backend **nunca** llama a pokemontcg.io. Las cartas viven en la base de datos local.

---

## Variables de application.yml relevantes

### Cambiar el tiempo de expiración del JWT

```yaml
# application.yml
app:
  jwt:
    expiration-ms: 86400000   # 24 horas en milisegundos
```

### Cambiar la base URL de pokemontcg.io

```yaml
pokemon-tcg:
  api:
    base-url: https://api.pokemontcg.io/v2
```

### Cambiar el tipo de cache

```yaml
spring:
  cache:
    type: simple   # default — en memoria, sin configuración
    # type: caffeine  # alternativa con TTL configurable
```

---

## Solución de problemas comunes

### El backend no arranca: "Could not connect to database"
- Verificar que PostgreSQL está corriendo: `docker ps` o `pg_isready`.
- Verificar que las variables `DB_URL`, `DB_USER`, `DB_PASSWORD` están seteadas.
- Alternativa: arrancar con perfil dev (H2): `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.

### El backend no arranca: "Flyway migration failed"
- Revisar que el schema SQL en `V1__initial_schema.sql` es compatible con la versión de PostgreSQL.
- Los ENUMs de PostgreSQL (`game_status`, `turn_phase`, etc.) deben crearse antes de las tablas que los usan.
- Solución de emergencia (solo en dev): borrar la BD y recrearla — Flyway reintenta desde cero.

### El frontend no puede conectarse al backend (CORS)
- Verificar que el backend está corriendo en `http://localhost:8080`.
- Verificar que `CorsConfig.java` permite `http://localhost:4200`.
- Verificar que el frontend usa la URL correcta del backend en los services.

### npm install falla: "@angular/cdk version not found"
- El CDK en la serie Angular 20 solo tiene versiones `20.0.x`. Usar `^20.0.0`, no `^20.3.0`.
- Ya fue corregido en el `package.json` del proyecto.

### El WebSocket no conecta: "401 Unauthorized"
- El token JWT debe incluirse en el header STOMP CONNECT (ver sección de autenticación WS).
- Verificar que el token no está expirado (24 horas de vida por defecto).

### JaCoCo no genera el reporte
- JaCoCo está configurado para correr en la fase `verify`, no en `test`.
- Usar `./mvnw verify` en lugar de `./mvnw test`.
- El reporte se genera en `BE/target/site/jacoco/index.html`.
