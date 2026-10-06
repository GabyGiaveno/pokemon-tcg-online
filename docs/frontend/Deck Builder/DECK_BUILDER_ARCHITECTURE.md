# Deck Builder Architecture

## Objetivo

Definir la arquitectura frontend de la feature **Deck Builder** dentro del proyecto Angular.

La feature permite construir mazos usando el contrato real de la API backend y actualmente ya se encuentra conectada a servicios reales para las operaciones principales:

```txt id="2akyvn"
GET /api/cards
POST /api/decks
```

La arquitectura debe mantener la feature aislada, clara y fácil de extender, sin tocar backend, engine ni otras features fuera del alcance.

---

## Estado actual de integración

El flujo principal esperado es:

```txt id="2hcp77"
DeckBuilderPage
   ↓
CardApi / DeckApi
   ↓
RealCardApiService / RealDeckApiService
   ↓
Backend real
```

Los servicios mock siguen existiendo como alternativa de desarrollo aislado:

```txt id="5lcmgv"
DeckBuilderPage
   ↓
CardApi / DeckApi
   ↓
MockCardApiService / MockDeckApiService
   ↓
Mock data
```

Pero los mocks no deben ser el flujo principal de la feature salvo indicación explícita.

---

## Principios de arquitectura

La feature debe respetar estos principios:

* Mantener la feature aislada dentro de `features/deck-builder`.
* No implementar lógica de autenticación propia.
* No tocar backend.
* No tocar engine.
* No tocar lobby salvo integración mínima de ruta si hace falta.
* No tocar pokedex.
* No tocar game board.
* No tocar WebSocket.
* Usar contrato real de API.
* Usar servicios reales ya integrados como flujo principal.
* Mantener mocks como fallback de desarrollo.
* Separar UI, estado, modelos, mocks y acceso a datos.
* Evitar lógica pesada dentro de componentes visuales chicos.
* Mantener componentes reutilizables dentro de la feature.
* Evitar dependencias innecesarias con otras features.
* Permitir cambiar entre servicios reales y mocks desde providers, sin reescribir componentes.

---

## Ubicación principal

La feature debe vivir en:

```txt id="lnft1w"
FE/src/app/features/deck-builder/
```

Estructura esperada o aproximada:

```txt id="079g4y"
features/deck-builder/
├── pages/
│   └── deck-builder-page/
│       ├── deck-builder-page.ts
│       ├── deck-builder-page.html
│       └── deck-builder-page.css
│
├── components/
│   ├── card-search-panel/
│   │   ├── card-search-panel.ts
│   │   ├── card-search-panel.html
│   │   └── card-search-panel.css
│   │
│   ├── card-result-list/
│   │   ├── card-result-list.ts
│   │   ├── card-result-list.html
│   │   └── card-result-list.css
│   │
│   ├── deck-current-list/
│   │   ├── deck-current-list.ts
│   │   ├── deck-current-list.html
│   │   └── deck-current-list.css
│   │
│   ├── deck-summary/
│   │   ├── deck-summary.ts
│   │   ├── deck-summary.html
│   │   └── deck-summary.css
│   │
│   └── deck-validation-panel/
│       ├── deck-validation-panel.ts
│       ├── deck-validation-panel.html
│       └── deck-validation-panel.css
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

La estructura puede adaptarse al estilo ya existente del proyecto, pero debe mantener estas responsabilidades separadas.

---

# Capas de la feature

## 1. Page

La carpeta `pages` contiene pantallas completas.

Para esta feature, la pantalla principal es:

```txt id="g0v0ew"
deck-builder-page
```

Esta pantalla coordina toda la feature.

Responsabilidades:

* Crear y manejar el formulario del nombre del mazo.
* Cargar cartas desde `CardApi`.
* Mantener el estado del mazo en construcción.
* Recibir eventos de los componentes hijos.
* Agregar cartas.
* Quitar cartas.
* Aumentar cantidades.
* Disminuir cantidades.
* Transformar estado local a request API.
* Guardar mazo usando `DeckApi`.
* Mostrar validaciones devueltas por el backend.
* Mostrar estados de carga/error/éxito.
* Manejar búsqueda, filtros y paginación.
* Conectar los componentes visuales.

La page puede tener lógica de orquestación, pero no debe tener lógica visual detallada que pueda vivir en componentes hijos.

---

## 2. Components

La carpeta `components` contiene componentes reutilizables dentro de la feature.

Los componentes deben ser presentacionales siempre que sea razonable.

Esto significa que deben recibir datos por `input` y emitir acciones mediante `output`.

No deben llamar directamente a servicios de API o mocks.

Ejemplos:

```txt id="853dnn"
CardSearchPanel
CardResultList
DeckCurrentList
DeckSummary
DeckValidationPanel
```

---

## 3. Data Access

La carpeta `data-access` contiene servicios relacionados con obtener o enviar datos.

Debe existir una separación entre:

```txt id="s8v1x3"
Contrato abstracto
Implementación real
Implementación mock fallback
Provider para elegir implementación
```

Durante la etapa actual, la feature debe usar los servicios reales.

---

## 4. Models

La carpeta `models` contiene interfaces TypeScript.

Deben respetar el contrato real de la API.

No deben mezclarse con mocks ni lógica visual.

---

## 5. Mocks

La carpeta `mocks` contiene datos falsos con forma de API real.

Los mocks no deben estar hardcodeados dentro de componentes.

Los mocks se conservan para:

* Desarrollo visual aislado.
* Pruebas sin backend levantado.
* Simular errores de UI.
* Comparar forma de responses contra el contrato real.

---

## 6. Utils

La carpeta `utils` puede contener funciones puras para transformar datos.

Ejemplo:

```txt id="c9q5od"
DeckBuilderCard[] → CreateDeckRequest
```

Estas funciones no deben depender de Angular ni de servicios.

---

# Arquitectura de datos

## Flujo principal actual

```txt id="r9j0kw"
DeckBuilderPage
   ↓
