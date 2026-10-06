package ar.edu.utn.frc.tup.piii.exceptions;

/** Thrown when a requested game session does not exist or the player is not a participant. */
public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(String message) {
        super(message);
    }
}
