package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.entities.GameSession;
import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.repositories.GameSessionRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Intercepts STOMP frames:
 * <ul>
 *   <li><b>CONNECT</b> — validates the JWT token before allowing the
 *       WebSocket session to be established.</li>
 *   <li><b>SUBSCRIBE</b> — for game-scoped topics, verifies the
 *       authenticated user is a participant of that game.</li>
 * </ul>
 *
 * <p>The JWT token is expected in one of:
 * <ul>
 *   <li><b>Authorization</b> header: {@code Bearer <JWT>} (preferred)</li>
 *   <li><b>token</b> header: {@code <JWT>} (fallback, used by some STOMP clients)</li>
 * </ul>
 *
 * <p>If validation fails, the frame is rejected and an ERROR frame is sent
 * back to the client.
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SUBSCRIBE_DESTINATION_PATTERN =
            Pattern.compile("^/topic/games/([0-9a-fA-F-]+)/(events|state-changed)$");

    private final JwtService jwtService;
    private final PlayerRepository playerRepository;
    private final GameSessionRepository gameSessionRepository;

    public JwtChannelInterceptor(JwtService jwtService,
                                 PlayerRepository playerRepository,
                                 GameSessionRepository gameSessionRepository) {
        this.jwtService = jwtService;
        this.playerRepository = playerRepository;
        this.gameSessionRepository = gameSessionRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    // ================================================================
    //  CONNECT handling
    // ================================================================

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);

        if (token == null || !jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Missing or invalid JWT token");
        }

        String username = jwtService.extractUsername(token);
        Optional<Player> playerOpt = playerRepository.findByUsername(username);

        if (playerOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found for JWT token");
        }

        Player player = playerOpt.get();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(player, null, Collections.emptyList());
        accessor.setUser(auth);
    }

    // ================================================================
    //  SUBSCRIBE handling
    // ================================================================

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required to subscribe");
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            throw new IllegalArgumentException("Missing destination");
        }

        var matcher = SUBSCRIBE_DESTINATION_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            // Non-game topics (e.g. /topic/game/pong) are allowed without extra checks.
            return;
        }

        UUID gameId = UUID.fromString(matcher.group(1));
        Long playerId = resolvePlayerId(principal);

        GameSession session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        boolean isParticipant = session.getPlayer1().getId().equals(playerId)
                || (session.getPlayer2() != null && session.getPlayer2().getId().equals(playerId));

        if (!isParticipant) {
            throw new IllegalArgumentException(
                    "Player " + playerId + " is not a participant of game " + gameId);
        }
    }

    /**
     * Extracts the authenticated Player's database ID from the Principal
     * that was set during the CONNECT handshake.
     */
    private Long resolvePlayerId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            Object principalObj = token.getPrincipal();
            if (principalObj instanceof Player player) {
                return player.getId();
            }
        }
        throw new IllegalArgumentException("Cannot resolve player identity from principal");
    }

    // ================================================================
    //  Token extraction
    // ================================================================

    /**
     * Extracts the JWT token from the STOMP CONNECT frame.
     *
     * <p>Priority: {@code Authorization: Bearer <JWT>} first,
     * then a plain {@code token: <JWT>} header as fallback.
     */
    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        String token = accessor.getFirstNativeHeader("token");
        if (token != null && !token.isBlank()) {
            return token;
        }
        return null;
    }
}
