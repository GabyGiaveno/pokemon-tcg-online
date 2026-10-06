# Pokédex de Cartas — Documentación de Frontend

> Estado: **maqueta visual completa con datos mock**, lista para conectar a `PokedexService` /
> `GET /api/cards` y para integrarse a las rutas del resto de la app (lobby, auth, etc.).
>
> Referencia de diseño: [`SPECS/POKEDEX_SPEC.md`](../../SPECS/POKEDEX_SPEC.md) (sección 5 en
> adelante) y [`FE/public/images/img.png`](../../FE/public/images/img.png).

Este documento existe para que cualquier persona (o agente) que retome el trabajo de la Pokédex
entienda **qué se hizo, por qué se hizo así, qué es mock vs. real, y qué falta** para integrarla
al resto del frontend y al backend.

---

## 1. Resumen de lo implementado

Se construyó la pantalla `/pokedex` como una "consola/Pokédex de biblioteca": una pantalla azul
enmarcada en un escritorio de madera, dividida en 4 zonas:

```
┌─────────────────────────────────────────────────────────┐
│                    Pokédex de Cartas (titlebar)          │
├──────┬─────────────────────────────────┬────────────────┤
│ Type │                                  │                │
│ Rail │          Card Grid               │  Card Detail   │
│ (8%) │  (cartas + paginación)  (62%)    │  Panel (30%)   │
│      │                                  │                │
├──────┴─────────────────────────────────┴────────────────┤
│                    Filter Bar (full width)                │
└─────────────────────────────────────────────────────────┘
```

Todo el contenido de cartas viene hoy de **mocks locales** (`MOCK_POKEDEX_CARDS`), no hay ningún
llamado HTTP todavía. El objetivo de esta etapa fue **resolver el diseño visual y el layout
responsive**; la lógica de datos reales queda pendiente (ver sección 5).

---

## 2. Estructura de carpetas

```
FE/src/app/features/pokedex/
├── pages/
│   └── pokedex-page/
│       ├── pokedex-page.ts      # Componente contenedor (estado, mocks, filtros, paginación)
│       ├── pokedex-page.html    # Layout: type-rail + card-grid + detail-panel + filter-bar
│       └── pokedex-page.css     # Variables de paleta (--pokedex-*), grid del "screen", responsive
├── components/
│   ├── type-rail/        # Riel vertical (horizontal en mobile/tablet) de íconos de tipo
│   ├── card-grid/         # Grilla de cartas + estados (loading/error/empty) + paginación
│   ├── card-preview/      # Miniatura de carta individual dentro de la grilla
│   ├── pagination/         # Controles "‹ Anterior / 1 2 3 / Siguiente ›"
│   ├── card-detail-panel/ # Panel derecho con detalle completo de la carta seleccionada
│   └── filter-bar/         # Barra inferior: búsqueda, set, supertipo, sync, chips activos
├── domain/
│   ├── models/
│   │   ├── pokedex-card.ts     # Interfaces PokedexCard, Attack, WeaknessResistance
│   │   └── pokedex-filters.ts  # Interface PokedexFilters
│   └── constants/
│       └── pokemon-types.ts    # POKEMON_TYPES (id, label, color, icon) + helpers
└── data-access/
    ├── mocks/
    │   └── pokedex-mock-cards.ts  # 12 cartas de ejemplo (set "xy1" / XY Base Set)
    └── services/
        └── .gitkeep                # <- acá va el futuro PokedexService
```

---

## 3. Componentes — qué hace cada uno

### 3.1 `PokedexPage` (`pages/pokedex-page/`)
Componente contenedor / "smart component". Hoy:

- Mantiene **signals** de estado: `allCards`, `loading`, `error`, `syncing`, `filters`, `page`,
  `selectedCard`.
- `pageSize` está fijado en una constante `PAGE_SIZE = 16` (ver sección 4 — por qué 16).
- `filteredCards` (computed): aplica los filtros de `PokedexFilters` sobre `allCards()` —
  **esto es lógica de ejemplo, client-side, sobre el array mock completo**. Cuando se conecte el
  backend, el filtrado/paginado real lo va a hacer `GET /api/cards` (que ya soporta `name`,
  `set`, `supertype`, `type`, `page`, `size` — ver sección 5.2), por lo que esta función debería
  achicarse o eliminarse.
