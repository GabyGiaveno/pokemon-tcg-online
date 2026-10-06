# 004 - Trainer Effects Flyweight & Polymorphism

## Contexto
El motor del juego necesitaba procesar dinámicamente las cartas de Entrenador (Items, Supporters, Stadiums). Inicialmente, el `MainPhaseState` permitía jugar estas cartas (moviéndolas de la mano a la pila de descartes) pero no ejecutaba ningún comportamiento, delegándolo a futuros `TODOs`.
Con el éxito de la implementación del patrón Flyweight para los efectos de los ataques (ADR 003), se decidió extender la misma arquitectura para las Cartas de Entrenador.

## Decisión Arquitectónica
Replicar el patrón **Flyweight + Polimorfismo de Jackson** para la ejecución de Cartas de Entrenador.

1. **DTOs Polimórficos (`TrainerEffect`)**:
   - Se creó una clase abstracta base `TrainerEffect` con las anotaciones de Jackson `@JsonTypeInfo` y `@JsonSubTypes`.
   - Se definieron subclases DTO puras (sin comportamiento ni dependencias del motor) como `DrawCardsTrainerEffect` y `HealTrainerEffect`.
   - Se agregó el campo transitorio `@Transient List<TrainerEffect> parsedTrainerEffects;` en la entidad `Card` para almacenar estos efectos parseados sin afectar el esquema actual de la base de datos (que es responsabilidad del equipo de API).

2. **Patrón Flyweight (`TrainerEffectRegistry` y `TrainerEffectLogic`)**:
   - Se creó la interfaz genérica `TrainerEffectLogic<T extends TrainerEffect>` que define el contrato `execute(T effectData, BoardState board, Long playerId)`.
   - Se implementaron lógicas sin estado como `DrawCardsTrainerLogic` y `HealTrainerLogic`.
   - El `TrainerEffectRegistry` actúa como una fábrica Singleton que asocia cada clase DTO con su lógica correspondiente.

3. **Integración Desacoplada en `MainPhaseState`**:
   - Los métodos de acción (`handlePlayItem`, `handlePlaySupporter`, `handlePlayStadium`) ahora invocan dinámicamente el `TrainerEffectRegistry` iterando sobre `card.getParsedTrainerEffects()`.
   - Todos los eventos generados por las lógicas se propagan de vuelta al `ActionResult`.

## Consecuencias
- **Positivas**: 
  - Al igual que con los ataques, las lógicas de los entrenadores son 100% testeables unitariamente sin necesidad de inicializar todo el motor, mockeando únicamente un fragmento del `BoardState`.
  - La escalabilidad es absoluta: para agregar un nuevo efecto (ej. "Lanzar una moneda"), solo se crea un DTO y una lógica, sin tocar el `MainPhaseState`.
  - No se generan dependencias transitorias ni se colisiona con el trabajo paralelo del equipo de API/Base de datos.
- **Negativas**: 
  - Requiere que la capa de persistencia (o API) mapee explícitamente el JSON crudo de la carta hacia la lista de `parsedTrainerEffects` antes de enviarla al Engine.
