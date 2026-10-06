package ar.edu.utn.frc.tup.piii.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for POST and PUT /api/decks (name, list of cardId and quantity pairs).
 * <p>
 * Quantity is intentionally limited only by @Min(1) — no @Max here because Basic Energy cards
 * allow unlimited copies. The 4-copy rule is enforced in deck validators by card name
 * with a Basic Energy exception, not at DTO level.</p>
 */
@Data
public class CreateDeckRequest {

    @NotBlank(message = "Deck name is required")
    @Size(max = 100, message = "Deck name must be at most 100 characters")
    private String name;

    @NotEmpty(message = "At least one card is required")
    @Valid
    private List<CardEntry> cards;

    /**
     * A card-quantity pair within the deck request.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardEntry {
        @NotBlank(message = "Card ID is required")
        private String cardId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;
    }
}