- `pagedCards` (computed): hace `slice()` sobre `filteredCards()` según `page` y `pageSize`.
  Cuando haya backend, esto deja de ser necesario porque la paginación la devuelve la API
  (`CardPageResponse`).
- Maneja todos los handlers de eventos que disparan los componentes hijos (`onTypeToggle`,
  `onSearchChange`, `onSetChange`, `onSupertypeToggle`, `onClearFilters`, `onRemoveChip`,
  `onCardSelect`, `onPageChange`, `onRetry`, `onSyncSet`).
- `onSyncSet` hoy es un mock (`setTimeout` que prende/apaga `syncing`). Debe llamar a
  `POST /api/cards/sync?set=...`.

`pokedex-page.css` define **todas las variables de paleta** (`--pokedex-bisel`,
`--pokedex-titlebar`, `--pokedex-rail`, `--pokedex-grid`, `--pokedex-detail`,
`--pokedex-filterbar`, `--pokedex-text`, `--pokedex-gold*`, etc.) en `:host`, y arma:

- `.library-scene`: fondo cálido tipo escritorio/biblioteca (decorativo, fuera de la "pantalla").
- `.pokedex-screen`: el "televisor" Pokédex — `display:grid; grid-template-rows: auto
  minmax(0,1fr) auto;` (el `minmax(0,1fr)` es importante, ver sección 4.1).
- `.screen-content`: grid de 3 columnas `4.5rem minmax(0,1fr) 17rem` (type-rail / card-grid /
  detail-panel) en desktop. Con media queries se convierte en una sola columna apilada en
  `max-width: 1023px` y `max-width: 640px`.

### 3.2 `TypeRail` (`components/type-rail/`)
Columna angosta con un botón circular por cada tipo de Pokémon (`POKEMON_TYPES`). Al hacer click
emite `typeToggle` con el `id` del tipo (o `null` si se vuelve a hacer click sobre el activo).
`activeType` (input) resalta el tipo seleccionado. En mobile/tablet pasa a ser una fila
horizontal scrolleable arriba de la grilla.

### 3.3 `CardGrid` (`components/card-grid/`)
Grilla central. Maneja 4 estados según los inputs:
1. `loading()` → 12 `.skeleton-card` con shimmer.
2. `error()` → mensaje + botón "Reintentar" (`retry` output).
3. `cards().length === 0` → mensaje de "no se encontraron cartas".
4. caso normal → `.grid` con `<app-card-preview>` por carta + `<app-pagination>` debajo.

`.grid` usa `grid-template-columns: repeat(auto-fill, minmax(7.5rem, 1fr))` — se adapta solo a la
cantidad de columnas que entren según el ancho disponible.

### 3.4 `CardPreview` (`components/card-preview/`)
Miniatura: imagen pequeña (`imageUrlSmall`), nombre, HP badge, color de acento según
`types[0]` (vía `getTypeColor`). Emite `select` al hacer click. Estado `selected` resalta con
borde dorado.

### 3.5 `Pagination` (`components/pagination/`)
Recibe `page`, `size`, `total` (todos `input.required<number>()`), emite `pageChange`. Calcula
`totalPages` y una ventana deslizante de hasta 5 botones de página. Texto "Mostrando X-Y de Z".

### 3.6 `CardDetailPanel` (`components/card-detail-panel/`)
Panel derecho. Si `card()` es `null` muestra estado vacío ("Seleccioná una carta..."). Si hay
carta, muestra: imagen grande (`imageUrlLarge`), nombre, HP, tipo(s), `evolvesFrom`, set,
ataques (con pips de costo de energía por tipo), debilidades/resistencias (chips), costo de
retirada (pips).

### 3.7 `FilterBar` (`components/filter-bar/`)
Barra inferior full-width. Contiene:
- Buscador de texto (`searchChange`).
- `<select>` de expansión/set (`setChange`) — hoy las opciones vienen **hardcodeadas** como
  `input<SetOption[]>` con default `[{id:'xy1', name:'XY Base Set'}, {id:'xy2', name:'Flashfire'}]`.
- Chips de supertipo: Pokémon / Entrenador / Energía (`supertypeToggle`).
- Botón "Sincronizar set" (solo si `isAdmin()` es `true` — hoy hardcodeado a `true` desde
  `pokedex-page.html`, **debe conectarse al rol real del usuario logueado**).
