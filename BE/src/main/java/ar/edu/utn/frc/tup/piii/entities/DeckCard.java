package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** JPA entity: card-quantity pair within a deck (deck FK, card FK, quantity). */
@Entity
@Table(name = "deck_card",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"deck_id", "card_id"})})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Deck deck;

    @Column(name = "card_id", nullable = false)
    private String cardId;

    @Column(name = "quantity",nullable = false)
    private int quantity;
}
