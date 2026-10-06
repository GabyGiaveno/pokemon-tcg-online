import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ProfileBadge } from '../../data-access/profile-api.types';
import { BadgeItem } from '../badge-item/badge-item';

@Component({
  selector: 'app-trainer-badges',
  imports: [BadgeItem],
  templateUrl: './trainer-badges.html',
  styleUrl: './trainer-badges.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrainerBadges {
  readonly badges = input.required<ProfileBadge[]>();
}