- Fila de chips de filtros activos con botón "Limpiar" (`clearFilters`, `removeChip`).

---

## 4. Decisiones de layout / CSS importantes (para no repetir bugs ya resueltos)

### 4.1 Overflow vertical de `.pokedex-screen`
En CSS Grid, una fila `1fr` tiene un mínimo implícito de `auto` (= tamaño del contenido). Si el
contenido es más alto que el espacio disponible, **la fila crece y rompe el layout**. Por eso
`.pokedex-screen` usa `grid-template-rows: auto minmax(0, 1fr) auto;` y `.screen-content` tiene
`min-height: 0; overflow: hidden;`.

### 4.2 Overflow horizontal en mobile (`.screen-content` con `1fr`)
Mismo problema pero en el eje horizontal: en la media query mobile, `.screen-content` tenía
`grid-template-columns: 1fr`, y esa única columna **crecía hasta el `max-content` de la grilla de
cartas** (que con `repeat(auto-fill, minmax(7.5rem, 1fr))` puede ser bastante ancho), empujando
todo el `.pokedex-screen` fuera del viewport. Se corrigió usando
`grid-template-columns: minmax(0, 1fr)`.

**Regla general a recordar**: cualquier elemento que sea hijo directo de un grid container
(`app-card-grid`, `app-card-detail-panel`, `app-filter-bar`, etc.) y que internamente tenga
contenido flex/grid con anchos mínimos, necesita `min-width: 0` (y a veces `min-height: 0`) para
poder *encogerse* dentro de su celda. Si en el futuro se agregan nuevos componentes a
`.screen-content` o a `.filter-bar-row` y aparecen overflows raros, **empezar por acá**.

### 4.3 Paneles que no llegaban hasta abajo (`height: 100%`)
`app-type-rail` y `app-card-grid` son *grid items* dentro de `.screen-content` y por defecto se
estiran (`align-self: stretch`) para ocupar toda la altura de la fila — **pero sus elementos
internos** (`.type-rail`, `.card-grid-panel`) no heredan esa altura automáticamente porque tienen
`height: auto`. Se les agregó `height: 100%; box-sizing: border-box;` para que el fondo de color
(riel de tipos / grilla) llegue realmente hasta el borde inferior de la pantalla, alineado con la
`filter-bar`, en vez de cortarse a la altura del contenido.

### 4.4 Paginación de los mocks (`PAGE_SIZE`)
`PAGE_SIZE` se subió de 8 a **16** para que las 12 cartas mock actuales entren todas en la
"página 1" (mostrando "1-12 de 12", sin segunda página). La idea es que sea un valor cómodo: ni
tan chico que la grilla se vea vacía con pocas cartas, ni tan grande que pagine de forma rara con
muchas. **Si el set de datos crece mucho (cientos de cartas), considerar bajarlo a algo entre 20
y 30** dependiendo de cuántas columnas entren en el ancho típico de pantalla — no hace falta
tocar ningún otro componente, `CardGrid`/`Pagination` ya son agnósticos al tamaño de página.

### 4.5 Paleta de colores
Toda la paleta vive como custom properties en `:host` de `pokedex-page.css`
(`--pokedex-bisel`, `--pokedex-titlebar`, `--pokedex-rail`, `--pokedex-grid`,
`--pokedex-detail`, `--pokedex-filterbar`, `--pokedex-text`, `--pokedex-text-secondary`,
`--pokedex-gold`, `--pokedex-gold-active`, `--pokedex-gold-hover`). Si se agregan componentes
nuevos, **reusar estas variables** en vez de hardcodear colores nuevos, para mantener consistencia
con `SPECS/POKEDEX_SPEC.md` Apéndice A.

---

## 5. Lo que falta para integrar (pendiente — para el equipo / próximos agentes)

### 5.1 Reemplazar mocks por `PokedexService`
Hoy `PokedexPage` importa `MOCK_POKEDEX_CARDS` directamente
(`data-access/mocks/pokedex-mock-cards.ts`, 12 cartas del set `xy1`/XY Base Set, con `name`,
`types`, `hp`, `attacks`, etc. ya verificados contra el set real).

