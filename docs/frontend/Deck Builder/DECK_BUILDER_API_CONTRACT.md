# Deck Builder API Contract

## Objetivo

Definir el contrato de datos que debe usar la feature **Deck Builder** del frontend Angular.

La feature ya cuenta con integración real para las operaciones principales:

```txt id="1b9zt3"
GET /api/cards
POST /api/decks
```

Por lo tanto, este documento define el contrato que deben respetar los servicios reales del frontend y también los mocks conservados como fallback.

El frontend debe consumir modelos TypeScript basados en los DTOs reales del backend, sin modificar backend, engine, controllers, DTOs Java, services Java ni base de datos.

---

## Principio principal

La UI del Deck Builder debe consumir modelos TypeScript basados en los DTOs reales del backend.

Durante la etapa actual, el flujo principal es:

```txt id="x9g5kz"
Deck Builder → Real Services → Backend
```

Los mocks quedan como alternativa de desarrollo:

```txt id="6zhp4o"
Deck Builder → Mock Services → Mock Data
```

Los componentes no deben cambiar si se usa la implementación real o la mock. Esa decisión debe quedar encapsulada en la capa de `data-access` y en los providers.

---

## Archivos backend usados como referencia

El contrato debe respetar los DTOs actuales del backend:

```txt id="l51tqv"
BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/request/CreateDeckRequest.java
BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/CardPageResponse.java
BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/CardResponse.java
BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/DeckResponse.java
BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/DeckCardResponse.java
BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/DeckValidationResponse.java
BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/DeckValidationError.java
BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/common/ErrorApi.java
```

Endpoints relevantes:

```txt id="nhm2t9"
BE/src/main/java/ar/edu/utn/frc/tup/piii/controllers/CardController.java
BE/src/main/java/ar/edu/utn/frc/tup/piii/controllers/DeckController.java
```

Estos archivos se usan solo como referencia de contrato.

No modificar backend desde la tarea de Deck Builder frontend.

---

# Endpoints que debe respetar la feature

Los servicios reales deben consumir estos endpoints.

Los servicios mock deben simular la misma forma de requests/responses para poder usarse como fallback sin reescribir la UI.

---

## 1. Listar / buscar cartas

### Endpoint real

```http id="z8np92"
GET /api/cards
```

### Query params soportados

```txt id="4625g4"
name
set
supertype
type
page
size
sort
```

### Uso esperado desde Deck Builder

El Deck Builder usa este endpoint para mostrar el catálogo de cartas disponibles, buscar, filtrar y paginar resultados.

Ejemplos:

```http id="qiu7fb"
GET /api/cards?page=0&size=20
```

```http id="nax2ol"
GET /api/cards?name=pikachu&page=0&size=20
```

```http id="8u283j"
GET /api/cards?supertype=Pokémon&type=Lightning&page=0&size=20
```

```http id="y14ov3"
GET /api/cards?set=xy1&page=0&size=20
```

### Response esperada

```ts id="enfami"
export interface CardPageResponse {
  data: CardResponse[];
  total: number;
  page: number;
  size: number;
}
```

```ts id="f7x0ch"
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
```

### Notas frontend

* `id` es el valor que después se manda como `cardId` al crear el mazo.
* `imageUrlSmall` debe usarse para mostrar cartas en listados.
* `imageUrlLarge` puede reservarse para detalle o vista ampliada.
* `hp` puede ser `null`, por ejemplo para Trainer o Energy.
* `types` puede venir vacío.
* `subtypes` puede venir vacío.
* `attacks`, `weaknesses` y `resistances` llegan como `string | null`, no como objetos parseados.
* El frontend no debe depender de que todas las cartas tengan imagen.
* El frontend debe usar `total`, `page` y `size` para paginación real.

---

## 2. Obtener carta por ID

### Endpoint real

```http id="5osh32"
GET /api/cards/{id}
```

### Uso esperado desde Deck Builder

No es obligatorio para la pantalla actual de creación si el listado ya trae toda la información necesaria.

Puede usarse más adelante para una vista de detalle o modal ampliado.

### Response esperada

```ts id="ttjqh1"
CardResponse
```

### Nota

Si el backend devuelve 404 para una carta inexistente, la UI debe transformar ese error a un mensaje simple.

No mostrar errores crudos ni stack traces.

---

## 3. Listar mazos del jugador

### Endpoint real

```http id="5wryqb"
GET /api/decks
```

### Uso esperado

No es necesario para la pantalla actual de creación de mazo nuevo.

Puede usarse más adelante si la feature se expande a listado de mazos.

### Response esperada

```ts id="v8p1ak"
export type DeckListResponse = DeckResponse[];
```

### Nota