CardApi / DeckApi
   ↓
RealCardApiService / RealDeckApiService
   ↓
Backend real
```

## Flujo alternativo con mocks

```txt id="mcbidz"
DeckBuilderPage
   ↓
CardApi / DeckApi
   ↓
MockCardApiService / MockDeckApiService
   ↓
mock-cards.ts / mock-decks.ts / mock-validation.ts
```

Los componentes visuales no deben saber si los datos vienen de servicios reales o mocks.

---

# Servicios esperados

## CardApi

Archivo sugerido:

```txt id="j6hxyt"
data-access/card-api.service.ts
```

Debe definir el contrato para obtener cartas.

```ts id="ndnnu3"
import { Observable } from 'rxjs';
import { CardPageResponse, CardResponse, CardSearchParams } from '../models/card.model';

export abstract class CardApi {
  abstract getCards(params: CardSearchParams): Observable<CardPageResponse>;
  abstract getCardById(id: string): Observable<CardResponse>;
}
```

---

## RealCardApiService

Archivo sugerido:

```txt id="ciewb3"
data-access/real-card-api.service.ts
```

Responsabilidad:

* Consumir `GET /api/cards`.
* Consumir `GET /api/cards/{id}` si la UI lo necesita.
* Enviar query params soportados por el contrato:

```txt id="uik17h"
name
set
supertype
type
page
size
sort
```

Debe usar `HttpClient`.

Debe transformar o propagar errores de forma que la UI pueda mostrarlos con mensajes claros.

---

## MockCardApiService

Archivo sugerido:

```txt id="a8gck7"
data-access/mock-card-api.service.ts
```

Debe implementar `CardApi`.

Responsabilidades:

* Devolver cartas mockeadas.
* Simular búsqueda por nombre.
* Simular filtros simples.
* Simular paginación básica.
* Simular delay corto si se quiere representar carga.
* Simular error de carga si se necesita probar UI de error.

No debe hacer requests HTTP reales.

No debe ser el provider principal salvo indicación explícita.

---

## DeckApi

Archivo sugerido:

```txt id="sdtsqf"
data-access/deck-api.service.ts
```

Debe definir el contrato de operaciones de mazos.

```ts id="sfc9ru"
import { Observable } from 'rxjs';
import { DeckResponse } from '../models/deck.model';
import { CreateDeckRequest, UpdateDeckRequest } from '../models/deck-request.model';
import { DeckValidationResponse } from '../models/deck-validation.model';

