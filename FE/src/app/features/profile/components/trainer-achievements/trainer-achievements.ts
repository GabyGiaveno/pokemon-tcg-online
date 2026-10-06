import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ProfileAchievement } from '../../data-access/profile-api.types';
import { AchievementItem } from '../achievement-item/achievement-item';

@Component({
  selector: 'app-trainer-achievements',
  imports: [AchievementItem],
  templateUrl: './trainer-achievements.html',
  styleUrl: './trainer-achievements.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrainerAchievements {
  readonly achievements = input.required<ProfileAchievement[]>();
}
