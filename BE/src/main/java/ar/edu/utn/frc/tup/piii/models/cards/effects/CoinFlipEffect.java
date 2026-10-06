package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CoinFlipEffect extends AttackEffect {
    private List<AttackEffect> ifHeads;
    private List<AttackEffect> ifTails;
}
