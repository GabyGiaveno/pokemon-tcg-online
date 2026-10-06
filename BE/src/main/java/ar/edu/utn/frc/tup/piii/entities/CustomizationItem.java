package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** JPA entity: catalogue of equippable customization items (clothes, accessories, poses, backgrounds). */
@Entity
@Table(name = "customization_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizationItem {

    @Id
    @Column(length = 30)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(length = 7)
    private String color;

    @Column(name = "icon_url")
    private String iconUrl;
}
