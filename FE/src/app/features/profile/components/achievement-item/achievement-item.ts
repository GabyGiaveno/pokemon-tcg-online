import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ProfileAchievement } from '../../data-access/profile-api.types';

@Component({
  selector: 'app-achievement-item',
  imports: [],
  templateUrl: './achievement-item.html',
  styleUrl: './achievement-item.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AchievementItem {
  readonly achievement = input.required<ProfileAchievement>();
}
