# 005 - GameEngineFacade como delegador puro y seam CardLookup → CardCacheService

## Contexto
El `GameEngineFacade` es el patrón Facade obligatorio (PROJECT_CONSTITUTION §3.2): expone el motor puro al resto de la app. Su única responsabilidad debe ser **wiring + delegación**, sin lógica de negocio ni conocimiento de la persistencia.

Al revisarlo se detectaron dos desviaciones del diseño original:

1. **Leak de persistencia hacia el Facade.** El Facade inyectaba directamente `CardRepository` (un bean JPA) y construía el `CardLookup` con `cardId -> cardRepository.findById(cardId).orElse(null)`. Esto rompía el principio de que `CardLookup` es la *única* frontera entre el engine y Spring (ADR 002 §4): el Facade quedaba acoplado a la capa de datos.

2. **Contrato de `CardLookup` violado y documentación desincronizada.** El Javadoc de `CardLookup` promete `cardCacheService::findById` como implementación y declara `@throws IllegalArgumentException if the card is not found`. Pero (a) `CardCacheService` no tenía ningún método `findById` que devolviera la entidad `Card` —solo `getCardById` que devuelve un DTO `CardResponse`—, y (b) la lambda real usaba `.orElse(null)`, devolviendo `null` en lugar de lanzar la excepción. Cualquier handler del engine que asumiera no-null podía explotar con un `NullPointerException`.

## Decisión Arquitectónica
Restaurar el seam tal como lo describe el diseño: el engine solo conoce la abstracción `CardLookup`, y el único componente con conexión a Spring/persistencia es `CardCacheService`.

1. **`CardCacheService.findById(String): Card`**:
   - Se agregó el método que el seam necesita. Devuelve la **entidad** `Card` (no el DTO), porque el engine necesita los datos completos de la carta.
   - Lanza `IllegalArgumentException` si la carta no está en la caché local, respetando el contrato declarado en `CardLookup`.
   - Convive con `getCardById` (que sigue devolviendo `CardResponse` para la capa web). Son consumidores distintos: web → DTO, engine → entidad.

2. **`GameEngineFacade` 100% delegación**:
   - Pasa a inyectar `CardCacheService` en lugar de `CardRepository`. El Facade ya **no conoce** la capa de datos.
   - El `CardLookup` se construye **una sola vez** en el constructor (`cardCacheService::findById`) y se reutiliza, en lugar de reconstruirse en cada llamada vía el viejo helper `buildCardLookup()`.
   - Se eliminó el método privado `buildCardLookup()`.

## Consecuencias
- **Positivas**:
  - El Facade vuelve a ser un delegador puro: recibe la request, busca el board en su registry en memoria, invoca al `TurnManager` y devuelve. Sin lógica de negocio.
  - El seam `CardLookup` queda íntegro: el motor sigue siendo Java puro, testeable con una lambda (`id -> testCards.get(id)`) sin levantar `ApplicationContext`.
  - El contrato de `CardLookup` ahora es verdadero de punta a punta: el ejemplo del Javadoc compila y la semántica de "no encontrado → excepción" se cumple.
  - Verificado: 33 tests unitarios en verde tras el cambio.
- **Negativas / Pendientes**:
  - El Facade sigue siendo dueño del registry en memoria (`Map<Long, BoardState> activeGames`) y arma el `BoardState` inline en `startGame()`. Eso es persistencia + inicialización, no fachada. Queda como deuda a extraer (ver doc de estado, punto crítico de registry/estado dual).
  - **El Facade no está integrado con la capa web**: ningún controller/service lo invoca todavía. Ver `docs/architecture/006-estado-actual-y-puntos-criticos.md`.
