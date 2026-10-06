# Pasos 8–14: Backend perfil de entrenador

> Instrucciones para completar el subsistema de perfil.
> **Prerequisito**: tener los pasos 1–7 (commiteados y pusheados a Producción).

---

## Paso 8: `equipSkin()` en `PlayerService`

Agregar este método en `PlayerService.java`. Debe ir después de `updateProfile` y antes del cierre de la clase.

```java
@Transactional
public void equipSkin(Long playerId, String skinId) {
    PlayerSkinId id = new PlayerSkinId(playerId, skinId);
    PlayerSkin playerSkin = playerSkinRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Player " + playerId + " does not own skin " + skinId));

    // De-equip all other skins for this player
    List<PlayerSkin> owned = playerSkinRepository.findByPlayerId(playerId);
    for (PlayerSkin ps : owned) {
        ps.setEquipped(false);
    }
    playerSkinRepository.saveAll(owned);

    // Equip the selected one
    playerSkin.setEquipped(true);
    playerSkinRepository.save(playerSkin);
}
```

**Imports necesarios** (arriba con los demás):
```java
import ar.edu.utn.frc.tup.piii.entities.PlayerSkinId;
```

---

## Paso 9: `initNewPlayerProfile()` + hook en `register()`

### 9a. Crear el método en `PlayerService`:

```java
private void initNewPlayerProfile(Player player) {
    Long pid = player.getId();

    // 1. Create empty stats row
    PlayerStats stats = PlayerStats.builder()
            .player(player)
            .wins(0).losses(0).streak(0)
            .tournamentsWon(0).packsOpened(0)
            .totalCards(0).decksCreated(0)
            .build();
    playerStatsRepository.save(stats);

    // 2. Give the player all skins + equip default
    List<String> allSkinIds = List.of("default", "fire", "water", "electric", "dark");
    for (String skinId : allSkinIds) {
        PlayerSkin ps = PlayerSkin.builder()
                .id(new PlayerSkinId(pid, skinId))
                .player(player)
                .skin(trainerSkinRepository.findById(skinId)
                        .orElseThrow(() -> new ResourceNotFoundException("Skin not found: " + skinId)))
                .equipped(skinId.equals("default"))
                .build();
        playerSkinRepository.save(ps);
    }

    // 3. Unlock base customization items
    List<String> baseItems = List.of(
            "shirt-blue", "shirt-red", "hat-cap",
            "glasses", "pose-1", "pose-2", "bg-stadium");
    for (String itemId : baseItems) {
        PlayerCustomization pc = PlayerCustomization.builder()
                .id(new PlayerCustomizationId(pid, itemId))
                .player(player)
                .item(customizationItemRepository.findById(itemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId)))
                .build();
        playerCustomizationRepository.save(pc);
    }
}
```

**Imports** que faltan:
```java
import ar.edu.utn.frc.tup.piii.entities.PlayerCustomizationId;
import ar.edu.utn.frc.tup.piii.entities.PlayerSkinId;
```

### 9b. Hook en `register()` — agregar UNA línea después de `provisioningService.provisionDefaults(player);`

En `PlayerService.register()`, línea 85 (aproximadamente), **después** de `provisioningService.provisionDefaults(player);`, agregar:

```java
        player = playerRepository.save(player);

        provisioningService.provisionDefaults(player);

        // ====== AGREGAR ESTA LÍNEA ======
        initNewPlayerProfile(player);
        // ================================

        String token = jwtService.generateToken(player.getId(), player.getUsername());
```

---

## Paso 10: `PlayerController`

Crear `BE/src/main/java/ar/edu/utn/frc/tup/piii/controllers/PlayerController.java`:

```java
package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.request.EquipSkinRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.UpdateProfileRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.AchievementResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.BadgeResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.CustomizationItemResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.ProfileResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.SkinResponse;
import ar.edu.utn.frc.tup.piii.security.CurrentPlayerService;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;
    private final CurrentPlayerService currentPlayerService;

    public PlayerController(PlayerService playerService, CurrentPlayerService currentPlayerService) {
        this.playerService = playerService;
        this.currentPlayerService = currentPlayerService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile() {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.getProfile(playerId));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(
            @RequestBody @Valid UpdateProfileRequest request) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.updateProfile(playerId, request));
    }

    @GetMapping("/me/badges")
    public ResponseEntity<List<BadgeResponse>> getBadges() {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.getBadges(playerId));
    }

    @GetMapping("/me/achievements")
    public ResponseEntity<List<AchievementResponse>> getAchievements() {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.getAchievements(playerId));
    }

    @GetMapping("/me/skins")
    public ResponseEntity<List<SkinResponse>> getSkins() {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.getSkins(playerId));
    }

    @PutMapping("/me/skin")
    public ResponseEntity<Void> equipSkin(@RequestBody @Valid EquipSkinRequest request) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        playerService.equipSkin(playerId, request.getSkinId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/customization")
    public ResponseEntity<List<CustomizationItemResponse>> getCustomizationItems() {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.getCustomizationItems(playerId));
    }
}
```

---

## Paso 11: Migración V009 — seed data

