import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FeedEntry } from '../models/feed-entry.model';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.html',
  styleUrl: './notifications.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Notifications {
  readonly notifications = input<FeedEntry[]>([]);
}
