export interface Attack {
  name: string;
  cost: string[];
  damage: string;
  text: string;
  convertedEnergyCost: number;
}

export interface WeaknessResistance {
  type: string;
  value: string;
}

export interface PokedexCard {
  id: string;
  name: string;
  supertype: string;
  subtypes: string[];
  hp: number | null;
  types: string[];
  cardSetId: string;
  cardSetName: string;
  imageUrlSmall: string;
  imageUrlLarge: string;
  evolvesFrom: string | null;
  attacks: Attack[] | null;
  weaknesses: WeaknessResistance[] | null;
  resistances: WeaknessResistance[] | null;
  retreatCost: string[];
  aceTactician: boolean;
}
