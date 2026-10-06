# Especificación Funcional — Módulo Pokédex

> **Proyecto**: Pokémon TCG Online — TPI Programación III UTN FRC  
> **Stack real**: Angular 20.3 · TypeScript 5.9 · CSS3 · Java 21 · Spring Boot 4.0.0 · PostgreSQL 15  
> **Versión spec**: 1.3  
> **Fecha**: 2026-06-09  
> **Backend verificado**: Sí — spec ajustada a endpoints reales existentes (sin cambios de backend en esta versión)  
> **Diseño visual**: Realineado con `FE/public/images/img.png` — pantalla azul con bisel redondeado en escritorio de biblioteca Pokémon

---

## Índice

1. [Objetivo del módulo](#1-objetivo-del-módulo)
2. [Alcance](#2-alcance)
3. [Casos de uso](#3-casos-de-uso)
4. [Reglas de negocio](#4-reglas-de-negocio)
5. [Componentes visuales](#5-componentes-visuales)
6. [Estados de interfaz](#6-estados-de-interfaz)
7. [Integraciones backend](#7-integraciones-backend)
8. [Modelos de datos](#8-modelos-de-datos)
9. [Eventos de usuario](#9-eventos-de-usuario)
10. [Criterios de aceptación](#10-criterios-de-aceptación)
11. [Consideraciones de accesibilidad](#11-consideraciones-de-accesibilidad)
12. [Consideraciones responsive](#12-consideraciones-responsive)
13. [Estructura de componentes](#13-estructura-de-componentes)
14. [Gaps detectados backend](#14-gaps-detectados-backend)

---

## 1. Objetivo del módulo

Proporcionar una interfaz de exploración y consulta de todas las cartas Pokémon disponibles en el sistema, obtenidas desde el backend (que a su vez las sincroniza desde la API pública de pokemontcg.io). El módulo funciona exclusivamente en modo **solo lectura**: no se crean, modifican ni eliminan cartas desde la Pokédex.

La experiencia visual debe inspirarse en una **pantalla/consola Pokédex apoyada en un escritorio de biblioteca Pokémon** — una pantalla de tonos azules, enmarcada por un bisel oscuro redondeado, ubicada dentro de una escena cálida con estanterías de madera, pokébolas y objetos decorativos a los costados. Estilo Pokémon TCG / RPG inventory, cálido y acogedor (no un dashboard corporativo plano).

---

## 2. Alcance

### Incluye

- Listado paginado de cartas con grilla visual (card grid)
- Búsqueda por nombre (texto libre, case-insensitive)
- Filtros combinables:
  - Tipo Pokémon (Fire, Water, Grass, Lightning, Psychic, Fighting, Darkness, Metal, Fairy, Dragon, Colorless)
  - Supertype (Pokémon, Trainer, Energy)
  - Expansión (set ID, ej. `xy1`, `xy2`, etc.)
- Vista de detalle de una carta individual con toda su información disponible
- Sincronización de sets (trigger manual desde interfaz administrativa)
- Indicador de carga, empty state y manejo de errores
- Parseo en frontend de campos JSON embebidos (attacks, weaknesses, resistances)

### Excluye

- Creación, edición o eliminación de cartas
- Comparación de cartas
- Favoritos / colección personal
- Vista de cartas en 3D o volteo animado
- Filtro por rareza (pendiente de implementación backend)
- Visualización de habilidades (pendiente de implementación backend)

---

## 3. Casos de uso

| ID | Nombre | Descripción | Estado backend |
|----|--------|-------------|----------------|
| UC-01 | Explorar cartas | El usuario accede a la Pokédex y ve la grilla paginada de cartas del set por defecto (xy1). | ✅ Soportado |
| UC-02 | Buscar por nombre | El usuario escribe total o parcialmente el nombre de una carta y el sistema filtra (debounced 300ms). | ✅ Soportado |
| UC-03 | Filtrar por tipo | El usuario selecciona un tipo Pokémon y la grilla se reduce a cartas de ese tipo. | ✅ Soportado |
| UC-04 | Filtrar por supertype | El usuario selecciona un supertype (Pokémon, Trainer, Energy) y la grilla se reduce. | ✅ Soportado |
| UC-05 | Filtrar por expansión | El usuario selecciona una expansión y la grilla se reduce a cartas de ese set. | ✅ Soportado |
| UC-06 | Combinar filtros | El usuario puede aplicar nombre + tipo + supertype + expansión simultáneamente. | ✅ Soportado |
| UC-07 | Ver detalle de carta | El usuario hace clic en una carta y navega a una vista detallada con toda la información. | ✅ Soportado |
| UC-08 | Ver ataques | En el detalle, el usuario ve los ataques con nombre, daño, costo de energía y descripción. | ✅ Soportado (JSON string) |
| UC-09 | Ver debilidad y resistencia | En el detalle, el usuario ve el tipo de debilidad (×2) y resistencia (-30). | ✅ Soportado (JSON string) |
| UC-10 | Ver HP y evolución | En el detalle, el usuario ve los HP y de qué Pokémon evoluciona (si aplica). | ✅ Soportado |
| UC-11 | Ver costo de retirada | En el detalle, el usuario ve las energías necesarias para retirar al Pokémon. | ✅ Soportado |
| UC-12 | Navegación desde detalle | El usuario puede volver a la grilla desde el detalle. | ✅ Frontend |
| UC-13 | Paginación | El usuario navega entre páginas de resultados (anterior/siguiente). | ✅ Soportado |
| UC-14 | Sincronizar set | El usuario administrador fuerza la sincronización de un set desde la API externa. | ✅ Soportado |
| UC-15 | Filtrar por rareza | ⏳ Pendiente — requiere agregar `rarity` al backend. | ❌ No implementado |
| UC-16 | Ver habilidades | ⏳ Pendiente — requiere agregar `abilities` al backend. | ❌ No implementado |

---

## 4. Reglas de negocio

| ID | Regla |
|----|-------|
| BR-01 | La Pokédex es **solo lectura**. Ninguna acción permite modificar datos de cartas. |
| BR-02 | Los filtros disponibles son **combinables** mediante AND. Todos los filtros activos deben coincidir para que una carta sea incluida en resultados. |
| BR-03 | La búsqueda por nombre usa **contains case-insensitive**. Si se busca "char", aparecen "Charmander", "Charizard", "Charmeleon", etc. |
| BR-04 | La grilla muestra **máximo 20 cartas por página** por defecto (configurable vía query param `size`). |
| BR-05 | Si no hay filtros activos, se muestra el set por defecto (`xy1`). |
| BR-06 | Si una carta no tiene imagen disponible, se muestra un placeholder visual. |
| BR-07 | El detalle de carta se carga por ID único. Si el ID no existe, se muestra un error 404 amigable. |
| BR-08 | El parámetro `set` en la API selecciona el set completo a consultar — no es un filtro aditivo. Para ver otro set, se cambia el valor. |
| BR-09 | Los campos `attacks`, `weaknesses` y `resistances` vienen como **JSON strings** desde el backend. El frontend debe parsearlos con `JSON.parse()` para obtener los objetos utilizables. |

---

## 5. Componentes visuales

### Layout general — pantalla Pokédex en escritorio de biblioteca

La interfaz simula una **pantalla/monitor de tonos azules** apoyada sobre un escritorio, dentro de una escena de **biblioteca Pokémon cálida** (estanterías de madera, pokébolas y objetos decorativos visibles a los costados, detrás del marco de la pantalla). Esto refleja fielmente `FE/public/images/img.png`.

```
┌────────────────────────────────────────────────────────────────────┐
│  (fondo) escena de biblioteca — estanterías de madera, pokébolas    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  Pokédex de Cartas                       (barra de título)    │  │
│  │ ┌────┐ ┌──────────────────────────────┐ ┌──────────────────┐ │  │
│  │ │TIPO│ │      GRILLA DE CARTAS         │ │ CARTA DESTACADA  │ │  │
│  │ │RAIL│ │           (centro)            │ │  + DESCRIPCIÓN   │ │  │
│  │ │ ~8%│ │            ~62%               │ │      ~30%        │ │  │
│  │ └────┘ └──────────────────────────────┘ └──────────────────┘ │  │
│  │ ┌──────────────────────────────────────────────────────────┐ │  │
│  │ │ 🔍 Buscar...   [Set ▾]   [Tipos: chips/iconos]            │ │  │
│  │ └──────────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
```

**Distribución horizontal del área de contenido**: TypeRail ~8% | Grilla ~62% | Detalle ~30%  
**NO es 33/33/33** — la grilla central tiene el mayor peso visual, igual que en `img.png`.
La barra de filtros/búsqueda ocupa el ancho completo, **debajo** del área de contenido (no en un panel lateral).

### Marco de pantalla

La UI completa (título + contenido + barra de filtros) está contenida en una **pantalla con bisel redondeado**:
- Bisel exterior oscuro (gris azulado / casi negro), grosor moderado, esquinas bien redondeadas
- Interior con tonos azules (degradé de azul oscuro a azul medio), simulando el "encendido" de la pantalla
- Sombra suave hacia afuera que separa la pantalla del fondo de biblioteca
- Sin elementos de "consola Nintendo DS" (sin botones de colores, sin paneles con luces)

### Fondo — Escritorio de biblioteca Pokémon

El fondo (visible apenas como margen alrededor de la pantalla) es una escena cálida:
- Tonos madera / marrón cálido y dorado
- Estanterías o repisas con pokébolas y objetos decorativos en los bordes
- Iluminación cálida que contrasta con el azul de la pantalla
- Debe ser sutil: la pantalla ocupa la mayor parte del viewport, el fondo es un marco decorativo

---

### 5.1 PokedexPage (página principal)
- **Ruta**: `/pokedex`
- **Función**: Contenedor principal que orquesta título, TypeRail, CardGrid, CardDetailPanel y FilterBar.
- **Layout**: CSS Grid — fila de título, fila de contenido (3 columnas: ~8% / ~62% / ~30%), fila de barra de filtros
- **Estado local**:
  - `cards: Signal<Card[]>` — cartas de la página actual (ya parseadas)
  - `total: Signal<number>` — total de resultados
  - `page: Signal<number>` — página actual
  - `filters: Signal<PokedexFilters>` — filtros activos
  - `loading: Signal<boolean>` — indicador de carga
  - `error: Signal<string | null>` — mensaje de error
  - `selectedCard: Signal<PokedexCard | null>` — carta seleccionada para detalle
- **Estilo**: Pantalla con bisel redondeado envolviendo toda la UI, fondo de biblioteca Pokémon visible alrededor (ver "Marco de pantalla" y "Fondo" arriba)

### 5.2 TypeRail (columna izquierda angosta — filtro rápido por tipo)
- **Estilo**: Columna angosta (~8% del ancho) con fondo azul más oscuro que la grilla, separada por un borde sutil
- **Contenido**: Pila vertical de **iconos circulares/cuadrados de tipo Pokémon** (Fire, Water, Grass, Lightning, Psychic, Fighting, Darkness, Metal, Fairy, Dragon, Colorless), cada uno con el color de tipo correspondiente (ver paleta en Apéndice A)
- **Interacción**: Click en un icono activa/desactiva el filtro `filters.type` con ese tipo (recarga desde página 0)
- **Estado activo**: El icono del tipo seleccionado se ve resaltado (borde brillante o escala levemente mayor); el resto se atenúa un poco
- Tooltip / `aria-label` con el nombre del tipo en cada icono (accesibilidad, ya que el icono solo no es suficiente)
- En mobile, esta columna se convierte en una fila horizontal scrolleable arriba de la grilla (ver sección 12)

### 5.3 CardGrid (grilla central)
- **Estilo**: Zona central con fondo azul medio (más claro que el bisel, más oscuro que el TypeRail), levemente hundida respecto al resto de la pantalla (sombra interna sutil)
- **Grilla responsiva** (CSS Grid) de `CardPreview`
- **Distribución en desktop**: 5-6 columnas de cartas, varias filas visibles con scroll vertical
- Las cartas se muestran como **imágenes con marco** estilo Pokémon TCG (proporción vertical de carta real)
- Muestra skeleton cards (con `aria-hidden="true"`) mientras se cargan los datos
- Muestra mensaje "No se encontraron cartas" si no hay resultados
- **Scroll vertical** dentro de la grilla cuando hay más cartas que espacio visible
- `Pagination` se renderiza debajo de la grilla, dentro de esta misma zona

### 5.4 CardPreview (preview individual de carta)
- **Estilo**: Imagen de la carta (`imageUrlSmall`) con un marco fino redondeado
- **Borde de carta**: Tono azul/dorado discreto, bordes redondeados, sombra externa suave
- **Hover**: Elevación leve + borde iluminado (glow sutil dorado, en línea con la paleta Pokémon)
- **Carta seleccionada**: Borde dorado/amarillo más marcado que indica que está activa en el panel de detalle
- Nombre de la carta debajo de la imagen (texto claro, pequeño, centrado)
- HP visible en la esquina superior de la carta (badge pequeño)

### 5.5 Pagination (controles de paginación)
- Ubicados debajo de la grilla central, dentro de la misma zona azul
- Botones "Anterior" / "Siguiente"
- Números de página (máximo 5 visibles con ellipsis)
- Texto informativo: "Cartas Totales: 156 / [Mostrando 1-20]"

### 5.6 CardDetailPanel (columna derecha — carta destacada + descripción)
- **NO es una ruta separada** — se muestra como columna derecha (~30% del ancho)
- **Estilo**: Fondo azul, separado de la grilla por un borde sutil; visualmente se siente como una "ficha" dentro de la misma pantalla
- **Contenido**:
  - Carta ampliada (`imageUrlLarge`) centrada en la parte superior, con marco destacado
  - Debajo de la carta: bloque de texto con descripción/lore e info clave (Nombre, HP, Tipo(s), Evolución, Expansión)
  - Scroll vertical para contenido largo
  - Ataques parseados con nombre, costo, daño, texto
  - Debilidad y resistencia parseadas
  - Costo de retirada
- **Cuando no hay carta seleccionada**: Mensaje "Seleccioná una carta para ver su detalle" + ilustración/silueta placeholder de carta
- **Carta placeholder**: Cuando la grilla carga, mostrar esqueleto de ficha

### 5.7 FilterBar (barra inferior — búsqueda y filtros principales)
- **Ubicación**: Franja horizontal de ancho completo, debajo del área de contenido (TypeRail + Grid + Detail)
- **Estilo**: Fondo azul oscuro, separada del resto por un borde sutil, controles alineados horizontalmente
- **Contenido** (de izquierda a derecha):
  - 🔍 **Barra de búsqueda** por nombre, con icono de lupa, debounce 300ms
  - **Selector de set/expansión** (dropdown). Mientras no exista `GET /api/cards/sets`, este selector usa una lista corta predefinida o un input manual de texto para el ID del set
  - **Filtro de supertype** (Pokémon / Entrenador / Energía) como chips o botones pequeños
- Los filtros activos (incluyendo el tipo elegido en el `TypeRail`) se muestran como **chips removibles** dentro de esta barra
- Botón "Limpiar filtros" al final de la barra

### 5.8 SyncPanel (panel de sincronización)
- (Solo para administradores) Input para ID de set + Botón para sincronizar
- Se integra como un control adicional dentro de la `FilterBar` (o como modal accesible desde ahí)
- Indicador de progreso durante la sincronización
- Mensaje de confirmación al finalizar

---

## 6. Estados de interfaz

Cada componente debe manejar estos estados:

| Estado | Comportamiento |
|--------|---------------|
| **Loading** | Skeleton cards / spinner de carga en la zona de contenido. Inputs y filtros deshabilitados durante carga inicial. |
| **Empty** | Mensaje "No se encontraron cartas con los filtros seleccionados." + sugerencia de limpiar filtros. |
| **Error** | Mensaje de error amigable + botón "Reintentar" que re-dispara la última petición. Para errores de red, mensaje genérico: "No pudimos conectar con el servidor. Verificá tu conexión." |
| **Success** | Grilla de cartas renderizada con datos. |
| **Detail Loading** | Esqueleto de detalle mientras se carga la carta individual. |
| **Detail Error** | Mensaje "Carta no encontrada" con enlace para volver a la grilla. |
| **Detail Success** | Información completa de la carta renderizada. |

---

## 7. Integraciones backend

### 7.1 Endpoints existentes

#### `GET /api/cards`

Listado paginado con filtros. Endpoint existente y funcional.

**Parámetros**:

| Parámetro | Tipo | Obligatorio | Descripción | Soportado |
|-----------|------|-------------|-------------|-----------|
| `name` | string | No | Búsqueda por nombre (contains, case-insensitive) | ✅ |
| `type` | string | No | Tipo Pokémon (Fire, Water, etc.) | ✅ |
| `supertype` | string | No | Supertype (Pokémon, Trainer, Energy) | ✅ |
| `set` | string | No | ID del set (xy1, xy2, etc.). Determina qué set cargar del caché | ✅ |
| `page` | int | No | Número de página (0-indexed, vía Pageable) | ✅ |
| `size` | int | No | Tamaño de página (default: 20, vía Pageable) | ✅ |
| `sort` | string | No | Ordenación (vía Pageable) | ✅ |

**Parámetros NO soportados actualmente**: `rarity`, `subtypes`

**Comportamiento del filtro `set`**: A diferencia del resto de los filtros, `set` no filtra sobre un conjunto existente — SELECTEA qué set cargar del caché. Si no se envía, usa `xy1` por defecto.

**Respuesta** (`CardPageResponse`):

```json
{
  "data": [
    {
      "id": "xy1-1",
      "name": "Venusaur",
      "supertype": "Pokémon",
      "subtypes": ["Stage 2"],
      "hp": 140,
      "types": ["Grass"],
      "cardSetId": "xy1",
      "cardSetName": "XY Base Set",
      "imageUrlSmall": "https://images.pokemontcg.io/xy1/1.png",
      "imageUrlLarge": "https://images.pokemontcg.io/xy1/1_hires.png",
      "evolvesFrom": "Ivysaur",
      "attacks": "[{\"name\":\"Leech Seed\",\"cost\":[\"Grass\",\"Colorless\"],\"convertedEnergyCost\":2,\"damage\":\"20\",\"text\":\"Heal 20 damage from this Pokémon.\"}]",
      "weaknesses": "[{\"type\":\"Fire\",\"value\":\"×2\"}]",
      "resistances": "[{\"type\":\"Water\",\"value\":\"-30\"}]",
      "retreatCost": ["Colorless", "Colorless", "Colorless"],
      "aceTactician": false
    }
  ],
  "total": 156,
  "page": 0,
  "size": 20
}
```

> ⚠️ **Importante**: `attacks`, `weaknesses` y `resistances` son **strings JSON**, no objetos. El frontend debe parsearlos con `JSON.parse()` para obtener los arreglos de objetos utilizables. Si vienen `null`, significa que la carta no tiene ataques/debilidades/resistencias.

#### `GET /api/cards/{id}`

Detalle de carta individual. Endpoint existente y funcional.

**Parámetros**:

| Parámetro | Tipo | Obligatorio | Descripción |
|-----------|------|-------------|-------------|
| `id` | string | Sí | ID de la carta (ej. `xy1-1`) |

**Respuesta**: Mismo `CardResponse` individual. Mismos campos JSON string.

#### `POST /api/cards/sync`

Fuerza la sincronización de un set desde la API externa de pokemontcg.io.

**Parámetros**:

| Parámetro | Tipo | Obligatorio | Descripción |
|-----------|------|-------------|-------------|
| `set` | string | Sí | ID del set a sincronizar (ej. `xy1`) |

**Respuesta** (`SyncResponse`):

```json
{
  "message": "Set sincronizado correctamente",
  "set": "xy1",
  "cardsImported": 156,
  "error": null
}
```

### 7.2 Endpoints adicionales necesarios (para completar la Pokédex)

#### `GET /api/cards/sets` (propuesto)

Lista de sets disponibles para el filtro de expansión. Requiere implementación en backend.

```json
{
  "data": [
    { "id": "xy1", "name": "XY Base Set" },
    { "id": "xy2", "name": "Flashfire" }
  ]
}
```

**Mientras no exista**: el frontend usará un input manual de texto para el ID del set.

### 7.3 Servicio Angular

```typescript
// FE/src/app/features/pokedex/data-access/services/pokedex.service.ts
@Injectable({ providedIn: 'root' })
export class PokedexService {
  private http = inject(HttpClient);

  /**
   * Obtiene lista paginada de cartas con filtros.
   * Los campos attacks, weaknesses y resistances vienen como JSON strings.
   */
  getCards(filters: PokedexFilters, page: number, size: number): Observable<CardPageResponse> {
    const params = this.buildParams(filters, page, size);
    return this.http.get<CardPageResponse>('/api/cards', { params });
  }

  getCardById(id: string): Observable<CardResponse> {
    return this.http.get<CardResponse>(`/api/cards/${id}`);
  }

  syncSet(setId: string): Observable<SyncResponse> {
    return this.http.post<SyncResponse>(`/api/cards/sync?set=${setId}`, {});
  }

  // ⏳ Pendiente de implementación backend
  // getSets(): Observable<SetListResponse> { ... }
}
```

---

## 8. Modelos de datos

### 8.1 Frontend — Response Model (exactamente como viene del backend)

```typescript
// FE/src/app/features/pokedex/data-access/models/card-response.ts
// NO MODIFICAR — refleja exactamente la respuesta del backend

export interface CardResponse {
  id: string;
  name: string;
  supertype: string;
  subtypes: string[];
  hp: number | null;
  types: string[];
  cardSetId: string;
  cardSetName: string;
  imageUrlSmall: string;
  imageUrlLarge: string;
  evolvesFrom: string | null;
  attacks: string | null;          // 🔴 JSON string — hay que parsearlo
  weaknesses: string | null;       // 🔴 JSON string — hay que parsearlo
  resistances: string | null;      // 🔴 JSON string — hay que parsearlo
  retreatCost: string[];
  aceTactician: boolean;
}

export interface CardPageResponse {
  data: CardResponse[];
  total: number;
  page: number;
  size: number;
}
```

### 8.2 Frontend — Domain Model (post-parsing, para usar en componentes)

```typescript
// FE/src/app/features/pokedex/domain/models/pokedex-card.ts

export interface PokedexCard {
  id: string;
  name: string;
  supertype: string;
  subtypes: string[];
  hp: number | null;
  types: string[];
  cardSetId: string;
  cardSetName: string;
  imageUrlSmall: string;
  imageUrlLarge: string;
  evolvesFrom: string | null;
  attacks: Attack[] | null;         // ✅ Ya parseado
  weaknesses: WeaknessResistance[] | null;  // ✅ Ya parseado
  resistances: WeaknessResistance[] | null; // ✅ Ya parseado
  retreatCost: string[];
  aceTactician: boolean;
}

export interface Attack {
  name: string;
  cost: string[];
  damage: string;
  text: string;
  convertedEnergyCost: number;
}

export interface WeaknessResistance {
  type: string;
  value: string;  // "×2" | "-30" | etc.
}
```

### 8.3 Utilidad de parseo

```typescript
// FE/src/app/features/pokedex/data-access/utils/parse-card.ts
// Convierte CardResponse (raw del backend) → PokedexCard (domain model)

export function parseCardResponse(raw: CardResponse): PokedexCard {
  return {
    ...raw,
    attacks: safeParseJson<Attack[]>(raw.attacks),
    weaknesses: safeParseJson<WeaknessResistance[]>(raw.weaknesses),
    resistances: safeParseJson<WeaknessResistance[]>(raw.resistances),
  };
}

export function parseCardPageResponse(raw: CardPageResponse): CardPageResponseParsed {
  return {
    ...raw,
    data: raw.data.map(parseCardResponse),
  };
}

function safeParseJson<T>(json: string | null): T | null {
  if (!json) return null;
  try {
    return JSON.parse(json) as T;
  } catch {
    console.warn('Failed to parse JSON field:', json);
    return null;
  }
}
```

### 8.4 CardPageResponse (versión parseada)

```typescript
export interface CardPageResponseParsed {
  data: PokedexCard[];
  total: number;
  page: number;
  size: number;
}
```

### 8.5 Filter Model

```typescript
// FE/src/app/features/pokedex/domain/models/pokedex-filters.ts
export interface PokedexFilters {
  name: string;
  type: string | null;
  supertype: string | null;
  set: string | null;       // ID del set, ej. "xy1"
  // rarity: string | null;  // ⏳ Pendiente backend
}
```

---

## 9. Eventos de usuario

| Evento | Componente | Acción |
|--------|-----------|--------|
| Escribir en search bar | FilterBar | Actualiza signal `filters.name` con debounce 300ms → dispara recarga |
| Click en icono de tipo | TypeRail | Actualiza signal `filters.type` (toggle) → recarga desde página 0 |
| Click en chip de supertype | FilterBar | Actualiza signal `filters.supertype` → recarga desde página 0 |
| Cambiar selector de expansión | FilterBar | Actualiza signal `filters.set` → recarga desde página 0 |
| Click "Limpiar filtros" | FilterBar | Resetea todos los filtros a valores por defecto → recarga |
| Click en chip removible | FilterBar | Quita ese filtro puntual → recarga |
| Click en CardPreview | CardGrid | Navega a `/pokedex/{id}` (o actualiza `selectedCard` si el detalle es panel lateral) |
| Click "Volver" en detalle | CardDetailPage | Navega a `/pokedex` preservando filtros via query params |
| Click página N | Pagination | Cambia `page` signal → recarga cards |
| Click "Reintentar" | Cualquier error | Re-dispara última operación |
| Click "Sincronizar set" | SyncPanel | POST `/api/cards/sync?set={setId}` |

---

## 10. Criterios de aceptación

| ID | Criterio | Relacionado con |
|----|----------|-----------------|
| AC-01 | El listado de cartas se muestra en una grilla responsiva con imágenes visibles. | UC-01 |
| AC-02 | Escribir un nombre parcial en la búsqueda actualiza los resultados. | UC-02 |
| AC-03 | Seleccionar un tipo Pokémon filtra la grilla a solo cartas de ese tipo. | UC-03 |
| AC-04 | Seleccionar un supertype filtra la grilla a cartas de ese supertype. | UC-04 |
| AC-05 | Seleccionar una expansión (por ID) cambia el set de cartas mostradas. | UC-05 |
| AC-06 | Aplicar 3+ filtros simultáneos devuelve resultados que cumplen TODOS los filtros. | UC-06 |
| AC-07 | Hacer clic en una carta navega al detalle con toda la información de la carta. | UC-07 |
| AC-08 | El detalle muestra ataques correctamente parseados con nombre, costo, daño y texto. | UC-08 |
| AC-09 | El detalle muestra debilidad y resistencia correctamente parseadas. | UC-09 |
| AC-10 | El detalle muestra HP y de qué evoluciona. | UC-10 |
| AC-11 | El detalle muestra costo de retirada. | UC-11 |
| AC-12 | "Volver a resultados" retorna a la grilla. | UC-12 |
| AC-13 | Los controles de paginación funcionan y muestran el total de cartas. | UC-13 |
| AC-14 | El mensaje de "No se encontraron cartas" aparece cuando no hay resultados. | Empty state |
| AC-15 | Se muestra skeleton loading mientras se cargan los datos. | Loading state |
| AC-16 | El error de red muestra mensaje amigable con botón "Reintentar". | Error state |
| AC-17 | La interfaz se ve correctamente en mobile, tablet y desktop. | Responsive |
| AC-18 | Todos los elementos interactivos son accesibles por teclado. | Accesibilidad |
| AC-19 | Las imágenes de cartas tienen atributo `alt` descriptivo. | Accesibilidad |
| AC-20 | Los campos JSON (attacks, weaknesses, resistances) se renderizan sin errores aunque estén vacíos o mal formados. | BR-09 |

---

## 11. Consideraciones de accesibilidad

- Todos los botones y enlaces deben tener texto descriptivo o `aria-label`
- Los filtros deben ser navegables por teclado (Tab, Enter, Escape)
- Las imágenes de cartas deben incluir `alt="Carta {nombre}"`
- El panel de filtros debe anunciar cambios con `aria-live="polite"`
- Los mensajes de error deben tener `role="alert"`
- La paginación debe tener `aria-label` y `aria-current="page"` en el botón activo
- El contraste de colores debe cumplir AA (relación 4.5:1 para texto normal)
- Los skeleton loaders deben tener `aria-hidden="true"`

---

## 12. Consideraciones responsive

### Layout por breakpoint

| Rango | Dispositivo | Layout | Grilla cartas |
|-------|-------------|--------|---------------|
| < 640px | Mobile | 1 columna: TypeRail como fila horizontal scrolleable arriba, grilla, FilterBar colapsable, detalle como modal/fullscreen | 2 columnas |
| 640px – 1024px | Tablet | TypeRail como fila horizontal + grilla, FilterBar visible, detalle como overlay o sheet | 3 columnas |
| 1024px – 1280px | Desktop | 3 columnas: TypeRail (~8%) / grilla (~62%) / detalle (~30%) + FilterBar abajo | 4-5 columnas |
| > 1280px | Desktop XL | 3 columnas: TypeRail (~8%) / grilla (~62%) / detalle (~30%) + FilterBar abajo | 5-6 columnas |

### Comportamiento mobile (< 640px)

- El `TypeRail` se convierte en una fila horizontal de iconos con scroll lateral, ubicada arriba de la grilla
- La `FilterBar` se colapsa detrás de un botón "Filtros" (búsqueda, set, supertype)
- La grilla ocupa todo el ancho
- El detalle (`CardDetailPanel`) se muestra como **modal fullscreen** o **sheet deslizante** desde abajo al seleccionar una carta
- La paginación se simplifica a "Anterior" / "Siguiente"
- Las imágenes se cargan en tamaño small para ahorrar ancho de banda

### Comportamiento tablet (640px – 1024px)

- El `TypeRail` se muestra como fila horizontal compacta sobre la grilla
- La `FilterBar` se mantiene visible como barra horizontal
- La grilla ocupa el espacio principal
- El detalle se muestra como **overlay** o **panel deslizante** desde la derecha

### Conservar la identidad visual

En todos los breakpoints, conservar:
- La pantalla de tonos azules con bisel redondeado como contenedor principal
- El fondo de biblioteca como marco decorativo (puede reducirse a un borde delgado en mobile)
- Los acentos dorados/de tipo Pokémon en estados activos y hover

---

## 13. Estructura de componentes

```
features/pokedex/
├── pages/
│   ├── pokedex-page/
│   │   ├── pokedex-page.ts
│   │   ├── pokedex-page.html
│   │   └── pokedex-page.css
│   └── card-detail-page/
│       ├── card-detail-page.ts
│       ├── card-detail-page.html
│       └── card-detail-page.css
├── components/
│   ├── type-rail/
│   │   ├── type-rail.ts
│   │   ├── type-rail.html
│   │   └── type-rail.css
│   ├── filter-bar/
│   │   ├── filter-bar.ts
│   │   ├── filter-bar.html
│   │   └── filter-bar.css
│   ├── card-grid/
│   │   ├── card-grid.ts
│   │   ├── card-grid.html
│   │   └── card-grid.css
│   ├── card-preview/
│   │   ├── card-preview.ts
│   │   ├── card-preview.html
│   │   └── card-preview.css
│   ├── pagination/
│   │   ├── pagination.ts
│   │   ├── pagination.html
│   │   └── pagination.css
│   └── sync-panel/
│       ├── sync-panel.ts
│       ├── sync-panel.html
│       └── sync-panel.css
├── data-access/
│   ├── models/
│   │   └── card-response.ts    ← Raw response types
│   ├── services/
│   │   └── pokedex.service.ts
│   └── utils/
│       └── parse-card.ts       ← Parser JSON → domain model
├── domain/
│   └── models/
│       ├── pokedex-card.ts     ← Domain model post-parsing
│       └── pokedex-filters.ts
└── routes/
    └── pokedex.routes.ts       ← Lazy-loaded child routes
```

### Árbol de dependencias

```
PokedexPage
├── TypeRail ───────────────── emite cambios de filters.type
├── CardGrid ───────────────── recibe lista de cartas
│   ├── CardPreview [] ────── emite click → navega a detalle
│   └── Pagination ─────────── emite cambio de página
├── CardDetailPanel ────────── recibe carta seleccionada
└── FilterBar ──────────────── emite cambios de name/set/supertype + chips removibles
    └── SyncPanel ──────────── (admin-only) botón de sync

CardDetailPage
├── AttackList ─────────────── lista de ataques parseados
│   └── AttackRow [] ───────── ataque individual
├── WeaknessResistance ─────── debilidad y resistencia parseadas
├── RetreatCost ────────────── costo de retirada
└── BackButton ─────────────── volver a resultados
```

---

## 14. Gaps detectados backend

Durante el análisis del código existente se identificaron los siguientes gaps que están del lado del backend y deben ser resueltos para ampliar la funcionalidad de la Pokédex:

| Gap | Impacto | Dónde se arregla |
|-----|---------|------------------|
| **`rarity` no se mapea desde API externa** | No se puede filtrar ni mostrar rareza | `PokemonTCGApiService.toEntity()` no extrae `rarity` del DTO externo |
| **`rarity` no existe en `CardResponse`** | El frontend nunca recibe rareza | `CardCacheService.toCardResponse()` + `Card.java` |
| **`rarity` no es parámetro aceptado** | El controller no recibe filtro de rareza | `CardController.getCards()` no tiene `@RequestParam rarity` |
| **`abilities` no se mapea desde API externa** | No se pueden mostrar habilidades en el detalle | `PokemonTCGApiService.toEntity()` no extrae `abilities` del DTO externo |
| **No hay endpoint `GET /api/cards/sets`** | No se puede poblar dropdown de expansiones | Habría que deducir los sets disponibles del caché o de un cache adicional |
| **El caché solo carga un set por vez** | No se puede consultar "todos los sets" simultáneamente | `CardCacheService` usa `LoadingCache<String, List<Card>>` por set ID |
| **`GET /api/cards/{id}` no traduce "no encontrado" a 404** | `CardCacheService.findById()` lanza `IllegalArgumentException` si la carta o el set no están en caché; sin un `@ExceptionHandler`, esto puede llegar al frontend como un error 500 genérico en vez de un 404 | `CardController` / un `@RestControllerAdvice` (no implementado a la fecha de esta spec) |
| **Filtros `type`/`supertype`/`name` aplican solo dentro del `set` activo** | No existe búsqueda "global" entre sets; cambiar `type` sin cambiar `set` solo filtra dentro del set por defecto (`xy1`) u otro seleccionado | `CardCacheService.getCards()` resuelve `targetSet` primero y filtra sobre esa lista |

> 💡 **Nota para el frontend**: Ninguno de estos gaps bloquea la implementación inicial de la Pokédex. Podés programar todos los componentes con los datos que YA existen y dejar preparados los hooks para cuando el backend agregue estos campos. La spec está ajustada para que lo que programes HOY funcione correctamente con el backend actual.
>
> Para los dos últimos gaps, el frontend debe ser defensivo:
> - En `CardDetailPage`/`CardDetailPanel`, tratar **cualquier error** de `getCardById()` (404, 500 u otro) como "Carta no encontrada" (BR-07), sin asumir un código de estado específico.
> - En el `FilterBar`, dejar claro (mediante texto de ayuda o tooltip) que el selector de **expansión/set** determina el universo de cartas sobre el que aplican el resto de los filtros.

---

## Apéndice A: Referencia visual — Pantalla Pokédex en biblioteca Pokémon

### Concepto general

La interfaz simula una **pantalla de tonos azules** (como un monitor o una Pokédex digital) apoyada en un escritorio dentro de una **biblioteca Pokémon cálida**: estanterías de madera, pokébolas y objetos decorativos visibles como marco alrededor de la pantalla. El foco visual está en la pantalla; la biblioteca es ambientación, no protagonista. NO es una web moderna tipo dashboard plano, pero tampoco es un dispositivo "Nintendo DS" con botones físicos.

### Keywords de estilo

- Pokémon UI
- Pokédex digital screen
- Cozy library desk scene
- RPG inventory
- Pixel/flat illustration aesthetic
- Pokémon TCG collection viewer

### A evitar

- Material UI / Bootstrap / admin panels
- Diseño corporativo / enterprise
- Fondos completamente planos sin la escena de biblioteca
- Consolas con botones de colores tipo Nintendo DS (no aparecen en `img.png`)
- Layout centrado simétrico 33/33/33

### Paleta de colores

| Elemento | Color | Uso |
|----------|-------|-----|
| Fondo biblioteca | Marrón/madera cálido (#7a4a2b aprox.) con dorado | Marco decorativo detrás/alrededor de la pantalla |
| Bisel de pantalla | Azul grisáceo muy oscuro (#1b2838) | Borde redondeado que envuelve toda la UI |
| Barra de título | Azul oscuro (#1f3a5f) | Franja superior con "Pokédex de Cartas" |
| TypeRail | Azul oscuro (#23456e) | Columna angosta de iconos de tipo |
| Panel grilla | Azul medio (#2f5d8a) | Zona central de cartas |
| Panel detalle | Azul medio-oscuro (#274b73) | Columna derecha (carta destacada + descripción) |
| FilterBar | Azul oscuro (#1f3a5f) | Franja inferior de búsqueda y filtros |
| Texto principal | Blanco (#f5f7fa) | Nombres, labels |
| Texto secundario | Celeste grisáceo (#a9c0d8) | Descripciones, stats |
| Borde carta | Tono dorado suave (#d9b35b) | Marco de cada carta en grilla |
| Carta seleccionada | Dorado (#f4c542) | Borde de carta activa |
| Hover carta | Glow dorado claro (#ffe08a) | Elevación sutil al pasar mouse |

### Colores de tipo Pokémon

Usar la paleta oficial para indicadores de tipo:

| Tipo | Color |
|------|-------|
| Grass | #78C850 |
| Fire | #F08030 |
| Water | #6890F0 |
| Lightning | #F8D030 |
| Psychic | #F85888 |
| Fighting | #C03028 |
| Darkness | #705848 |
| Metal | #B8B8D0 |
| Fairy | #EE99AC |
| Dragon | #7038F8 |
| Colorless | #A8A878 |

### Tipografía

- **Familia**: Sistema (Inter, -apple-system, BlinkMacSystemFont, sans-serif)
- **Títulos**: Bold, blanco, tamaño grande
- **Labels de categorías**: Medium, gris claro
- **Texto de cartas**: Regular, blanco, tamaño pequeño
- **Stats (HP, daño)****: Bold, color según tipo

### Efectos visuales

- **Bisel redondeado**: La pantalla completa tiene `border-radius` grande y un borde oscuro grueso que la separa del fondo de biblioteca
- **Profundidad sutil**: Sombra suave hacia afuera de la pantalla (separación del fondo) + sombra interna leve en la zona de la grilla
- **Hover de cartas**: Elevación leve + borde/glow dorado sutil
- **Carta seleccionada**: Borde dorado + sombra externa dorada
- **TypeRail activo**: Icono de tipo resaltado con glow del color de ese tipo
- **Skeleton loading**: Formas en tono azul más claro con animación de pulso
- **Transiciones**: Suaves (200-300ms) para cambios de estado

### Referencia de imagen

La referencia visual principal está en: `FE/public/images/img.png`

Esa imagen muestra:
- Una pantalla de tonos azules con bisel redondeado, título "Pokédex de Cartas" arriba
- Columna angosta a la izquierda con iconos de tipo apilados
- Grilla central con cartas en marcos pequeños, varias filas y columnas
- Columna derecha con una carta ampliada destacada + texto descriptivo debajo
- Franja inferior con buscador, selector de set y filtros de tipo
- Fondo de escritorio/biblioteca Pokémon (madera, pokébolas, decoración) visible alrededor de la pantalla
- Distribución aproximada: TypeRail ~8% / Grilla ~62% / Detalle ~30%, FilterBar de ancho completo abajo

---

*Documento generado como parte del flujo SDD — Spec-Driven Development.*  
*Actualizado a versión 1.3 — diseño visual realineado con `FE/public/images/img.png` (pantalla azul + escritorio de biblioteca, sin marco metálico/consola Nintendo DS).*
