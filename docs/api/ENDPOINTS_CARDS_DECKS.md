# Implementación API - Cards + Decks

## Contexto general

Este proyecto es un TPI de Pokémon TCG con backend en Java 21 + Spring Boot 3.x y frontend Angular.  
La consigna exige que el backend sea la única fuente de verdad del juego. El frontend solo debe presentar datos y enviar acciones.

Actualmente el proyecto ya tiene entidades modeladas y varias clases stub. La idea de esta tarea es avanzar sobre la parte de API relacionada con:

- Cartas
- Caché local de cartas
- Sincronización del set XY1
- Mazos
- Validación de mazos
- DTOs request/response
- Controllers
- Services
- Repositories
- Tests básicos

Esta tarea NO debe implementar todavía el motor completo del juego.  
No implementar ataques, turnos, KO, condiciones especiales ni lógica de partida avanzada. Eso queda para otra etapa.

---

# Objetivo de esta tarea

Implementar una API REST funcional y limpia para Cards y Decks.

Debe permitir:

1. Consultar cartas desde el caché local.
2. Buscar cartas con filtros y paginado.
3. Consultar una carta por ID.
4. Sincronizar cartas del set obligatorio `xy1`.
5. Crear mazos.
6. Listar mazos del jugador.
7. Consultar un mazo por ID.
8. Editar un mazo.
9. Eliminar un mazo.
10. Validar un mazo según reglas oficiales.

---

# Reglas importantes de arquitectura

## No exponer entidades directamente

Los controllers NO deben devolver entidades JPA directamente.

Usar DTOs siempre:

- Request DTOs para datos que entran.
- Response DTOs para datos que salen.

Esto evita:

- ciclos infinitos por relaciones JPA;
- exponer datos internos;
- respuestas enormes;
- acoplar frontend con la base de datos;
- problemas futuros cuando cambien las entidades.

---

## Controller liviano

Los controllers solo deben:

- recibir requests;
- validar formato básico con `@Valid`;
- delegar en services;
- devolver responses con códigos HTTP correctos.

No meter lógica de negocio fuerte en controllers.

Flujo esperado:

```text
Controller
    -> Service
        -> Repository
        -> Mapper DTO/Entity
    -> Response DTO