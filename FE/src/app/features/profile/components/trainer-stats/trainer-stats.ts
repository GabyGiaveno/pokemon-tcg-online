import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ProfileStats } from '../../data-access/profile-api.types';

@Component({
  selector: 'app-trainer-stats',
  imports: [],
  templateUrl: './trainer-stats.html',
  styleUrl: './trainer-stats.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrainerStats {
  readonly stats = input.required<ProfileStats>();
}
