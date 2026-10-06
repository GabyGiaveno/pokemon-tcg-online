package ar.edu.utn.frc.tup.piii.engine.passive;

import ar.edu.utn.frc.tup.piii.engine.chain.AttackContext;

public interface ToolEffect {
    default int modifyOutgoingDamage(AttackContext ctx) { return 0; }
    default int modifyIncomingDamage(AttackContext ctx) { return 0; }
}
