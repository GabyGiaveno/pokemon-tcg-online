# Starter Decks — Bug de composición

## Problema

Los 6 mazos starter (`DefaultDeckProvisioningService.java`) tienen una cantidad absurda de energías básicas (44-48 copias en un mazo de 60 cartas), lo que hace que **el 73-80% del mazo sea energía**. Al robar, el jugador saca energía casi siempre y no puede jugar.

Además, **ningún mazo tiene cartas de Trainer**.

## Mazos actuales

| Mazo | Pokémon | Energía | Trainers | % Energía |
|------|---------|---------|----------|-----------|
| Fuego | 12 | **48** | 0 | **80%** |
| Rayo | 12 | **48** | 0 | **80%** |
| Agua | 16 | **44** | 0 | **73%** |
| Planta | 16 | **44** | 0 | **73%** |
| Lucha | 16 | **44** | 0 | **73%** |
| Psíquico | 16 | **44** | 0 | **73%** |

## Causa

En `DefaultDeckProvisioningService.java` las templates definen:

```java
new CreateDeckRequest.CardEntry("xy1-133", 48)  // 48 copias de Fire Energy
```

El segundo parámetro de `CardEntry` es **cantidad de copias** de esa carta en el mazo. 48 copias de energía básica en un mazo de 60 es ridículo.

## Composición recomendada

Un mazo estándar de Pokémon TCG tiene aproximadamente:

| Tipo | Cantidad |
|------|----------|
| Pokémon | 15-20 |
| Energía | **15-20** |
| Trainers | 20-25 |

## Sugerencia de Mazo Fuego (ejemplo)

```java
new DefaultDeckTemplate("starter-fire", "Mazo Fuego Inicial",
    List.of(
        new CreateDeckRequest.CardEntry("xy1-20", 4),   // Slugma × 4
        new CreateDeckRequest.CardEntry("xy1-22", 4),   // Pansear × 4
        new CreateDeckRequest.CardEntry("xy1-24", 4),   // Fennekin × 4
        new CreateDeckRequest.CardEntry("xy1-133", 16)  // Fire Energy × 16 (en vez de 48)
        // + agregar trainers del set xy1 si están disponibles
    )),
```

## Notas

- Las energías básicas se identifican por `supertype = "Energy"`, `subtypes = ["Basic"]`
- IDs de energías en el set xy1: `xy1-132` (Grass), `xy1-133` (Fire), `xy1-134` (Water), `xy1-135` (Lightning), `xy1-136` (Psychic), `xy1-137` (Fighting)
- Para verificar qué trainers hay disponibles, pegar a `GET /api/cards?supertype=Trainer&size=50`
- El archivo a modificar es: `BE/src/main/java/.../services/DefaultDeckProvisioningService.java`
