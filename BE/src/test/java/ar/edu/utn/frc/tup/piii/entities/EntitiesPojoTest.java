package ar.edu.utn.frc.tup.piii.entities;

import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntitiesPojoTest {

    // ------------------------------------------------------------------ Player
    @Test
    void player_noArgConstructor() {
        Player p = new Player();
        assertNull(p.getId());
        assertNull(p.getUsername());
    }

    @Test
    void player_allArgsConstructorAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        Player p = new Player(1L, "ash", "ash@test.com", "hash", now,
                "bio", 5, 100, "Kanto", "Pikachu", 10,
                new ArrayList<>(), null, new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        assertEquals(1L, p.getId());
        assertEquals("ash", p.getUsername());
        assertEquals("ash@test.com", p.getEmail());
        assertEquals("hash", p.getPasswordHash());
        assertEquals(now, p.getCreatedAt());
        assertEquals("bio", p.getBio());
        assertEquals(5, p.getLevel());
        assertEquals(100, p.getXp());
        assertEquals("Kanto", p.getFavoriteRegion());
        assertEquals("Pikachu", p.getFavoritePokemon());
        assertEquals(10, p.getTotalCards());
    }

    @Test
    void player_setters() {
        Player p = new Player();
        p.setId(2L);
        p.setUsername("misty");
        p.setEmail("misty@test.com");
        p.setPasswordHash("hash2");
        p.setLevel(3);
        p.setXp(50);
        p.setFavoriteRegion("Johto");
        p.setFavoritePokemon("Togepi");
        p.setTotalCards(5);
        p.setBio("Water trainer");
        assertEquals(2L, p.getId());
        assertEquals("misty", p.getUsername());
        assertEquals("misty@test.com", p.getEmail());
        assertEquals(3, p.getLevel());
        assertEquals(50, p.getXp());
        assertEquals("Johto", p.getFavoriteRegion());
        assertEquals("Togepi", p.getFavoritePokemon());
        assertEquals(5, p.getTotalCards());
        assertEquals("Water trainer", p.getBio());
    }

    @Test
    void player_builder() {
        Player p = Player.builder()
                .id(3L)
                .username("brock")
                .email("brock@test.com")
                .passwordHash("hash3")
                .level(10)
                .build();
        assertEquals(3L, p.getId());
        assertEquals("brock", p.getUsername());
        assertEquals(10, p.getLevel());
        assertNotNull(p.toString());
    }

    @Test
    void player_equalsAndHashCode() {
        Player a = Player.builder().id(1L).username("ash").build();
        Player b = Player.builder().id(1L).username("ash").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Player.builder().id(2L).username("misty").build());
    }

    @Test
    void player_lists() {
        Player p = new Player();
        p.setDecks(new ArrayList<>());
        p.setBadges(new ArrayList<>());
        p.setAchievements(new ArrayList<>());
        p.setSkins(new ArrayList<>());
        p.setCustomizationItems(new ArrayList<>());
        p.setSessionsAsPlayer1(new ArrayList<>());
        assertNotNull(p.getDecks());
        assertNotNull(p.getBadges());
        assertNotNull(p.getAchievements());
        assertNotNull(p.getSkins());
        assertNotNull(p.getCustomizationItems());
    }

    // ------------------------------------------------------------------ Deck
    @Test
    void deck_noArgConstructor() {
        Deck d = new Deck();
        assertNull(d.getId());
        assertNull(d.getName());
    }

    @Test
    void deck_settersAndGetters() {
        Deck d = new Deck();
        d.setId(1L);
        d.setName("Fire Deck");
        d.setValid(true);
        d.setDefaultKey("fire");
        d.setCards(new ArrayList<>());
        d.setCreatedAt(LocalDateTime.now());
        assertEquals(1L, d.getId());
        assertEquals("Fire Deck", d.getName());
        assertTrue(d.isValid());
        assertEquals("fire", d.getDefaultKey());
        assertNotNull(d.getCards());
    }

    @Test
    void deck_builder() {
        Deck d = Deck.builder().id(1L).name("Water Deck").isValid(false).build();
        assertEquals(1L, d.getId());
        assertEquals("Water Deck", d.getName());
        assertFalse(d.isValid());
        assertNotNull(d.toString());
    }

    @Test
    void deck_equalsAndHashCode() {
        Deck a = Deck.builder().id(1L).name("Deck").build();
        Deck b = Deck.builder().id(1L).name("Deck").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ DeckCard
    @Test
    void deckCard_noArgConstructor() {
        DeckCard dc = new DeckCard();
        assertNull(dc.getId());
    }

    @Test
    void deckCard_settersAndGetters() {
        DeckCard dc = new DeckCard();
        dc.setId(1L);
        dc.setCardId("xy1-1");
        dc.setQuantity(4);
        assertEquals(1L, dc.getId());
        assertEquals("xy1-1", dc.getCardId());
        assertEquals(4, dc.getQuantity());
    }

    @Test
    void deckCard_allArgsConstructor() {
        Deck deck = Deck.builder().id(1L).name("d").build();
        DeckCard dc = new DeckCard(1L, deck, "xy1-2", 2);
        assertEquals(1L, dc.getId());
        assertEquals("xy1-2", dc.getCardId());
        assertEquals(2, dc.getQuantity());
        assertNotNull(dc.toString());
    }

    @Test
    void deckCard_builder() {
        DeckCard dc = DeckCard.builder().id(5L).cardId("xy1-5").quantity(3).build();
        assertEquals(5L, dc.getId());
        assertEquals("xy1-5", dc.getCardId());
        assertEquals(3, dc.getQuantity());
    }

    @Test
    void deckCard_equalsAndHashCode() {
        DeckCard a = DeckCard.builder().id(1L).cardId("xy1-1").quantity(1).build();
        DeckCard b = DeckCard.builder().id(1L).cardId("xy1-1").quantity(1).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ Achievement
    @Test
    void achievement_noArgConstructor() {
        Achievement a = new Achievement();
        assertNull(a.getId());
    }

    @Test
    void achievement_settersAndGetters() {
        Achievement a = new Achievement();
        a.setId("first_win");
        a.setName("First Win");
        a.setDescription("Win your first game");
        a.setIcon("🏆");
        assertEquals("first_win", a.getId());
        assertEquals("First Win", a.getName());
        assertEquals("Win your first game", a.getDescription());
        assertEquals("🏆", a.getIcon());
    }

    @Test
    void achievement_allArgsAndBuilder() {
        Achievement a = new Achievement("id", "name", "desc", "icon");
        assertEquals("id", a.getId());
        Achievement b = Achievement.builder().id("b").name("n").description("d").icon("i").build();
        assertEquals("b", b.getId());
        assertNotNull(a.toString());
        assertEquals(a, a);
    }

    @Test
    void achievement_equalsAndHashCode() {
        Achievement a = Achievement.builder().id("x").name("n").description("d").icon("i").build();
        Achievement b = Achievement.builder().id("x").name("n").description("d").icon("i").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ Badge
    @Test
    void badge_noArgConstructor() {
        Badge b = new Badge();
        assertNull(b.getId());
    }

    @Test
    void badge_settersAndGetters() {
        Badge b = new Badge();
        b.setId("boulder");
        b.setLabel("Boulder Badge");
        b.setDescription("Earned at Pewter City");
        b.setIcon("🪨");
        b.setHowToUnlock("Win 10 games");
        assertEquals("boulder", b.getId());
        assertEquals("Boulder Badge", b.getLabel());
        assertEquals("Earned at Pewter City", b.getDescription());
        assertEquals("🪨", b.getIcon());
        assertEquals("Win 10 games", b.getHowToUnlock());
    }

    @Test
    void badge_allArgsAndBuilder() {
        Badge b = new Badge("id", "label", "desc", "icon", "howto");
        assertEquals("id", b.getId());
        Badge c = Badge.builder().id("c").label("l").description("d").icon("i").howToUnlock("h").build();
        assertEquals("c", c.getId());
        assertNotNull(b.toString());
    }

    @Test
    void badge_equalsAndHashCode() {
        Badge a = Badge.builder().id("b").label("l").description("d").icon("i").howToUnlock("h").build();
        Badge b = Badge.builder().id("b").label("l").description("d").icon("i").howToUnlock("h").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ PlayerStats
    @Test
    void playerStats_noArgConstructor() {
        PlayerStats ps = new PlayerStats();
        assertEquals(0, ps.getWins());
        assertEquals(0, ps.getLosses());
    }

    @Test
    void playerStats_settersAndGetters() {
        PlayerStats ps = new PlayerStats();
        ps.setPlayerId(1L);
        ps.setWins(5);
        ps.setLosses(3);
        ps.setStreak(2);
        ps.setTournamentsWon(1);
        ps.setPacksOpened(10);
        ps.setTotalCards(100);
        ps.setDecksCreated(4);
        assertEquals(1L, ps.getPlayerId());
        assertEquals(5, ps.getWins());
        assertEquals(3, ps.getLosses());
        assertEquals(2, ps.getStreak());
        assertEquals(1, ps.getTournamentsWon());
        assertEquals(10, ps.getPacksOpened());
        assertEquals(100, ps.getTotalCards());
        assertEquals(4, ps.getDecksCreated());
    }

    @Test
    void playerStats_builder() {
        PlayerStats ps = PlayerStats.builder().playerId(1L).wins(10).losses(2).build();
        assertEquals(1L, ps.getPlayerId());
        assertEquals(10, ps.getWins());
        assertNotNull(ps.toString());
    }

    @Test
    void playerStats_equalsAndHashCode() {
        PlayerStats a = PlayerStats.builder().playerId(1L).wins(5).build();
        PlayerStats b = PlayerStats.builder().playerId(1L).wins(5).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ TrainerSkin
    @Test
    void trainerSkin_noArgConstructor() {
        TrainerSkin ts = new TrainerSkin();
        assertNull(ts.getId());
    }

    @Test
    void trainerSkin_settersAndGetters() {
        TrainerSkin ts = new TrainerSkin();
        ts.setId("default");
        ts.setName("Default Trainer");
        ts.setHatColor("#FF0000");
        ts.setShirtColor("#00FF00");
        ts.setPantsColor("#0000FF");
        ts.setSkinTone("#FFCCAA");
        assertEquals("default", ts.getId());
        assertEquals("Default Trainer", ts.getName());
        assertEquals("#FF0000", ts.getHatColor());
        assertEquals("#00FF00", ts.getShirtColor());
        assertEquals("#0000FF", ts.getPantsColor());
        assertEquals("#FFCCAA", ts.getSkinTone());
    }

    @Test
    void trainerSkin_allArgsAndBuilder() {
        TrainerSkin ts = new TrainerSkin("id", "name", "#F", "#G", "#B", "#S", "ash");
        assertEquals("id", ts.getId());
        TrainerSkin b = TrainerSkin.builder().id("b").name("n").hatColor("#1").shirtColor("#2").pantsColor("#3").skinTone("#4").build();
        assertEquals("b", b.getId());
        assertNotNull(ts.toString());
        assertEquals(ts, ts);
    }

    @Test
    void trainerSkin_equalsAndHashCode() {
        TrainerSkin a = TrainerSkin.builder().id("x").name("n").hatColor("#1").shirtColor("#2").pantsColor("#3").skinTone("#4").build();
        TrainerSkin b = TrainerSkin.builder().id("x").name("n").hatColor("#1").shirtColor("#2").pantsColor("#3").skinTone("#4").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ CustomizationItem
    @Test
    void customizationItem_noArgConstructor() {
        CustomizationItem ci = new CustomizationItem();
        assertNull(ci.getId());
    }

    @Test
    void customizationItem_settersAndGetters() {
        CustomizationItem ci = new CustomizationItem();
        ci.setId("hat1");
        ci.setName("Red Hat");
        ci.setCategory("HAT");
        ci.setColor("#FF0000");
        ci.setIconUrl("http://example.com/hat.png");
        assertEquals("hat1", ci.getId());
        assertEquals("Red Hat", ci.getName());
        assertEquals("HAT", ci.getCategory());
        assertEquals("#FF0000", ci.getColor());
        assertEquals("http://example.com/hat.png", ci.getIconUrl());
    }

    @Test
    void customizationItem_builder() {
        CustomizationItem ci = CustomizationItem.builder().id("id").name("n").category("c").build();
        assertEquals("id", ci.getId());
        assertNotNull(ci.toString());
    }

    @Test
    void customizationItem_equalsAndHashCode() {
        CustomizationItem a = CustomizationItem.builder().id("x").name("n").category("c").build();
        CustomizationItem b = CustomizationItem.builder().id("x").name("n").category("c").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ PlayerAchievementId
    @Test
    void playerAchievementId_noArgConstructor() {
        PlayerAchievementId id = new PlayerAchievementId();
        assertNull(id.getPlayerId());
        assertNull(id.getAchievementId());
    }

    @Test
    void playerAchievementId_allArgsConstructorAndSetters() {
        PlayerAchievementId id = new PlayerAchievementId(1L, "first_win");
        assertEquals(1L, id.getPlayerId());
        assertEquals("first_win", id.getAchievementId());
        id.setPlayerId(2L);
        id.setAchievementId("second_win");
        assertEquals(2L, id.getPlayerId());
        assertEquals("second_win", id.getAchievementId());
        assertNotNull(id.toString());
    }

    @Test
    void playerAchievementId_equalsAndHashCode() {
        PlayerAchievementId a = new PlayerAchievementId(1L, "win");
        PlayerAchievementId b = new PlayerAchievementId(1L, "win");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new PlayerAchievementId(2L, "win"));
    }

    // ------------------------------------------------------------------ PlayerBadgeId
    @Test
    void playerBadgeId_noArgConstructor() {
        PlayerBadgeId id = new PlayerBadgeId();
        assertNull(id.getPlayerId());
    }

    @Test
    void playerBadgeId_allArgsConstructorAndSetters() {
        PlayerBadgeId id = new PlayerBadgeId(1L, "boulder");
        assertEquals(1L, id.getPlayerId());
        assertEquals("boulder", id.getBadgeId());
        id.setPlayerId(3L);
        id.setBadgeId("cascade");
        assertEquals(3L, id.getPlayerId());
        assertEquals("cascade", id.getBadgeId());
        assertNotNull(id.toString());
    }

    @Test
    void playerBadgeId_equalsAndHashCode() {
        PlayerBadgeId a = new PlayerBadgeId(1L, "badge");
        PlayerBadgeId b = new PlayerBadgeId(1L, "badge");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ PlayerSkinId
    @Test
    void playerSkinId_noArgConstructor() {
        PlayerSkinId id = new PlayerSkinId();
        assertNull(id.getPlayerId());
    }

    @Test
    void playerSkinId_allArgsConstructorAndSetters() {
        PlayerSkinId id = new PlayerSkinId(1L, "default");
        assertEquals(1L, id.getPlayerId());
        assertEquals("default", id.getSkinId());
        id.setPlayerId(2L);
        id.setSkinId("red");
        assertEquals(2L, id.getPlayerId());
        assertEquals("red", id.getSkinId());
        assertNotNull(id.toString());
    }

    @Test
    void playerSkinId_equalsAndHashCode() {
        PlayerSkinId a = new PlayerSkinId(1L, "s");
        PlayerSkinId b = new PlayerSkinId(1L, "s");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ PlayerCustomizationId
    @Test
    void playerCustomizationId_noArgConstructor() {
        PlayerCustomizationId id = new PlayerCustomizationId();
        assertNull(id.getPlayerId());
    }

    @Test
    void playerCustomizationId_allArgsConstructorAndSetters() {
        PlayerCustomizationId id = new PlayerCustomizationId(1L, "hat1");
        assertEquals(1L, id.getPlayerId());
        assertEquals("hat1", id.getItemId());
        id.setPlayerId(5L);
        id.setItemId("shirt1");
        assertEquals(5L, id.getPlayerId());
        assertEquals("shirt1", id.getItemId());
        assertNotNull(id.toString());
    }

    @Test
    void playerCustomizationId_equalsAndHashCode() {
        PlayerCustomizationId a = new PlayerCustomizationId(1L, "i");
        PlayerCustomizationId b = new PlayerCustomizationId(1L, "i");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ PlayerAchievement
    @Test
    void playerAchievement_noArgConstructor() {
        PlayerAchievement pa = new PlayerAchievement();
        assertNull(pa.getId());
    }

    @Test
    void playerAchievement_settersAndGetters() {
        PlayerAchievement pa = new PlayerAchievement();
        PlayerAchievementId id = new PlayerAchievementId(1L, "win");
        pa.setId(id);
        pa.setUnlockedAt(LocalDateTime.now());
        assertEquals(id, pa.getId());
        assertNotNull(pa.getUnlockedAt());
        assertNotNull(pa.toString());
    }

    @Test
    void playerAchievement_builder() {
        LocalDateTime now = LocalDateTime.now();
        PlayerAchievementId id = new PlayerAchievementId(1L, "win");
        PlayerAchievement pa = PlayerAchievement.builder().id(id).unlockedAt(now).build();
        assertEquals(id, pa.getId());
        assertEquals(now, pa.getUnlockedAt());
    }

    @Test
    void playerAchievement_equalsAndHashCode() {
        PlayerAchievementId id = new PlayerAchievementId(1L, "win");
        PlayerAchievement a = PlayerAchievement.builder().id(id).build();
        PlayerAchievement b = PlayerAchievement.builder().id(id).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ PlayerBadge
    @Test
    void playerBadge_noArgConstructor() {
        PlayerBadge pb = new PlayerBadge();
        assertNull(pb.getId());
    }

    @Test
    void playerBadge_settersAndGetters() {
        PlayerBadge pb = new PlayerBadge();
        PlayerBadgeId id = new PlayerBadgeId(1L, "boulder");
        pb.setId(id);
        pb.setUnlockedAt(LocalDateTime.now());
        assertEquals(id, pb.getId());
        assertNotNull(pb.getUnlockedAt());
        assertNotNull(pb.toString());
    }

    @Test
    void playerBadge_builder() {
        PlayerBadgeId id = new PlayerBadgeId(1L, "boulder");
        LocalDateTime now = LocalDateTime.now();
        PlayerBadge pb = PlayerBadge.builder().id(id).unlockedAt(now).build();
        assertEquals(id, pb.getId());
    }

    // ------------------------------------------------------------------ PlayerSkin
    @Test
    void playerSkin_noArgConstructor() {
        PlayerSkin ps = new PlayerSkin();
        assertNull(ps.getId());
        assertFalse(ps.isEquipped());
    }

    @Test
    void playerSkin_settersAndGetters() {
        PlayerSkin ps = new PlayerSkin();
        PlayerSkinId id = new PlayerSkinId(1L, "default");
        ps.setId(id);
        ps.setEquipped(true);
        assertEquals(id, ps.getId());
        assertTrue(ps.isEquipped());
        assertNotNull(ps.toString());
    }

    @Test
    void playerSkin_builder() {
        PlayerSkinId id = new PlayerSkinId(1L, "red");
        PlayerSkin ps = PlayerSkin.builder().id(id).equipped(true).build();
        assertEquals(id, ps.getId());
        assertTrue(ps.isEquipped());
    }

    // ------------------------------------------------------------------ PlayerCustomization
    @Test
    void playerCustomization_noArgConstructor() {
        PlayerCustomization pc = new PlayerCustomization();
        assertNull(pc.getId());
    }

    @Test
    void playerCustomization_settersAndGetters() {
        PlayerCustomization pc = new PlayerCustomization();
        PlayerCustomizationId id = new PlayerCustomizationId(1L, "hat1");
        pc.setId(id);
        assertEquals(id, pc.getId());
        assertNotNull(pc.toString());
    }

    @Test
    void playerCustomization_allArgsConstructor() {
        PlayerCustomizationId id = new PlayerCustomizationId(1L, "hat1");
        Player player = Player.builder().id(1L).username("ash").build();
        CustomizationItem item = CustomizationItem.builder().id("hat1").name("Red Hat").category("HAT").build();
        PlayerCustomization pc = new PlayerCustomization(id, player, item);
        assertEquals(id, pc.getId());
        assertEquals(player, pc.getPlayer());
        assertEquals(item, pc.getItem());
    }

    @Test
    void playerCustomization_builder() {
        PlayerCustomizationId id = new PlayerCustomizationId(1L, "hat1");
        PlayerCustomization pc = PlayerCustomization.builder().id(id).build();
        assertEquals(id, pc.getId());
        assertNotNull(pc.toString());
    }

    @Test
    void playerCustomization_equalsAndHashCode() {
        PlayerCustomizationId id = new PlayerCustomizationId(1L, "hat1");
        PlayerCustomization a = PlayerCustomization.builder().id(id).build();
        PlayerCustomization b = PlayerCustomization.builder().id(id).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ CardSet
    @Test
    void cardSet_noArgConstructor() {
        CardSet cs = new CardSet();
        assertNull(cs.getId());
    }

    @Test
    void cardSet_settersAndGetters() {
        CardSet cs = new CardSet();
        cs.setId("xy1");
        cs.setName("XY");
        cs.setSeries("XY");
        cs.setPrintedTotal(146);
        cs.setReleaseDate(LocalDateTime.now());
        assertEquals("xy1", cs.getId());
        assertEquals("XY", cs.getName());
        assertEquals("XY", cs.getSeries());
        assertEquals(146, cs.getPrintedTotal());
        assertNotNull(cs.getReleaseDate());
    }

    @Test
    void cardSet_builder() {
        CardSet cs = CardSet.builder().id("xy1").name("XY").series("XY").printedTotal(146).build();
        assertEquals("xy1", cs.getId());
        assertNotNull(cs.toString());
    }

    @Test
    void cardSet_equalsAndHashCode() {
        CardSet a = CardSet.builder().id("xy1").name("XY").series("XY").printedTotal(146).build();
        CardSet b = CardSet.builder().id("xy1").name("XY").series("XY").printedTotal(146).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ Card
    @Test
    void card_noArgConstructor() {
        Card c = new Card();
        assertNull(c.getId());
    }

    @Test
    void card_settersAndGetters() {
        Card c = new Card();
        c.setId("xy1-1");
        c.setName("Venusaur-EX");
        c.setSupertype("Pokémon");
        c.setSubtypes(List.of("Basic", "EX"));
        c.setHp(180);
        c.setTypes(List.of("Grass"));
        c.setAttacks("[]");
        c.setParsedEffects("{}");
        c.setWeaknesses("Fire");
        c.setResistances("");
        c.setRetreatCost(List.of("C", "C", "C", "C"));
        c.setEvolvesFrom(null);
        c.setParsedTrainerEffects(new ArrayList<>());
        assertEquals("xy1-1", c.getId());
        assertEquals("Venusaur-EX", c.getName());
        assertEquals(180, c.getHp());
    }

    @Test
    void card_builder() {
        Card c = Card.builder().id("xy1-1").name("Venusaur-EX").supertype("Pokémon").hp(180).build();
        assertEquals("xy1-1", c.getId());
        assertNotNull(c.toString());
    }

    @Test
    void card_equalsAndHashCode() {
        Card a = Card.builder().id("xy1-1").name("Venusaur-EX").build();
        Card b = Card.builder().id("xy1-1").name("Venusaur-EX").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ RecoveryToken
    @Test
    void recoveryToken_noArgConstructor() {
        RecoveryToken rt = new RecoveryToken();
        assertNull(rt.getId());
        assertFalse(rt.isUsed());
    }

    @Test
    void recoveryToken_settersAndGetters() {
        RecoveryToken rt = new RecoveryToken();
        rt.setId(1L);
        rt.setToken("abc123");
        rt.setUsed(true);
        rt.setExpiresAt(LocalDateTime.now().plusHours(1));
        assertEquals(1L, rt.getId());
        assertEquals("abc123", rt.getToken());
        assertTrue(rt.isUsed());
        assertNotNull(rt.getExpiresAt());
    }

    @Test
    void recoveryToken_builder() {
        RecoveryToken rt = RecoveryToken.builder().id(1L).token("tok").used(false).build();
        assertEquals(1L, rt.getId());
        assertEquals("tok", rt.getToken());
        assertNotNull(rt.toString());
    }

    @Test
    void recoveryToken_equalsAndHashCode() {
        RecoveryToken a = RecoveryToken.builder().id(1L).token("tok").used(false).build();
        RecoveryToken b = RecoveryToken.builder().id(1L).token("tok").used(false).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ GameAction
    @Test
    void gameAction_noArgConstructor() {
        GameAction ga = new GameAction();
        assertNull(ga.getId());
    }

    @Test
    void gameAction_settersAndGetters() {
        GameAction ga = new GameAction();
        ga.setId(1L);
        ga.setTurnNumber(3);
        ga.setPlayerId(42L);
        ga.setActionType(ActionType.END_TURN);
        ga.setPayload("{}");
        ga.setResult("{}");
        ga.setTimestamp(LocalDateTime.now());
        assertEquals(1L, ga.getId());
        assertEquals(3, ga.getTurnNumber());
        assertEquals(42L, ga.getPlayerId());
        assertEquals(ActionType.END_TURN, ga.getActionType());
        assertEquals("{}", ga.getPayload());
        assertEquals("{}", ga.getResult());
        assertNotNull(ga.getTimestamp());
    }

    @Test
    void gameAction_builder() {
        GameAction ga = GameAction.builder().id(1L).turnNumber(1).playerId(1L)
                .actionType(ActionType.DRAW_CARD).payload("{}").result("{}").build();
        assertEquals(ActionType.DRAW_CARD, ga.getActionType());
        assertNotNull(ga.toString());
    }

    @Test
    void gameAction_equalsAndHashCode() {
        GameAction a = GameAction.builder().id(1L).turnNumber(1).playerId(1L)
                .actionType(ActionType.END_TURN).payload("{}").result("{}").build();
        GameAction b = GameAction.builder().id(1L).turnNumber(1).playerId(1L)
                .actionType(ActionType.END_TURN).payload("{}").result("{}").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ GameSession
    @Test
    void gameSession_noArgConstructor() {
        GameSession gs = new GameSession();
        assertNull(gs.getId());
    }

    @Test
    void gameSession_settersAndGetters() {
        GameSession gs = new GameSession();
        UUID id = UUID.randomUUID();
        gs.setId(id);
        gs.setStatus(GameStatus.ACTIVE);
        gs.setCurrentPlayerId(1L);
        gs.setPrizeCardsCount(6);
        gs.setCreatedAt(LocalDateTime.now());
        gs.setFinishedAt(LocalDateTime.now());
        assertEquals(id, gs.getId());
        assertEquals(GameStatus.ACTIVE, gs.getStatus());
        assertEquals(1L, gs.getCurrentPlayerId());
        assertEquals(6, gs.getPrizeCardsCount());
        assertNotNull(gs.getCreatedAt());
    }

    @Test
    void gameSession_prePersist_setsIdAndCreatedAt() {
        GameSession gs = new GameSession();
        gs.prePersist();
        assertNotNull(gs.getId());
        assertNotNull(gs.getCreatedAt());
    }

    @Test
    void gameSession_prePersist_doesNotOverwrite() {
        GameSession gs = new GameSession();
        UUID id = UUID.randomUUID();
        LocalDateTime time = LocalDateTime.of(2024, 1, 1, 0, 0);
        gs.setId(id);
        gs.setCreatedAt(time);
        gs.prePersist();
        assertEquals(id, gs.getId());
        assertEquals(time, gs.getCreatedAt());
    }

    @Test
    void gameSession_builder() {
        UUID id = UUID.randomUUID();
        GameSession gs = GameSession.builder().id(id).status(GameStatus.WAITING).prizeCardsCount(6).build();
        assertEquals(id, gs.getId());
        assertNotNull(gs.toString());
    }

    // ------------------------------------------------------------------ GameState
    @Test
    void gameState_noArgConstructor() {
        GameState gs = new GameState();
        assertNull(gs.getId());
    }

    @Test
    void gameState_settersAndGetters() {
        GameState gs = new GameState();
        gs.setId(1L);
        gs.setStateJson("{\"key\":\"value\"}");
        gs.setTurnNumber(5);
        gs.setTurnPhase(TurnPhase.MAIN);
        gs.setUpdatedAt(LocalDateTime.now());
        assertEquals(1L, gs.getId());
        assertEquals("{\"key\":\"value\"}", gs.getStateJson());
        assertEquals(5, gs.getTurnNumber());
        assertEquals(TurnPhase.MAIN, gs.getTurnPhase());
        assertNotNull(gs.getUpdatedAt());
    }

    @Test
    void gameState_builder() {
        GameState gs = GameState.builder().id(1L).stateJson("{}").turnNumber(1).turnPhase(TurnPhase.DRAW).build();
        assertEquals(1L, gs.getId());
        assertNotNull(gs.toString());
    }

    @Test
    void gameState_equalsAndHashCode() {
        GameState a = GameState.builder().id(1L).stateJson("{}").turnNumber(1).turnPhase(TurnPhase.DRAW).build();
        GameState b = GameState.builder().id(1L).stateJson("{}").turnNumber(1).turnPhase(TurnPhase.DRAW).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ------------------------------------------------------------------ StringListConverter
    @Test
    void stringListConverter_convertToDatabaseColumn_withValues() {
        StringListConverter converter = new StringListConverter();
        String result = converter.convertToDatabaseColumn(List.of("fire", "water", "grass"));
        assertEquals("fire,water,grass", result);
    }

    @Test
    void stringListConverter_convertToDatabaseColumn_empty() {
        StringListConverter converter = new StringListConverter();
        assertEquals("", converter.convertToDatabaseColumn(new ArrayList<>()));
        assertEquals("", converter.convertToDatabaseColumn(null));
    }

    @Test
    void stringListConverter_convertToEntityAttribute_withValues() {
        StringListConverter converter = new StringListConverter();
        List<String> result = converter.convertToEntityAttribute("fire,water,grass");
        assertEquals(3, result.size());
        assertTrue(result.contains("fire"));
        assertTrue(result.contains("water"));
        assertTrue(result.contains("grass"));
    }

    @Test
    void stringListConverter_convertToEntityAttribute_empty() {
        StringListConverter converter = new StringListConverter();
        assertTrue(converter.convertToEntityAttribute("").isEmpty());
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
    }

    @Test
    void stringListConverter_convertToEntityAttribute_withSpaces() {
        StringListConverter converter = new StringListConverter();
        List<String> result = converter.convertToEntityAttribute("fire, water , grass");
        assertEquals(3, result.size());
        assertTrue(result.contains("fire"));
        assertTrue(result.contains("water"));
        assertTrue(result.contains("grass"));
    }
}
