# Opencode Deck Builder Prompt

## Contexto

Estamos trabajando en el frontend Angular del proyecto Pokémon TCG.

La feature **Deck Builder** ya existe y ya fue conectada al backend real para las operaciones principales:

```txt
GET /api/cards
POST /api/decks
```

Mi responsabilidad actual es continuar desarrollando y puliendo el **Deck Builder desde el frontend**.

No debo tocar backend, engine, controllers, DTOs Java, services Java ni base de datos.

El flujo general esperado de la app es:

```txt
Iniciar sesión → Lobby → Creación de Mazo
```

Esta tarea empieza únicamente en la pantalla de **Creación de Mazo**.

La autenticación, JWT, guards, interceptor, login, register, lobby y navegación previa son responsabilidad de otras partes del proyecto. Deck Builder debe apoyarse en lo que ya exista, no reimplementar nada de eso.

---

## Objetivo actual

Continuar el desarrollo del **Deck Builder** usando la integración real ya existente.

La pantalla debe permitir y mejorar:

* Cargar/listar cartas desde el backend real.
* Buscar cartas usando `/api/cards`.
* Filtrar cartas usando parámetros soportados por el contrato.
* Paginar resultados del catálogo.
* Agregar cartas al mazo.
* Quitar cartas del mazo.
* Aumentar y disminuir cantidades.
* Ingresar nombre del mazo.
* Ver resumen del mazo.
* Ver contador total de cartas.
* Ver cantidad de cartas únicas.
* Guardar mazo usando `/api/decks`.
* Mostrar validaciones devueltas por el backend.
* Mostrar errores y mensajes de éxito claros.
* Mejorar estados de loading, empty, error y success.
* Mejorar la estética retro / pixel-art / Game Boy.
* Mantener mocks solo como fallback de desarrollo, no como flujo principal.

---

## Regla principal

Usar:

```txt
Contrato real de API + servicios reales ya integrados
```

El flujo principal esperado es:

```txt
DeckBuilderPage
   ↓
CardApi / DeckApi
   ↓
RealCardApiService / RealDeckApiService
   ↓
Backend real
```

No volver el provider principal a mocks.

Los mocks pueden permanecer en el proyecto, pero solo como fallback para desarrollo aislado.

---

## Documentos que debés leer antes de implementar

Leé y respetá estos documentos:

```txt
docs/frontend/deck-builder/DECK_BUILDER_SCOPE.md
docs/frontend/deck-builder/DECK_BUILDER_API_CONTRACT.md
docs/frontend/deck-builder/DECK_BUILDER_ARCHITECTURE.md
docs/frontend/deck-builder/DECK_BUILDER_TASKS.md
docs/frontend/deck-builder/DECK_BUILDER_UI_UX_GUIDELINES.md
```

Si existe `AGENTS.md`, `README.md` o algún documento de convenciones del proyecto, respetalo también.

---

## Restricción absoluta

No modificar:

```txt
BE/
engine
backend DTOs
backend controllers
backend services
backend tests
base de datos
```

Si aparece un problema que parece venir del backend, reportarlo claramente en el resumen final, pero no corregirlo desde esta tarea.

---

## Reglas estrictas

No implementar autenticación.

No implementar JWT.

No modificar interceptor.

No modificar guards.

No implementar lobby.

No implementar game board.

No implementar WebSocket.

No modificar backend.

No modificar engine.

No modificar DTOs Java.

No modificar controllers Java.

No modificar services Java.

No cambiar el provider principal a mocks.

No hardcodear arrays de cartas dentro de componentes.

No introducir librerías visuales nuevas.

No reemplazar ni romper auth existente.

No reemplazar ni romper pokedex existente.

No duplicar reglas completas del TCG en frontend.

No mostrar stack traces ni errores crudos al usuario.

---

## Excepciones permitidas

Se puede modificar:

```txt
FE/src/app/features/deck-builder/
```

También se permiten cambios mínimos frontend si son necesarios para compilar o integrar la feature, por ejemplo:

```txt
FE/src/app/app.routes.ts
```

Solo si el cambio es estrictamente necesario.

No tocar `BE/`.

---

## Ubicación principal de la feature

Trabajar principalmente dentro de:

```txt
FE/src/app/features/deck-builder/
```

Estructura esperada o aproximada:

