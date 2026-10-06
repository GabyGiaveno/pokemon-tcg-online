import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ActionAvailabilityDto, AvailableAction } from '../models/game-state.dto';

@Component({
  selector: 'app-action-panel',
  templateUrl: './action-panel.html',
  styleUrl: './action-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActionPanel {
  readonly actions = input<ActionAvailabilityDto | null>(null);
  /** true when the drawer is expanded into view. */
  readonly open = input(false);
  readonly execute = output<AvailableAction>();
}
