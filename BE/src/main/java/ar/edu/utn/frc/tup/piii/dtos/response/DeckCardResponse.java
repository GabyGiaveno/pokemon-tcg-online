package ar.edu.utn.frc.tup.piii.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for a card entry within a deck (cardId, cardName, quantity, supertype, types, subtypes, imageUrlSmall).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckCardResponse {
    private String cardId;
    private String cardName;
    private int quantity;
    private String supertype;
    private List<String> types;
    private List<String> subtypes;
    private String imageUrlSmall;
}
