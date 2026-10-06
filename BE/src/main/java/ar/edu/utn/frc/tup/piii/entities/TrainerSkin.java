package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** JPA entity: catalogue of equippable trainer skins. */
@Entity
@Table(name = "trainer_skin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerSkin {

    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "hat_color", nullable = false, length = 7)
    private String hatColor;

    @Column(name = "shirt_color", nullable = false, length = 7)
    private String shirtColor;

    @Column(name = "pants_color", nullable = false, length = 7)
    private String pantsColor;

    @Column(name = "skin_tone", nullable = false, length = 7)
    private String skinTone;

    @Column(name = "character", nullable = false, length = 20)
    private String character;
}
