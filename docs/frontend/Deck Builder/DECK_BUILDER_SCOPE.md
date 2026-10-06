# Deck Builder Scope

## Objetivo

Continuar el desarrollo de la feature de **creación de mazos** en el frontend Angular del proyecto Pokémon TCG.

La feature debe permitir que un usuario construya un mazo a partir de un catálogo de cartas, agregando y quitando cartas, modificando cantidades, visualizando el estado del mazo, validaciones y resultado de guardado.

A diferencia de la etapa inicial, el Deck Builder ya se encuentra conectado al backend real para las operaciones principales:

```txt
GET /api/cards
POST /api/decks
```

Por lo tanto, el desarrollo actual debe enfocarse en **estabilizar y mejorar la experiencia frontend** sobre la integración real ya existente.

No se debe modificar backend, engine, controllers, DTOs Java, services Java ni base de datos.

---

## Estado actual de la feature

La feature ya cuenta con:

* Ruta funcional para creación de mazo:

```txt
/decks/new
```

* Estructura frontend dentro de:

```txt
FE/src/app/features/deck-builder/
```

* Modelos TypeScript basados en el contrato real de API.
* Componentes separados para búsqueda, listado de cartas, mazo actual, resumen y validación.
* Servicios abstractos `CardApi` y `DeckApi`.
* Servicios mock conservados como fallback.
* Servicios reales conectados al backend.
* Provider configurado para usar servicios reales.
* Carga de cartas desde `/api/cards`.
* Guardado de mazo mediante `/api/decks`.
* Uso del interceptor/JWT existente del proyecto para autenticar requests cuando corresponda.

---

## Contexto general del flujo de usuario

El flujo general esperado de la aplicación es:

```txt
Iniciar sesión → Lobby → Creación de Mazo
```

Esta feature comienza cuando el usuario ya ingresó a la pantalla de creación de mazo.

La autenticación, el login, el JWT, los guards, el lobby general y la navegación previa son responsabilidad de otras partes del proyecto.

Deck Builder debe asumir que esas capas ya existen o serán gestionadas fuera de esta feature.

---

## Alcance incluido

La feature de Deck Builder debe incluir y continuar mejorando:

* Pantalla principal de creación de mazo.
* Campo para ingresar el nombre del mazo.
* Catálogo/listado de cartas disponibles usando backend real.
* Búsqueda básica de cartas.
* Filtros simples soportados por el contrato actual.
* Paginación del catálogo de cartas.
* Agregar cartas al mazo.
* Quitar cartas del mazo.
* Aumentar y disminuir cantidad de una carta dentro del mazo.
* Mostrar cantidad total de cartas del mazo.
* Mostrar cantidad de cartas únicas.
* Mostrar resumen del mazo actual.
* Mostrar panel de validación visual del mazo.
* Guardar mazo usando el backend real.
* Mostrar errores de validación devueltos por el backend.
* Mostrar estados de carga cuando corresponda.
* Mostrar mensajes de error o éxito.
* Mejorar la experiencia visual respetando la estética retro / pixel-art / Game Boy.
* Mantener mocks como fallback para desarrollo aislado, pero no como flujo principal.
* Mantener la separación entre UI, modelos, servicios y estado local.

---

## Alcance excluido

Esta feature NO debe implementar, modificar ni corregir:

* Login.
* Register.
* Forgot password.
* Reset password.
* JWT.
* Interceptor de autenticación.
* Auth guards.
* Manejo global de sesión.
* Navbar o layout general de la aplicación.
* Lobby completo.
* Pokedex.
* Game board.
* Acciones de juego.
* WebSocket.
* Engine.
* Backend.
* Base de datos.
* Controllers, services, DTOs o tests del backend.

Cualquier cambio fuera de `features/deck-builder` debe ser mínimo, justificado y limitado a integración frontend necesaria, por ejemplo ajustes en rutas o imports.

No se debe tocar `BE/`.

---

## Decisión técnica principal

La feature debe continuar usando:

```txt
Contrato real de API + servicios reales ya integrados
```

Esto significa que los modelos TypeScript, requests y responses deben respetar la forma esperada por el backend real.

