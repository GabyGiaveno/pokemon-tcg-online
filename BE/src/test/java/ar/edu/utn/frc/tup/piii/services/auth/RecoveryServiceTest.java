package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.entities.RecoveryToken;
import ar.edu.utn.frc.tup.piii.mail.EmailService;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.repositories.RecoveryTokenRepository;
import ar.edu.utn.frc.tup.piii.services.RecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecoveryServiceTest {

    @Mock private PlayerRepository playerRepository;
    @Mock private RecoveryTokenRepository recoveryTokenRepository;
    @Mock private EmailService emailService;

    private PasswordEncoder passwordEncoder;
    private RecoveryService recoveryService;

    @Captor private ArgumentCaptor<RecoveryToken> tokenCaptor;

    private Player existingPlayer;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        recoveryService = new RecoveryService(
                playerRepository, recoveryTokenRepository, emailService, passwordEncoder
        );
        ReflectionTestUtils.setField(recoveryService, "frontendUrl", "http://localhost:4200");

        existingPlayer = Player.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("oldPass123"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void requestRecovery_withExistingEmail_createsTokenAndSendsEmail() {
        when(playerRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(existingPlayer));

        recoveryService.requestRecovery("test@example.com");

        verify(recoveryTokenRepository).deleteByPlayerId(1L);
        verify(recoveryTokenRepository).save(tokenCaptor.capture());

        RecoveryToken saved = tokenCaptor.getValue();
        assertNotNull(saved.getToken());
        assertEquals(existingPlayer.getId(), saved.getPlayer().getId());
        assertFalse(saved.isUsed());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));

        verify(emailService).sendRecoveryEmail(
                eq("test@example.com"),
                eq("testuser"),
                contains("/auth/reset-password?token=")
        );
    }

    @Test
    void requestRecovery_withNonExistingEmail_doesNothing() {
        when(playerRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        recoveryService.requestRecovery("nonexistent@example.com");

        verify(recoveryTokenRepository, never()).deleteByPlayerId(any());
        verify(recoveryTokenRepository, never()).save(any());
        verify(emailService, never()).sendRecoveryEmail(anyString(), anyString(), anyString());
    }

    @Test
    void requestRecovery_invalidatesPreviousTokensBeforeCreatingNewOne() {
        when(playerRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(existingPlayer));

        recoveryService.requestRecovery("test@example.com");

        verify(recoveryTokenRepository).deleteByPlayerId(1L);
    }

    @Test
    void resetPassword_withValidToken_updatesPasswordAndMarksUsed() {
        String rawToken = "valid-uuid-token";
        RecoveryToken recoveryToken = RecoveryToken.builder()
                .id(1L)
                .token(rawToken)
                .player(existingPlayer)
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .used(false)
                .build();

        when(recoveryTokenRepository.findByToken(rawToken))
                .thenReturn(Optional.of(recoveryToken));

        String newPassword = "NewPassword123!";
        recoveryService.resetPassword(rawToken, newPassword);

        assertTrue(passwordEncoder.matches(newPassword, existingPlayer.getPasswordHash()));
        assertTrue(recoveryToken.isUsed());
        verify(playerRepository).save(existingPlayer);
        verify(recoveryTokenRepository).save(recoveryToken);
    }

    @Test
    void resetPassword_withInvalidToken_throwsException() {
        when(recoveryTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        SecurityException ex = assertThrows(SecurityException.class,
                () -> recoveryService.resetPassword("invalid-token", "anyPass"));
        assertEquals("Invalid or expired token", ex.getMessage());
        verify(playerRepository, never()).save(any());
    }

    @Test
    void resetPassword_withExpiredToken_throwsException() {
        RecoveryToken expiredToken = RecoveryToken.builder()
                .id(1L)
                .token("expired-token")
                .player(existingPlayer)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(recoveryTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(expiredToken));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> recoveryService.resetPassword("expired-token", "newPass"));
        assertEquals("Token expired", ex.getMessage());
        verify(playerRepository, never()).save(any());
    }

    @Test
    void resetPassword_withUsedToken_throwsException() {
        RecoveryToken usedToken = RecoveryToken.builder()
                .id(1L)
                .token("used-token")
                .player(existingPlayer)
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .used(true)
                .build();

        when(recoveryTokenRepository.findByToken("used-token"))
                .thenReturn(Optional.of(usedToken));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> recoveryService.resetPassword("used-token", "newPass"));
        assertEquals("Token already used", ex.getMessage());
        verify(playerRepository, never()).save(any());
    }
}
