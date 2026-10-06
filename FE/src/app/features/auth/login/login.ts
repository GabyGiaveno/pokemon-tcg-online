import { ChangeDetectionStrategy, Component, OnDestroy, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthFeedbackComponent } from '../components/auth-feedback/auth-feedback';
import { AuthShell } from '../components/auth-shell/auth-shell';
import { PokeballLoader } from '../../../shared/components/pokeball-loader/pokeball-loader';
import { AuthApiService } from '../data-access/auth-api.service';
import { AuthTokenService } from '../data-access/auth-token.service';
import { mapLoginFormToRequest } from '../data-access/auth-mapper';
import { AuthFeedback } from '../models/auth-feedback.types';
import { AuthFormState } from '../models/auth-form.types';
import { BackgroundMusicService } from '../../../shared/services/background-music.service';

const LOGIN_IDENTIFIER_KEY = 'pokemon_tcg_login_identifier';

/** How long the Poké Ball transition overlay shows before landing on the lobby. */
const LOBBY_TRANSITION_MS = 1800;

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthShell, AuthFeedbackComponent, PokeballLoader],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginPage implements OnDestroy {
  private readonly authApi = inject(AuthApiService);
  private readonly authToken = inject(AuthTokenService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly backgroundMusic = inject(BackgroundMusicService);

  protected readonly feedback = signal<AuthFeedback | null>(this.buildInitialFeedback());

  /** While true, the spinning Poké Ball overlay covers the screen during the lobby transition. */
  protected readonly entering = signal(false);

  private transitionTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.backgroundMusic.stop();
  }

  protected readonly form = new FormGroup({
    identifier: new FormControl(this.loadRememberedIdentifier(), { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    rememberMe: new FormControl(false, { nonNullable: true })
  });

  protected readonly loading = signal(false);

  protected submit(): void {
    if (this.form.invalid) {
      this.feedback.set({ type: 'error', message: 'Completá los campos obligatorios antes de continuar.' });
      this.form.markAllAsTouched();
      return;
    }

    if (this.loading()) {
      return;
    }

    const payload = mapLoginFormToRequest(this.form.getRawValue() as AuthFormState);
    this.loading.set(true);
    this.feedback.set({ type: 'info', message: 'Verificando credenciales...' });

    const { identifier, rememberMe } = this.form.getRawValue();
    if (rememberMe) {
      localStorage.setItem(LOGIN_IDENTIFIER_KEY, identifier);
    } else {
      localStorage.removeItem(LOGIN_IDENTIFIER_KEY);
    }

    this.authApi.login(payload).subscribe({
      next: (response) => {
        this.authToken.setSession(response.token, response.username, response.id);
        this.feedback.set({ type: 'success', message: `Bienvenido ${response.username}.` });
        // Spinning Poké Ball transition, then land on the lobby.
        this.entering.set(true);
        this.transitionTimeout = setTimeout(() => {
          void this.router.navigateByUrl('/lobby');
        }, LOBBY_TRANSITION_MS);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.feedback.set({ type: 'error', message: this.resolveLoginError(error) });
      }
    });
  }

  ngOnDestroy(): void {
    if (this.transitionTimeout) {
      clearTimeout(this.transitionTimeout);
    }
  }

  private buildInitialFeedback(): AuthFeedback {
    const registeredUser = this.route.snapshot.queryParamMap.get('registered');
    if (registeredUser) {
      return {
        type: 'success',
        message: `Cuenta creada para ${registeredUser}. Iniciá sesión para entrar al lobby.`,
      };
    }
    return { type: 'info', message: 'Entrá con tu usuario o email para continuar.' };
  }

  protected hasError(name: 'identifier' | 'password', errorName: string): boolean {
    const control = this.form.controls[name];
    return Boolean(control.touched && control.errors?.[errorName]);
  }

  private resolveLoginError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return 'Usuario o contraseña incorrectos.';
      }

      if (error.status === 0) {
        return 'No se pudo conectar con el servidor. Intentá de nuevo en unos segundos.';
      }

      const backendMessage = error.error?.message;
      if (typeof backendMessage === 'string' && backendMessage.trim().length > 0) {
        return backendMessage;
      }
    }

    return 'No se pudo iniciar sesión. Revisá tus credenciales.';
  }

  private loadRememberedIdentifier(): string {
    try {
      return localStorage.getItem(LOGIN_IDENTIFIER_KEY) ?? '';
    } catch {
      return '';
    }
  }
}
