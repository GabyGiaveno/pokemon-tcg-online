# WebSocket realtime — contrato final

WebSocket es el canal de notificación en tiempo real para que dos jugadores mantengan sincronizada la partida sin que el frontend calcule reglas ni reconstruya estado localmente.

## Resumen rápido

| Tema | Decisión final |
|------|----------------|
| Propósito | Sincronización de partida en tiempo real. |
| Acciones del jugador | Siempre por REST: `POST /api/games/{gameId}/actions`. |
| Fuente de verdad | Backend + estado persistido. |
| WebSocket envía estado completo | No. Solo eventos discretos e invalidaciones. |
| Frontend calcula reglas | No. Solo renderiza, anima y pide snapshots REST. |
| Endpoint WS | `/ws` con STOMP sobre SockJS. |
| Topics oficiales | `/topic/games/{gameId}/events`, `/topic/games/{gameId}/state-changed`. |

## Flujo principal

```text
Jugador hace acción por REST
→ backend valida JWT y reglas
→ GameService / GameEngineFacade aplican la acción
→ backend persiste el nuevo estado
→ backend publica eventos WebSocket
→ frontend recibe /events para log o animaciones
→ frontend recibe /state-changed
→ frontend hace GET /api/games/{gameId}/state
→ cada jugador recibe estado filtrado según permisos
```

El punto importante: **`state-changed` invalida el estado local, no lo reemplaza**. El estado real vuelve por REST.

## Contrato WebSocket

### Conexión

```text
Endpoint: /ws
Protocolo: STOMP sobre SockJS
Broker prefix: /topic
App prefix: /app
```

El cliente actual envía el JWT de dos formas compatibles con SockJS/STOMP:

1. Query param SockJS: `/ws?token=<JWT>`
2. Header STOMP CONNECT: `Authorization: Bearer <JWT>`

### Topics oficiales

| Topic | Payload | Uso |
|-------|---------|-----|
| `/topic/games/{gameId}/events` | `GameEventMessage` | Eventos discretos para log, animaciones y feedback visual. |
| `/topic/games/{gameId}/state-changed` | `GameStateChangedMessage` | Señal liviana para recargar estado real por REST. |

### `/events`

Ejemplo:

```json
{
  "gameId": "uuid",
  "eventType": "DAMAGE_DEALT",
  "payload": { "amount": 30 },
  "timestamp": "2026-06-25T12:00:00"
}
```

Uso correcto:

- animaciones;
- log de eventos;
- feedback visual transitorio.

Uso incorrecto:

- reconstruir el tablero;
- inferir reglas en frontend;
- exponer mano, mazo o premios ocultos.

### `/state-changed`

Ejemplo:

```json
{
  "gameId": "uuid",
  "actionType": "USE_ATTACK",
  "status": "ACTIVE",
  "timestamp": "2026-06-25T12:00:00"
}
```

Al recibirlo, el frontend debe ejecutar:

```text
GET /api/games/{gameId}/state
```

Ese endpoint devuelve un `BoardStateDTO` filtrado para el jugador autenticado.

## Seguridad

La seguridad está en dos capas:

| Momento | Validación |
|---------|------------|
| `CONNECT` | Valida JWT y crea el `Principal` autenticado. |
| `SUBSCRIBE` | Valida que el usuario autenticado participe en la partida del `gameId`. |

Consecuencia: un usuario autenticado **no puede escuchar partidas ajenas** suscribiéndose manualmente a topics de otro `gameId`.

### Datos privados

WebSocket no debe enviar:

- mano completa;
- orden del mazo;
- premios ocultos;
- estado privado completo;
- snapshots completos del tablero.

La privacidad del estado se protege en:

```text
GET /api/games/{gameId}/state
```

Ese endpoint filtra la vista según el JWT del jugador.

## Reconexión frontend

Estados posibles de conexión:

| Estado | Significado |
|--------|-------------|
| `disconnected` | Sin conexión activa. |
| `connecting` | Primer intento de conexión. |
| `connected` | STOMP conectado y topics suscriptos. |
| `reconnecting` | Reconexión automática en curso. |
| `error` | Se agotaron los intentos o hubo error irrecuperable. |

Regla clave:

```text
reconnectAttempts se resetea solo cuando onConnect confirma conexión exitosa.
```

Cuando una reconexión termina bien, el servicio emite:

```json
{ "topic": "system", "payload": { "type": "reconnected" } }
```

`BoardContainer` escucha ese evento y llama:

```text
loadGameState(gameId)
```

Así el jugador recupera el snapshot real si perdió eventos mientras estuvo desconectado.

## Estrategia de refresh

| Estado del WS | Estrategia |
|---------------|------------|
| `connected` | El refresh principal viene por `/state-changed`. |
| `disconnected`, `reconnecting`, `error` | Después de una acción REST exitosa, se usa fallback REST. |

Esto evita dos problemas:

1. **Doble GET innecesario** cuando WebSocket funciona.
2. **Estado stale** cuando WebSocket falla o está reconectando.

## Prueba manual con dos jugadores

1. Levantar backend.
2. Levantar frontend.
3. Usuario A inicia sesión y crea una partida.
4. Usuario B inicia sesión y se une a la partida.
5. Verificar que ambos ven el indicador WebSocket en `Conectado`.
6. Usuario A realiza una acción.
7. Usuario B ve la actualización sin presionar F5.
8. En la pestaña de Usuario B, cortar red desde DevTools.
9. Usuario A realiza otra acción.
10. Restaurar red en Usuario B.
11. Usuario B pasa por reconexión y recarga estado por REST.
12. Verificar que el tablero queda actualizado.

## Tests de verificación

Backend:

```powershell
cd BE
.\mvnw.cmd test -Dtest="ar.edu.utn.frc.tup.piii.configs.JwtChannelInterceptorTest"
```

Resultado esperado:

```text
14/14 OK
```

Frontend:

```powershell
cd FE
npx ng test --watch=false --browsers=ChromeHeadless
```

Resultado esperado:

```text
21/21 OK
```

## Fuera del contrato actual

Estos elementos no forman parte del contrato realtime vigente:

| Elemento | Estado |
|----------|--------|
| Chat por WebSocket | No implementado, fuera de alcance. |
| Acciones de juego por WebSocket | No permitido; van por REST. |
| `/user/queue/...` | No usado en esta fase. |
| `/topic/game/{gameId}` singular | Legacy/no oficial; no usar como contrato principal. |
| `/ws/websocket` | No es el endpoint documentado; usar `/ws`. |
| Estado completo por WebSocket | No permitido; usar `GET /state`. |
