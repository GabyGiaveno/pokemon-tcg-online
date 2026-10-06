# Auth Security

## Requirements

### Requirement: HTTP Authentication and Public Route Access

The system MUST enforce JWT Bearer authentication on protected HTTP endpoints and
permit access to designated public endpoints.

#### Scenario: Authenticated HTTP request to a protected endpoint

- GIVEN a registered player with a valid JWT
- WHEN the client sends `GET /api/games` with header `Authorization: Bearer <valid token>`
- THEN the response status MUST be 200

#### Scenario: Public endpoints remain accessible without authentication

- GIVEN no `Authorization` header and no token
- WHEN the client requests an endpoint listed under `permitAll()` (e.g. `/api/auth/register`,
  `GET /api/cards/**`)
- THEN the response status MUST NOT be 401

### Requirement: WebSocket/SockJS Handshake Authentication

The system MUST authenticate the WebSocket/SockJS handshake (`/ws/info`) using a valid JWT
sent as a `?token=` query parameter, and MUST return a non-401 response for an authenticated
player, regardless of whether that player's lazy JPA collections (`decks`,
`sessionsAsPlayer1`) are loaded.

#### Scenario: Authenticated player requests /ws/info

- GIVEN a registered player with a valid JWT
- AND the player's `decks` and `sessionsAsPlayer1` collections are not eagerly loaded
- WHEN the client sends `GET /ws/info?token=<valid token>`
- THEN the response status MUST be 200 (non-401)
- AND no `LazyInitializationException` MUST occur while resolving the security principal

#### Scenario: Invalid or missing token on /ws/info

- GIVEN no token or an invalid/expired JWT
- WHEN the client sends `GET /ws/info`
- THEN the response MUST reflect an authentication failure consistent with existing JWT
  validation behavior

### Requirement: Security Principal Lazy-Safety

The `Player` entity used as the Spring Security principal MUST NOT trigger
`LazyInitializationException` when `toString()`, `equals()`, or `hashCode()` is invoked
outside an active Hibernate session.

#### Scenario: toString/equals/hashCode called without a Hibernate session

- GIVEN a `Player` instance loaded as a security principal with `open-in-view: false`
- AND no active Hibernate session
- WHEN `toString()`, `equals()`, or `hashCode()` is called on the `Player` instance
- THEN no `LazyInitializationException` MUST be thrown
- AND the lazy collections `decks` and `sessionsAsPlayer1` MUST NOT be traversed by these methods

### Requirement: Error Dispatch Status Transparency

When Spring forwards a request to `/error` after an unhandled exception on a protected route,
the `/error` dispatch MUST be permitted (not itself re-protected), so the original error
status (e.g. 500) surfaces to the client instead of being re-masked as 401.

#### Scenario: Unhandled internal error on a protected route

- GIVEN an authenticated request to a protected route
- WHEN the route handler throws an unhandled exception (e.g. 500)
- AND Spring forwards the request internally to `/error`
- THEN the `/error` dispatch MUST be permitted by `SecurityConfig`
- AND the response status returned to the client MUST be the original error status, not 401
