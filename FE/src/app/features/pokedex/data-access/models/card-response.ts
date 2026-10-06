export interface CardResponse {
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
  attacks: string | null;
  weaknesses: string | null;
  resistances: string | null;
  retreatCost: string[];
  aceTactician: boolean;
}

export interface CardPageResponse {
  data: CardResponse[];
  total: number;
  page: number;
  size: number;
}

export interface CardSearchParams {
  name?: string;
  set?: string;
  supertype?: string;
  type?: string;
  page?: number;
  size?: number;
  sort?: string;
}
