import { TrainerCharacter } from '../data-access/profile-api.types';

export interface SpritePalette {
  skin: string;
  skinShadow: string;
  hair: string;
  hairShadow: string;
  hat: string;
  hatShadow: string;
  hatBrim: string;
  shirt: string;
  shirtShadow: string;
  innerShirt: string;
  innerShirtShadow: string;
  pants: string;
  pantsShadow: string;
  shoes: string;
  shoesShadow: string;
  eye: string;
  eyeWhite: string;
}

export interface TrainerSpriteConfig {
  hatColor: string;
  shirtColor: string;
  pantsColor: string;
  skinTone: string;
  character?: TrainerCharacter;
}

const BASE_W = 32;
const BASE_H = 48;

export function buildPalette(config: TrainerSpriteConfig): SpritePalette {
  return {
    skin: config.skinTone,
    skinShadow: darken(config.skinTone, 24),
    hair: '#1a1a1a',
    hairShadow: '#000000',
    hat: config.hatColor,
    hatShadow: darken(config.hatColor, 25),
    hatBrim: darken(config.hatColor, 35),
    shirt: config.shirtColor,
    shirtShadow: darken(config.shirtColor, 22),
    innerShirt: '#e8e8e8',
    innerShirtShadow: '#b0b0b0',
    pants: config.pantsColor,
    pantsShadow: darken(config.pantsColor, 22),
    shoes: '#2c3e50',
    shoesShadow: '#1a252f',
    eye: '#1a1a1a',
    eyeWhite: '#ffffff',
  };
}

export function generateTrainerSprite(palette: SpritePalette, frame: number, character: TrainerCharacter = 'ash'): HTMLCanvasElement {
  if (character === 'brock') return generateBrockSprite(palette, frame);
  if (character === 'misty') return generateMistySprite(palette, frame);
  return generateAshSprite(palette, frame);
}

function darken(hex: string, amount: number): string {
  const num = parseInt(hex.replace('#', ''), 16);
  const r = Math.max(0, (num >> 16) - amount);
  const g = Math.max(0, ((num >> 8) & 0xff) - amount);
  const b = Math.max(0, (num & 0xff) - amount);
  return `#${(r << 16 | g << 8 | b).toString(16).padStart(6, '0')}`;
}

function rect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, color: string) {
  ctx.fillStyle = color;
  ctx.fillRect(x, y, w, h);
}

function px(ctx: CanvasRenderingContext2D, x: number, y: number, color: string) {
  ctx.fillStyle = color;
  ctx.fillRect(x, y, 1, 1);
}

function fillCircle(ctx: CanvasRenderingContext2D, cx: number, cy: number, r: number, color: string) {
  ctx.fillStyle = color;
  for (let dy = -r; dy <= r; dy++) {
    for (let dx = -r; dx <= r; dx++) {
      if (dx * dx + dy * dy <= r * r) {
        ctx.fillRect(cx + dx, cy + dy, 1, 1);
      }
    }
  }
}

// ═══════════════════════════════════════════════════════════════
// ASH
// ═══════════════════════════════════════════════════════════════

function generateAshSprite(palette: SpritePalette, frame: number): HTMLCanvasElement {
  const canvas = document.createElement('canvas');
  canvas.width = BASE_W;
  canvas.height = BASE_H;
  const ctx = canvas.getContext('2d')!;
  const bobY = frame === 0 ? 0 : 1;

  drawShoes(ctx, palette, frame, bobY);
  drawLegs(ctx, palette, frame, bobY);
  drawBodyArms(ctx, palette, bobY);
  drawJacket(ctx, palette, bobY);
  drawHairSide(ctx, palette, bobY);
  drawHead(ctx, palette, bobY);
  drawEyes(ctx, palette, bobY);
  drawMouth(ctx, palette, bobY);
  drawHairTop(ctx, palette, bobY);
  drawHat(ctx, palette, bobY);

  return canvas;
}

