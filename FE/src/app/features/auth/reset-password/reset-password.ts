import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthFeedbackComponent } from '../components/auth-feedback/auth-feedback';
import { AuthShell } from '../components/auth-shell/auth-shell';
import { PasswordResetApiService } from '../data-access/password-reset-api.service';
import { AuthFeedback } from '../models/auth-feedback.types';
import { BackgroundMusicService } from '../../../shared/services/background-music.service';

@Component({
  selector: 'app-reset-password-page',
  imports: [ReactiveFormsModule, RouterLink, AuthShell, AuthFeedbackComponent],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResetPasswordPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly passwordResetApi = inject(PasswordResetApiService);
  private readonly backgroundMusic = inject(BackgroundMusicService);

  constructor() {
    this.backgroundMusic.stop();
  }

  protected readonly token = this.route.snapshot.queryParamMap.get('token');
  protected readonly feedback = signal<AuthFeedback | null>(
    !this.token
      ? { type: 'error', message: 'Enlace inválido o expirado. Solicitá un nuevo restablecimiento.' }
      : null
  );

  protected readonly form = new FormGroup({
    newPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(6)],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  protected get invalidToken(): boolean {
    return !this.token;
  }

  protected submit(): void {
    if (this.form.invalid || !this.token) {
      this.feedback.set({ type: 'error', message: 'Completá los campos obligatorios.' });
      this.form.markAllAsTouched();
      return;
    }

    const { newPassword, confirmPassword } = this.form.getRawValue();

    if (newPassword !== confirmPassword) {
      this.feedback.set({ type: 'error', message: 'Las contraseñas no coinciden.' });
      return;
    }

    this.passwordResetApi.resetPassword({ token: this.token, newPassword }).subscribe({
      next: (response) => {
        this.feedback.set({ type: 'success', message: response.message });
        this.form.reset();
        void this.router.navigateByUrl('/auth/login');
      },
      error: () => {
        this.feedback.set({
          type: 'error',
          message: 'El enlace es inválido o expiró. Solicitá uno nuevo.',
        });
      },
    });
  }
}