Pasos sugeridos:
1. Crear `data-access/services/pokedex.service.ts` (la carpeta ya existe con `.gitkeep`), como
   `@Injectable({ providedIn: 'root' })`, usando `HttpClient` para pegarle a `/api/cards`.
2. Definir métodos:
   - `getCards(filters: PokedexFilters, page: number, size: number): Observable<CardPageResponse>`
     → `GET /api/cards?name=&set=&supertype=&type=&page=&size=`
   - `getCardById(id: string): Observable<PokedexCard>` → `GET /api/cards/{id}`
   - `syncSet(setId: string): Observable<SyncResponse>` → `POST /api/cards/sync?set={setId}`
3. En `PokedexPage`:
   - Reemplazar `allCards` + `filteredCards` + `pagedCards` por una llamada reactiva al service
     cada vez que cambien `filters()` o `page()` (p.ej. con `rxResource`, `toSignal` +
     `switchMap`, o un `effect()` que dispare la carga).
   - `loading`/`error` deben reflejar el estado real de la petición HTTP, no los signals mock
     actuales.
   - `total` debe venir de `CardPageResponse.total`, no de `filteredCards().length`.
   - `onSyncSet` debe llamar a `syncSet()` real y manejar éxito/error (hoy es un `setTimeout`).

### 5.2 ⚠️ Mismatch de tipos entre Backend y Frontend (importante)
El DTO real del backend (`CardResponse.java`, `BE/.../dtos/response/CardResponse.java`) define:

```java
private String attacks;     // <- String, probablemente JSON serializado
private String weaknesses;  // <- String
private String resistances; // <- String
```

Pero el modelo de frontend (`domain/models/pokedex-card.ts`) espera:

```ts
attacks: Attack[] | null;
weaknesses: WeaknessResistance[] | null;
resistances: WeaknessResistance[] | null;
```

**Hay que ponerse de acuerdo con el equipo de backend** sobre el contrato real:
- Opción A: el backend devuelve estos campos como JSON *string* y el frontend los parsea
  (`JSON.parse(...)`) al mapear `CardResponse` → `PokedexCard` en `PokedexService`.
- Opción B (preferible si es fácil del lado backend): que `CardResponse` exponga estos campos ya
  tipados como listas/objetos (`List<AttackDto>`, etc.) para que Jackson los serialice como JSON
  real y el frontend los reciba tipados directamente.

Mientras no se resuelva esto, **no se debe asumir que el array tal cual viene del backend matchea
1:1 con `PokedexCard`** — va a hacer falta una función de mapeo (`mapCardResponseToPokedexCard`)
en el `PokedexService` o en un archivo `data-access/mappers/`.

> Nota: por pedido explícito de quien pidió este trabajo, **no se modificó código de backend**.
> Esta sección es para que el equipo de backend y frontend definan el contrato juntos.
# Integración de la Capa Data Access - Pokedex 

## Objetivo

Desacoplar completamente la UI de la fuente de datos mediante una arquitectura basada en:

- DTOs de transporte
- Mappers
- Contratos abstractos
- Implementaciones intercambiables (Mock / Backend Real)

De esta forma, la pantalla de Pokedex no depende de datos mock ni de detalles del backend.

---

# Archivos Creados

## 1. `data-access/models/card-response.ts`

Define las interfaces que representan exactamente la respuesta del backend.

### CardResponse

```ts
export interface CardResponse {
  id: string;
  name: string;
  // ...

  attacks: string | null;
  weaknesses: string | null;
  resistances: string | null;

  // ...
}
```

### CardPageResponse

```ts
export interface CardPageResponse {
  data: CardResponse[];
  total: number;
  page: number;
  size: number;
}
```

### CardSearchParams

```ts
export interface CardSearchParams {
  name?: string;
  set?: string;
  supertype?: string;
  type?: string;
  page?: number;
  size?: number;
  sort?: string;
}
```

### Motivo de existencia

El frontend necesitaba un modelo que coincidiera **1:1** con el DTO enviado por el backend (`CardResponse.java`).

Los campos:

- `attacks`
- `weaknesses`
- `resistances`

llegan serializados como JSON en formato `String`, por lo que no pueden mapearse directamente a los modelos de la UI.

Esta capa representa exclusivamente los datos de transporte HTTP.