/* ─── HAT: Ash's cap — top half colored, bottom half white band, logo center ─── */
function drawHat(ctx: CanvasRenderingContext2D, p: SpritePalette, bob: number) {
  const y = bob;
  rect(ctx, 10, 1 + y, 12, 3, p.hat);
  rect(ctx, 9,  3 + y, 14, 1, p.hat);
  rect(ctx, 10, 4 + y, 12, 2, '#ffffff');
  rect(ctx, 15, 4 + y, 2, 2, p.hat);
  rect(ctx, 11, 6 + y, 10, 1, p.hatShadow);
  rect(ctx, 6,  7 + y, 16, 1, p.hatBrim);
  rect(ctx, 6,  8 + y, 4,  1, p.hatShadow);
}

function drawHairSide(ctx: CanvasRenderingContext2D, p: SpritePalette, bob: number) {
  const y = bob;
  rect(ctx, 9,  7 + y, 2, 9, p.hair);
  rect(ctx, 8,  9 + y, 2, 5, p.hair);
  rect(ctx, 7,  11 + y, 2, 3, p.hair);
  rect(ctx, 9,  8 + y, 1, 1, p.hairShadow);
  rect(ctx, 21, 7 + y, 2, 9, p.hair);
  rect(ctx, 22, 9 + y, 2, 5, p.hair);
  rect(ctx, 23, 11 + y, 2, 3, p.hair);
  rect(ctx, 22, 8 + y, 1, 1, p.hairShadow);
}

function drawHairTop(ctx: CanvasRenderingContext2D, p: SpritePalette, bob: number) {
  const y = bob;
  px(ctx, 11, 7 + y, p.hair);
  px(ctx, 12, 6 + y, p.hair);
  px(ctx, 13, 7 + y, p.hair);
  px(ctx, 14, 6 + y, p.hair);
  px(ctx, 15, 7 + y, p.hair);
  px(ctx, 18, 7 + y, p.hair);
  px(ctx, 19, 6 + y, p.hair);
  px(ctx, 20, 7 + y, p.hair);
  px(ctx, 10, 6 + y, p.hair);
  px(ctx, 9,  7 + y, p.hair);
}

function drawHead(ctx: CanvasRenderingContext2D, p: SpritePalette, bob: number) {
  const y = bob;
  fillCircle(ctx, 16, 12 + y, 5, p.skin);
  rect(ctx, 12, 7 + y, 8, 1, p.skin);
  rect(ctx, 11, 8 + y, 10, 7, p.skin);
  rect(ctx, 12, 15 + y, 8, 1, p.skin);
  rect(ctx, 13, 16 + y, 6, 1, p.skin);
  rect(ctx, 11, 9 + y, 1, 5, p.skinShadow);
  rect(ctx, 20, 9 + y, 1, 5, p.skinShadow);
  px(ctx, 12, 14 + y, p.skinShadow);
  px(ctx, 19, 14 + y, p.skinShadow);
}

function drawEyes(ctx: CanvasRenderingContext2D, p: SpritePalette, bob: number) {
  const y = bob;
  rect(ctx, 13, 9 + y, 2, 2, p.eyeWhite);
  rect(ctx, 17, 9 + y, 2, 2, p.eyeWhite);
  rect(ctx, 13, 9 + y, 2, 1, p.eye);
  rect(ctx, 13, 10 + y, 1, 1, p.eye);
  rect(ctx, 18, 10 + y, 1, 1, p.eye);
  rect(ctx, 17, 9 + y, 2, 1, p.eye);
}

function drawMouth(ctx: CanvasRenderingContext2D, p: SpritePalette, bob: number) {
  const y = bob;
  rect(ctx, 14, 13 + y, 4, 1, '#b83020');
  px(ctx, 13, 12 + y, '#b83020');
  px(ctx, 18, 12 + y, '#b83020');
}

