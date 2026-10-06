# Deck Builder Tasks

## Objetivo

Continuar el desarrollo de la feature **Deck Builder** en Angular, partiendo del estado actual del proyecto.

La feature ya se encuentra conectada al backend real para las operaciones principales:

```txt
GET /api/cards
POST /api/decks
```

Por lo tanto, las próximas tareas deben enfocarse en estabilizar la integración real, mejorar la experiencia de usuario y avanzar con el pulido visual del Deck Builder.

No se debe tocar backend, engine, controllers, DTOs Java, services Java ni base de datos.

---

## Regla principal

La feature debe usar:

```txt
Contrato real de API + servicios reales ya integrados
```

El flujo principal actual debe mantenerse así:

```txt
DeckBuilderPage
   ↓
CardApi / DeckApi
   ↓
RealCardApiService / RealDeckApiService
   ↓
Backend real
```

Los mocks quedan disponibles como fallback o herramienta de desarrollo aislado, pero no deben ser el flujo principal salvo indicación explícita.

---

## Restricción principal de trabajo

El trabajo actual corresponde únicamente al frontend.

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

Si durante el desarrollo aparece un error que parece venir del backend, se debe reportar claramente, pero no corregirlo desde esta tarea.

---

## Documentos base

Antes de implementar, leer:

```txt
docs/frontend/deck-builder/DECK_BUILDER_SCOPE.md
docs/frontend/deck-builder/DECK_BUILDER_API_CONTRACT.md
docs/frontend/deck-builder/DECK_BUILDER_ARCHITECTURE.md
docs/frontend/deck-builder/DECK_BUILDER_UI_UX_GUIDELINES.md
```

Si existe `AGENTS.md`, `README.md` o algún documento de reglas del proyecto, respetarlo también.

---

# Estado actual esperado

La feature ya debería contar con:

* Ruta funcional:

```txt
/decks/new
```

* Estructura principal en:

```txt
FE/src/app/features/deck-builder/
```

* Modelos TypeScript basados en contrato real.
* Servicios abstractos `CardApi` y `DeckApi`.
* Servicios mock conservados como fallback.
* Servicios reales `RealCardApiService` y `RealDeckApiService`.
* Provider configurado para usar servicios reales.
* Componentes presentacionales.
* Pantalla principal `DeckBuilderPage`.
* Carga de cartas desde `/api/cards`.
* Guardado de mazos desde `/api/decks`.
* Interacción con auth/JWT mediante el interceptor global existente.

---

# Fases anteriores — Estado

Las fases iniciales se consideran completadas o absorbidas por el estado actual:

```txt
Fase 1 — Revisar estructura actual
Fase 2 — Crear modelos TypeScript
Fase 3 — Crear mocks
Fase 4 anterior — Crear utilidades de mapeo
Fase 5 anterior — Crear capa data-access
Fase 6 anterior — Crear componentes presentacionales
Fase 7 anterior — Crear pantalla principal
Fase 8 anterior — Crear ruta
Fase 9 anterior — Estilos base
```

No rehacer estas fases desde cero.

No eliminar mocks existentes.

No volver el provider principal a mocks salvo indicación explícita.

---

# Fase 4 — Estabilizar integración real

## Objetivo

Mejorar el funcionamiento real del Deck Builder conectado al backend, sin tocar backend.

Esta fase debe hacer que la pantalla sea más cómoda, estable y clara para usar con datos reales.

---

## Tarea 4.1 — Verificar provider actual

Revisar:

```txt
FE/src/app/features/deck-builder/data-access/deck-builder-api.provider.ts
```

Debe mantenerse el uso de servicios reales:

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

No cambiar a mocks.

Los mocks deben quedar disponibles, pero no activos en el flujo principal.

---

## Tarea 4.2 — Agregar estado de paginación

En `DeckBuilderPage`, agregar o consolidar estado para:

```txt
currentPage
pageSize
totalCards
totalPages
```

El frontend debe aprovechar la respuesta real de `/api/cards`:

```ts
export interface CardPageResponse {
  data: CardResponse[];
  total: number;
  page: number;
  size: number;
}
```

El catálogo no debe quedar fijo para siempre en:

```txt
page = 0
size = 20
```

---

## Tarea 4.3 — Agregar controles de paginación en UI

Agregar controles simples para navegar el catálogo:

```txt
Anterior
Siguiente
Página X de Y
Total: N cartas
```

Reglas esperadas:

* Deshabilitar `Anterior` en la primera página.
* Deshabilitar `Siguiente` si no hay más páginas.
* Al cambiar página, volver a llamar `CardApi.getCards(...)`.
* Mantener búsqueda y filtros activos al cambiar de página.
* Mostrar loading mientras se carga la nueva página.

