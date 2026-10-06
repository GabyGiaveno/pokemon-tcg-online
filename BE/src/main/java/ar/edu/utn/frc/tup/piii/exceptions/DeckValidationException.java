package ar.edu.utn.frc.tup.piii.exceptions;

/** Thrown by DeckService when deck composition violates the 60-card rules. */
public class DeckValidationException extends RuntimeException {
    public DeckValidationException(String message) {
        super(message);
    }
}
