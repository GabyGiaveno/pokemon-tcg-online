package ar.edu.utn.frc.tup.piii.models.game.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardInstanceState {

    private String instanceId; // Unique UUID for this specific card
    private String cardId;     // The pokemontcg.io ID
    private String name;
    private List<String> subtypes; // Card subtypes (e.g., "Basic", "Stage 1", "Energy")
    private CardZone zone;
}