No implementar paginación infinita en esta etapa.

---

## Tarea 4.4 — Agregar debounce en búsqueda

Actualmente la búsqueda puede disparar requests en cada tecla.

Agregar debounce para evitar llamadas excesivas al backend.

Comportamiento esperado:

* Esperar aproximadamente `300ms` desde la última tecla.
* Resetear `page` a `0` cuando cambia la búsqueda.
* No hacer requests duplicadas si el texto no cambió.
* Mantener loading y error funcionando correctamente.

Se puede implementar con Reactive Forms, RxJS o la estrategia más consistente con el proyecto.

---

## Tarea 4.5 — Revisar filtros soportados

El contrato de cartas soporta:

```txt
name
set
supertype
type
page
size
sort
```

Revisar qué filtros ya existen en UI y cuáles conviene exponer.

Prioridad sugerida:

```txt
1. name
2. supertype
3. type
4. set
```

Si se agrega `set`, puede empezar simple con un valor inicial conocido o un input/select básico.

No inventar filtros que el backend no soporte.

---

## Tarea 4.6 — Mejorar estados visuales de carga y error

La pantalla debe mostrar claramente:

```txt
Cargando cartas...
No se pudieron cargar las cartas.
No se encontraron cartas.
Guardando mazo...
No se pudo guardar el mazo.
Mazo guardado correctamente.
```

No mostrar errores crudos del backend, stack traces ni objetos JSON completos en pantalla.

Transformar errores a mensajes entendibles para el usuario.

---

## Tarea 4.7 — Mejorar manejo de validación real

Al guardar mazo mediante `/api/decks`, el backend puede devolver:

```txt
valid
cardCount
validationErrors
```

La UI debe mostrar:

* Si el mazo quedó válido o inválido.
* Cantidad total de cartas.
* Errores de validación devueltos por backend.
* Mensaje claro cuando todavía no se guardó/validó.

No duplicar en frontend toda la lógica real del TCG.

El frontend puede validar solo reglas mínimas de UX:

```txt
nombre requerido
nombre máximo 100 caracteres
mazo no vacío
cantidades mayores a 0
no permitir cantidades negativas
```

---

## Tarea 4.8 — Revisar caso 401 / sesión inválida

Si el backend responde 401 o 403 al guardar, la pantalla debe mostrar un mensaje entendible.

Ejemplo:

```txt
Tu sesión no es válida o expiró. Volvé a iniciar sesión.
```

No implementar login.

No implementar JWT.

No modificar interceptor.

No modificar guards.

Solo manejar el error desde la UI si llega a esta pantalla.

---

## Tarea 4.9 — Agregar acción “Limpiar mazo”

Agregar un botón secundario para vaciar el mazo actual.

Comportamiento esperado:

* Elimina todas las cartas del mazo en memoria.
* Resetea validación mostrada.
* Limpia mensaje de éxito/error relacionado con guardado.
* No llama al backend.
* No borra el nombre del mazo salvo que se decida explícitamente.

---

## Tarea 4.10 — Reemplazar placeholders de acciones futuras

Si existen botones o textos como:

```txt
Acción futura
```

reemplazarlos por acciones reales o quitarlos.

Acciones recomendadas para esta fase:

```txt
Guardar mazo
Limpiar mazo
Reintentar carga
```

No agregar edición, borrado o listado de mazos todavía.

---

# Fase 5 — Pulido visual del Deck Builder

## Objetivo

Mejorar la apariencia de la pantalla respetando la estética retro / pixel-art / Game Boy del proyecto.

No introducir librerías visuales nuevas.

---

## Tarea 5.1 — Consolidar layout desktop

La implementación actual puede usar layout de tres zonas:

```txt
Filtros / búsqueda
Catálogo y mazo actual
Resumen / validación / acciones
```

El layout debe sentirse como una pantalla de juego, no como un dashboard administrativo.

---

## Tarea 5.2 — Mejorar contador del mazo

El contador debe ser visible y fácil de entender:

```txt
0 / 60
24 / 60
60 / 60
```

Estados sugeridos:

```txt
faltan cartas
cantidad exacta
exceso de cartas
```

No hace falta bloquear guardado por no tener 60 cartas, porque el backend valida las reglas reales.

---

## Tarea 5.3 — Mejorar listado de cartas del catálogo

Las cartas deben verse como elementos visuales de colección, no como tabla administrativa.

Cada carta debe mostrar:

* Imagen chica.
* Nombre.
* Supertype.
* Types si existen.
* Botón para agregar.

Debe contemplar imagen rota o faltante.

---

## Tarea 5.4 — Mejorar listado del mazo actual

Cada carta agregada debe mostrar:

