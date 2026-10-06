package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.entities.Achievement;
import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.entities.PlayerAchievement;
import ar.edu.utn.frc.tup.piii.entities.PlayerAchievementId;
import ar.edu.utn.frc.tup.piii.repositories.AchievementRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerAchievementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AchievementUnlockService {

    private static final Logger log = LoggerFactory.getLogger(AchievementUnlockService.class);

    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;

    public AchievementUnlockService(AchievementRepository achievementRepository,
                                    PlayerAchievementRepository playerAchievementRepository) {
        this.achievementRepository = achievementRepository;
        this.playerAchievementRepository = playerAchievementRepository;
    }

    public void checkAndUnlock(Player player, int totalWins, int totalGames, int totalCards) {
        if (totalWins >= 1) {
            unlockIfMissing(player, "first-win");
        }
        if (totalCards >= 100) {
            unlockIfMissing(player, "collector-100");
        }
        if (totalGames >= 100) {
            unlockIfMissing(player, "veteran");
        }
    }

    public void checkDeckMaster(Player player, int decksCreated) {
        if (decksCreated >= 10) {
            unlockIfMissing(player, "deck-master");
        }
    }

    public void checkLucky(Player player) {
        unlockIfMissing(player, "lucky");
    }

    private void unlockIfMissing(Player player, String achievementId) {
        PlayerAchievementId id = new PlayerAchievementId(player.getId(), achievementId);
        if (playerAchievementRepository.findById(id).isEmpty()) {
            Optional<Achievement> achievementOpt = achievementRepository.findById(achievementId);
            if (achievementOpt.isPresent()) {
                PlayerAchievement pa = PlayerAchievement.builder()
                        .id(id)
                        .player(player)
                        .achievement(achievementOpt.get())
                        .build();
                playerAchievementRepository.save(pa);
                log.info("Achievement unlocked [{}] for player [{}]", achievementId, player.getUsername());
            }
        }
    }
}
