const FINISH_REASON_LABELS: Record<string, string> = {
  NO_POKEMON_LEFT: 'Un jugador se quedó sin Pokémon en juego.',
  ALL_PRIZES_TAKEN: 'Se tomaron todas las cartas de premio.',
  DECK_OUT: 'Un jugador se quedó sin cartas en el mazo.',
  CONCEDE: 'Un jugador concedió la partida.',
};

/** Converts backend finish-reason codes into user-facing copy. */
export function formatGameFinishReason(reason: string | null | undefined): string | null {
  if (!reason) return null;

  const normalized = reason.trim().toUpperCase();
  if (!normalized) return null;

  return FINISH_REASON_LABELS[normalized] ?? prettifyUnknownReason(normalized);
}

function prettifyUnknownReason(reason: string): string {
  return reason
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((word, index) => index === 0 ? capitalize(word) : word)
    .join(' ');
}

function capitalize(value: string): string {
  return value.length === 0 ? value : value[0].toUpperCase() + value.slice(1);
}
