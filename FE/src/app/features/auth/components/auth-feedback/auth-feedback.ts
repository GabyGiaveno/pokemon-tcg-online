import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AuthFeedback } from '../../models/auth-feedback.types';

@Component({
  selector: 'app-auth-feedback',
  imports: [],
  templateUrl: './auth-feedback.html',
  styleUrl: './auth-feedback.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuthFeedbackComponent {
  readonly feedback = input<AuthFeedback | null>(null);
}
