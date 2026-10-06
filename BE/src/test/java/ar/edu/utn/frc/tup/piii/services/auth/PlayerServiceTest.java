package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.dtos.response.PlayerResponse;
import ar.edu.utn.frc.tup.piii.entities.Achievement;
import ar.edu.utn.frc.tup.piii.entities.Badge;
import ar.edu.utn.frc.tup.piii.entities.CustomizationItem;
import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.entities.PlayerAchievement;
import ar.edu.utn.frc.tup.piii.entities.PlayerAchievementId;
import ar.edu.utn.frc.tup.piii.entities.PlayerBadge;
import ar.edu.utn.frc.tup.piii.entities.PlayerBadgeId;
import ar.edu.utn.frc.tup.piii.entities.PlayerCustomization;
import ar.edu.utn.frc.tup.piii.entities.PlayerCustomizationId;
import ar.edu.utn.frc.tup.piii.entities.PlayerStats;
import ar.edu.utn.frc.tup.piii.entities.PlayerSkin;
import ar.edu.utn.frc.tup.piii.entities.PlayerSkinId;
import ar.edu.utn.frc.tup.piii.entities.TrainerSkin;
import ar.edu.utn.frc.tup.piii.exceptions.DuplicateResourceException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerStatsRepository;
import ar.edu.utn.frc.tup.piii.repositories.BadgeRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerBadgeRepository;
import ar.edu.utn.frc.tup.piii.repositories.AchievementRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerAchievementRepository;
import ar.edu.utn.frc.tup.piii.repositories.TrainerSkinRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerSkinRepository;
import ar.edu.utn.frc.tup.piii.repositories.CustomizationItemRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCustomizationRepository;
import ar.edu.utn.frc.tup.piii.dtos.request.UpdateProfileRequest;
import ar.edu.utn.frc.tup.piii.security.JwtService;
import ar.edu.utn.frc.tup.piii.services.DefaultDeckProvisioningService;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private DefaultDeckProvisioningService provisioningService;

    @Mock
    private PlayerStatsRepository playerStatsRepository;
    @Mock
    private BadgeRepository badgeRepository;
    @Mock
    private PlayerBadgeRepository playerBadgeRepository;
    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private PlayerAchievementRepository playerAchievementRepository;
    @Mock
    private TrainerSkinRepository trainerSkinRepository;
    @Mock
    private PlayerSkinRepository playerSkinRepository;
    @Mock
    private CustomizationItemRepository customizationItemRepository;
    @Mock
    private PlayerCustomizationRepository playerCustomizationRepository;

    private PasswordEncoder passwordEncoder;
    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        playerService = new PlayerService(playerRepository, passwordEncoder, jwtService, provisioningService,
                playerStatsRepository, badgeRepository, playerBadgeRepository, achievementRepository,
                playerAchievementRepository, trainerSkinRepository, playerSkinRepository,
                customizationItemRepository, playerCustomizationRepository);
    }

    /**
     * Stubs the profile catalog lookups (skins + customization items) that
     * {@code register()} resolves inline via {@code initNewPlayerProfile}. Only the
     * register happy-path tests reach this code, so the stubs live per-test (strict
     * Mockito would flag them as unnecessary on the duplicate/login paths).
     */
    private void stubProfileCatalogs() {
        when(trainerSkinRepository.findById(anyString())).thenReturn(Optional.of(mock(TrainerSkin.class)));
        when(customizationItemRepository.findById(anyString())).thenReturn(Optional.of(mock(CustomizationItem.class)));
    }

    @Test
    void registerSuccess() {
        when(playerRepository.existsByUsername("testuser")).thenReturn(false);
        when(playerRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> {
            Player p = i.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(jwtService.generateToken(any(), anyString())).thenReturn("jwt-token");
        stubProfileCatalogs();

        PlayerResponse response = playerService.register("testuser", "test@test.com", "password123");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void registerDuplicateUsername() {
        when(playerRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> playerService.register("testuser", "test@test.com", "password123"));
    }

    @Test
    void registerDuplicateEmail() {
        when(playerRepository.existsByUsername("testuser")).thenReturn(false);
        when(playerRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> playerService.register("testuser", "test@test.com", "password123"));
    }

    @Test
    void passwordIsHashedOnRegister() {
        when(playerRepository.existsByUsername("testuser")).thenReturn(false);
        when(playerRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any(), anyString())).thenReturn("token");
        stubProfileCatalogs();

        playerService.register("testuser", "test@test.com", "plaintext123");

        verify(playerRepository).save(argThat(p ->
                p.getPasswordHash() != null &&
                !p.getPasswordHash().equals("plaintext123") &&
                p.getPasswordHash().startsWith("$2a$")
        ));
    }

    @Test
    void registerCallsProvisioning() {
        when(playerRepository.existsByUsername("testuser")).thenReturn(false);
        when(playerRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> {
            Player p = i.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(jwtService.generateToken(any(), anyString())).thenReturn("jwt-token");
        doNothing().when(provisioningService).provisionDefaults(any(Player.class));
        stubProfileCatalogs();

        PlayerResponse response = playerService.register("testuser", "test@test.com", "password123");

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        verify(provisioningService).provisionDefaults(argThat(p ->
                p.getId().equals(1L) && "testuser".equals(p.getUsername())
        ));
    }

    @Test
    void registerDoesNotCallProvisioningOnDuplicateUsername() {
        when(playerRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> playerService.register("testuser", "test@test.com", "password123"));
        verifyNoInteractions(provisioningService);
    }

    @Test
    void registerDoesNotCallProvisioningOnDuplicateEmail() {
        when(playerRepository.existsByUsername("testuser")).thenReturn(false);
        when(playerRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> playerService.register("testuser", "test@test.com", "password123"));
        verifyNoInteractions(provisioningService);
    }

    @Test
    void provisioningFailurePropagatesAndTokenNotGenerated() {
        when(playerRepository.existsByUsername("testuser")).thenReturn(false);
        when(playerRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> {
            Player p = i.getArgument(0);
            p.setId(1L);
            return p;
        });
        doThrow(new RuntimeException("Provisioning failed"))
                .when(provisioningService).provisionDefaults(any(Player.class));

        assertThrows(RuntimeException.class,
                () -> playerService.register("testuser", "test@test.com", "password123"));

        verify(jwtService, never()).generateToken(any(), anyString());
    }

    @Test
    void loginSuccess() {
        String hashedPassword = passwordEncoder.encode("password123");
        Player player = Player.builder()
                .id(1L)
                .username("testuser")
                .passwordHash(hashedPassword)
                .build();

        when(playerRepository.findByUsername("testuser")).thenReturn(Optional.of(player));
        when(jwtService.generateToken(1L, "testuser")).thenReturn("jwt-token");

        PlayerResponse response = playerService.login("testuser", "password123");

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void loginWrongPassword() {
        String hashedPassword = passwordEncoder.encode("correctpassword");
        Player player = Player.builder()
                .id(1L)
                .username("testuser")
                .passwordHash(hashedPassword)
                .build();

        when(playerRepository.findByUsername("testuser")).thenReturn(Optional.of(player));

        assertThrows(SecurityException.class,
                () -> playerService.login("testuser", "wrongpassword"));
    }

    @Test
    void loginUserNotFound() {
        when(playerRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(SecurityException.class,
                () -> playerService.login("unknown", "password123"));
    }

    @Test
    void getProfile_returnsMappedStatsAndDeckCount() {
        Player player = Player.builder()
                .id(1L)
                .username("ash")
                .email("ash@example.com")
                .createdAt(LocalDateTime.of(2026, 6, 24, 18, 0, 0))
                .xp(125)
                .level(3)
                .bio("bio")
                .favoriteRegion("Kanto")
                .favoritePokemon("Pikachu")
                .totalCards(300)
                .decks(List.of(mock(ar.edu.utn.frc.tup.piii.entities.Deck.class), mock(ar.edu.utn.frc.tup.piii.entities.Deck.class)))
                .build();
        PlayerStats stats = PlayerStats.builder()
                .player(player)
                .wins(7)
                .losses(2)
                .streak(4)
                .tournamentsWon(1)
                .packsOpened(9)
                .decksCreated(5)
                .totalCards(300)
                .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerStatsRepository.findByPlayerId(1L)).thenReturn(Optional.of(stats));

        var response = playerService.getProfile(1L);

        assertEquals(1L, response.getId());
        assertEquals(2, response.getDecksCount());
        assertEquals(25, response.getXpPercent());
        assertEquals(7, response.getWins());
        assertEquals(2, response.getLosses());
        assertEquals(4, response.getStreak());
    }

    @Test
    void getProfile_whenMissing_throwsNotFound() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> playerService.getProfile(99L));
    }

    @Test
    void updateProfile_updatesOnlyProvidedFields() {
        Player player = Player.builder()
                .id(1L)
                .username("ash")
                .email("ash@example.com")
                .createdAt(LocalDateTime.of(2026, 6, 24, 18, 0, 0))
                .bio("old bio")
                .favoriteRegion("Kanto")
                .favoritePokemon("Pikachu")
                .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerStatsRepository.findByPlayerId(1L)).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setBio("new bio");

        var response = playerService.updateProfile(1L, request);

        assertEquals("new bio", player.getBio());
        assertEquals("Kanto", player.getFavoriteRegion());
        assertEquals("Pikachu", player.getFavoritePokemon());
        assertEquals("new bio", response.getBio());
        verify(playerRepository).save(player);
    }

    @Test
    void getBadges_marksUnlockedAndLocked() {
        Badge unlocked = Badge.builder().id("first-badge").label("First Badge").description("d").icon("i").howToUnlock("h").build();
        Badge locked = Badge.builder().id("second-badge").label("Second Badge").description("d2").icon("i2").howToUnlock("h2").build();
        PlayerBadge playerBadge = PlayerBadge.builder()
                .id(new PlayerBadgeId(1L, "first-badge"))
                .badge(unlocked)
                .build();

        when(badgeRepository.findAll()).thenReturn(List.of(unlocked, locked));
        when(playerBadgeRepository.findByPlayerId(1L)).thenReturn(List.of(playerBadge));

        var response = playerService.getBadges(1L);

        assertEquals(2, response.size());
        assertTrue(response.stream().anyMatch(b -> "first-badge".equals(b.getId()) && b.isUnlocked()));
        assertTrue(response.stream().anyMatch(b -> "second-badge".equals(b.getId()) && !b.isUnlocked()));
    }

    @Test
    void getAchievements_marksUnlocked() {
        Achievement unlocked = Achievement.builder().id("first-win").name("First Win").description("d").icon("i").build();
        Achievement locked = Achievement.builder().id("veteran").name("Veteran").description("d2").icon("i2").build();
        PlayerAchievement playerAchievement = PlayerAchievement.builder()
                .id(new PlayerAchievementId(1L, "first-win"))
                .achievement(unlocked)
                .build();

        when(achievementRepository.findAll()).thenReturn(List.of(unlocked, locked));
        when(playerAchievementRepository.findByPlayerId(1L)).thenReturn(List.of(playerAchievement));

        var response = playerService.getAchievements(1L);

        assertEquals(2, response.size());
        assertTrue(response.stream().anyMatch(a -> "first-win".equals(a.getId()) && a.isUnlocked()));
        assertTrue(response.stream().anyMatch(a -> "veteran".equals(a.getId()) && !a.isUnlocked()));
    }

    @Test
    void getSkins_marksEquippedAndDefaultsCharacter() {
        TrainerSkin defaultSkin = TrainerSkin.builder().id("default").name("Default").build();
        TrainerSkin fireSkin = TrainerSkin.builder().id("fire").name("Fire").character("blaze").build();
        PlayerSkin equipped = PlayerSkin.builder()
                .id(new PlayerSkinId(1L, "fire"))
                .skin(fireSkin)
                .equipped(true)
                .build();

        when(trainerSkinRepository.findAll()).thenReturn(List.of(defaultSkin, fireSkin));
        when(playerSkinRepository.findByPlayerIdAndEquippedTrue(1L)).thenReturn(Optional.of(equipped));

        var response = playerService.getSkins(1L);

        assertEquals(2, response.size());
        assertTrue(response.stream().anyMatch(s -> "fire".equals(s.getId()) && s.isEquipped() && "blaze".equals(s.getCharacter())));
        assertTrue(response.stream().anyMatch(s -> "default".equals(s.getId()) && !s.isEquipped() && "ash".equals(s.getCharacter())));
    }

    @Test
    void getCustomizationItems_marksUnlocked() {
        CustomizationItem unlocked = CustomizationItem.builder().id("hat-01").name("Cap").category("hat").color("blue").build();
        CustomizationItem locked = CustomizationItem.builder().id("pose-1").name("Pose").category("pose").color("gray").build();
        PlayerCustomization playerCustomization = PlayerCustomization.builder()
                .id(new PlayerCustomizationId(1L, "hat-01"))
                .item(unlocked)
                .build();

        when(customizationItemRepository.findAll()).thenReturn(List.of(unlocked, locked));
        when(playerCustomizationRepository.findByPlayerId(1L)).thenReturn(List.of(playerCustomization));

        var response = playerService.getCustomizationItems(1L);

        assertEquals(2, response.size());
        assertTrue(response.stream().anyMatch(i -> "hat-01".equals(i.getId()) && i.isUnlocked()));
        assertTrue(response.stream().anyMatch(i -> "pose-1".equals(i.getId()) && !i.isUnlocked()));
    }

    @Test
    void equipSkin_replacesCurrentEquippedSkin() {
        Player player = Player.builder().id(1L).username("ash").build();
        TrainerSkin defaultSkin = TrainerSkin.builder().id("default").name("Default").build();
        TrainerSkin fireSkin = TrainerSkin.builder().id("fire").name("Fire").build();
        PlayerSkin equippedDefault = PlayerSkin.builder()
                .id(new PlayerSkinId(1L, "default"))
                .player(player)
                .skin(defaultSkin)
                .equipped(true)
                .build();
        PlayerSkin targetSkin = PlayerSkin.builder()
                .id(new PlayerSkinId(1L, "fire"))
                .player(player)
                .skin(fireSkin)
                .equipped(false)
                .build();

        when(playerSkinRepository.findById(new PlayerSkinId(1L, "fire"))).thenReturn(Optional.of(targetSkin));
        when(playerSkinRepository.findByPlayerId(1L)).thenReturn(List.of(equippedDefault, targetSkin));

        playerService.equipSkin(1L, "fire");

        assertFalse(equippedDefault.isEquipped());
        assertTrue(targetSkin.isEquipped());
        verify(playerSkinRepository).saveAll(any());
        verify(playerSkinRepository).save(targetSkin);
    }

    @Test
    void equipSkin_whenNotOwned_throwsNotFound() {
        when(playerSkinRepository.findById(new PlayerSkinId(1L, "missing"))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> playerService.equipSkin(1L, "missing"));
    }
}
