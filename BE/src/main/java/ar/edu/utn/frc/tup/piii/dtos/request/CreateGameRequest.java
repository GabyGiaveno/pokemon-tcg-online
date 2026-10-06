package ar.edu.utn.frc.tup.piii.dtos.request;

/** Request DTO for POST /api/games (deckId). */
public class CreateGameRequest {
    private Long deckId;

    public Long getDeckId() { return deckId; }
    public void setDeckId(Long deckId) { this.deckId = deckId; }
}
