package ar.edu.utn.frc.tup.piii.dtos.request;

/** Request DTO for POST /api/games/{id}/choose-first: who takes the first turn, relative to the caller. */
public class ChooseFirstRequest {
    /** "ME" (the coin-flip winner starts) or "OPPONENT" (the other player starts). */
    private String starter;

    public String getStarter() { return starter; }
    public void setStarter(String starter) { this.starter = starter; }
}
