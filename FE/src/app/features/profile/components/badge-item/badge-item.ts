import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ProfileBadge } from '../../data-access/profile-api.types';

@Component({
  selector: 'app-badge-item',
  imports: [],
  templateUrl: './badge-item.html',
  styleUrl: './badge-item.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BadgeItem {
  readonly badge = input.required<ProfileBadge>();
}
