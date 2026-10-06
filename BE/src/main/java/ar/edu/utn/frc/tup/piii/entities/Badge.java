package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** JPA entity: catalogue of gym badges available in the system. */
@Entity
@Table(name = "badge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Badge {

    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 10)
    private String icon;

    @Column(name = "how_to_unlock", nullable = false)
    private String howToUnlock;
}
