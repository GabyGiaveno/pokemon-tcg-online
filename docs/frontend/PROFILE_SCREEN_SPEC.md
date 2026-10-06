# PROFILE_SCREEN_SPEC: Pantalla de Perfil del Entrenador

Este documento define la especificación inicial (MVP) para la pantalla de **Perfil del Jugador/Entrenador**, manteniendo la misma identidad visual del Lobby (`FE/src/app/features/auth/lobby/`) y alineando los datos mostrados con lo que el backend ya expone (o expone con un cambio mínimo).

---

## 1. Objetivo

Crear una pantalla "Perfil" accesible desde el botón **Perfil** del lobby (`lobby.html:46`, hoy sin acción), que muestre la información real del entrenador logueado, reutilizando la paleta de colores, tipografías y componentes "glass" del lobby para no romper la temática Pokémon ya establecida.

Esta primera versión es deliberadamente acotada: mostrar lo que el backend puede entregar hoy (o con un endpoint simple a agregar), dejando espacio para sumar logros, avatares, historial de partidas, etc. en iteraciones futuras.

---

## 2. Alcance

### 2.1 Incluye (MVP)
- Ruta `/perfil` (lazy-loaded standalone component).
- Conectar el botón "Perfil" del lobby (`lobby.html:46`) con `[routerLink]="['/perfil']"`.
- **Acceso adicional desde el lobby**: el bloque de identidad del jugador en la topbar (avatar + nombre, arriba a la izquierda, `lobby.html:3-9`) también es clickeable y navega a `/perfil` — atajo natural, como en la mayoría de juegos/lobbies.
- Card de identidad del entrenador: avatar con inicial, username, email, fecha de registro (createdAt).
- Listado de mazos del jugador (nombre, cantidad de cartas, si es válido) consumiendo `GET /api/decks`.
- Panel de **Estadísticas** (victorias/derrotas/racha) — mismo contenido/estética que el panel "ESTADÍSTICAS" del lobby (`lobby.html:18-29`), reutilizado/duplicado en el perfil para dar más cuerpo a la pantalla. Mientras no haya datos reales, se muestran los mismos valores mock que ya usa el lobby.
- **Barra de nivel/XP** decorativa debajo del nombre, igual a la del lobby (`xp-bar`/`xp-fill`).
- Panel de **Insignias / Logros** (decorativo): grid de medallas tipo "gimnasio Pokémon" con los íconos de tipo que ya están en el fondo del lobby (fuego, agua, planta, eléctrico, psíquico), mostrando cuáles están "desbloqueadas" (mock).
- **Cerrar sesión** desde el perfil: mismo flujo que el lobby (menú con "Cerrar sesión" + modal de confirmación `.confirm-backdrop`/`.confirm-card`), accesible desde un botón ⚙ en la topbar del perfil.
- Botón "Volver al lobby".
- Reutilizar el mismo fondo (`imagen-B.png`), overlay, topbar y estilos `.glass`, `.panel`, `.panel-title`, `.stat-row`, `.nav-btn`, `.xp-bar`/`.xp-fill`, `.confirm-*`, paleta dorada (`#f0b82b` / `#DE940E`) y azul (`#105189` / `#1a73c8`).

### 2.2 No incluye (futuro / fuera de alcance ahora)
- Edición de perfil (cambiar username, email, avatar, contraseña).
- Estadísticas reales de victorias/derrotas/racha (hoy son datos hardcodeados en el lobby — `lobby.html:19-29`; el perfil replica el mismo mock).
- Historial de partidas jugadas.
- Lógica real de niveles/XP/insignias (todo decorativo/mock por ahora).
- Subida de imagen de avatar (se usa inicial del username, igual que en el lobby).

---

## 3. Datos del Backend

### 3.1 Lo que ya existe y se puede usar tal cual
- `GET /api/decks` (`DeckController.java:31`) → lista de `DeckResponse` del jugador autenticado (vía `CurrentPlayerService`). Sirve para la sección "Mis mazos".
- El JWT decodificado en el frontend (`AuthTokenService`) ya expone `username` (usado en `lobby.ts:35`).