La UI debe depender de contratos abstractos de data-access:

```txt
DeckBuilderPage
   ↓
CardApi / DeckApi
   ↓
RealCardApiService / RealDeckApiService
   ↓
Backend real
```

Los mocks deben conservarse únicamente como alternativa de desarrollo local o fallback:

```txt
DeckBuilderPage
   ↓
CardApi / DeckApi
   ↓
MockCardApiService / MockDeckApiService
   ↓
Mock data
```

Los componentes no deben saber si los datos vienen del backend real o de mocks. Esa decisión debe quedar encapsulada en providers/servicios.

---

## Uso de mocks

Los mocks ya no son el flujo principal de la feature.

Deben conservarse para:

* Desarrollo visual aislado.
* Pruebas rápidas sin backend levantado.
* Simular errores de UI.
* Comparar respuestas esperadas contra el contrato real.

No se debe volver a cambiar el provider principal a mocks salvo que se indique explícitamente.

No se deben hardcodear arrays de cartas dentro de componentes.

---

## Ruta esperada

La pantalla debe poder accederse desde:

```txt
/decks/new
```

Esta ruta representa la creación de un mazo nuevo.

No es necesario implementar desde Deck Builder la navegación completa desde lobby hacia esta pantalla.

---

## Responsabilidad de esta feature

La feature debe encargarse de:

1. Mostrar cartas disponibles desde el backend real.
2. Permitir buscar cartas.
3. Permitir filtrar cartas si el contrato lo soporta.
4. Permitir navegar páginas del catálogo.
5. Permitir construir un mazo en memoria antes de guardar.
6. Permitir revisar el contenido actual del mazo.
7. Permitir guardar el mazo usando `POST /api/decks`.
8. Permitir visualizar validaciones devueltas por el backend.
9. Mostrar correctamente estados de carga, error y éxito.
10. Mantener una UI clara y coherente con la estética del proyecto.

La feature no debe encargarse de determinar manualmente quién es el usuario logueado. Debe apoyarse en la capa de autenticación/interceptor ya existente.

---

## Reglas de implementación Angular

El proyecto utiliza Angular moderno. La implementación debe respetar las siguientes reglas:

* Usar standalone components.
* No crear NgModules.
* Usar `inject()` en lugar de constructor injection cuando sea posible.
* Usar signals para estado local de la pantalla.
* Usar `computed` para estado derivado.
* Usar Reactive Forms para formularios.
* Usar `ChangeDetectionStrategy.OnPush`.
* Usar control flow moderno de Angular: `@if`, `@for`, `@switch` cuando corresponda.
* Mantener componentes chicos y con responsabilidad clara.
* Evitar lógica pesada dentro del template.
* Separar modelos, mocks, servicios, componentes y utils.
* Evitar suscripciones manuales sin limpieza.
* Usar `takeUntilDestroyed()` si se necesitan subscriptions manuales.

---

## Estructura esperada

La feature debe mantenerse dentro de:

```txt
FE/src/app/features/deck-builder/
```

Estructura esperada:

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
│   ├── deck.model.ts
│   ├── deck-request.model.ts
│   ├── deck-validation.model.ts
│   └── deck-builder-state.model.ts
│
├── mocks/
│   ├── mock-cards.ts
│   ├── mock-decks.ts
│   └── mock-validation.ts
│
└── utils/
    └── deck-builder-mappers.ts
