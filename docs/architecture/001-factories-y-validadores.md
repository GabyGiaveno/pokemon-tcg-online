# Reporte de Implementación: Creacionales y Validación (Sesión 1)

## Resumen Ejecutivo
Se estableció la arquitectura base para la validación de mazos y la inicialización del estado del motor de juego, aislando la lógica de negocio (Java Puro) de la infraestructura (Spring).

## Novedades y Fundamentos Técnicos

### 1. Validación de Mazos - Patrón Strategy (`services/deck/validators`)
- **Qué hicimos**: En lugar de un `DeckService` con múltiples sentencias `if`, creamos una interfaz `DeckValidator` con implementaciones independientes (`ExactSizeValidator`, `BasicPokemonValidator`, `CopyLimitValidator`, `AceTacticianValidator`).
- **Por qué**: Cumplimos con el **Open/Closed Principle (SOLID)**. Si en el futuro se añade una nueva regla (por ejemplo, prohibir ciertas cartas en un torneo), simplemente creamos un nuevo validador sin tocar ni arriesgar el código existente.
- **Funcionamiento**: El `DeckService` inyecta automáticamente una lista con todos los validadores. Al validar un mazo, recorre la lista y acumula los errores en un DTO limpio (`DeckValidationError`).

### 2. Resolución de Tipos - `CardTypeResolver` (`engine/factories`)
- **Qué hicimos**: Una clase de lógica pura que deduce el `CardType` de dominio (ej. `MEGA_POKEMON`, `ITEM`) leyendo los metadatos de texto crudo de la carta (supertipo, subtipo, nombre).
- **Por qué**: La base de datos no guarda un Enum estricto de `CardType`. El Engine necesita saber exactamente qué es cada carta para aplicar las reglas, pero no debe depender de la BD. Este resolver sirve como un "traductor" seguro.

### 3. Instanciación - `PokemonFactory` (Patrón Singleton) (`engine/factories`)
- **Qué hicimos**: Una fábrica dedicada a transformar la entidad JPA `Card` en modelos inmutables de juego como `ActivePokemon` y `BenchPokemon`.
- **Por qué**: Usamos el patrón Singleton para asegurar que haya una única instancia de la fábrica, lo que permite **inyectarla por constructor** en otros componentes del Engine. Esto es crucial para poder "mockearla" (simularla) en los tests unitarios sin depender de Spring Framework.

### 4. Inicialización - `PlayerFieldBuilder` (Patrón Builder) (`engine/factories`)
- **Qué hicimos**: Un constructor paso a paso para el lado del tablero del jugador. 
- **Funcionamiento**: Recibe las 60 cartas, usa un `SecureRandom` para un Fisher-Yates shuffle (barajado criptográficamente seguro nativo de `Collections.shuffle`), reparte 7 cartas a la mano, 6 a las Prize Cards (o 1 en Muerte Súbita), y deja el resto en el mazo.
- **Por qué**: El armado inicial de la partida es un proceso complejo. El Builder encapsula esta complejidad garantizando que el `PlayerField` nazca en un estado válido y consistente, evitando la pérdida o duplicación de cartas en memoria.

### 5. Corrección de Infraestructura - Lombok + Java 21 (`pom.xml`)
- **Qué hicimos**: Actualizamos la versión de Lombok a `1.18.32`.
- **Por qué**: Java 21 modificó su Árbol de Sintaxis Abstracta (AST). Versiones anteriores de Lombok fallaban al intentar inyectar métodos en tiempo de compilación (lanzando `TypeTag :: UNKNOWN`).

## Organización del Sistema de Carpetas
Este reporte se archiva bajo el modelo de **Architectural Decision Records (ADR)** en `docs/architecture/`. Esta práctica mantiene un historial limpio de *por qué* se tomaron decisiones de diseño, permitiendo a cualquier desarrollador nuevo entender las bases del sistema sin bucear en el historial de Git.
