package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.entities.Achievement;
import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.entities.PlayerAchievement;
import ar.edu.utn.frc.tup.piii.entities.PlayerAchievementId;
import ar.edu.utn.frc.tup.piii.repositories.AchievementRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerAchievementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementUnlockServiceTest {

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private PlayerAchievementRepository playerAchievementRepository;

    @Captor
    private ArgumentCaptor<PlayerAchievement> playerAchievementCaptor;

    private AchievementUnlockService achievementUnlockService;

    @BeforeEach
    void setUp() {
        achievementUnlockService = new AchievementUnlockService(achievementRepository, playerAchievementRepository);
    }

    @Test
    void checkAndUnlock_unlocksAllMatchingAchievements() {
        Player player = Player.builder().id(1L).username("ash").build();
        when(playerAchievementRepository.findById(any())).thenReturn(Optional.empty());
        when(achievementRepository.findById("first-win")).thenReturn(Optional.of(Achievement.builder().id("first-win").build()));
        when(achievementRepository.findById("collector-100")).thenReturn(Optional.of(Achievement.builder().id("collector-100").build()));
        when(achievementRepository.findById("veteran")).thenReturn(Optional.of(Achievement.builder().id("veteran").build()));

        achievementUnlockService.checkAndUnlock(player, 1, 100, 100);

        verify(playerAchievementRepository, times(3)).save(playerAchievementCaptor.capture());
        assertEquals(3, playerAchievementCaptor.getAllValues().size());
    }

    @Test
    void checkDeckMaster_unlocksAtTen() {
        Player player = Player.builder().id(1L).username("ash").build();
        when(playerAchievementRepository.findById(new PlayerAchievementId(1L, "deck-master"))).thenReturn(Optional.empty());
        when(achievementRepository.findById("deck-master")).thenReturn(Optional.of(Achievement.builder().id("deck-master").build()));

        achievementUnlockService.checkDeckMaster(player, 10);

        verify(playerAchievementRepository).save(playerAchievementCaptor.capture());
        assertEquals(new PlayerAchievementId(1L, "deck-master"), playerAchievementCaptor.getValue().getId());
    }

    @Test
    void checkLucky_unlocksAlwaysWhenMissing() {
        Player player = Player.builder().id(1L).username("ash").build();
        when(playerAchievementRepository.findById(new PlayerAchievementId(1L, "lucky"))).thenReturn(Optional.empty());
        when(achievementRepository.findById("lucky")).thenReturn(Optional.of(Achievement.builder().id("lucky").build()));

        achievementUnlockService.checkLucky(player);

        verify(playerAchievementRepository).save(playerAchievementCaptor.capture());
        assertEquals(new PlayerAchievementId(1L, "lucky"), playerAchievementCaptor.getValue().getId());
    }

    @Test
    void checkAndUnlock_skipsAlreadyOwnedAchievements() {
        Player player = Player.builder().id(1L).username("ash").build();
        when(playerAchievementRepository.findById(new PlayerAchievementId(1L, "first-win")))
                .thenReturn(Optional.of(mock(PlayerAchievement.class)));
        when(playerAchievementRepository.findById(new PlayerAchievementId(1L, "collector-100")))
                .thenReturn(Optional.of(mock(PlayerAchievement.class)));
        when(playerAchievementRepository.findById(new PlayerAchievementId(1L, "veteran")))
                .thenReturn(Optional.of(mock(PlayerAchievement.class)));

        achievementUnlockService.checkAndUnlock(player, 1, 100, 100);

        verify(achievementRepository, never()).findById(anyString());
        verify(playerAchievementRepository, never()).save(any());
    }
}
