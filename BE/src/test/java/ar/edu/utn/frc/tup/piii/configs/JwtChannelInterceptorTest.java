package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.entities.GameSession;
import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.repositories.GameSessionRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    private JwtChannelInterceptor interceptor;

    private Player player;
    private UUID gameId;
    private GameSession session;

    @BeforeEach
    void setUp() {
        interceptor = new JwtChannelInterceptor(jwtService, playerRepository, gameSessionRepository);

        player = Player.builder()
                .id(1L)
                .username("testuser")
                .build();

        gameId = UUID.randomUUID();

        Player opponent = Player.builder()
                .id(2L)
                .username("opponent")
                .build();

        session = GameSession.builder()
                .id(gameId)
                .player1(player)
                .player2(opponent)
                .status(GameStatus.ACTIVE)
                .build();
    }

    /**
     * Creates a CONNECT STOMP message with the given native header.
     * The accessor is set mutable so the interceptor can call setUser().
     */
    private Message<?> connectMessageWithHeader(String headerName, String headerValue) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (headerName != null && headerValue != null) {
            accessor.addNativeHeader(headerName, headerValue);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    /**
     * Creates a SUBSCRIBE STOMP message with the given destination and authentication.
     * The accessor is set mutable so the interceptor can read headers.
     */
    private Message<?> subscribeMessage(String destination,
                                        UsernamePasswordAuthenticationToken auth) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(auth);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    // ================================================================
    //  CONNECT tests
    // ================================================================

    @Test
    void connect_withValidBearerToken_shouldSetPrincipal() {
        Message<?> message = connectMessageWithHeader("Authorization", "Bearer valid-token");

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-token")).thenReturn("testuser");
        when(playerRepository.findByUsername("testuser")).thenReturn(Optional.of(player));

        Message<?> result = interceptor.preSend(message, null);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor
                .getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor.getUser());
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, resultAccessor.getUser());
        assertEquals(player, ((UsernamePasswordAuthenticationToken) resultAccessor.getUser()).getPrincipal());
    }

    @Test
    void connect_withValidFallbackToken_shouldSetPrincipal() {
        Message<?> message = connectMessageWithHeader("token", "valid-token");

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-token")).thenReturn("testuser");
        when(playerRepository.findByUsername("testuser")).thenReturn(Optional.of(player));

        Message<?> result = interceptor.preSend(message, null);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor
                .getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor.getUser());
    }

    @Test
    void connect_withInvalidToken_shouldThrow() {
        Message<?> message = connectMessageWithHeader("Authorization", "Bearer bad-token");

        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void connect_withMissingToken_shouldThrow() {
        Message<?> message = connectMessageWithHeader(null, null);

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void connect_withValidTokenButUnknownUser_shouldThrow() {
        Message<?> message = connectMessageWithHeader("Authorization", "Bearer valid-token");

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-token")).thenReturn("nobody");
        when(playerRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, null));
    }

    // ================================================================
    //  SUBSCRIBE tests
    // ================================================================

    @Test
    void subscribe_toOwnGameEvents_shouldAllow() {
        var auth = new UsernamePasswordAuthenticationToken(player, null, Collections.emptyList());
        Message<?> message = subscribeMessage("/topic/games/" + gameId + "/events", auth);

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        Message<?> result = interceptor.preSend(message, null);
        assertNotNull(result);
    }

    @Test
    void subscribe_toOwnGameStateChanged_shouldAllow() {
        var auth = new UsernamePasswordAuthenticationToken(player, null, Collections.emptyList());
        Message<?> message = subscribeMessage("/topic/games/" + gameId + "/state-changed", auth);

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        Message<?> result = interceptor.preSend(message, null);
        assertNotNull(result);
    }

    @Test
    void subscribe_toAnotherGame_shouldThrow() {
        UUID otherGameId = UUID.randomUUID();
        Player otherPlayer1 = Player.builder().id(3L).username("other1").build();
        Player otherPlayer2 = Player.builder().id(4L).username("other2").build();
        GameSession otherSession = GameSession.builder()
                .id(otherGameId)
                .player1(otherPlayer1)
                .player2(otherPlayer2)
                .status(GameStatus.ACTIVE)
                .build();

        var auth = new UsernamePasswordAuthenticationToken(player, null, Collections.emptyList());
        Message<?> message = subscribeMessage("/topic/games/" + otherGameId + "/events", auth);

        when(gameSessionRepository.findById(otherGameId)).thenReturn(Optional.of(otherSession));

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void subscribe_withoutAuth_shouldThrow() {
        Message<?> message = subscribeMessage("/topic/games/" + gameId + "/events", null);

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void subscribe_toNonGameTopic_shouldPassWithoutValidation() {
        var auth = new UsernamePasswordAuthenticationToken(player, null, Collections.emptyList());
        Message<?> message = subscribeMessage("/topic/game/pong", auth);

        // No mock on gameSessionRepository needed — validation is skipped
        Message<?> result = interceptor.preSend(message, null);
        assertNotNull(result);
    }

    @Test
    void subscribe_toGameWherePlayer2_shouldAllow() {
        // player is player2, not player1
        Player player1 = Player.builder().id(3L).username("p1").build();
        GameSession p2Session = GameSession.builder()
                .id(gameId)
                .player1(player1)
                .player2(player)
                .status(GameStatus.ACTIVE)
                .build();

        var auth = new UsernamePasswordAuthenticationToken(player, null, Collections.emptyList());
        Message<?> message = subscribeMessage("/topic/games/" + gameId + "/events", auth);

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(p2Session));

        Message<?> result = interceptor.preSend(message, null);
        assertNotNull(result);
    }

    @Test
    void subscribe_toGameWhenPlayer2Null_shouldOnlyCheckPlayer1() {
        // session has no player2 (WAITING status)
        GameSession waitingSession = GameSession.builder()
                .id(gameId)
                .player1(player)
                .player2(null)
                .status(GameStatus.WAITING)
                .build();

        var auth = new UsernamePasswordAuthenticationToken(player, null, Collections.emptyList());
        Message<?> message = subscribeMessage("/topic/games/" + gameId + "/state-changed", auth);

        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(waitingSession));

        Message<?> result = interceptor.preSend(message, null);
        assertNotNull(result);
    }

    @Test
    void subscribe_withGameNotFound_shouldThrow() {
        UUID unknownId = UUID.randomUUID();

        var auth = new UsernamePasswordAuthenticationToken(player, null, Collections.emptyList());
        Message<?> message = subscribeMessage("/topic/games/" + unknownId + "/events", auth);

        when(gameSessionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, null));
    }

    // ================================================================
    //  Non-STOMP messages (should pass through)
    // ================================================================

    @Test
    void nonStompMessage_shouldPassThrough() {
        Message<?> message = MessageBuilder.withPayload(new byte[0]).build();

        Message<?> result = interceptor.preSend(message, null);
        assertNotNull(result);
    }
}