export abstract class DeckApi {
  abstract getDecks(): Observable<DeckResponse[]>;
  abstract getDeckById(id: number): Observable<DeckResponse>;
  abstract createDeck(request: CreateDeckRequest): Observable<DeckResponse>;
  abstract updateDeck(id: number, request: UpdateDeckRequest): Observable<DeckResponse>;
  abstract deleteDeck(id: number): Observable<void>;
  abstract validateDeck(id: number): Observable<DeckValidationResponse>;
}
```

---

## RealDeckApiService

Archivo sugerido:

```txt id="tf17m6"
data-access/real-deck-api.service.ts
```

Responsabilidad:

* Consumir `GET /api/decks`.
* Consumir `GET /api/decks/{id}`.
* Consumir `POST /api/decks`.
* Consumir `PUT /api/decks/{id}`.
* Consumir `DELETE /api/decks/{id}`.
* Consumir `POST /api/decks/{id}/validate`.

Para la pantalla actual de creación, el endpoint principal es:

```http id="iq3dvn"
POST /api/decks
```

Debe usar `HttpClient`.

La autenticación debe depender del interceptor global existente, no de lógica propia dentro de Deck Builder.

---

## MockDeckApiService

Archivo sugerido:

```txt id="fyrwgv"
data-access/mock-deck-api.service.ts
```

Debe implementar `DeckApi`.

Responsabilidades:

* Simular creación de mazo.
* Simular actualización de mazo.
* Simular obtención de mazos si hace falta.
* Simular validación de mazo.
* Calcular `cardCount`.
* Devolver `DeckResponse`.
* Devolver `DeckValidationResponse`.
* Simular errores de guardado o validación para probar UI.

No debe hacer requests HTTP reales.

No debe ser el provider principal salvo indicación explícita.

---

# Provider de data-access

Archivo sugerido:

```txt id="0kc9wu"
data-access/deck-builder-api.provider.ts
```

Debe centralizar qué implementación se usa.

Durante la etapa actual, debe usar servicios reales:

```ts id="6tu8sn"
export const DECK_BUILDER_API_PROVIDERS = [
  {
    provide: CardApi,
    useClass: RealCardApiService,
  },
  {
    provide: DeckApi,
    useClass: RealDeckApiService,
  },
];
```

Los mocks pueden quedar disponibles para fallback:

```ts id="jrx07h"
export const DECK_BUILDER_MOCK_API_PROVIDERS = [
  {
    provide: CardApi,
    useClass: MockCardApiService,
  },
  {
    provide: DeckApi,
    useClass: MockDeckApiService,
  },
];
```

La page debe depender de `CardApi` y `DeckApi`, no directamente de `RealCardApiService`, `RealDeckApiService`, `MockCardApiService` ni `MockDeckApiService`.

---

# Modelos principales

La feature debe usar los modelos definidos en `DECK_BUILDER_API_CONTRACT.md`.

Modelos principales:

```txt id="gxk53c"
CardPageResponse
CardResponse
CardSearchParams
CreateDeckRequest
UpdateDeckRequest
DeckCardEntry
DeckResponse
DeckCardResponse
DeckValidationResponse
DeckValidationError
ErrorApi
DeckBuilderCard
```

---

# Estado interno de la pantalla

El estado local de `DeckBuilderPage` debe poder representar:

```txt id="wht2fy"
cartas disponibles
búsqueda actual
filtros actuales
página actual
tamaño de página
total de resultados
mazo en construcción
resultado de validación
estado de carga de cartas
estado de guardado
errores
mensaje de éxito
```

Se recomienda usar signals.

Ejemplo conceptual:

```ts id="sr3gtu"
readonly cards = signal<CardResponse[]>([]);
readonly deckCards = signal<DeckBuilderCard[]>([]);
readonly validationResult = signal<DeckValidationResponse | null>(null);

readonly currentPage = signal(0);
readonly pageSize = signal(20);
readonly totalResults = signal(0);

readonly loadingCards = signal(false);
readonly savingDeck = signal(false);

readonly cardsLoadError = signal<string | null>(null);
readonly saveError = signal<string | null>(null);
readonly saveSuccess = signal<string | null>(null);
```

Estados calculados con `computed`:

```ts id="ots6el"
readonly totalDeckCards = computed(() =>
  this.deckCards().reduce((total, item) => total + item.quantity, 0)
);

readonly uniqueCards = computed(() => this.deckCards().length);

readonly totalPages = computed(() =>
  Math.ceil(this.totalResults() / this.pageSize())
);

