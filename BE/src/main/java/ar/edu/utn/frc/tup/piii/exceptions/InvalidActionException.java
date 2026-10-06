package ar.edu.utn.frc.tup.piii.exceptions;

/**
 * Thrown when the GameEngine rejects a player action.
 * Carries a short machine-readable errorCode (e.g. "CARD_NOT_IN_HAND") plus a human-readable message.
 */
public class InvalidActionException extends RuntimeException {

    private final String errorCode;

    public InvalidActionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