---

## 2. `data-access/mappers/card-response.mapper.ts`

Responsable de transformar los DTOs recibidos del backend en modelos consumibles por la interfaz de usuario.

### Funciones

#### Safe JSON Parse

```ts
safeJsonParse<T>(json: string | null): T | null
```

Responsabilidades:

- Parsear un string JSON.
- Retornar `null` si el valor es `null`.
- Retornar `null` si ocurre algún error de parseo.

---

#### Mapper Principal

```ts
mapCardResponseToPokedexCard(
  response: CardResponse
): PokedexCard
```

Responsabilidades:

- Convertir `attacks` mediante `JSON.parse()`.
- Convertir `weaknesses` mediante `JSON.parse()`.
- Convertir `resistances` mediante `JSON.parse()`.
- Copiar el resto de propiedades directamente.

---

#### Mapper de Página

```ts
mapCardPageResponse(
  response: CardPageResponse
)
```

Transforma una página completa:

```ts
{
  cards,
  total,
  page,
  size
}
```

---

### Motivo de existencia

Este mapper actúa como puente entre:

```text
Backend DTO
      ↓
 Mapper
      ↓
Frontend Model
```

Es el único lugar del sistema donde se ejecuta `JSON.parse()`.

Si en el futuro el backend deja de enviar strings JSON y comienza a enviar listas tipadas, únicamente será necesario modificar este archivo.

---

## 3. `data-access/services/pokedex-api.service.ts`

Define el contrato abstracto para cualquier proveedor de datos de cartas.

### Definición

```ts
export abstract class PokedexApi {

  abstract getCards(
    params: CardSearchParams
  ): Observable<CardPageResponse>;

  abstract getCardById(
    id: string
  ): Observable<CardResponse>;

  abstract syncSet(
    setId: string
  ): Observable<void>;
}
```

### Motivo de existencia

Permite que la aplicación dependa de una abstracción y no de una implementación concreta.

La Pokedex nunca sabe si está consumiendo:

- Datos Mock
- Backend Real

Solo conoce el contrato `PokedexApi`.

---

## 4. `data-access/services/mock-pokedex-api.service.ts`

Implementación Mock del contrato `PokedexApi`.

### Implementación

```ts
@Injectable()
export class MockPokedexApiService
  implements PokedexApi
```

### Métodos

#### getCards()

```ts
getCards(params)
```

Responsabilidades:

- Filtrar por:
    - name
    - type
    - supertype
    - set
- Paginar resultados.
- Convertir datos Mock a formato `CardResponse`.
- Serializar:
    - attacks
    - weaknesses
    - resistances
- Simular latencia de red.

```ts
delay(300ms)
```

---

#### getCardById()

```ts
getCardById(id)
```

Responsabilidades:

- Buscar carta por ID.
- Retornar error 404 si no existe.

---

#### syncSet()

```ts
syncSet(setId)
```

Responsabilidades:

- Simular sincronización de sets.
- Delay artificial de:

```ts
800ms
```

---

### Motivo de existencia

Permite desarrollar y testear el frontend sin necesidad de un backend disponible.

Además, utiliza exactamente el mismo pipeline que se utilizará en producción:

```text
Mock
 ↓
CardResponse
 ↓
Mapper
 ↓
PokedexCard
```

Esto asegura que la integración futura tenga el menor riesgo posible.

---

## 5. `data-access/services/pokedex-api.provider.ts`

Configura qué implementación concreta será inyectada.

### Configuración actual

```ts
export const POKEDEX_API_PROVIDERS: Provider[] = [
  {
    provide: PokedexApi,
    useClass: MockPokedexApiService,
  },
];
```

### Motivo de existencia

Centraliza la selección del origen de datos.

Permite cambiar entre:

- Mock
- Backend Real

sin modificar ningún componente.

---

# Archivos Modificados

## 6. `pages/pokedex-page/pokedex-page.ts`

### Situación anterior

La página:

- Importaba directamente `MOCK_POKEDEX_CARDS`.
- Gestionaba manualmente:
    - allCards
    - filteredCards
    - pagedCards
    - loading
    - error
    - total
- Realizaba filtrado local.
- Realizaba paginación local.

---

### Situación actual

La página utiliza `resource()` de Angular.

### Comportamiento

