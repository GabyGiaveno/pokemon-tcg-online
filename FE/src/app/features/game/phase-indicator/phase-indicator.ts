import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Dumb component: status bar showing turn, phase, and turn number. */
@Component({
  selector: 'app-phase-indicator',
  templateUrl: './phase-indicator.html',
  styleUrl: './phase-indicator.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PhaseIndicator {
  readonly phase = input<'SETUP' | 'DRAW' | 'MAIN' | 'ATTACK' | 'BETWEEN_TURNS' | null>(null);
  readonly turnNumber = input(0);
  readonly isMyTurn = input(false);

  protected readonly turnLabel = computed(() => (this.isMyTurn() ? 'Tu turno' : 'Turno rival'));

  protected readonly phaseLabel = computed(() => {
    switch (this.phase()) {
      case 'SETUP': return 'Preparación';
      case 'DRAW': return 'Robo';
      case 'MAIN': return 'Principal';
      case 'ATTACK': return 'Ataque';
      case 'BETWEEN_TURNS': return 'Cambio de turno';
      default: return 'Preparación';
    }
  });
}
