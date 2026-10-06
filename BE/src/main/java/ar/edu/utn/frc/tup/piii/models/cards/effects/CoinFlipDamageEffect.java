package ar.edu.utn.frc.tup.piii.models.cards.effects;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Coin-flip-gated damage used by ability triggers (e.g. Voltorb's Destiny Burst):
 * on heads, {@code headsCondition} is applied; on tails nothing happens.
 *
 * <p>Distinct from {@link CoinFlipEffect}, which carries {@code ifHeads}/{@code ifTails}
 * lists for attack effects — this is the single-branch shape the bigpickle scripts emit
 * for abilities ({@code COIN_FLIP_DAMAGE}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CoinFlipDamageEffect extends AttackEffect {
    private AttackEffect headsCondition;
}
