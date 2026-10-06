package ar.edu.utn.frc.tup.piii.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for POST /api/decks/{id}/validate (valid flag, errors list, total cardCount).
 * Lightweight alternative to {@link DeckResponse} when only validation status is needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckValidationResponse {
    private boolean valid;
    private List<DeckValidationError> errors;
    private int cardCount;
}
