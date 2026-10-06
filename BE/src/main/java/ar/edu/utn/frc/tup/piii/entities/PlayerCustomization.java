package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** JPA entity: junction — which customization items a player has unlocked. */
@Entity
@Table(name = "player_customization")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCustomization {

    @EmbeddedId
    private PlayerCustomizationId id;

    @ManyToOne(optional = false)
    @MapsId("playerId")
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(optional = false)
    @MapsId("itemId")
    @JoinColumn(name = "item_id")
    private CustomizationItem item;
}
