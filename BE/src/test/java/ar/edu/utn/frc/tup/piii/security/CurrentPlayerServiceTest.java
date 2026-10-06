package ar.edu.utn.frc.tup.piii.security;

import ar.edu.utn.frc.tup.piii.entities.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentPlayerServiceTest {

    private final CurrentPlayerService currentPlayerService = new CurrentPlayerService();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentPlayer_returnsAuthenticatedPlayer() {
        Player player = Player.builder().id(7L).username("ash").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(player, null)
        );

        assertEquals(player, currentPlayerService.getCurrentPlayer());
        assertEquals(7L, currentPlayerService.getCurrentPlayerId());
    }

    @Test
    void getCurrentPlayer_whenPrincipalIsNotPlayer_throws() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ash", null)
        );

        assertThrows(IllegalStateException.class, () -> currentPlayerService.getCurrentPlayer());
    }
}
