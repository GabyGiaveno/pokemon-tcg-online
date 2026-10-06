import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FeedEntry } from '../models/feed-entry.model';

@Component({
  selector: 'app-log-ribbon',
  templateUrl: './log-ribbon.html',
  styleUrl: './log-ribbon.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogRibbon {
  readonly entries = input<FeedEntry[]>([]);
}