function drawBodyArms(ctx: CanvasRenderingContext2D, p: SpritePalette, bob: number) {
  const y = bob;
  const glove = '#4a8c3f';
  const gloveShadow = '#2e5a28';
  rect(ctx, 14, 17 + y, 4, 1, p.skin);
  rect(ctx, 13, 18 + y, 6, 3, p.skin);
  rect(ctx, 9,  20 + y, 3, 5, p.skin);
  rect(ctx, 20, 20 + y, 3, 5, p.skin);
  rect(ctx, 8,  21 + y, 2, 4, p.skin);
  rect(ctx, 22, 21 + y, 2, 4, p.skin);
  rect(ctx, 9,  25 + y, 3, 3, glove);
  rect(ctx, 20, 25 + y, 3, 3, glove);
  rect(ctx, 8,  25 + y, 2, 2, glove);
  rect(ctx, 22, 25 + y, 2, 2, glove);
  rect(ctx, 13, 28 + y, 6, 1, p.skin);
  rect(ctx, 9,  28 + y, 3, 1, gloveShadow);
  rect(ctx, 20, 28 + y, 3, 1, gloveShadow);
  rect(ctx, 8,  27 + y, 2, 1, gloveShadow);
  rect(ctx, 22, 27 + y, 2, 1, gloveShadow);
}

function drawJacket(ctx: CanvasRenderingContext2D, p: SpritePalette, bob: number) {
  const y = bob;
  rect(ctx, 12, 20 + y, 8, 8, p.innerShirt);
  rect(ctx, 11, 21 + y, 10, 6, p.innerShirt);
  rect(ctx, 13, 28 + y, 6, 1, p.innerShirt);
  rect(ctx, 12, 19 + y, 8, 1, p.shirt);
  rect(ctx, 11, 20 + y, 1, 8, p.shirt);
  rect(ctx, 20, 20 + y, 1, 8, p.shirt);
  rect(ctx, 10, 21 + y, 1, 6, p.shirt);
  rect(ctx, 21, 21 + y, 1, 6, p.shirt);
  rect(ctx, 11, 28 + y, 1, 1, p.shirt);
  rect(ctx, 20, 28 + y, 1, 1, p.shirt);
  rect(ctx, 10, 21 + y, 1, 4, p.shirtShadow);
  rect(ctx, 21, 21 + y, 1, 4, p.shirtShadow);
  rect(ctx, 12, 19 + y, 1, 1, p.shirtShadow);
  rect(ctx, 19, 19 + y, 1, 1, p.shirtShadow);
  rect(ctx, 12, 21 + y, 8, 6, p.innerShirtShadow);
  rect(ctx, 11, 22 + y, 10, 4, p.innerShirt);
  rect(ctx, 12, 27 + y, 8, 1, p.innerShirtShadow);
}

function drawLegs(ctx: CanvasRenderingContext2D, p: SpritePalette, frame: number, bob: number) {
  const y = bob;
  const legShift = frame === 0 ? 0 : frame === 1 ? 1 : -1;
  rect(ctx, 13, 29 + y, 2, 9, p.pants);
  rect(ctx, 17, 29 + y, 2, 9, p.pants);
  rect(ctx, 12, 30 + y, 8, 7 + legShift, p.pants);
  rect(ctx, 11, 31 + y + legShift, 10, 4, p.pants);
  rect(ctx, 12, 38 + y, 2, 1, p.pantsShadow);
  rect(ctx, 18, 38 + y, 2, 1, p.pantsShadow);
}

function drawShoes(ctx: CanvasRenderingContext2D, p: SpritePalette, frame: number, bob: number) {
  const y = bob;
  const shoeOff = frame === 0 ? 0 : frame === 1 ? 1 : -1;
  rect(ctx, 11, 39 + y, 4, 3, p.shoes);
  rect(ctx, 17, 39 + y, 4, 3, p.shoes);
  rect(ctx, 10, 39 + y + shoeOff, 5, 2, p.shoesShadow);
  rect(ctx, 17, 39 + y - shoeOff, 5, 2, p.shoesShadow);
  rect(ctx, 11, 42 + y, 4, 1, p.shoes);
  rect(ctx, 17, 42 + y, 4, 1, p.shoes);
}

