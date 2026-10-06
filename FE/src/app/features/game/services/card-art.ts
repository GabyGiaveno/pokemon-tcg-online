/**
 * Resolves a deterministic pokemontcg.io card art URL from a `cardId`.
 *
 * `cardId` follows the `{setId}-{number}` convention (e.g. `xy1-1`, `sm115-1`,
 * `base1-4`). Set ids may themselves contain digits, so we split on the LAST
 * dash to separate `{setId}` from `{number}`.
 *
 * Returns an empty string for malformed ids (no dash, or empty segments) so
 * callers (the `card` component) can detect it and render a placeholder
 * instead of requesting a broken image.
 */
export function resolveCardArt(cardId: string | null | undefined): string {
  if (!cardId) {
    return '';
  }

  const lastDash = cardId.lastIndexOf('-');
  if (lastDash <= 0 || lastDash === cardId.length - 1) {
    return '';
  }

  const setId = cardId.slice(0, lastDash);
  const number = cardId.slice(lastDash + 1);

  if (!setId || !number) {
    return '';
  }

  return `https://images.pokemontcg.io/${setId}/${number}.png`;
}
