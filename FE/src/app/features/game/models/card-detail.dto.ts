/** A single attack option resolved from a card's static metadata. */
export interface AttackDetailDto {
  /** Index of this attack within the card's attack list — sent to the backend as `attackIndex`. */
  index: number;
  name: string;
  /** Energy types required to use this attack (e.g. ['Fire', 'Colorless']). */
  cost: string[];
  /** Raw damage value parsed from the card's attack text (0 if non-numeric, e.g. "20+"). */
  damage: number;
  text: string;
}

/** A typed modifier (weakness or resistance), e.g. `{ type: 'Grass', value: '×2' }`. */
export interface TypedValue {
  type: string;
  value: string;
}

/** Full card metadata for the inspection overlay (Pokédex-style detail). */
export interface CardDetailDto {
  cardId: string;
  name: string;
  supertype: string;
  subtypes: string[];
  hp: number | null;
  types: string[];
  evolvesFrom: string | null;
  setName: string;
  attacks: AttackDetailDto[];
  weaknesses: TypedValue[];
  resistances: TypedValue[];
  retreatCost: string[];
}