Escucha automáticamente cambios en:

```ts
filters()
page()
```

Cada modificación dispara:

```ts
PokedexApi.getCards(...)
```

La respuesta es transformada mediante:

```ts
mapCardPageResponse(...)
```

---

### Estado expuesto por Resource

Datos:

```ts
cardsResource.value()
```

Loading:

```ts
cardsResource.isLoading()
```

Error:

```ts
cardsResource.error()
```

---

### Retry

```ts
onRetry()
```

Ejecuta:

```ts
cardsResource.reload()
```

---

### Sincronización

```ts
onSyncSet()
```

Invoca:

```ts
PokedexApi.syncSet(...)
```

---

### Cambio adicional

Antes:

```ts
selectedCard = primeraCartaMock
```

Ahora:

```ts
selectedCard = null
```

---

### Providers

Se agregó:

```ts
providers: [
  ...POKEDEX_API_PROVIDERS
]
```

en el decorador del componente.

---

## 7. `pages/pokedex-page/pokedex-page.html`

Actualización de bindings.

### Antes

```html
pagedCards()
```

### Después

```html
cardsResource.value().cards
```

---

### Antes

```html
loading()
```

### Después

```html
cardsResource.isLoading()
```

---

### Antes

```html
total()
```

### Después

```html
cardsResource.value().total
```

---

# Integración con Backend Real

Cuando el backend esté disponible, únicamente deberán realizarse dos cambios.

---

## Paso 1: Crear `real-pokedex-api.service.ts`

Ubicación:

```text
FE/src/app/features/pokedex/data-access/services/
├── pokedex-api.service.ts
├── mock-pokedex-api.service.ts
├── pokedex-api.provider.ts
└── real-pokedex-api.service.ts
```

### Implementación

```ts
@Injectable()
export class RealPokedexApiService
  implements PokedexApi {

  private readonly http = inject(HttpClient);

  getCards(
    params: CardSearchParams
  ): Observable<CardPageResponse> {

    const query = new HttpParams({
      fromObject: params as any,
    });

    return this.http.get<CardPageResponse>(
      '/api/cards',
      { params: query }
    );
  }

  getCardById(
    id: string
  ): Observable<CardResponse> {

    return this.http.get<CardResponse>(
      `/api/cards/${id}`
    );
  }

  syncSet(
    setId: string
  ): Observable<void> {

    return this.http.post<void>(
      '/api/cards/sync',
      null,
      {
        params: new HttpParams()
          .set('set', setId),
      }
    );
  }
}
```

---

## Paso 2: Cambiar el Provider

Archivo:

```text
pokedex-api.provider.ts
```

### Antes

```ts
useClass: MockPokedexApiService
```

### Después

```ts
useClass: RealPokedexApiService
```

---

# Resultado Final

Una vez realizado el cambio del provider:

```text
UI
 ↓
PokedexApi
 ↓
RealPokedexApiService
 ↓
Backend REST
```

No será necesario modificar:

- PokedexPage
- Template HTML
- Mapper
- Componentes hijos
- Lógica de filtros
- Lógica de paginación

Toda la aplicación continuará funcionando gracias a que depende exclusivamente del contrato abstracto `PokedexApi`.
### 5.3 Sets disponibles (dropdown de `FilterBar`)
`FilterBar.sets` hoy es un `input<SetOption[]>` con default hardcodeado
(`[{id:'xy1', name:'XY Base Set'}, {id:'xy2', name:'Flashfire'}]`). Falta:
- Un endpoint o fuente de datos para listar los sets disponibles/sincronizados
  (revisar si `CardCacheService` / backend ya expone algo tipo `/api/cards/sets`; si no existe,
  coordinarlo con backend).
- Pasar esa lista real como `[sets]="..."` desde `PokedexPage`.

### 5.4 Rol de administrador (`isAdmin`)
`pokedex-page.html` pasa `[isAdmin]="true"` hardcodeado a `<app-filter-bar>`. Esto debe venir de
`AuthService` (`FE/src/app/core/services/auth.service.ts`) una vez que tenga lógica real (hoy es
una clase vacía, ver sección 5.6) — probablemente decodificando el rol desde el JWT guardado en
`localStorage`, o de un endpoint `/api/users/me`.