```txt
features/deck-builder/
├── pages/
│   └── deck-builder-page/
│       ├── deck-builder-page.ts
│       ├── deck-builder-page.html
│       └── deck-builder-page.css
│
├── components/
│   ├── card-search-panel/
│   ├── card-result-list/
│   ├── deck-current-list/
│   ├── deck-summary/
│   └── deck-validation-panel/
│
├── data-access/
│   ├── card-api.service.ts
│   ├── real-card-api.service.ts
│   ├── mock-card-api.service.ts
│   ├── deck-api.service.ts
│   ├── real-deck-api.service.ts
│   ├── mock-deck-api.service.ts
│   └── deck-builder-api.provider.ts
│
├── models/
│   ├── api-error.model.ts
│   ├── card.model.ts
│   ├── deck-builder-state.model.ts
│   ├── deck.model.ts
│   ├── deck-request.model.ts
│   └── deck-validation.model.ts
│
├── mocks/
│   ├── mock-cards.ts
│   ├── mock-decks.ts
│   └── mock-validation.ts
│
└── utils/
    └── deck-builder-mappers.ts
```

Si el proyecto ya tiene una convención distinta, adaptarse a esa convención, pero mantener la separación conceptual:

```txt
pages        → pantallas principales
components   → componentes visuales
data-access  → servicios
models       → interfaces TypeScript
mocks        → datos mockeados fallback
utils        → funciones puras de transformación
```

---

## Estado actual esperado

Antes de cambiar código, revisar que existan o ubicar equivalentes de:

```txt
DeckBuilderPage
CardApi
DeckApi
RealCardApiService
RealDeckApiService
MockCardApiService
MockDeckApiService
deck-builder-api.provider.ts
deck-builder-mappers.ts
```

El provider principal debe usar servicios reales:

```ts
{
  provide: CardApi,
  useClass: RealCardApiService,
},
{
  provide: DeckApi,
  useClass: RealDeckApiService,
}
```

No cambiarlo a mocks.

---

## Reglas Angular

El proyecto usa Angular moderno. Implementar respetando:

* Standalone components.
* No crear NgModules.
* `ChangeDetectionStrategy.OnPush`.
* `inject()` cuando sea posible.
* Signals para estado local.
* `computed` para datos derivados.
* Reactive Forms para el formulario del nombre del mazo.
* Control flow moderno: `@if`, `@for`, `@switch`.
* Templates simples.
* Componentes chicos y con responsabilidad clara.
* Evitar lógica pesada en HTML.
* Evitar suscripciones sin limpiar.
* Usar `takeUntilDestroyed()` si hacen falta subscriptions manuales.

---

## Ruta esperada

La ruta actual esperada es:

```txt
/decks/new
```

Debe cargar la pantalla principal del Deck Builder.

No hace falta implementar navegación desde el lobby.

No hace falta crear listado de mazos.

No hace falta crear detalle de mazo.

No hace falta crear edición de mazo en esta fase.

---

## Contrato de cartas

El catálogo usa:

```http
GET /api/cards
```

Query params soportados:

```txt
name
set
supertype
type
page
size
sort
```

Modelo esperado:

```ts
export interface CardPageResponse {
  data: CardResponse[];
  total: number;
  page: number;
  size: number;
}

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
  aceTactician: boolean;
  evolvesFrom: string | null;
  attacks: string | null;
  weaknesses: string | null;
  resistances: string | null;
  retreatCost: string[];
}

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

---

## Contrato de mazos

Crear mazo usa:

```http
POST /api/decks
```

Request esperado:

```ts
export interface CreateDeckRequest {
  name: string;
  cards: DeckCardEntry[];
}

export interface UpdateDeckRequest {
  name: string;
  cards: DeckCardEntry[];
}

export interface DeckCardEntry {
  cardId: string;
  quantity: number;
}
```

Response esperado:

```ts
import { DeckValidationError } from './deck-validation.model';

export interface DeckResponse {
  id: number;
  name: string;
  valid: boolean;
  cardCount: number;
  cards: DeckCardResponse[];
  validationErrors: DeckValidationError[];
  createdAt: string;
}

export interface DeckCardResponse {
  cardId: string;
  cardName: string;
  quantity: number;
  supertype: string;
  types: string[];
  subtypes: string[];
  imageUrlSmall: string;
}

export type DeckListResponse = DeckResponse[];
```

Validación esperada:

```ts
export interface DeckValidationResponse {
  valid: boolean;
  errors: DeckValidationError[];
  cardCount: number;
}

