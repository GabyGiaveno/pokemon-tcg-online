# Deck Builder UI/UX Guidelines

## Objetivo

Definir la dirección visual y de experiencia de usuario para la feature **Deck Builder** del frontend Angular.

La feature debe respetar la estética general del proyecto:

```txt id="wz8h9j"
retro / pixel-art / Game Boy / Pokémon-inspired
```

La pantalla debe ser clara, funcional y fácil de usar, sin parecer un formulario administrativo ni un dashboard genérico.

Esta guía no busca un diseño pixel-perfect final. El objetivo es que el Deck Builder mantenga una identidad visual coherente mientras se mejora su integración real con backend.

---

## Estado actual de la pantalla

El Deck Builder ya se encuentra conectado al backend real para las operaciones principales:

```txt id="2y6mc9"
GET /api/cards
POST /api/decks
```

Por lo tanto, la UI debe contemplar datos reales, paginación, estados de carga, errores de red o sesión, validaciones devueltas por backend y feedback claro al usuario.

Los mocks pueden seguir existiendo como fallback de desarrollo, pero no son el flujo principal de la pantalla.

---

## Referencias visuales

Se toman como referencia dos estilos visuales del proyecto.

---

### Referencia 1 — Battle Board

Características observadas:

* Arena de combate dividida en zonas.
* Interfaz tipo HUD.
* Fondo oscuro con textura.
* Bordes brillantes.
* Sensación competitiva y futurista.
* Zonas delimitadas para cartas.
* Elementos centrales destacados.

Uso recomendado:

Esta referencia es más apropiada para el futuro **Game Board** o tablero de partida.

Para Deck Builder puede tomarse únicamente como inspiración secundaria para:

* Separación visual de zonas.
* Bordes resaltados.
* Paneles oscuros.
* Acentos de color.
* Sensación de interfaz de juego.

---

### Referencia 2 — Trainer Club / Lobby

Características observadas:

* Estética pixel-art.
* Fondo ilustrado tipo club o centro de entrenadores.
* UI superpuesta sobre fondo.
* Paneles laterales oscuros semitransparentes.
* Botones grandes y claros.
* Tipografía con sensación retro.
* Colores cálidos y azules oscuros.
* Diseño de videojuego, no de dashboard empresarial.
* Jerarquía clara: título central, paneles secundarios y acciones principales.

Uso recomendado:

Esta referencia debe ser la principal para Deck Builder.

El Deck Builder debe sentirse como una pantalla dentro de un juego de cartas, no como un formulario administrativo.

---

# Dirección visual principal

La pantalla de creación de mazo debe combinar:

```txt id="1z3pvx"
Pixel-art / retro UI
+
Paneles oscuros semitransparentes
+
Botones grandes estilo videojuego
+
Cartas visuales tipo colección
+
Layout funcional de editor de mazos
+
Feedback claro de integración real
```

No debe verse como:

```txt id="81fxbw"
Bootstrap genérico
Angular Material genérico
Dashboard empresarial
Formulario plano sin identidad visual
Tabla administrativa común
```

---

# Tono visual esperado

La UI debe transmitir:

* Juego.
* Colección.
* Estrategia.
* Cartas.
* Retro/pixel-art.
* Interfaz clara y usable.
* Ambiente de club de entrenadores o mesa de construcción de mazos.

No debe priorizar efectos visuales complejos por encima de la funcionalidad.

---

# Layout recomendado

La implementación actual puede usar un layout de **tres zonas principales** en desktop:

```txt id="mnr4z3"
┌──────────────────────────────────────────────────────────────────────────────┐
│ Crear Mazo                                                                  │
│ Nombre: [______________________________]                                     │
├───────────────────────┬────────────────────────────────┬─────────────────────┤
│ FILTROS / BÚSQUEDA    │ CATÁLOGO / MAZO                 │ RESUMEN / ACCIONES  │
│                       │                                │                     │
│ Buscar carta          │ Catálogo de cartas              │ Contador 0/60       │
│ Supertype             │ Resultados paginados            │ Cartas únicas       │
│ Type                  │ Página X de Y                   │ Validación          │
│ Set                   │                                │ Guardar mazo        │
│                       │ Mazo actual                     │ Limpiar mazo        │
└───────────────────────┴────────────────────────────────┴─────────────────────┘
```

