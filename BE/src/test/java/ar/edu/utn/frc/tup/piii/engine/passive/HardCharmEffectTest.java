package ar.edu.utn.frc.tup.piii.engine.passive;

import ar.edu.utn.frc.tup.piii.engine.passive.tools.HardCharmEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HardCharmEffectTest {

    @Test
    void incomingDamageReturnsMinus20() {
        assertEquals(-20, new HardCharmEffect().modifyIncomingDamage(null));
    }

    @Test
    void outgoingDamageReturnsZeroByDefault() {
        assertEquals(0, new HardCharmEffect().modifyOutgoingDamage(null));
    }
}