readonly canSave = computed(() =>
  this.deckForm.valid && this.totalDeckCards() > 0 && !this.savingDeck()
);
```

---

# Formulario

El nombre del mazo debe manejarse con Reactive Forms.

Ejemplo conceptual:

```ts id="s5xenf"
readonly deckForm = this.fb.nonNullable.group({
  name: ['', [Validators.required, Validators.maxLength(100)]],
});
```

Validaciones mínimas:

* Nombre requerido.
* Nombre máximo 100 caracteres.
* No guardar si el mazo está vacío.
* No guardar mientras está en progreso el guardado.

Las reglas completas del TCG deben quedar del lado del backend.

---

# Flujo de carga de cartas

Al abrir la pantalla:

```txt id="d55jpg"
1. DeckBuilderPage inicializa.
2. Llama a CardApi.getCards({ page: 0, size: 20 }).
3. Activa loadingCards.
4. Recibe CardPageResponse.
5. Guarda cards, total, page y size en signals.
6. Desactiva loadingCards.
7. Si falla, guarda cardsLoadError.
```

---

# Flujo de búsqueda

Cuando el usuario busca por nombre:

```txt id="ztshqp"
1. CardSearchPanel emite término de búsqueda.
2. DeckBuilderPage espera debounce.
3. DeckBuilderPage actualiza búsqueda.
4. Resetea currentPage a 0.
5. Llama a CardApi.getCards(params).
6. CardResultList muestra resultados.
7. Si no hay resultados, se muestra estado vacío.
```

La búsqueda debe evitar requests excesivas por cada tecla.

---

# Flujo de filtros

Cuando el usuario cambia filtros:

```txt id="5zsffr"
1. CardSearchPanel emite filtros.
2. DeckBuilderPage actualiza filtros.
3. Resetea currentPage a 0.
4. Llama a CardApi.getCards(params).
5. Mantiene loading/error/empty state.
```

Solo se deben usar filtros soportados por el contrato:

```txt id="dcr7zl"
name
set
supertype
type
page
size
sort
```

---

# Flujo de paginación

Cuando el usuario cambia de página:

```txt id="w8w7ci"
1. DeckBuilderPage actualiza currentPage.
2. Mantiene búsqueda y filtros actuales.
3. Llama a CardApi.getCards(params).
4. Actualiza cards, total, page y size.
5. Muestra loading mientras carga.
```

La UI debe mostrar:

```txt id="6lz73i"
Anterior
Siguiente
Página X de Y
Total: N cartas
```

---

# Flujo de agregar carta

Cuando el usuario presiona "Agregar":

```txt id="jv0x8w"
1. CardResultList emite la carta seleccionada.
2. DeckBuilderPage verifica si la carta ya existe en deckCards.
3. Si no existe, la agrega con quantity = 1.
4. Si existe, incrementa quantity.
5. Se recalculan totalDeckCards y uniqueCards.
6. Se limpia o actualiza validationResult si corresponde.
```

---

# Flujo de quitar carta

Cuando el usuario presiona "Quitar":

```txt id="9247ht"
1. DeckCurrentList emite cardId.
2. DeckBuilderPage elimina esa carta del mazo.
3. Se recalculan totalDeckCards y uniqueCards.
4. Se limpia o actualiza validationResult si corresponde.
```

---

# Flujo de aumentar cantidad

Cuando el usuario presiona "+":

```txt id="1zu1tt"
1. DeckCurrentList emite cardId.
2. DeckBuilderPage incrementa quantity.
3. Se recalculan totalDeckCards y uniqueCards.
4. No llama al backend.
```

---

# Flujo de disminuir cantidad

Cuando el usuario presiona "-":

```txt id="3a4w3x"
1. DeckCurrentList emite cardId.
2. DeckBuilderPage decrementa quantity.
3. Si quantity llega a 0, se puede quitar la carta del mazo.
4. No se permiten cantidades negativas.
5. No llama al backend.
```

---

# Flujo de limpiar mazo

Cuando el usuario presiona "Limpiar mazo":

```txt id="swc6q2"
1. DeckBuilderPage vacía deckCards.
2. Resetea validationResult.
3. Limpia mensajes de éxito/error de guardado.
4. No llama al backend.
5. No borra el nombre del mazo salvo decisión explícita.
```

---

# Flujo de guardado

Cuando el usuario presiona "Guardar mazo":

```txt id="7yf5ms"
1. DeckBuilderPage valida formulario.
2. Verifica que haya al menos una carta.
3. Transforma DeckBuilderCard[] a CreateDeckRequest.
4. Llama a DeckApi.createDeck(request).
5. Activa savingDeck.
6. Si responde OK:
   - guarda response si hace falta
   - muestra mensaje de éxito
   - muestra validación incluida en DeckResponse
7. Si falla:
   - muestra mensaje de error entendible
8. Desactiva savingDeck.
```

---

# Transformación a CreateDeckRequest

El estado interno puede tener información completa de la carta para mostrar en UI.

Pero el request real debe enviar solamente:

```txt id="7sbhje"
name
cards.cardId
cards.quantity
```

Función sugerida:

```ts id="9zh6mr"
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