También puede aceptarse un layout de dos columnas si el proyecto ya lo tiene implementado y funciona bien:

```txt id="71bmw3"
Catálogo de cartas | Mazo actual + resumen + acciones
```

Pero para la etapa actual se prioriza la organización en tres zonas porque mejora la lectura de:

* búsqueda/filtros
* catálogo
* mazo actual
* resumen
* validación
* acciones principales

---

## Zona izquierda — Filtros y búsqueda

Debe contener:

* Buscador de cartas.
* Filtros simples soportados por el backend.
* Estado de búsqueda activa.
* Botón o acción de limpiar filtros si corresponde.

Filtros prioritarios:

```txt id="qp42k9"
name
supertype
type
set
```

No inventar filtros que el backend no soporte.

---

## Zona central — Catálogo y mazo actual

Debe contener:

* Lista o grid de cartas disponibles.
* Botón para agregar carta.
* Estado de carga.
* Estado sin resultados.
* Estado de error.
* Controles de paginación.
* Lista de cartas agregadas al mazo.

La zona central puede dividirse internamente:

```txt id="sm6v5t"
CATÁLOGO
MAZO ACTUAL
```

El catálogo debe tener suficiente espacio visual para que las cartas se vean como colección y no como tabla.

---

## Zona derecha — Resumen, validación y acciones

Debe contener:

* Nombre del mazo o resumen del nombre ingresado.
* Total de cartas.
* Cantidad de cartas únicas.
* Contador visible `0/60`.
* Estado general del mazo.
* Panel de validación.
* Botón principal `Guardar mazo`.
* Botón secundario `Limpiar mazo`.
* Mensajes de éxito o error.

No dejar placeholders visibles como:

```txt id="kf2m1a"
Acción futura
```

Si una acción no está implementada, no debe mostrarse.

---

# Jerarquía visual

La jerarquía debe ser clara:

1. Título de pantalla: `Crear Mazo`.
2. Input de nombre del mazo.
3. Contador del mazo: `0/60`, `24/60`, `60/60`.
4. Catálogo de cartas.
5. Mazo actual.
6. Resumen y validación.
7. Acción principal: `Guardar mazo`.
8. Acciones secundarias: `Limpiar mazo`, `Reintentar`.

El botón de guardar debe ser visualmente más importante que los botones secundarios.

---

# Estilo de paneles

Usar paneles inspirados en UI de videojuego:

* Fondos oscuros.
* Transparencia leve si hay fondo ilustrado.
* Bordes marcados.
* Esquinas levemente redondeadas o estilo pixel.
* Sombra suave.
* Separadores visibles.
* Títulos de panel en mayúsculas o con estilo retro.

Ejemplos de títulos:

```txt id="4ealnx"
FILTROS
CATÁLOGO
MAZO ACTUAL
RESUMEN
VALIDACIÓN
ACCIONES
```

---

# Estilo de botones

Los botones deben sentirse como controles de videojuego.

---

## Botón principal

Uso:

```txt id="4irbuu"
Guardar mazo
```

Características:

* Tamaño grande.
* Alto contraste.
* Color destacado.
* Texto fuerte.
* Estado disabled claro.
* Hover visible.
* Debe comunicar que es la acción principal de la pantalla.

Inspiración: botón `JUGAR` del lobby.

---

## Botones secundarios

Uso:

```txt id="ynsv19"
Agregar
Quitar
+
-
Limpiar mazo
Reintentar
Anterior
Siguiente
```

Características:

* Más pequeños que el botón principal.
* Consistentes entre sí.
* Claros y legibles.
* No deben competir visualmente con `Guardar mazo`.

---

# Estilo de cartas del catálogo

Las cartas del catálogo deben mostrarse como elementos visuales, no como filas de tabla.

Cada carta debe mostrar como mínimo:

* Imagen chica.
* Nombre.
* Supertype.
* Types si existen.
* Botón `Agregar`.

Formato recomendado:

```txt id="z76nwa"
┌────────────────────┐
│ [Imagen carta]     │
│ Pikachu            │
│ Pokémon · Lightning│
│ [Agregar]          │
└────────────────────┘
```

Si la imagen no carga, mostrar un placeholder simple.

No bloquear la UI por imágenes faltantes.

---

# Estilo del mazo actual

Las cartas agregadas al mazo pueden mostrarse en formato lista compacta.

Cada ítem debe mostrar:

* Imagen mini o placeholder.
* Nombre de carta.
* Tipo/supertype.
* Cantidad.
* Botón `+`.
* Botón `-`.
* Botón quitar.

Ejemplo:

```txt id="x55vbj"
[Pikachu img] Pikachu
Pokémon · Lightning
[-] x2 [+] [Quitar]
```

Reglas visuales:

* No permitir cantidades negativas.
* Si la cantidad llega a 0, quitar la carta.
* Mantener visible el total del mazo.
* Limpiar o actualizar validación si el mazo cambia después de guardar.

---

# Contador del mazo

El contador debe ser uno de los elementos más visibles de la pantalla.

Formato recomendado:

```txt id="1x09i3"
0 / 60
24 / 60
60 / 60
65 / 60
```

Estados sugeridos:

```txt id="6d3paj"
Faltan cartas
Cantidad exacta
Exceso de cartas
```

El frontend puede mostrar ayuda visual, pero no debe duplicar toda la lógica real de validación del TCG.

La validación final debe venir del backend.

---

# Paginación del catálogo

Como el backend devuelve `total`, `page` y `size`, la UI debe mostrar paginación simple.

Elementos mínimos:

```txt id="7vg8wo"
Anterior
Siguiente
Página X de Y
Total: N cartas
```

Reglas:

* Deshabilitar `Anterior` en la primera página.
* Deshabilitar `Siguiente` si no hay más páginas.
* Mantener búsqueda y filtros al cambiar página.
* Mostrar loading mientras se carga una nueva página.
* Evitar que la paginación se vea como un control administrativo genérico.

---

# Búsqueda y filtros

La búsqueda debe sentirse fluida.

Recomendaciones:

* Usar debounce para no disparar una request por cada tecla.
* Resetear a página 0 cuando cambia la búsqueda.
* Mostrar estado de carga si corresponde.
* Mostrar empty state si no hay resultados.
* Mantener los filtros visibles y fáciles de entender.

Filtros recomendados:

```txt id="6xoqik"
Supertype
Type
Set
```

No agregar filtros que el backend no soporte.

---

# Colores

La paleta debe respetar la estética existente del proyecto.

Se recomienda usar:

* Azul oscuro para paneles principales.
* Amarillo/dorado para acciones importantes.
* Rojo para errores o estados inválidos.
* Verde o azul claro para éxito/validación correcta.
* Blanco o gris claro para texto principal.
* Gris azulado para texto secundario.

No introducir una paleta completamente nueva si el proyecto ya tiene colores definidos.

Si existen variables CSS globales, reutilizarlas.

---

# Fondos

Para esta etapa no es obligatorio crear assets definitivos.

Opciones válidas:

1. Usar fondo existente del proyecto si ya hay uno adecuado.
2. Usar un fondo simple con gradiente oscuro.
3. Usar textura sutil generada con CSS.
4. Dejar preparado el contenedor para reemplazar por imagen pixel-art propia más adelante.

No descargar assets externos sin autorización.

No depender de imágenes de referencia como assets productivos.

---

# Tipografía

Si el proyecto ya tiene una fuente pixel/retro definida, usarla.

Si no existe, no agregar una dependencia nueva solo para esto.

En ese caso:

* Usar la fuente global actual.
* Aplicar pesos, mayúsculas y espaciado para acercarse a una estética retro.
* Dejar preparado para una futura fuente pixel-art.

Recomendaciones:

* Títulos de panel en mayúsculas.
* Botones con texto fuerte.
* Evitar textos demasiado pequeños.
* Mantener buena legibilidad.

---

# Responsive

La pantalla debe funcionar de forma básica en desktop y mobile.

---

## Desktop

Layout recomendado:

```txt id="zzuz00"
Filtros / búsqueda
Catálogo + mazo actual
Resumen + validación + acciones
```

También se acepta:

```txt id="qeueos"
Catálogo
Mazo actual + resumen
```

si el diseño existente está más cerca de dos columnas y no rompe la experiencia.

---

## Mobile / pantallas chicas

Las secciones deben apilarse verticalmente:

```txt id="wy7sut"
Crear Mazo
Nombre
Resumen rápido / contador
Filtros
Catálogo
Paginación
Mazo actual
Validación
Acciones
```

No hace falta perfección visual mobile en esta etapa, pero la pantalla no debe romperse.