Este endpoint depende de usuario autenticado.

Deck Builder no debe implementar auth propia. Debe apoyarse en el interceptor/JWT existente del proyecto.

---

## 4. Obtener mazo por ID

### Endpoint real

```http id="82j23j"
GET /api/decks/{id}
```

### Uso esperado

No es necesario para creación de mazo nuevo.

Puede usarse más adelante para edición:

```txt id="i7m8za"
/decks/:id/edit
```

### Response esperada

```ts id="vfx5hl"
DeckResponse
```

---

## 5. Crear mazo

### Endpoint real

```http id="a35urz"
POST /api/decks
```

### Uso esperado desde Deck Builder

Este es el endpoint principal de la pantalla actual.

La pantalla debe construir un request con:

* nombre del mazo
* lista de cartas
* cantidad de cada carta

### Request esperada

```ts id="vwqfzu"
export interface CreateDeckRequest {
  name: string;
  cards: DeckCardEntry[];
}

export interface DeckCardEntry {
  cardId: string;
  quantity: number;
}
```

### Ejemplo JSON

```json id="958ofu"
{
  "name": "Mazo Eléctrico",
  "cards": [
    {
      "cardId": "xy1-42",
      "quantity": 4
    },
    {
      "cardId": "xy1-25",
      "quantity": 2
    }
  ]
}
```

### Response esperada

```ts id="4us1eu"
export interface DeckResponse {
  id: number;
  name: string;
  valid: boolean;
  cardCount: number;
  cards: DeckCardResponse[];
  validationErrors: DeckValidationError[];
  createdAt: string;
}
```

```ts id="tn0b4h"
export interface DeckCardResponse {
  cardId: string;
  cardName: string;
  quantity: number;
  supertype: string;
  types: string[];
  subtypes: string[];
  imageUrlSmall: string;
}
```

```ts id="jio2et"
export interface DeckValidationError {
  code: string;
  message: string;
  cardId: string | null;
}
```

### Nota sobre `createdAt`

En backend es `LocalDateTime`.

En frontend debe modelarse como:

```ts id="to0f05"
createdAt: string;
```

---

## 6. Actualizar mazo

### Endpoint real

```http id="w2mqmm"
PUT /api/decks/{id}
```

### Uso esperado

No es necesario para la pantalla actual de creación.

La estructura puede quedar preparada para edición futura.

### Request esperada

Usa el mismo formato base que crear mazo:

```ts id="8uq0oh"
export interface UpdateDeckRequest {
  name: string;
  cards: DeckCardEntry[];
}
```

### Response esperada

```ts id="05p16h"
DeckResponse
```

---

## 7. Eliminar mazo

### Endpoint real

```http id="yyuln6"
DELETE /api/decks/{id}
```

### Uso esperado

No entra en el alcance actual de creación de mazo.

Puede usarse más adelante desde listado/detalle de mazos.

### Response esperada

```txt id="fdb18n"
204 No Content
```

---

## 8. Validar mazo

### Endpoint real

```http id="t6me9n"
POST /api/decks/{id}/validate
```

### Uso esperado

En backend real, este endpoint valida un mazo ya existente.

Para la pantalla actual de creación, la validación principal puede mostrarse a partir del `DeckResponse` devuelto por:

```http id="yexobk"
POST /api/decks
```

Más adelante, si se agrega listado o edición de mazos, puede usarse este endpoint para revalidar un mazo guardado.

### Response esperada

```ts id="s42087"
export interface DeckValidationResponse {
  valid: boolean;
  errors: DeckValidationError[];
  cardCount: number;
}
```

```ts id="0c02zm"
export interface DeckValidationError {
  code: string;
  message: string;
  cardId: string | null;
}
```

### Ejemplo de mazo válido

```json id="nrcfqs"
{
  "valid": true,
  "errors": [],
  "cardCount": 60
}
```

### Ejemplo de mazo inválido

```json id="i6mwud"
{
  "valid": false,
  "errors": [
    {
      "code": "DECK_SIZE_INVALID",
      "message": "Deck must contain exactly 60 cards",
      "cardId": null
    },
    {
      "code": "TOO_MANY_COPIES",
      "message": "Only 4 copies of the same card are allowed",
      "cardId": "xy1-42"
    }
  ],
  "cardCount": 38
}
```

---

# Error API

El backend tiene un DTO común de error:

```ts id="k58n0j"
export interface ErrorApi {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
```

### Ejemplo

```json id="6idqr6"
{
  "timestamp": "2026-06-08T21:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Deck not found",
  "path": "/api/decks/99"
}
```

### Uso esperado en frontend

La UI debe poder manejar errores básicos:

* error al cargar cartas
* error al guardar mazo
* error al validar mazo
* error 401/403 por sesión inválida o expirada
* error 404 si se intenta obtener una carta o mazo inexistente

No mostrar al usuario objetos JSON completos, stack traces ni errores crudos del backend.

Transformar errores a mensajes entendibles.

---

# Modelos frontend sugeridos

Los modelos pueden dividirse así:

```txt id="jrjmat"
features/deck-builder/models/
├── card.model.ts
├── deck.model.ts
├── deck-request.model.ts
├── deck-validation.model.ts
├── deck-builder-state.model.ts
└── api-error.model.ts
```

---

## `card.model.ts`

```ts id="0dcatj"
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

## `deck-request.model.ts`

```ts id="ur4tcj"
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

---

## `deck.model.ts`

```ts id="2d71ts"
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

---

## `deck-validation.model.ts`

```ts id="podfp8"
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

---

## `api-error.model.ts`

```ts id="no1d16"
export interface ErrorApi {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
```

---

# Modelo interno para UI

Además de los DTOs reales, el frontend necesita un modelo interno para manejar el mazo en construcción antes de guardarlo.

Este modelo no representa directamente la API. Es solo para estado local de la UI.

Archivo sugerido:

```txt id="v7a3z6"
deck-builder-state.model.ts
```

```ts id="3824jf"
import { CardResponse } from './card.model';

export interface DeckBuilderCard {
  card: CardResponse;
  quantity: number;
}
```

### Uso

Este modelo sirve para que la UI pueda mostrar:

* nombre de la carta
* imagen
* tipo
* subtipo
* cantidad

Antes de guardar, debe transformarse a `CreateDeckRequest`.

---

# Transformación de estado UI a request API

El Deck Builder puede manejar internamente un array de `DeckBuilderCard`.

Ejemplo:

```ts id="dsojtc"
deckCards: DeckBuilderCard[]
```

Antes de guardar, debe transformarlo a `CreateDeckRequest`.

```ts id="vord5u"
function toCreateDeckRequest(name: string, cards: DeckBuilderCard[]): CreateDeckRequest {
  return {
    name,
    cards: cards.map(item => ({
      cardId: item.card.id,
      quantity: item.quantity
    }))
  };
}
```

No enviar al backend datos innecesarios como:

```txt id="ccizx7"
cardName
imageUrlSmall
supertype
types
subtypes
```

Esos datos sirven para la UI, pero el request real solo necesita:

```txt id="dwgzzc"
name
cardId
quantity
```

---

# Validaciones frontend mínimas

El frontend debe validar solamente lo necesario para una buena UX:

```txt id="s4h8ms"
Nombre requerido
Nombre máximo 100 caracteres
Al menos 1 carta
Cantidad por carta mayor o igual a 1
No permitir cantidades negativas
Contador total visible
```

Las reglas completas del TCG deben quedar del lado del backend.

La UI debe mostrar los errores de validación que devuelva el backend en `DeckResponse.validationErrors` o `DeckValidationResponse.errors`.

No duplicar reglas complejas del TCG en frontend.

---

# Contrato de navegación

La ruta esperada para crear mazo es:

```txt id="mk4mu4"
/decks/new
```

Esta ruta abre directamente el Deck Builder.

No implementar todavía:

```txt id="uw5uuf"
/decks
/decks/:id
/decks/:id/edit
```

salvo que sea necesario para una fase futura.

---

# Estados mínimos de UI

La pantalla debe poder representar:

```txt id="qwi3jr"
loadingCards
cardsLoadError
savingDeck
saveSuccess
saveError
validationResult
emptyDeck
emptySearchResults
currentPage
pageSize
totalResults
totalPages
```

Estos estados deben funcionar con servicios reales.

También pueden probarse con mocks si se usa el fallback.

---

# Criterios de aceptación del contrato

La implementación respeta este contrato si:

* Los modelos TypeScript reflejan los DTOs reales del backend.
* Los servicios reales consumen `/api/cards` y `/api/decks`.
* Los mocks devuelven objetos con la misma forma que la API real.
* Los componentes consumen servicios, no arrays hardcodeados.
* El request de creación de mazo usa `{ name, cards: [{ cardId, quantity }] }`.
* El response de creación de mazo usa `DeckResponse`.
* La validación usa `DeckResponse.validationErrors` o `DeckValidationResponse`.
* Los errores usan `ErrorApi` o una adaptación clara.
* La paginación usa `total`, `page` y `size` de `CardPageResponse`.
* La búsqueda/filtros usan query params soportados por el contrato.
* Los componentes no dependen de si la implementación es real o mock.
* No se implementa auth ni JWT dentro de Deck Builder.
* No se toca backend.
* No se toca engine.