### 3.2 Lo que falta y se propone agregar (cambio mínimo en backend)
Hoy no existe un endpoint "perfil del jugador actual". `PlayerResponse` (`PlayerResponse.java`) solo se usa en login/register y trae `id`, `username`, `token` (sin email ni fecha de registro).

Se propone agregar a `PlayerService` + un nuevo endpoint, por ejemplo:

```
GET /api/players/me   (requiere JWT, usa CurrentPlayerService.getCurrentPlayerId())
```

Respuesta — nuevo DTO `PlayerProfileResponse`:
```json
{
  "id": 1,
  "username": "Ash",
  "email": "ash@pallet.town",
  "createdAt": "2026-01-15 10:30:00",
  "decksCount": 3
}
```

Esto se arma directamente desde la entidad `Player` (`Player.java`), que ya tiene `username`, `email`, `createdAt` y la relación `decks`. No requiere cambios de schema ni migraciones.

> Si por tiempos no se llega a tocar el backend en esta iteración, el MVP puede arrancar mostrando `username` (del JWT) + lista de mazos (`GET /api/decks`), dejando "email" y "miembro desde" como placeholders ("—") hasta que `/api/players/me` exista.

### 3.3 Futuro (no ahora)
- Estadísticas reales de partidas: se podrían calcular contando `GameSession` donde `player1`/`player2` = jugador actual y `status = FINISHED`, comparando contra `winner`. Requeriría un repositorio/consulta nueva — queda para otra iteración.

---

## 4. Diseño Visual

Mismo lenguaje visual que el lobby (`lobby.css`), para que el usuario sienta que es la misma app:

- **Fondo**: `imagen-B.png` con el mismo overlay oscuro (`.lobby::before`).
- **Topbar**: reutilizar la misma estructura (`avatar` redondo con inicial + degradé dorado, nombre del jugador + `xp-bar`/`xp-fill`), con un botón ⚙ (`icon-btn`) que abre el menú de "Cerrar sesión" y un botón "← Volver al lobby" (`nav-btn`) en `topbar-right`.
- **Paneles**: clase `.glass` + `.panel` para las tarjetas de "Datos del Entrenador", "Estadísticas", "Mis Mazos" e "Insignias", con `.panel-title` en dorado mayúsculas (`#DE940E`), igual que "ESTADÍSTICAS" / "NOTICIAS" en el lobby.
- **Datos clave-valor**: reutilizar `.stat-row`, `.stat-label` (gris claro) y `.stat-value` (dorado `#f0b82b` con `text-shadow`) para mostrar Email, Miembro desde, Cantidad de mazos, y también Victorias/Derrotas/Racha.
- **Lista de mazos**: estilo similar a `.game-row` del panel de partidas (fila con nombre del mazo a la izquierda y badge de estado — "Válido"/"Incompleto" — a la derecha, usando los colores de `.toast.success` / `.toast.error`).
- **Insignias**: grid de círculos/medallas (`.glass`, borde dorado si está "desbloqueada", gris/opaco si no), cada una con el ícono de un tipo Pokémon (🔥💧🌿⚡🔮), tooltip/leyenda con el nombre de la insignia.
- **Cerrar sesión**: reutilizar tal cual `.settings-backdrop`, `.settings-menu`, `.settings-item`, `.confirm-backdrop`, `.confirm-card`, `.confirm-actions`, `.confirm-btn--ghost`/`.confirm-btn--danger` del lobby (`lobby.css:38-129`), mismo texto y comportamiento (`logout()` → `AuthTokenService.clear()` + redirect a `/auth/login`).
- **Botón volver**: estilo `.nav-btn` o `.icon-btn` (coherente con los ya usados).
- **Tipografía y colores generales**: `Segoe UI`/system-ui, texto blanco `#fff`, acentos dorado `#f0b82b`/`#DE940E` y azul `#105189`/`#1a73c8`, bordes `rgba(255,255,255,0.06-0.12)`.

### 4.1 Layout propuesto (desktop)

