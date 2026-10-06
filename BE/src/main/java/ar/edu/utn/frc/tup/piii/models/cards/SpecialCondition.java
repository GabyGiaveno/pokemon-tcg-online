package ar.edu.utn.frc.tup.piii.models.cards;


public enum SpecialCondition {
    NONE, ASLEEP, BURNED, CONFUSED, PARALYZED, POISONED

    /**
    NONE : Estado normal.

    ASLEEP: No puede atacar en este estado, entre turnos se lanza una moneda, cara = despierta y elimina la condicion.
            Excluyente con CONFUSED, PARALYZED

    BURNED: Recibe daño entre turnos (20 de daño + flip), se lanza una moneda, cara = se cura la quemadura.
            Independiente, coexiste con todoo

    CONFUSED: Antes de atacar se lanza una moneda: cara = realiza el ataque normalmente - cruz = se daña a sí mismo(30 de daño)
              y el ataque falla.
              Excluyente con ASLEEP, PARALYZED

    PARALYZED: No puede atacar ni retirarse durante ese turno. La condición se elimina automáticamente entre turnos.
               Excluyente con ASLEEP, CONFUSED

    POISONED: Recibe daño automático entre turnos(10 de daño).
              Independiente, coexiste con todoo
    */
}
