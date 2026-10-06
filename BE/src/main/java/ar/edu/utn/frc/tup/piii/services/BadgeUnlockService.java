package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.entities.Badge;
import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.entities.PlayerBadge;
import ar.edu.utn.frc.tup.piii.entities.PlayerBadgeId;
import ar.edu.utn.frc.tup.piii.repositories.BadgeRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerBadgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BadgeUnlockService {

    private static final Logger log = LoggerFactory.getLogger(BadgeUnlockService.class);

    private final BadgeRepository badgeRepository;
    private final PlayerBadgeRepository playerBadgeRepository;

    public BadgeUnlockService(BadgeRepository badgeRepository, PlayerBadgeRepository playerBadgeRepository) {
        this.badgeRepository = badgeRepository;
        this.playerBadgeRepository = playerBadgeRepository;
    }

    public void checkAndUnlock(Player player, int winStreak) {
        if (winStreak >= 10) {
            unlockIfMissing(player, "dragon");
        }
    }

    private void unlockIfMissing(Player player, String badgeId) {
        PlayerBadgeId id = new PlayerBadgeId(player.getId(), badgeId);
        if (playerBadgeRepository.findById(id).isEmpty()) {
            Optional<Badge> badgeOpt = badgeRepository.findById(badgeId);
            if (badgeOpt.isPresent()) {
                PlayerBadge pb = PlayerBadge.builder()
                        .id(id)
                        .player(player)
                        .badge(badgeOpt.get())
                        .build();
                playerBadgeRepository.save(pb);
                log.info("Badge unlocked [{}] for player [{}]", badgeId, player.getUsername());
            }
        }
    }
}
