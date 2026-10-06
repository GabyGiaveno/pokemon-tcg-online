# Diseño del Tablero Pokémon TCG — Estética Hearthstone

## Contexto Visual

Este documento describe el diseño del tablero de juego para un TCG Pokémon custom. El objetivo es **fusionar la estructura oficial del JCC Pokémon con la estética visual de Hearthstone**: materiales ricos (madera, piedra tallada, cuero), iluminación ambiental dramática, marcos ornamentados, y efectos de brillo en las zonas activas.

---

## Referencia 1: Tablero Pokémon TCG Online (base estructural)

### Descripción general

El tablero está dividido en **dos mitades simétricas** (jugador 1 abajo, rival arriba), sobre una mesa de madera oscura con un **Poké Ball gigante** como motivo central decorativo. Cada mitad tiene su propio color de tema:

- **Mitad superior (rival):** rojo/carmesí, con el logo del rival en la esquina izquierda.
- **Mitad inferior (jugador local):** azul marino .

### Zonas visibles en el tablero Pokémon TCG Online

#### Zona del jugador activo (mitad inferior):

|Zona|Posición|Descripción visual|
|---|---|---|
|**Pokémon Activo**|Centro, zona media|Carta grande, destacada, ligeramente elevada sobre el tablero. Es la más prominente.|
|**Banca (Bench)**|Fila inferior de la zona de juego|5 slots horizontales para Pokémon de reserva, cartas más pequeñas.|
|**Cartas de Premio**|Columna lateral izquierda|6 cartas apiladas/en cuadrícula, boca abajo.|
|**Baraja (Deck)**|Esquina inferior derecha|Pila de cartas boca abajo con borde decorativo.|
|**Pila de Descartes**|Junto a la baraja|Carta superior visible, boca arriba.|
|**Mano**|Franja inferior de la pantalla|Cartas del jugador en forma de abanico, visibles y seleccionables.|
|**Estadio**|Centro del tablero, entre ambas mitades|Carta horizontal compartida, decorativa y funcional.|

#### Zona del rival (mitad superior, espejada):

Misma estructura, rotada 180°. Las cartas de mano del rival se ven como reversos.

### Estética del tablero Pokémon TCG Online

- Fondo: textura de madera oscura para el "borde" del tablero.
- Zonas de juego: superficie de fieltro o tela texturizada, color distintivo por jugador.
- Separador central: línea visual con el logo de la Poké Ball.
- Slots vacíos: contornos con línea punteada, ligeramente visibles.
- Indicadores de turno: número visible en pantalla.
- Logotipos de expansión como decoración de las esquinas.

---

## Referencia 2: Tablero Hearthstone (guía estética)

### Descripción general

El tablero de Hearthstone es una **taberna de fantasía viva e interactiva**. Los elementos clave que deben inspirar el diseño:

### Elementos estéticos clave de Hearthstone

#### Marco y estructura del tablero

- **Madera envejecida tallada** como material principal del marco exterior. Bordes gruesos con relieves y ornamentos.
- **Piedra rúnica / metálica** para el área central donde se colocan los minions (zona de batalla).
- El tablero tiene **profundidad 3D**: las zonas se perciben como superficies físicas elevadas o hundidas.
- **Iluminación ambiental dinámica**: luz cálida lateral (como velas o antorchas) que proyecta sombras suaves.

#### Zonas del tablero Hearthstone

|Zona|Descripción visual|
|---|---|
|**Zona de batalla (centro)**|Superficie de piedra o arena con textura rugosa. Los minions se colocan aquí como figuras tridimensionales dentro de marcos ovalados/redondeados.|
|**Héroe (abajo/arriba)**|Retrato circular grande del personaje, con marco ornamentado dorado o de piedra. Muestra HP con número grande y un cristal de armadura encima.|
|**Mano del jugador**|Cartas en abanico en la parte inferior, con fondo oscuro semitransparente. Las cartas brillan levemente al hacer hover.|
|**Maná**|Cristales azules brillantes en la esquina inferior izquierda. Los usados se oscurecen.|
|**Deck**|Pila de cartas en la esquina inferior derecha, con número visible encima.|
|**Zona del enemigo (arriba)**|Misma estructura, espejada. Las cartas enemigas muestran dorso hasta que se juegan.|

