package ar.edu.utn.frc.tup.piii.models.cards;


public enum TurnPhase {
    SETUP, DRAW, MAIN, ATTACK, BETWEEN_TURNS
    /**
     SETUP: Ambos jugadores colocan Pokémon Activo y Banca antes de empezar.

     DRAW: Jugador roba 1 carta obligatoriamente.

     MAIN: Jugador, juega cartas, adjunta energía, se retira.

     ATTACK: Jugador, ejecuta exactamente 1 ataque (opcional — puede saltearse con END_TURN).

     BETWEEN_TURNS: Sistema, resuelve condiciones especiales, cambia turno.
    */
}