Crear `BE/src/main/resources/db/migration/V009__seed_profile_catalogs.sql`:

```sql
-- =============================================================
-- V009__seed_profile_catalogs.sql
-- Seeds badge, achievement, trainer_skin, customization_item tables
-- PostgreSQL 15+ | Flyway migration V009
-- =============================================================

-- =============================================================
-- Badges
-- =============================================================

INSERT INTO badge (id, label, description, icon, how_to_unlock) VALUES
('fire',    'Insignia Llama',    'Otorgada por el líder del Gimnasio Fuego',    '🔥', 'Gana una partida con un mazo de tipo Fuego'),
('water',   'Insignia Cascada',  'Otorgada por el líder del Gimnasio Agua',     '💧', 'Gana una partida con un mazo de tipo Agua'),
('grass',   'Insignia Bosque',   'Otorgada por el líder del Gimnasio Planta',   '🌿', 'Gana una partida con un mazo de tipo Planta'),
('electric','Insignia Trueno',   'Otorgada por el líder del Gimnasio Eléctrico','⚡',  'Gana una partida con un mazo de tipo Eléctrico'),
('psychic', 'Insignia Mente',    'Otorgada por el líder del Gimnasio Psíquico', '🔮', 'Gana una partida con un mazo de tipo Psíquico'),
('fighting','Insignia Lucha',    'Otorgada por el líder del Gimnasio Lucha',    '🥊', 'Gana una partida con un mazo de tipo Lucha'),
('dark',    'Insignia Oscuridad','Otorgada por el líder del Gimnasio Siniestro', '🌑', 'Gana una partida con un mazo de tipo Siniestro'),
('dragon',  'Insignia Dragón',   'Otorgada por el líder del Gimnasio Dragón',   '🐉', 'Gana 10 partidas consecutivas')
ON CONFLICT (id) DO NOTHING;

-- =============================================================
-- Achievements
-- =============================================================

INSERT INTO achievement (id, name, description, icon) VALUES
('first-win',     'Campeón Novato',     'Obtén tu primera victoria.',       '🏆'),
('collector-100', 'Coleccionista de Cartas', 'Consigue 100 cartas.',        '📚'),
('deck-master',   'Maestro de Mazos',   'Crea 10 mazos.',                  '🃏'),
('veteran',       'Veterano Pokémon',   'Juega 100 partidas.',              '⭐'),
('lucky',         'Golpe de Suerte',    'Gana una partida con una carta decisiva.', '🍀')
ON CONFLICT (id) DO NOTHING;

-- =============================================================
-- Trainer skins
-- =============================================================

INSERT INTO trainer_skin (id, name, hat_color, shirt_color, pants_color, skin_tone) VALUES
('default', 'Ash',      '#e74c3c', '#2980b9', '#2c3e50', '#f5d0a9'),
('fire',    'Fuego',    '#e74c3c', '#c0392b', '#8e44ad', '#f5d0a9'),
('water',   'Agua',     '#3498db', '#2980b9', '#1a5276', '#f5d0a9'),
('electric','Eléctrico','#f1c40f', '#2c3e50', '#7f8c8d', '#f5d0a9'),
('dark',    'Oscuro',   '#2c3e50', '#1a1a2e', '#16213e', '#d4a574')
ON CONFLICT (id) DO NOTHING;

-- =============================================================
-- Customization items
-- =============================================================

INSERT INTO customization_item (id, name, category, color) VALUES
('shirt-blue',    'Camisa Azul',      'clothes',    '#105189'),
('shirt-red',     'Camisa Roja',      'clothes',    '#c0392b'),
('shirt-black',   'Camisa Negra',     'clothes',    '#2c3e50'),
('hat-cap',       'Gorra Clásica',    'accessory',  '#DE940E'),
('hat-beanie',    'Gorro Invernal',   'accessory',  '#3498db'),
('glasses',       'Gafas Oscuras',    'accessory',  NULL),
('pose-1',        'Firme',            'pose',       NULL),
('pose-2',        'Con Puño',         'pose',       NULL),
('pose-3',        'Saludando',        'pose',       NULL),
('bg-stadium',    'Estadio',          'background', NULL),
('bg-beach',      'Playa',            'background', NULL),
('bg-mountain',   'Montaña',          'background', NULL)
ON CONFLICT (id) DO NOTHING;
```

---

## Paso 12: `BadgeUnlockService`

Crear `BE/src/main/java/ar/edu/utn/frc/tup/piii/services/BadgeUnlockService.java`:

```java
package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.entities.*;
import ar.edu.utn.frc.tup.piii.repositories.BadgeRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerBadgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Evaluates badge unlock conditions after a match finishes.
 * Each badge has a specific condition checked here.
 */
@Service
public class BadgeUnlockService {

    private static final Logger log = LoggerFactory.getLogger(BadgeUnlockService.class);

    private final BadgeRepository badgeRepository;
    private final PlayerBadgeRepository playerBadgeRepository;

    public BadgeUnlockService(BadgeRepository badgeRepository,
                              PlayerBadgeRepository playerBadgeRepository) {
        this.badgeRepository = badgeRepository;
        this.playerBadgeRepository = playerBadgeRepository;
    }

    public void checkAndUnlock(Player player, int winStreak) {
        // Dragon badge: 10 consecutive wins
        if (winStreak >= 10) {
            unlockIfMissing(player, "dragon");
        }

        // Future badges based on deck type can be added here.
        // For now, type-based badges (fire, water, etc.) require tracking
        // the deck's primary type in GameSession — pending feature.
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
```