---

# Estados visuales requeridos

La UI debe contemplar estados reales de integración.

---

## Loading

Ejemplos:

```txt id="66o67u"
Cargando cartas...
Guardando mazo...
Cargando página...
```

Debe verse integrado al estilo de la pantalla.

---

## Empty state

Ejemplos:

```txt id="zh1cga"
No se encontraron cartas.
Tu mazo está vacío.
Agregá cartas desde el catálogo.
No hay resultados para esta búsqueda.
```

---

## Error

Ejemplos:

```txt id="wtrcs1"
No se pudieron cargar las cartas.
No se pudo guardar el mazo.
Tu sesión no es válida o expiró. Volvé a iniciar sesión.
```

Los errores deben ser visibles y claros.

No mostrar:

```txt id="cfs8lw"
stack traces
objetos JSON completos
errores crudos de Java
```

---

## Success

Ejemplo:

```txt id="ml4e69"
Mazo guardado correctamente.
```

Si el mazo se guarda pero queda inválido, el mensaje debe ser claro:

```txt id="679yvv"
Mazo guardado, pero todavía tiene errores de validación.
```

---

## Validación

Mostrar claramente:

* Mazo válido.
* Mazo inválido.
* Lista de errores devueltos por backend.
* Cantidad total de cartas.
* Mensaje informativo si todavía no se guardó/validó.

Ejemplo:

```txt id="fxyj4y"
Mazo inválido
- El mazo debe tener exactamente 60 cartas.
- Hay demasiadas copias de una carta.
```

No duplicar reglas completas del TCG en frontend.

---

# Acciones principales

La pantalla debe tener acciones claras:

```txt id="21azpk"
Guardar mazo
Limpiar mazo
Reintentar carga
```

No mostrar acciones futuras si no hacen nada.

Acciones no prioritarias para esta etapa:

```txt id="ffpt31"
Editar mazo existente
Borrar mazo
Exportar mazo
Importar mazo
Duplicar mazo
```

Estas pueden quedar para fases futuras.

---

# Nivel de pulido esperado en esta etapa

La implementación debe tener:

* Layout claro.
* Componentes visualmente integrados.
* Estética retro base.
* Botones y paneles coherentes.
* Responsive básico.
* Estados de carga/error/éxito.
* Paginación clara.
* Contador del mazo visible.
* Código CSS ordenado.

No es obligatorio todavía:

* Animaciones complejas.
* Pixel-perfect.
* Assets finales.
* Texturas definitivas.
* Sonidos.
* Efectos avanzados.
* Ilustraciones propias.
* Transiciones elaboradas.

---

# Reglas importantes

No usar Angular Material.

No usar Bootstrap.

No usar Tailwind si el proyecto no lo usa actualmente.

No agregar librerías visuales nuevas.

No descargar assets externos.

No usar imágenes de referencia como assets finales.

No romper estilos globales existentes.

No rediseñar auth, lobby, pokedex o game.

No modificar layout global salvo que sea estrictamente necesario.

No tocar backend.

No tocar engine.

---

# Integración con documentos técnicos

Este documento complementa:

```txt id="bwk2m9"
DECK_BUILDER_SCOPE.md
DECK_BUILDER_API_CONTRACT.md
DECK_BUILDER_ARCHITECTURE.md
DECK_BUILDER_TASKS.md
OPENCODE_DECK_BUILDER_PROMPT.md
```

La prioridad sigue siendo que la feature funcione correctamente.

La estética debe acompañar la funcionalidad desde el inicio, pero sin bloquear la implementación.

---

# Criterios de aceptación visual

La UI se considera aceptable para esta etapa si:

* No parece una pantalla genérica de formulario.
* Se integra con la estética retro/pixel-art del proyecto.
* Usa paneles oscuros o estilo HUD.
* Tiene botones claros estilo videojuego.
* El catálogo de cartas se ve como colección.
* El mazo actual se entiende rápidamente.
* El contador `0/60` es visible.
* El botón de guardar destaca.
* La acción limpiar mazo existe o se eliminan placeholders innecesarios.
* Los estados de error, loading, empty y éxito son visibles.
* La paginación es clara y usable.
* La pantalla es usable en desktop.
* La pantalla no se rompe en mobile.
* Los estilos están encapsulados en la feature.
* No introduce dependencias visuales nuevas.
* No toca backend.