#### Detalles decorativos de Hearthstone

- **Partes animadas del escenario**: elementos del fondo (animales, objetos) que se mueven en bucle.
- **Efectos de luz en cartas activas**: glow dorado o verde cuando una carta puede atacar (Divine Shield = borde plateado brillante).
- **Tipografía**: fuente medieval/fantasy con sombra para los números de ataque (rojo) y defensa (amarillo/dorado).
- **Marcos de cartas**: muy ornamentados, con diferentes colores según rareza (marrón, azul, morado, dorado).
- **Humo, polvo o partículas** cuando una carta es destruida.

---

## Tablero Final: Fusión Pokémon TCG + Hearthstone

### Concepto visual unificado

> Un tablero de batalla que parece una **mesa de juego antigua tallada en madera oscura**, con el área de juego central como una **superficie de piedra iluminada por cristales de energía**. El centro del tablero tiene una **Poké Ball grabada en relieve**. Cada zona tiene marcos ornamentados al estilo Hearthstone.

---

### Layout del tablero (estructura Pokémon, estética Hearthstone)

```
┌─────────────────────────────────────────────────────────────────┐
│  MARCO DE MADERA TALLADA — borde exterior grueso, ornamentos    │
│                                                                  │
│  [Premios x6]  [Baraja]  [Descarte]          [Mano Rival ↓↓]   │ ← Rival (rotado 180°)
│                                                                  │
│         [ Banca Rival: 5 slots ovalados ornamentados ]           │
│                                                                  │
│              ┌──────────────────────────────┐                    │
│              │   POKÉMON ACTIVO RIVAL        │  ← marco dorado   │
│              │   (grande, centrado, glow)    │     con relieve   │
│              └──────────────────────────────┘                    │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─[  ⊙ POKÉ BALL  ]─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─   │
│              ┌──────────────────────────────┐                    │
│              │   POKÉMON ACTIVO JUGADOR      │  ← glow activo    │
│              │   (grande, centrado, glow)    │     amarillo/oro  │
│              └──────────────────────────────┘                    │
│                                                                  │
│         [ Banca Jugador: 5 slots ovalados ornamentados ]         │
│                                                                  │
│  [Premios x6]  [Baraja]  [Descarte]          [Mano Jugador ↑↑] │ ← Jugador local
│                                                                  │
│  MARCO DE MADERA TALLADA — borde exterior grueso, ornamentos    │
└─────────────────────────────────────────────────────────────────┘
```

---

### Especificaciones por zona

#### 1. Marco exterior del tablero

- **Material visual**: madera oscura (wengué o roble oscuro) con talla de relieve.
- **Ornamentos**: en las 4 esquinas, medallones decorativos con los tipos de energía Pokémon (fuego, agua, hierba, rayo).
- **Grosor del borde**: aproximadamente 48–64px en pantalla.
- **Textura CSS**: usar `radial-gradient` y `box-shadow` internas para simular profundidad.
- Inspiración directa: el marco dorado/madera de la taberna de Hearthstone.

#### 2. Superficie de juego central

- **Color base**: verde oscuro profundo (`#1a472a` a `#0d2b18`) con ligera textura de fieltro.
- **Separación entre mitades**: la mitad superior tiene un tinte rojo oscuro sutil (`rgba(180,30,30,0.15)`), la inferior un tinte azul oscuro (`rgba(20,60,140,0.15)`). Esto hereda el color por equipo del tablero PTCGO.
- **Centro del tablero**: Poké Ball grabada en relieve, como SVG o pseudo-elemento CSS, sin obstruir el juego.

#### 3. Pokémon Activo

- **Tamaño**: carta más grande del tablero (100–120px de ancho, ratio 2.5:3.5).
- **Marco**: ornamentado al estilo Hearthstone. Marco dorado para el jugador activo, plateado para el rival.
- **Glow de turno activo**: `box-shadow: 0 0 20px 6px rgba(255,220,0,0.6)` pulsante (2s infinite).
- **Contadores de daño**: fichas rojas circulares superpuestas en la esquina inferior derecha de la carta.
- **Condiciones especiales**: badge en la parte inferior de la carta (ZZZ morado, PAR amarillo, BRN naranja, PSN verde, CFZ rosa).
- **Slot vacío**: contorno punteado dorado con icono de Poké Ball tenue al centro.

