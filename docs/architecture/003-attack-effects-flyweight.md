# Architecture Decision Record: 003 - Resolución Dinámica de Efectos de Ataque (Patrón Flyweight)

## 1. El Problema Original

En la iteración inicial del Motor de Juego (Engine), la resolución de efectos adicionales a los ataques (como paralizar, curar, descartar energías, etc.) era frágil y poco escalable. 
El `PostDamageHandler` dentro de la *Chain of Responsibility* dependía del texto en crudo (human-readable string) provisto por la API de Pokémon TCG. 

El código original utilizaba *hardcoded string matching* (búsqueda de subcadenas estáticas) para determinar qué hacer:
```java
if (text.contains("defending pokémon is now asleep") || text.contains("defending pokemon is now asleep")) {
    defender.setCondition(SpecialCondition.ASLEEP);
}
```

**Problemas detectados:**
- **Inmantenibilidad:** Cualquier ligera variación en la redacción de la API rompía la lógica.
- **Falta de Extensibilidad:** Imposible programar comportamientos complejos como "tirar 3 monedas y hacer 20 de daño por cada cara" mediante simples condicionales de texto.
- **Baja Performance:** Ejecutar `.contains()` múltiples veces por cada ataque es ineficiente computacionalmente en un entorno de alta concurrencia.

## 2. La Solución: Jackson Polimórfico + Patrón Flyweight

Para solucionar este problema de raíz, diseñamos una arquitectura basada en **separación de responsabilidades (Datos vs. Comportamiento)**, dividiendo el problema en dos capas:

### Capa 1: Estado Extrínseco (Datos) - Deserialización Polimórfica
Se desarrolló un script para traducir los textos complejos de las habilidades a objetos atómicos en formato JSON (`xy1_parsed.json`).
Para que el backend en Java pueda procesar este array dinámico donde cada elemento puede tener propiedades distintas, utilizamos las anotaciones polimórficas de **Jackson**.

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ApplyConditionEffect.class, name = "APPLY_CONDITION"),
    @JsonSubTypes.Type(value = HealEffect.class, name = "HEAL"),
    // ...
})
public abstract class AttackEffect { ... }
```
Con esto, `AttackData.parsedEffects` se carga automáticamente con DTOs fuertemente tipados. Estos DTOs representan únicamente la *información* del efecto (ej. "Curar", "Cantidad: 30", "Objetivo: SELF").

### Capa 2: Estado Intrínseco (Comportamiento) - Patrón Flyweight
No tendría sentido instanciar un nuevo objeto con el código para curar 30 puntos de vida por cada carta `Venusaur-EX` existente en la memoria. El comportamiento es el mismo, solo cambian los parámetros.

Aquí entra el patrón **Flyweight**. 
Se creó una fábrica/registro singleton llamado `EffectRegistry` que mapea cada clase DTO a una única instancia de su lógica correspondiente (`EffectLogic`).

```java
public class EffectRegistry {
    private static final EffectRegistry INSTANCE = new EffectRegistry();
    private final Map<Class<?>, EffectLogic<?>> registry = new HashMap<>();

    private EffectRegistry() {
        register(ApplyConditionEffect.class, new ApplyConditionLogic());
        register(HealEffect.class, new HealLogic());
        register(AddDamageEffect.class, new AddDamageLogic());
        // ...
    }
}
```

Cada `EffectLogic` es una clase sin estado (stateless) que define un método `execute(DTO, AttackContext)`. Al no tener estado interno, estas lógicas son inherentemente *thread-safe* y pueden ser re-utilizadas por cualquier cantidad de partidas concurrentes simultáneamente.

## 3. Integración en el Chain of Responsibility

Los handlers del pipeline de ataques se refactorizaron para aprovechar esta nueva arquitectura:

1. **`PreAttackHandler`**: Ahora itera los efectos buscando aquellos que deban resolverse *antes* del cálculo de daño (e.g., `AddDamageEffect`, `MultiplierDamageEffect`) e invoca sus respectivos Flyweights pasándoles el contexto actual para que modifiquen los acumuladores de daño.
2. **`PostDamageHandler`**: Recorre los efectos que deban resolverse *después* del daño (e.g., `ApplyConditionEffect`, `HealEffect`) e invoca a sus lógicas usando el `EffectRegistry`. 

## 4. Conclusión y Beneficios

- **Performance O(1) en Memoria de Lógicas:** Tenemos exactamente 1 instancia de cada lógica de efecto, previniendo la saturación del *Garbage Collector* y reduciendo drásticamente la latencia.
- **Robustez:** Adiós al `String.contains()`. Ahora la ejecución se basa en contratos tipados y variables numéricas controladas.
- **Escalabilidad:** Añadir un nuevo efecto al juego consta únicamente de crear un DTO, escribir su clase de lógica (Flyweight), y registrarla en el `EffectRegistry` con una línea de código. Todo el pipeline subyacente sigue funcionando sin modificaciones.
