package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.entities.Badge;
import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.entities.PlayerBadge;
import ar.edu.utn.frc.tup.piii.entities.PlayerBadgeId;
import ar.edu.utn.frc.tup.piii.repositories.BadgeRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerBadgeRepository;
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
class BadgeUnlockServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private PlayerBadgeRepository playerBadgeRepository;

    @Captor
    private ArgumentCaptor<PlayerBadge> playerBadgeCaptor;

    private BadgeUnlockService badgeUnlockService;

    @BeforeEach
    void setUp() {
        badgeUnlockService = new BadgeUnlockService(badgeRepository, playerBadgeRepository);
    }

    @Test
    void checkAndUnlock_belowThreshold_doesNothing() {
        badgeUnlockService.checkAndUnlock(Player.builder().id(1L).username("ash").build(), 9);

        verifyNoInteractions(badgeRepository, playerBadgeRepository);
    }

    @Test
    void checkAndUnlock_unlocksMissingDragonBadge() {
        Player player = Player.builder().id(1L).username("ash").build();
        Badge badge = Badge.builder().id("dragon").label("Dragon").build();

        when(playerBadgeRepository.findById(new PlayerBadgeId(1L, "dragon"))).thenReturn(Optional.empty());
        when(badgeRepository.findById("dragon")).thenReturn(Optional.of(badge));

        badgeUnlockService.checkAndUnlock(player, 10);

        verify(playerBadgeRepository).save(playerBadgeCaptor.capture());
        PlayerBadge saved = playerBadgeCaptor.getValue();
        assertEquals(new PlayerBadgeId(1L, "dragon"), saved.getId());
        assertEquals(player, saved.getPlayer());
        assertEquals(badge, saved.getBadge());
    }

    @Test
    void checkAndUnlock_skipsWhenAlreadyOwned() {
        Player player = Player.builder().id(1L).username("ash").build();

        when(playerBadgeRepository.findById(new PlayerBadgeId(1L, "dragon")))
                .thenReturn(Optional.of(mock(PlayerBadge.class)));

        badgeUnlockService.checkAndUnlock(player, 10);

        verifyNoMoreInteractions(badgeRepository, playerBadgeRepository);
    }

    @Test
    void checkAndUnlock_skipsWhenBadgeCatalogMissing() {
        Player player = Player.builder().id(1L).username("ash").build();

        when(playerBadgeRepository.findById(new PlayerBadgeId(1L, "dragon"))).thenReturn(Optional.empty());
        when(badgeRepository.findById("dragon")).thenReturn(Optional.empty());

        badgeUnlockService.checkAndUnlock(player, 10);

        verify(playerBadgeRepository, never()).save(any());
    }
}
