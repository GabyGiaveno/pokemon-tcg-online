package ar.edu.utn.frc.tup.piii.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for card data (id, name, supertype, subtypes, hp, types, cardSet info, images).
 * Exposes no JPA entities. Mapped from {@link ar.edu.utn.frc.tup.piii.entities.Card} + {@link ar.edu.utn.frc.tup.piii.entities.CardSet}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private String id;
    private String name;
    private String supertype;
    private List<String> subtypes;
    private Integer hp;
    private List<String> types;
    private String cardSetId;
    private String cardSetName;
    private String imageUrlSmall;
    private String imageUrlLarge;
    private String evolvesFrom;
    private String attacks;
    private String weaknesses;
    private String resistances;
    private List<String> retreatCost;
}
