-- =============================================================
-- V009__seed_profile_catalogs.sql
-- Seeds badge, achievement, trainer_skin, customization_item tables
-- PostgreSQL 15+ | Flyway migration V009
-- =============================================================

INSERT INTO badge (id, label, description, icon, how_to_unlock) VALUES
('fire',    'Insignia Llama',    'Otorgada por el líder del Gimnasio Fuego',    '🔥', 'Gana una partida con un mazo de tipo Fuego'),
('water',   'Insignia Cascada',  'Otorgada por el líder del Gimnasio Agua',     '💧', 'Gana una partida con un mazo de tipo Agua'),
('grass',   'Insignia Bosque',   'Otorgada por el líder del Gimnasio Planta',   '🌿', 'Gana una partida con un mazo de tipo Planta'),
('electric','Insignia Trueno',   'Otorgada por el líder del Gimnasio Eléctrico','⚡',  'Gana una partida con un mazo de tipo Eléctrico'),
('psychic', 'Insignia Mente',    'Otorgada por el líder del Gimnasio Psíquico', '🔮', 'Gana una partida con un mazo de tipo Psíquico'),
('fighting','Insignia Lucha',    'Otorgada por el líder del Gimnasio Lucha',    '🥊', 'Gana una partida con un mazo de tipo Lucha'),
('dark',    'Insignia Oscuridad','Otorgada por el líder del Gimnasio Siniestro', '🌑', 'Gana una partida con un mazo de tipo Siniestro'),
('dragon',  'Insignia Dragón',   'Otorgada por el líder del Gimnasio Dragón',   '🐉', 'Gana 10 partidas consecutivas')
ON CONFLICT (id) DO NOTHING;

INSERT INTO achievement (id, name, description, icon) VALUES
('first-win',     'Campeón Novato',     'Obtén tu primera victoria.',       '🏆'),
('collector-100', 'Coleccionista de Cartas', 'Consigue 100 cartas.',        '📚'),
('deck-master',   'Maestro de Mazos',   'Crea 10 mazos.',                  '🃏'),
('veteran',       'Veterano Pokémon',   'Juega 100 partidas.',              '⭐'),
('lucky',         'Golpe de Suerte',    'Gana una partida con una carta decisiva.', '🍀')
ON CONFLICT (id) DO NOTHING;

INSERT INTO trainer_skin (id, name, hat_color, shirt_color, pants_color, skin_tone) VALUES
('default', 'Ash',      '#e74c3c', '#2980b9', '#2c3e50', '#f5d0a9'),
('fire',    'Fuego',    '#e74c3c', '#c0392b', '#8e44ad', '#f5d0a9'),
('water',   'Agua',     '#3498db', '#2980b9', '#1a5276', '#f5d0a9'),
('electric','Eléctrico','#f1c40f', '#2c3e50', '#7f8c8d', '#f5d0a9'),
('dark',    'Oscuro',   '#2c3e50', '#1a1a2e', '#16213e', '#d4a574')
ON CONFLICT (id) DO NOTHING;

INSERT INTO customization_item (id, name, category, color) VALUES
('shirt-blue',    'Camisa Azul',      'clothes',    '#105189'),
('shirt-red',     'Camisa Roja',      'clothes',    '#c0392b'),
('shirt-black',   'Camisa Negra',     'clothes',    '#2c3e50'),
('hat-cap',       'Gorra Clásica',    'accessory',  '#DE940E'),
('hat-beanie',    'Gorro Invernal',   'accessory',  '#3498db'),
('glasses',       'Gafas Oscuras',    'accessory',  NULL),
('pose-1',        'Firme',            'pose',       NULL),
('pose-2',        'Con Puño',         'pose',       NULL),
('pose-3',        'Saludando',        'pose',       NULL),
('bg-stadium',    'Estadio',          'background', NULL),
('bg-beach',      'Playa',            'background', NULL),
('bg-mountain',   'Montaña',          'background', NULL)
ON CONFLICT (id) DO NOTHING;
