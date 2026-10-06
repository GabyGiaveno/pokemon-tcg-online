import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { PasswordStrengthState } from '../../models/password-strength.types';

@Component({
  selector: 'app-password-strength',
  imports: [],
  templateUrl: './password-strength.html',
  styleUrl: './password-strength.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PasswordStrengthComponent {
  readonly strength = input<PasswordStrengthState>({
    score: 0,
    label: 'Sin evaluar',
    colorClass: 'weak'
  });
}