```

La estructura puede adaptarse al estilo ya existente del proyecto, pero debe mantener la separación conceptual entre:

* `pages`: pantallas principales.
* `components`: componentes de UI reutilizables dentro de la feature.
* `data-access`: servicios para obtener o enviar datos.
* `models`: interfaces TypeScript.
* `mocks`: datos falsos con forma de API real.
* `utils`: funciones puras de transformación.

---

## Componentes mínimos esperados

### DeckBuilderPage

Pantalla principal de creación de mazo.

Responsabilidades:

* Coordinar el estado general de la feature.
* Manejar el formulario del nombre del mazo.
* Obtener cartas desde `CardApi`.
* Mantener el mazo actual en memoria.
* Agregar/quitar cartas.
* Aumentar/disminuir cantidades.
* Guardar mazo usando `DeckApi`.
* Mostrar mensajes de éxito/error.
* Mostrar validaciones.
* Conectar los componentes hijos.

---

### CardSearchPanel

Responsabilidades:

* Mostrar input de búsqueda.
* Emitir cambios de búsqueda hacia la pantalla principal.
* Mostrar filtros simples si están implementados.
* No obtener datos directamente desde servicios.

---

### CardResultList

Responsabilidades:

* Mostrar las cartas disponibles.
* Mostrar información básica de cada carta.
* Permitir agregar una carta al mazo.
* Mostrar loading, empty state y error.
* Emitir eventos hacia la pantalla padre.

No debe modificar directamente el estado global del mazo.

---

### DeckCurrentList

Responsabilidades:

* Mostrar las cartas actualmente agregadas al mazo.
* Permitir aumentar cantidad.
* Permitir disminuir cantidad.
* Permitir quitar una carta.
* Mostrar cantidad por carta.

---

### DeckSummary

Responsabilidades:

* Mostrar nombre del mazo.
* Mostrar cantidad total de cartas.
* Mostrar cantidad de cartas únicas.
* Mostrar estado general del mazo.
* Mostrar acceso claro a la acción principal de guardado si corresponde.

---

### DeckValidationPanel

Responsabilidades:

* Mostrar si el mazo es válido o no.
* Mostrar errores de validación devueltos por el backend.
* Mostrar advertencias visuales si faltan cartas o si el mazo todavía no fue guardado/validado.
* No implementar reglas complejas del TCG en frontend.

---

## Validaciones frontend mínimas

La pantalla debe validar como mínimo:

* El nombre del mazo es obligatorio.
* El nombre del mazo no debe superar el máximo definido por el contrato.
* El mazo debe tener al menos una carta para poder guardar.
* La cantidad de cada carta debe ser mayor a 0.
* No debe haber cantidades negativas.
* Debe mostrarse un contador total de cartas.

Las reglas completas del TCG deben quedar del lado del backend.

El frontend puede mostrar ayudas visuales, pero no debe duplicar la lógica completa de validación real.

---

## Criterio de éxito para la etapa actual

La feature se considera lista para avanzar si:

* `/decks/new` abre correctamente la pantalla.
* La pantalla carga cartas desde `/api/cards`.
* Se pueden buscar cartas.
* Se pueden agregar cartas al mazo.
* Se pueden quitar cartas del mazo.
* Se pueden modificar cantidades.
* Se muestra total de cartas.
* Se muestra cantidad de cartas únicas.
* Se puede guardar usando `/api/decks`.
* Se muestran errores reales de validación si el backend los devuelve.
* Se muestran estados de loading/error/success.
* Los componentes no dependen directamente de datos hardcodeados.
* Los mocks quedan disponibles pero no son el flujo principal.
* No se toca auth, lobby, pokedex, game, engine ni backend.

---

## Restricciones importantes

No modificar archivos del backend.

No modificar archivos del engine.

No implementar autenticación.

No implementar JWT.

No implementar lógica de lobby.

No implementar lógica de juego.

No implementar WebSocket.

No reemplazar ni romper la feature de auth existente.

No reemplazar ni romper la feature de pokedex existente.

No introducir una arquitectura paralela incompatible con el proyecto Angular actual.

No volver la feature a mocks salvo indicación explícita.

---

## Próxima etapa recomendada

Antes de seguir con features grandes, la próxima etapa del Deck Builder debe enfocarse en estabilizar la integración real:

```txt
Fase 4 — Estabilizar integración real
```

Tareas recomendadas:

* Agregar paginación real del catálogo.
* Agregar debounce en búsqueda.
* Mostrar total de resultados.
* Mejorar mensajes de errores reales.
* Mejorar estados de carga y guardado.
* Mejorar visualmente contador `0/60`.
* Agregar botón para limpiar mazo.
* Reemplazar placeholders de acciones futuras.
* Mantener la estética retro / pixel-art / Game Boy.
