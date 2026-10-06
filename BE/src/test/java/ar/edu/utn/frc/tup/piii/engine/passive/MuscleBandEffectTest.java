package ar.edu.utn.frc.tup.piii.engine.passive;

import ar.edu.utn.frc.tup.piii.engine.passive.tools.MuscleBandEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MuscleBandEffectTest {

    @Test
    void outgoingDamageReturns20() {
        assertEquals(20, new MuscleBandEffect().modifyOutgoingDamage(null));
    }

    @Test
    void incomingDamageReturnsZeroByDefault() {
        assertEquals(0, new MuscleBandEffect().modifyIncomingDamage(null));
    }
}
