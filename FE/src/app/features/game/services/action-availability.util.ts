import { ActionAvailabilityDto, ActionState } from '../models/game-state.dto';

/**
 * Shared pure function that computes which actions are available based on
 * the current game phase and basic board state.
 *
 * This is intentionally conservative — the BE performs authoritative validation.
 * The purpose is to give the player clear visual feedback about which actions
 * are likely to succeed in the current phase.
 *
 * Replaces previously duplicated logic in game-state.mapper.ts and
 * game-board-adapter.service.ts so both adapters produce the same result.
 */
export function computeActionAvailability(
  phase: string,
  isMyTurn: boolean,
  hasActive: boolean,
  hasBench: boolean,
  oppHasActive: boolean,
): ActionAvailabilityDto {
  const ifMyTurn = (enabled: boolean, reason?: string): ActionState =>
    isMyTurn
      ? { enabled, reason: enabled ? undefined : (reason ?? 'No disponible') }
      : { enabled: false, reason: 'Esperá tu turno' };

  const disabled = (reason: string): ActionState => ({ enabled: false, reason });

  switch (phase) {
    case 'SETUP':
      return {
        attack: disabled('Fase de preparación'),
        retreat: disabled('Fase de preparación'),
        playEnergy: disabled('Fase de preparación'),
        playTrainer: disabled('Fase de preparación'),
        evolve: disabled('Fase de preparación'),
        endTurn: ifMyTurn(true, 'Confirmar preparación'),
        playBasicPokemon: ifMyTurn(true, 'Colocar Pokémon básico'),
      };

    case 'DRAW':
      return {
        attack: disabled('Robando carta...'),
        retreat: disabled('Robando carta...'),
        playEnergy: disabled('Robando carta...'),
        playTrainer: disabled('Robando carta...'),
        evolve: disabled('Robando carta...'),
        endTurn: disabled('Robando carta...'),
      };

    case 'MAIN':
      return {
        attack: ifMyTurn(
          hasActive && oppHasActive,
          !hasActive
            ? 'Necesitás un Pokémon activo'
            : !oppHasActive
              ? 'No hay objetivo'
              : undefined,
        ),
        retreat: ifMyTurn(
          hasActive && hasBench,
          !hasActive
            ? 'Necesitás un Pokémon activo'
            : !hasBench
              ? 'No hay banco disponible'
              : undefined,
        ),
        playEnergy: ifMyTurn(hasActive, !hasActive ? 'Necesitás un Pokémon activo' : undefined),
        playTrainer: ifMyTurn(true),
        evolve: ifMyTurn(hasActive, !hasActive ? 'Necesitás un Pokémon activo' : undefined),
        endTurn: ifMyTurn(true),
        playBasicPokemon: ifMyTurn(true),
      };

    case 'ATTACK':
      return {
        attack: ifMyTurn(
          hasActive && oppHasActive,
          !hasActive
            ? 'Necesitás un Pokémon activo'
            : !oppHasActive
              ? 'No hay objetivo'
              : undefined,
        ),
        retreat: disabled('No podés retirarte en fase de ataque'),
        playEnergy: disabled('No podés jugar energía en fase de ataque'),
        playTrainer: disabled('No podés jugar entrenadores en fase de ataque'),
        evolve: disabled('No podés evolucionar en fase de ataque'),
        endTurn: disabled('Finalizá el ataque primero'),
      };

    case 'BETWEEN_TURNS':
      return {
        attack: disabled('Cambiando de turno...'),
        retreat: disabled('Cambiando de turno...'),
        playEnergy: disabled('Cambiando de turno...'),
        playTrainer: disabled('Cambiando de turno...'),
        evolve: disabled('Cambiando de turno...'),
        endTurn: disabled('Cambiando de turno...'),
      };

    default:
      return allDisabled();
  }
}

function allDisabled(): ActionAvailabilityDto {
  const d: ActionState = {
    enabled: false,
    reason: 'La disponibilidad real se conectará luego al backend.',
  };
  return {
    attack: d,
    retreat: d,
    playEnergy: d,
    playTrainer: d,
    evolve: d,
    endTurn: d,
  };
}