// ═══════════════════════════════════════════════════════════════
// BROCK
// ═══════════════════════════════════════════════════════════════

function generateBrockSprite(palette: SpritePalette, frame: number): HTMLCanvasElement {
  const canvas = document.createElement('canvas');
  canvas.width = BASE_W;
  canvas.height = BASE_H;
  const ctx = canvas.getContext('2d')!;

  const hair  = palette.hat;       // hat slot holds hair color
  const vest  = palette.shirt;     // shirt slot holds vest color
  const pants = palette.pants;
  const skin  = palette.skin;
  const hairShadow = darken(hair, 20);
  const vestShadow = darken(vest, 20);
  const pantsShadow = palette.pantsShadow;
  const innerShirt = '#7a5c40';
  const innerShadow = darken(innerShirt, 18);

  const bobY = frame === 0 ? 0 : 1;
  const y = bobY;

  // ── Shoes ──
  rect(ctx, 11, 39 + y, 4, 3, '#1a1a1a');
  rect(ctx, 17, 39 + y, 4, 3, '#1a1a1a');
  rect(ctx, 10, 39 + y, 5, 2, '#333333');
  rect(ctx, 17, 39 + y, 5, 2, '#333333');

  // ── Legs ──
  rect(ctx, 12, 29 + y, 8, 10, pants);
  rect(ctx, 11, 30 + y, 10, 7, pants);
  rect(ctx, 12, 37 + y, 3, 2, pantsShadow);
  rect(ctx, 17, 37 + y, 3, 2, pantsShadow);

  // ── Arms (skin, no gloves) ──
  rect(ctx, 8,  20 + y, 3, 9, skin);
  rect(ctx, 21, 20 + y, 3, 9, skin);
  rect(ctx, 7,  22 + y, 2, 6, skin);
  rect(ctx, 23, 22 + y, 2, 6, skin);
  // Hands
  rect(ctx, 8,  28 + y, 3, 2, skin);
  rect(ctx, 21, 28 + y, 3, 2, skin);

  // ── Inner shirt (center torso) ──
  rect(ctx, 13, 19 + y, 6, 10, innerShirt);
  rect(ctx, 12, 20 + y, 8, 8, innerShirt);
  rect(ctx, 11, 21 + y, 10, 6, innerShirt);
  rect(ctx, 12, 27 + y, 8, 2, innerShadow);

  // ── Vest panels (sides) ──
  rect(ctx, 10, 19 + y, 3, 10, vest);
  rect(ctx, 19, 19 + y, 3, 10, vest);
  rect(ctx, 10, 28 + y, 3, 1, vestShadow);
  rect(ctx, 19, 28 + y, 3, 1, vestShadow);
  // Vest collar/top
  rect(ctx, 13, 18 + y, 6, 2, vest);
  rect(ctx, 12, 19 + y, 1, 2, vest);
  rect(ctx, 19, 19 + y, 1, 2, vest);

  // ── Neck ──
  rect(ctx, 14, 17 + y, 4, 1, skin);

  // ── Head ──
  fillCircle(ctx, 16, 12 + y, 5, skin);
  rect(ctx, 12, 7 + y, 8, 2, skin);
  rect(ctx, 11, 8 + y, 10, 7, skin);
  rect(ctx, 12, 15 + y, 8, 1, skin);
  rect(ctx, 13, 16 + y, 6, 1, skin);
  rect(ctx, 11, 9 + y, 1, 5, darken(skin, 18));
  rect(ctx, 20, 9 + y, 1, 5, darken(skin, 18));

  // ── Brock eyes: signature squint (closed-looking horizontal lines) ──
  // Eyebrows (thick, expressive)
  rect(ctx, 12, 8 + y, 4, 1, hairShadow);
  rect(ctx, 17, 8 + y, 4, 1, hairShadow);
  // Squinted eyes — just a thin dark line
  rect(ctx, 13, 10 + y, 3, 1, '#1a1a1a');
  rect(ctx, 17, 10 + y, 3, 1, '#1a1a1a');
  // Small shadow below each eye
  px(ctx, 13, 11 + y, darken(skin, 12));
  px(ctx, 15, 11 + y, darken(skin, 12));
  px(ctx, 17, 11 + y, darken(skin, 12));
  px(ctx, 19, 11 + y, darken(skin, 12));

  // ── Mouth: slight smirk ──
  rect(ctx, 14, 13 + y, 4, 1, '#7a3520');
  px(ctx, 13, 13 + y, '#7a3520');

  // ── Brock hair: spiky on top, no hat, sides going down ──
  // Top spikes (3 main spikes pointing up/outward)
  rect(ctx, 9,  0 + y, 3, 4, hair);   // left outer spike
  rect(ctx, 12, 1 + y, 3, 3, hair);   // left inner spike
  rect(ctx, 15, 0 + y, 3, 3, hair);   // center spike
  rect(ctx, 18, 1 + y, 3, 3, hair);   // right inner spike
  rect(ctx, 21, 0 + y, 3, 4, hair);   // right outer spike
  // Hair band connecting spikes across forehead
  rect(ctx, 9,  3 + y, 14, 5, hair);
  // Side hair going down past ears
  rect(ctx, 9,  8 + y, 2, 8, hair);
  rect(ctx, 8,  10 + y, 2, 6, hair);
  rect(ctx, 21, 8 + y, 2, 8, hair);
  rect(ctx, 22, 10 + y, 2, 6, hair);
  // Spike tips shadow
  px(ctx, 9,  0 + y, hairShadow);
  px(ctx, 21, 0 + y, hairShadow);
  px(ctx, 15, 0 + y, hairShadow);

  return canvas;
}

