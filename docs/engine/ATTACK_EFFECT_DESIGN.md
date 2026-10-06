# ATTACK_EFFECT_DESIGN — De texto de carta a implementación real

> Diseño del sistema `AttackEffectRegistry` para el set XY1.  
> Responde: qué es el enum, quién traduce el texto y quién implementa el efecto.

---

## El problema

La API `pokemontcg.io` devuelve los efectos de los ataques como texto libre en inglés:

```json
{
  "name": "Flamethrower",
  "damage": "120",
  "text": "Discard an Energy attached to this Pokémon."
}
```

```json
{
  "name": "Sleepy Song",
  "damage": "50",
  "text": "Your opponent's Active Pokémon is now Asleep."
}
```

```json
{
  "name": "Mind Jack",
  "damage": "10+",
  "text": "Does 20 more damage for each of your opponent's Benched Pokémon."
}
```

El engine no puede operar sobre strings. Necesita efectos tipados que se ejecuten en el `AttackContext`.

---

## Por qué NO usar parsing de texto

El `PostDamageHandler` actual hace keyword matching sobre el texto. Funciona para condiciones simples, pero es frágil:

| Problema | Ejemplo real |
|----------|-------------|
| Fraseos inconsistentes entre sets | "is now Asleep" vs "is now asleep" vs "becomes Asleep" |
| Efectos numéricos en el texto | "20 more damage for each Benched" — ¿cómo parsear "each Benched"? |
| Efectos compuestos | "Flip a coin; if heads, discard 2 Energy; if tails, deal 30 damage" |
| Condiciones condicionales | "If this Pokémon has any damage counters on it, this attack does 60 more damage" |

Para un set **abierto** (muchos sets, efectos desconocidos) el parsing tiene sentido como fallback. Para **XY1** — que es un conjunto finito y conocido — es un error: vas a tener bugs silenciosos donde el texto no matchea y el efecto simplemente no se aplica.

**La solución correcta para XY1: registro manual, uno a uno.**

---

## La arquitectura: tres piezas

```
AttackData.text (String)
        │
        ▼
 AttackEffectKey (enum)          ← LA TRADUCCIÓN: texto → tipo de efecto
        │
        ▼
 AttackEffectRegistry            ← LA IMPLEMENTACIÓN: tipo → lógica Java
        │
        ▼
 AttackContext (mutado)          ← EL RESULTADO: el estado del tablero cambia
```

---

## Pieza 1 — `AttackEffectKey` (el enum)

Cada valor del enum representa **un tipo de efecto atómico** que puede tener un ataque. No es el ataque en sí — es el efecto.

```java
public enum AttackEffectKey {

    // Condiciones especiales al defensor
    APPLY_SLEEP,
    APPLY_PARALYSIS,
    APPLY_CONFUSION,
    APPLY_BURN,
    APPLY_POISON,

    // Daño variable
    DAMAGE_PLUS_PER_BENCH,        // +20 por cada pokémon en banca del rival
    DAMAGE_PLUS_PER_ENERGY,       // +10 por cada energía adjunta
    DAMAGE_PLUS_IF_DAMAGED,       // +60 si el atacante tiene daño
    COIN_FLIP_DAMAGE_BONUS,       // flip: cara = +30 daño, cruz = nada

    // Efectos sobre energías
    DISCARD_ATTACKER_ENERGY_1,    // descartar 1 energía del atacante
    DISCARD_ATTACKER_ENERGY_2,    // descartar 2 energías del atacante

    // Curación
    HEAL_ATTACKER_30,
    HEAL_ATTACKER_60,

    // Daño a banca
    SNIPE_BENCH_10,               // 10 daño a 1 pokémon de banca del rival
    SNIPE_BENCH_30,

    // Efectos de auto-daño
    RECOIL_10,                    // el atacante recibe 10 de daño

    // Ataques de efecto puro (sin daño)
    NO_EFFECT                     // daño base = 0, sin efecto especial
}
```

**Regla de diseño:** un ataque puede tener **N efectos** (ej: Flamethrower tiene `DISCARD_ATTACKER_ENERGY_1`; un ataque podría tener `APPLY_POISON` + `DISCARD_ATTACKER_ENERGY_1`). El registro guarda una `List<AttackEffectKey>` por ataque, no un único valor.

---

## Pieza 2 — El registro de traducción (quién traduce)

