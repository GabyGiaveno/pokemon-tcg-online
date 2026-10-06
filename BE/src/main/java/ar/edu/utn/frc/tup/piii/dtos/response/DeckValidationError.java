package ar.edu.utn.frc.tup.piii.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a validation error for a deck.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckValidationError {
    private String code;
    private String message;
    private String cardId;
}
