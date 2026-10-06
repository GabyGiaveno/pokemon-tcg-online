package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.entities.RecoveryToken;
import ar.edu.utn.frc.tup.piii.mail.EmailService;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.repositories.RecoveryTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecoveryService {

    private static final long TOKEN_EXPIRATION_MINUTES = 30;

    private final PlayerRepository playerRepository;
    private final RecoveryTokenRepository recoveryTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public RecoveryService(PlayerRepository playerRepository,
                           RecoveryTokenRepository recoveryTokenRepository,
                           EmailService emailService,
                           PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.recoveryTokenRepository = recoveryTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void requestRecovery(String email) {
        Optional<Player> playerOpt = playerRepository.findByEmail(email);

        if (playerOpt.isEmpty()) {
            return;
        }

        Player player = playerOpt.get();

        recoveryTokenRepository.deleteByPlayerId(player.getId());

        String tokenValue = UUID.randomUUID().toString();
        RecoveryToken recoveryToken = RecoveryToken.builder()
                .token(tokenValue)
                .player(player)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES))
                .used(false)
                .build();

        recoveryTokenRepository.save(recoveryToken);

        String recoveryLink = frontendUrl + "/auth/reset-password?token=" + tokenValue;
        emailService.sendRecoveryEmail(player.getEmail(), player.getUsername(), recoveryLink);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        RecoveryToken recoveryToken = recoveryTokenRepository.findByToken(token)
                .orElseThrow(() -> new SecurityException("Invalid or expired token"));

        if (recoveryToken.isUsed()) {
            throw new SecurityException("Token already used");
        }

        if (recoveryToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new SecurityException("Token expired");
        }

        Player player = recoveryToken.getPlayer();
        player.setPasswordHash(passwordEncoder.encode(newPassword));
        playerRepository.save(player);

        recoveryToken.setUsed(true);
        recoveryTokenRepository.save(recoveryToken);
    }
}