### 5.5 Ruteo / integración con el resto del frontend (lobby, navegación)
Estado actual de rutas (`FE/src/app/app.routes.ts`):

```ts
export const routes: Routes = [
  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: 'auth', children: AUTH_ROUTES },
  { path: 'pokedex', component: PokedexPage },          // <- sin guard
  { path: 'decks/new', loadComponent: () => ... },
  { path: '**', redirectTo: 'auth/login' },
];
```

Pendiente:
- **Agregar `canActivate: [authGuard]`** a la ruta `pokedex` (y probablemente a `decks/new`)
  una vez que `authGuard` (`FE/src/app/core/guards/auth.guard.ts`) deje de ser un stub
  (`() => true`) y verifique el JWT real.
- **Convertir `pokedex` a `loadComponent`** (lazy load), igual que `decks/new`, para no cargar el
  feature completo en el bundle inicial:
  ```ts
  {
    path: 'pokedex',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/pokedex/pages/pokedex-page/pokedex-page').then(m => m.PokedexPage),
  }
  ```
- **Lobby** (`FE/src/app/features/lobby/lobby/lobby.ts`) hoy es un componente vacío
  (`template: ''`). Cuando se construya, va a necesitar:
  - Un link/botón de navegación hacia `/pokedex` (p.ej. "Ver mi colección" / "Pokédex").
  - Posiblemente un layout compartido (header/nav) que envuelva tanto `lobby` como `pokedex` y
    `decks/*` — hoy `PokedexPage` renderiza una pantalla "full bleed" (`.library-scene` ocupa
    `min-height: 100vh`), lo cual **puede chocar con un header/nav global** si se agrega uno más
    adelante. Si se define un `AppShell`/`MainLayout` con header, `.library-scene` debería pasar
    a `min-height: 100%`/`100dvh` relativo al `<router-outlet>` y no a todo el viewport.
  - Definir cómo se vuelve del Pokédex al lobby (botón "‹ Volver al lobby" en el titlebar de
    `.screen-titlebar`, por ejemplo).

### 5.6 Servicios core compartidos (para tener en cuenta)
- `AuthService` (`core/services/auth.service.ts`) y `authGuard` (`core/guards/auth.guard.ts`)
  son **stubs vacíos** (`@Injectable() export class AuthService {}` / `() => true`). La Pokédex
  depende de que esto se implemente para: (a) proteger la ruta, (b) saber si el usuario es admin
  (sección 5.4), (c) eventualmente mostrar info del usuario en algún header global.
- `WebSocketService` (`core/services/websocket.service.ts`) existe para el juego en tiempo real;
  **no debería ser necesario para la Pokédex** (es solo lectura de catálogo de cartas), salvo que
  se quiera notificar en vivo cuando termina un `sync` de un set (no es prioritario).

### 5.7 Checklist resumido para quien continúe

- [ ] Crear `PokedexService` (`data-access/services/`) con `getCards`, `getCardById`, `syncSet`.
- [ ] Definir con backend el formato real de `attacks` / `weaknesses` / `resistances` y escribir
      el mapper correspondiente.
- [ ] Reemplazar `MOCK_POKEDEX_CARDS` y la lógica de filtrado/paginado client-side en
      `PokedexPage` por llamadas reactivas al `PokedexService`.
- [ ] Resolver de dónde sale la lista de `sets` para el `<select>` de `FilterBar`.
- [ ] Conectar `isAdmin` a `AuthService` real.
- [ ] Agregar `authGuard` real a la ruta `/pokedex` y convertirla a `loadComponent` (lazy).
- [ ] Definir layout compartido (lobby ↔ pokedex ↔ deck-builder) y ajustar `.library-scene` si
      se agrega un header/nav global.
- [ ] Implementar `onSyncSet` contra `POST /api/cards/sync` (con feedback de éxito/error real).

---

## 6. Cómo correr y ver la Pokédex localmente

```powershell
cd FE
npm install      # si no se hizo antes
npm start        # o: npx ng serve
```

Abrir `http://localhost:4200/pokedex`. Todo funciona con mocks, sin necesidad de levantar el
backend. Para probar breakpoints responsive: DevTools → modo responsive → 375px (mobile),
768px (tablet), 1280px+ (desktop) — los tres están verificados sin overflow horizontal/vertical.