#### 4. Banca (Bench)

- **5 slots** por jugador, distribuidos horizontalmente bajo/sobre el Pokémon Activo.
- **Tamaño de carta**: 72px de ancho.
- **Marcos de slot**: óvalos o rectángulos redondeados con borde ornamentado fino (estilo Hearthstone minion zone).
- **Slots vacíos**: borde punteado con icono pequeño de Poké Ball.
- **Hover**: la carta escala a 1.1x y aparece un tooltip con nombre y HP.

#### 5. Cartas de Premio (Prize Cards)

- **6 cartas** por jugador, dispuestas en una cuadrícula 2×3 o columna de 6.
- **Posición**: lateral izquierdo de cada mitad.
- **Visual**: boca abajo, con dorso decorativo. Marco simple, sin ornamentos exagerados.
- **Indicador**: número visible sobre el grupo (cuántas quedan).
- Cuando se toma una carta de premio: animación de la carta que vuela hacia la mano del jugador.

#### 6. Baraja (Deck)

- **Posición**: esquina derecha de cada mitad del tablero.
- **Visual**: pila de cartas con efecto de profundidad (sombra que sugiere grosor).
- **Marco**: caja de madera tallada, al estilo de la pila de cartas de Hearthstone.
- **Número de cartas**: badge circular sobre la pila.

#### 7. Pila de Descartes

- **Posición**: junto a la Baraja.
- **Visual**: la carta superior visible, boca arriba. Las anteriores asoman por detrás.
- **Número de cartas**: badge pequeño.

#### 8. Mano del jugador

- **Posición**: franja inferior de la pantalla (jugador local) / superior (rival, boca abajo).
- **Presentación**: cartas en abanico ligeramente superpuestas.
- **Interacción**: al hacer hover, la carta seleccionada se eleva y escala.
- **Fondo**: panel semitransparente oscuro (`rgba(0,0,0,0.5)`) con borde dorado fino, al estilo de la zona de mano de Hearthstone.
- **Mano del rival**: solo se ven los dorsos, contados.

#### 9. Estadio (Stadium Card)

- **Posición**: centro exacto del tablero, entre ambas mitades (sobre la Poké Ball decorativa o a un lado).
- **Tamaño**: carta horizontal o del mismo tamaño que el activo.
- **Marco**: especial, distinto a las demás zonas — puede tener un borde brillante de color neutro/plateado.

#### 10. Indicadores de información del jugador

Inspirado en los retratos de héroe de Hearthstone:

- **Retrato del jugador**: ícono circular en la esquina (inferior izquierda para P1, superior izquierda para P2).
- **Nombre del jugador**: texto sobre el retrato.
- **Puntos de premio restantes**: badge numérico visible junto al retrato.

---

### Paleta de colores

|Elemento|Color|
|---|---|
|Marco de madera|`#3b1f0a` a `#6b3a1f` con gradiente|
|Ornamentos dorados|`#c9a84c`, `#f0d060`|
|Superficie de juego|`#1a472a` a `#0d2b18`|
|Tinte zona rival|`rgba(180,30,30,0.15)`|
|Tinte zona jugador|`rgba(20,60,140,0.15)`|
|Glow turno activo|`rgba(255,220,0,0.6)`|
|Borde slots vacíos|`rgba(255,255,255,0.2)` punteado|
|Panel mano jugador|`rgba(0,0,0,0.5)`|
|Texto principal|`#f5e6c8` (crema cálido)|
|Contadores de daño|`#e84040`|

---

### Tipografía

- **Fuente principal**: serif medieval o fantasy (ej. `Cinzel`, `MedievalSharp`, o fallback `Georgia`). Usar para nombres de zonas y títulos.
- **Números de HP/daño**: sans-serif bold con sombra oscura para legibilidad (ej. `bold 18px #fff` con `text-shadow: 0 0 4px #000`).
- **Log de acciones**: monospace pequeño, panel semitransparente en esquina inferior derecha.

