package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.request.UpdateProfileRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.AchievementResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.BadgeResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.CustomizationItemResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.PlayerResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.ProfileResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.SkinResponse;
import ar.edu.utn.frc.tup.piii.entities.*;
import ar.edu.utn.frc.tup.piii.exceptions.DuplicateResourceException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.*;
import ar.edu.utn.frc.tup.piii.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DefaultDeckProvisioningService provisioningService;
    private final PlayerStatsRepository playerStatsRepository;
    private final BadgeRepository badgeRepository;
    private final PlayerBadgeRepository playerBadgeRepository;
    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final TrainerSkinRepository trainerSkinRepository;
    private final PlayerSkinRepository playerSkinRepository;
    private final CustomizationItemRepository customizationItemRepository;
    private final PlayerCustomizationRepository playerCustomizationRepository;

    public PlayerService(PlayerRepository playerRepository, PasswordEncoder passwordEncoder,
                         JwtService jwtService, DefaultDeckProvisioningService provisioningService,
                         PlayerStatsRepository playerStatsRepository,
                         BadgeRepository badgeRepository,
                         PlayerBadgeRepository playerBadgeRepository,
                         AchievementRepository achievementRepository,
                         PlayerAchievementRepository playerAchievementRepository,
                         TrainerSkinRepository trainerSkinRepository,
                         PlayerSkinRepository playerSkinRepository,
                         CustomizationItemRepository customizationItemRepository,
                         PlayerCustomizationRepository playerCustomizationRepository) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.provisioningService = provisioningService;
        this.playerStatsRepository = playerStatsRepository;
        this.badgeRepository = badgeRepository;
        this.playerBadgeRepository = playerBadgeRepository;
        this.achievementRepository = achievementRepository;
        this.playerAchievementRepository = playerAchievementRepository;
        this.trainerSkinRepository = trainerSkinRepository;
        this.playerSkinRepository = playerSkinRepository;
        this.customizationItemRepository = customizationItemRepository;
        this.playerCustomizationRepository = playerCustomizationRepository;
    }

    @Transactional
    public PlayerResponse register(String username, String email, String password) {
        if (playerRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("El usuario ya existe.");
        }
        if (playerRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("El email ya existe.");
        }

        Player player = Player.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .createdAt(LocalDateTime.now())
                .build();

        player = playerRepository.save(player);

        provisioningService.provisionDefaults(player);
        initNewPlayerProfile(player);

        String token = jwtService.generateToken(player.getId(), player.getUsername());

        PlayerResponse response = new PlayerResponse();
        response.setId(player.getId());
        response.setUsername(player.getUsername());
        response.setToken(token);
        return response;
    }

    public PlayerResponse login(String username, String password) {
        Player player = playerRepository.findByUsername(username)
                .orElseGet(() -> playerRepository.findByEmail(username)
                        .orElseThrow(() -> new SecurityException("Usuario o contraseña incorrectos.")));

        if (!passwordEncoder.matches(password, player.getPasswordHash())) {
            throw new SecurityException("Usuario o contraseña incorrectos.");
        }

        String token = jwtService.generateToken(player.getId(), player.getUsername());

        PlayerResponse response = new PlayerResponse();
        response.setId(player.getId());
        response.setUsername(player.getUsername());
        response.setToken(token);
        return response;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));

        PlayerStats stats = playerStatsRepository.findByPlayerId(playerId).orElse(null);

        ProfileResponse response = new ProfileResponse();
        response.setId(player.getId());
        response.setUsername(player.getUsername());
        response.setEmail(player.getEmail());
        response.setCreatedAt(player.getCreatedAt());
        response.setDecksCount(player.getDecks() != null ? player.getDecks().size() : 0);
        response.setXpPercent(player.getXp() % 100);
        response.setBio(player.getBio());
        response.setLevel(player.getLevel());
        response.setFavoriteRegion(player.getFavoriteRegion());
        response.setFavoritePokemon(player.getFavoritePokemon());
        response.setTotalCards(player.getTotalCards());
        if (stats != null) {
            response.setWins(stats.getWins());
            response.setLosses(stats.getLosses());
            response.setStreak(stats.getStreak());
            response.setTournamentsWon(stats.getTournamentsWon());
            response.setPacksOpened(stats.getPacksOpened());
            response.setDecksCreated(stats.getDecksCreated());
        }
        return response;
    }

    @Transactional
    public ProfileResponse updateProfile(Long playerId, UpdateProfileRequest request) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));

        if (request.getBio() != null) {
            player.setBio(request.getBio());
        }
        if (request.getFavoriteRegion() != null) {
            player.setFavoriteRegion(request.getFavoriteRegion());
        }
        if (request.getFavoritePokemon() != null) {
            player.setFavoritePokemon(request.getFavoritePokemon());
        }

        playerRepository.save(player);
        return getProfile(playerId);
    }

    @Transactional
    public void equipSkin(Long playerId, String skinId) {
        PlayerSkinId id = new PlayerSkinId(playerId, skinId);
        PlayerSkin playerSkin = playerSkinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Player " + playerId + " does not own skin " + skinId));

        List<PlayerSkin> owned = playerSkinRepository.findByPlayerId(playerId);
        for (PlayerSkin ps : owned) {
            ps.setEquipped(false);
        }
        playerSkinRepository.saveAll(owned);

        playerSkin.setEquipped(true);
        playerSkinRepository.save(playerSkin);
    }

    @Transactional(readOnly = true)
    public List<BadgeResponse> getBadges(Long playerId) {
        List<Badge> allBadges = badgeRepository.findAll();
        Set<String> unlockedIds = playerBadgeRepository.findByPlayerId(playerId)
                .stream()
                .map(pb -> pb.getBadge().getId())
                .collect(Collectors.toSet());

        return allBadges.stream().map(badge -> {
            BadgeResponse response = new BadgeResponse();
            response.setId(badge.getId());
            response.setLabel(badge.getLabel());
            response.setIcon(badge.getIcon());
            response.setUnlocked(unlockedIds.contains(badge.getId()));
            response.setDescription(badge.getDescription());
            response.setHowToUnlock(badge.getHowToUnlock());
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AchievementResponse> getAchievements(Long playerId) {
        List<Achievement> all = achievementRepository.findAll();
        Set<String> unlockedIds = playerAchievementRepository.findByPlayerId(playerId)
                .stream()
                .map(pa -> pa.getAchievement().getId())
                .collect(Collectors.toSet());

        return all.stream().map(a -> {
            AchievementResponse response = new AchievementResponse();
            response.setId(a.getId());
            response.setName(a.getName());
            response.setDescription(a.getDescription());
            response.setIcon(a.getIcon());
            response.setUnlocked(unlockedIds.contains(a.getId()));
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SkinResponse> getSkins(Long playerId) {
        List<TrainerSkin> all = trainerSkinRepository.findAll();
        String equippedSkinId = playerSkinRepository.findByPlayerIdAndEquippedTrue(playerId)
                .map(ps -> ps.getSkin().getId())
                .orElse(null);

        return all.stream().map(skin -> {
            SkinResponse response = new SkinResponse();
            response.setId(skin.getId());
            response.setName(skin.getName());
            response.setHatColor(skin.getHatColor());
            response.setShirtColor(skin.getShirtColor());
            response.setPantsColor(skin.getPantsColor());
            response.setSkinTone(skin.getSkinTone());
            response.setEquipped(skin.getId().equals(equippedSkinId));
            response.setCharacter(skin.getCharacter() != null ? skin.getCharacter() : "ash");
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomizationItemResponse> getCustomizationItems(Long playerId) {
        List<CustomizationItem> all = customizationItemRepository.findAll();
        Set<String> unlockedIds = playerCustomizationRepository.findByPlayerId(playerId)
                .stream()
                .map(pc -> pc.getItem().getId())
                .collect(Collectors.toSet());

        return all.stream().map(item -> {
            CustomizationItemResponse response = new CustomizationItemResponse();
            response.setId(item.getId());
            response.setName(item.getName());
            response.setCategory(item.getCategory());
            response.setUnlocked(unlockedIds.contains(item.getId()));
            response.setColor(item.getColor());
            return response;
        }).collect(Collectors.toList());
    }

    private void initNewPlayerProfile(Player player) {
        Long pid = player.getId();

        PlayerStats stats = PlayerStats.builder()
                .player(player)
                .wins(0)
                .losses(0)
                .streak(0)
                .tournamentsWon(0)
                .packsOpened(0)
                .totalCards(0)
                .decksCreated(0)
                .build();
        playerStatsRepository.save(stats);

        List<String> allSkinIds = List.of("default", "fire", "water", "electric", "dark", "brock", "misty");
        List<PlayerSkin> skins = new ArrayList<>();
        for (String skinId : allSkinIds) {
            skins.add(PlayerSkin.builder()
                    .id(new PlayerSkinId(pid, skinId))
                    .player(player)
                    .skin(trainerSkinRepository.findById(skinId)
                            .orElseThrow(() -> new ResourceNotFoundException("Skin not found: " + skinId)))
                    .equipped(skinId.equals("default"))
                    .build());
        }
        playerSkinRepository.saveAll(skins);

        List<String> baseItems = List.of(
                "shirt-blue", "shirt-red", "hat-cap",
                "glasses", "pose-1", "pose-2", "bg-stadium");
        List<PlayerCustomization> customizations = new ArrayList<>();
        for (String itemId : baseItems) {
            customizations.add(PlayerCustomization.builder()
                    .id(new PlayerCustomizationId(pid, itemId))
                    .player(player)
                    .item(customizationItemRepository.findById(itemId)
                            .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId)))
                    .build());
        }
        playerCustomizationRepository.saveAll(customizations);
    }
}
