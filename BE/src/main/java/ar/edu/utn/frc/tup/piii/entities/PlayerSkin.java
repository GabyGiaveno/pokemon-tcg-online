package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** JPA entity: junction — which skins a player owns and which one is equipped. */
@Entity
@Table(name = "player_skin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSkin {

    @EmbeddedId
    private PlayerSkinId id;

    @ManyToOne(optional = false)
    @MapsId("playerId")
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(optional = false)
    @MapsId("skinId")
    @JoinColumn(name = "skin_id")
    private TrainerSkin skin;

    @Column(nullable = false)
    @Builder.Default
    private boolean equipped = false;
}