No enviar:

```txt id="2cdseu"
cardName
imageUrlSmall
supertype
types
subtypes
```

---

# Validación

El frontend debe validar solamente reglas mínimas de UX:

```txt id="o6n66j"
nombre requerido
nombre máximo 100 caracteres
mazo no vacío
cantidades mayores a 0
no permitir cantidades negativas
```

Las reglas reales del TCG deben quedar del lado del backend.

La UI debe mostrar la respuesta del backend:

```txt id="mnf6jd"
valid
cardCount
validationErrors
```

No duplicar reglas completas del TCG en frontend.

---

# Manejo de errores

La feature debe manejar errores mínimos:

```txt id="wiytuo"
Error cargando cartas
Error guardando mazo
Error validando mazo
Mazo vacío
Nombre inválido
Sesión inválida o expirada
Sin resultados de búsqueda
```

La UI no debe mostrar errores crudos de Java, objetos JSON completos ni stack traces.

Los errores deben transformarse a mensajes simples y entendibles.

---

# Integración con auth

Auth es responsabilidad de otra parte del proyecto.

Deck Builder debe asumir:

```txt id="732a5a"
El usuario ya está autenticado.
El JWT es gestionado por otra capa.
El interceptor global agrega el token cuando corresponde.
```

Deck Builder no debe guardar ni leer manualmente tokens salvo que el proyecto ya tenga una utilidad central para eso y sea estrictamente necesario.

Si el backend responde `401` o `403`, la UI puede mostrar:

```txt id="xg53mo"
Tu sesión no es válida o expiró. Volvé a iniciar sesión.
```

No implementar login desde Deck Builder.

---

# Ruta

La ruta principal esperada es:

```txt id="xj3skk"
/decks/new
```

Ejemplo conceptual:

```ts id="qf1bxi"
{
  path: 'decks/new',
  loadComponent: () =>
    import('./features/deck-builder/pages/deck-builder-page/deck-builder-page')
      .then(m => m.DeckBuilderPage)
}
```

No implementar todavía rutas de listado, detalle o edición salvo que ya existan o sean necesarias por estructura.

---

# Integración con lobby

El lobby será responsabilidad de otra parte del equipo.

Deck Builder solo debe exponer o mantener la ruta:

```txt id="kvxr2m"
/decks/new
```

Más adelante, el lobby podrá navegar hacia esa ruta.

No implementar cards de lobby, menú principal ni navegación completa desde esta feature.

---

# Reglas de Angular

La implementación debe respetar:

* Standalone components.
* No usar NgModules.
* Usar `inject()` cuando sea posible.
* Usar Reactive Forms.
* Usar signals para estado local.
* Usar `computed` para datos derivados.
* Usar `ChangeDetectionStrategy.OnPush`.
* Usar control flow moderno `@if`, `@for`, `@switch`.
* Evitar suscripciones sin limpiar.
* Preferir `takeUntilDestroyed()` si se usan subscriptions manuales.
* Mantener templates simples.

---

# Qué no debe hacerse

No se debe:

* Modificar backend.
* Modificar engine.
* Modificar controllers o DTOs Java.
* Modificar services Java.
* Modificar base de datos.
* Guardar token JWT dentro de Deck Builder.
* Crear un sistema propio de auth.
* Cambiar el provider principal a mocks salvo indicación explícita.
* Hardcodear arrays de cartas dentro de componentes.
* Implementar lógica real de reglas del TCG en frontend.
* Reescribir la feature de auth.
* Reescribir pokedex.
* Implementar tablero de juego.
* Implementar WebSocket.
* Crear dependencias innecesarias con lobby.
* Introducir librerías visuales nuevas sin justificación.

---

# Criterios de arquitectura aceptada

La arquitectura se considera correcta si:

* La feature está aislada en `features/deck-builder`.
* Los modelos respetan el contrato definido.
* Los mocks están centralizados y quedan como fallback.
* Los componentes consumen datos desde servicios, no desde arrays locales.
* La page orquesta estado y acciones.
* Los componentes hijos son mayormente presentacionales.
* La ruta `/decks/new` funciona.
* La carga de cartas usa `CardApi`.
* El guardado usa `DeckApi`.
* El provider principal usa servicios reales.
* Los servicios reales consumen `/api/cards` y `/api/decks`.
* Los componentes no dependen de si la implementación es real o mock.
* La validación se muestra desde la respuesta del backend.
* No se toca backend.
* No se toca engine.
* No se toca auth.
* No se toca lobby.
* No se toca pokedex.
* No se toca game board.