---

### Animaciones requeridas

|Acción|Animación|
|---|---|
|Robar carta|Carta desliza desde la baraja hacia la mano (400ms, ease-out)|
|Jugar carta|Carta se eleva de la mano y vuela a la zona destino (500ms, spring)|
|Ataque|Pokémon activo se lanza hacia adelante, impacto con shake en el defensor (600ms)|
|Daño recibido|Flash blanco + número de daño flota y desvanece (800ms)|
|KO / Fuera de combate|Carta oscurece, rota levemente, se desvanece; flash de pantalla (1000ms)|
|Adjuntar energía|Token de energía (color del tipo) vuela desde la mano al Pokémon (400ms)|
|Tomar carta de premio|Carta de premio vuela hacia la mano del jugador (500ms)|
|Hover sobre carta|Scale 1→1.08, z-index elevado, drop-shadow suave (150ms CSS)|

---

### Efectos de partículas (canvas overlay)

- Canvas `pointer-events: none` sobre todo el tablero, `z-index: 50`.
- **Ataques tipo fuego**: 30–40 círculos naranjas/rojos desde atacante a defensor.
- **Ataques tipo rayo**: líneas en zigzag amarillas desde atacante.
- **Impacto general**: 8–12 chispas en estrella desde el punto de impacto, color según tipo de energía.
- **KO**: partículas grises/oscuras que caen y se disipan.
- Máximo 50 partículas activas simultáneamente.

---

### Reglas estructurales (del JCC Pokémon oficial)

Estas reglas deben reflejarse en el diseño visual de las zonas:

- **Mano**: cada jugador roba 7 cartas al inicio. Las cartas de la mano son ocultas al rival.
- **Cartas de Premio**: 6 cartas separadas de la baraja al inicio, colocadas boca abajo. Al dejar KO un Pokémon rival, se toma 1 carta de premio. Tomar la última = ganar la partida.
- **Pokémon Activo**: exactamente 1 por jugador en todo momento. Si no hay Pokémon Activo y tampoco en Banca = derrota.
- **Banca**: hasta 5 Pokémon de reserva. Los Pokémon no activos deben estar en Banca.
- **Baraja**: 60 cartas al inicio. Si no se puede robar al inicio del turno = derrota.
- **Pila de Descartes**: las cartas eliminadas van aquí. Nadie puede cambiar su orden.
- **Estadio**: carta compartida entre ambos jugadores. Solo puede haber 1 en juego; jugar otro reemplaza el anterior.
- **Zona de juego compartida**: los jugadores comparten el espacio central, cada uno con su sección dividida en dos filas (Activo + Banca).

---

### Notas de implementación para Claude Code

1. **Una sola mitad = un componente reutilizable** (`PlayerZone`), instanciado dos veces con `transform: rotate(180deg)` para el rival.
2. **No construir motor de reglas completo** a menos que se pida explícitamente — el tablero es un asistente visual.
3. **IDs de zona** (usar exactamente estos en el DOM/estado):
    - `p1-active`, `p2-active`
    - `p1-bench-0` a `p1-bench-4`, `p2-bench-0` a `p2-bench-4`
    - `p1-hand`, `p2-hand`
    - `p1-deck`, `p2-deck`
    - `p1-discard`, `p2-discard`
    - `p1-prizes`, `p2-prizes`
    - `stadium`
4. **Relación de aspecto de cartas**: siempre `2.5:3.5` en todas las zonas.
5. **Sin librerías de animación externas** — solo CSS keyframes + vanilla JS.
6. **Sin `localStorage`** salvo que se pida.
7. **Output por defecto**: HTML + CSS + JS en un único archivo, sin build step.

--- 

## Animacion esperada en las cartas

1. Las cartas de la mano, del bench y el pokemon activo, tiene que poder inspeccionarse y usar los ataques desde la misma carta, aumentando el zoom de las habilidades y permitiendo su seleccion desde la propia carta, tambien haciendo la animacion que tiene hoy la inspeccion de las cartas en la pokedex ademas del brillo particular de las cartas