// ═══════════════════════════════════════════════════════════════
// MISTY
// ═══════════════════════════════════════════════════════════════

function generateMistySprite(palette: SpritePalette, frame: number): HTMLCanvasElement {
  const canvas = document.createElement('canvas');
  canvas.width = BASE_W;
  canvas.height = BASE_H;
  const ctx = canvas.getContext('2d')!;

  const hair       = palette.hat;      // hat slot holds hair color (orange)
  const hairShadow = darken(hair, 28);
  const top        = palette.shirt;    // shirt slot holds top color (red)
  const topShadow  = darken(top, 22);
  const shorts     = palette.pants;    // pants slot holds shorts color (teal)
  const shortsShadow = palette.pantsShadow;
  const skin       = palette.skin;
  const skinShadow = darken(skin, 20);
  const suspender  = '#ffd700';        // iconic yellow suspenders
  const shoe       = top;             // shoes match the top

  const bobY = frame === 0 ? 0 : 1;
  const y = bobY;

  // ── Shoes (red, matching top) ──
  rect(ctx, 11, 39 + y, 4, 3, shoe);
  rect(ctx, 17, 39 + y, 4, 3, shoe);
  rect(ctx, 10, 39 + y, 5, 2, darken(shoe, 20));
  rect(ctx, 17, 39 + y, 5, 2, darken(shoe, 20));
  rect(ctx, 11, 42 + y, 4, 1, shoe);
  rect(ctx, 17, 42 + y, 4, 1, shoe);

  // ── Legs (bare skin from knee down, shorts above) ──
  // Bare legs (skin-colored)
  rect(ctx, 12, 34 + y, 3, 5, skin);
  rect(ctx, 17, 34 + y, 3, 5, skin);
  rect(ctx, 11, 35 + y, 5, 4, skin);
  rect(ctx, 16, 35 + y, 5, 4, skin);

  // ── Shorts ──
  rect(ctx, 11, 29 + y, 10, 6, shorts);
  rect(ctx, 12, 28 + y, 8, 7, shorts);
  rect(ctx, 12, 34 + y, 8, 1, shortsShadow);

  // ── Arms (skin, bare) ──
  rect(ctx, 8,  20 + y, 3, 9, skin);
  rect(ctx, 21, 20 + y, 3, 9, skin);
  rect(ctx, 7,  22 + y, 2, 6, skin);
  rect(ctx, 23, 22 + y, 2, 6, skin);
  rect(ctx, 8,  28 + y, 3, 2, skin);
  rect(ctx, 21, 28 + y, 3, 2, skin);

  // ── Crop top (covers upper torso only) ──
  rect(ctx, 11, 19 + y, 10, 9, top);
  rect(ctx, 12, 18 + y, 8, 10, top);
  rect(ctx, 10, 20 + y, 12, 7, top);
  rect(ctx, 11, 27 + y, 10, 1, topShadow);

  // ── Suspenders (yellow Y-shape) ──
  // Left strap going from shorts up to shoulder
  rect(ctx, 13, 18 + y, 2, 10, suspender);
  // Right strap
  rect(ctx, 17, 18 + y, 2, 10, suspender);

  // ── Neck ──
  rect(ctx, 14, 17 + y, 4, 2, skin);

  // ── Head ──
  fillCircle(ctx, 16, 12 + y, 5, skin);
  rect(ctx, 12, 7 + y, 8, 2, skin);
  rect(ctx, 11, 8 + y, 10, 7, skin);
  rect(ctx, 12, 15 + y, 8, 1, skin);
  rect(ctx, 13, 16 + y, 6, 1, skin);
  rect(ctx, 11, 9 + y, 1, 5, skinShadow);
  rect(ctx, 20, 9 + y, 1, 5, skinShadow);

  // ── Eyes (open, with slight lash hint) ──
  rect(ctx, 13, 9 + y, 2, 2, '#ffffff');
  rect(ctx, 17, 9 + y, 2, 2, '#ffffff');
  rect(ctx, 13, 9 + y, 2, 1, '#1a1a1a');
  rect(ctx, 13, 10 + y, 1, 1, '#1a1a1a');
  rect(ctx, 18, 10 + y, 1, 1, '#1a1a1a');
  rect(ctx, 17, 9 + y, 2, 1, '#1a1a1a');
  // Eyelash hints (single px above each eye)
  px(ctx, 12, 8 + y, '#1a1a1a');
  px(ctx, 14, 8 + y, '#1a1a1a');
  px(ctx, 17, 8 + y, '#1a1a1a');
  px(ctx, 19, 8 + y, '#1a1a1a');

  // ── Mouth: small smile ──
  rect(ctx, 14, 13 + y, 4, 1, '#c0392b');
  px(ctx, 13, 12 + y, '#c0392b');
  px(ctx, 18, 12 + y, '#c0392b');

  // ── Misty hair: orange, side ponytail to the right ──
  // Front hair (swept up, small section on forehead/sides)
  rect(ctx, 10, 3 + y, 3, 6, hair);          // left side hair
  rect(ctx, 9,  5 + y, 2, 5, hair);
  // Top of head hair (swept/tied up)
  rect(ctx, 12, 1 + y, 9, 5, hair);           // main hair mass on top
  rect(ctx, 10, 3 + y, 11, 4, hair);
  rect(ctx, 11, 2 + y, 10, 2, hair);
  // Hair tie area (small)
  rect(ctx, 19, 3 + y, 3, 3, darken(hair, 15));
  // Ponytail going to the right
  rect(ctx, 21, 4 + y, 7, 3, hair);           // ponytail body
  rect(ctx, 22, 3 + y, 6, 5, hair);
  rect(ctx, 24, 2 + y, 5, 7, hair);
  rect(ctx, 26, 6 + y, 4, 4, hair);
  rect(ctx, 27, 9 + y, 3, 3, hair);
  // Ponytail tip (curved down)
  rect(ctx, 27, 11 + y, 2, 3, hairShadow);
  rect(ctx, 25, 7 + y, 2, 2, hairShadow);

  return canvas;
}