* Imagen mini o placeholder.
* Nombre.
* Tipo/supertype.
* Cantidad.
* Botón `+`.
* Botón `-`.
* Botón para quitar.

No permitir cantidades negativas.

---

## Tarea 5.5 — Mejorar jerarquía de acciones

El botón principal debe ser:

```txt
Guardar mazo
```

Debe tener más peso visual que acciones secundarias.

Acciones secundarias:

```txt
Limpiar mazo
Reintentar
```

No deben competir visualmente con guardar.

---

## Tarea 5.6 — Responsive básico

En desktop, mantener columnas.

En pantallas chicas, apilar secciones:

```txt
Nombre del mazo
Filtros
Catálogo
Mazo actual
Resumen
Validación
Acciones
```

No hace falta perfección mobile en esta etapa, pero la pantalla no debe romperse.

---

# Fase 6 — Funciones futuras de mazos

Esta fase no es prioridad inmediata.

No implementarla hasta cerrar Fase 4 y Fase 5.

Funciones posibles:

```txt
GET /api/decks
GET /api/decks/{id}
PUT /api/decks/{id}
DELETE /api/decks/{id}
POST /api/decks/{id}/validate
```

Pantallas futuras posibles:

```txt
/decks
/decks/:id
/decks/:id/edit
```

Tareas futuras:

* Listar mis mazos.
* Abrir mazo existente.
* Editar mazo.
* Borrar mazo.
* Revalidar mazo guardado.
* Duplicar mazo.
* Exportar/importar lista.

No implementar en la fase actual salvo indicación explícita.

---

# Fase 7 — Integración futura con lobby / partida

Esta fase queda fuera del trabajo actual.

Más adelante, cuando corresponda:

* El lobby podrá navegar hacia `/decks/new`.
* El lobby podrá listar mazos válidos.
* La creación de partida podrá usar un `deckId`.
* Unirse a partida podrá requerir un `deckId`.

No implementar esta integración desde Deck Builder en esta etapa.

---

# Verificación

Ejecutar desde `FE`:

```bash
npm install
npm run build
```

Si el proyecto usa lockfile y corresponde:

```bash
npm ci
npm run build
```

También ejecutar tests si existen:

```bash
npm test
```

Si aparecen errores preexistentes no relacionados con Deck Builder, reportarlos por separado.

No ejecutar ni modificar backend como parte de esta tarea.

---

# Entregable final de opencode

Al terminar una fase, entregar un resumen con:

```txt
Archivos modificados
Archivos creados, si aplica
Qué se implementó
Cómo probarlo en /decks/new
Qué quedó conectado al backend real
Qué quedó mockeado como fallback
Limitaciones conocidas
Resultado de build/tests
Confirmación de que no se tocó BE/
```

---

# Restricciones estrictas

No modificar:

```txt
BE/
engine
auth
pokedex
game
lobby
WebSocket
backend DTOs
backend controllers
backend services
backend tests
base de datos
```

No implementar autenticación propia.

No implementar JWT.

No modificar interceptor.

No modificar guards.

No cambiar el provider principal a mocks.

No hardcodear cartas dentro de componentes.

No introducir Bootstrap, Angular Material, Tailwind u otra librería visual nueva salvo que el proyecto ya la use.

No duplicar reglas completas del TCG en frontend.

No mostrar stack traces al usuario.

---

# Criterios de aceptación de Fase 4

La Fase 4 se considera completa si:

* `/decks/new` sigue abriendo correctamente.
* Las cartas se cargan desde `/api/cards`.
* El guardado usa `/api/decks`.
* El provider principal sigue usando servicios reales.
* Hay paginación funcional del catálogo.
* Se muestra total de resultados.
* Se puede navegar entre páginas.
* La búsqueda usa debounce.
* Cambiar búsqueda vuelve a página 0.
* Los filtros disponibles no rompen la carga.
* Se muestran estados claros de loading/error/empty.
* Se muestran mensajes claros al guardar.
* Se muestran validaciones devueltas por backend.
* Se puede limpiar el mazo actual.
* No quedan placeholders visibles como `Acción futura`.
* No se toca backend.
* No se toca engine.
* No se toca auth.
* El build frontend pasa o se reportan errores claramente.

---

# Criterios de aceptación de Fase 5

La Fase 5 se considera completa si:

* La pantalla mantiene una estética retro / pixel-art / Game Boy.
* El layout es claro en desktop.
* La pantalla no se rompe en mobile.
* El catálogo se ve como colección de cartas.
* El mazo actual es fácil de leer.
* El contador `0/60` es visible.
* El botón `Guardar mazo` tiene jerarquía principal.
* Los estados de error, éxito, loading y vacío son claros.
* No se introducen librerías visuales nuevas.
* No se toca backend.
