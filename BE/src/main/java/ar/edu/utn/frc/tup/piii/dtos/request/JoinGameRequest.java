package ar.edu.utn.frc.tup.piii.dtos.request;

/** Request DTO for POST /api/games/{id}/join (deckId). */
public class JoinGameRequest {
    private Long deckId;

    public Long getDeckId() { return deckId; }
    public void setDeckId(Long deckId) { this.deckId = deckId; }
}