```
┌─────────────────────────────────────────────────────────┐
│ [Avatar] Ash  [xp-bar]                  [⚙] [← Lobby]    │  topbar
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────────────┐   ┌────────────────────────┐  │
│  │ DATOS DEL ENTRENADOR │   │ ESTADÍSTICAS           │  │
│  │ Usuario: Ash         │   │ Victorias  47          │  │
│  │ Email: ash@...       │   │ Derrotas   42          │  │
│  │ Miembro desde: ...   │   │ Racha       3          │  │
│  │ Mazos: 3             │   │                         │  │
│  └──────────────────────┘   └────────────────────────┘  │
│                                                           │
│  ┌──────────────────────┐   ┌────────────────────────┐  │
│  │ MIS MAZOS            │   │ INSIGNIAS              │  │
│  │ - Mazo Fuego [Válido]│   │  🔥  💧  🌿  ⚡  🔮     │  │
│  │ - Mazo Agua [Incomp.]│   │ (desbloqueadas/grises) │  │
│  │ - ...                │   │                         │  │
│  └──────────────────────┘   └────────────────────────┘  │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

En mobile, las cuatro tarjetas se apilan en una sola columna (igual criterio que el resto de la app, sin grid de varias columnas).

---

## 5. Estructura de Carpetas (Frontend)

Siguiendo el patrón ya usado en `auth/` y `lobby/`:

```
FE/src/app/features/profile/
  profile.routes.ts            (opcional si se usa loadChildren, o registrar directo en app.routes.ts)
  pages/
    profile-page/
      profile-page.ts
      profile-page.html
      profile-page.css
  data-access/
    profile-api.service.ts      (GET /api/players/me cuando exista)
    profile-api.types.ts         (PlayerProfileResponse, etc.)
```

Reutilizar `LobbyApiService`/`DeckController` ya existentes para `GET /api/decks` (no duplicar lógica de mazos).

---

## 6. Ruteo

En `app.routes.ts`:
```ts
{
  path: 'perfil',
  loadComponent: () =>
    import('./features/profile/pages/profile-page/profile-page')
      .then(m => m.ProfilePage),
}
```

En `lobby.html:46`:
```html
<button class="nav-btn" type="button" [routerLink]="['/perfil']">Perfil</button>
```

En `lobby.html:3-9` (topbar-left), agregar navegación al perfil envolviendo o agregando `(click)`/`[routerLink]` al bloque del avatar + nombre:
```html
<div class="topbar-left" [routerLink]="['/perfil']" style="cursor: pointer;">
  <div class="avatar">{{ playerName[0] }}</div>
  <div class="player-data">
    <h2>{{ playerName }}</h2>
    <div class="xp-bar"><div class="xp-fill" style="width: 65%"></div></div>
  </div>
</div>
```

---

## 7. Plan por fases

1. **Fase 1 (esta spec)**: pantalla de perfil con username (JWT), email + fecha de registro (si se agrega `/api/players/me`) y lista de mazos reales. Mismo theme del lobby.
2. **Fase 2**: estadísticas reales (victorias/derrotas/racha) calculadas desde `GameSession`.
3. **Fase 3**: edición de perfil (username, email, password) y selección de avatar.
4. **Fase 4**: historial de partidas, logros, nivel/XP real.

---

## 8. Checklist de implementación (Fase 1)

- [ ] (Backend, opcional) `PlayerProfileResponse` DTO + `GET /api/players/me` en un nuevo `PlayerController` (o agregar a uno existente).
- [ ] (Frontend) Crear `features/profile/` con la estructura indicada.
- [ ] Conectar botón "Perfil" del lobby con la nueva ruta.
- [ ] Hacer clickeable el bloque avatar+nombre de la topbar del lobby (`topbar-left`) para ir a `/perfil`.
- [ ] Maquetar `profile-page.html`/`.css` reutilizando clases/paleta del lobby (`.glass`, `.panel`, `.stat-row`, `.nav-btn`, `.xp-bar`, colores `#f0b82b`/`#DE940E`/`#105189`).
- [ ] Agregar panel "Estadísticas" (mock, mismos valores que el lobby).
- [ ] Agregar panel "Insignias" decorativo (grid de medallas por tipo).
- [ ] Agregar menú ⚙ + modal de confirmación para "Cerrar sesión" (mismo patrón que `lobby.ts`).
- [ ] Consumir `GET /api/decks` para la lista de mazos.
- [ ] Manejar estado de carga/error igual que el lobby (signals + feedback).