El registro de traducción mapea `(cardId, attackIndex)` → `List<AttackEffectKey>`.

```java
public class AttackEffectRegistry {

    // cardId → [efectos del ataque 0, efectos del ataque 1, ...]
    private static final Map<String, List<List<AttackEffectKey>>> REGISTRY = new HashMap<>();

    static {
        // xy1-4 — Venusaur-EX
        //   Ataque 0: "Frog Hop" — 40 daño, sin efecto
        //   Ataque 1: "Poison Powder" — 60 daño, envenena
        register("xy1-4",
            List.of(),                          // ataque 0: sin efecto
            List.of(APPLY_POISON)               // ataque 1: envenena al defensor
        );

        // xy1-11 — Charizard-EX
        //   Ataque 0: "Wing Attack" — 60 daño, sin efecto
        //   Ataque 1: "Stoke" — 0 daño, flip x3, por cada cara roba 1 carta
        register("xy1-11",
            List.of(),
            List.of(COIN_FLIP_DRAW_CARDS)       // efecto especial de Stoke
        );

        // ...continuar para cada carta de XY1
    }
}
```

### ¿Quién hace esta tarea?

**Cualquier miembro del equipo puede hacerla — no requiere conocer el engine.**

El proceso es mecánico:
1. Abrir la lista de cartas XY1 (pokemontcg.io o la BD)
2. Leer el texto de cada ataque
3. Identificar qué `AttackEffectKey` corresponde
4. Agregar la entrada al `static {}` block del registry

Si el efecto del ataque no existe en el enum → crear el nuevo valor primero, luego el dev que conoce el engine implementa el caso.

**Responsabilidad:** cualquier integrante del equipo puede hacer el mapeo texto → enum. Solo necesitan conocer las reglas del TCG.

---

## Pieza 3 — La implementación de efectos (quién implementa)

Cada `AttackEffectKey` necesita un `AttackEffect` que ejecute la lógica real sobre el `AttackContext`.

```java
@FunctionalInterface
public interface AttackEffect {
    void apply(AttackContext ctx);
}
```

```java
public class AttackEffectExecutor {

    private static final Map<AttackEffectKey, AttackEffect> EFFECTS = new EnumMap<>(AttackEffectKey.class);

    static {
        EFFECTS.put(APPLY_SLEEP, ctx ->
            ctx.getDefenderPokemon().setCondition(SpecialCondition.ASLEEP)
        );

        EFFECTS.put(APPLY_POISON, ctx ->
            ctx.getDefenderPokemon().setPoisoned(true)
        );

        EFFECTS.put(DISCARD_ATTACKER_ENERGY_1, ctx -> {
            PlayerField attacker = ctx.getAttackerField();
            List<AttachedCard> energies = ctx.getAttackerPokemon().getAttachedEnergies();
            if (!energies.isEmpty()) {
                AttachedCard discarded = energies.remove(energies.size() - 1);
                attacker.getDiscardPile().add(discarded.getCardId());
            }
        });

        EFFECTS.put(DAMAGE_PLUS_PER_BENCH, ctx -> {
            int benchCount = ctx.getDefenderField().getBench().size();
            ctx.addDamageBonus(benchCount * 20);  // +20 por cada banca del rival
        });

        EFFECTS.put(COIN_FLIP_DAMAGE_BONUS, ctx -> {
            if (ctx.getRandom().nextBoolean()) {  // cara
                ctx.addDamageBonus(30);
            }
        });

        // ...uno por cada AttackEffectKey
    }

    public static void execute(AttackEffectKey key, AttackContext ctx) {
        AttackEffect effect = EFFECTS.get(key);
        if (effect != null) effect.apply(ctx);
    }
}
```

**Responsabilidad:** quien conoce el engine y el `AttackContext`. Esto requiere entender el modelo de estado (`PlayerField`, `ActivePokemon`, etc.).

---

## Dónde se integra todo en el pipeline

### Modificaciones necesarias al `AttackContext`

`AttackContext` necesita dos campos nuevos:

```java
// En AttackContext
private int damageBonus = 0;         // acumulado por efectos pre-daño
private Random random;               // ya existe en AttackResolutionChain, pasarlo al ctx
```

### `PreAttackHandler` — efectos antes del daño

