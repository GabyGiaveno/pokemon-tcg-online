package ar.edu.utn.frc.tup.piii.dtos.response;

import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for game session data (gameId, status, player usernames, createdAt). */
public class GameSessionResponse {
    private UUID gameId;
    private GameStatus status;
    private String player1Username;
    private String player2Username;
    private LocalDateTime createdAt;
    private int prizeCardsCount;

    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }
    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }
    public String getPlayer1Username() { return player1Username; }
    public void setPlayer1Username(String player1Username) { this.player1Username = player1Username; }
    public String getPlayer2Username() { return player2Username; }
    public void setPlayer2Username(String player2Username) { this.player2Username = player2Username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getPrizeCardsCount() { return prizeCardsCount; }
    public void setPrizeCardsCount(int prizeCardsCount) { this.prizeCardsCount = prizeCardsCount; }
}
