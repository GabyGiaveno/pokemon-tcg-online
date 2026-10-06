import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthFeedbackComponent } from '../components/auth-feedback/auth-feedback';
import { AuthShell } from '../components/auth-shell/auth-shell';
import { PasswordResetApiService } from '../data-access/password-reset-api.service';
import { AuthFeedback } from '../models/auth-feedback.types';

@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink, AuthShell, AuthFeedbackComponent],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ForgotPasswordPage {
  private readonly passwordResetApi = inject(PasswordResetApiService);
  private readonly router = inject(Router);
  protected readonly feedback = signal<AuthFeedback | null>(null);

  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] })
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.feedback.set({ type: 'error', message: 'Ingresá un correo válido antes de continuar.' });
      this.form.markAllAsTouched();
      return;
    }

    this.passwordResetApi.forgotPassword({ email: this.form.getRawValue().email }).subscribe({
      next: (response) => {
        this.feedback.set({ type: 'success', message: response.message });
        this.form.reset();
        if (response.token) {
          void this.router.navigateByUrl(`/auth/reset-password?token=${response.token}`);
        }
      },
      error: () => {
        this.feedback.set({
          type: 'error',
          message: 'No se pudo procesar la solicitud. Intentá de nuevo más tarde.',
        });
      },
    });
  }
}
