import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { PlayerProfileResponse, TrainerSkin } from '../../data-access/profile-api.types';
import { TrainerAvatar } from '../trainer-avatar/trainer-avatar';

@Component({
  selector: 'app-trainer-card',
  imports: [TrainerAvatar, DatePipe],
  templateUrl: './trainer-card.html',
  styleUrl: './trainer-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrainerCard {
  readonly profile = input.required<PlayerProfileResponse>();
  readonly currentSkin = input.required<TrainerSkin>();
  readonly skinIndex = input.required<number>();
  readonly totalSkins = input.required<number>();
  readonly equiping = input<boolean>(false);
  readonly prevSkin = output<void>();
  readonly nextSkin = output<void>();
  readonly confirmSkin = output<void>();
}