---

## Paso 13: `AchievementUnlockService`

Crear `BE/src/main/java/ar/edu/utn/frc/tup/piii/services/AchievementUnlockService.java`:

```java
package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.entities.*;
import ar.edu.utn.frc.tup.piii.repositories.AchievementRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerAchievementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Evaluates achievement unlock conditions after relevant game events.
 */
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
```

---

## Paso 14: Hook en `GameService` post-partida

### 14a. Inyectar dependencias

En `GameService.java`, agregar al constructor:

```java
private final PlayerStatsRepository playerStatsRepository;
private final BadgeUnlockService badgeUnlockService;
private final AchievementUnlockService achievementUnlockService;
```

Y en el constructor (después de `objectMapper`):

```java
                        PlayerStatsRepository playerStatsRepository,
                        BadgeUnlockService badgeUnlockService,
                        AchievementUnlockService achievementUnlockService) {
    // ... existing fields ...
    this.playerStatsRepository = playerStatsRepository;
    this.badgeUnlockService = badgeUnlockService;
    this.achievementUnlockService = achievementUnlockService;
}
```

**Imports**:
```java
import ar.edu.utn.frc.tup.piii.repositories.PlayerStatsRepository;
```

### 14b. Agregar el hook

En el método `performGameAction`, **reemplazar** las líneas 281–287:

**Antes** (≈ líneas 281–287):
```java
            // 10. If game finished → update session
            if (engineResult.isGameFinished()) {
                session.setStatus(GameStatus.FINISHED);
                session.setFinishedAt(LocalDateTime.now());
                session.setWinner(playerRepository.findById(engineResult.getWinnerPlayerId()).orElse(null));
                gameSessionRepository.save(session);
            }
```

**Después**:
```java
            // 10. If game finished → update session + profile stats/badges/achievements
            if (engineResult.isGameFinished()) {
                session.setStatus(GameStatus.FINISHED);
                session.setFinishedAt(LocalDateTime.now());
                Player winner = playerRepository.findById(engineResult.getWinnerPlayerId()).orElse(null);
                session.setWinner(winner);
                gameSessionRepository.save(session);

                // Update stats for both players
                if (winner != null) {
                    Player loser = session.getPlayer1().getId().equals(winner.getId())
                            ? session.getPlayer2()
                            : session.getPlayer1();

                    updatePlayerStats(winner, true, loser);
                }
            }
```

### 14c. Agregar el método helper `updatePlayerStats`

En `GameService.java`, agregar este método privado:

```java
private void updatePlayerStats(Player winner, boolean isWinner, Player loser) {
    PlayerStats winnerStats = playerStatsRepository.findByPlayerId(winner.getId())
            .orElseGet(() -> PlayerStats.builder().player(winner).build());
    winnerStats.setWins(winnerStats.getWins() + 1);
    winnerStats.setStreak(winnerStats.getStreak() + 1);
    playerStatsRepository.save(winnerStats);

    int totalGames = winnerStats.getWins();
    if (loser != null) {
        PlayerStats loserStats = playerStatsRepository.findByPlayerId(loser.getId())
                .orElseGet(() -> PlayerStats.builder().player(loser).build());
        loserStats.setLosses(loserStats.getLosses() + 1);
        loserStats.setStreak(0);
        playerStatsRepository.save(loserStats);
        totalGames += loserStats.getLosses();
    }

    badgeUnlockService.checkAndUnlock(winner, winnerStats.getStreak());
    achievementUnlockService.checkAndUnlock(winner,
            winnerStats.getWins(), totalGames, winner.getTotalCards());
}
```

*(Si `loserStats()` es demasiado rebuscado, se puede reemplazar por `orElseGet(() -> PlayerStats.builder().player(loser).build()` directamente en la línea de `loserStats.getLosses()`).)*

---

## Verificación final

```bash
cd BE && ./mvnw.cmd compile -q
cd BE && ./mvnw.cmd test -q
```

Ambos deben dar output vacío (sin errores).

---

## Resumen de archivos a crear/modificar

| Paso | Archivo | Acción |
|---|---|---|
| 8 | `services/PlayerService.java` | Agregar método `equipSkin()` |
| 9 | `services/PlayerService.java` | Agregar `initNewPlayerProfile()` + línea en `register()` |
| 10 | `controllers/PlayerController.java` | **Crear** — 7 endpoints |
| 11 | `db/migration/V009__seed_profile_catalogs.sql` | **Crear** — inserts de catálogo |
| 12 | `services/BadgeUnlockService.java` | **Crear** |
| 13 | `services/AchievementUnlockService.java` | **Crear** |
| 14 | `services/GameService.java` | Inyectar PlayerStatsRepository + unlock services + hook post-partida |
