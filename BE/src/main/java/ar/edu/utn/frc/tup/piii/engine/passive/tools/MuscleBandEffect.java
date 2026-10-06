package ar.edu.utn.frc.tup.piii.engine.passive.tools;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.passive.ToolEffect;

public class MuscleBandEffect implements ToolEffect {
    @Override
    public int modifyOutgoingDamage(AttackContext ctx) { return 20; }
}
