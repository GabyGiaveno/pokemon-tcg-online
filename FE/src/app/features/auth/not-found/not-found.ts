import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthFeedbackComponent } from '../components/auth-feedback/auth-feedback';
import { AuthShell } from '../components/auth-shell/auth-shell';
import { AuthFeedback } from '../models/auth-feedback.types';

@Component({
  selector: 'app-not-found-page',
  standalone: true,
  imports: [RouterLink, AuthShell, AuthFeedbackComponent],
  templateUrl: './not-found.html',
  styleUrl: './not-found.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotFoundPage {
  protected readonly feedback = signal<AuthFeedback>({
    type: 'error',
    message: 'No encontramos la página que buscás.'
  });
}
