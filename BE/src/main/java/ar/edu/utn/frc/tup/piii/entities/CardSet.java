package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity caching a Pokemon TCG set from pokemontcg.io (id String, name,
 * series, printedTotal, releaseDate).
 */
@Entity
@Table(name = "card_set")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSet {

    @Id
    @Column(nullable = false, length = 20)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String series;

    @Column(name = "printed_total")
    private int printedTotal;

    @Column(name = "release_date")
    private LocalDateTime releaseDate;

}
