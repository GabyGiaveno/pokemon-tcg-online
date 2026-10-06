export interface PokemonTypeInfo {
  id: string;
  label: string;
  color: string;
  icon: string;
}

/** Paleta e iconos de tipo Pokémon — usados en TypeRail y badges de carta. */
export const POKEMON_TYPES: PokemonTypeInfo[] = [
  { id: 'Grass', label: 'Planta', color: '#78C850', icon: '🌿' },
  { id: 'Fire', label: 'Fuego', color: '#F08030', icon: '🔥' },
  { id: 'Water', label: 'Agua', color: '#6890F0', icon: '💧' },
  { id: 'Lightning', label: 'Eléctrico', color: '#F8D030', icon: '⚡' },
  { id: 'Psychic', label: 'Psíquico', color: '#F85888', icon: '🔮' },
  { id: 'Fighting', label: 'Lucha', color: '#C03028', icon: '🥊' },
  { id: 'Darkness', label: 'Siniestro', color: '#705848', icon: '🌑' },
  { id: 'Metal', label: 'Metal', color: '#B8B8D0', icon: '⚙️' },
  { id: 'Fairy', label: 'Hada', color: '#EE99AC', icon: '✨' },
  { id: 'Dragon', label: 'Dragón', color: '#7038F8', icon: '🐉' },
  { id: 'Colorless', label: 'Normal', color: '#A8A878', icon: '⭐' },
];

const TYPE_MAP = new Map(POKEMON_TYPES.map(t => [t.id, t]));

export function getTypeInfo(type: string): PokemonTypeInfo | undefined {
  return TYPE_MAP.get(type);
}

export function getTypeColor(type: string): string {
  return TYPE_MAP.get(type)?.color ?? '#A8A878';
}
