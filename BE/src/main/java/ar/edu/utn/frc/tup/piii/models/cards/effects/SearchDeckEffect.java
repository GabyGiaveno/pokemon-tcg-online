package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SearchDeckEffect extends AttackEffect {
    private String filter;
    private String destination;
    private int amount;
}