export interface DeckValidationError {
  code: string;
  message: string;
  cardId: string | null;
}
```

Error API:

```ts
export interface ErrorApi {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
```

Estado interno de UI:

```ts
import { CardResponse } from './card.model';

export interface DeckBuilderCard {
  card: CardResponse;
  quantity: number;
}
```

---

## Mapper obligatorio

El estado interno puede tener datos completos de carta para la UI.

Pero antes de guardar debe transformarse a request real.

Usar o mantener una función equivalente a:

```ts
export function toCreateDeckRequest(
  name: string,
  deckCards: DeckBuilderCard[]
): CreateDeckRequest {
  return {
    name,
    cards: deckCards.map(item => ({
      cardId: item.card.id,
      quantity: item.quantity,
    })),
  };
}
```

El request no debe enviar:

```txt
cardName
imageUrlSmall
supertype
types
subtypes
```

Solo debe enviar:

```txt
name
cards.cardId
cards.quantity
```

---

# Fase actual — Estabilizar integración real

## Objetivo

Mejorar el funcionamiento real del Deck Builder conectado al backend, sin tocar backend.

---

## Tarea 1 — Verificar provider real

Revisar:

```txt
FE/src/app/features/deck-builder/data-access/deck-builder-api.provider.ts
```

Confirmar que use:

```txt
RealCardApiService
RealDeckApiService
```

No cambiar a mocks.

---

## Tarea 2 — Agregar paginación del catálogo

Agregar estado para:

```txt
currentPage
pageSize
totalResults
totalPages
```

Usar la respuesta real:

```ts
CardPageResponse.total
CardPageResponse.page
CardPageResponse.size
```

El catálogo no debe quedar fijo en:

```txt
page = 0
size = 20
```

---

## Tarea 3 — Agregar controles de paginación

Agregar controles simples:

```txt
Anterior
Siguiente
Página X de Y
Total: N cartas
```

Comportamiento esperado:

* `Anterior` deshabilitado en página 0.
* `Siguiente` deshabilitado si no hay más páginas.
* Cambiar página vuelve a llamar `CardApi.getCards(...)`.
* Mantener búsqueda y filtros activos.
* Mostrar loading mientras carga.

---

## Tarea 4 — Agregar debounce en búsqueda

Evitar requests por cada tecla.

Comportamiento esperado:

* Esperar aproximadamente `300ms`.
* Resetear página a `0` cuando cambia búsqueda.
* Evitar requests duplicadas si el texto no cambió.
* Mantener loading/error/empty state.

---

## Tarea 5 — Revisar filtros soportados

El contrato soporta:

```txt
name
set
supertype
type
page
size
sort
```

Prioridad sugerida:

```txt
1. name
2. supertype
3. type
4. set
```

No inventar filtros que el backend no soporte.

---

## Tarea 6 — Mejorar estados visuales

La pantalla debe mostrar claramente:

```txt
Cargando cartas...
No se pudieron cargar las cartas.
No se encontraron cartas.
Guardando mazo...
No se pudo guardar el mazo.
Mazo guardado correctamente.
```

No mostrar errores crudos del backend.

Transformar errores a mensajes entendibles.

---

## Tarea 7 — Mejorar validación real

Al guardar con `/api/decks`, el backend puede devolver:

```txt
valid
cardCount
validationErrors
```

La UI debe mostrar:

* Si el mazo quedó válido o inválido.
* Cantidad total de cartas.
* Errores de validación del backend.
* Mensaje claro cuando todavía no se guardó/validó.

No duplicar reglas completas del TCG en frontend.

Frontend solo valida reglas mínimas de UX:

```txt
nombre requerido
nombre máximo 100 caracteres
mazo no vacío
cantidades mayores a 0
no permitir cantidades negativas
```

---

## Tarea 8 — Manejar 401 / 403

Si al guardar el backend responde 401 o 403, mostrar un mensaje simple:

```txt
Tu sesión no es válida o expiró. Volvé a iniciar sesión.
```

No implementar login.

No implementar JWT.

No modificar interceptor.

No modificar guards.

---

## Tarea 9 — Agregar acción “Limpiar mazo”

Agregar botón secundario para vaciar el mazo actual.

Comportamiento esperado:

* Elimina todas las cartas del mazo en memoria.
* Resetea validación mostrada.
* Limpia mensaje de éxito/error de guardado.
* No llama al backend.
* No borra el nombre del mazo salvo que se decida explícitamente.

---

## Tarea 10 — Reemplazar placeholders

Si existen textos o botones como:

```txt
Acción futura
```

reemplazarlos por acciones reales o quitarlos.

Acciones recomendadas:

```txt
Guardar mazo
Limpiar mazo
Reintentar carga
```

No agregar edición, borrado o listado de mazos en esta fase.

---

# Fase visual — Pulido retro / Game Boy

Después o junto con la estabilización, mejorar la estética sin introducir librerías nuevas.

## Objetivos visuales

* Mantener estética retro / pixel-art / Game Boy.
* Evitar pantalla genérica tipo dashboard.
* Usar paneles claros y bien separados.
* Dar jerarquía al botón `Guardar mazo`.
* Hacer visible el contador `0/60`.
* Hacer que el catálogo se vea como colección de cartas.
* Hacer que el mazo actual sea fácil de leer.

## Layout recomendado

En desktop puede usarse layout de tres zonas:

```txt
Filtros / búsqueda
Catálogo y mazo actual
Resumen / validación / acciones
```

En mobile, apilar secciones:

```txt
Nombre del mazo
Filtros
Catálogo
Mazo actual
Resumen
Validación
Acciones
```

---

## Componentes esperados

### DeckBuilderPage

Debe:

* Manejar formulario de nombre.
* Cargar cartas reales.
* Manejar búsqueda.
* Manejar filtros.
* Manejar paginación.
* Mantener estado del mazo.
* Agregar cartas.
* Quitar cartas.
* Aumentar cantidad.
* Disminuir cantidad.
* Calcular total.
* Calcular cantidad de cartas únicas.
* Guardar mazo con backend real.
* Mostrar validación real.
* Mostrar error/éxito.
* Conectar componentes hijos.

---

### CardSearchPanel

Debe:

* Mostrar input de búsqueda.
* Mostrar filtros simples.
* Emitir cambios de búsqueda/filtros.
* No llamar servicios.

---

### CardResultList

Debe:

* Mostrar cartas disponibles.
* Mostrar loading.
* Mostrar error.
* Mostrar estado sin resultados.
* Mostrar imagen chica, nombre, supertype y types.
* Permitir agregar carta.
* Emitir `addCard`.
* No llamar servicios.

---

### DeckCurrentList

Debe:

* Mostrar cartas agregadas.
* Mostrar cantidad por carta.
* Permitir aumentar cantidad.
* Permitir disminuir cantidad.
* Permitir quitar carta.
* Emitir `cardId`.
* No modificar estado global directamente.

---

### DeckSummary

Debe:

* Mostrar total de cartas.
* Mostrar cantidad de cartas únicas.
* Mostrar estado general.
* Mostrar contador visible.
* Mostrar botón de guardado si se ubica ahí.
* Emitir `save`.

---

### DeckValidationPanel

Debe:

* Mostrar si el mazo es válido o inválido.
* Mostrar errores de validación.
* Mostrar contador actual.
* Mostrar mensaje informativo si todavía no se validó.
* No calcular reglas complejas del TCG.

---

## Verificación

Desde `FE`, ejecutar:

```bash
npm install
npm run build
```

Si el proyecto usa lockfile y corresponde:

```bash
npm ci
npm run build
```

Ejecutar tests si existen:

```bash
npm test
```

Si aparecen errores preexistentes no relacionados con Deck Builder, reportarlos por separado.

No ejecutar ni modificar backend como parte de esta tarea.

---

## Entregable final

Al terminar, devolver un informe con:

```txt
1. Archivos creados.
2. Archivos modificados.
3. Qué se implementó.
4. Cómo probar `/decks/new`.
5. Qué quedó conectado al backend real.
6. Qué quedó mockeado como fallback.
7. Limitaciones conocidas.
8. Resultado de build.
9. Resultado de tests, si aplica.
10. Confirmación de que no se tocó backend, engine, auth, lobby, pokedex ni game.
```

---

## Criterios de aceptación

La implementación se considera correcta si:

* `/decks/new` abre el Deck Builder.
* Las cartas se cargan desde `/api/cards`.
* El guardado usa `/api/decks`.
* El provider principal sigue usando servicios reales.
* Se puede buscar por nombre.
* La búsqueda tiene debounce.
* Se puede paginar el catálogo.
* Se muestra total de resultados.
* Cambiar búsqueda vuelve a página 0.
* Se puede agregar una carta al mazo.
* Se puede aumentar cantidad.
* Se puede disminuir cantidad.
* Se puede quitar carta.
* Se puede limpiar el mazo.
* Se muestra total de cartas.
* Se muestra cantidad de cartas únicas.
* Se valida nombre requerido.
* No se puede guardar mazo vacío.
* Se muestran errores reales de validación del backend.
* Se muestran loading/error/empty/success states.
* No quedan placeholders visibles como `Acción futura`.
* Los componentes no dependen directamente de arrays hardcodeados.
* Los mocks quedan disponibles solo como fallback.
* No se implementa auth.
* No se toca backend.
* No se toca engine.
* No se toca lobby.
* No se toca pokedex.
* No se toca game board.
* El build frontend pasa o se reportan errores claramente.
