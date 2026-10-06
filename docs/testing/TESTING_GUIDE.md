# Guía para escribir tests en el Backend (sin modificar código de producción)

> Para: Jesús — cómo testear el backend SIN tocar ni una línea de `src/main/`

## Filosofía

Un buen test **valida comportamiento, no verifica que Lombok funciona**. No importa cuántos tests tengas si ninguno te avisa cuando rompés una regla de negocio.

---

## Patrones de test en el proyecto

### 1. Test de integración con `@SpringBootTest`

Ejemplo: `AuthIntegrationTest`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class XxxIntegrationTest {

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary
        SomeService mockService() {
            return mock(SomeService.class);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private SomeService someService;

    private RestClient client;

    @BeforeEach
    void setUp() {
        reset(someService);
        client = RestClient.create("http://localhost:" + port);
    }
}
```

**Cuándo usarlo:** cuando querés probar el stack completo (controller → service → repositorio mockeado → response). Verifica que:
- El endpoint existe y responde el HTTP status correcto
- El body se serializa/deserializa bien
- Los errores se manejan correctamente (400, 401, 404, 500)

### 2. Test unitario puro con mocks

Ejemplo: `TurnManagerTest`, `EnergyValidationHandlerTest`

```java
class XxxTest {
    private ServiceA mockA;
    private ServiceB mockB;
    private TargetClass target;

    @BeforeEach
    void setUp() {
        mockA = mock(ServiceA.class);
        mockB = mock(ServiceB.class);
        target = new TargetClass(mockA, mockB);
    }

    @Test
    void shouldDoX_whenY() {
        when(mockA.something()).thenReturn(value);

        var result = target.doSomething(input);

        assertTrue(result.isSuccess());
        verify(mockA).something();
    }
}
```

**Cuándo usarlo:** para lógica de negocio compleja (engine, validators, effect logics). Rápidos, aislados, sin Spring.

### 3. Test de controller con MockMvc

Ejemplo referencia (no existe en el proyecto aún, es lo que deberías hacer):

```java
@WebMvcTest(XxxController.class)
class XxxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XxxService xxxService;

    @Test
    void shouldReturn200() throws Exception {
        when(xxxService.findAll()).thenReturn(List.of(...));

        mockMvc.perform(get("/api/xxx"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturn404_whenNotFound() throws Exception {
        when(xxxService.findById(999L)).thenThrow(new NoSuchElementException("not found"));

        mockMvc.perform(get("/api/xxx/999"))
               .andExpect(status().isNotFound());
    }
}
```

---

## Qué controllers NO tienen tests (ACA PODÉS CONTRIBUIR)

| Controller | Endpoints | Tiene test? |
|------------|-----------|-------------|
| `PingController` | `GET /ping` | ✅ `PingControllerTest` |
| `AuthController` | register, login, etc. | ✅ `AuthIntegrationTest` |
| `CardController` | `GET /api/cards`, `GET /api/cards/{id}`, `POST /api/cards/sync` | ❌ **VACÍO** |
| `DeckController` | CRUD `/api/decks`, validate | ❌ **VACÍO** |
| `GameController` | create, join, actions, state | ❌ **VACÍO** |
| `PlayerController` | profile, skins, achievements | ❌ **VACÍO** |
| `GameWebSocketController` | WebSocket `/game/ping` | ❌ **VACÍO** |

---

## Qué deberías testear (priorizado)

### Prioridad 1: Controladores REST (MockMvc)

Son los más fáciles de testear y los que más falta hacen. NO modificás nada de producción.

```
CardControllerTest
├── GET /api/cards?page=0&size=10 → 200 + body paginado
├── GET /api/cards?name=Venusaur → 200 + filtrado
├── GET /api/cards?type=Fire → 200 + filtrado
├── GET /api/cards/xy1-1 → 200 + Venusaur-EX
├── GET /api/cards/xyz-999 → 404 (no existe)
└── POST /api/cards/sync?set=xy1 → 200 + sync response

DeckControllerTest
├── GET /api/decks → 200 + lista de mazos
├── GET /api/decks/1 → 200 + mazo con cartas
├── GET /api/decks/999 → 404
├── POST /api/decks → 201 + mazo creado
├── POST /api/decks (nombre vacío) → 400
├── PUT /api/decks/1 → 200 + actualizado
├── DELETE /api/decks/1 → 204
└── POST /api/decks/1/validate → 200 + validación

GameControllerTest
├── POST /api/games → 201 + game session
├── POST /api/games/{id}/join → 200 + unido
├── POST /api/games/{id}/join (lleno) → 400
├── GET /api/games/{id}/state → 200 + board state
├── POST /api/games/{id}/actions → 200 + action result
└── POST /api/games/{id}/actions (sin auth) → 401
```

### Prioridad 2: Más casos borde en servicios existentes

Los services ya tienen tests, pero siempre faltan edge cases:

```java
DeckServiceTest
├── crear mazo con 61 cartas → error
├── crear mazo con 4 copias + 1 extra → error de validación
├── crear mazo sin Pokémon básico → error
└── eliminar mazo de otro jugador → error

CardCacheServiceTest
├── getCards con filtros combinados (name + type + supertype)
├── findById con UUID (instanceId) → IllegalArgumentException
└── warmCache con API caída → log warning, no crash
```

### Prioridad 3: Más escenarios en el engine

El engine ya tiene 36 tests, pero podés agregar:

```java
VictoryConditionCheckerTest
├── ambos jugadores sin Pokémon → empate / error
├── Pokémon con 0 HP pero con herramienta que previene KO
└── KO múltiple en un solo ataque

MainPhaseStateTest
├── jugar stadium cuando ya hay uno activo
├── jugar supporter después de haber jugado otro en el mismo turno
└── adjuntar energía a un Pokémon que no es del tipo correcto
```

---

## Lo que NO tenés que hacer NUNCA

### ❌ Tests POJO manuales (ya están cubiertos por `PojoReflectionTest`)

```java
// MAL: esto no testea nada útil, Lombok ya genera los getters
@Test
void loginRequest_gettersSetters() {
    LoginRequest r = new LoginRequest();
    r.setUsername("ash");
    r.setPassword("pikachu");
    assertEquals("ash", r.getUsername());
    assertEquals("pikachu", r.getPassword());
}
```

El `PojoReflectionTest` ya ejercita automáticamente todos los getters/setters/builders/equals/hashCode/toString de todas las clases. Agregar tests manuales para cada DTO es **trabal repetido y sin valor**.

### ❌ Tests que solo existen para subir cobertura

```java
// MAL: test que existe solo para que JaCoCo vea una línea "cubierta"
@Test
void noOpTest() {
    assertTrue(true);
}
```

### ❌ Modificar código de producción para hacerlo "testeable"

Si necesitás cambiar visibilidad de métodos, agregar setters, o exponer internals para poder testear → **está mal**. El código de producción no se toca. Usá mocks, reflection, o pensá otro approach.

---

## Checklist para cada test que escribas

- [ ] ¿Este test me avisaría si alguien rompe esta funcionalidad?
- [ ] ¿Testea comportamiento o verifica que el compilador funciona?
- [ ] ¿Cubre al menos un caso borde (null, vacío, 404, 400, etc.)?
- [ ] ¿Corre sin modificar una sola línea de `src/main/`?
- [ ] ¿El nombre del test explica QUÉ debería pasar y EN QUÉ condición? (`shouldReturn404_whenCardDoesNotExist`)

---

## Resumen

| Hacé esto | No hagas esto |
|-----------|---------------|
| Tests de controllers con MockMvc | Tests manuales de getters/setters |
| Casos borde en services | Tests que solo existen por cobertura |
| Escenarios de error (400, 401, 404) | Modificar código de producción |
| Tests con nombres descriptivos | Testear que Lombok funciona |

> Un test que siempre pasa pero nunca falla cuando el código se rompe no es un test, es un checklist de compilación.
