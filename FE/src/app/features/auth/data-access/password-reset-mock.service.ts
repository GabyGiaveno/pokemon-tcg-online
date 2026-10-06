import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { ForgotPasswordRequest, PasswordResetResponse, ResetPasswordRequest } from '../models/password-reset.types';
import { createMockResetToken, updateMockPassword } from './mock-auth-store';
import { PasswordResetSource } from './password-reset-source';

@Injectable({ providedIn: 'root' })
export class PasswordResetMockService implements PasswordResetSource {
  forgotPassword(request: ForgotPasswordRequest): Observable<PasswordResetResponse> {
    try {
      const token = createMockResetToken(request.email);
      return of({
        message: 'Te enviamos un enlace de recuperación de prueba.',
        token,
      });
    } catch (error) {
      return throwError(() => error instanceof Error ? error : new Error('Reset request failed'));
    }
  }

  resetPassword(request: ResetPasswordRequest): Observable<PasswordResetResponse> {
    try {
      const username = updateMockPassword(request.token, request.newPassword);
      return of({ message: `Contraseña actualizada para ${username}.` });
    } catch (error) {
      return throwError(() => error instanceof Error ? error : new Error('Reset failed'));
    }
  }
}
