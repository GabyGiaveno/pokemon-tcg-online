import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { CardInstanceDto } from '../models/board-state.dto';
import { AttackDetailDto, CardDetailDto } from '../models/card-detail.dto';
import { resolveCardArt } from '../services/card-art';
import { getTypeColor, getTypeInfo } from '../../pokedex/domain/constants/pokemon-types';
import { VanillaTiltDirective } from '../../../shared/directives/vanilla-tilt.directive';
import { CardGlowDirective } from '../../../shared/directives/card-glow.directive';

/** A single attack option, decorated with whether it's currently affordable. */
export interface InspectionAttackOption extends AttackDetailDto {
  affordable: boolean;
}

export interface InspectionAbility {
  name: string;
  text: string;
}

/** Energy-type names in Spanish (the API serves them in English). */
const TYPE_ES: Record<string, string> = {
  Grass: 'Planta',
  Fire: 'Fuego',
  Water: 'Agua',
  Lightning: 'Rayo',
  Psychic: 'Psíquico',
  Fighting: 'Lucha',
  Darkness: 'Oscuridad',
  Metal: 'Metal',
  Fairy: 'Hada',
  Dragon: 'Dragón',
  Colorless: 'Incoloro',
};

/** Card supertype / subtype labels in Spanish. */
const CATEGORY_ES: Record<string, string> = {
  'Pokémon': 'Pokémon',
  Energy: 'Energía',
  Trainer: 'Entrenador',
  Basic: 'Básico',
  'Stage 1': 'Fase 1',
  'Stage 2': 'Fase 2',
  EX: 'EX',
  MEGA: 'MEGA',
  Item: 'Objeto',
  Supporter: 'Partidario',
  Stadium: 'Estadio',
  'Pokémon Tool': 'Herramienta',
};

/**
 * Dumb component: fullscreen card inspection overlay, replacing `attack-modal`.
 * Reuses the Pokédex `card-fullscreen-*` visual pattern (blur backdrop, gold
 * glow, scanlines, 5:7 shell).
 *
 * - `mode: 'inspect'` — read-only (hand/bench/any non-actionable card).
 * - `mode: 'inspect-attack'` — own active Pokémon; renders attacks/abilities
 *   as selectable buttons gated by `energyCount` (display-only; BE remains
 *   the source of truth for legality).
 */
@Component({
  selector: 'app-card-inspection-overlay',
  templateUrl: './card-inspection-overlay.html',
  styleUrl: './card-inspection-overlay.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [VanillaTiltDirective, CardGlowDirective],
})
export class CardInspectionOverlay {
  /** The card being inspected. */
  readonly card = input.required<CardInstanceDto>();
  /** 'inspect' = read-only; 'inspect-attack' = own active, attacks are clickable. */
  readonly mode = input<'inspect' | 'inspect-attack'>('inspect');
  /** Full card detail (HP, types, attacks, weakness/resistance, retreat) — null while loading. */
  readonly detail = input<CardDetailDto | null>(null);
  /** Read-only abilities shown below the attack list. */
  readonly abilities = input<InspectionAbility[]>([]);
  /** Energy currently attached to the active Pokémon (affordability display). */
  readonly energyCount = input(0);
  /** Whether the inspected own-active can retreat (has bench) — shows the Retreat button. */
  readonly canRetreat = input(false);

  readonly attackSelected = output<number>();
  readonly retreatRequested = output<void>();
  readonly closed = output<void>();

  /** Index of the currently selected attack — drives the gold glow (FIX 3, no panel over the art). */
  protected readonly selectedAttackIndex = signal<number | null>(null);

  protected readonly artUrl = computed(() => resolveCardArt(this.card().cardId));
  protected readonly hasArt = computed(() => this.artUrl().length > 0);

  protected readonly attackOptions = computed<InspectionAttackOption[]>(() => {
    const energy = this.energyCount();
    return (this.detail()?.attacks ?? []).map((attack) => ({
      ...attack,
      affordable: attack.cost.length <= energy,
    }));
  });

  /** Spanish category line, e.g. "Pokémon · Fase 1" or "Entrenador · Objeto". */
  protected readonly categoryLabel = computed(() => {
    const detail = this.detail();
    if (!detail) return '';
    return [detail.supertype, ...detail.subtypes]
      .map((value) => this.categoryEs(value))
      .filter((label) => label)
      .join(' · ');
  });

  protected typeIcon(type: string): string {
    return getTypeInfo(type)?.icon ?? '⭐';
  }

  protected typeColor(type: string): string {
    return getTypeColor(type);
  }

  protected typeNameEs(type: string): string {
    return TYPE_ES[type] ?? type;
  }

  private categoryEs(value: string): string {
    return CATEGORY_ES[value] ?? value;
  }

  protected handleAttackClick(option: InspectionAttackOption): void {
    if (!option.affordable) {
      return;
    }
    this.selectedAttackIndex.set(option.index);
    this.attackSelected.emit(option.index);
  }

  protected handleRetreat(): void {
    this.retreatRequested.emit();
  }

  protected handleClose(): void {
    this.closed.emit();
  }
}
