package ar.edu.utn.frc.tup.piii.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for deck data (id, name, valid, cardCount, cards, validationErrors, createdAt).
 * Exposes no JPA entities. Mapped from {@link ar.edu.utn.frc.tup.piii.entities.Deck} + its DeckCard list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckResponse {
    private Long id;
    private String name;
    private boolean valid;
    private int cardCount;
    private List<DeckCardResponse> cards;
    private List<DeckValidationError> validationErrors;
    private LocalDateTime createdAt;
}
