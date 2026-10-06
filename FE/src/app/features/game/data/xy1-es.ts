/**
 * Curated Spanish translations for XY1 card attack names and effect texts.
 *
 * <p>The pokemontcg.io data is English-only; this is the hand-translated layer the
 * card-inspection overlay uses to show effects in Spanish. Keyed by `cardId`, then by the
 * ENGLISH attack name (the lookup key from the API). Missing entries fall back to English.
 *
 * <p>Coverage is incremental — this batch covers the two playable starter decks
 * (Fuego/Delphox + Agua/Greninja). Remaining XY1 cards are added over time.
 */
export interface AttackTranslation {
  name: string;
  text: string;
}

export interface CardTranslation {
  attacks: Record<string, AttackTranslation>;
}

export const XY1_ES: Record<string, CardTranslation> = {
  // ── Mazo Fuego / Delphox ──
  'xy1-24': {
    attacks: {
      'Will-O-Wisp': { name: 'Fuego Fatuo', text: '' },
    },
  },
  'xy1-25': {
    attacks: {
      'Clairvoyant Eye': {
        name: 'Ojo Clarividente',
        text: 'Mira las 3 cartas superiores de tu mazo y vuelve a ponerlas encima de tu mazo en el orden que quieras.',
      },
      'Fire Tail Slap': { name: 'Coletazo de Fuego', text: 'Descarta una Energía Fuego de este Pokémon.' },
    },
  },
  'xy1-26': {
    attacks: {
      'Blaze Ball': {
        name: 'Bola Ardiente',
        text: 'Este ataque hace 20 puntos de daño más por cada Energía Fuego unida a este Pokémon.',
      },
    },
  },
  'xy1-20': {
    attacks: {
      'Flamethrower': { name: 'Lanzallamas', text: 'Descarta una Energía de este Pokémon.' },
    },
  },
  'xy1-21': {
    attacks: {
      'Magma Mantle': {
        name: 'Manto de Magma',
        text: 'Puedes descartar la carta superior de tu mazo. Si esa carta es una carta de Energía Fuego, este ataque hace 50 puntos de daño más.',
      },
      'Heat Blast': { name: 'Ráfaga de Calor', text: '' },
    },
  },
  'xy1-22': {
    attacks: {
      'Live Coal': { name: 'Brasa Viva', text: '' },
      'Fireworks': { name: 'Fuegos Artificiales', text: 'Descarta una Energía de este Pokémon.' },
    },
  },
  'xy1-100': {
    attacks: {
      'Take Down': { name: 'Derribo', text: 'Este Pokémon se hace 10 puntos de daño a sí mismo.' },
      'Seething Anger': {
        name: 'Ira Hirviente',
        text: 'Lanza una moneda por cada contador de daño en este Pokémon. Este ataque hace 30 puntos de daño multiplicado por el número de caras.',
      },
    },
  },

  // ── Mazo Agua / Greninja ──
  'xy1-39': {
    attacks: {
      'Bounce': {
        name: 'Rebote',
        text: 'Lanza una moneda. Si sale cara, cambia este Pokémon por 1 de tus Pokémon en Banca.',
      },
    },
  },
  'xy1-40': {
    attacks: {
      'Lick': {
        name: 'Lengüetazo',
        text: 'Lanza una moneda. Si sale cara, el Pokémon Activo de tu rival queda Paralizado.',
      },
    },
  },
  'xy1-41': {
    attacks: {
      'Mist Slash': {
        name: 'Tajo Niebla',
        text: 'El daño de este ataque no se ve afectado por Debilidad, Resistencia ni ningún otro efecto en el Pokémon Activo de tu rival.',
      },
    },
  },
  'xy1-33': {
    attacks: {
      'Reckless Charge': { name: 'Carga Temeraria', text: 'Este Pokémon se hace 10 puntos de daño a sí mismo.' },
    },
  },
  'xy1-34': {
    attacks: {
      'Recover': { name: 'Recuperación', text: 'Descarta una Energía de este Pokémon y cúrale todo el daño.' },
      'Core Splash': {
        name: 'Salpicadura de Núcleo',
        text: 'Si este Pokémon tiene alguna Energía Psíquica unida, este ataque hace 30 puntos de daño más.',
      },
    },
  },
  'xy1-35': {
    attacks: {
      'Seafaring': {
        name: 'Travesía Marina',
        text: 'Lanza 3 monedas. Por cada cara, une una carta de Energía Agua de tu pila de descartes a tus Pokémon en Banca como quieras.',
      },
      'Hydro Pump': {
        name: 'Hidrobomba',
        text: 'Este ataque hace 20 puntos de daño más por cada Energía Agua unida a este Pokémon.',
      },
    },
  },

  // ── Resto del set XY1 ──
  'xy1-1': {
    attacks: {
      'Poison Powder': { name: 'Polvo Veneno', text: 'El Pokémon Activo de tu rival queda Envenenado.' },
      'Jungle Hammer': { name: 'Martillo de la Selva', text: 'Cura 30 puntos de daño de este Pokémon.' },
    },
  },
  'xy1-2': {
    attacks: {
      'Crisis Vine': { name: 'Liana de Crisis', text: 'El Pokémon Activo de tu rival queda Paralizado y Envenenado.' },
    },
  },
  'xy1-3': {
    attacks: {
      'Leaf Munch': { name: 'Mordisco de Hoja', text: 'Si el Pokémon Activo de tu rival es un Pokémon Planta, este ataque hace 20 puntos de daño más.' },
    },
  },
  'xy1-4': {
    attacks: {
      'Harden': { name: 'Fortaleza', text: 'Durante el próximo turno de tu rival, si este Pokémon fuera dañado por un ataque, evita ese daño hecho a este Pokémon si ese daño es 60 o menos.' },
    },
  },
  'xy1-5': {
    attacks: {
      'Poison Jab': { name: 'Puya Nociva', text: 'El Pokémon Activo de tu rival queda Envenenado.' },
      'Flash Needle': { name: 'Aguja Flash', text: 'Lanza 3 monedas. Este ataque hace 40 puntos de daño multiplicado por el número de caras. Si todas salen cara, evita todos los efectos de ataques, incluido el daño, hechos a este Pokémon durante el próximo turno de tu rival.' },
    },
  },
  'xy1-6': {
    attacks: { 'Spinning Attack': { name: 'Ataque Giratorio', text: '' } },
  },
  'xy1-7': {
    attacks: {
      'Tackle': { name: 'Placaje', text: '' },
      'Mach Punch': { name: 'Ultrapuño', text: 'Este ataque hace 10 puntos de daño a 1 de los Pokémon en Banca de tu rival. (No apliques Debilidad ni Resistencia para los Pokémon en Banca.)' },
    },
  },
  'xy1-8': {
    attacks: {
      'Luring Glow': { name: 'Brillo Atrayente', text: 'Cambia 1 de los Pokémon en Banca de tu rival por su Pokémon Activo.' },
      'Signal Beam': { name: 'Rayo Señal', text: 'El Pokémon Activo de tu rival queda Confundido.' },
    },
  },
  'xy1-9': {
    attacks: {
      'Pheromotion': { name: 'Feromoción', text: 'Busca en tu mazo un Pokémon Planta, muéstralo y ponlo en tu mano. Después, baraja tu mazo.' },
      'Quick Attack': { name: 'Ataque Rápido', text: 'Lanza una moneda. Si sale cara, este ataque hace 20 puntos de daño más.' },
    },
  },
  'xy1-10': {
    attacks: {
      'Vine Whip': { name: 'Látigo Cepa', text: '' },
      'Leech Seed': { name: 'Drenadoras', text: 'Cura 10 puntos de daño de este Pokémon.' },
    },
  },
  'xy1-11': {
    attacks: {
      'Torment': { name: 'Tormento', text: 'Elige 1 de los ataques del Pokémon Activo de tu rival. Ese Pokémon no puede usar ese ataque durante el próximo turno de tu rival.' },
      'Solar Beam': { name: 'Rayo Solar', text: '' },
    },
  },
  'xy1-12': {
    attacks: { 'Pin Missile': { name: 'Misil Aguja', text: 'Lanza 4 monedas. Este ataque hace 10 puntos de daño multiplicado por el número de caras.' } },
  },
  'xy1-13': {
    attacks: {
      'Scrunch': { name: 'Encogimiento', text: 'Lanza una moneda. Si sale cara, evita todo el daño hecho a este Pokémon por ataques durante el próximo turno de tu rival.' },
      'Wood Hammer': { name: 'Mazazo', text: 'Este Pokémon se hace 10 puntos de daño a sí mismo.' },
    },
  },
  'xy1-14': {
    attacks: { 'Touchdown': { name: 'Aterrizaje', text: 'Cura 20 puntos de daño de este Pokémon.' } },
  },
  'xy1-15': {
    attacks: { 'Bug Bite': { name: 'Picadura', text: '' } },
  },
  'xy1-16': {
    attacks: {
      'Bug Bite': { name: 'Picadura', text: '' },
      'Stun Spore': { name: 'Paralizador', text: 'Lanza una moneda. Si sale cara, el Pokémon Activo de tu rival queda Paralizado.' },
    },
  },
  'xy1-17': {
    attacks: {
      'Conversion Powder': { name: 'Polvo de Conversión', text: 'Elige Dormido o Envenenado. El Pokémon Activo de tu rival queda afectado por esa Condición Especial.' },
      'Colorful Wind': { name: 'Viento Colorido', text: 'Este ataque hace 30 puntos de daño más por cada tipo distinto de Energía básica unida a este Pokémon.' },
    },
  },
  'xy1-18': {
    attacks: {
      'Lead': { name: 'Guía', text: 'Lanza una moneda. Si sale cara, busca en tu mazo una carta de Partidario, muéstrala y ponla en tu mano. Después, baraja tu mazo.' },
      'Tackle': { name: 'Placaje', text: '' },
    },
  },
  'xy1-19': {
    attacks: {
      'Lead': { name: 'Guía', text: 'Busca en tu mazo hasta 2 cartas de Partidario, muéstralas y ponlas en tu mano. Después, baraja tu mazo.' },
      'Charge Dash': { name: 'Embestida de Carga', text: 'Puedes hacer 20 puntos de daño más. Si lo haces, este Pokémon se hace 20 puntos de daño a sí mismo.' },
    },
  },
  'xy1-23': {
    attacks: {
      'Yawn': { name: 'Bostezo', text: 'El Pokémon Activo de tu rival queda Dormido.' },
      'Flamethrower': { name: 'Lanzallamas', text: 'Descarta una Energía Fuego de este Pokémon.' },
    },
  },
  'xy1-27': {
    attacks: {
      'Flame Charge': { name: 'Nitrocarga', text: 'Busca en tu mazo una carta de Energía Fuego y únela a este Pokémon. Después, baraja tu mazo.' },
      'Fire Wing': { name: 'Ala de Fuego', text: '' },
    },
  },
  'xy1-28': {
    attacks: {
      'Devastating Wind': { name: 'Viento Devastador', text: 'Tu rival baraja su mano en su mazo y roba 4 cartas.' },
      'Flare Blitz': { name: 'Envite Ígneo', text: 'Descarta todas las Energías Fuego unidas a este Pokémon.' },
    },
  },
  'xy1-29': {
    attacks: {
      'Rapid Spin': { name: 'Giro Rápido', text: 'Cambia este Pokémon por 1 de tus Pokémon en Banca. Luego, tu rival cambia su Pokémon Activo por 1 de sus Pokémon en Banca.' },
      'Splash Bomb': { name: 'Bomba de Agua', text: 'Lanza una moneda. Si sale cruz, este Pokémon se hace 30 puntos de daño a sí mismo.' },
    },
  },
  'xy1-30': {
    attacks: { 'Hydro Bombard': { name: 'Bombardeo Hidro', text: 'Este ataque hace 30 puntos de daño a 2 de los Pokémon en Banca de tu rival. (No apliques Debilidad ni Resistencia para los Pokémon en Banca.)' } },
  },
  'xy1-31': {
    attacks: { 'Rain Splash': { name: 'Salpicadura', text: '' } },
  },
  'xy1-32': {
    attacks: {
      'Clamp Crush': { name: 'Tenaza Trituradora', text: 'Lanza una moneda. Si sale cara, el Pokémon Activo de tu rival queda Paralizado y descarta una Energía unida a ese Pokémon.' },
      'Spike Cannon': { name: 'Clavos Cañón', text: 'Lanza 5 monedas. Este ataque hace 30 puntos de daño multiplicado por el número de caras.' },
    },
  },
  'xy1-36': {
    attacks: {
      'Refresh': { name: 'Alivio', text: 'Cura 30 puntos de daño y quita todas las Condiciones Especiales de este Pokémon.' },
      'Spiny Rush': { name: 'Carga Espinosa', text: 'Lanza una moneda hasta que salga cruz. Este ataque hace 20 puntos de daño más por cada cara.' },
    },
  },
  'xy1-37': {
    attacks: {
      'Wave Splash': { name: 'Salpicadura de Ola', text: '' },
      'Water Splash': { name: 'Salpicadura de Agua', text: 'Lanza una moneda. Si sale cara, este ataque hace 20 puntos de daño más.' },
    },
  },
  'xy1-38': {
    attacks: {
      'Recycle': { name: 'Reciclaje', text: 'Pon una carta de tu pila de descartes encima de tu mazo.' },
      'Surf': { name: 'Surf', text: '' },
    },
  },
  'xy1-42': {
    attacks: {
      'Nuzzle': { name: 'Moflete Estático', text: 'Lanza una moneda. Si sale cara, el Pokémon Activo de tu rival queda Paralizado.' },
      'Quick Attack': { name: 'Ataque Rápido', text: 'Lanza una moneda. Si sale cara, este ataque hace 10 puntos de daño más.' },
    },
  },
  'xy1-43': {
    attacks: {
      'Circle Circuit': { name: 'Circuito Circular', text: 'Este ataque hace 20 puntos de daño multiplicado por el número de tus Pokémon en Banca.' },
      'Thunderbolt': { name: 'Rayo', text: 'Descarta todas las Energías unidas a este Pokémon.' },
    },
  },
  'xy1-44': {
    attacks: { 'Rollout': { name: 'Rodada', text: '' } },
  },
  'xy1-45': {
    attacks: {
      'Eerie Impulse': { name: 'Onda Anómala', text: 'Lanza una moneda. Si sale cara, descarta una Energía unida a 1 de los Pokémon de tu rival.' },
      'Rollout': { name: 'Rodada', text: '' },
    },
  },
  'xy1-46': {
    attacks: {
      'Energy Glide': { name: 'Planeo de Energía', text: 'Busca en tu mazo una carta de Energía Rayo y únela a este Pokémon. Después, baraja tu mazo. Si uniste Energía de esta manera, cambia este Pokémon por 1 de tus Pokémon en Banca.' },
      'Electron Crush': { name: 'Aplastamiento Electrón', text: 'Puedes descartar una Energía unida a este Pokémon. Si lo haces, este ataque hace 30 puntos de daño más.' },
    },
  },
  'xy1-47': {
    attacks: { 'Bite': { name: 'Mordisco', text: '' } },
  },
  'xy1-48': {
    attacks: {
      'Gastro Acid': { name: 'Bilis', text: 'El Pokémon Defensor no tiene Habilidades hasta el final de tu próximo turno.' },
      'Poison Jab': { name: 'Puya Nociva', text: 'El Pokémon Activo de tu rival queda Envenenado.' },
    },
  },
  'xy1-49': {
    attacks: { 'Splash': { name: 'Salpicadura', text: '' } },
  },
  'xy1-50': {
    attacks: {
      'Tricky Steps': { name: 'Pasos Engañosos', text: 'Puedes mover una Energía unida al Pokémon Activo de tu rival a 1 de sus Pokémon en Banca.' },
      'Psybeam': { name: 'Psicorrayo', text: 'El Pokémon Activo de tu rival queda Confundido.' },
    },
  },
  'xy1-51': {
    attacks: { 'Poison Sting': { name: 'Picotazo Veneno', text: 'El Pokémon Activo de tu rival queda Envenenado.' } },
  },
  'xy1-52': {
    attacks: { 'Continuous Tumble': { name: 'Voltereta Continua', text: 'Lanza una moneda hasta que salga cruz. Este ataque hace 30 puntos de daño multiplicado por el número de caras.' } },
  },
  'xy1-53': {
    attacks: {
      'Random Peck': { name: 'Picotazo Aleatorio', text: 'Lanza 2 monedas. Este ataque hace 20 puntos de daño más por cada cara.' },
      'Poison Ring': { name: 'Anillo Veneno', text: 'El Pokémon Activo de tu rival queda Envenenado. Ese Pokémon no puede retirarse durante el próximo turno de tu rival.' },
    },
  },
  'xy1-54': {
    attacks: {
      'Astonish': { name: 'Impresionar', text: 'Elige una carta al azar de la mano de tu rival. Tu rival muestra esa carta y la baraja en su mazo.' },
      'Hook': { name: 'Gancho', text: '' },
    },
  },
  'xy1-55': {
    attacks: { 'Tree Slam': { name: 'Golpe Arbóreo', text: 'Este ataque hace 20 puntos de daño a 2 de los Pokémon en Banca de tu rival. (No apliques Debilidad ni Resistencia para los Pokémon en Banca.)' } },
  },
  'xy1-56': {
    attacks: { 'Confuse Ray': { name: 'Rayo Confuso', text: 'El Pokémon Activo de tu rival queda Confundido.' } },
  },
  'xy1-57': {
    attacks: {
      'Eerie Voice': { name: 'Voz Inquietante', text: 'Pon 2 contadores de daño en cada uno de los Pokémon de tu rival.' },
      'Spirit Scream': { name: 'Grito Espectral', text: 'Pon contadores de daño en ambos Pokémon Activos hasta que los PS restantes de cada Pokémon sean 10.' },
    },
  },
  'xy1-58': {
    attacks: {
      'Mine': { name: 'Minar', text: 'Mira la carta superior del mazo de tu rival. Luego, puedes hacer que tu rival baraje su mazo.' },
      'Mud-Slap': { name: 'Bofetón Lodo', text: '' },
    },
  },
  'xy1-59': {
    attacks: {
      'Earthquake': { name: 'Terremoto', text: 'Este ataque hace 10 puntos de daño a cada uno de tus Pokémon en Banca. (No apliques Debilidad ni Resistencia para los Pokémon en Banca.)' },
      'Rock Tumble': { name: 'Caída de Rocas', text: 'El daño de este ataque no se ve afectado por la Resistencia.' },
    },
  },
  'xy1-60': {
    attacks: {
      'Dig Out': { name: 'Excavar', text: 'Descarta la carta superior de tu mazo. Si esa carta es una Energía Lucha, únela a este Pokémon.' },
      'Horn Drill': { name: 'Perforador', text: '' },
    },
  },
  'xy1-61': {
    attacks: {
      'Horn Drill': { name: 'Perforador', text: '' },
      'Mad Mountain': { name: 'Montaña Furiosa', text: 'Lanza 2 monedas. Si ambas salen cara, descarta la carta superior del mazo de tu rival por cada contador de daño en este Pokémon.' },
    },
  },
  'xy1-62': {
    attacks: {
      'Rock Black': { name: 'Roca Negra', text: 'Lanza una moneda por cada Energía Lucha unida a este Pokémon. Este ataque hace 50 puntos de daño multiplicado por el número de caras.' },
      'Rock Wrecker': { name: 'Roca Demoledora', text: 'El daño de este ataque no se ve afectado por Debilidad ni Resistencia. Este Pokémon no puede atacar durante tu próximo turno.' },
    },
  },
  'xy1-63': {
    attacks: {
      'Double Draw': { name: 'Doble Robo', text: 'Roba 2 cartas.' },
      'Moonblast': { name: 'Fuerza Lunar', text: 'Durante el próximo turno de tu rival, todo el daño hecho por ataques del Pokémon Defensor se reduce en 20 (antes de aplicar Debilidad y Resistencia).' },
    },
  },
  'xy1-64': {
    attacks: {
      'Cosmic Spin': { name: 'Giro Cósmico', text: 'Si Lunatone está en tu Banca, este ataque hace 30 puntos de daño más.' },
      'Solar Beam': { name: 'Rayo Solar', text: '' },
    },
  },
  'xy1-65': {
    attacks: { 'Pummel': { name: 'Aporreo', text: 'Lanza una moneda. Si sale cara, este ataque hace 20 puntos de daño más.' } },
  },
  'xy1-66': {
    attacks: {
      'Pummel': { name: 'Aporreo', text: 'Lanza una moneda. Si sale cara, este ataque hace 20 puntos de daño más.' },
      'Hammer Arm': { name: 'Machada', text: 'Descarta la carta superior del mazo de tu rival.' },
    },
  },
  'xy1-67': {
    attacks: {
      'Wake-Up Slap': { name: 'Bofetón Despertar', text: 'Si el Pokémon Activo de tu rival está afectado por una Condición Especial, este ataque hace 60 puntos de daño más. Luego, quita todas las Condiciones Especiales de ese Pokémon.' },
      'Dynamic Punch': { name: 'Puño Dinámico', text: 'Lanza una moneda. Si sale cara, este ataque hace 40 puntos de daño más y el Pokémon Activo de tu rival queda Confundido.' },
    },
  },
  'xy1-68': {
    attacks: {
      'Filch': { name: 'Hurto', text: 'Roba una carta.' },
      'Rip Claw': { name: 'Garra Desgarradora', text: 'Lanza una moneda. Si sale cara, descarta una Energía unida al Pokémon Activo de tu rival.' },
    },
  },
  'xy1-69': {
    attacks: {
      'Ram': { name: 'Embestida', text: '' },
      'Darkness Fang': { name: 'Colmillo Oscuro', text: '' },
    },
  },
  'xy1-70': {
    attacks: {
      'Crunch': { name: 'Triturar', text: 'Lanza una moneda. Si sale cara, descarta una Energía unida al Pokémon Activo de tu rival.' },
      'Darkness Fang': { name: 'Colmillo Oscuro', text: '' },
    },
  },
  'xy1-71': {
    attacks: {
      'Bother': { name: 'Molestar', text: 'Lanza una moneda. Si sale cara, tu rival no puede jugar cartas de Partidario de su mano durante su próximo turno.' },
      'Knock Back': { name: 'Empujón', text: 'Tu rival cambia su Pokémon Activo por 1 de sus Pokémon en Banca.' },
    },
  },
  'xy1-72': {
    attacks: {
      'Scratch': { name: 'Arañazo', text: '' },
      'Nasty Plot': { name: 'Maquinación', text: 'Busca en tu mazo una carta y ponla en tu mano. Después, baraja tu mazo.' },
    },
  },
  'xy1-73': {
    attacks: {
      'Corner': { name: 'Acorralar', text: 'El Pokémon Defensor no puede retirarse durante el próximo turno de tu rival.' },
      'Night Claw': { name: 'Garra Nocturna', text: 'Lanza una moneda. Si sale cruz, descarta 2 Energías unidas a este Pokémon.' },
    },
  },
  'xy1-74': {
    attacks: { 'Confusion Wave': { name: 'Onda de Confusión', text: 'Ambos Pokémon Activos quedan Confundidos.' } },
  },
  'xy1-75': {
    attacks: {
      'Tackle': { name: 'Placaje', text: '' },
      'Puncture': { name: 'Pinchazo', text: 'El daño de este ataque no se ve afectado por la Resistencia.' },
    },
  },
  'xy1-76': {
    attacks: {
      'Mental Trash': { name: 'Basura Mental', text: 'Tu rival lanza 4 monedas. Por cada cruz, descarta una carta de su mano.' },
      'Distortion Beam': { name: 'Rayo de Distorsión', text: 'Lanza una moneda. Si sale cara, el Pokémon Activo de tu rival queda Dormido. Si sale cruz, el Pokémon Activo de tu rival queda Confundido.' },
    },
  },
  'xy1-77': {
    attacks: {
      'Mental Panic': { name: 'Pánico Mental', text: 'Si el Pokémon Defensor intenta atacar durante el próximo turno de tu rival, tu rival lanza una moneda. Si sale cruz, ese ataque no hace nada.' },
      'Puncture': { name: 'Pinchazo', text: 'El daño de este ataque no se ve afectado por la Resistencia.' },
    },
  },
  'xy1-78': {
    attacks: {
      'Oblivion Wing': { name: 'Ala Mortífera', text: 'Une una carta de Energía Oscura de tu pila de descartes a 1 de tus Pokémon en Banca.' },
      'Darkness Blade': { name: 'Espada Oscura', text: 'Lanza una moneda. Si sale cruz, este Pokémon no puede atacar durante tu próximo turno.' },
    },
  },
  'xy1-79': {
    attacks: {
      'Evil Ball': { name: 'Bola Siniestra', text: 'Este ataque hace 20 puntos de daño más por cada Energía unida a ambos Pokémon Activos.' },
      'Y Cyclone': { name: 'Ciclón Y', text: 'Mueve una Energía de este Pokémon a 1 de tus Pokémon en Banca.' },
    },
  },
  'xy1-80': {
    attacks: {
      'Joust': { name: 'Justa', text: 'Antes de hacer daño, descarta todas las cartas de Herramienta Pokémon unidas al Pokémon Activo de tu rival.' },
      'Tailspin Piledriver': { name: 'Barrena Demoledora', text: 'Si el Pokémon Activo de tu rival ya tiene algún contador de daño, este ataque hace 40 puntos de daño más.' },
    },
  },
  'xy1-81': {
    attacks: {
      'Cut Down': { name: 'Tajo', text: 'Lanza una moneda. Si sale cara, descarta una Energía unida al Pokémon Activo de tu rival.' },
      'Metal Claw': { name: 'Garra Metal', text: '' },
    },
  },
  'xy1-82': {
    attacks: {
      'Metal Sound': { name: 'Eco Metálico', text: 'El Pokémon Activo de tu rival queda Confundido.' },
      'Metal Wallop': { name: 'Golpe Metálico', text: 'Durante tu próximo turno, el ataque Golpe Metálico de este Pokémon hace 40 puntos de daño más (antes de aplicar Debilidad y Resistencia).' },
    },
  },
  'xy1-83': {
    attacks: { 'Pierce': { name: 'Perforar', text: '' } },
  },
  'xy1-84': {
    attacks: { 'Dual Blades': { name: 'Doble Filo', text: 'Lanza 2 monedas. Este ataque hace 30 puntos de daño multiplicado por el número de caras.' } },
  },
  'xy1-85': {
    attacks: { 'Buster Swing': { name: 'Tajo Demoledor', text: 'El daño de este ataque no se ve afectado por la Resistencia.' } },
  },
  'xy1-86': {
    attacks: { "King's Shield": { name: 'Escudo Real', text: 'Evita todo el daño hecho a este Pokémon por ataques durante el próximo turno de tu rival. Este Pokémon no puede usar Escudo Real durante tu próximo turno.' } },
  },
  'xy1-87': {
    attacks: {
      'Rollout': { name: 'Rodada', text: '' },
      'Heartfelt Song': { name: 'Canción Sentida', text: 'Descarta una Energía Oscura unida al Pokémon Activo de tu rival.' },
    },
  },
  'xy1-88': {
    attacks: { 'Body Slam': { name: 'Bofetón Cuerpo', text: 'Lanza una moneda. Si sale cara, el Pokémon Activo de tu rival queda Paralizado.' } },
  },
  'xy1-89': {
    attacks: {
      'Gather Energy': { name: 'Reunir Energía', text: 'Busca en tu mazo una carta de Energía básica y únela a 1 de tus Pokémon. Después, baraja tu mazo.' },
      'Hocus Pinkus': { name: 'Abracadabra', text: 'El Pokémon Defensor no puede atacar durante el próximo turno de tu rival.' },
    },
  },
  'xy1-90': {
    attacks: {
      'Balloon Barrage': { name: 'Bombardeo Globo', text: 'Este ataque hace 20 puntos de daño multiplicado por la cantidad de Energías unidas a este Pokémon.' },
      'Double-Edge': { name: 'Doble Filo', text: 'Este Pokémon se hace 10 puntos de daño a sí mismo.' },
    },
  },
  'xy1-91': {
    attacks: {
      'Massage': { name: 'Masaje', text: 'Cura 60 puntos de daño de 1 de tus Pokémon en Banca.' },
      'Slap Down': { name: 'Bofetada', text: 'Lanza 2 monedas. Este ataque hace 20 puntos de daño más por cada cara.' },
    },
  },
  'xy1-92': {
    attacks: {
      'Sweet Scent': { name: 'Dulce Aroma', text: 'Cura 20 puntos de daño de 1 de tus Pokémon.' },
      'Flop': { name: 'Coletazo', text: '' },
    },
  },
  'xy1-93': {
    attacks: { 'Fairy Wind': { name: 'Viento Feérico', text: '' } },
  },
  'xy1-94': {
    attacks: {
      'Tackle': { name: 'Placaje', text: '' },
      'Fairy Wind': { name: 'Viento Feérico', text: '' },
    },
  },
  'xy1-95': {
    attacks: { 'Draining Kiss': { name: 'Beso Drenaje', text: 'Cura 30 puntos de daño de este Pokémon.' } },
  },
  'xy1-96': {
    attacks: {
      'Geomancy': { name: 'Geocontrol', text: 'Elige 2 de tus Pokémon en Banca. Por cada uno de esos Pokémon, busca en tu mazo una carta de Energía Hada y únela a ese Pokémon. Después, baraja tu mazo.' },
      'Rainbow Spear': { name: 'Lanza Arcoíris', text: 'Descarta una Energía unida a este Pokémon.' },
    },
  },
  'xy1-97': {
    attacks: {
      'Break Through': { name: 'Penetración', text: 'Este ataque hace 30 puntos de daño a 1 de los Pokémon en Banca de tu rival. (No apliques Debilidad ni Resistencia para los Pokémon en Banca.)' },
      'X Blast': { name: 'Explosión X', text: 'Este Pokémon no puede usar Explosión X durante tu próximo turno.' },
    },
  },
  'xy1-98': {
    attacks: { 'Double Hit': { name: 'Doble Golpe', text: 'Lanza 2 monedas. Este ataque hace 30 puntos de daño multiplicado por el número de caras.' } },
  },
  'xy1-99': {
    attacks: {
      'Rage': { name: 'Furia', text: 'Este ataque hace 10 puntos de daño más por cada contador de daño en este Pokémon.' },
      'Endeavor': { name: 'Esfuerzo', text: 'Lanza 2 monedas. Este ataque hace 20 puntos de daño más por cada cara.' },
    },
  },
  'xy1-101': {
    attacks: {
      'Glare': { name: 'Deslumbrar', text: 'Lanza una moneda. Si sale cara, el Pokémon Activo de tu rival queda Paralizado.' },
      'Second Bite': { name: 'Segundo Mordisco', text: 'Hace 10 puntos de daño más por cada contador de daño en el Pokémon Activo de tu rival.' },
    },
  },
  'xy1-102': {
    attacks: { 'Aerial Ace': { name: 'Golpe Aéreo', text: 'Lanza una moneda. Si sale cara, este ataque hace 30 puntos de daño más.' } },
  },
  'xy1-103': {
    attacks: { 'Wing Attack': { name: 'Ataque Ala', text: '' } },
  },
  'xy1-104': {
    attacks: {
      'Heal Bell': { name: 'Campana Cura', text: 'Cura 10 puntos de daño de cada uno de tus Pokémon.' },
      'Tail Whap': { name: 'Coletazo', text: '' },
    },
  },
  'xy1-105': {
    attacks: {
      'Energy Salon': { name: 'Salón de Energía', text: 'Busca en tu mazo 3 tipos distintos de cartas de Energía básica, muéstralas y ponlas en tu mano. Después, baraja tu mazo.' },
      'Fake Out': { name: 'Sorpresa', text: 'Lanza una moneda. Si sale cara, el Pokémon Activo de tu rival queda Paralizado.' },
    },
  },
  'xy1-106': {
    attacks: { 'Hyper Fang': { name: 'Hipercolmillo', text: 'Lanza una moneda. Si sale cruz, este ataque no hace nada.' } },
  },
  'xy1-107': {
    attacks: {
      'Double Headbutt': { name: 'Doble Cabezazo', text: 'Lanza 2 monedas. Este ataque hace 30 puntos de daño por cada cara.' },
      'Hypno Headbutt': { name: 'Cabezazo Hipnótico', text: 'Puedes hacer 30 puntos de daño más. Si lo haces, este Pokémon queda Dormido.' },
    },
  },
  'xy1-108': {
    attacks: {
      'Tackle': { name: 'Placaje', text: '' },
      'Bite': { name: 'Mordisco', text: '' },
    },
  },
  'xy1-109': {
    attacks: {
      'Bite': { name: 'Mordisco', text: '' },
      'Jump On': { name: 'Salto Encima', text: 'Lanza una moneda. Si sale cara, este ataque hace 20 puntos de daño más.' },
    },
  },
  'xy1-110': {
    attacks: {
      'Bite Off': { name: 'Mordisco Profundo', text: 'Si el Pokémon Activo de tu rival es un Pokémon-EX, este ataque hace 60 puntos de daño más.' },
      'Wild Barking': { name: 'Ladrido Salvaje', text: 'Hace 20 puntos de daño a 1 de los Pokémon en Banca de tu rival. (No apliques Debilidad ni Resistencia para los Pokémon en Banca.)' },
    },
  },
  'xy1-111': {
    attacks: { 'Dig': { name: 'Excavar', text: 'Lanza una moneda. Si sale cara, evita todos los efectos de ataques, incluido el daño, hechos a este Pokémon durante el próximo turno de tu rival.' } },
  },
  'xy1-112': {
    attacks: {
      'Pickup': { name: 'Recogida', text: 'Pon 2 cartas de Objeto de tu pila de descartes en tu mano.' },
      'Dig': { name: 'Excavar', text: 'Lanza una moneda. Si sale cara, evita todos los efectos de ataques, incluido el daño, hechos a este Pokémon durante el próximo turno de tu rival.' },
    },
  },
  'xy1-113': {
    attacks: {
      'Me First': { name: 'Yo Primero', text: 'Roba una carta.' },
      'Peck': { name: 'Picotazo', text: '' },
    },
  },
  'xy1-114': {
    attacks: { 'Energy Cutoff': { name: 'Corte de Energía', text: 'Lanza una moneda. Si sale cara, descarta una Energía unida al Pokémon Activo de tu rival.' } },
  },

  // ── Cartas de arte alternativo (mismos ataques) ──
  'xy1-141': {
    attacks: {
      'Poison Powder': { name: 'Polvo Veneno', text: 'El Pokémon Activo de tu rival queda Envenenado.' },
      'Jungle Hammer': { name: 'Martillo de la Selva', text: 'Cura 30 puntos de daño de este Pokémon.' },
    },
  },
  'xy1-142': {
    attacks: {
      'Rapid Spin': { name: 'Giro Rápido', text: 'Cambia este Pokémon por 1 de tus Pokémon en Banca. Luego, tu rival cambia su Pokémon Activo por 1 de sus Pokémon en Banca.' },
      'Splash Bomb': { name: 'Bomba de Agua', text: 'Lanza una moneda. Si sale cruz, este Pokémon se hace 30 puntos de daño a sí mismo.' },
    },
  },
  'xy1-143': {
    attacks: {
      'Energy Glide': { name: 'Planeo de Energía', text: 'Busca en tu mazo una carta de Energía Rayo y únela a este Pokémon. Después, baraja tu mazo. Si uniste Energía de esta manera, cambia este Pokémon por 1 de tus Pokémon en Banca.' },
      'Electron Crush': { name: 'Aplastamiento Electrón', text: 'Puedes descartar una Energía unida a este Pokémon. Si lo haces, este ataque hace 30 puntos de daño más.' },
    },
  },
  'xy1-144': {
    attacks: {
      'Evil Ball': { name: 'Bola Siniestra', text: 'Este ataque hace 20 puntos de daño más por cada Energía unida a ambos Pokémon Activos.' },
      'Y Cyclone': { name: 'Ciclón Y', text: 'Mueve una Energía de este Pokémon a 1 de tus Pokémon en Banca.' },
    },
  },
  'xy1-145': {
    attacks: {
      'Joust': { name: 'Justa', text: 'Antes de hacer daño, descarta todas las cartas de Herramienta Pokémon unidas al Pokémon Activo de tu rival.' },
      'Tailspin Piledriver': { name: 'Barrena Demoledora', text: 'Si el Pokémon Activo de tu rival ya tiene algún contador de daño, este ataque hace 40 puntos de daño más.' },
    },
  },
  'xy1-146': {
    attacks: {
      'Break Through': { name: 'Penetración', text: 'Este ataque hace 30 puntos de daño a 1 de los Pokémon en Banca de tu rival. (No apliques Debilidad ni Resistencia para los Pokémon en Banca.)' },
      'X Blast': { name: 'Explosión X', text: 'Este Pokémon no puede usar Explosión X durante tu próximo turno.' },
    },
  },
};
