package ar.edu.utn.frc.tup.piii.models.cards;


public enum GameStatus {
    WAITING, READY_CHECK, SETUP, ACTIVE, FINISHED, SUDDEN_DEATH;

    /**
    WAITING: Creada por player1, esperando oponente

    READY_CHECK: Player2 se unió; ambos deben pulsar "Listo". Cuando ambos están listos
    se lanza la moneda de apertura y el ganador elige quién empieza; entonces pasa a SETUP.

    SETUP: Ambos colocan Pokémon Activo y Prize Cards

    ACTIVE: Partida en curso, turnos alternados

    FINISHED: Partida terminada, hay un ganador
    */
}