```java
@Override
public void handle(AttackContext ctx) {
    String cardId = ctx.getAttackerCard().getId();
    int attackIndex = ctx.getAttackIndex();

    List<AttackEffectKey> keys = AttackEffectRegistry.get(cardId, attackIndex);
    for (AttackEffectKey key : keys) {
        if (key.isPreDamage()) {            // DISCARD_ENERGY, COIN_FLIP_BONUS, etc.
            AttackEffectExecutor.execute(key, ctx);
        }
    }
}
```

### `DamageApplicationHandler` — usa el bonus acumulado

```java
int finalDamage = DamageCalculator.calculate(
    attackData.getBaseDamage() + ctx.getDamageBonus(),
    ctx.getAttackerCard(),
    ctx.getDefenderCard()
);
```

### `PostDamageHandler` — efectos después del daño

```java
// Reemplaza el keyword matching actual por:
List<AttackEffectKey> keys = AttackEffectRegistry.get(cardId, attackIndex);
for (AttackEffectKey key : keys) {
    if (key.isPostDamage()) {              // APPLY_SLEEP, APPLY_POISON, HEAL, SNIPE, etc.
        AttackEffectExecutor.execute(key, ctx);
    }
}
```

---

## Cómo categorizar cada `AttackEffectKey`

El enum puede llevar su propia categoría:

```java
public enum AttackEffectKey {

    APPLY_SLEEP(Phase.POST_DAMAGE),
    DISCARD_ATTACKER_ENERGY_1(Phase.PRE_DAMAGE),
    COIN_FLIP_DAMAGE_BONUS(Phase.PRE_DAMAGE),
    DAMAGE_PLUS_PER_BENCH(Phase.PRE_DAMAGE),
    HEAL_ATTACKER_30(Phase.POST_DAMAGE),
    SNIPE_BENCH_10(Phase.POST_DAMAGE);

    public enum Phase { PRE_DAMAGE, POST_DAMAGE }

    private final Phase phase;

    AttackEffectKey(Phase phase) { this.phase = phase; }

    public boolean isPreDamage()  { return phase == Phase.PRE_DAMAGE; }
    public boolean isPostDamage() { return phase == Phase.POST_DAMAGE; }
}
```

---

## Resumen de responsabilidades

| Tarea | Quién | Conocimiento requerido |
|-------|-------|----------------------|
| Leer texto de carta e identificar el `AttackEffectKey` correcto | Cualquier miembro del equipo | Reglas del TCG |
| Crear un `AttackEffectKey` nuevo si el efecto no existe | Cualquier miembro del equipo | Solo agregar al enum |
| Implementar el `AttackEffect` en `AttackEffectExecutor` | Dev con conocimiento del engine | `AttackContext`, `PlayerField`, `ActivePokemon` |
| Agregar la entrada al `AttackEffectRegistry` | Cualquier miembro del equipo | Solo conocer el cardId |
| Integrar en `PreAttackHandler` / `PostDamageHandler` | Dev con conocimiento del engine | Pipeline Chain of Responsibility |

---

## Flujo de trabajo recomendado por carta

```
Para cada carta de XY1 con efecto de ataque:

1. [Cualquiera] Leer el texto del ataque
2. [Cualquiera] Verificar si el AttackEffectKey ya existe en el enum
   ├── Sí → ir al paso 4
   └── No → [Cualquiera] agregar el valor al enum
             [Dev engine] implementar su AttackEffect en AttackEffectExecutor
3. [Cualquiera] Agregar entrada en AttackEffectRegistry.REGISTRY
4. ✅ Listo para esa carta
```

---

## Estado actual del código

| Clase | Estado | Acción necesaria |
|-------|--------|-----------------|
| `PostDamageHandler` | Usa keyword matching frágil | Reemplazar por `AttackEffectRegistry` |
| `PreAttackHandler` | Stub vacío | Implementar con `AttackEffectRegistry` |
| `DamageApplicationHandler` | No usa `damageBonus` | Agregar `ctx.getDamageBonus()` al cálculo |
| `AttackEffectKey` | **No existe** | Crear |
| `AttackEffectRegistry` | **No existe** | Crear |
| `AttackEffectExecutor` | **No existe** | Crear |
| `AttackContext` | Falta `damageBonus` y `random` expuesto | Agregar campos |

---

*Documento de diseño — tpi-pokemon-2w1-03 · 2026-05-25*
