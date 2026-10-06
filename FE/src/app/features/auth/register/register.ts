import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthFeedbackComponent } from '../components/auth-feedback/auth-feedback';
import { AuthShell } from '../components/auth-shell/auth-shell';
import { PasswordStrengthComponent } from '../components/password-strength/password-strength';
import { AuthApiService } from '../data-access/auth-api.service';
import { mapRegisterFormToRequest } from '../data-access/auth-mapper';
import { AuthFeedback } from '../models/auth-feedback.types';
import { RegisterFormState } from '../models/auth-form.types';
import { BackgroundMusicService } from '../../../shared/services/background-music.service';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, AuthShell, AuthFeedbackComponent, PasswordStrengthComponent],
  templateUrl: './register.html',
  styleUrl: './register.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RegisterPage {
  private readonly authApi = inject(AuthApiService);
  private readonly router = inject(Router);
  private readonly backgroundMusic = inject(BackgroundMusicService);

  constructor() {
    this.backgroundMusic.stop();
  }

  protected readonly feedback = signal<AuthFeedback | null>({
    type: 'info',
    message: 'Prepará tu perfil para un acceso competitivo al ecosistema del juego.'
  });

  protected readonly form = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(3), Validators.maxLength(50)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    confirmPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] })
    //avatarUrl: new FormControl('', { nonNullable: true })
  });

  protected readonly loading = signal(false);

  protected get strength() {
    const password = this.form.controls.password.value;
    let score = 0;

    if (password.length >= 8) score += 1;
    if (/[A-Z]/.test(password)) score += 1;
    if (/[a-z]/.test(password)) score += 1;
    if (/\d/.test(password)) score += 1;
    if (/[^A-Za-z0-9]/.test(password)) score += 1;

    if (score <= 1) return { score, label: 'Débil', colorClass: 'weak' };
    if (score <= 3) return { score, label: 'Media', colorClass: 'medium' };
    return { score, label: 'Fuerte', colorClass: 'strong' };
  }

  protected control(name: keyof typeof this.form.controls): AbstractControl<string, string> {
    return this.form.controls[name];
  }

  protected hasControlError(name: keyof typeof this.form.controls, errorName: string): boolean {
    const control = this.form.controls[name];
    return Boolean(control.touched && control.errors?.[errorName]);
  }

  protected get passwordsMatch(): boolean {
    const { password, confirmPassword } = this.form.controls;
    return password.value === confirmPassword.value;
  }

  protected submit(): void {
    const { password, confirmPassword } = this.form.controls;

    if (this.loading()) {
      return;
    }

    if (this.form.invalid || password.value !== confirmPassword.value) {
      this.feedback.set({ type: 'error', message: this.resolveFormErrorMessage() });
      this.form.markAllAsTouched();
      return;
    }

    const payload = mapRegisterFormToRequest(this.form.getRawValue() as RegisterFormState);
    this.loading.set(true);
    this.feedback.set({ type: 'info', message: 'Creando tu cuenta...' });

    this.authApi.register(payload).subscribe({
      next: (response) => {
        this.loading.set(false);
        // Tras crear la cuenta, mandamos al login (no auto-login): el usuario
        // inicia sesión para entrar al lobby. El query param dispara el mensaje de éxito.
        void this.router.navigate(['/auth/login'], {
          queryParams: { registered: response.username },
        });
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.feedback.set({ type: 'error', message: this.resolveRegisterError(error) });
      }
    });
  }

  private resolveRegisterError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'No se pudo conectar con el servidor. Intentá de nuevo en unos segundos.';
      }

      const backendMessage = error.error?.message;
      if (typeof backendMessage === 'string' && backendMessage.trim().length > 0) {
        return backendMessage;
      }
    }

    return 'No se pudo crear la cuenta. Revisá los datos ingresados.';
  }

  private resolveFormErrorMessage(): string {
    const { username, email, password, confirmPassword } = this.form.controls;

    if (username.hasError('required')) {
      return 'El usuario es obligatorio.';
    }

    if (username.hasError('minlength')) {
      return 'El usuario debe tener entre 3 y 50 caracteres.';
    }

    if (email.hasError('required')) {
      return 'El email es obligatorio.';
    }

    if (email.hasError('email')) {
      return 'El email no tiene un formato válido.';
    }

    if (password.hasError('required')) {
      return 'La contraseña es obligatoria.';
    }

    if (confirmPassword.hasError('required')) {
      return 'Confirmá la contraseña.';
    }

    if (password.value !== confirmPassword.value) {
      return 'Las contraseñas no coinciden.';
    }

    return 'Revisá los datos ingresados.';
  }
}
