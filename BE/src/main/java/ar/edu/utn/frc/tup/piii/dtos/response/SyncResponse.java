package ar.edu.utn.frc.tup.piii.dtos.response;

/** Response DTO for POST /api/cards/sync ({message, set, cardsImported, error}). */
public class SyncResponse {
    private String message;
    private String set;
    private int cardsImported;
    private String error;

    public SyncResponse() {}

    public SyncResponse(String message, String set) {
        this.message = message;
        this.set = set;
    }

    public SyncResponse(String message, String set, int cardsImported) {
        this.message = message;
        this.set = set;
        this.cardsImported = cardsImported;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSet() { return set; }
    public void setSet(String set) { this.set = set; }
    public int getCardsImported() { return cardsImported; }
    public void setCardsImported(int cardsImported) { this.cardsImported = cardsImported; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
