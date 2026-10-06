package ar.edu.utn.frc.tup.piii.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** Response DTO for the full trainer profile (GET /api/players/me). */
public class ProfileResponse {
    private Long id;
    private String username;
    private String email;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private int decksCount;
    private int xpPercent;
    private String bio;
    private int level;
    private String favoriteRegion;
    private String favoritePokemon;
    private int totalCards;
    private int wins;
    private int losses;
    private int streak;
    private int tournamentsWon;
    private int packsOpened;
    private int decksCreated;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getDecksCount() { return decksCount; }
    public void setDecksCount(int decksCount) { this.decksCount = decksCount; }

    public int getXpPercent() { return xpPercent; }
    public void setXpPercent(int xpPercent) { this.xpPercent = xpPercent; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getFavoriteRegion() { return favoriteRegion; }
    public void setFavoriteRegion(String favoriteRegion) { this.favoriteRegion = favoriteRegion; }

    public String getFavoritePokemon() { return favoritePokemon; }
    public void setFavoritePokemon(String favoritePokemon) { this.favoritePokemon = favoritePokemon; }

    public int getTotalCards() { return totalCards; }
    public void setTotalCards(int totalCards) { this.totalCards = totalCards; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }

    public int getTournamentsWon() { return tournamentsWon; }
    public void setTournamentsWon(int tournamentsWon) { this.tournamentsWon = tournamentsWon; }

    public int getPacksOpened() { return packsOpened; }
    public void setPacksOpened(int packsOpened) { this.packsOpened = packsOpened; }

    public int getDecksCreated() { return decksCreated; }
    public void setDecksCreated(int decksCreated) { this.decksCreated = decksCreated; }
}
